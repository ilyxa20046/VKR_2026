import { useMemo, useState, type ReactNode } from "react";
import {
  buildSnrValues,
  channelNotes,
  CODE_PROFILES,
  formatTarget,
  modulationNotes,
  normalizeInfoBlockLength,
  runResearchSweep,
  spatialNotes,
  summarizeResearch,
  waveformNotes,
  type ChannelName,
  type CodeProfileId,
  type ModulationName,
  type ResearchPoint,
  type ResearchSettings,
  type SpatialModeName,
  type WaveformName,
} from "./researchModel";

type AppTab = "model" | "results" | "science" | "javafx" | "batch" | "roadmap";

type FormState = {
  infoBlockLength: number;
  snrStart: number;
  snrEnd: number;
  snrStep: number;
  blocks: number;
  maxIterations: number;
  normalization: number;
  seed: number;
  modulation: ModulationName;
  channel: ChannelName;
  codeProfile: CodeProfileId;
  waveform: WaveformName;
  spatialMode: SpatialModeName;
};

type ScienceCard = {
  title: string;
  description: string;
  bullets: string[];
};

const thesisTitle =
  "Исследование и моделирование помехоустойчивого кодирования в зашумлённом цифровом канале мобильной сети 5G";

const tabs: { id: AppTab; label: string; note: string }[] = [
  { id: "model", label: "Научная модель", note: "Канал, OFDM, MIMO-like, LDPC" },
  { id: "results", label: "Результаты", note: "BER, BLER, throughput, energy gain" },
  { id: "science", label: "Исследовательская ценность", note: "Что усилили и почему" },
  { id: "javafx", label: "Как перенести в JavaFX", note: "Что менять в desktop-приложении" },
  { id: "batch", label: "Batch-режим", note: "Пакетное сравнение сценариев" },
  { id: "roadmap", label: "Следующие шаги", note: "Куда развивать модель дальше" },
];

const modulationOptions: ModulationName[] = ["BPSK", "QPSK", "16-QAM"];
const channelOptions: ChannelName[] = ["AWGN", "Rayleigh"];
const codeOptions: CodeProfileId[] = ["edu-24-12", "qcldpc-96-48"];
const waveformOptions: WaveformName[] = ["Single-carrier", "OFDM-64", "OFDM-128"];
const spatialOptions: SpatialModeName[] = ["SISO", "2x2 Diversity"];

const scienceCards: ScienceCard[] = [
  {
    title: "1. OFDM-like waveform",
    description:
      "В модель добавлены OFDM-64 и OFDM-128, чтобы приблизить её к широкополосному радиоинтерфейсу 5G и показать влияние cyclic prefix / pilot overhead на полезную скорость.",
    bullets: [
      "Single-carrier даёт базовый эталон без OFDM-overhead.",
      "OFDM-64 демонстрирует более учебный многонесущий режим с заметным overhead.",
      "OFDM-128 показывает более широкополосный сценарий с меньшими относительными потерями на служебные ресурсы.",
    ],
  },
  {
    title: "2. Пространственное diversity-усиление",
    description:
      "Добавлен упрощённый 2x2 diversity / Alamouti-like режим, который позволяет исследовать, как пространственное разнесение улучшает устойчивость передачи в fading-канале.",
    bullets: [
      "SISO используется как базовый режим сравнения.",
      "2x2 Diversity в модели реализует идею combining по нескольким независимым ветвям.",
      "Это позволяет говорить в дипломе уже не только о кодировании и модуляции, но и о простейших MIMO-элементах.",
    ],
  },
  {
    title: "3. Ближе к 5G-радиоинтерфейсу",
    description:
      "Комбинация OFDM, QPSK/16-QAM, Rayleigh и 2x2 diversity делает исследовательскую постановку заметно ближе к реальным задачам PHY-уровня мобильной сети.",
    bullets: [
      "Можно анализировать, как OFDM снижает влияние частотно-селективных замираний в упрощённой модели.",
      "Можно исследовать компромисс между надёжностью и эффективностью при разных spatial/waveform режимах.",
      "Это усиливает содержательность главы 3 и практическую значимость разработанного ПО.",
    ],
  },
  {
    title: "4. Throughput и spectral efficiency с учётом waveform",
    description:
      "Теперь throughput и spectral efficiency учитывают не только BLER и кодовую скорость, но и полезную долю ресурса waveform-профиля.",
    bullets: [
      "OFDM-модель вводит overhead, уменьшая полезную пропускную способность.",
      "2x2 diversity влияет на BER/BLER, а значит и на реальную полезную скорость доставки данных.",
      "Это помогает обсуждать компромисс между устойчивостью и производительностью более инженерно корректно.",
    ],
  },
];

