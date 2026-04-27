package ru.vkr.ldpcapp.service.phy.codec;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.NrBaseGraphLoader;

import java.util.ArrayList;
import java.util.List;

public class CodecEngine {
    private static final NrBaseGraphLoader BG_LOADER = new NrBaseGraphLoader();

    private static final LdpcCode NR_BG1_Z8_CODE = build5gNrBg1Code(8);
    private static final LdpcCode NR_BG1_Z16_CODE = build5gNrBg1Code(16);
    private static final LdpcCode NR_BG1_Z32_CODE = build5gNrBg1Code(32);
    private static final LdpcCode EDUCATIONAL_CODE = buildEducationalCode();
    private static final LdpcCode QC_LIKE_CODE = buildQcInspiredCode();
    private static final PolarCode POLAR_128_64 = buildPolarCode128_64();

    public ActiveCode resolveActiveCode(SimulationConfig config) {
        if (SimulationConfig.PROFILE_POLAR.equals(config.getLdpcProfile())) {
            return new ActiveCode(true, null, POLAR_128_64, POLAR_128_64.k, POLAR_128_64.n, POLAR_128_64.rate);
        }
        LdpcCode ldpc = getLdpcCode(config.getLdpcProfile(), config);
        return new ActiveCode(false, ldpc, null, ldpc.k, ldpc.n, ldpc.rate);
    }

    public int[] encode(int[] infoBits, ActiveCode code) {
        return code.polarMode
                ? encodePolar(infoBits, code.polar)
                : encodeLdpc(infoBits, code.ldpc);
    }

    public DecodeResult decodeFromLlr(double[] llr, ActiveCode code, int maxIterations, double normalization) {
        return code.polarMode
                ? decodePolarFromLlr(llr, code.polar)
                : decodeLdpcFromLlr(llr, code.ldpc, maxIterations, normalization);
    }

    private LdpcCode getLdpcCode(String profile, SimulationConfig config) {
        if (!SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            return SimulationConfig.PROFILE_QC.equals(profile) ? QC_LIKE_CODE : EDUCATIONAL_CODE;
        }
        int z = config == null ? 8 : config.getLiftingSize();
        return switch (z) {
            case 16 -> NR_BG1_Z16_CODE;
            case 32 -> NR_BG1_Z32_CODE;
            default -> NR_BG1_Z8_CODE;
        };
    }

    private int[] encodeLdpc(int[] infoBits, LdpcCode code) {
        int[] parityBits = new int[code.m];
        for (int row = 0; row < code.m; row++) {
            int parityEquation = 0;
            for (int column : code.messageTaps[row]) {
                parityEquation ^= infoBits[column];
            }
            parityBits[row] = parityEquation ^ (row > 0 ? parityBits[row - 1] : 0);
        }

        int[] encoded = new int[code.n];
        System.arraycopy(infoBits, 0, encoded, 0, code.k);
        System.arraycopy(parityBits, 0, encoded, code.k, code.m);
        return encoded;
    }

    private DecodeResult decodeLdpcFromLlr(double[] inputLlr, LdpcCode code, int maxIterations, double normalization) {
        double[] channelLlr = new double[code.n];
        System.arraycopy(inputLlr, 0, channelLlr, 0, code.n);

        double[][] q = new double[code.checkToVars.length][];
        double[][] r = new double[code.checkToVars.length][];

        for (int check = 0; check < code.checkToVars.length; check++) {
            int degree = code.checkToVars[check].length;
            q[check] = new double[degree];
            r[check] = new double[degree];
            for (int edge = 0; edge < degree; edge++) {
                int variable = code.checkToVars[check][edge];
                q[check][edge] = channelLlr[variable];
                r[check][edge] = 0.0;
            }
        }

        int[] hard = hardDecision(channelLlr);

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            for (int check = 0; check < code.checkToVars.length; check++) {
                double[] messages = q[check];
                int signProduct = 1;
                double min1 = Double.POSITIVE_INFINITY;
                double min2 = Double.POSITIVE_INFINITY;
                int minIndex = -1;

                for (int edge = 0; edge < messages.length; edge++) {
                    double value = messages[edge];
                    int sign = value < 0.0 ? -1 : 1;
                    double absolute = Math.abs(value);
                    signProduct *= sign;

                    if (absolute < min1) {
                        min2 = min1;
                        min1 = absolute;
                        minIndex = edge;
                    } else if (absolute < min2) {
                        min2 = absolute;
                    }
                }

                for (int edge = 0; edge < messages.length; edge++) {
                    double value = messages[edge];
                    int sign = value < 0.0 ? -1 : 1;
                    int extrinsicSign = signProduct * sign;
                    double minimum = edge == minIndex ? min2 : min1;
                    r[check][edge] = normalization * extrinsicSign * minimum;
                }
            }

            double[] posterior = channelLlr.clone();
            for (int variable = 0; variable < code.n; variable++) {
                for (EdgeRef edgeRef : code.varToChecks[variable]) {
                    posterior[variable] += r[edgeRef.check][edgeRef.edge];
                }
            }

            hard = hardDecision(posterior);
            if (checkSyndrome(hard, code)) {
                return new DecodeResult(sliceInfo(hard, code.k), iteration, true);
            }

            for (int variable = 0; variable < code.n; variable++) {
                for (EdgeRef edgeRef : code.varToChecks[variable]) {
                    q[edgeRef.check][edgeRef.edge] = posterior[variable] - r[edgeRef.check][edgeRef.edge];
                }
            }
        }

