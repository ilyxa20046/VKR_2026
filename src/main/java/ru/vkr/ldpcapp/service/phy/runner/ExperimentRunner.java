package ru.vkr.ldpcapp.service.phy.runner;

import ru.vkr.ldpcapp.model.ResultPoint;
import ru.vkr.ldpcapp.model.SimulationConfig;
import ru.vkr.ldpcapp.service.phy.channel.ChannelEngine;
import ru.vkr.ldpcapp.service.phy.codec.CodecEngine;
import ru.vkr.ldpcapp.service.phy.crc.CrcEngine;
import ru.vkr.ldpcapp.service.phy.metrics.PhyMetricsEngine;
import ru.vkr.ldpcapp.service.phy.stats.StatsMath;
import ru.vkr.ldpcapp.service.phy.transport.BitTransport;
import ru.vkr.ldpcapp.service.config.SimulationConfigFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExperimentRunner {
    private final CrcEngine crcEngine;
    private final BitTransport bitTransport;
    private final StatsMath statsMath;
    private final ChannelEngine channelEngine;
    private final CodecEngine codecEngine;
    private final PhyMetricsEngine phyMetricsEngine;

    public ExperimentRunner(
            CrcEngine crcEngine,
            BitTransport bitTransport,
            StatsMath statsMath,
            ChannelEngine channelEngine,
            CodecEngine codecEngine,
            PhyMetricsEngine phyMetricsEngine
    ) {
        this.crcEngine = crcEngine;
        this.bitTransport = bitTransport;
        this.statsMath = statsMath;
        this.channelEngine = channelEngine;
        this.codecEngine = codecEngine;
        this.phyMetricsEngine = phyMetricsEngine;
    }

    public ResultPoint simulatePoint(double snrDb, SimulationConfig config, int index) {
        CodecEngine.ActiveCode activeCode = codecEngine.resolveActiveCode(config);
        int codeK = activeCode.k();
        int codeN = activeCode.n();
        double codeRate = SimulationConfigFactory.getEffectiveCodeRate(config);

        Random uncodedRandom = new Random(config.getSeed() + 1009L * (index + 1));
        Random codedRandom = new Random(config.getSeed() + 5003L * (index + 1));

        double ebN0UncodedDb = phyMetricsEngine.toEbN0Db(
                snrDb,
                config.getSnrDomain(),
                config.getModulation(),
                1.0
        );

        double ebN0CodedDb = phyMetricsEngine.toEbN0Db(
                snrDb,
                config.getSnrDomain(),
                config.getModulation(),
                codeRate
        );

        double sigmaUncoded = phyMetricsEngine.sigmaFromEbN0(ebN0UncodedDb, config.getModulation(), 1.0);
        double sigmaCoded = phyMetricsEngine.sigmaFromEbN0(ebN0CodedDb, config.getModulation(), codeRate);

        int codewordsPerBlock = config.getInfoBlockLength() / codeK;
        int targetBlocks = config.isAdaptiveStopEnabled() ? config.getMaxBlocksPerSnr() : config.getBlocks();

        boolean crcEnabled = config.isCrcEnabled();
        int crcBits = config.getCrcBits();
        boolean segmentationEnabled = config.isSegmentationEnabled();
        boolean rateMatchingEnabled = config.isRateMatchingEnabled();
        boolean blerByCrc = SimulationConfig.BLER_BY_CRC_FAIL.equals(config.getBlerCriterion());

        boolean harqEnabled = config.isHarqEnabled();
        int harqMaxRetx = Math.max(0, config.getHarqMaxRetx());

        int uncodedBitErrors = 0;
        int uncodedBlockErrors = 0;
        int codedBitErrors = 0;
        int codedBlockErrors = 0;
        double iterationsSum = 0.0;
        int successfulCodewords = 0;
        int processedBlocks = 0;

        long totalHarqExtraTx = 0L;       // дополнительные передачи сверх первой
        long totalHarqSegments = 0L;      // число сегментов (codewords)
        long totalHarqSuccessSegments = 0L;

        for (int block = 0; block < targetBlocks; block++) {
            processedBlocks++;

            BlockStat uncodedResult = simulateUncodedBlock(config, sigmaUncoded, uncodedRandom);
            uncodedBitErrors += uncodedResult.bitErrors;
            if (uncodedResult.blockError) {
                uncodedBlockErrors++;
            }

            boolean blockHasBitMismatch = false;
            int[] sourceInfoBlock = phyMetricsEngine.randomBits(config.getInfoBlockLength(), codedRandom);
            int[] transportBlock = crcEnabled ? crcEngine.append(sourceInfoBlock, crcBits) : sourceInfoBlock;

            List<int[]> segments = segmentationEnabled
                    ? bitTransport.segmentBits(transportBlock, codeK)
                    : List.of(transportBlock);

            List<int[]> decodedSegments = new ArrayList<>(segments.size());

            for (int[] rawSegment : segments) {
                int[] info = bitTransport.padToLength(rawSegment, codeK);
                int[] encoded = codecEngine.encode(info, activeCode);

                int targetE = config.getTargetCodewordLength() > 0 ? config.getTargetCodewordLength() : encoded.length;
                int[] txBitsBase = rateMatchingEnabled ? bitTransport.rateMatchBits(encoded, targetE) : encoded;

                double[] llrAccum = null;
                CodecEngine.DecodeResult lastDecoded = null;
                boolean decodedSuccess = false;
                int usedExtraTx = 0;

                int attempts = harqEnabled ? (harqMaxRetx + 1) : 1;

                for (int txAttempt = 0; txAttempt < attempts; txAttempt++) {
                    ChannelEngine.Transmission transmission =
                            channelEngine.transmitBits(txBitsBase, config, sigmaCoded, codedRandom);

                    double[] llrTx = channelEngine.demapToLlr(transmission, sigmaCoded, config);
                    double[] llr = rateMatchingEnabled ? bitTransport.rateDematchLlr(llrTx, codeN) : llrTx;

                    if (llrAccum == null) {
                        llrAccum = llr.clone();
                    } else {
                        for (int i = 0; i < llrAccum.length; i++) {
                            llrAccum[i] += llr[i]; // HARQ Chase Combining
                        }
                        usedExtraTx++;
                    }

                    lastDecoded = codecEngine.decodeFromLlr(
                            llrAccum,
                            activeCode,
                            config.getMaxIterations(),
                            config.getNormalization(),
                            config.getDecoderType()
                    );

                    iterationsSum += lastDecoded.iterationsUsed();

                    if (lastDecoded.success()) {
                        decodedSuccess = true;
                        break;
                    }
                }

                totalHarqSegments++;
                totalHarqExtraTx += usedExtraTx;
                if (decodedSuccess) {
                    totalHarqSuccessSegments++;
                    successfulCodewords++;
                }

                // lastDecoded гарантированно не null (минимум 1 попытка)
                int compareLength = Math.min(rawSegment.length, lastDecoded.decodedInfo().length);
                int[] decodedSegment = new int[compareLength];
                System.arraycopy(lastDecoded.decodedInfo(), 0, decodedSegment, 0, compareLength);
                decodedSegments.add(decodedSegment);

                for (int i = 0; i < compareLength; i++) {
                    if (decodedSegment[i] != rawSegment[i]) {
                        codedBitErrors++;
                        blockHasBitMismatch = true;
                    }
                }
            }

            boolean blockHasCrcFail = false;
            if (crcEnabled) {
                int[] decodedTransportBlock = bitTransport.concatenateSegments(decodedSegments);
                blockHasCrcFail = !crcEngine.check(decodedTransportBlock, crcBits);
            }

            boolean blockHasError = blerByCrc && crcEnabled ? blockHasCrcFail : blockHasBitMismatch;
            if (blockHasError) {
                codedBlockErrors++;
            }
        }

        int totalBlocks = Math.max(1, processedBlocks);
        int totalBits = totalBlocks * config.getInfoBlockLength();
        int totalCodewords = Math.max(1, totalBlocks * codewordsPerBlock);

        double berLdpc = statsMath.safeDivide(codedBitErrors, totalBits);
        double blerLdpc = statsMath.safeDivide(codedBlockErrors, totalBlocks);
        double averageIterations = statsMath.safeDivide(iterationsSum, totalCodewords);
        double successRatio = 1.0 - blerLdpc;

        double spectralEfficiency = phyMetricsEngine.estimateSpectralEfficiency(config, codeRate, blerLdpc);
        double throughputMbps = phyMetricsEngine.estimateThroughputMbps(config, codeRate, blerLdpc);

        StatsMath.Interval berCi = statsMath.wilsonInterval(codedBitErrors, totalBits, config.getConfidenceLevel());
        StatsMath.Interval blerCi = statsMath.wilsonInterval(codedBlockErrors, totalBlocks, config.getConfidenceLevel());

        double averageRetx = totalHarqSegments == 0 ? 0.0 : (double) totalHarqExtraTx / totalHarqSegments;
        double harqSuccessRatio = totalHarqSegments == 0 ? 0.0 : (double) totalHarqSuccessSegments / totalHarqSegments;

        return new ResultPoint(
                snrDb,
                statsMath.safeDivide(uncodedBitErrors, totalBits),
                berLdpc,
                statsMath.safeDivide(uncodedBlockErrors, totalBlocks),
                blerLdpc,
                averageIterations,
                successRatio,
                throughputMbps,
                spectralEfficiency,
                berCi.low(),
                berCi.high(),
                blerCi.low(),
                blerCi.high(),
                codedBitErrors,
                codedBlockErrors,
                totalBits,
                totalBlocks,
                config.getConfidenceLevel(),
                averageRetx,
                harqSuccessRatio
        );
    }

    private BlockStat simulateUncodedBlock(SimulationConfig config, double sigma, Random random) {
        int[] info = phyMetricsEngine.randomBits(config.getInfoBlockLength(), random);
        ChannelEngine.Transmission transmission = channelEngine.transmitBits(info, config, sigma, random);
        double[] llr = channelEngine.demapToLlr(transmission, sigma, config);

        int bitErrors = 0;
        for (int i = 0; i < config.getInfoBlockLength(); i++) {
            int hard = llr[i] < 0.0 ? 1 : 0;
            if (hard != info[i]) {
                bitErrors++;
            }
        }
        return new BlockStat(bitErrors, bitErrors > 0);
    }

    private static class BlockStat {
        private final int bitErrors;
        private final boolean blockError;

        private BlockStat(int bitErrors, boolean blockError) {
            this.bitErrors = bitErrors;
            this.blockError = blockError;
        }
    }
}