const javafxNotes = [
  "SimulationConfig, SimulationView.fxml, SimulationController и ConfigFileService уже расширены параметрами waveform и spatial mode, поэтому desktop-приложение понимает Single-carrier, OFDM-64, OFDM-128, SISO и 2x2 Diversity.",
  "ParameterHelpService и tooltips формы уже объясняют OFDM-overhead и diversity gain, а Reports/Chapter 3 материалы учитывают waveform и spatial mode в текстовом анализе.",
  "SimulationService.java уже получил OFDM/MIMO-like логику: учитывается полезная доля ресурса waveform, более короткая fading-корреляция на поднесущих и diversity-combining в режиме 2x2.",
  "На Results-экране уже реализован режим демонстрации защиты: он выводит крупные KPI-карточки по BER gain, BLER gain, peak throughput и required SNR для показа комиссии.",
  "Waveform и spatial mode теперь также выведены явно на Results / Compare / Batch, поэтому новый OFDM/MIMO-like контекст виден не только в форме настроек, но и на аналитических экранах.",
  "Results, Compare и Batch теперь оформлены в scroll-friendly аналитическом layout: крупные графики расположены выше, а таблицы и narrative вынесены в раскрывающиеся секции для более чистого восприятия.",
  "MainLayout финально отполирован как shell-часть desktop-продукта: refined top-bar, более премиальный sidebar и спокойная status-bar теперь делают всё приложение визуально цельным. Стартовая страница переосмыслена как экран «Главное» и уже объясняет назначение приложения, исследуемую задачу, архитектуру системы, её возможности, практическую значимость для ВКР, формируемые метрики и графики, отличия от базовой модели и типовой сценарий работы пользователя.",
  "Defense Mode уже расширен и на Compare/Batch: на этих экранах есть крупные презентационные карточки победителя, peak throughput, spectral efficiency и required SNR, что делает систему готовой к показу на защите.",
  "Дополнительно появился presentation-summary экспорт: на Results, Compare и Batch можно сохранить короткую защитную сводку в TXT для слайдов, доклада и итоговых материалов ВКР.",
  "Для дипломного текста уже доступны Word-friendly и HTML-friendly экспортные форматы: single, compare и batch-отчёты можно сохранять в виде структурированного текста для Word и HTML-файлов для печати, конвертации в PDF и вставки в приложения.",
  "Batch persistence уже сохраняет расширенные сценарии, поэтому можно накапливать и переоткрывать OFDM/MIMO-like batch-эксперименты между сессиями.",
  "Для запуска desktop-версии через Maven важно, чтобы сам Maven был привязан к JDK 17; если Maven стартует на Java 8, `javafx:run` завершится ошибкой `UnsupportedClassVersionError`, тогда как запуск через Launcher из IDE продолжит работать нормально.",
  "С точки зрения ВКР это уже позволяет говорить, что JavaFX-система поддерживает не только канал и кодирование, но и упрощённые элементы OFDM/MIMO-контура 5G-like PHY.",
];

const roadmap = [
  "Базовый перенос OFDM-like waveform и 2x2 diversity в JavaFX уже выполнен, поэтому следующим шагом становится не интеграция, а углубление физической модели.",
  "Далее можно усилить модель частотно-селективным многолучевым каналом, где OFDM будет давать ещё более физически правдоподобный выигрыш по сравнению с single-carrier режимом.",
  "Затем можно перейти к spatial multiplexing вместо только diversity, чтобы исследовать уже не только устойчивость, но и рост пропускной способности.",
  "Ещё один сильный научный шаг — добавить более строгий OFDM frame-structure: subcarrier spacing, pilot pattern и approximation к slot-based 5G NR resource grid.",
  "После появления Word-friendly и HTML-friendly форматов следующим шагом становится уже прямой PDF-экспорт с печатными шаблонами для приложений, главы 3 и материалов к защите.",
  "Самый глубокий дальнейший шаг — приблизиться к 5G NR coding chain и base-graph / rate-matching модели LDPC поверх OFDM/MIMO-контура.",
];

