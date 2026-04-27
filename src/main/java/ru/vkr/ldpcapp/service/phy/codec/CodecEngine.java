package ru.vkr.ldpcapp.service.phy.codec;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.NrBaseGraphLoader;
import ru.vkr.ldpcapp.service.phy.codec.ldpc.LdpcCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodecEngine {
    private static final NrBaseGraphLoader BG_LOADER = new NrBaseGraphLoader();
    private static final LdpcCodec REAL_LDPC = new LdpcCodec();
    private static final Map<String, LdpcCodec.Spec> NR_SPEC_CACHE = new ConcurrentHashMap<>();

    private static final String DECODER_SUM_PRODUCT = "Sum-Product";
    private static final String DECODER_MIN_SUM = "Min-Sum";
    private static final String DECODER_NMS = "Normalized Min-Sum";
    private static final double TANH_CLAMP = 1.0 - 1e-12;

    private static final LdpcCode NR_BG1_Z8_CODE = build5gNrBg1Code(8);
    private static final LdpcCode NR_BG1_Z16_CODE = build5gNrBg1Code(16);
    private static final LdpcCode NR_BG1_Z32_CODE = build5gNrBg1Code(32);
    private static final LdpcCode NR_BG2_Z8_CODE = build5gNrBg2Code(8);
    private static final LdpcCode NR_BG2_Z16_CODE = build5gNrBg2Code(16);
    private static final LdpcCode NR_BG2_Z32_CODE = build5gNrBg2Code(32);
    private static final LdpcCode EDUCATIONAL_CODE = buildEducationalCode();
    private static final LdpcCode QC_LIKE_CODE = buildQcInspiredCode();
    private static final PolarCode POLAR_128_64 = buildPolarCode128_64();
    private static final double NEG_INF = -1e9;
    private static final TurboCode TURBO_LTE_CODE = buildTurboCode(40, 2026);

    public ActiveCode resolveActiveCode(SimulationConfig config) {
        if (SimulationConfig.PROFILE_POLAR.equals(config.getLdpcProfile())) {
            return new ActiveCode(
                    true, false, false,
                    null, POLAR_128_64, null, null,
                    POLAR_128_64.k, POLAR_128_64.n, POLAR_128_64.rate
            );
        }

        if (SimulationConfig.PROFILE_TURBO_LTE.equals(config.getLdpcProfile())) {
            return new ActiveCode(
                    false, true, false,
                    null, null, TURBO_LTE_CODE, null,
                    TURBO_LTE_CODE.k, TURBO_LTE_CODE.n, TURBO_LTE_CODE.rate
            );
        }

        if (SimulationConfig.PROFILE_5GNR_BG1.equals(config.getLdpcProfile())) {
            LdpcCodec.Spec spec = resolveNrSpec(config);
            double rate = (double) spec.k() / (double) spec.n();
            return new ActiveCode(
                    false, false, true,
                    null, null, null, spec,
                    spec.k(), spec.n(), rate
            );
        }

        LdpcCode ldpc = getLdpcCode(config.getLdpcProfile(), config);
        return new ActiveCode(
                false, false, false,
                ldpc, null, null, null,
                ldpc.k, ldpc.n, ldpc.rate
        );
    }

    public int[] encode(int[] infoBits, ActiveCode code) {
        if (code.polarMode) return encodePolar(infoBits, code.polar);
        if (code.turboMode) return encodeTurbo(infoBits, code.turbo);
        if (code.realNrLdpcMode) return REAL_LDPC.encode(infoBits, code.realSpec);
        return encodeLdpc(infoBits, code.ldpc);
    }

    public DecodeResult decodeFromLlr(double[] llr, ActiveCode code, int maxIterations, double normalization) {
        return decodeFromLlr(llr, code, maxIterations, normalization, DECODER_NMS);
    }

    public DecodeResult decodeFromLlr(
            double[] llr,
            ActiveCode code,
            int maxIterations,
            double normalization,
            String decoderType
    ) {
        if (code.polarMode) {
            return decodePolarFromLlr(llr, code.polar);
        }
        if (code.turboMode) {
            int iter = Math.max(2, Math.min(10, maxIterations));
            return decodeTurboFromLlr(llr, code.turbo, iter);
        }
        if (code.realNrLdpcMode) {
            LdpcCodec.DecodeStats stats = REAL_LDPC.decodeNmsWithStats(
                    llr,
                    code.realSpec,
                    maxIterations,
                    normalization
            );
            return new DecodeResult(stats.decodedInfo(), stats.iterationsUsed(), stats.success());
        }
        return decodeLdpcFromLlr(llr, code.ldpc, maxIterations, normalization, decoderType);
    }


    private LdpcCode getLdpcCode(String profile, SimulationConfig config) {
        if (!SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            return SimulationConfig.PROFILE_QC.equals(profile) ? QC_LIKE_CODE : EDUCATIONAL_CODE;
        }

        int z = config == null ? 8 : config.getLiftingSize();
        String bg = config == null ? SimulationConfig.NR_BG_AUTO : config.getNrBaseGraph();

        if (SimulationConfig.NR_BG2.equals(bg)) {
            return switch (z) {
                case 16 -> NR_BG2_Z16_CODE;
                case 32 -> NR_BG2_Z32_CODE;
                default -> NR_BG2_Z8_CODE;
            };
        }

        // AUTO и BG1 -> BG1
        return switch (z) {
            case 16 -> NR_BG1_Z16_CODE;
            case 32 -> NR_BG1_Z32_CODE;
            default -> NR_BG1_Z8_CODE;
        };
    }
    private int[] encodeTurbo(int[] infoBits, TurboCode code) {
        int[] sys = Arrays.copyOf(infoBits, code.k);
        int[] p1 = rscEncode(sys, code.nextState, code.parityBit);

        int[] interleaved = interleaveInt(sys, code.interleaver);
        int[] p2 = rscEncode(interleaved, code.nextState, code.parityBit);

        int[] out = new int[code.n];
        System.arraycopy(sys, 0, out, 0, code.k);
        System.arraycopy(p1, 0, out, code.k, code.k);
        System.arraycopy(p2, 0, out, 2 * code.k, code.k);
        return out;
    }

    private DecodeResult decodeTurboFromLlr(double[] llr, TurboCode code, int iterations) {
        int k = code.k;
        double[] sys = new double[k];
        double[] p1 = new double[k];
        double[] p2 = new double[k];

        for (int i = 0; i < k; i++) {
            sys[i] = i < llr.length ? llr[i] : 0.0;
            p1[i] = (k + i) < llr.length ? llr[k + i] : 0.0;
            p2[i] = (2 * k + i) < llr.length ? llr[2 * k + i] : 0.0;
        }

        double[] sysInter = interleaveDouble(sys, code.interleaver);
        double[] apriori1 = new double[k];
        double[] finalLlrInter = new double[k];

        for (int it = 0; it < iterations; it++) {
            TurboDecodeInternal dec1 = maxLogMap(sys, p1, apriori1, code.nextState, code.parityBit);
            double[] apriori2 = interleaveDouble(dec1.extrinsic, code.interleaver);

            TurboDecodeInternal dec2 = maxLogMap(sysInter, p2, apriori2, code.nextState, code.parityBit);
            apriori1 = deinterleaveDouble(dec2.extrinsic, code.inverseInterleaver);
            finalLlrInter = dec2.llr;
        }

        double[] finalLlr = deinterleaveDouble(finalLlrInter, code.inverseInterleaver);
        int[] bits = new int[k];
        for (int i = 0; i < k; i++) bits[i] = finalLlr[i] < 0.0 ? 1 : 0;

        return new DecodeResult(bits, iterations, true);
    }

    private TurboDecodeInternal maxLogMap(
            double[] sysLlr,
            double[] parityLlr,
            double[] aprioriLlr,
            int[][] nextState,
            int[][] parityBit
    ) {
        int n = sysLlr.length;
        double[][] alpha = new double[n + 1][8];
        double[][] beta = new double[n + 1][8];

        for (int k = 0; k <= n; k++) {
            Arrays.fill(alpha[k], NEG_INF);
            Arrays.fill(beta[k], 0.0);
        }
        alpha[0][0] = 0.0;

        for (int k = 0; k < n; k++) {
            for (int s = 0; s < 8; s++) {
                if (alpha[k][s] <= NEG_INF / 2) continue;
                for (int u = 0; u <= 1; u++) {
                    int ns = nextState[s][u];
                    int pb = parityBit[s][u];
                    double sysSign = (u == 0) ? 1.0 : -1.0;
                    double parSign = (pb == 0) ? 1.0 : -1.0;
                    double gamma = 0.5 * (
                            aprioriLlr[k] * sysSign +
                                    sysLlr[k] * sysSign +
                                    parityLlr[k] * parSign
                    );
                    double cand = alpha[k][s] + gamma;
                    if (cand > alpha[k + 1][ns]) alpha[k + 1][ns] = cand;
                }
            }
        }

        for (int k = n - 1; k >= 0; k--) {
            double[] cur = new double[8];
            Arrays.fill(cur, NEG_INF);
            for (int s = 0; s < 8; s++) {
                for (int u = 0; u <= 1; u++) {
                    int ns = nextState[s][u];
                    int pb = parityBit[s][u];
                    double sysSign = (u == 0) ? 1.0 : -1.0;
                    double parSign = (pb == 0) ? 1.0 : -1.0;
                    double gamma = 0.5 * (
                            aprioriLlr[k] * sysSign +
                                    sysLlr[k] * sysSign +
                                    parityLlr[k] * parSign
                    );
                    double cand = beta[k + 1][ns] + gamma;
                    if (cand > cur[s]) cur[s] = cand;
                }
            }
            beta[k] = cur;
        }

        double[] llr = new double[n];
        double[] ext = new double[n];

        for (int k = 0; k < n; k++) {
            double m0 = NEG_INF;
            double m1 = NEG_INF;
            for (int s = 0; s < 8; s++) {
                for (int u = 0; u <= 1; u++) {
                    int ns = nextState[s][u];
                    int pb = parityBit[s][u];
                    double sysSign = (u == 0) ? 1.0 : -1.0;
                    double parSign = (pb == 0) ? 1.0 : -1.0;
                    double gamma = 0.5 * (
                            aprioriLlr[k] * sysSign +
                                    sysLlr[k] * sysSign +
                                    parityLlr[k] * parSign
                    );
                    double cand = alpha[k][s] + gamma + beta[k + 1][ns];
                    if (u == 0) m0 = Math.max(m0, cand);
                    else m1 = Math.max(m1, cand);
                }
            }
            llr[k] = m0 - m1;
            ext[k] = llr[k] - aprioriLlr[k] - sysLlr[k];
        }

        return new TurboDecodeInternal(llr, ext);
    }
    private static TurboCode buildTurboCode(int k, int seed) {
        int[][] next = new int[8][2];
        int[][] parity = new int[8][2];

        for (int state = 0; state < 8; state++) {
            int s1 = state & 1;
            int s2 = (state >> 1) & 1;
            int s3 = (state >> 2) & 1;

            for (int u = 0; u <= 1; u++) {
                int feedback = u ^ s2 ^ s3;
                int pb = feedback ^ s1 ^ s2 ^ s3;
                int ns = feedback | (s1 << 1) | (s2 << 2);

                next[state][u] = ns;
                parity[state][u] = pb;
            }
        }

        int[] pi = buildInterleaver(k, seed);
        int[] inv = new int[k];
        for (int i = 0; i < k; i++) inv[pi[i]] = i;

        return new TurboCode(k, 3 * k, 1.0 / 3.0, pi, inv, next, parity);
    }

    private static int[] buildInterleaver(int size, int seed) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) arr[i] = i;

        long x = seed & 0xffffffffL;
        for (int i = size - 1; i > 0; i--) {
            x = (1664525L * x + 1013904223L) & 0xffffffffL;
            int j = (int) (x % (i + 1));
            int t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
        return arr;
    }

    private int[] rscEncode(int[] bits, int[][] nextState, int[][] parityBit) {
        int[] out = new int[bits.length];
        int state = 0;
        for (int i = 0; i < bits.length; i++) {
            int u = bits[i] & 1;
            out[i] = parityBit[state][u];
            state = nextState[state][u];
        }
        return out;
    }

    private int[] interleaveInt(int[] v, int[] pi) {
        int[] out = new int[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[pi[i]];
        return out;
    }

    private double[] interleaveDouble(double[] v, int[] pi) {
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[pi[i]];
        return out;
    }

    private double[] deinterleaveDouble(double[] v, int[] invPi) {
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[invPi[i]];
        return out;
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

    private DecodeResult decodeLdpcFromLlr(
            double[] inputLlr,
            LdpcCode code,
            int maxIterations,
            double normalization,
            String decoderType
    ) {
        int n = code.n();
        int k = code.k();
        int[][] checkToVars = code.checkToVars();
        List<EdgeRef>[] varToChecks = code.varToChecks();

        double[] channelLlr = new double[n];
        System.arraycopy(inputLlr, 0, channelLlr, 0, Math.min(inputLlr.length, n));

        double[][] q = new double[checkToVars.length][];
        double[][] r = new double[checkToVars.length][];

        for (int check = 0; check < checkToVars.length; check++) {
            int degree = checkToVars[check].length;
            q[check] = new double[degree];
            r[check] = new double[degree];
            for (int edge = 0; edge < degree; edge++) {
                int variable = checkToVars[check][edge];
                q[check][edge] = channelLlr[variable];
                r[check][edge] = 0.0;
            }
        }

        int[] hard = hardDecision(channelLlr);
        int iterationsUsed = 0;
        boolean success = false;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            iterationsUsed = iteration;

            for (int check = 0; check < q.length; check++) {
                for (int edge = 0; edge < q[check].length; edge++) {
                    r[check][edge] = computeCheckToVarMessage(q[check], edge, decoderType, normalization);
                }
            }

            updateVariableToCheckMessages(channelLlr, q, r, checkToVars, varToChecks);

            double[] posterior = buildPosteriorLlr(channelLlr, r, varToChecks);
            hard = hardDecision(posterior);

            if (isSyndromeZero(hard, checkToVars)) {
                success = true;
                break;
            }
        }

        return new DecodeResult(Arrays.copyOf(hard, k), iterationsUsed == 0 ? maxIterations : iterationsUsed, success);
    }

    private void updateVariableToCheckMessages(
            double[] channelLlr,
            double[][] q,
            double[][] r,
            int[][] checkToVars,
            List<EdgeRef>[] varToChecks
    ) {
        for (int variable = 0; variable < channelLlr.length; variable++) {
            double sumR = 0.0;
            for (EdgeRef ref : varToChecks[variable]) {
                sumR += r[ref.check][ref.edge];
            }

            for (EdgeRef ref : varToChecks[variable]) {
                q[ref.check][ref.edge] = channelLlr[variable] + (sumR - r[ref.check][ref.edge]);
            }
        }
    }

    private double[] buildPosteriorLlr(
            double[] channelLlr,
            double[][] r,
            List<EdgeRef>[] varToChecks
    ) {
        double[] posterior = new double[channelLlr.length];
        for (int variable = 0; variable < channelLlr.length; variable++) {
            double sum = channelLlr[variable];
            for (EdgeRef ref : varToChecks[variable]) {
                sum += r[ref.check][ref.edge];
            }
            posterior[variable] = sum;
        }
        return posterior;
    }

    private boolean isSyndromeZero(int[] hardBits, int[][] checkToVars) {
        for (int[] check : checkToVars) {
            int parity = 0;
            for (int variable : check) {
                parity ^= hardBits[variable];
            }
            if (parity != 0) {
                return false;
            }
        }
        return true;
    }

    private double computeCheckToVarMessage(
            double[] qRow,
            int targetEdge,
            String decoderType,
            double normalization
    ) {
        String mode = decoderType == null ? DECODER_NMS : decoderType;

        if (DECODER_SUM_PRODUCT.equals(mode)) {
            double product = 1.0;
            for (int e = 0; e < qRow.length; e++) {
                if (e == targetEdge) {
                    continue;
                }
                product *= Math.tanh(0.5 * qRow[e]);
            }
            return 2.0 * atanh(clamp(product, -TANH_CLAMP, TANH_CLAMP));
        }

        double sign = 1.0;
        double minAbs = Double.POSITIVE_INFINITY;

        for (int e = 0; e < qRow.length; e++) {
            if (e == targetEdge) {
                continue;
            }
            double value = qRow[e];
            if (value < 0.0) {
                sign = -sign;
            }
            minAbs = Math.min(minAbs, Math.abs(value));
        }

        if (Double.isInfinite(minAbs)) {
            return 0.0;
        }

        if (DECODER_NMS.equals(mode) || (!DECODER_MIN_SUM.equals(mode) && !DECODER_SUM_PRODUCT.equals(mode))) {
            double alpha = clamp(normalization, 0.5, 1.0);
            minAbs *= alpha;
        }

        return sign * minAbs;
    }

    private double atanh(double x) {
        double c = clamp(x, -TANH_CLAMP, TANH_CLAMP);
        return 0.5 * Math.log((1.0 + c) / (1.0 - c));
    }

    private double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
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

    private int[] hardDecision(double[] llr) {
        int[] bits = new int[llr.length];
        for (int i = 0; i < llr.length; i++) {
            bits[i] = llr[i] < 0.0 ? 1 : 0;
        }
        return bits;
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
    private static LdpcCode build5gNrBg2Code(int z) {
        int infoCols = 10;   // BG2 info part
        int rowGroups = 42;  // BG2 rows
        int k = infoCols * z;
        int m = rowGroups * z;

        int[][] shifts = BG_LOADER.loadBg2InfoPartShifts(z, rowGroups, infoCols);

        int[][] taps = new int[m][];
        int rowIndex = 0;

        for (int rg = 0; rg < rowGroups; rg++) {
            for (int localRow = 0; localRow < z; localRow++) {
                List<Integer> vars = new ArrayList<>();
                for (int col = 0; col < infoCols; col++) {
                    int shift = shifts[rg][col];
                    if (shift < 0) continue;
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

        double rate = (double) k / (double) n;
        return new LdpcCode(k, m, n, rate, messageTaps, checkToVars, varToChecks);
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
    private LdpcCodec.Spec resolveNrSpec(SimulationConfig config) {
        int z = config == null ? 8 : config.getLiftingSize();
        String bg = (config == null || config.getNrBaseGraph() == null)
                ? SimulationConfig.NR_BG_AUTO
                : config.getNrBaseGraph();
        String resolvedBg = SimulationConfig.NR_BG2.equals(bg) ? SimulationConfig.NR_BG2 : SimulationConfig.NR_BG1;
        String key = resolvedBg + "-Z" + z;

        return NR_SPEC_CACHE.computeIfAbsent(key, k ->
                REAL_LDPC.buildSpec(
                        SimulationConfig.PROFILE_5GNR_BG1,
                        resolvedBg,
                        z,
                        BG_LOADER
                )
        );
    }

    private record EdgeRef(int check, int edge) {}
    private record LdpcCode(int k, int m, int n, double rate, int[][] messageTaps, int[][] checkToVars, List<EdgeRef>[] varToChecks) {}
    private record PolarCode(int n, int k, double rate, boolean[] infoMask) {}

    public record DecodeResult(int[] decodedInfo, int iterationsUsed, boolean success) {}
    private record TurboCode(
            int k,
            int n,
            double rate,
            int[] interleaver,
            int[] inverseInterleaver,
            int[][] nextState,
            int[][] parityBit
    ) {}

    private record TurboDecodeInternal(double[] llr, double[] extrinsic) {}
    public record ActiveCode(
            boolean polarMode,
            boolean turboMode,
            boolean realNrLdpcMode,
            LdpcCode ldpc,
            PolarCode polar,
            TurboCode turbo,
            LdpcCodec.Spec realSpec,
            int k,
            int n,
            double rate
    ) {}
}