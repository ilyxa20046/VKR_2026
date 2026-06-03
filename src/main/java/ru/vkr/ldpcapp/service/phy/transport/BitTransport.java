package ru.vkr.ldpcapp.service.phy.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class BitTransport {

    public List<int[]> segmentBits(int[] bits, int segmentSize) {
        if (bits == null || bits.length == 0) {
            return List.of();
        }
        if (segmentSize <= 0) {
            throw new IllegalArgumentException("segmentSize должен быть > 0");
        }

        List<int[]> segments = new ArrayList<>();
        for (int start = 0; start < bits.length; start += segmentSize) {
            int end = Math.min(bits.length, start + segmentSize);
            int[] seg = new int[end - start];
            System.arraycopy(bits, start, seg, 0, seg.length);
            segments.add(seg);
        }
        return segments;
    }

    public int[] concatenateSegments(List<int[]> segments) {
        if (segments == null || segments.isEmpty()) {
            return new int[0];
        }
        int total = 0;
        for (int[] segment : segments) {
            total += segment.length;
        }
        int[] out = new int[total];
        int position = 0;
        for (int[] segment : segments) {
            System.arraycopy(segment, 0, out, position, segment.length);
            position += segment.length;
        }
        return out;
    }

    public int[] padToLength(int[] bits, int targetLength) {
        if (bits.length == targetLength) {
            return bits;
        }
        if (bits.length > targetLength) {
            int[] trimmed = new int[targetLength];
            System.arraycopy(bits, 0, trimmed, 0, targetLength);
            return trimmed;
        }
        int[] padded = new int[targetLength];
        System.arraycopy(bits, 0, padded, 0, bits.length);
        return padded;
    }

    public int[] rateMatchBits(int[] codedBits, int targetLength) {
        if (targetLength <= 0) {
            throw new IllegalArgumentException("targetLength должен быть > 0");
        }
        int[] out = new int[targetLength];
        for (int i = 0; i < targetLength; i++) {
            out[i] = codedBits[i % codedBits.length];
        }
        return out;
    }

    public double[] rateDematchLlr(double[] rmLlr, int originalLength) {
        double[] out = new double[originalLength];
        for (int i = 0; i < rmLlr.length; i++) {
            out[i % originalLength] += rmLlr[i];
        }
        return out;
    }

    public int[] randomPermutation(int n, Random rnd) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        return p;
    }

    public int[] permuteBits(int[] in, int[] p) {
        int[] out = new int[in.length];
        for (int i = 0; i < in.length; i++) out[i] = in[p[i]];
        return out;
    }

    public double[] inversePermuteLlr(double[] in, int[] p) {
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; i++) out[p[i]] = in[i];
        return out;
    }
}