        return new DecodeResult(sliceInfo(hard, code.k), maxIterations, checkSyndrome(hard, code));
    }

    private int[] encodePolar(int[] infoBits, PolarCode code) {
        int[] u = new int[code.n];
        int src = 0;
        for (int i = 0; i < code.n; i++) {
            u[i] = code.infoMask[i] ? infoBits[src++] : 0;
        }
        return polarTransform(u);
    }

    private DecodeResult decodePolarFromLlr(double[] llr, PolarCode code) {
        int[] uHat = decodePolarRecursive(llr, code.infoMask, 0);
        int[] info = new int[code.k];
        int idx = 0;
        for (int i = 0; i < code.n; i++) {
            if (code.infoMask[i]) {
                info[idx++] = uHat[i];
            }
        }
        return new DecodeResult(info, 1, true);
    }

    private int[] polarTransform(int[] input) {
        int[] out = input.clone();
        for (int len = 1; len < out.length; len <<= 1) {
            int step = len << 1;
            for (int start = 0; start < out.length; start += step) {
                for (int i = 0; i < len; i++) {
                    out[start + i] ^= out[start + len + i];
                }
            }
        }
        return out;
    }

    private int[] decodePolarRecursive(double[] llr, boolean[] infoMask, int offset) {
        int n = llr.length;
        int[] u = new int[n];
        if (n == 1) {
            boolean info = infoMask[offset];
            u[0] = info ? (llr[0] < 0.0 ? 1 : 0) : 0;
            return u;
        }

        int half = n / 2;
        double[] leftLlr = new double[half];
        for (int i = 0; i < half; i++) {
            leftLlr[i] = fFunction(llr[i], llr[i + half]);
        }
        int[] uLeft = decodePolarRecursive(leftLlr, infoMask, offset);

        double[] rightLlr = new double[half];
        for (int i = 0; i < half; i++) {
            rightLlr[i] = gFunction(llr[i], llr[i + half], uLeft[i]);
        }
        int[] uRight = decodePolarRecursive(rightLlr, infoMask, offset + half);

        for (int i = 0; i < half; i++) {
            u[i] = uLeft[i] ^ uRight[i];
            u[i + half] = uRight[i];
        }
        return u;
    }

    private double fFunction(double a, double b) {
        double sign = Math.signum(a) * Math.signum(b);
        return sign * Math.min(Math.abs(a), Math.abs(b));
    }

    private double gFunction(double a, double b, int u) {
        return ((u & 1) == 0) ? (b + a) : (b - a);
    }

    private boolean checkSyndrome(int[] bits, LdpcCode code) {
        for (int[] check : code.checkToVars) {
            int syndrome = 0;
            for (int variable : check) {
                syndrome ^= bits[variable];
            }
            if (syndrome != 0) {
                return false;
            }
        }
        return true;
    }

    private int[] hardDecision(double[] llr) {
        int[] bits = new int[llr.length];
        for (int i = 0; i < llr.length; i++) {
            bits[i] = llr[i] < 0.0 ? 1 : 0;
        }
        return bits;
    }

    private int[] sliceInfo(int[] bits, int k) {
        int[] info = new int[k];
        System.arraycopy(bits, 0, info, 0, k);
        return info;
    }

    private static LdpcCode build5gNrBg1Code(int z) {
        int infoCols = 22;
        int rowGroups = 46;
        int k = infoCols * z;
        int m = rowGroups * z;

        int[][] shifts = BG_LOADER.loadBg1InfoPartShifts(z, rowGroups, infoCols);

        int[][] taps = new int[m][];
        int rowIndex = 0;

        for (int rg = 0; rg < rowGroups; rg++) {
            for (int localRow = 0; localRow < z; localRow++) {
                List<Integer> vars = new ArrayList<>();
                for (int col = 0; col < infoCols; col++) {
                    int shift = shifts[rg][col];
                    if (shift < 0) {
                        continue;
                    }
                    int varIndex = col * z + ((localRow + shift) % z);
                    vars.add(varIndex);
                }
                taps[rowIndex] = vars.stream().mapToInt(Integer::intValue).toArray();
                rowIndex++;
            }
        }

        return createSystematicCode(k, m, taps);
    }

    private static LdpcCode buildEducationalCode() {
        int k = 12;
        int m = 12;
        int[] offsets = {0, 3, 7};
        int[][] taps = new int[m][offsets.length];
        for (int row = 0; row < m; row++) {
            for (int i = 0; i < offsets.length; i++) {
                taps[row][i] = (row + offsets[i]) % k;
            }
        }
        return createSystematicCode(k, m, taps);
    }

    private static LdpcCode buildQcInspiredCode() {
        int z = 8;
        int k = z * 6;
        int m = z * 6;

        int[][] baseGraph = {
                {0, 1, 3, 5},
                {0, 2, 4, 5},
                {1, 2, 3, 4},
                {0, 2, 4, 5},
                {1, 3, 4, 5},
                {0, 1, 2, 3}
        };
        int[][] shifts = {
                {0, 1, 2, 5},
                {3, 0, 4, 6},
                {2, 5, 1, 7},
                {6, 1, 3, 0},
                {4, 6, 2, 1},
                {5, 7, 4, 0}
        };

        int[][] taps = new int[m][4];
        int rowIndex = 0;
        for (int groupRow = 0; groupRow < baseGraph.length; groupRow++) {
            for (int localRow = 0; localRow < z; localRow++) {
                for (int j = 0; j < 4; j++) {
                    int group = baseGraph[groupRow][j];
                    int shift = shifts[groupRow][j];
                    taps[rowIndex][j] = group * z + ((localRow + shift) % z);
                }
                rowIndex++;
            }
        }

        return createSystematicCode(k, m, taps);
    }

    private static LdpcCode createSystematicCode(int k, int m, int[][] messageTaps) {
        int n = k + m;
        int[][] checkToVars = new int[m][];

        for (int row = 0; row < m; row++) {
            int extra = row > 0 ? 1 : 0;
            int[] vars = new int[messageTaps[row].length + 1 + extra];
            int idx = 0;
            for (int tap : messageTaps[row]) {
                vars[idx++] = tap;
            }
            vars[idx++] = k + row;
            if (row > 0) {
                vars[idx] = k + row - 1;
            }
            checkToVars[row] = vars;
        }

        @SuppressWarnings("unchecked")
        List<EdgeRef>[] varToChecks = (List<EdgeRef>[]) new List<?>[n];
        for (int i = 0; i < n; i++) {
            varToChecks[i] = new ArrayList<>();
        }

        for (int check = 0; check < checkToVars.length; check++) {
            for (int edge = 0; edge < checkToVars[check].length; edge++) {
                int variable = checkToVars[check][edge];
                varToChecks[variable].add(new EdgeRef(check, edge));
            }
        }

        return new LdpcCode(k, m, n, 0.5, messageTaps, checkToVars, varToChecks);
    }

    private static PolarCode buildPolarCode128_64() {
        int n = 128;
        int k = 64;
        int[] reliabilityOrder = buildPolarReliabilityOrder(n);
        boolean[] infoMask = new boolean[n];
        for (int i = n - k; i < n; i++) {
            infoMask[reliabilityOrder[i]] = true;
        }
        return new PolarCode(n, k, 0.5, infoMask);
    }

    private static int[] buildPolarReliabilityOrder(int n) {
        class RankedIndex {
            int index;
            double score;
        }

        List<RankedIndex> ranked = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            RankedIndex item = new RankedIndex();
            item.index = i;
            item.score = polarizationWeight(i, 8);
            ranked.add(item);
        }

        ranked.sort((a, b) -> Double.compare(a.score, b.score));

        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = ranked.get(i).index;
        }
        return out;
    }

    private static double polarizationWeight(int index, int bits) {
        double beta = Math.pow(2.0, 0.25);
        double value = 0.0;
        for (int j = 0; j < bits; j++) {
            int bit = (index >>> j) & 1;
            value += bit * Math.pow(beta, j);
        }
        return value;
    }

    private record EdgeRef(int check, int edge) {}
    private record LdpcCode(int k, int m, int n, double rate, int[][] messageTaps, int[][] checkToVars, List<EdgeRef>[] varToChecks) {}
    private record PolarCode(int n, int k, double rate, boolean[] infoMask) {}

    public record DecodeResult(int[] decodedInfo, int iterationsUsed, boolean success) {}
    public record ActiveCode(boolean polarMode, LdpcCode ldpc, PolarCode polar, int k, int n, double rate) {}
}