const batchNotes = [
  "Batch-режим особенно полезен для многосценарного анализа OFDM/MIMO-like профилей: можно сравнивать Single-carrier, OFDM-64 и OFDM-128 на одних и тех же SNR-точках.",
  "Отдельный интерес представляет матрица сравнений SISO vs 2x2 Diversity для AWGN и Rayleigh, чтобы показать эффект пространственного разнесения именно в fading-среде.",
  "Throughput и spectral efficiency теперь интерпретируются уже в контексте waveform overhead: OFDM может улучшать устойчивость, но уменьшать полезную долю ресурса.",
  "Для ВКР это даёт уже почти полноценный comparative radio-interface study: modulation + coding + channel + waveform + spatial mode.",
];

const initialForm: FormState = {
  infoBlockLength: 192,
  snrStart: 0,
  snrEnd: 8,
  snrStep: 1,
  blocks: 120,
  maxIterations: 10,
  normalization: 0.85,
  seed: 2025,
  modulation: "QPSK",
  channel: "Rayleigh",
  codeProfile: "qcldpc-96-48",
  waveform: "OFDM-64",
  spatialMode: "2x2 Diversity",
};

function normalizeForm(form: FormState): FormState {
  return {
    ...form,
    infoBlockLength: normalizeInfoBlockLength(form.infoBlockLength, form.codeProfile),
    snrStart: Math.min(form.snrStart, form.snrEnd),
    snrEnd: Math.max(form.snrStart, form.snrEnd),
    snrStep: Math.max(0.5, form.snrStep),
    blocks: Math.max(20, Math.round(form.blocks)),
    maxIterations: Math.max(2, Math.round(form.maxIterations)),
    normalization: Math.min(1, Math.max(0.5, form.normalization)),
    seed: Math.max(1, Math.round(form.seed)),
  };
}

function runExperiment(form: FormState) {
  const normalized = normalizeForm(form);
  const settings: ResearchSettings = {
    infoBlockLength: normalized.infoBlockLength,
    blocks: normalized.blocks,
    seed: normalized.seed,
    maxIterations: normalized.maxIterations,
    normalization: normalized.normalization,
    modulation: normalized.modulation,
    channel: normalized.channel,
    codeProfile: normalized.codeProfile,
    waveform: normalized.waveform,
    spatialMode: normalized.spatialMode,
  };

  const points = runResearchSweep(settings, normalized.snrStart, normalized.snrEnd, normalized.snrStep);
  return {
    normalized,
    settings,
    points,
    summary: summarizeResearch(points),
  };
}

function copyText(text: string) {
  if (navigator.clipboard?.writeText) return navigator.clipboard.writeText(text);
  const area = document.createElement("textarea");
  area.value = text;
  document.body.appendChild(area);
  area.select();
  document.execCommand("copy");
  document.body.removeChild(area);
  return Promise.resolve();
}

function formatMetric(value: number) {
  if (value === 0) return "0";
  if (value >= 0.1) return value.toFixed(3);
  if (value >= 0.001) return value.toFixed(4);
  return value.toExponential(2);
}

function formatGain(value: number) {
  if (!Number.isFinite(value)) return "∞";
  if (value >= 100) return `${value.toFixed(0)}x`;
  if (value >= 10) return `${value.toFixed(1)}x`;
  return `${value.toFixed(2)}x`;
}

function formatDb(value: number | null) {
  return value === null ? "не оценён" : `${value.toFixed(2)} дБ`;
}

function formatThroughput(value: number) {
  return `${value.toFixed(2)} Мбит/с`;
}

function formatEfficiency(value: number) {
  return `${value.toFixed(2)} бит/с/Гц`;
}

function buildCsv(points: ResearchPoint[]) {
  return [
    "SNR_dB,BER_uncoded,BER_coded,BLER_uncoded,BLER_coded,avg_iterations,success_ratio,throughput_mbps,spectral_efficiency",
    ...points.map((point) =>
      [
        point.snr.toFixed(2),
        point.berUncoded.toFixed(8),
        point.berCoded.toFixed(8),
        point.blerUncoded.toFixed(8),
        point.blerCoded.toFixed(8),
        point.averageIterations.toFixed(4),
        point.successfulCodewords.toFixed(4),
        point.effectiveThroughputMbps.toFixed(4),
        point.spectralEfficiency.toFixed(4),
      ].join(","),
    ),
  ].join("\n");
}

