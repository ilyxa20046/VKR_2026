package ru.vkr.ldpcapp.service.phy.crc;

public class CrcEngine {

    public int[] append(int[] bits, int crcBits) {
        if (crcBits == 0) {
            return bits;
        }
        if (crcBits == 16) {
            return appendCrc16(bits);
        }
        throw new IllegalArgumentException("Пока поддерживается только CRC-16, запрошено: " + crcBits);
    }

    public boolean check(int[] bitsWithCrc, int crcBits) {
        if (crcBits == 0) {
            return true;
        }
        if (crcBits == 16) {
            return checkCrc16(bitsWithCrc);
        }
        throw new IllegalArgumentException("Пока поддерживается только CRC-16, запрошено: " + crcBits);
    }

    // CRC-16-CCITT (poly 0x1021), init 0xFFFF
    private int[] appendCrc16(int[] bits) {
        int crc = 0xFFFF;
        for (int bit : bits) {
            int msb = ((crc >>> 15) & 1) ^ (bit & 1);
            crc = ((crc << 1) & 0xFFFF);
            if (msb == 1) {
                crc ^= 0x1021;
            }
        }

        int[] out = new int[bits.length + 16];
        System.arraycopy(bits, 0, out, 0, bits.length);

        // CRC bits MSB->LSB
        for (int i = 0; i < 16; i++) {
            out[bits.length + i] = (crc >>> (15 - i)) & 1;
        }
        return out;
    }

    private boolean checkCrc16(int[] bitsWithCrc) {
        if (bitsWithCrc == null || bitsWithCrc.length < 16) {
            return false;
        }

        int crc = 0xFFFF;
        for (int bit : bitsWithCrc) {
            int msb = ((crc >>> 15) & 1) ^ (bit & 1);
            crc = ((crc << 1) & 0xFFFF);
            if (msb == 1) {
                crc ^= 0x1021;
            }
        }

        // Для корректного CRC остаток равен 0
        return crc == 0;
    }
}