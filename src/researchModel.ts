export type Complex = {
  re: number;
  im: number;
};

export type ModulationName = "BPSK" | "QPSK" | "16-QAM";
export type ChannelName = "AWGN" | "Rayleigh";
export type CodeProfileId = "edu-24-12" | "qcldpc-96-48";
export type WaveformName = "Single-carrier" | "OFDM-64" | "OFDM-128";
export type SpatialModeName = "SISO" | "2x2 Diversity";
export type TargetMetric = "BER" | "BLER";

export type EdgeRef = {
  check: number;
  edge: number;
};

export type LdpcCode = {
  id: CodeProfileId;
  name: string;
  k: number;
  m: number;
  n: number;
  rate: number;
  family: "Educational" | "QC-Inspired";
  description: string;
  messageTaps: number[][];
  checkToVars: number[][];
  varToChecks: EdgeRef[][];
};

export type ResearchSettings = {
  infoBlockLength: number;
  blocks: number;
  seed: number;
  maxIterations: number;
  normalization: number;
  modulation: ModulationName;
  channel: ChannelName;
  codeProfile: CodeProfileId;
  waveform: WaveformName;
  spatialMode: SpatialModeName;
};

export type ResearchPoint = {
  snr: number;
  berUncoded: number;
  berCoded: number;
  blerUncoded: number;
  blerCoded: number;
  averageIterations: number;
  successfulCodewords: number;
  effectiveThroughputMbps: number;
  spectralEfficiency: number;
};

export type ThresholdEstimate = {
  reached: boolean;
  snr: number | null;
  note: string;
};

export type EnergyGainEstimate = {
  metric: TargetMetric;
  target: number;
  uncoded: ThresholdEstimate;
  coded: ThresholdEstimate;
  gainDb: number | null;
  note: string;
};

export type ResearchSummary = {
  bestBerGain: { value: number; point: ResearchPoint };
  bestBlerGain: { value: number; point: ResearchPoint };
  averageIterations: number;
  successRatio: number;
  minCodedBer: number;
  minCodedBler: number;
  averageThroughputMbps: number;
  peakThroughputMbps: number;
  averageSpectralEfficiency: number;
  peakSpectralEfficiency: number;
  requiredSnrBer: number | null;
  requiredSnrBler: number | null;
  berEnergyGain: EnergyGainEstimate;
  blerEnergyGain: EnergyGainEstimate;
};

type ConstellationEntry = {
  bits: number[];
  symbol: Complex;
};

type Constellation = {
  bitsPerSymbol: number;
  entries: ConstellationEntry[];
  symbolMap: Record<string, Complex>;
};

type BaseConnection = {
  group: number;
  shift: number;
};

type WaveformProfile = {
  id: WaveformName;
  usefulFraction: number;
  fadingSpan: number;
  description: string;
};

type SpatialProfile = {
  id: SpatialModeName;
  branches: number;
  description: string;
};

const complex = (re: number, im: number): Complex => ({ re, im });
const ONE = complex(1, 0);
const INV_SQRT2 = 1 / Math.sqrt(2);
const INV_SQRT10 = 1 / Math.sqrt(10);
const BASE_SYMBOL_RATE_MBAUD = 20;

export const modulationNotes: Record<ModulationName, string> = {
  BPSK: "Самая устойчивая базовая модуляция; удобна как эталон для сравнения более сложных режимов.",
  QPSK: "Компромисс между помехоустойчивостью и спектральной эффективностью; хороший базовый режим для 5G-like моделирования.",
  "16-QAM": "Даёт более высокую спектральную эффективность, но требует большего SNR и сильнее страдает от fading-канала.",
};

export const channelNotes: Record<ChannelName, string> = {
  AWGN: "Базовый канал с аддитивным белым гауссовским шумом; нужен для эталонного анализа BER и BLER.",
  Rayleigh: "Канал с замираниями, типичный для мобильной радиосвязи без LOS-компоненты; лучше раскрывает полезность LDPC, OFDM и diversity.",
};