function Panel({ title, subtitle, children, action }: { title: string; subtitle: string; children: ReactNode; action?: ReactNode }) {
  return (
    <section className="rounded-[1.75rem] border border-white/10 bg-white/5 p-5 shadow-xl shadow-slate-950/10">
      <div className="flex flex-col gap-4 border-b border-white/10 pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-white">{title}</h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-400">{subtitle}</p>
        </div>
        {action}
      </div>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function MetricCard({ label, value, note }: { label: string; value: string; note: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-slate-950/45 p-5">
      <div className="text-sm text-slate-400">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-white">{value}</div>
      <p className="mt-2 text-sm leading-6 text-slate-400">{note}</p>
    </div>
  );
}

function SidebarButton({ active, label, note, onClick }: { active: boolean; label: string; note: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`w-full rounded-2xl border px-4 py-3 text-left transition ${
        active
          ? "border-cyan-400/50 bg-cyan-400/15 text-white"
          : "border-white/10 bg-slate-950/40 text-slate-300 hover:border-white/20 hover:bg-white/10"
      }`}
    >
      <div className="font-medium">{label}</div>
      <div className="mt-1 text-xs text-slate-400">{note}</div>
    </button>
  );
}

function NumberField({ label, value, step, min, max, onChange }: { label: string; value: number; step?: number; min?: number; max?: number; onChange: (value: number) => void }) {
  return (
    <label className="block rounded-2xl border border-white/10 bg-slate-950/40 p-4">
      <div className="text-sm font-medium text-slate-200">{label}</div>
      <input
        type="number"
        value={value}
        step={step}
        min={min}
        max={max}
        onChange={(event) => onChange(Number(event.target.value))}
        className="mt-3 w-full rounded-xl border border-white/10 bg-slate-900/80 px-3 py-2 text-white outline-none transition focus:border-cyan-400/40"
      />
    </label>
  );
}

function SelectField<T extends string>({ label, value, options, onChange }: { label: string; value: T; options: readonly T[]; onChange: (value: T) => void }) {
  return (
    <label className="block rounded-2xl border border-white/10 bg-slate-950/40 p-4">
      <div className="text-sm font-medium text-slate-200">{label}</div>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value as T)}
        className="mt-3 w-full rounded-xl border border-white/10 bg-slate-900/80 px-3 py-2 text-white outline-none transition focus:border-cyan-400/40"
      >
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}

function LogChart({ title, snrValues, series }: { title: string; snrValues: number[]; series: { label: string; color: string; values: number[] }[] }) {
  const width = 760;
  const height = 320;
  const paddingLeft = 58;
  const paddingRight = 24;
  const paddingTop = 18;
  const paddingBottom = 42;
  const minX = Math.min(...snrValues);
  const maxX = Math.max(...snrValues);
  const xRange = maxX - minX || 1;
  const logMin = -5;
  const logMax = 0;
  const logRange = logMax - logMin;

  const x = (value: number) => paddingLeft + ((value - minX) / xRange) * (width - paddingLeft - paddingRight);
  const y = (value: number) => {
    const clamped = Math.max(1e-5, Math.min(1, value));
    const logValue = Math.log10(clamped);
    return paddingTop + ((logMax - logValue) / logRange) * (height - paddingTop - paddingBottom);
  };

  const ticks = [0, -1, -2, -3, -4, -5];

  return (
    <div className="rounded-[1.5rem] border border-white/10 bg-slate-950/40 p-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-lg font-semibold text-white">{title}</h3>
          <p className="mt-1 text-sm text-slate-400">Логарифмическая шкала по оси ошибок.</p>
        </div>
        <div className="flex flex-wrap gap-3 text-xs text-slate-300">
          {series.map((item) => (
            <div key={item.label} className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5">
              <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: item.color }} />
              {item.label}
            </div>
          ))}
        </div>
      </div>
      <div className="mt-4 overflow-x-auto">
        <svg viewBox={`0 0 ${width} ${height}`} className="min-w-[720px] text-slate-400">
          {ticks.map((tick) => {
            const tickValue = 10 ** tick;
            const yPos = y(tickValue);
            return (
              <g key={tick}>
                <line x1={paddingLeft} x2={width - paddingRight} y1={yPos} y2={yPos} stroke="rgba(148,163,184,0.16)" strokeDasharray="4 6" />
                <text x={paddingLeft - 10} y={yPos + 4} textAnchor="end" fontSize="11" fill="currentColor">
                  {tick === 0 ? "1" : `10${tick}`}
                </text>
              </g>
            );
          })}
          {snrValues.map((value) => {
            const xPos = x(value);
            return (
              <g key={value}>
                <line x1={xPos} x2={xPos} y1={paddingTop} y2={height - paddingBottom} stroke="rgba(148,163,184,0.12)" />
                <text x={xPos} y={height - 14} textAnchor="middle" fontSize="11" fill="currentColor">
                  {value}
                </text>
              </g>
            );
          })}
          <line x1={paddingLeft} x2={width - paddingRight} y1={height - paddingBottom} y2={height - paddingBottom} stroke="rgba(148,163,184,0.35)" />
          <line x1={paddingLeft} x2={paddingLeft} y1={paddingTop} y2={height - paddingBottom} stroke="rgba(148,163,184,0.35)" />
          {series.map((item) => {
            const points = item.values.map((value, index) => `${x(snrValues[index])},${y(value)}`).join(" ");
            return (
              <g key={item.label}>
                <polyline fill="none" stroke={item.color} strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" points={points} />
                {item.values.map((value, index) => (
                  <circle key={`${item.label}-${index}`} cx={x(snrValues[index])} cy={y(value)} r="4" fill={item.color} />
                ))}
              </g>
            );
          })}
        </svg>
      </div>
    </div>
  );
}

