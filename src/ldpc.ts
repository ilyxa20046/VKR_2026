export type EdgeRef = {
  check: number;
  edge: number;
};

export type LdpcCode = {
  k: number;
  m: number;
  n: number;
  rate: number;
  messageTaps: number[][];
  checkToVars: number[][];
  varToChecks: EdgeRef[][];
  description: string;
};

export type LdpcSimulationSettings = {
  infoBlockLength: number;
  blocks: number;
  seed: number;
  maxIterations: number;
  normalization: number;
};

export type LdpcSimulationPoint = {
  snr: number;
  berUncoded: number;
  berCoded: number;
  blerUncoded: number;
  blerCoded: number;
  averageIterations: number;
  successfulCodewords: number;
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

function sigmaFromEbN0(snrDb: number, codeRate: number) {
  const ebn0 = 10 ** (snrDb / 10);
  return Math.sqrt(1 / (2 * codeRate * ebn0));
}

function buildEducationalLdpcCode(): LdpcCode {
  const k = 12;
  const m = 12;
  const n = k + m;
  const offsets = [0, 3, 7];

  const messageTaps = Array.from({ length: m }, (_, row) => offsets.map((offset) => (row + offset) % k));

  const checkToVars = Array.from({ length: m }, (_, row) => {
    const vars = [...messageTaps[row], k + row];

    if (row > 0) {
      vars.push(k + row - 1);
    }

    return vars;
  });

  const varToChecks: EdgeRef[][] = Array.from({ length: n }, () => []);

  checkToVars.forEach((vars, check) => {
    vars.forEach((variable, edge) => {
      varToChecks[variable].push({ check, edge });
    });
  });

  return {
    k,
    m,
    n,
    rate: k / n,
    messageTaps,
    checkToVars,
    varToChecks,
    description: "Учебный систематический LDPC-код (24,12) со скоростью 1/2 и итерационным soft-decision декодированием normalized min-sum.",
  };
}

export const LDPC_DEMO_CODE = buildEducationalLdpcCode();

export function encodeEducationalLdpc(infoBits: number[], code: LdpcCode = LDPC_DEMO_CODE) {
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

    for (const variable of code.checkToVars[check]) {
      syndrome ^= bits[variable];
    }

    if (syndrome !== 0) {
      return false;
    }
  }

  return true;
}

export function decodeEducationalLdpc(
  receivedSymbols: number[],
  sigma: number,
  maxIterations: number,
  normalization: number,
  code: LdpcCode = LDPC_DEMO_CODE,
) {
  const channelLLR = receivedSymbols.map((symbol) => (2 * symbol) / (sigma * sigma));
  const q = code.checkToVars.map((vars) => vars.map((variable) => channelLLR[variable]));
  const r = code.checkToVars.map((vars) => vars.map(() => 0));
  let hard = channelLLR.map((value) => (value < 0 ? 1 : 0));

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
        const value = messages[edge];
        const sign = value < 0 ? -1 : 1;
        const extrinsicSign = signProduct * sign;
        const minimum = edge === minIndex ? min2 : min1;
        r[check][edge] = normalization * extrinsicSign * minimum;
      }
    }

    const posterior = channelLLR.slice();

    for (let variable = 0; variable < code.n; variable += 1) {
      for (const edgeRef of code.varToChecks[variable]) {
        posterior[variable] += r[edgeRef.check][edgeRef.edge];
      }
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

function detectBit(bit: number, sigma: number, random: () => number) {
  const symbol = bit === 0 ? 1 : -1;
  const received = symbol + sigma * gaussian(random);
  return received < 0 ? 1 : 0;
}

export function simulateLdpcPoint(
  snrDb: number,
  settings: LdpcSimulationSettings,
  index: number,
  code: LdpcCode = LDPC_DEMO_CODE,
): LdpcSimulationPoint {
  const uncodedRandom = mulberry32(settings.seed + 1009 * (index + 1));
  const codedRandom = mulberry32(settings.seed + 5003 * (index + 1));
  const sigmaUncoded = sigmaFromEbN0(snrDb, 1);
  const sigmaCoded = sigmaFromEbN0(snrDb, code.rate);
  const totalBits = settings.blocks * settings.infoBlockLength;
  const codewordsPerBlock = settings.infoBlockLength / code.k;
  const totalCodewords = settings.blocks * codewordsPerBlock;

  let uncodedBitErrors = 0;
  let uncodedBlockErrors = 0;

  for (let block = 0; block < settings.blocks; block += 1) {
    let blockHasError = false;

    for (let bit = 0; bit < settings.infoBlockLength; bit += 1) {
      const sourceBit = uncodedRandom() >= 0.5 ? 1 : 0;
      const detectedBit = detectBit(sourceBit, sigmaUncoded, uncodedRandom);

      if (detectedBit !== sourceBit) {
        uncodedBitErrors += 1;
        blockHasError = true;
      }
    }

    if (blockHasError) {
      uncodedBlockErrors += 1;
    }
  }

  let codedBitErrors = 0;
  let codedBlockErrors = 0;
  let iterationsSum = 0;
  let successfulCodewords = 0;

  for (let block = 0; block < settings.blocks; block += 1) {
    let blockHasError = false;

    for (let word = 0; word < codewordsPerBlock; word += 1) {
      const info = Array.from({ length: code.k }, () => (codedRandom() >= 0.5 ? 1 : 0));
      const encoded = encodeEducationalLdpc(info, code);
      const received = encoded.map((bit) => {
        const symbol = bit === 0 ? 1 : -1;
        return symbol + sigmaCoded * gaussian(codedRandom);
      });
      const decoded = decodeEducationalLdpc(received, sigmaCoded, settings.maxIterations, settings.normalization, code);

      iterationsSum += decoded.iterationsUsed;
      successfulCodewords += decoded.success ? 1 : 0;

      for (let i = 0; i < code.k; i += 1) {
        if (decoded.decoded[i] !== info[i]) {
          codedBitErrors += 1;
          blockHasError = true;
        }
      }
    }

    if (blockHasError) {
      codedBlockErrors += 1;
    }
  }

  return {
    snr: snrDb,
    berUncoded: uncodedBitErrors / totalBits,
    berCoded: codedBitErrors / totalBits,
    blerUncoded: uncodedBlockErrors / settings.blocks,
    blerCoded: codedBlockErrors / settings.blocks,
    averageIterations: iterationsSum / totalCodewords,
    successfulCodewords: successfulCodewords / totalCodewords,
  };
}