export const waveformNotes: Record<WaveformName, string> = {
  "Single-carrier": "Однонесущий режим. В Rayleigh-модели используется более длинная fading-корреляция, поэтому замирания действуют тяжелее на целые участки блока.",
  "OFDM-64": "Учебный OFDM-like режим с 64 поднесущими, cyclic prefix и pilot-overhead. Лучше отражает идею 5G-радиоинтерфейса, но уменьшает полезную долю ресурса.",
  "OFDM-128": "Более ёмкий OFDM-like режим с 128 поднесущими и меньшим относительным overhead, что делает его ближе к практическому broadband-сценарию.",
};

export const spatialNotes: Record<SpatialModeName, string> = {
  SISO: "Обычная передача через один пространственный канал без diversity-комбинирования.",
  "2x2 Diversity": "Упрощённый 2x2 diversity / Alamouti-like режим: полезен для исследования выигрыша по устойчивости без перехода к spatial multiplexing.",
};

function mulberry32(seed: number) {
  let state = seed >>> 0;
  return function random() {
    state += 0x6d2b79f5;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function gaussian(random: () => number) {
  let u = 0;
  let v = 0;
  while (u === 0) u = random();
  while (v === 0) v = random();
  return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
}

function add(a: Complex, b: Complex): Complex {
  return complex(a.re + b.re, a.im + b.im);
}

function sub(a: Complex, b: Complex): Complex {
  return complex(a.re - b.re, a.im - b.im);
}

function mul(a: Complex, b: Complex): Complex {
  return complex(a.re * b.re - a.im * b.im, a.re * b.im + a.im * b.re);
}

function abs2(a: Complex) {
  return a.re * a.re + a.im * a.im;
}

function gray16QamLevel(b0: number, b1: number) {
  if (b0 === 0 && b1 === 0) return -3;
  if (b0 === 0 && b1 === 1) return -1;
  if (b0 === 1 && b1 === 1) return 1;
  return 3;
}

function buildConstellation(modulation: ModulationName): Constellation {
  let entries: ConstellationEntry[];

  if (modulation === "BPSK") {
    entries = [
      { bits: [0], symbol: complex(1, 0) },
      { bits: [1], symbol: complex(-1, 0) },
    ];
  } else if (modulation === "QPSK") {
    entries = [
      { bits: [0, 0], symbol: complex(INV_SQRT2, INV_SQRT2) },
      { bits: [0, 1], symbol: complex(-INV_SQRT2, INV_SQRT2) },
      { bits: [1, 1], symbol: complex(-INV_SQRT2, -INV_SQRT2) },
      { bits: [1, 0], symbol: complex(INV_SQRT2, -INV_SQRT2) },
    ];
  } else {
    entries = [];
    const bitPairs = [
      [0, 0],
      [0, 1],
      [1, 1],
      [1, 0],
    ];

    for (const iBits of bitPairs) {
      for (const qBits of bitPairs) {
        entries.push({
          bits: [...iBits, ...qBits],
          symbol: complex(gray16QamLevel(iBits[0], iBits[1]) * INV_SQRT10, gray16QamLevel(qBits[0], qBits[1]) * INV_SQRT10),
        });
      }
    }
  }

  const symbolMap = Object.fromEntries(entries.map((entry) => [entry.bits.join(""), entry.symbol]));
  return {
    bitsPerSymbol: entries[0].bits.length,
    entries,
    symbolMap,
  };
}

const CONSTELLATIONS: Record<ModulationName, Constellation> = {
  BPSK: buildConstellation("BPSK"),
  QPSK: buildConstellation("QPSK"),
  "16-QAM": buildConstellation("16-QAM"),
};

const WAVEFORM_PROFILES: Record<WaveformName, WaveformProfile> = {
  "Single-carrier": {
    id: "Single-carrier",
    usefulFraction: 1,
    fadingSpan: 12,
    description: waveformNotes["Single-carrier"],
  },
  "OFDM-64": {
    id: "OFDM-64",
    usefulFraction: (64 / (64 + 8)) * 0.9,
    fadingSpan: 1,
    description: waveformNotes["OFDM-64"],
  },
  "OFDM-128": {
    id: "OFDM-128",
    usefulFraction: (128 / (128 + 8)) * 0.93,
    fadingSpan: 1,
    description: waveformNotes["OFDM-128"],
  },
};

const SPATIAL_PROFILES: Record<SpatialModeName, SpatialProfile> = {
  SISO: {
    id: "SISO",
    branches: 1,
    description: spatialNotes.SISO,
  },
  "2x2 Diversity": {
    id: "2x2 Diversity",
    branches: 4,
    description: spatialNotes["2x2 Diversity"],
  },
};

function createSystematicSparseCode(
  id: CodeProfileId,
  name: string,
  family: "Educational" | "QC-Inspired",
  description: string,
  k: number,
  m: number,
  messageTaps: number[][],
): LdpcCode {
  const n = k + m;
  const checkToVars = Array.from({ length: m }, (_, row) => {
    const vars = [...messageTaps[row], k + row];
    if (row > 0) vars.push(k + row - 1);
    return vars;
  });

  const varToChecks: EdgeRef[][] = Array.from({ length: n }, () => []);
  checkToVars.forEach((vars, check) => {
    vars.forEach((variable, edge) => {
      varToChecks[variable].push({ check, edge });
    });
  });

  return {
    id,
    name,
    k,
    m,
    n,
    rate: k / n,
    family,
    description,
    messageTaps,
    checkToVars,
    varToChecks,
  };
}

function buildEducationalCode() {
  const k = 12;
  const m = 12;
  const offsets = [0, 3, 7];
  const messageTaps = Array.from({ length: m }, (_, row) => offsets.map((offset) => (row + offset) % k));

  return createSystematicSparseCode(
    "edu-24-12",
    "Учебный LDPC (24,12)",
    "Educational",
    "Компактный систематический LDPC-код со скоростью 1/2. Подходит для базового исследования и быстрой калибровки модели.",
    k,
    m,
    messageTaps,
  );
}

function buildQcInspiredCode() {
  const z = 8;
  const k = z * 6;
  const m = z * 6;

  const baseGraph: BaseConnection[][] = [
    [
      { group: 0, shift: 0 },
      { group: 1, shift: 1 },
      { group: 3, shift: 2 },
      { group: 5, shift: 5 },
    ],
    [
      { group: 0, shift: 3 },
      { group: 2, shift: 0 },
      { group: 4, shift: 4 },
      { group: 5, shift: 6 },
    ],
    [
      { group: 1, shift: 2 },
      { group: 2, shift: 5 },
      { group: 3, shift: 1 },
      { group: 4, shift: 7 },
    ],
    [
      { group: 0, shift: 6 },
      { group: 2, shift: 1 },
      { group: 4, shift: 3 },
      { group: 5, shift: 0 },
    ],
    [
      { group: 1, shift: 4 },
      { group: 3, shift: 6 },
      { group: 4, shift: 2 },
      { group: 5, shift: 1 },
    ],
    [
      { group: 0, shift: 5 },
      { group: 1, shift: 7 },
      { group: 2, shift: 4 },
      { group: 3, shift: 0 },
    ],
  ];

  const messageTaps: number[][] = [];
  for (let groupRow = 0; groupRow < baseGraph.length; groupRow += 1) {
    for (let localRow = 0; localRow < z; localRow += 1) {
      messageTaps.push(
        baseGraph[groupRow].map((connection) => connection.group * z + ((localRow + connection.shift) % z)),
      );
    }
  }

  return createSystematicSparseCode(
    "qcldpc-96-48",
    "QC-inspired LDPC (96,48)",
    "QC-Inspired",
    "Более длинный квазициркулянтный LDPC-профиль со скоростью 1/2. Это не полный 3GPP NR-код, но он ближе к 5G-подходу по структуре, чем компактный учебный вариант.",
    k,
    m,
    messageTaps,
  );
}

export const CODE_PROFILES: Record<CodeProfileId, LdpcCode> = {
  "edu-24-12": buildEducationalCode(),
  "qcldpc-96-48": buildQcInspiredCode(),
};

export function getCodeProfile(id: CodeProfileId) {
  return CODE_PROFILES[id];
}

export function normalizeInfoBlockLength(value: number, codeProfile: CodeProfileId) {
  const k = getCodeProfile(codeProfile).k;
  const safe = Math.max(k, Math.round(value));
  return Math.max(k, Math.ceil(safe / k) * k);
}

export function buildSnrValues(start: number, end: number, step: number) {
  const min = Math.min(start, end);
  const max = Math.max(start, end);
  const safeStep = Math.max(0.5, step);
  const values: number[] = [];

  for (let current = min; current <= max + 1e-9; current += safeStep) {
    values.push(Number(current.toFixed(2)));
  }

  return values;
}

function sigmaFromEbN0(snrDb: number, modulation: ModulationName, rate: number) {
  const ebN0 = 10 ** (snrDb / 10);
  const bitsPerSymbol = CONSTELLATIONS[modulation].bitsPerSymbol;
  return Math.sqrt(1 / (2 * bitsPerSymbol * rate * ebN0));
}

function randomBits(length: number, random: () => number) {
  return Array.from({ length }, () => (random() >= 0.5 ? 1 : 0));
}

function modulateBits(bits: number[], modulation: ModulationName) {
  const constellation = CONSTELLATIONS[modulation];
  const padded = bits.slice();
  while (padded.length % constellation.bitsPerSymbol !== 0) padded.push(0);

  const symbols: Complex[] = [];
  for (let i = 0; i < padded.length; i += constellation.bitsPerSymbol) {
    const key = padded.slice(i, i + constellation.bitsPerSymbol).join("");
    symbols.push(constellation.symbolMap[key]);
  }

  return { symbols, paddedLength: padded.length };
}

function rayleigh(random: () => number) {
  return complex(gaussian(random) * INV_SQRT2, gaussian(random) * INV_SQRT2);
}

function transmit(
  symbols: Complex[],
  sigma: number,
  channel: ChannelName,
  waveform: WaveformName,
  spatialMode: SpatialModeName,
  random: () => number,
) {
  const waveformProfile = WAVEFORM_PROFILES[waveform];
  const spatialProfile = SPATIAL_PROFILES[spatialMode];
  const received: Complex[][] = [];
  const gains: Complex[][] = [];
  let currentBlockGains = Array.from({ length: spatialProfile.branches }, () => ONE);
  let remaining = 0;

  for (const symbol of symbols) {
    if (channel === "Rayleigh" && waveform === "Single-carrier" && remaining <= 0) {
      currentBlockGains = Array.from({ length: spatialProfile.branches }, () => rayleigh(random));
      remaining = waveformProfile.fadingSpan;
    }

    const rxBranches: Complex[] = [];
    const gainBranches: Complex[] = [];

    for (let branch = 0; branch < spatialProfile.branches; branch += 1) {
      const h =
        channel === "AWGN"
          ? ONE
          : waveform === "Single-carrier"
            ? currentBlockGains[branch]
            : rayleigh(random);
      const noise = complex(sigma * gaussian(random), sigma * gaussian(random));
      rxBranches.push(add(mul(h, symbol), noise));
      gainBranches.push(h);
    }

    if (channel === "Rayleigh" && waveform === "Single-carrier") remaining -= 1;
    received.push(rxBranches);
    gains.push(gainBranches);
  }

  return { received, gains };
}

function demapToLlr(received: Complex[][], gains: Complex[][], sigma: number, modulation: ModulationName) {
  const constellation = CONSTELLATIONS[modulation];
  const noiseVariance = 2 * sigma * sigma;
  const llr: number[] = [];

  for (let symbolIndex = 0; symbolIndex < received.length; symbolIndex += 1) {
    const yBranches = received[symbolIndex];
    const hBranches = gains[symbolIndex];

    for (let bitIndex = 0; bitIndex < constellation.bitsPerSymbol; bitIndex += 1) {
      let min0 = Number.POSITIVE_INFINITY;
      let min1 = Number.POSITIVE_INFINITY;

      for (const entry of constellation.entries) {
        let distance = 0;
        for (let branch = 0; branch < yBranches.length; branch += 1) {
          const diff = sub(yBranches[branch], mul(hBranches[branch], entry.symbol));
          distance += abs2(diff);
        }

        if (entry.bits[bitIndex] === 0) {
          min0 = Math.min(min0, distance);
        } else {
          min1 = Math.min(min1, distance);
        }
      }

      llr.push((min1 - min0) / noiseVariance);
    }
  }

  return llr;
}

export function encodeLdpc(infoBits: number[], code: LdpcCode) {
  const parityBits = new Array(code.m).fill(0);

  for (let row = 0; row < code.m; row += 1) {
    let parityEquation = 0;
    for (const column of code.messageTaps[row]) {
      parityEquation ^= infoBits[column];
    }
    parityBits[row] = parityEquation ^ (row > 0 ? parityBits[row - 1] : 0);
  }

  return [...infoBits, ...parityBits];
}

function checkSyndrome(bits: number[], code: LdpcCode) {
  for (let check = 0; check < code.checkToVars.length; check += 1) {
    let syndrome = 0;
    for (const variable of code.checkToVars[check]) syndrome ^= bits[variable];
    if (syndrome !== 0) return false;
  }
  return true;
}

export function decodeLdpcFromLlr(llr: number[], maxIterations: number, normalization: number, code: LdpcCode) {
  const channelLlr = llr.slice();
  const q = code.checkToVars.map((vars) => vars.map((variable) => channelLlr[variable]));
  const r = code.checkToVars.map((vars) => vars.map(() => 0));
  let hard = channelLlr.map((value) => (value < 0 ? 1 : 0));

  for (let iteration = 1; iteration <= maxIterations; iteration += 1) {
    for (let check = 0; check < code.checkToVars.length; check += 1) {
      const messages = q[check];
      let signProduct = 1;
      let min1 = Number.POSITIVE_INFINITY;
      let min2 = Number.POSITIVE_INFINITY;
      let minIndex = -1;

      for (let edge = 0; edge < messages.length; edge += 1) {
        const value = messages[edge];
        const sign = value < 0 ? -1 : 1;
        const absolute = Math.abs(value);
        signProduct *= sign;

        if (absolute < min1) {
          min2 = min1;
          min1 = absolute;
          minIndex = edge;
        } else if (absolute < min2) {
          min2 = absolute;
        }
      }

      for (let edge = 0; edge < messages.length; edge += 1) {
        const sign = messages[edge] < 0 ? -1 : 1;
        const extrinsicSign = signProduct * sign;
        const minimum = edge === minIndex ? min2 : min1;
        r[check][edge] = normalization * extrinsicSign * minimum;
      }
    }

    const posterior = channelLlr.slice();
    for (let variable = 0; variable < code.n; variable += 1) {
      for (const edgeRef of code.varToChecks[variable]) posterior[variable] += r[edgeRef.check][edgeRef.edge];
    }

    hard = posterior.map((value) => (value < 0 ? 1 : 0));
    if (checkSyndrome(hard, code)) {
      return {
        decoded: hard.slice(0, code.k),
        iterationsUsed: iteration,
        success: true,
      };
    }

    for (let variable = 0; variable < code.n; variable += 1) {
      for (const edgeRef of code.varToChecks[variable]) {
        q[edgeRef.check][edgeRef.edge] = posterior[variable] - r[edgeRef.check][edgeRef.edge];
      }
    }
  }

  return {
    decoded: hard.slice(0, code.k),
    iterationsUsed: maxIterations,
    success: checkSyndrome(hard, code),
  };
}

function simulateUncodedBlock(settings: ResearchSettings, sigma: number, random: () => number) {
  const info = randomBits(settings.infoBlockLength, random);
  const { symbols } = modulateBits(info, settings.modulation);
  const { received, gains } = transmit(symbols, sigma, settings.channel, settings.waveform, settings.spatialMode, random);
  const llr = demapToLlr(received, gains, sigma, settings.modulation).slice(0, settings.infoBlockLength);
  const hard = llr.map((value) => (value < 0 ? 1 : 0));

  let bitErrors = 0;
  for (let i = 0; i < settings.infoBlockLength; i += 1) {
    if (hard[i] !== info[i]) bitErrors += 1;
  }

  return {
    bitErrors,
    blockError: bitErrors > 0,
  };
}

function simulateCodedBlock(settings: ResearchSettings, code: LdpcCode, sigma: number, random: () => number) {
  const codewordsPerBlock = settings.infoBlockLength / code.k;
  let bitErrors = 0;
  let blockError = false;
  let iterationsSum = 0;
  let successCount = 0;

  for (let word = 0; word < codewordsPerBlock; word += 1) {
    const info = randomBits(code.k, random);
    const encoded = encodeLdpc(info, code);
    const { symbols } = modulateBits(encoded, settings.modulation);
    const { received, gains } = transmit(symbols, sigma, settings.channel, settings.waveform, settings.spatialMode, random);
    const llr = demapToLlr(received, gains, sigma, settings.modulation).slice(0, code.n);
    const decoded = decodeLdpcFromLlr(llr, settings.maxIterations, settings.normalization, code);

    iterationsSum += decoded.iterationsUsed;
    successCount += decoded.success ? 1 : 0;

    for (let i = 0; i < code.k; i += 1) {
      if (decoded.decoded[i] !== info[i]) {
        bitErrors += 1;
        blockError = true;
      }
    }
  }

  return {
    bitErrors,
    blockError,
    iterationsSum,
    successCount,
    codewordsPerBlock,
  };
}

function estimateThroughput(settings: ResearchSettings, bler: number, code: LdpcCode) {
  const bitsPerSymbol = CONSTELLATIONS[settings.modulation].bitsPerSymbol;
  const usefulFraction = WAVEFORM_PROFILES[settings.waveform].usefulFraction;
  const successFactor = Math.max(0, 1 - bler);
  return BASE_SYMBOL_RATE_MBAUD * bitsPerSymbol * code.rate * usefulFraction * successFactor;
}

function estimateSpectralEfficiency(settings: ResearchSettings, bler: number, code: LdpcCode) {
  const bitsPerSymbol = CONSTELLATIONS[settings.modulation].bitsPerSymbol;
  const usefulFraction = WAVEFORM_PROFILES[settings.waveform].usefulFraction;
  const successFactor = Math.max(0, 1 - bler);
  return bitsPerSymbol * code.rate * usefulFraction * successFactor;
}

export function simulateResearchPoint(snrDb: number, settings: ResearchSettings, index: number): ResearchPoint {
  const code = getCodeProfile(settings.codeProfile);
  const sigmaUncoded = sigmaFromEbN0(snrDb, settings.modulation, 1);
  const sigmaCoded = sigmaFromEbN0(snrDb, settings.modulation, code.rate);
  const uncodedRandom = mulberry32(settings.seed + 1009 * (index + 1));
  const codedRandom = mulberry32(settings.seed + 5003 * (index + 1));

  let uncodedBitErrors = 0;
  let uncodedBlockErrors = 0;
  for (let block = 0; block < settings.blocks; block += 1) {
    const result = simulateUncodedBlock(settings, sigmaUncoded, uncodedRandom);
    uncodedBitErrors += result.bitErrors;
    uncodedBlockErrors += result.blockError ? 1 : 0;
  }

  let codedBitErrors = 0;
  let codedBlockErrors = 0;
  let iterationsSum = 0;
  let successfulCodewords = 0;
  const totalCodewords = settings.blocks * (settings.infoBlockLength / code.k);

  for (let block = 0; block < settings.blocks; block += 1) {
    const result = simulateCodedBlock(settings, code, sigmaCoded, codedRandom);
    codedBitErrors += result.bitErrors;
    codedBlockErrors += result.blockError ? 1 : 0;
    iterationsSum += result.iterationsSum;
    successfulCodewords += result.successCount;
  }

  const totalBits = settings.blocks * settings.infoBlockLength;
  const blerCoded = codedBlockErrors / settings.blocks;

  return {
    snr: snrDb,
    berUncoded: uncodedBitErrors / totalBits,
    berCoded: codedBitErrors / totalBits,
    blerUncoded: uncodedBlockErrors / settings.blocks,
    blerCoded,
    averageIterations: iterationsSum / totalCodewords,
    successfulCodewords: successfulCodewords / totalCodewords,
    effectiveThroughputMbps: estimateThroughput(settings, blerCoded, code),
    spectralEfficiency: estimateSpectralEfficiency(settings, blerCoded, code),
  };
}

export function runResearchSweep(settings: ResearchSettings, snrStart: number, snrEnd: number, snrStep: number) {
  const snrValues = buildSnrValues(snrStart, snrEnd, snrStep);
  return snrValues.map((snr, index) => simulateResearchPoint(snr, settings, index));
}

function gain(base: number, improved: number) {
  if (improved <= 0 && base > 0) return Number.POSITIVE_INFINITY;
  if (improved <= 0 && base <= 0) return 1;
  return base / improved;
}

function estimateThreshold(points: ResearchPoint[], metric: TargetMetric, mode: "uncoded" | "coded", target: number): ThresholdEstimate {
  const values = points.map((point) => {
    if (metric === "BER") return mode === "coded" ? point.berCoded : point.berUncoded;
    return mode === "coded" ? point.blerCoded : point.blerUncoded;
  });

  const snr = points.map((point) => point.snr);

  if (values.every((value) => value > target)) {
    return {
      reached: false,
      snr: null,
      note: "Цель не достигнута в исследованном диапазоне SNR.",
    };
  }

  if (values[0] <= target) {
    return {
      reached: false,
      snr: null,
      note: `Цель уже достигается ниже нижней границы диапазона (${snr[0].toFixed(2)} дБ).`,
    };
  }

  for (let i = 0; i < values.length - 1; i += 1) {
    const current = values[i];
    const next = values[i + 1];
    if ((current >= target && next <= target) || (current <= target && next >= target)) {
      const logCurrent = Math.log10(Math.max(current, 1e-9));
      const logNext = Math.log10(Math.max(next, 1e-9));
      const logTarget = Math.log10(Math.max(target, 1e-9));
      const ratio = Math.abs(logNext - logCurrent) < 1e-12 ? 0 : (logTarget - logCurrent) / (logNext - logCurrent);
      return {
        reached: true,
        snr: snr[i] + ratio * (snr[i + 1] - snr[i]),
        note: "Порог оценён интерполяцией между соседними точками SNR.",
      };
    }
  }

  return {
    reached: false,
    snr: null,
    note: "Не удалось устойчиво оценить порог по имеющимся точкам.",
  };
}

export function estimateEnergyGain(points: ResearchPoint[], metric: TargetMetric, target: number): EnergyGainEstimate {
  const uncoded = estimateThreshold(points, metric, "uncoded", target);
  const coded = estimateThreshold(points, metric, "coded", target);

  if (uncoded.reached && coded.reached && uncoded.snr !== null && coded.snr !== null) {
    return {
      metric,
      target,
      uncoded,
      coded,
      gainDb: uncoded.snr - coded.snr,
      note: `Энергетический выигрыш оценивается как разность требуемых SNR для достижения ${metric} = ${target}.`,
    };
  }

  return {
    metric,
    target,
    uncoded,
    coded,
    gainDb: null,
    note: "Для одной из кривых порог не достигнут или лежит вне исследованного диапазона.",
  };
}

export function summarizeResearch(points: ResearchPoint[]): ResearchSummary {
  const bestBerGain = points.reduce<{ value: number; point: ResearchPoint }>(
    (best, current) => {
      const value = gain(current.berUncoded, current.berCoded);
      return value > best.value ? { value, point: current } : best;
    },
    { value: -1, point: points[0] as ResearchPoint },
  );

  const bestBlerGain = points.reduce<{ value: number; point: ResearchPoint }>(
    (best, current) => {
      const value = gain(current.blerUncoded, current.blerCoded);
      return value > best.value ? { value, point: current } : best;
    },
    { value: -1, point: points[0] as ResearchPoint },
  );

  const berEnergyGain = estimateEnergyGain(points, "BER", 1e-3);
  const blerEnergyGain = estimateEnergyGain(points, "BLER", 1e-1);

  return {
    bestBerGain,
    bestBlerGain,
    averageIterations: points.reduce((sum, point) => sum + point.averageIterations, 0) / points.length,
    successRatio: points.reduce((sum, point) => sum + point.successfulCodewords, 0) / points.length,
    minCodedBer: Math.min(...points.map((point) => point.berCoded)),
    minCodedBler: Math.min(...points.map((point) => point.blerCoded)),
    averageThroughputMbps: points.reduce((sum, point) => sum + point.effectiveThroughputMbps, 0) / points.length,
    peakThroughputMbps: Math.max(...points.map((point) => point.effectiveThroughputMbps)),
    averageSpectralEfficiency: points.reduce((sum, point) => sum + point.spectralEfficiency, 0) / points.length,
    peakSpectralEfficiency: Math.max(...points.map((point) => point.spectralEfficiency)),
    requiredSnrBer: berEnergyGain.coded.snr,
    requiredSnrBler: blerEnergyGain.coded.snr,
    berEnergyGain,
    blerEnergyGain,
  };
}

export function formatTarget(target: number) {
  if (target >= 0.01) return target.toFixed(2);
  if (target >= 0.001) return target.toFixed(3);
  return target.toExponential(1);
}