export function App() {
  const initialRun = useMemo(() => runExperiment(initialForm), []);
  const [activeTab, setActiveTab] = useState<AppTab>("model");
  const [form, setForm] = useState<FormState>(initialRun.normalized);
  const [results, setResults] = useState<ResearchPoint[]>(initialRun.points);
  const [summary, setSummary] = useState(initialRun.summary);
  const [copiedKey, setCopiedKey] = useState("");

  const code = CODE_PROFILES[form.codeProfile];
  const snrValues = useMemo(() => results.map((point) => point.snr), [results]);
  const csvText = useMemo(() => buildCsv(results), [results]);
  const researchNarrative = useMemo(() => {
    return `В текущем сценарии (${form.modulation}, ${form.channel}, ${form.waveform}, ${form.spatialMode}, ${code.name}) ` +
      `максимальный выигрыш по BER достигает ${formatGain(summary.bestBerGain.value)}, а по BLER — ${formatGain(summary.bestBlerGain.value)}. ` +
      `Пиковая полезная пропускная способность оценивается как ${formatThroughput(summary.peakThroughputMbps)}, ` +
      `а максимальная спектральная эффективность — как ${formatEfficiency(summary.peakSpectralEfficiency)}. ` +
      `Энергетический выигрыш по порогу BER = ${formatTarget(summary.berEnergyGain.target)} составляет ${formatDb(summary.berEnergyGain.gainDb)}.`;
  }, [code.name, form.channel, form.modulation, form.spatialMode, form.waveform, summary]);

  const runModeling = () => {
    const experiment = runExperiment(form);
    setForm(experiment.normalized);
    setResults(experiment.points);
    setSummary(experiment.summary);
    setActiveTab("results");
  };

  const handleCopy = async (key: string, value: string) => {
    await copyText(value);
    setCopiedKey(key);
    window.setTimeout(() => setCopiedKey(""), 1800);
  };

  const updateForm = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const snrList = buildSnrValues(form.snrStart, form.snrEnd, form.snrStep);

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,rgba(34,211,238,0.14),transparent_25%),linear-gradient(180deg,#020617_0%,#0f172a_42%,#111827_100%)] text-slate-100">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <section className="overflow-hidden rounded-[2rem] border border-white/10 bg-white/5 shadow-2xl shadow-cyan-950/20 backdrop-blur">
          <div className="grid gap-8 px-6 py-8 lg:grid-cols-[1.2fr_0.8fr] lg:px-10 lg:py-10">
            <div className="space-y-6">
              <div className="inline-flex items-center gap-2 rounded-full border border-cyan-400/30 bg-cyan-400/10 px-4 py-2 text-sm text-cyan-100">
                OFDM / MIMO-like расширение исследовательской модели для ВКР по 5G
              </div>
              <div className="space-y-4">
                <h1 className="max-w-4xl text-4xl font-semibold tracking-tight text-white sm:text-5xl">
                  Модель стала ближе к радиоинтерфейсу 5G: LDPC + QPSK/16-QAM + Rayleigh + OFDM + 2x2 diversity
                </h1>
                <p className="max-w-3xl text-lg leading-8 text-slate-300">
                  Теперь исследование включает не только кодирование и канал, но и упрощённые элементы waveform/spatial контура:
                  single-carrier и OFDM-профили, а также SISO и 2x2 diversity для более сильной инженерной интерпретации BER, BLER,
                  throughput и required SNR.
                </p>
              </div>

              <div className="grid gap-4 sm:grid-cols-5">
                <MetricCard label="Модуляция" value={form.modulation} note={modulationNotes[form.modulation]} />
                <MetricCard label="Канал" value={form.channel} note={channelNotes[form.channel]} />
                <MetricCard label="Waveform" value={form.waveform} note={waveformNotes[form.waveform]} />
                <MetricCard label="Spatial mode" value={form.spatialMode} note={spatialNotes[form.spatialMode]} />
                <MetricCard label="LDPC" value={code.name} note={code.description} />
              </div>
            </div>

            <div className="rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-6">
              <div className="text-sm text-slate-400">Тема исследования</div>
              <div className="mt-2 text-xl font-semibold leading-8 text-white">{thesisTitle}</div>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <div className="text-sm text-slate-400">SNR-точки</div>
                  <div className="mt-2 font-semibold text-white">{snrList.map((value) => value.toFixed(1)).join(", ")}</div>
                </div>
                <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <div className="text-sm text-slate-400">Инфо-блок</div>
                  <div className="mt-2 font-semibold text-white">{form.infoBlockLength} бит</div>
                </div>
              </div>
              <button
                onClick={runModeling}
                className="mt-5 inline-flex w-full items-center justify-center rounded-2xl bg-cyan-400 px-4 py-3 font-semibold text-slate-950 transition hover:bg-cyan-300"
              >
                Пересчитать OFDM/MIMO-like модель
              </button>
              <div className="mt-5 rounded-2xl border border-emerald-300/20 bg-emerald-300/10 p-4 text-sm leading-6 text-emerald-50">
                Для главы 3 это уже можно описывать как исследование влияния не только кодирования, но и waveform/spatial профиля на помехоустойчивость и полезную скорость передачи данных.
              </div>
            </div>
          </div>
        </section>

        <div className="mt-8 grid gap-8 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside className="space-y-6 lg:sticky lg:top-6 lg:self-start">
            <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-5 shadow-xl shadow-slate-950/10">
              <div className="text-sm font-medium uppercase tracking-[0.2em] text-cyan-200/80">Разделы</div>
              <div className="mt-4 space-y-2">
                {tabs.map((tab) => (
                  <SidebarButton key={tab.id} active={activeTab === tab.id} label={tab.label} note={tab.note} onClick={() => setActiveTab(tab.id)} />
                ))}
              </div>
            </div>
            <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-5 shadow-xl shadow-slate-950/10">
              <div className="text-sm font-medium uppercase tracking-[0.2em] text-cyan-200/80">Быстрые действия</div>
              <div className="mt-4 space-y-3">
                <button onClick={() => handleCopy("csv", csvText)} className="w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-3 text-sm text-slate-100 transition hover:bg-white/10">
                  {copiedKey === "csv" ? "CSV скопирован" : "Скопировать CSV"}
                </button>
                <button onClick={() => handleCopy("narrative", researchNarrative)} className="w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-3 text-sm text-slate-100 transition hover:bg-white/10">
                  {copiedKey === "narrative" ? "Вывод скопирован" : "Скопировать вывод"}
                </button>
              </div>
            </div>
          </aside>

          <main className="space-y-8">
            {activeTab === "model" && (
              <div className="space-y-8">
                <Panel title="Параметры OFDM/MIMO-like модели" subtitle="Теперь можно управлять модуляцией, каналом, LDPC-профилем, waveform-профилем и пространственным режимом передачи.">
                  <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
                    <SelectField label="Модуляция" value={form.modulation} options={modulationOptions} onChange={(value) => updateForm("modulation", value)} />
                    <SelectField label="Канал" value={form.channel} options={channelOptions} onChange={(value) => updateForm("channel", value)} />
                    <SelectField label="LDPC-профиль" value={form.codeProfile} options={codeOptions} onChange={(value) => updateForm("codeProfile", value)} />
                    <SelectField label="Waveform" value={form.waveform} options={waveformOptions} onChange={(value) => updateForm("waveform", value)} />
                    <SelectField label="Spatial mode" value={form.spatialMode} options={spatialOptions} onChange={(value) => updateForm("spatialMode", value)} />
                    <NumberField label="Длина информационного блока" value={form.infoBlockLength} step={CODE_PROFILES[form.codeProfile].k} min={CODE_PROFILES[form.codeProfile].k} onChange={(value) => updateForm("infoBlockLength", value)} />
                    <NumberField label="SNR от, дБ" value={form.snrStart} step={0.5} onChange={(value) => updateForm("snrStart", value)} />
                    <NumberField label="SNR до, дБ" value={form.snrEnd} step={0.5} onChange={(value) => updateForm("snrEnd", value)} />
                    <NumberField label="Шаг SNR, дБ" value={form.snrStep} step={0.5} min={0.5} onChange={(value) => updateForm("snrStep", value)} />
                    <NumberField label="Блоков на точку" value={form.blocks} step={10} min={20} onChange={(value) => updateForm("blocks", value)} />
                    <NumberField label="Макс. итераций" value={form.maxIterations} step={1} min={2} onChange={(value) => updateForm("maxIterations", value)} />
                    <NumberField label="Коэффициент normalization" value={form.normalization} step={0.05} min={0.5} max={1} onChange={(value) => updateForm("normalization", value)} />
                    <NumberField label="Seed" value={form.seed} step={1} min={1} onChange={(value) => updateForm("seed", value)} />
                  </div>
                  <div className="mt-5 grid gap-4 lg:grid-cols-4">
                    <MetricCard label="Waveform note" value={form.waveform} note={waveformNotes[form.waveform]} />
                    <MetricCard label="Spatial note" value={form.spatialMode} note={spatialNotes[form.spatialMode]} />
                    <MetricCard label="Channel note" value={form.channel} note={channelNotes[form.channel]} />
                    <MetricCard label="LDPC note" value={code.name} note={`${code.description} Кодовая скорость ${code.rate.toFixed(2)}, длина слова ${code.n}.`} />
                  </div>
                </Panel>
              </div>
            )}

            {activeTab === "results" && (
              <div className="space-y-8">
                <Panel title="Сводка результатов" subtitle="BER, BLER, throughput, spectral efficiency и required SNR уже интерпретируются с учётом waveform/spatial режима.">
                  <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <MetricCard label="Лучший выигрыш по BER" value={formatGain(summary.bestBerGain.value)} note={`Точка SNR = ${summary.bestBerGain.point.snr.toFixed(1)} дБ`} />
                    <MetricCard label="Лучший выигрыш по BLER" value={formatGain(summary.bestBlerGain.value)} note={`Точка SNR = ${summary.bestBlerGain.point.snr.toFixed(1)} дБ`} />
                    <MetricCard label="Peak throughput" value={formatThroughput(summary.peakThroughputMbps)} note="Учитывает BLER, кодовую скорость и полезную долю waveform-ресурса." />
                    <MetricCard label="Peak spectral efficiency" value={formatEfficiency(summary.peakSpectralEfficiency)} note="Позволяет сравнивать производительность режимов OFDM/SISO/MIMO-like на инженерном уровне." />
                  </div>
                  <div className="mt-4 grid gap-4 md:grid-cols-2">
                    <MetricCard label="Required SNR @ BER = 1e-3" value={formatDb(summary.requiredSnrBer)} note="Требуемый SNR для coded-кривой по BER-порогу." />
                    <MetricCard label="Required SNR @ BLER = 1e-1" value={formatDb(summary.requiredSnrBler)} note="Требуемый SNR для coded-кривой по BLER-порогу." />
                  </div>
                  <div className="mt-5 rounded-[1.5rem] border border-cyan-400/20 bg-cyan-400/10 p-5 text-sm leading-7 text-cyan-50">{researchNarrative}</div>
                </Panel>

                <LogChart
                  title="BER(SNR)"
                  snrValues={snrValues}
                  series={[
                    { label: "Без кодирования", color: "#f97316", values: results.map((point) => point.berUncoded) },
                    { label: "LDPC", color: "#22d3ee", values: results.map((point) => point.berCoded) },
                  ]}
                />

                <LogChart
                  title="BLER(SNR)"
                  snrValues={snrValues}
                  series={[
                    { label: "Без кодирования", color: "#f43f5e", values: results.map((point) => point.blerUncoded) },
                    { label: "LDPC", color: "#34d399", values: results.map((point) => point.blerCoded) },
                  ]}
                />

                <Panel title="Таблица численных результатов" subtitle="Теперь таблица пригодна для анализа компромисса между устойчивостью и эффективностью при разных OFDM/MIMO-like режимах.">
                  <div className="overflow-x-auto rounded-[1.5rem] border border-white/10 bg-slate-950/40">
                    <table className="min-w-full text-left text-sm">
                      <thead className="bg-white/5 text-slate-200">
                        <tr>
                          <th className="px-4 py-3 font-medium">SNR, дБ</th>
                          <th className="px-4 py-3 font-medium">BER без кода</th>
                          <th className="px-4 py-3 font-medium">BER LDPC</th>
                          <th className="px-4 py-3 font-medium">BLER без кода</th>
                          <th className="px-4 py-3 font-medium">BLER LDPC</th>
                          <th className="px-4 py-3 font-medium">Ср. итерации</th>
                          <th className="px-4 py-3 font-medium">Сходимость</th>
                          <th className="px-4 py-3 font-medium">Throughput</th>
                          <th className="px-4 py-3 font-medium">Spectral efficiency</th>
                        </tr>
                      </thead>
                      <tbody>
                        {results.map((point) => (
                          <tr key={point.snr} className="border-t border-white/10 text-slate-300">
                            <td className="px-4 py-3 font-semibold text-white">{point.snr.toFixed(1)}</td>
                            <td className="px-4 py-3">{formatMetric(point.berUncoded)}</td>
                            <td className="px-4 py-3 text-cyan-300">{formatMetric(point.berCoded)}</td>
                            <td className="px-4 py-3">{formatMetric(point.blerUncoded)}</td>
                            <td className="px-4 py-3 text-emerald-300">{formatMetric(point.blerCoded)}</td>
                            <td className="px-4 py-3">{point.averageIterations.toFixed(2)}</td>
                            <td className="px-4 py-3">{(point.successfulCodewords * 100).toFixed(1)}%</td>
                            <td className="px-4 py-3">{point.effectiveThroughputMbps.toFixed(2)}</td>
                            <td className="px-4 py-3">{point.spectralEfficiency.toFixed(2)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Panel>
              </div>
            )}

            {activeTab === "science" && (
              <div className="space-y-8">
                <Panel title="Чем именно усилена научная модель" subtitle="Ниже перечислены добавления, которые заметно приближают работу к реальному PHY-контексту мобильной сети.">
                  <div className="grid gap-4 md:grid-cols-2">
                    {scienceCards.map((card) => (
                      <article key={card.title} className="rounded-[1.5rem] border border-white/10 bg-slate-950/40 p-5">
                        <h3 className="text-lg font-semibold text-white">{card.title}</h3>
                        <p className="mt-3 text-sm leading-7 text-slate-300">{card.description}</p>
                        <ul className="mt-4 space-y-2 text-sm leading-6 text-slate-400">
                          {card.bullets.map((bullet) => (
                            <li key={bullet} className="flex gap-3">
                              <span className="mt-2 h-1.5 w-1.5 rounded-full bg-cyan-300" />
                              <span>{bullet}</span>
                            </li>
                          ))}
                        </ul>
                      </article>
                    ))}
                  </div>
                </Panel>
              </div>
            )}

            {activeTab === "javafx" && (
              <div className="space-y-8">
                <Panel title="Что теперь добавить в JavaFX-приложение" subtitle="Web-прототип уже показывает OFDM/MIMO-like уровень модели. Ниже — точечные изменения для desktop-версии.">
                  <div className="grid gap-4 md:grid-cols-2">
                    {javafxNotes.map((item) => (
                      <div key={item} className="rounded-[1.5rem] border border-white/10 bg-slate-950/40 p-5 text-sm leading-7 text-slate-300">
                        {item}
                      </div>
                    ))}
                  </div>
                </Panel>
              </div>
            )}

            {activeTab === "batch" && (
              <div className="space-y-8">
                <Panel title="Batch-режим для OFDM/MIMO-like сценариев" subtitle="Пакетный анализ становится особенно ценным при сравнении waveform и spatial-профилей на одних и тех же канальных условиях.">
                  <div className="grid gap-4 md:grid-cols-2">
                    {batchNotes.map((item) => (
                      <div key={item} className="rounded-[1.5rem] border border-white/10 bg-slate-950/40 p-5 text-sm leading-7 text-slate-300">
                        {item}
                      </div>
                    ))}
                  </div>
                </Panel>
              </div>
            )}

            {activeTab === "roadmap" && (
              <div className="space-y-8">
                <Panel title="Следующие научные шаги" subtitle="После OFDM/MIMO-like расширения модель уже смотрится значительно ближе к радиоинтерфейсу 5G. Дальше развитие можно вести по следующим линиям.">
                  <div className="space-y-4">
                    {roadmap.map((item, index) => (
                      <div key={item} className="rounded-[1.5rem] border border-white/10 bg-slate-950/40 p-5 text-sm leading-7 text-slate-300">
                        <span className="mr-3 inline-flex h-8 w-8 items-center justify-center rounded-full bg-cyan-400/15 font-semibold text-cyan-200">{index + 1}</span>
                        {item}
                      </div>
                    ))}
                  </div>
                </Panel>
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
