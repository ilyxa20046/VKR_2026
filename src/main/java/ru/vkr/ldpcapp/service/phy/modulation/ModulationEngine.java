package ru.vkr.ldpcapp.service.phy.modulation;

import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.math.Complex;

import java.util.ArrayList;
import java.util.List;

public class ModulationEngine {
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);

    public Constellation buildConstellation(String modulation) {
        if (SimulationConfig.MOD_BPSK.equals(modulation)) {
            return new Constellation(
                    1,
                    List.of(
                            new ConstellationEntry(new int[]{0}, new Complex(1.0, 0.0)),
                            new ConstellationEntry(new int[]{1}, new Complex(-1.0, 0.0))
                    )
            );
        }

        if (SimulationConfig.MOD_QPSK.equals(modulation)) {
            return new Constellation(
                    2,
                    List.of(
                            new ConstellationEntry(new int[]{0, 0}, new Complex(INV_SQRT2, INV_SQRT2)),
                            new ConstellationEntry(new int[]{0, 1}, new Complex(-INV_SQRT2, INV_SQRT2)),
                            new ConstellationEntry(new int[]{1, 1}, new Complex(-INV_SQRT2, -INV_SQRT2)),
                            new ConstellationEntry(new int[]{1, 0}, new Complex(INV_SQRT2, -INV_SQRT2))
                    )
            );
        }

        if (SimulationConfig.MOD_16QAM.equals(modulation)) {
            return buildSquareQamConstellation(2);
        }
        if (SimulationConfig.MOD_64QAM.equals(modulation)) {
            return buildSquareQamConstellation(3);
        }
        if (SimulationConfig.MOD_256QAM.equals(modulation)) {
            return buildSquareQamConstellation(4);
        }

        throw new IllegalArgumentException("Неподдерживаемая модуляция: " + modulation);
    }

    private Constellation buildSquareQamConstellation(int bitsPerAxis) {
        int levels = 1 << bitsPerAxis;
        int bitsPerSymbol = bitsPerAxis * 2;
        double norm = Math.sqrt((2.0 / 3.0) * (levels * levels - 1.0));

        List<ConstellationEntry> entries = new ArrayList<>();
        int combinationsPerAxis = 1 << bitsPerAxis;

        for (int iIndex = 0; iIndex < combinationsPerAxis; iIndex++) {
            int[] iBits = toBits(iIndex, bitsPerAxis);
            int iLevel = grayPamLevel(iBits);

            for (int qIndex = 0; qIndex < combinationsPerAxis; qIndex++) {
                int[] qBits = toBits(qIndex, bitsPerAxis);
                int qLevel = grayPamLevel(qBits);

                int[] bits = new int[bitsPerSymbol];
                System.arraycopy(iBits, 0, bits, 0, bitsPerAxis);
                System.arraycopy(qBits, 0, bits, bitsPerAxis, bitsPerAxis);

                entries.add(new ConstellationEntry(
                        bits,
                        new Complex(iLevel / norm, qLevel / norm)
                ));
            }
        }

        return new Constellation(bitsPerSymbol, entries);
    }

    private int[] toBits(int value, int width) {
        int[] bits = new int[width];
        for (int i = width - 1; i >= 0; i--) {
            bits[i] = value & 1;
            value >>= 1;
        }
        return bits;
    }

    private int grayPamLevel(int[] grayBits) {
        int gray = 0;
        for (int bit : grayBits) {
            gray = (gray << 1) | (bit & 1);
        }

        int binary = 0;
        for (int g = gray; g > 0; g >>= 1) {
            binary ^= g;
        }

        int levels = 1 << grayBits.length;
        return 2 * binary - (levels - 1);
    }

    public static final class ConstellationEntry {
        private final int[] bits;
        private final Complex symbol;

        public ConstellationEntry(int[] bits, Complex symbol) {
            this.bits = bits;
            this.symbol = symbol;
        }

        public int[] bits() {
            return bits;
        }

        public Complex symbol() {
            return symbol;
        }
    }

    public static final class Constellation {
        private final int bitsPerSymbol;
        private final List<ConstellationEntry> entries;

        public Constellation(int bitsPerSymbol, List<ConstellationEntry> entries) {
            this.bitsPerSymbol = bitsPerSymbol;
            this.entries = entries;
        }

        public int bitsPerSymbol() {
            return bitsPerSymbol;
        }

        public List<ConstellationEntry> entries() {
            return entries;
        }

        public Complex lookup(int[] bits) {
            outer:
            for (ConstellationEntry entry : entries) {
                if (entry.bits.length != bits.length) {
                    continue;
                }
                for (int i = 0; i < bits.length; i++) {
                    if (entry.bits[i] != bits[i]) {
                        continue outer;
                    }
                }
                return entry.symbol;
            }
            return entries.get(0).symbol;
        }
    }
}