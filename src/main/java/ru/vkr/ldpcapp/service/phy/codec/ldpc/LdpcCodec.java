package ru.vkr.ldpcapp.service.phy.codec.ldpc;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.NrBaseGraphLoader;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class LdpcCodec {

    private static final double NEG_INF = -1e9;

    public record EdgeRef(int check, int edge) {}

    public record Spec(
            int k, int m, int n,
            int[][] checkToVars,
            List<EdgeRef>[] varToChecks,
            BitSet[] transformRows // T = B^{-1}A, row i defines parity bit p_i
    ) {}

    public Spec buildSpec(String profile, String nrBaseGraph, int z, NrBaseGraphLoader loader) {
        if (!SimulationConfig.PROFILE_5GNR_BG1.equals(profile)) {
            throw new IllegalArgumentException("LdpcCodec supports NR profile only in this builder.");
        }

        final boolean bg2 = SimulationConfig.NR_BG2.equals(nrBaseGraph);
        final int infoCols = bg2 ? 10 : 22;
        final int totalCols = bg2 ? 52 : 68;
        final int rowGroups = bg2 ? 42 : 46;

        int[][] shifts = bg2
                ? loader.loadBg2FullShifts(z, rowGroups, totalCols)
                : loader.loadBg1FullShifts(z, rowGroups, totalCols);

        QcExpanded expanded = expandQcToH(shifts, z, infoCols, totalCols);

        // H = [A|B]
        BitSet[] T = precomputeTransform(expanded.A, expanded.B, expanded.m, expanded.k);

        int[][] checkToVars = buildCheckToVars(expanded.H, expanded.m, expanded.n);
        List<EdgeRef>[] varToChecks = buildVarToChecks(checkToVars, expanded.n);

        return new Spec(expanded.k, expanded.m, expanded.n, checkToVars, varToChecks, T);
    }

    public int[] encode(int[] infoBits, Spec spec) {
        if (infoBits.length != spec.k) {
            throw new IllegalArgumentException("infoBits length " + infoBits.length + " != k=" + spec.k);
        }

        int[] parity = new int[spec.m];
        BitSet u = toBitSet(infoBits);

        for (int row = 0; row < spec.m; row++) {
            BitSet tmp = (BitSet) spec.transformRows[row].clone();
            tmp.and(u);
            parity[row] = (tmp.cardinality() & 1);
        }

        int[] codeword = new int[spec.n];
        System.arraycopy(infoBits, 0, codeword, 0, spec.k);
        System.arraycopy(parity, 0, codeword, spec.k, spec.m);
        return codeword;
    }

    public int[] decodeNms(double[] llr, Spec spec, int maxIterations, double normalization) {
        return decodeNmsWithStats(llr, spec, maxIterations, normalization).decodedInfo();
    }

    public DecodeStats decodeNmsWithStats(double[] llr, Spec spec, int maxIterations, double normalization) {
        int n = spec.n;
        int k = spec.k;

        double[][] q = new double[spec.checkToVars.length][];
        double[][] r = new double[spec.checkToVars.length][];

        for (int c = 0; c < spec.checkToVars.length; c++) {
            int deg = spec.checkToVars[c].length;
            q[c] = new double[deg];
            r[c] = new double[deg];
            for (int e = 0; e < deg; e++) {
                int v = spec.checkToVars[c][e];
                q[c][e] = v < llr.length ? llr[v] : 0.0;
            }
        }

        double alpha = Math.max(0.5, Math.min(1.0, normalization));
        int[] hard = new int[n];
        int iterationsUsed = maxIterations;
        boolean success = false;

        for (int it = 0; it < maxIterations; it++) {
            for (int c = 0; c < q.length; c++) {
                double[] qRow = q[c];
                double[] rRow = r[c];

                for (int e = 0; e < qRow.length; e++) {
                    double sign = 1.0;
                    double minAbs = Double.POSITIVE_INFINITY;

                    for (int j = 0; j < qRow.length; j++) {
                        if (j == e) continue;
                        double val = qRow[j];
                        if (val < 0) sign = -sign;
                        minAbs = Math.min(minAbs, Math.abs(val));
                    }

                    rRow[e] = sign * alpha * (Double.isInfinite(minAbs) ? 0.0 : minAbs);
                }
            }

            for (int v = 0; v < n; v++) {
                double sum = v < llr.length ? llr[v] : 0.0;
                for (EdgeRef ref : spec.varToChecks[v]) {
                    sum += r[ref.check][ref.edge];
                }

                hard[v] = sum < 0 ? 1 : 0;

                for (EdgeRef ref : spec.varToChecks[v]) {
                    q[ref.check][ref.edge] = sum - r[ref.check][ref.edge];
                }
            }

            if (isSyndromeZero(hard, spec.checkToVars)) {
                success = true;
                iterationsUsed = it + 1;
                break;
            }
        }

        int[] info = new int[k];
        System.arraycopy(hard, 0, info, 0, k);
        return new DecodeStats(info, iterationsUsed, success);
    }

    private boolean isSyndromeZero(int[] bits, int[][] checkToVars) {
        for (int[] row : checkToVars) {
            int p = 0;
            for (int v : row) p ^= bits[v];
            if (p != 0) return false;
        }
        return true;
    }

    private QcExpanded expandQcToH(int[][] shifts, int z, int infoCols, int totalCols) {
        int rowGroups = shifts.length;
        int m = rowGroups * z;
        int n = totalCols * z;
        int k = infoCols * z;

        BitSet[] H = new BitSet[m];
        BitSet[] A = new BitSet[m];
        BitSet[] B = new BitSet[m];
        for (int r = 0; r < m; r++) {
            H[r] = new BitSet(n);
            A[r] = new BitSet(k);
            B[r] = new BitSet(m);
        }

        for (int rg = 0; rg < rowGroups; rg++) {
            for (int cg = 0; cg < totalCols; cg++) {
                int shift = shifts[rg][cg];
                if (shift < 0) continue;

                for (int i = 0; i < z; i++) {
                    int row = rg * z + i;
                    int col = cg * z + ((i + shift) % z);

                    H[row].set(col);
                    if (col < k) A[row].set(col);
                    else B[row].set(col - k);
                }
            }
        }

        return new QcExpanded(H, A, B, k, m, n);
    }

    private BitSet[] precomputeTransform(BitSet[] A, BitSet[] B, int m, int k) {
        BitSet[] left = new BitSet[m];
        BitSet[] right = new BitSet[m];
        for (int i = 0; i < m; i++) {
            left[i] = (BitSet) B[i].clone();
            right[i] = (BitSet) A[i].clone();
        }

        int pivotRow = 0;
        for (int col = 0; col < m && pivotRow < m; col++) {
            int sel = -1;
            for (int r = pivotRow; r < m; r++) {
                if (left[r].get(col)) {
                    sel = r;
                    break;
                }
            }
            if (sel < 0) continue;

            if (sel != pivotRow) {
                BitSet t1 = left[sel]; left[sel] = left[pivotRow]; left[pivotRow] = t1;
                BitSet t2 = right[sel]; right[sel] = right[pivotRow]; right[pivotRow] = t2;
            }

            for (int r = 0; r < m; r++) {
                if (r == pivotRow) continue;
                if (left[r].get(col)) {
                    left[r].xor(left[pivotRow]);
                    right[r].xor(right[pivotRow]);
                }
            }

            pivotRow++;
        }

        // left should be I for invertible B
        for (int i = 0; i < m; i++) {
            if (!left[i].get(i)) {
                throw new IllegalStateException("Parity sub-matrix B is singular in GF(2).");
            }
        }

        BitSet[] T = new BitSet[m];
        for (int i = 0; i < m; i++) {
            T[i] = right[i];
            if (T[i].length() > k) {
                T[i].clear(k, T[i].length());
            }
        }
        return T;
    }

    private int[][] buildCheckToVars(BitSet[] H, int m, int n) {
        int[][] rows = new int[m][];
        for (int r = 0; r < m; r++) {
            List<Integer> vars = new ArrayList<>();
            for (int c = H[r].nextSetBit(0); c >= 0; c = H[r].nextSetBit(c + 1)) {
                vars.add(c);
            }
            rows[r] = vars.stream().mapToInt(Integer::intValue).toArray();
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private List<EdgeRef>[] buildVarToChecks(int[][] checkToVars, int n) {
        List<EdgeRef>[] varToChecks = (List<EdgeRef>[]) new List<?>[n];
        for (int i = 0; i < n; i++) varToChecks[i] = new ArrayList<>();

        for (int c = 0; c < checkToVars.length; c++) {
            for (int e = 0; e < checkToVars[c].length; e++) {
                int v = checkToVars[c][e];
                varToChecks[v].add(new EdgeRef(c, e));
            }
        }
        return varToChecks;
    }

    private BitSet toBitSet(int[] bits) {
        BitSet bs = new BitSet(bits.length);
        for (int i = 0; i < bits.length; i++) {
            if ((bits[i] & 1) == 1) bs.set(i);
        }
        return bs;
    }
    public record DecodeStats(int[] decodedInfo, int iterationsUsed, boolean success) {}
    private record QcExpanded(BitSet[] H, BitSet[] A, BitSet[] B, int k, int m, int n) {}
}