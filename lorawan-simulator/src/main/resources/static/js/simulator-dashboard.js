console.log("simulator-dashboard.js cargado");

const state = {
  currentStep: 1,
  file: {
    rawFile: null,
    inputPath: "",
    uploadedPath: "",
    fileName: "",
    delimiter: ",",
    hasHeader: true,
    columns: [],
    previewRows: [],
    rowCountEstimate: 0
  },
  topology: {
    rowsToProcess: 10,
    sendIntervalMs: 20,
    gateway: {
      gatewayId: "gw-1",
      x: 100,
      y: 50,
      maxTxPowerDBm: 20,
      udpPort: 5000,
      tcpPort: 6000
    },
    layout: {
      mode: "linear",
      baseX: 10,
      baseY: 20,
      distanceMeters: 18
    },
    deviceCount: 2
  },
  propagation: {
    frequencyMHz: 915.0,
    los: false,
    streetWidthMeters: 25.0,
    baseStationHeightMeters: 30.0,
    averageBuildingHeightMeters: 15.0,
    mobileStationHeightMeters: 3.0,
    streetOrientationDegrees: 30.0,
    buildingSeparationMeters: 50.0,
    urbanEnvironment: "MEDIUM_SIZE_CITY"
  },
  experiment: {
    adrEnabled: true,
    randomLossEnabled: false,
    randomLossProbability: 0.0
  },
  devices: [],
  result: null,
  lastExecutedPayload: null,
  comparison: {
    scenarioLabel: "",
    history: []
  },
  errors: {},
  ui: {
    busy: false,
    uploadMessage: ""
  }
};

function normalizeSlashes(value) {
  return String(value || "").trim().replace(/\\/g, "/");
}

function hasExplicitPath(value) {
  const v = normalizeSlashes(value);
  return v.includes("/") || /^[A-Za-z]:/.test(v);
}

function resolveInputFilePath(rawValue, selectedFile, uploadedPath) {
  const uploaded = normalizeSlashes(uploadedPath);
  if (uploaded) return uploaded;

  const typedValue = normalizeSlashes(rawValue);
  if (typedValue) {
    return hasExplicitPath(typedValue) ? typedValue : `data/${typedValue}`;
  }

  if (selectedFile?.name) {
    return `data/${selectedFile.name}`;
  }

  return "";
}

function getResolvedInputFile() {
  return resolveInputFilePath(
    state.file.inputPath,
    state.file.rawFile,
    state.file.uploadedPath
  );
}

function isSupportedFile(name) {
  return /\.(csv|txt)$/i.test(name || "");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#39;");
}


const COMPARISON_STORAGE_KEY =
  "lorawanSimulatorComparisonHistoryV1";

function cloneJson(value) {
  return JSON.parse(
    JSON.stringify(value)
  );
}

function restoreComparisonHistory() {
  try {
    const raw =
      sessionStorage.getItem(
        COMPARISON_STORAGE_KEY);

    if (!raw) {
      return;
    }

    const parsed =
      JSON.parse(raw);

    if (Array.isArray(parsed)) {
      state.comparison.history =
        parsed;
    }
  } catch (error) {
    console.warn(
      "No se pudo restaurar el historial de comparación.",
      error
    );
  }
}

function persistComparisonHistory() {
  try {
    sessionStorage.setItem(
      COMPARISON_STORAGE_KEY,
      JSON.stringify(
        state.comparison.history
      )
    );
  } catch (error) {
    console.warn(
      "No se pudo guardar el historial de comparación.",
      error
    );
  }
}

function clearComparisonHistory() {
  state.comparison.history = [];
  persistComparisonHistory();
  render();
}

function removeComparisonRun(runId) {
  state.comparison.history =
    state.comparison.history.filter(
      run => run.id !== runId
    );

  persistComparisonHistory();
  render();
}

function calculateScenarioSnapshot(
  label,
  payload,
  result) {

  const metrics =
    Array.isArray(result?.metrics)
      ? result.metrics
      : [];

  const totals =
    calculateGlobalMetrics(metrics);

  const radioMetrics =
    metrics.filter(metric =>
      Number(metric.radioSamples) > 0
    );

  const average = (items, field) => {
    if (!items.length) {
      return null;
    }

    const values =
      items
        .map(item => Number(item[field]))
        .filter(Number.isFinite);

    if (!values.length) {
      return null;
    }

    return values.reduce(
      (sum, value) => sum + value,
      0
    ) / values.length;
  };

  const maxExistingId =
    state.comparison.history.reduce(
      (maxId, run) =>
        Math.max(
          maxId,
          Number(run.id) || 0
        ),
      0
    );

  return {
    id: maxExistingId + 1,
    label,
    savedAt:
      new Date().toISOString(),

    configuration: {
      deviceCount:
        Array.isArray(payload?.devices)
          ? payload.devices.length
          : 0,

      frequencyMHz:
        Number(
          payload?.linkBudgetParameters
            ?.frequencyMHz
        ),

      los:
        Boolean(
          payload?.linkBudgetParameters
            ?.los
        ),

      adrEnabled:
        Boolean(payload?.adrEnabled),

      randomLossEnabled:
        Boolean(
          payload?.randomLossEnabled
        ),

      randomLossProbability:
        Number(
          payload?.randomLossProbability
          ?? 0
        )
    },

    metrics: {
      txAttempts:
        totals.txAttempts,

      rx:
        totals.rx,

      lost:
        totals.lost,

      lostByLinkBudget:
        totals.lostByLinkBudget,

      lostByRandom:
        totals.lostByRandom,

      pdr:
        totals.pdr,

      deliveryRate:
        totals.deliveryRate,

      rxPowerAvgDbm:
        totals.rxPowerAvg,

      linkMarginAvgDb:
        totals.linkMarginAvg,

      throughputKbps:
        totals.throughputKbps,

      latencyAvgMs:
        totals.latencyAvgMs,

      distanceAvgMeters:
        average(
          radioMetrics,
          "distanceMeters"
        ),

      pathLossAvgDb:
        average(
          radioMetrics,
          "pathLossDb"
        )
    },

    payload:
      cloneJson(payload),

    deviceMetrics:
      cloneJson(metrics)
  };
}

function saveCurrentResultForComparison() {
  if (!state.result?.success) {
    return;
  }

  const metrics =
    getResultMetrics();

  if (!metrics.length
      || !state.lastExecutedPayload) {

    return;
  }

  const typedLabel =
    String(
      state.comparison.scenarioLabel
      || ""
    ).trim();

  const label =
    typedLabel
    || `Ejecución ${
      state.comparison.history.length + 1
    }`;

  const snapshot =
    calculateScenarioSnapshot(
      label,
      state.lastExecutedPayload,
      state.result
    );

  state.comparison.history.push(
    snapshot
  );

  state.comparison.scenarioLabel = "";

  persistComparisonHistory();
  render();
}

function renderComparisonWarning(history) {
  if (history.length < 2) {
    return "";
  }

  const deviceCounts =
    new Set(
      history.map(
        run =>
          Number(
            run.configuration
              ?.deviceCount
          ) || 0
      )
    );

  if (deviceCounts.size <= 1) {
    return "";
  }

  return `
    <div class="results-note warning">
      <strong>Comparación con diferente número de dispositivos.</strong>
      Las métricas agregadas deben interpretarse considerando que
      las ejecuciones almacenadas no usan la misma cantidad de nodos.
      Para una comparación académica directa entre escenarios A, B3,
      C y D conviene utilizar un dispositivo por ejecución.
    </div>
  `;
}

function renderScenarioComparisonTable(history) {
  if (!history.length) {
    return "";
  }

  return `
    <div class="comparison-table-wrapper">
      <table class="metrics-table comparison-table responsive-data-table">
        <thead>
          <tr>
            <th>Escenario</th>
            <th>Disp.</th>
            <th>PDR</th>
            <th>RxPower Avg</th>
            <th>Link Margin Avg</th>
            <th>Link loss</th>
            <th>Random loss</th>
            <th>ADR</th>
            <th>Random p</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${history.map(run => `
            <tr>
              <td data-label="Escenario">
                <strong>
                  ${escapeHtml(run.label)}
                </strong>
              </td>
              <td data-label="Dispositivos">
                ${run.configuration.deviceCount}
              </td>
              <td data-label="PDR">
                ${formatMetric(run.metrics.pdr)}%
              </td>
              <td data-label="RxPower Avg">
                ${formatMetric(
                  run.metrics.rxPowerAvgDbm
                )} dBm
              </td>
              <td data-label="Link Margin Avg">
                ${formatMetric(
                  run.metrics.linkMarginAvgDb
                )} dB
              </td>
              <td data-label="Link loss">
                ${run.metrics.lostByLinkBudget}
              </td>
              <td data-label="Random loss">
                ${run.metrics.lostByRandom}
              </td>
              <td data-label="ADR">
                ${run.configuration.adrEnabled
                  ? "ON"
                  : "OFF"}
              </td>
              <td data-label="Random p">
                ${run.configuration.randomLossEnabled
                  ? formatMetric(
                      run.configuration
                        .randomLossProbability,
                      2
                    )
                  : "-"}
              </td>
              <td data-label="Acción">
                <button
                  type="button"
                  class="mini-action danger"
                  data-remove-comparison="${run.id}"
                  title="Eliminar esta ejecución de la comparación"
                >
                  Eliminar
                </button>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderComparisonPercentBars(
  history,
  field,
  title,
  subtitle,
  suffix = "%"
) {
  return `
    <article class="comparison-chart-card">
      <h4>${escapeHtml(title)}</h4>
      <p>${escapeHtml(subtitle)}</p>

      <div class="comparison-bar-list">
        ${history.map(run => {
          const value =
            Number(
              run.metrics[field]
            ) || 0;

          const width =
            clampMetric(
              value,
              0,
              100
            );

          return `
            <div class="comparison-bar-row">
              <div class="comparison-bar-heading">
                <strong>
                  ${escapeHtml(run.label)}
                </strong>
                <span>
                  ${formatMetric(value)}${suffix}
                </span>
              </div>

              <div class="comparison-track">
                <span
                  class="comparison-fill"
                  style="width:${width}%"
                ></span>
              </div>
            </div>
          `;
        }).join("")}
      </div>
    </article>
  `;
}

function renderComparisonRadioBars(
  history,
  field,
  title,
  subtitle,
  min,
  max,
  unit
) {
  return `
    <article class="comparison-chart-card">
      <h4>${escapeHtml(title)}</h4>
      <p>${escapeHtml(subtitle)}</p>

      <div class="comparison-bar-list">
        ${history.map(run => {
          const value =
            Number(
              run.metrics[field]
            ) || 0;

          const bounded =
            clampMetric(
              value,
              min,
              max
            );

          const width =
            ((bounded - min)
              / (max - min))
            * 100;

          return `
            <div class="comparison-bar-row">
              <div class="comparison-bar-heading">
                <strong>
                  ${escapeHtml(run.label)}
                </strong>
                <span>
                  ${formatMetric(value)}
                  ${escapeHtml(unit)}
                </span>
              </div>

              <div class="comparison-track">
                <span
                  class="comparison-fill"
                  style="width:${width}%"
                ></span>
              </div>
            </div>
          `;
        }).join("")}
      </div>

      <small class="comparison-scale">
        Escala visual común:
        ${min} a ${max}
        ${escapeHtml(unit)}
      </small>
    </article>
  `;
}

function renderComparisonLossBars(history) {
  return `
    <article class="comparison-chart-card">
      <h4>Pérdidas por causa</h4>
      <p>
        Comparación entre pérdidas por presupuesto de enlace
        y pérdidas aleatorias.
      </p>

      <div class="comparison-bar-list">
        ${history.map(run => {
          const tx =
            Math.max(
              Number(
                run.metrics.txAttempts
              ) || 0,
              1
            );

          const linkLoss =
            Number(
              run.metrics
                .lostByLinkBudget
            ) || 0;

          const randomLoss =
            Number(
              run.metrics
                .lostByRandom
            ) || 0;

          const linkPct =
            (linkLoss * 100) / tx;

          const randomPct =
            (randomLoss * 100) / tx;

          return `
            <div class="comparison-bar-row">
              <div class="comparison-bar-heading">
                <strong>
                  ${escapeHtml(run.label)}
                </strong>
                <span>
                  Link ${linkLoss} · Random ${randomLoss}
                </span>
              </div>

              <div class="comparison-track comparison-loss-track">
                <span
                  class="comparison-loss-link"
                  style="width:${linkPct}%"
                  title="Link budget: ${linkLoss}"
                ></span>
                <span
                  class="comparison-loss-random"
                  style="width:${randomPct}%"
                  title="Aleatorias: ${randomLoss}"
                ></span>
              </div>
            </div>
          `;
        }).join("")}
      </div>

      <div class="bar-legend">
        <span>
          <i class="legend-dot legend-link"></i>
          Link budget
        </span>
        <span>
          <i class="legend-dot legend-random"></i>
          Aleatoria
        </span>
      </div>
    </article>
  `;
}

function renderScenarioComparison(history) {
  if (!history.length) {
    return `
      <div class="results-note">
        Todavía no hay ejecuciones guardadas.
        Ejecuta un escenario, escribe un nombre y utiliza
        <strong>Guardar resultado para comparar</strong>.
      </div>
    `;
  }

  return `
    ${renderComparisonWarning(history)}

    ${renderScenarioComparisonTable(history)}

    <div class="comparison-chart-grid">
      ${renderComparisonPercentBars(
        history,
        "pdr",
        "PDR por escenario",
        "Porcentaje de paquetes recibidos respecto al total evaluado."
      )}

      ${renderComparisonLossBars(history)}

      ${renderComparisonRadioBars(
        history,
        "rxPowerAvgDbm",
        "RxPower promedio por escenario",
        "Potencia recibida modelada agregada por ejecución.",
        -140,
        -40,
        "dBm"
      )}

      ${renderComparisonRadioBars(
        history,
        "linkMarginAvgDb",
        "Link Margin promedio por escenario",
        "Reserva promedio respecto a la sensibilidad del receptor.",
        -20,
        60,
        "dB"
      )}
    </div>
  `;
}

function onFileSelected(event) {
  const file = event.target.files[0];
  if (!file) return;

  state.file.rawFile = file;
  state.file.fileName = file.name;
  state.file.uploadedPath = "";
  state.file.inputPath = file.name;
  state.ui.uploadMessage = "";

  if (!isSupportedFile(file.name)) {
    state.errors = { inputFile: "Solo se permiten archivos .csv o .txt" };
  } else {
    state.errors = {};
  }

  render();
}

async function uploadSelectedFile() {
  if (!state.file.rawFile) {
    throw new Error("Selecciona un archivo antes de subirlo.");
  }

  if (!isSupportedFile(state.file.rawFile.name)) {
    throw new Error("Formato de archivo no permitido. Usa .csv o .txt");
  }

  const formData = new FormData();
  formData.append("file", state.file.rawFile);

  state.ui.uploadMessage = "Subiendo archivo...";
  render();

  const response = await fetch("/api/files/upload", {
    method: "POST",
    body: formData
  });

  const result = await response.json();

  if (!response.ok || !result.success) {
    throw new Error(result.message || "No se pudo subir el archivo.");
  }

  state.file.uploadedPath = normalizeSlashes(result.path);
  state.file.inputPath = state.file.uploadedPath;
  state.file.fileName = state.file.rawFile.name;
  state.ui.uploadMessage = `Archivo cargado: ${state.file.uploadedPath}`;

  render();
  return state.file.uploadedPath;
}

function buildSimulationPayload() {
    const inputFile = getResolvedInputFile();

    const payload = {
      inputFile,

      simulation: {
        rowsToProcess: Number(state.topology.rowsToProcess),
        sendIntervalMs: Number(state.topology.sendIntervalMs)
      },

      adrEnabled: Boolean(state.experiment.adrEnabled),
      randomLossEnabled: Boolean(state.experiment.randomLossEnabled),
      randomLossProbability: Number(state.experiment.randomLossProbability),

      linkBudgetParameters: {
        frequencyMHz: Number(state.propagation.frequencyMHz),
        los: Boolean(state.propagation.los),
        streetWidthMeters: Number(state.propagation.streetWidthMeters),
        baseStationHeightMeters: Number(state.propagation.baseStationHeightMeters),
        averageBuildingHeightMeters: Number(state.propagation.averageBuildingHeightMeters),
        mobileStationHeightMeters: Number(state.propagation.mobileStationHeightMeters),
        streetOrientationDegrees: Number(state.propagation.streetOrientationDegrees),
        buildingSeparationMeters: Number(state.propagation.buildingSeparationMeters),
        urbanEnvironment: state.propagation.urbanEnvironment
      },

      gateway: {
        gatewayId: state.topology.gateway.gatewayId,
        x: Number(state.topology.gateway.x),
        y: Number(state.topology.gateway.y),
        maxTxPowerDBm: Number(state.topology.gateway.maxTxPowerDBm),
        udpPort: Number(state.topology.gateway.udpPort),
        tcpPort: Number(state.topology.gateway.tcpPort)
      },

      layout: {
        mode: state.topology.layout.mode,
        baseX: Number(state.topology.layout.baseX),
        baseY: Number(state.topology.layout.baseY),
        distanceMeters: Number(state.topology.layout.distanceMeters)
      },

      devices: state.devices.map(device => ({
        deviceId: device.deviceId,

        enabled: true,

        config: device.config,

        fPort: Number(device.fPort),

        columnIndexes: [...device.columnIndexes],

        position: {
          x: Number(device.x),
          y: Number(device.y)
        },

        eirpDbm: device.eirpDbm === "" || device.eirpDbm == null
          ? null
          : Number(device.eirpDbm)
      }))
    };

    console.log("Payload de simulación:", payload);

    return payload;
  }

function validateFileStep() {
  const errors = {};
  const resolved = getResolvedInputFile();

  if (!resolved) {
    errors.inputFile = "Selecciona un archivo o escribe una ruta válida.";
  }

  if (state.file.rawFile && !isSupportedFile(state.file.rawFile.name)) {
    errors.inputFile = "Solo se permiten archivos .csv o .txt";
  }

  return errors;
}

function calculateLayoutPosition(index, count = state.topology.deviceCount) {
  const baseX = Number(state.topology.layout.baseX);
  const baseY = Number(state.topology.layout.baseY);
  const spacing = Number(state.topology.layout.distanceMeters);

  if (state.topology.layout.mode === "grid") {
    const columns = Math.max(1, Math.ceil(Math.sqrt(Math.max(count, 1))));
    return {
      x: baseX + (index % columns) * spacing,
      y: baseY + Math.floor(index / columns) * spacing
    };
  }

  return {
    x: baseX + index * spacing,
    y: baseY
  };
}

function buildDevices(count, existing = []) {
  return Array.from({ length: count }, (_, index) => {
    const current = existing[index];
    if (current) return current;

    const position = calculateLayoutPosition(index, count);

    return {
      deviceId: `device-${index + 1}`,
      config: "US915_CLASS_A",
      fPort: index + 1,
      x: position.x,
      y: position.y,
      eirpDbm: "",
      columnIndexes: []
    };
  });
}

function applyLayoutToDevices() {
  state.devices = buildDevices(state.topology.deviceCount, state.devices);
  state.devices.forEach((device, index) => {
    const position = calculateLayoutPosition(index, state.topology.deviceCount);
    device.x = position.x;
    device.y = position.y;
  });
  render();
}

function validateTopologyStep() {
  const errors = {};

  if (Number(state.topology.rowsToProcess) <= 0) {
    errors.rowsToProcess = "Debe ser mayor que 0.";
  }

  if (Number(state.topology.sendIntervalMs) < 0) {
    errors.sendIntervalMs = "No puede ser negativo.";
  }

  if (!String(state.topology.gateway.gatewayId || "").trim()) {
    errors.gatewayId = "Gateway ID obligatorio.";
  }

  if (Number(state.topology.gateway.udpPort) <= 0) {
    errors.udpPort = "Puerto UDP inválido.";
  }

  if (Number(state.topology.gateway.tcpPort) <= 0) {
    errors.tcpPort = "Puerto TCP inválido.";
  }

  if (Number(state.topology.deviceCount) <= 0) {
    errors.deviceCount = "Debe haber al menos 1 dispositivo.";
  }

  const experiment = state.experiment;

  if (!Number.isFinite(Number(experiment.randomLossProbability))
      || Number(experiment.randomLossProbability) < 0
      || Number(experiment.randomLossProbability) > 1) {
    errors.randomLossProbability =
      "La probabilidad debe estar entre 0.0 y 1.0 (por ejemplo, 0.30 = 30%).";
  }

  const p = state.propagation;

  if (Number(p.frequencyMHz) < 800 || Number(p.frequencyMHz) > 2000) {
    errors.frequencyMHz = "Para este modelo, usa una frecuencia entre 800 y 2000 MHz.";
  }

  if (Number(p.streetWidthMeters) <= 0) {
    errors.streetWidthMeters = "La anchura de calle debe ser mayor que 0 m.";
  }

  if (Number(p.baseStationHeightMeters) <= 0) {
    errors.baseStationHeightMeters = "La altura del gateway debe ser mayor que 0 m.";
  }

  if (Number(p.averageBuildingHeightMeters) <= 0) {
    errors.averageBuildingHeightMeters = "La altura media de edificios debe ser mayor que 0 m.";
  }

  if (Number(p.mobileStationHeightMeters) <= 0) {
    errors.mobileStationHeightMeters = "La altura del dispositivo debe ser mayor que 0 m.";
  }

  if (!p.los && Number(p.averageBuildingHeightMeters) <= Number(p.mobileStationHeightMeters)) {
    errors.averageBuildingHeightMeters = "En NLoS, la altura media de edificios debe superar la altura del dispositivo.";
  }

  if (Number(p.streetOrientationDegrees) <= 0 || Number(p.streetOrientationDegrees) >= 90) {
    errors.streetOrientationDegrees = "El ángulo debe ser mayor que 0° y menor que 90°.";
  }

  if (Number(p.buildingSeparationMeters) <= 0) {
    errors.buildingSeparationMeters = "La separación entre edificios debe ser mayor que 0 m.";
  }

  return errors;
}

function validateDevicesStep() {
  const errors = {};

  state.devices.forEach((device, index) => {
    if (!String(device.deviceId || "").trim()) {
      errors[`deviceId_${index}`] = "ID obligatorio.";
    }

    if (!String(device.config || "").trim()) {
      errors[`config_${index}`] = "Config obligatoria.";
    }

    if (Number(device.fPort) <= 0) {
      errors[`fPort_${index}`] = "FPort inválido.";
    }

    if (!Number.isFinite(Number(device.x))) {
      errors[`x_${index}`] = "La coordenada X debe ser numérica.";
    }

    if (!Number.isFinite(Number(device.y))) {
      errors[`y_${index}`] = "La coordenada Y debe ser numérica.";
    }

    if (device.eirpDbm !== "" && device.eirpDbm != null && !Number.isFinite(Number(device.eirpDbm))) {
      errors[`eirp_${index}`] = "La EIRP debe ser numérica o dejarse vacía.";
    }
  });

  return errors;
}

function analyzeLocalFile() {
  if (!state.file.rawFile) {
    state.errors = { inputFile: "Selecciona un archivo local para analizar." };
    render();
    return;
  }

  const reader = new FileReader();
  reader.onload = e => {
    const text = String(e.target.result || "");
    const delimiter = state.file.delimiter || ",";
    const lines = text.split(/\r?\n/).filter(Boolean);
    const rows = lines.slice(0, 6).map(line => line.split(delimiter));
    const maxCols = rows.reduce((max, row) => Math.max(max, row.length), 0);

    state.file.columns = Array.from({ length: maxCols }, (_, i) => ({
      index: i,
      name: state.file.hasHeader && rows[0]?.[i] ? rows[0][i] : `col_${i + 1}`
    }));

    state.file.previewRows = state.file.hasHeader ? rows.slice(1) : rows;
    state.file.rowCountEstimate = lines.length;
    state.errors = {};
    state.ui.uploadMessage = `Vista previa cargada (${lines.length} filas estimadas).`;
    render();
  };

  reader.readAsText(state.file.rawFile);
}

function renderPreviewTable() {
  if (!state.file.previewRows.length || !state.file.columns.length) {
    return `<p class="muted-text">Sin vista previa todavía.</p>`;
  }

  return `
    <div class="preview-table-wrap">
      <table class="preview-table">
        <thead>
          <tr>
            ${state.file.columns.map(col => `<th>${escapeHtml(col.name)}</th>`).join("")}
          </tr>
        </thead>
        <tbody>
          ${state.file.previewRows.slice(0, 5).map(row => `
            <tr>
              ${state.file.columns.map((_, i) => `<td>${escapeHtml(row[i] ?? "")}</td>`).join("")}
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderFileStep() {
  return `
    <section class="wizard-step">
      <h2>Archivo de entrada</h2>
      <p>Selecciona o escribe el archivo para la simulación.</p>

      <div class="field">
        <label for="inputPath">Ruta o nombre de archivo</label>
        <input id="inputPath" type="text" value="${escapeHtml(state.file.inputPath)}" placeholder="t5.csv o data/t5.csv" />
        ${state.errors.inputFile ? `<p class="error-text">${escapeHtml(state.errors.inputFile)}</p>` : ""}
      </div>

      <div class="field">
        <label for="fileInput">Archivo local</label>
        <input id="fileInput" type="file" accept=".csv,.txt" />
      </div>

      <div class="field-row">
        <div class="field">
          <label for="delimiter">Separador</label>
          <input id="delimiter" type="text" maxlength="1" value="${escapeHtml(state.file.delimiter)}" />
        </div>

        <div class="field">
          <label for="hasHeader">Encabezados</label>
          <select id="hasHeader">
            <option value="true" ${state.file.hasHeader ? "selected" : ""}>Sí</option>
            <option value="false" ${!state.file.hasHeader ? "selected" : ""}>No</option>
          </select>
        </div>
      </div>

      <div class="actions-row">
        <button id="analyzeBtn" type="button" class="primary-action">Analizar archivo</button>
      </div>
    </section>
  `;
}

function renderTopologyStep() {
  return `
    <section class="wizard-step">
      <h2>Topología</h2>
      <p>
        Configura la ejecución, la posición del gateway y la distribución espacial de los dispositivos.
        Estos parámetros determinan la geometría del escenario y algunos controles generales de la simulación.
      </p>

      <h3 class="section-title">Ejecución de la simulación</h3>
      <p class="section-help">
        Controla cuántos registros del archivo se procesan y el retardo adicional entre iteraciones.
      </p>

      <div class="field-row">
        <div class="field">
          <label for="rowsToProcess">Filas a procesar</label>
          <input id="rowsToProcess" type="number" min="1" value="${state.topology.rowsToProcess}" />
          <small class="field-help">
            Número máximo de filas de datos que se procesarán durante esta ejecución. Cada fila genera un mensaje por cada dispositivo habilitado.
          </small>
          ${state.errors.rowsToProcess ? `<p class="error-text">${escapeHtml(state.errors.rowsToProcess)}</p>` : ""}
        </div>

        <div class="field">
          <label for="sendIntervalMs">Intervalo de envío [ms]</label>
          <input id="sendIntervalMs" type="number" min="0" value="${state.topology.sendIntervalMs}" />
          <small class="field-help">
            Retardo adicional entre iteraciones de la simulación. No representa necesariamente el tiempo exacto entre uplinks, ya que las ventanas de recepción y los sockets también consumen tiempo.
          </small>
          ${state.errors.sendIntervalMs ? `<p class="error-text">${escapeHtml(state.errors.sendIntervalMs)}</p>` : ""}
        </div>
      </div>

      <hr class="section-divider" />
      <h3 class="section-title">Gateway</h3>
      <p class="section-help">
        Define la identidad, posición cartesiana y parámetros de comunicación del gateway.
      </p>

      <div class="field-row">
        <div class="field">
          <label for="gatewayId">Gateway ID</label>
          <input id="gatewayId" type="text" value="${escapeHtml(state.topology.gateway.gatewayId)}" />
          <small class="field-help">
            Identificador lógico del gateway utilizado para registrar y asociar las transmisiones dentro del simulador.
          </small>
          ${state.errors.gatewayId ? `<p class="error-text">${escapeHtml(state.errors.gatewayId)}</p>` : ""}
        </div>

        <div class="field">
          <label for="deviceCount">Cantidad de dispositivos</label>
          <input id="deviceCount" type="number" min="1" value="${state.topology.deviceCount}" />
          <small class="field-help">
            Número de end-devices que se crearán y configurarán en el escenario.
          </small>
          ${state.errors.deviceCount ? `<p class="error-text">${escapeHtml(state.errors.deviceCount)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="gatewayX">Gateway X [m]</label>
          <input id="gatewayX" type="number" value="${state.topology.gateway.x}" />
          <small class="field-help">
            Coordenada X del gateway. Junto con Gateway Y y la posición de cada dispositivo se utiliza para calcular la distancia geométrica del enlace.
          </small>
        </div>

        <div class="field">
          <label for="gatewayY">Gateway Y [m]</label>
          <input id="gatewayY" type="number" value="${state.topology.gateway.y}" />
          <small class="field-help">
            Coordenada Y del gateway en el plano de simulación.
          </small>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="maxTxPowerDBm">Potencia máxima TX del gateway [dBm]</label>
          <input id="maxTxPowerDBm" type="number" value="${state.topology.gateway.maxTxPowerDBm}" />
          <small class="field-help">
            Potencia máxima disponible para transmisiones del gateway. No se usa como potencia del uplink del end-device; el uplink utiliza la EIRP configurada para cada dispositivo.
          </small>
        </div>

        <div class="field">
          <label for="udpPort">Puerto UDP</label>
          <input id="udpPort" type="number" min="1" value="${state.topology.gateway.udpPort}" />
          <small class="field-help">
            Puerto local donde el servidor UDP del gateway recibe las transmisiones correspondientes al flujo configurado por el simulador.
          </small>
          ${state.errors.udpPort ? `<p class="error-text">${escapeHtml(state.errors.udpPort)}</p>` : ""}
        </div>
      </div>

      <div class="field">
        <label for="tcpPort">Puerto TCP</label>
        <input id="tcpPort" type="number" min="1" value="${state.topology.gateway.tcpPort}" />
        <small class="field-help">
          Puerto local utilizado por el servidor TCP del gateway para los flujos que emplean este mecanismo de comunicación.
        </small>
        ${state.errors.tcpPort ? `<p class="error-text">${escapeHtml(state.errors.tcpPort)}</p>` : ""}
      </div>

      <hr class="section-divider" />
      <h3 class="section-title">Distribución automática de dispositivos</h3>
      <p class="section-help">
        Estos campos son auxiliares del frontend: generan automáticamente las coordenadas X/Y de los dispositivos.
        No son parámetros del modelo COST231.
      </p>

      <div class="field-row">
        <div class="field">
          <label for="layoutMode">Distribución (layout)</label>
          <select id="layoutMode">
            <option value="linear" ${state.topology.layout.mode === "linear" ? "selected" : ""}>Lineal</option>
            <option value="grid" ${state.topology.layout.mode === "grid" ? "selected" : ""}>Cuadrícula</option>
          </select>
          <small class="field-help">
            Lineal coloca los dispositivos sobre una misma línea horizontal. Cuadrícula los distribuye en filas y columnas.
          </small>
        </div>

        <div class="field">
          <label for="distanceMeters">Separación entre nodos [m]</label>
          <input id="distanceMeters" type="number" min="1" value="${state.topology.layout.distanceMeters}" />
          <small class="field-help">
            Espaciado usado por el generador de layout. No equivale necesariamente a la distancia dispositivo–gateway.
          </small>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="baseX">Base X [m]</label>
          <input id="baseX" type="number" value="${state.topology.layout.baseX}" />
          <small class="field-help">
            Coordenada X inicial desde la que se genera la distribución automática. El primer dispositivo se coloca aquí.
          </small>
        </div>

        <div class="field">
          <label for="baseY">Base Y [m]</label>
          <input id="baseY" type="number" value="${state.topology.layout.baseY}" />
          <small class="field-help">
            Coordenada Y inicial desde la que se genera la distribución automática.
          </small>
        </div>
      </div>

      <div class="actions-row">
        <button id="applyLayoutBtn" type="button" class="secondary-action">Aplicar layout a dispositivos</button>
      </div>
      <small class="field-help">
        Calcula y escribe las coordenadas X/Y de todos los dispositivos usando Layout, Base X, Base Y y Separación entre nodos.
        Al aplicarlo se reemplazan las coordenadas manuales actuales de los dispositivos.
      </small>

      <hr class="section-divider" />
      <h3 class="section-title">Controles experimentales</h3>
      <p class="section-help">
        Estos controles modifican el comportamiento del experimento y no forman parte del modelo de propagación COST231.
      </p>

      <div class="field-row">
        <div class="field">
          <label for="adrEnabled">Adaptive Data Rate (ADR)</label>
          <select id="adrEnabled">
            <option value="true" ${state.experiment.adrEnabled ? "selected" : ""}>Activado</option>
            <option value="false" ${!state.experiment.adrEnabled ? "selected" : ""}>Desactivado</option>
          </select>
          <small class="field-help">
            Activa o desactiva la política ADR simplificada del simulador. Para pruebas de propagación aisladas conviene desactivarlo.
          </small>
        </div>

        <div class="field">
          <label for="randomLossEnabled">Pérdida aleatoria adicional</label>
          <select id="randomLossEnabled">
            <option value="false" ${!state.experiment.randomLossEnabled ? "selected" : ""}>Desactivada</option>
            <option value="true" ${state.experiment.randomLossEnabled ? "selected" : ""}>Activada</option>
          </select>
          <small class="field-help">
            Permite introducir pérdidas estadísticas independientes del presupuesto de enlace.
          </small>
        </div>
      </div>

      <div class="field">
        <label for="randomLossProbability">Probabilidad de pérdida aleatoria [0–1]</label>
        <input
          id="randomLossProbability"
          type="number"
          min="0"
          max="1"
          step="0.01"
          value="${state.experiment.randomLossProbability}"
          ${state.experiment.randomLossEnabled ? "" : "disabled"}
        />
        <small class="field-help">
          Valor decimal: 0.30 equivale a una probabilidad configurada del 30 %. Es un parámetro experimental, no normativo de LoRaWAN.
        </small>
        ${state.errors.randomLossProbability ? `<p class="error-text">${escapeHtml(state.errors.randomLossProbability)}</p>` : ""}
      </div>

      <hr class="section-divider" />
      <h3 class="section-title">Entorno de propagación — COST231 Walfisch-Ikegami</h3>
      <p class="section-help">Estos son parámetros físicos del escenario. Los coeficientes Lori, Lbsh, Ka, Kd y Kf se calculan internamente y no son editables.</p>

      <div class="field-row">
        <div class="field">
          <label for="frequencyMHz">Frecuencia de operación [MHz]</label>
          <input id="frequencyMHz" type="number" min="800" max="2000" step="0.1" value="${state.propagation.frequencyMHz}" />
          <small class="field-help">Frecuencia usada por COST231. Para escenarios LoRaWAN debe ser coherente con la configuración regional seleccionada; 900 MHz se utilizará en la prueba documental.</small>
          ${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
        </div>

        <div class="field">
          <label for="los">Condición del enlace</label>
          <select id="los">
            <option value="true" ${state.propagation.los ? "selected" : ""}>LoS — línea de vista</option>
            <option value="false" ${!state.propagation.los ? "selected" : ""}>NLoS — sin línea de vista</option>
          </select>
          <small class="field-help">Selecciona la rama de cálculo COST231 correspondiente a la visibilidad directa entre dispositivo y gateway.</small>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="streetWidthMeters">Anchura promedio de calle [m]</label>
          <input id="streetWidthMeters" type="number" min="0.1" step="0.1" value="${state.propagation.streetWidthMeters}" />
          <small class="field-help">Interviene en la pérdida de difracción tejado-calle Lrts.</small>
          ${state.errors.streetWidthMeters ? `<p class="error-text">${escapeHtml(state.errors.streetWidthMeters)}</p>` : ""}
        </div>

        <div class="field">
          <label for="buildingSeparationMeters">Separación promedio entre edificios [m]</label>
          <input id="buildingSeparationMeters" type="number" min="0.1" step="0.1" value="${state.propagation.buildingSeparationMeters}" />
          <small class="field-help">Representa B en el término de difracción multiscreen Lmsd.</small>
          ${state.errors.buildingSeparationMeters ? `<p class="error-text">${escapeHtml(state.errors.buildingSeparationMeters)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="baseStationHeightMeters">Altura de la antena del gateway [m]</label>
          <input id="baseStationHeightMeters" type="number" min="0.1" step="0.1" value="${state.propagation.baseStationHeightMeters}" />
          <small class="field-help">Altura de la estación base usada para calcular Lbsh, Ka y Kd.</small>
          ${state.errors.baseStationHeightMeters ? `<p class="error-text">${escapeHtml(state.errors.baseStationHeightMeters)}</p>` : ""}
        </div>

        <div class="field">
          <label for="averageBuildingHeightMeters">Altura promedio de edificios [m]</label>
          <input id="averageBuildingHeightMeters" type="number" min="0.1" step="0.1" value="${state.propagation.averageBuildingHeightMeters}" />
          <small class="field-help">Altura media de azoteas del escenario urbano; en NLoS debe ser mayor que la altura del dispositivo.</small>
          ${state.errors.averageBuildingHeightMeters ? `<p class="error-text">${escapeHtml(state.errors.averageBuildingHeightMeters)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="mobileStationHeightMeters">Altura de la antena del dispositivo [m]</label>
          <input id="mobileStationHeightMeters" type="number" min="0.1" step="0.1" value="${state.propagation.mobileStationHeightMeters}" />
          <small class="field-help">Altura del end-device empleada en Lrts.</small>
          ${state.errors.mobileStationHeightMeters ? `<p class="error-text">${escapeHtml(state.errors.mobileStationHeightMeters)}</p>` : ""}
        </div>

        <div class="field">
          <label for="streetOrientationDegrees">Orientación de la calle [°]</label>
          <input id="streetOrientationDegrees" type="number" min="0.1" max="89.9" step="0.1" value="${state.propagation.streetOrientationDegrees}" />
          <small class="field-help">Ángulo α del modelo. Se transforma internamente en el término Lori.</small>
          ${state.errors.streetOrientationDegrees ? `<p class="error-text">${escapeHtml(state.errors.streetOrientationDegrees)}</p>` : ""}
        </div>
      </div>

      <div class="field">
        <label for="urbanEnvironment">Tipo de entorno urbano</label>
        <select id="urbanEnvironment">
          <option value="MEDIUM_SIZE_CITY" ${state.propagation.urbanEnvironment === "MEDIUM_SIZE_CITY" ? "selected" : ""}>Ciudad de tamaño medio</option>
          <option value="DOWNTOWN" ${state.propagation.urbanEnvironment === "DOWNTOWN" ? "selected" : ""}>Centro urbano / downtown</option>
        </select>
        <small class="field-help">Determina la expresión usada por el modelo para Kf.</small>
      </div>
    </section>
  `;
}


function renderDevicesStep() {
  if (!state.devices.length) {
    state.devices = buildDevices(state.topology.deviceCount);
  }

  return `
    <section class="wizard-step">
      <h2>Dispositivos</h2>
      <p>Configura cada dispositivo LoRaWAN antes de ejecutar la simulación.</p>

      <div class="device-list">
        ${state.devices.map((device, index) => `
          <article class="device-card">
            <h3>Dispositivo ${index + 1}</h3>

            <div class="field-row">
              <div class="field">
                <label for="deviceId_${index}">Device ID</label>
                <input
                  id="deviceId_${index}"
                  data-index="${index}"
                  data-field="deviceId"
                  type="text"
                  value="${escapeHtml(device.deviceId)}"
                />
                ${state.errors[`deviceId_${index}`] ? `<p class="error-text">${escapeHtml(state.errors[`deviceId_${index}`])}</p>` : ""}
              </div>

              <div class="field">
                <label for="fPort_${index}">FPort</label>
                <input
                  id="fPort_${index}"
                  data-index="${index}"
                  data-field="fPort"
                  type="number"
                  min="1"
                  value="${device.fPort}"
                />
                ${state.errors[`fPort_${index}`] ? `<p class="error-text">${escapeHtml(state.errors[`fPort_${index}`])}</p>` : ""}
              </div>
            </div>

            <div class="field-row">
              <div class="field">
                <label for="config_${index}">Configuración LoRa</label>
                <select id="config_${index}" data-index="${index}" data-field="config">
                  <option value="US915_CLASS_A" ${device.config === "US915_CLASS_A" ? "selected" : ""}>US915 / CLASS_A</option>
                  <option value="US915_CLASS_B" ${device.config === "US915_CLASS_B" ? "selected" : ""}>US915 / CLASS_B</option>
                  <option value="US915_CLASS_C" ${device.config === "US915_CLASS_C" ? "selected" : ""}>US915 / CLASS_C</option>
                  <option value="EU868_CLASS_A" ${device.config === "EU868_CLASS_A" ? "selected" : ""}>EU868 / CLASS_A</option>
                  <option value="EU868_CLASS_B" ${device.config === "EU868_CLASS_B" ? "selected" : ""}>EU868 / CLASS_B</option>
                  <option value="EU868_CLASS_C" ${device.config === "EU868_CLASS_C" ? "selected" : ""}>EU868 / CLASS_C</option>
                  <option value="AS923_CLASS_A" ${device.config === "AS923_CLASS_A" ? "selected" : ""}>AS923 / CLASS_A</option>
                </select>
                ${state.errors[`config_${index}`] ? `<p class="error-text">${escapeHtml(state.errors[`config_${index}`])}</p>` : ""}
              </div>

              <div class="field">
                <label>Transporte de comunicación</label>
                <div class="read-only-field">
                  Determinado automáticamente por la clase/configuración LoRaWAN seleccionada.
                </div>
                <small class="field-help">
                  No es un parámetro editable independiente: la lógica de Device selecciona el mecanismo de comunicación según la clase configurada.
                </small>
              </div>
            </div>

            <div class="field-row">
              <div class="field">
                <label for="x_${index}">Posición X [m]</label>
                <input id="x_${index}" data-index="${index}" data-field="x" type="number" step="0.1" value="${device.x}" />
                <small class="field-help">Coordenada horizontal del dispositivo. Junto con Y determina la distancia real al gateway.</small>
                ${state.errors[`x_${index}`] ? `<p class="error-text">${escapeHtml(state.errors[`x_${index}`])}</p>` : ""}
              </div>

              <div class="field">
                <label for="y_${index}">Posición Y [m]</label>
                <input id="y_${index}" data-index="${index}" data-field="y" type="number" step="0.1" value="${device.y}" />
                <small class="field-help">Coordenada vertical del dispositivo en la topología simulada.</small>
                ${state.errors[`y_${index}`] ? `<p class="error-text">${escapeHtml(state.errors[`y_${index}`])}</p>` : ""}
              </div>
            </div>

            <div class="field">
              <label for="eirp_${index}">EIRP del dispositivo [dBm]</label>
              <input id="eirp_${index}" data-index="${index}" data-field="eirpDbm" type="number" step="0.1" value="${device.eirpDbm ?? ""}" placeholder="Vacío = valor regional" />
              <small class="field-help">Potencia radiada usada en el uplink. Si se deja vacía, el backend utiliza el valor regional definido por LoRaConfig.</small>
              ${state.errors[`eirp_${index}`] ? `<p class="error-text">${escapeHtml(state.errors[`eirp_${index}`])}</p>` : ""}
            </div>

            <div class="field">
              <label for="columns_${index}">Columnas del payload</label>
              <input
                id="columns_${index}"
                data-index="${index}"
                data-field="columns"
                type="text"
                value="${escapeHtml((device.columnIndexes || []).join(","))}"
                placeholder="0,1,2"
              />
            </div>
          </article>
        `).join("")}
      </div>
    </section>
  `;
}


function getResultMetrics() {
  return Array.isArray(state.result?.metrics)
    ? state.result.metrics
    : [];
}

function compareDeviceMetrics(a, b) {
  return String(a?.deviceId || "")
    .localeCompare(
      String(b?.deviceId || ""),
      undefined,
      {
        numeric: true,
        sensitivity: "base"
      }
    );
}

function getSortedResultMetrics() {
  return [...getResultMetrics()]
    .sort(compareDeviceMetrics);
}

function getObservedProcessedRows(metrics) {
  if (!metrics.length) {
    return 0;
  }

  return Math.max(
    ...metrics.map(metric =>
      Number(metric.originalMessages) || 0
    )
  );
}

function getExecutionSummary() {
  const summary =
    state.result?.summary || {};

  return {
    rowsProcessed:
      Number(
        summary.rowsProcessed
        ?? state.result?.rowsProcessed
        ?? 0
      ),

    rowsSkipped:
      Number(
        summary.rowsSkipped
        ?? state.result?.rowsSkipped
        ?? 0
      ),

    devicesConfigured:
      Number(
        summary.devicesConfigured
        ?? state.result?.devicesConfigured
        ?? 0
      ),

    uplinksSent:
      Number(
        summary.uplinksSent
        ?? 0
      ),

    uplinksFailed:
      Number(
        summary.uplinksFailed
        ?? 0
      ),

    downlinksReceived:
      Number(
        summary.downlinksReceived
        ?? 0
      ),

    durationMs:
      Number(
        summary.durationMs
        ?? 0
      )
  };
}

function formatMetric(value, digits = 2) {
  const number = Number(value);
  return Number.isFinite(number)
    ? number.toFixed(digits)
    : "-";
}

function clampMetric(value, min, max) {
  return Math.min(max, Math.max(min, Number(value) || 0));
}

function calculateGlobalMetrics(metrics) {
  const totals = metrics.reduce((acc, metric) => {
    acc.txAttempts += Number(metric.txAttempts) || 0;
    acc.originalMessages += Number(metric.originalMessages) || 0;
    acc.rx += Number(metric.rx) || 0;
    acc.lost += Number(metric.lost) || 0;
    acc.lostByLinkBudget += Number(metric.lostByLinkBudget) || 0;
    acc.lostByRandom += Number(metric.lostByRandom) || 0;
    acc.retransmissions += Number(metric.retransmissions) || 0;
    return acc;
  }, {
    txAttempts: 0,
    originalMessages: 0,
    rx: 0,
    lost: 0,
    lostByLinkBudget: 0,
    lostByRandom: 0,
    retransmissions: 0
  });

  const deliveredTotal = totals.rx + totals.lost;

  totals.pdr = deliveredTotal > 0
    ? (totals.rx * 100) / deliveredTotal
    : 0;

  totals.deliveryRate = totals.originalMessages > 0
    ? (totals.rx * 100) / totals.originalMessages
    : 0;

  totals.rxPowerAvg = metrics.length
    ? metrics.reduce((sum, metric) => sum + (Number(metric.rssiAvg) || 0), 0) / metrics.length
    : 0;

  totals.linkMarginAvg = metrics.length
    ? metrics.reduce((sum, metric) => sum + (Number(metric.linkMarginAvg) || 0), 0) / metrics.length
    : 0;

  totals.throughputKbps = metrics.reduce(
    (sum, metric) => sum + (Number(metric.throughputKbps) || 0),
    0
  );

  totals.latencyAvgMs = metrics.length
    ? metrics.reduce((sum, metric) => sum + (Number(metric.latencyAvgMs) || 0), 0) / metrics.length
    : 0;

  return totals;
}

function renderMetricKpis(metrics) {
  const totals = calculateGlobalMetrics(metrics);

  return `
    <div class="metric-kpi-grid">
      <article class="metric-kpi">
        <span class="metric-kpi-label">PDR global</span>
        <strong>${formatMetric(totals.pdr)}%</strong>
        <small>${totals.rx} recibidos de ${totals.rx + totals.lost}</small>
      </article>

      <article class="metric-kpi">
        <span class="metric-kpi-label">Paquetes perdidos</span>
        <strong>${totals.lost}</strong>
        <small>Link budget: ${totals.lostByLinkBudget} · Aleatorios: ${totals.lostByRandom}</small>
      </article>

      <article class="metric-kpi">
        <span class="metric-kpi-label">RxPower promedio</span>
        <strong>${formatMetric(totals.rxPowerAvg)} dBm</strong>
        <small>Potencia recibida modelada</small>
      </article>

      <article class="metric-kpi">
        <span class="metric-kpi-label">Link Margin promedio</span>
        <strong>${formatMetric(totals.linkMarginAvg)} dB</strong>
        <small>Reserva respecto a sensibilidad</small>
      </article>

      <article class="metric-kpi">
        <span class="metric-kpi-label">Throughput agregado</span>
        <strong>${formatMetric(totals.throughputKbps)} kbps</strong>
        <small>Suma de dispositivos</small>
      </article>

      <article class="metric-kpi">
        <span class="metric-kpi-label">Latencia promedio</span>
        <strong>${formatMetric(totals.latencyAvgMs)} ms</strong>
        <small>Métrica de ejecución local</small>
      </article>
    </div>
  `;
}

function renderDeliveryBars(metrics) {
  return `
    <div class="metric-chart-list">
      ${metrics.map(metric => {
        const rx = Number(metric.rx) || 0;
        const linkLost = Number(metric.lostByLinkBudget) || 0;
        const randomLost = Number(metric.lostByRandom) || 0;
        const total = Math.max(rx + linkLost + randomLost, 1);

        const rxPct = (rx * 100) / total;
        const linkPct = (linkLost * 100) / total;
        const randomPct = (randomLost * 100) / total;

        return `
          <div class="metric-chart-row">
            <div class="metric-chart-heading">
              <strong>${escapeHtml(metric.deviceId || "device")}</strong>
              <span>PDR ${formatMetric(metric.pdr)}%</span>
            </div>
            <div class="stacked-bar" aria-label="Distribución de recepción y pérdidas">
              <span class="bar-segment bar-rx" style="width:${rxPct}%"
                    title="Recibidos: ${rx}"></span>
              <span class="bar-segment bar-link-loss" style="width:${linkPct}%"
                    title="Pérdida por link budget: ${linkLost}"></span>
              <span class="bar-segment bar-random-loss" style="width:${randomPct}%"
                    title="Pérdida aleatoria: ${randomLost}"></span>
            </div>
            <div class="bar-legend">
              <span><i class="legend-dot legend-rx"></i>Rx ${rx}</span>
              <span><i class="legend-dot legend-link"></i>Link budget ${linkLost}</span>
              <span><i class="legend-dot legend-random"></i>Aleatoria ${randomLost}</span>
            </div>
          </div>
        `;
      }).join("")}
    </div>
  `;
}

function renderRadioGauges(metrics) {
  return `
    <div class="radio-gauge-grid">
      ${metrics.map(metric => {
        const rxPower = Number(metric.rssiAvg) || 0;
        const margin = Number(metric.linkMarginAvg) || 0;

        // Escalas visuales solamente. No modifican ni reinterpretan el cálculo backend.
        const rxPct = ((clampMetric(rxPower, -140, -40) + 140) / 100) * 100;
        const marginPct = ((clampMetric(margin, -20, 60) + 20) / 80) * 100;

        return `
          <article class="radio-gauge-card">
            <h4>${escapeHtml(metric.deviceId || "device")}</h4>

            <div class="gauge-block">
              <div class="gauge-label">
                <span>RxPower promedio</span>
                <strong>${formatMetric(rxPower)} dBm</strong>
              </div>
              <div class="metric-gauge">
                <span style="width:${rxPct}%"></span>
              </div>
              <small>Escala visual: −140 a −40 dBm</small>
            </div>

            <div class="gauge-block">
              <div class="gauge-label">
                <span>Link Margin promedio</span>
                <strong>${formatMetric(margin)} dB</strong>
              </div>
              <div class="metric-gauge">
                <span style="width:${marginPct}%"></span>
              </div>
              <small>Escala visual: −20 a 60 dB</small>
            </div>
          </article>
        `;
      }).join("")}
    </div>
  `;
}

function renderDeviceMetricsTable(metrics) {
  return `
    <div class="metrics-table-wrapper compact-table-wrapper">
      <table class="metrics-table compact-device-table responsive-data-table">
        <thead>
          <tr>
            <th>Dispositivo</th>
            <th>Tx / Rx</th>
            <th>PDR</th>
            <th>Distancia</th>
            <th>Path Loss</th>
            <th>RxPower</th>
            <th>Link Margin</th>
            <th>Pérdidas</th>
          </tr>
        </thead>
        <tbody>
          ${metrics.map(metric => `
            <tr>
              <td data-label="Dispositivo">
                <strong>${escapeHtml(metric.deviceId || "-")}</strong>
              </td>

              <td data-label="Tx / Rx">
                ${metric.txAttempts ?? 0}
                /
                ${metric.rx ?? 0}
              </td>

              <td data-label="PDR">
                ${formatMetric(metric.pdr)}%
              </td>

              <td data-label="Distancia">
                ${formatMetric(metric.distanceMeters, 0)} m
              </td>

              <td data-label="Path Loss">
                ${formatMetric(metric.pathLossDb)} dB
              </td>

              <td data-label="RxPower">
                ${formatMetric(metric.rxPowerDbm)} dBm
              </td>

              <td data-label="Link Margin">
                ${formatMetric(metric.radioLinkMarginAvgDb)} dB
              </td>

              <td data-label="Pérdidas">
                ${metric.lost ?? 0}
                <small class="table-secondary">
                  LB ${metric.lostByLinkBudget ?? 0}
                  · R ${metric.lostByRandom ?? 0}
                </small>
              </td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}


function renderDetailedDeviceCards(metrics) {
  return `
    <div class="device-results-grid">
      ${metrics.map(metric => `
        <article class="device-result-card">
          <h4>${escapeHtml(metric.deviceId || "device")}</h4>
          <dl>
            <div><dt>Intentos TX</dt><dd>${metric.txAttempts ?? 0}</dd></div>
            <div><dt>Mensajes originales</dt><dd>${metric.originalMessages ?? 0}</dd></div>
            <div><dt>Retransmisiones</dt><dd>${metric.retransmissions ?? 0}</dd></div>
            <div><dt>Recibidos</dt><dd>${metric.rx ?? 0}</dd></div>
            <div><dt>Perdidos</dt><dd>${metric.lost ?? 0}</dd></div>
            <div><dt>Link budget</dt><dd>${metric.lostByLinkBudget ?? 0}</dd></div>
            <div><dt>Aleatorios</dt><dd>${metric.lostByRandom ?? 0}</dd></div>
            <div><dt>Delivery Rate</dt><dd>${formatMetric(metric.deliveryRate)}%</dd></div>
            <div><dt>Throughput</dt><dd>${formatMetric(metric.throughputKbps)} kbps</dd></div>
            <div><dt>Latencia</dt><dd>${formatMetric(metric.latencyAvgMs)} ms</dd></div>
            <div><dt>ACK recibidos</dt><dd>${metric.ackReceived ?? 0}</dd></div>
            <div><dt>Éxito confirmados</dt><dd>${formatMetric(metric.confirmedSuccessRate)}%</dd></div>
          </dl>
        </article>
      `).join("")}
    </div>
  `;
}



function sanitizeFileName(value) {
  return String(value || "resultado")
    .trim()
    .replace(/[^\p{L}\p{N}_-]+/gu, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 80)
    || "resultado";
}

function csvCell(value) {
  const text =
    String(value ?? "");

  return `"${text.replace(/"/g, '""')}"`;
}

function downloadTextFile(
  fileName,
  content,
  mimeType
) {
  const blob =
    new Blob(
      [content],
      {
        type: mimeType
      }
    );

  const url =
    URL.createObjectURL(blob);

  const link =
    document.createElement("a");

  link.href = url;
  link.download = fileName;

  document.body.appendChild(link);
  link.click();
  link.remove();

  URL.revokeObjectURL(url);
}

function getCurrentExportLabel() {
  return sanitizeFileName(
    state.comparison.scenarioLabel
    || state.result?.scenarioName
    || "simulacion"
  );
}

function exportCurrentResultJson() {
  if (!state.result) {
    return;
  }

  const data = {
    exportedAt:
      new Date().toISOString(),

    payload:
      state.lastExecutedPayload,

    result:
      state.result
  };

  downloadTextFile(
    `${getCurrentExportLabel()}_resultado.json`,
    JSON.stringify(
      data,
      null,
      2
    ),
    "application/json;charset=utf-8"
  );
}

function exportCurrentMetricsCsv() {
  const metrics =
    getSortedResultMetrics();

  if (!metrics.length) {
    return;
  }

  const header = [
    "deviceId",
    "txAttempts",
    "originalMessages",
    "retransmissions",
    "rx",
    "lost",
    "lostByLinkBudget",
    "lostByRandom",
    "pdrPercent",
    "deliveryRatePercent",
    "distanceMeters",
    "pathLossDb",
    "rxPowerDbm",
    "linkMarginDb",
    "throughputKbps",
    "latencyAvgMs"
  ];

  const rows =
    metrics.map(metric => [
      metric.deviceId,
      metric.txAttempts,
      metric.originalMessages,
      metric.retransmissions,
      metric.rx,
      metric.lost,
      metric.lostByLinkBudget,
      metric.lostByRandom,
      formatMetric(metric.pdr),
      formatMetric(metric.deliveryRate),
      formatMetric(metric.distanceMeters),
      formatMetric(metric.pathLossDb),
      formatMetric(metric.rxPowerDbm),
      formatMetric(metric.radioLinkMarginAvgDb),
      formatMetric(metric.throughputKbps),
      formatMetric(metric.latencyAvgMs)
    ]);

  const csv =
    "\uFEFF"
    + [
        header,
        ...rows
      ]
      .map(row =>
        row
          .map(csvCell)
          .join(",")
      )
      .join("\r\n");

  downloadTextFile(
    `${getCurrentExportLabel()}_metricas.csv`,
    csv,
    "text/csv;charset=utf-8"
  );
}

function exportComparisonJson() {
  if (!state.comparison.history.length) {
    return;
  }

  const data = {
    exportedAt:
      new Date().toISOString(),

    scenarios:
      state.comparison.history
  };

  downloadTextFile(
    "comparacion_escenarios.json",
    JSON.stringify(
      data,
      null,
      2
    ),
    "application/json;charset=utf-8"
  );
}

function exportComparisonCsv() {
  const history =
    state.comparison.history;

  if (!history.length) {
    return;
  }

  const header = [
    "scenario",
    "deviceCount",
    "pdrPercent",
    "rxPowerAvgDbm",
    "linkMarginAvgDb",
    "lostByLinkBudget",
    "lostByRandom",
    "adrEnabled",
    "randomLossEnabled",
    "randomLossProbability",
    "frequencyMHz",
    "condition"
  ];

  const rows =
    history.map(run => [
      run.label,
      run.configuration.deviceCount,
      formatMetric(run.metrics.pdr),
      formatMetric(
        run.metrics.rxPowerAvgDbm
      ),
      formatMetric(
        run.metrics.linkMarginAvgDb
      ),
      run.metrics.lostByLinkBudget,
      run.metrics.lostByRandom,
      run.configuration.adrEnabled,
      run.configuration.randomLossEnabled,
      formatMetric(
        run.configuration
          .randomLossProbability
      ),
      formatMetric(
        run.configuration.frequencyMHz,
        1
      ),
      run.configuration.los
        ? "LoS"
        : "NLoS"
    ]);

  const csv =
    "\uFEFF"
    + [
        header,
        ...rows
      ]
      .map(row =>
        row
          .map(csvCell)
          .join(",")
      )
      .join("\r\n");

  downloadTextFile(
    "comparacion_escenarios.csv",
    csv,
    "text/csv;charset=utf-8"
  );
}

function printResultsView() {
  window.print();
}

function getPropagationMetrics() {
  return getSortedResultMetrics()
    .filter(metric =>
      Number.isFinite(Number(metric.distanceMeters))
      && Number(metric.radioSamples) > 0
    )
    .sort((a, b) =>
      Number(a.distanceMeters)
      - Number(b.distanceMeters)
    );
}

function renderSimpleLineChart({
  title,
  yLabel,
  unit,
  field,
  metrics,
  valueFormatter = value => formatMetric(value)
}) {
  if (!metrics.length) {
    return "";
  }

  const width = 760;
  const height = 300;
  const left = 66;
  const right = 24;
  const top = 28;
  const bottom = 58;

  const plotWidth =
    width - left - right;

  const plotHeight =
    height - top - bottom;

  const xValues =
    metrics.map(metric =>
      Number(metric.distanceMeters));

  const yValues =
    metrics.map(metric =>
      Number(metric[field]));

  const xMin =
    Math.min(...xValues);

  const xMax =
    Math.max(...xValues);

  let yMin =
    Math.min(...yValues);

  let yMax =
    Math.max(...yValues);

  if (yMin === yMax) {
    yMin -= 1;
    yMax += 1;
  }

  const yPadding =
    Math.max(
      (yMax - yMin) * 0.10,
      1
    );

  yMin -= yPadding;
  yMax += yPadding;

  const scaleX = value => {
    if (xMax === xMin) {
      return left + plotWidth / 2;
    }

    return left
      + ((value - xMin) / (xMax - xMin))
      * plotWidth;
  };

  const scaleY = value =>
    top
    + (1 - ((value - yMin) / (yMax - yMin)))
    * plotHeight;

  const points =
    metrics.map(metric =>
      `${scaleX(Number(metric.distanceMeters))},${scaleY(Number(metric[field]))}`
    ).join(" ");

  const yTicks =
    Array.from({ length: 5 }, (_, index) => {
      const fraction = index / 4;
      const value =
        yMax - fraction * (yMax - yMin);

      const y =
        top + fraction * plotHeight;

      return `
        <line
          x1="${left}"
          y1="${y}"
          x2="${width - right}"
          y2="${y}"
          class="chart-grid-line"
        />
        <text
          x="${left - 10}"
          y="${y + 4}"
          text-anchor="end"
          class="chart-axis-text"
        >${escapeHtml(valueFormatter(value))}</text>
      `;
    }).join("");

  const xTicks =
    metrics.map(metric => {
      const x =
        scaleX(Number(metric.distanceMeters));

      return `
        <line
          x1="${x}"
          y1="${top + plotHeight}"
          x2="${x}"
          y2="${top + plotHeight + 5}"
          class="chart-axis-line"
        />
        <text
          x="${x}"
          y="${top + plotHeight + 21}"
          text-anchor="middle"
          class="chart-axis-text"
        >${formatMetric(metric.distanceMeters, 0)}</text>
      `;
    }).join("");

  const dots =
    metrics.map(metric => {
      const x =
        scaleX(Number(metric.distanceMeters));

      const y =
        scaleY(Number(metric[field]));

      return `
        <circle
          cx="${x}"
          cy="${y}"
          r="5"
          class="chart-point"
        >
          <title>
            ${escapeHtml(metric.deviceId || "device")} ·
            ${formatMetric(metric.distanceMeters, 0)} m ·
            ${valueFormatter(metric[field])} ${unit}
          </title>
        </circle>
      `;
    }).join("");

  return `
    <article class="propagation-chart-card">
      <div class="propagation-chart-title">
        <h4>${escapeHtml(title)}</h4>
        <small>${escapeHtml(yLabel)} respecto a distancia dispositivo–gateway</small>
      </div>

      <div class="propagation-chart-scroll">
        <svg
          viewBox="0 0 ${width} ${height}"
          role="img"
          aria-label="${escapeHtml(title)}"
          class="propagation-chart"
        >
          ${yTicks}

          <line
            x1="${left}"
            y1="${top}"
            x2="${left}"
            y2="${top + plotHeight}"
            class="chart-axis-line"
          />

          <line
            x1="${left}"
            y1="${top + plotHeight}"
            x2="${width - right}"
            y2="${top + plotHeight}"
            class="chart-axis-line"
          />

          ${xTicks}

          <polyline
            points="${points}"
            class="chart-data-line"
          />

          ${dots}

          <text
            x="${left + plotWidth / 2}"
            y="${height - 10}"
            text-anchor="middle"
            class="chart-axis-title"
          >Distancia [m]</text>

          <text
            transform="translate(16 ${top + plotHeight / 2}) rotate(-90)"
            text-anchor="middle"
            class="chart-axis-title"
          >${escapeHtml(yLabel)} [${escapeHtml(unit)}]</text>
        </svg>
      </div>
    </article>
  `;
}

function renderPropagationCharts() {
  const metrics =
    getPropagationMetrics();

  if (!metrics.length) {
    return `
      <div class="results-note warning">
        <strong>Datos de propagación no disponibles.</strong>
        El backend todavía no entregó <code>distanceMeters</code>,
        <code>pathLossDb</code>, <code>rxPowerDbm</code> y
        <code>radioLinkMarginAvgDb</code>.
      </div>
    `;
  }

  if (metrics.length < 2) {
    return `
      <div class="results-note warning">
        <strong>Se recibió un único punto de propagación.</strong>
        Configura al menos dos dispositivos a distancias diferentes para
        obtener una curva comparativa.
      </div>
      ${renderSimpleLineChart({
        title: "Pérdida COST231",
        yLabel: "Path Loss",
        unit: "dB",
        field: "pathLossDb",
        metrics
      })}
      ${renderSimpleLineChart({
        title: "Potencia recibida",
        yLabel: "RxPower",
        unit: "dBm",
        field: "rxPowerDbm",
        metrics
      })}
      ${renderSimpleLineChart({
        title: "Margen de enlace",
        yLabel: "Link Margin",
        unit: "dB",
        field: "radioLinkMarginAvgDb",
        metrics
      })}
    `;
  }

  return `
    <div class="propagation-chart-grid">
      ${renderSimpleLineChart({
        title: "Pérdida COST231 vs distancia",
        yLabel: "Path Loss",
        unit: "dB",
        field: "pathLossDb",
        metrics
      })}

      ${renderSimpleLineChart({
        title: "RxPower vs distancia",
        yLabel: "RxPower",
        unit: "dBm",
        field: "rxPowerDbm",
        metrics
      })}

      ${renderSimpleLineChart({
        title: "Link Margin vs distancia",
        yLabel: "Link Margin",
        unit: "dB",
        field: "radioLinkMarginAvgDb",
        metrics
      })}
    </div>
  `;
}

function renderResultsStep() {
  const currentPayload =
    buildSimulationPayload();

  const payload =
    state.result
    && state.lastExecutedPayload
      ? state.lastExecutedPayload
      : currentPayload;

  const metrics =
    getSortedResultMetrics();

  const hasMetrics =
    metrics.length > 0;

  const comparisonHistory =
    state.comparison.history;

  const executionSummary =
    getExecutionSummary();

  const observedProcessedRows =
    getObservedProcessedRows(metrics);

  const backendProcessedRows =
    executionSummary.rowsProcessed;

  const hasRowsProcessedMismatch =
    Boolean(
      state.result?.success
      && observedProcessedRows > 0
      && backendProcessedRows
        !== observedProcessedRows
    );

  return `
    <section class="wizard-step">
      <h2>Resultados</h2>
      <p>
        Ejecuta la simulación y consulta las métricas entregadas por el backend.
        Las métricas radio mostradas corresponden al modelo y al presupuesto de enlace configurados.
      </p>

      <div class="results-grid">
        <article class="summary-card">
          <h3>Configuración ejecutada</h3>
          <ul class="summary-list">
            <li><strong>Archivo:</strong> ${escapeHtml(payload.inputFile || "-")}</li>
            <li><strong>Filas a procesar:</strong> ${payload.simulation.rowsToProcess}</li>
            <li><strong>Intervalo:</strong> ${payload.simulation.sendIntervalMs} ms</li>
            <li><strong>Gateway:</strong> ${escapeHtml(payload.gateway.gatewayId)}</li>
            <li><strong>Dispositivos:</strong> ${payload.devices.length}</li>
            <li><strong>ADR:</strong> ${payload.adrEnabled ? "Activado" : "Desactivado"}</li>
            <li><strong>Pérdida aleatoria:</strong> ${payload.randomLossEnabled ? `Activada (p=${payload.randomLossProbability})` : "Desactivada"}</li>
            <li><strong>Modelo:</strong> COST231 Walfisch-Ikegami</li>
            <li><strong>Frecuencia:</strong> ${payload.linkBudgetParameters.frequencyMHz} MHz</li>
            <li><strong>Condición:</strong> ${payload.linkBudgetParameters.los ? "LoS" : "NLoS"}</li>
          </ul>
        </article>

        <article class="summary-card">
          <h3>Estado de ejecución</h3>
          <p>${state.ui.busy ? "Ejecutando simulación..." : "Listo para ejecutar."}</p>
          ${state.result ? `
            <div class="result-box ${state.result.success ? "success" : "error"}">
              <p><strong>Éxito:</strong> ${state.result.success ? "Sí" : "No"}</p>
              <p><strong>Escenario:</strong> ${escapeHtml(state.result.scenarioName || "Escenario personalizado")}</p>
              <p><strong>Procesadas (backend):</strong> ${executionSummary.rowsProcessed}</p>
              <p><strong>Omitidas:</strong> ${executionSummary.rowsSkipped}</p>
              <p><strong>Dispositivos:</strong> ${executionSummary.devicesConfigured || payload.devices.length}</p>
              ${executionSummary.durationMs > 0 ? `
                <p><strong>Duración:</strong> ${executionSummary.durationMs} ms</p>
              ` : ""}
              <p><strong>Mensaje:</strong> ${escapeHtml(state.result.message || "-")}</p>
            </div>
          ` : `<p>Aún no hay resultado.</p>`}
        </article>
      </div>

      ${hasRowsProcessedMismatch ? `
        <div class="results-note warning consistency-warning">
          <strong>Inconsistencia detectada en rowsProcessed.</strong>
          La respuesta del backend reportó
          <code>${backendProcessedRows}</code>
          filas procesadas, mientras que las métricas registran
          <code>${observedProcessedRows}</code>
          mensajes originales por dispositivo.
          El frontend no corrige este valor automáticamente.
        </div>
      ` : ""}

      <div class="actions-row results-toolbar">
        <button id="runSimulationBtn" type="button" class="primary-action" ${state.ui.busy ? "disabled" : ""}>
          ${state.ui.busy ? "Ejecutando..." : "Ejecutar simulación"}
        </button>

        ${state.result?.success && hasMetrics ? `
          <button
            id="exportMetricsCsvBtn"
            type="button"
            class="secondary-action"
          >
            Exportar métricas CSV
          </button>

          <button
            id="exportResultJsonBtn"
            type="button"
            class="secondary-action"
          >
            Exportar resultado JSON
          </button>

          <button
            id="printResultsBtn"
            type="button"
            class="secondary-action"
          >
            Imprimir / Guardar PDF
          </button>
        ` : ""}
      </div>

      ${state.result?.success && hasMetrics ? `
        <section class="results-section">
          <div class="results-section-heading">
            <div>
              <h3>Resumen de rendimiento</h3>
              <p>Métricas agregadas a partir de los dispositivos configurados.</p>
            </div>
          </div>
          ${renderMetricKpis(metrics)}
        </section>

        <section class="results-section">
          <div class="results-section-heading">
            <div>
              <h3>Recepción y causas de pérdida</h3>
              <p>
                Cada barra separa paquetes recibidos, pérdidas por presupuesto de enlace
                y pérdidas aleatorias.
              </p>
            </div>
          </div>
          ${renderDeliveryBars(metrics)}
        </section>

        <section class="results-section">
          <div class="results-section-heading">
            <div>
              <h3>Rendimiento radio por dispositivo</h3>
              <p>
                RxPower corresponde a la potencia recibida modelada. Link Margin representa
                la reserva respecto a la sensibilidad del receptor.
              </p>
            </div>
          </div>
          ${renderRadioGauges(metrics)}
        </section>

        <section class="results-section">
          <div class="results-section-heading">
            <div>
              <h3>Propagación COST231 por distancia</h3>
              <p>
                Las curvas utilizan exclusivamente resultados calculados por el backend;
                el frontend no vuelve a evaluar las ecuaciones COST231.
              </p>
            </div>
          </div>
          ${renderPropagationCharts()}
        </section>

        <section class="results-section">
          <div class="results-section-heading">
            <div>
              <h3>Métricas por dispositivo</h3>
              <p>Resumen numérico utilizado también para reportes y análisis posteriores.</p>
            </div>
          </div>
          ${renderDeviceMetricsTable(metrics)}
          ${renderDetailedDeviceCards(metrics)}
        </section>

        <section class="results-section comparison-section">
          <div class="results-section-heading">
            <div>
              <h3>Comparación entre ejecuciones</h3>
              <p>
                Guarda resultados completos para comparar escenarios sin
                recalcular las métricas en el frontend.
              </p>
            </div>
          </div>

          <div class="comparison-save-panel">
            <div class="field comparison-name-field">
              <label for="comparisonScenarioLabel">
                Nombre del escenario / ejecución
              </label>
              <input
                id="comparisonScenarioLabel"
                type="text"
                maxlength="80"
                value="${escapeHtml(state.comparison.scenarioLabel)}"
                placeholder="Ej. A — Referencia favorable"
              />
              <small class="field-help">
                Usa nombres consistentes como A, B3, C y D para las
                comparaciones de tesis.
              </small>
            </div>

            <div class="comparison-actions">
              <button
                id="saveComparisonBtn"
                type="button"
                class="secondary-action"
              >
                Guardar resultado para comparar
              </button>

              ${comparisonHistory.length ? `
                <button
                  id="clearComparisonBtn"
                  type="button"
                  class="secondary-action danger-action"
                >
                  Limpiar comparación
                </button>
              ` : ""}
            </div>
          </div>

          ${comparisonHistory.length ? `
            <div class="comparison-export-actions">
              <button
                id="exportComparisonCsvBtn"
                type="button"
                class="secondary-action"
              >
                Exportar comparación CSV
              </button>

              <button
                id="exportComparisonJsonBtn"
                type="button"
                class="secondary-action"
              >
                Exportar comparación JSON
              </button>
            </div>
          ` : ""}

          ${renderScenarioComparison(
            comparisonHistory
          )}
        </section>

        <div class="results-note">
          <strong>Nota:</strong>
          la latencia y el throughput actuales incluyen efectos de ejecución local
          (sockets, esperas y procesamiento del software); no deben interpretarse
          exclusivamente como efectos de la distancia radio.
        </div>
      ` : ""}

      ${state.result?.success && !hasMetrics ? `
        <div class="results-note warning">
          <strong>Métricas no disponibles en la respuesta.</strong>
          La simulación finalizó, pero el frontend no recibió <code>metrics</code>.
          En ese caso revisaremos el contrato <code>SimulationResult</code> antes de
          incorporar las gráficas de propagación.
        </div>
      ` : ""}

      <details class="technical-details">
        <summary>Ver JSON técnico de la ejecución</summary>
        <pre>${escapeHtml(JSON.stringify(payload, null, 2))}</pre>
      </details>

      ${state.result ? `
        <details class="technical-details response-details">
          <summary>Ver respuesta técnica del backend</summary>
          <pre>${escapeHtml(JSON.stringify(state.result, null, 2))}</pre>
        </details>
      ` : ""}
    </section>
  `;
}


function renderCurrentStep() {
  if (state.currentStep === 1) return renderFileStep();
  if (state.currentStep === 2) return renderTopologyStep();
  if (state.currentStep === 3) return renderDevicesStep();
  return renderResultsStep();
}

function updateStepTabs() {
  document.querySelectorAll("[data-step-nav]").forEach(tab => {
    const step = Number(tab.dataset.stepNav);
    tab.classList.toggle("active", step === state.currentStep);
  });
}

function updateButtons() {
  const backBtn = document.getElementById("backBtn");
  const nextBtn = document.getElementById("nextBtn");

  if (backBtn) {
    backBtn.disabled = state.currentStep === 1 || state.ui.busy;
  }

  if (nextBtn) {
    nextBtn.textContent = state.currentStep === 4 ? "Finalizado" : "Siguiente";
    nextBtn.disabled = state.currentStep === 4 || state.ui.busy;
  }
}

function renderSidePanel() {
  const sidePanelContent = document.getElementById("sidePanelContent");
  if (!sidePanelContent) return;

  sidePanelContent.innerHTML = `
    <div class="preview-card">
      <h3>Vista previa</h3>
      <p><strong>Paso actual:</strong> ${state.currentStep}</p>
      <p><strong>Archivo:</strong> ${escapeHtml(state.file.fileName || "-")}</p>
      <p><strong>Ruta resuelta:</strong> ${escapeHtml(getResolvedInputFile() || "-")}</p>
      <p><strong>Estado:</strong> ${escapeHtml(state.ui.uploadMessage || "Sin analizar")}</p>
      <p><strong>Filas estimadas:</strong> ${state.file.rowCountEstimate || 0}</p>
      <p><strong>Dispositivos:</strong> ${state.topology.deviceCount || 0}</p>
      <p><strong>ADR:</strong> ${state.experiment.adrEnabled ? "ON" : "OFF"}</p>
      <p><strong>Pérdida aleatoria:</strong> ${state.experiment.randomLossEnabled ? `ON · p=${state.experiment.randomLossProbability}` : "OFF"}</p>
      <p><strong>COST231:</strong> ${state.propagation.frequencyMHz} MHz · ${state.propagation.los ? "LoS" : "NLoS"}</p>
      ${renderPreviewTable()}
    </div>
  `;
}

function bindStepOneEvents() {
  document.getElementById("inputPath")?.addEventListener("input", e => {
    state.file.inputPath = e.target.value;
    renderSidePanel();
  });

  document.getElementById("fileInput")?.addEventListener("change", onFileSelected);

  document.getElementById("delimiter")?.addEventListener("input", e => {
    state.file.delimiter = e.target.value || ",";
  });

  document.getElementById("hasHeader")?.addEventListener("change", e => {
    state.file.hasHeader = e.target.value === "true";
  });

  document.getElementById("analyzeBtn")?.addEventListener("click", analyzeLocalFile);
}

function bindTopologyEvents() {
  document.getElementById("rowsToProcess")?.addEventListener("input", e => {
    state.topology.rowsToProcess = Number(e.target.value);
    renderSidePanel();
  });

  document.getElementById("sendIntervalMs")?.addEventListener("input", e => {
    state.topology.sendIntervalMs = Number(e.target.value);
    renderSidePanel();
  });

  document.getElementById("gatewayId")?.addEventListener("input", e => {
    state.topology.gateway.gatewayId = e.target.value;
    renderSidePanel();
  });

  document.getElementById("deviceCount")?.addEventListener("input", e => {
    const count = Number(e.target.value);
    state.topology.deviceCount = count;
    state.devices = buildDevices(count, state.devices);
    renderSidePanel();
  });

  document.getElementById("gatewayX")?.addEventListener("input", e => {
    state.topology.gateway.x = Number(e.target.value);
  });

  document.getElementById("gatewayY")?.addEventListener("input", e => {
    state.topology.gateway.y = Number(e.target.value);
  });

  document.getElementById("maxTxPowerDBm")?.addEventListener("input", e => {
    state.topology.gateway.maxTxPowerDBm = Number(e.target.value);
  });

  document.getElementById("layoutMode")?.addEventListener("change", e => {
    state.topology.layout.mode = e.target.value;
  });

  document.getElementById("baseX")?.addEventListener("input", e => {
    state.topology.layout.baseX = Number(e.target.value);
  });

  document.getElementById("baseY")?.addEventListener("input", e => {
    state.topology.layout.baseY = Number(e.target.value);
  });

  document.getElementById("distanceMeters")?.addEventListener("input", e => {
    state.topology.layout.distanceMeters = Number(e.target.value);
  });

  document.getElementById("udpPort")?.addEventListener("input", e => {
    state.topology.gateway.udpPort = Number(e.target.value);
    renderSidePanel();
  });

  document.getElementById("tcpPort")?.addEventListener("input", e => {
    state.topology.gateway.tcpPort = Number(e.target.value);
    renderSidePanel();
  });

  document.getElementById("applyLayoutBtn")?.addEventListener("click", applyLayoutToDevices);

  document.getElementById("adrEnabled")?.addEventListener("change", e => {
    state.experiment.adrEnabled = e.target.value === "true";
    renderSidePanel();
  });

  document.getElementById("randomLossEnabled")?.addEventListener("change", e => {
    state.experiment.randomLossEnabled = e.target.value === "true";
    render();
  });

  document.getElementById("randomLossProbability")?.addEventListener("input", e => {
    state.experiment.randomLossProbability = Number(e.target.value);
    renderSidePanel();
  });

  document.getElementById("frequencyMHz")?.addEventListener("input", e => {
    state.propagation.frequencyMHz = Number(e.target.value);
  });

  document.getElementById("los")?.addEventListener("change", e => {
    state.propagation.los = e.target.value === "true";
  });

  [
    "streetWidthMeters",
    "buildingSeparationMeters",
    "baseStationHeightMeters",
    "averageBuildingHeightMeters",
    "mobileStationHeightMeters",
    "streetOrientationDegrees"
  ].forEach(field => {
    document.getElementById(field)?.addEventListener("input", e => {
      state.propagation[field] = Number(e.target.value);
    });
  });

  document.getElementById("urbanEnvironment")?.addEventListener("change", e => {
    state.propagation.urbanEnvironment = e.target.value;
  });
}

function bindDevicesEvents() {
  document.querySelectorAll("[data-field]").forEach(element => {
    const eventName = element.tagName === "SELECT" ? "change" : "input";

    element.addEventListener(eventName, e => {
      const index = Number(e.target.dataset.index);
      const field = e.target.dataset.field;

      if (field === "fPort" || field === "x" || field === "y") {
        state.devices[index][field] = Number(e.target.value);
      } else if (field === "eirpDbm") {
        state.devices[index].eirpDbm = e.target.value;
      } else if (field === "columns") {
        state.devices[index].columnIndexes = String(e.target.value || "")
          .split(",")
          .map(value => value.trim())
          .filter(Boolean)
          .map(Number)
          .filter(value => !Number.isNaN(value));
      } else {
        state.devices[index][field] = e.target.value;
      }

      renderSidePanel();
    });
  });
}

function bindResultsEvents() {
  document.getElementById("runSimulationBtn")
    ?.addEventListener(
      "click",
      runSimulation
    );

  document.getElementById(
    "comparisonScenarioLabel"
  )?.addEventListener(
    "input",
    event => {
      state.comparison.scenarioLabel =
        event.target.value;
    }
  );

  document.getElementById(
    "exportMetricsCsvBtn"
  )?.addEventListener(
    "click",
    exportCurrentMetricsCsv
  );

  document.getElementById(
    "exportResultJsonBtn"
  )?.addEventListener(
    "click",
    exportCurrentResultJson
  );

  document.getElementById(
    "printResultsBtn"
  )?.addEventListener(
    "click",
    printResultsView
  );

  document.getElementById(
    "saveComparisonBtn"
  )?.addEventListener(
    "click",
    saveCurrentResultForComparison
  );

  document.getElementById(
    "exportComparisonCsvBtn"
  )?.addEventListener(
    "click",
    exportComparisonCsv
  );

  document.getElementById(
    "exportComparisonJsonBtn"
  )?.addEventListener(
    "click",
    exportComparisonJson
  );

  document.getElementById(
    "clearComparisonBtn"
  )?.addEventListener(
    "click",
    clearComparisonHistory
  );

  document.querySelectorAll(
    "[data-remove-comparison]"
  ).forEach(button => {
    button.addEventListener(
      "click",
      () => {
        removeComparisonRun(
          Number(
            button.dataset
              .removeComparison
          )
        );
      }
    );
  });
}

function bindStepSpecificEvents() {
  if (state.currentStep === 1) bindStepOneEvents();
  if (state.currentStep === 2) bindTopologyEvents();
  if (state.currentStep === 3) bindDevicesEvents();
  if (state.currentStep === 4) bindResultsEvents();
}

function render() {
  const stepContent = document.getElementById("step-content");
  if (stepContent) {
    stepContent.innerHTML = renderCurrentStep();
  }

  renderSidePanel();
  updateStepTabs();
  updateButtons();
  bindStepSpecificEvents();
}

function goBack() {
  if (state.currentStep > 1 && !state.ui.busy) {
    state.errors = {};
    state.currentStep -= 1;
    render();
  }
}

function goNext() {
  if (state.ui.busy) return;

  if (state.currentStep === 1) {
    state.errors = validateFileStep();
    if (Object.keys(state.errors).length > 0) {
      render();
      return;
    }
  }

  if (state.currentStep === 2) {
    state.errors = validateTopologyStep();
    if (Object.keys(state.errors).length > 0) {
      render();
      return;
    }
    state.devices = buildDevices(state.topology.deviceCount, state.devices);
  }

  if (state.currentStep === 3) {
    state.errors = validateDevicesStep();
    if (Object.keys(state.errors).length > 0) {
      render();
      return;
    }
  }

  if (state.currentStep < 4) {
    state.currentStep += 1;
    render();
  }
}

async function runSimulation() {
  try {
    state.ui.busy = true;
    state.result = null;
    state.errors = {};
    render();

    if (state.file.rawFile && !state.file.uploadedPath) {
      await uploadSelectedFile();
    }

    const payload = buildSimulationPayload();

    state.lastExecutedPayload =
      cloneJson(payload);

    if (!payload.inputFile) {
      throw new Error("No hay archivo disponible para la simulación.");
    }

    const response = await fetch("/api/simulations/run", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const result = await response.json();
    state.result = result;
    state.ui.uploadMessage = payload.inputFile;
  } catch (error) {
    state.result = {
      success: false,
      rowsProcessed: 0,
      rowsSkipped: 0,
      devicesConfigured: state.devices.length,
      message: error.message || "Error al ejecutar la simulación."
    };
  } finally {
    state.ui.busy = false;
    render();
  }
}

function bindEvents() {
  const backBtn = document.getElementById("backBtn");
  const nextBtn = document.getElementById("nextBtn");

  backBtn?.addEventListener("click", goBack);
  nextBtn?.addEventListener("click", goNext);

  document.querySelectorAll("[data-step-nav]").forEach(tab => {
    tab.addEventListener("click", () => {
      if (state.ui.busy) return;
      state.currentStep = Number(tab.dataset.stepNav);
      render();
    });
  });
}

function injectWizardStyles() {
  if (document.getElementById("wizard-enhanced-styles")) return;

  const style = document.createElement("style");
  style.id = "wizard-enhanced-styles";
  style.textContent = `
    .wizard-step h2 {
      margin: 0 0 10px;
      font-size: 24px;
      font-weight: 700;
      color: #1f2937;
    }

    .wizard-step p {
      margin: 0 0 20px;
      color: #5f6b76;
    }

    .field,
    .device-card,
    .summary-card {
      margin-bottom: 16px;
    }

    .field label {
      display: block;
      margin-bottom: 8px;
      font-weight: 600;
      color: #334155;
    }

    .field input,
    .field select {
      width: 100%;
      padding: 12px 14px;
      border: 1px solid #d6d3d1;
      border-radius: 12px;
      background: #fff;
      font-size: 15px;
      outline: none;
    }

    .field input:focus,
    .field select:focus {
      border-color: #0f766e;
      box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
    }

    .field-row {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
    }

    .field-help,
    .section-help {
      display: block;
      margin-top: 6px;
      color: #64748b;
      font-size: 13px;
      line-height: 1.45;
    }

    .section-divider {
      border: 0;
      border-top: 1px solid #e2e8f0;
      margin: 26px 0 22px;
    }

    .section-title {
      margin: 0 0 8px;
      font-size: 19px;
      color: #1f2937;
    }

    .device-list {
      display: grid;
      gap: 16px;
    }

    .device-card,
    .summary-card,
    .preview-card {
      padding: 18px;
      border: 1px solid #d6d3d1;
      border-radius: 16px;
      background: #fff;
    }

    .device-card h3,
    .summary-card h3,
    .preview-card h3 {
      margin: 0 0 14px;
      font-size: 18px;
      color: #1f2937;
    }

    .results-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 18px;
    }

    .summary-list {
      margin: 0;
      padding-left: 18px;
      color: #334155;
    }

    .summary-list li {
      margin-bottom: 8px;
    }

    .result-box {
      margin-top: 12px;
      padding: 14px;
      border-radius: 12px;
    }

    .result-box.success {
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
    }

    .result-box.error {
      background: #fef2f2;
      border: 1px solid #fecaca;
    }

    .actions-row {
      display: flex;
      justify-content: flex-start;
      margin-bottom: 16px;
      gap: 12px;
    }

    .primary-action {
      padding: 12px 18px;
      border: none;
      border-radius: 12px;
      background: #0f766e;
      color: #fff;
      font-weight: 600;
      cursor: pointer;
    }

    .primary-action:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }


    .secondary-action {
      padding: 11px 16px;
      border: 1px solid #cbd5e1;
      border-radius: 12px;
      background: #ffffff;
      color: #334155;
      font-weight: 600;
      cursor: pointer;
      transition:
        background 0.15s ease,
        border-color 0.15s ease,
        transform 0.15s ease;
    }

    .secondary-action:hover {
      background: #f8fafc;
      border-color: #94a3b8;
    }

    .secondary-action:active,
    .primary-action:active {
      transform: translateY(1px);
    }

    .results-toolbar {
      flex-wrap: wrap;
      align-items: center;
    }

    .comparison-export-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin: 0 0 16px;
    }

    .consistency-warning code {
      font-weight: 700;
    }

    .compact-table-wrapper {
      overflow-x: visible;
    }

    .compact-device-table {
      min-width: 0;
      table-layout: fixed;
    }

    .compact-device-table th,
    .compact-device-table td {
      white-space: normal;
      padding: 9px 8px;
      font-size: 12px;
      vertical-align: middle;
    }

    .table-secondary {
      display: block;
      margin-top: 3px;
      color: #78716c;
      font-size: 10px;
      white-space: nowrap;
    }


    .technical-details {
      border: 1px solid #d6d3d1;
      border-radius: 14px;
      background: #fafaf9;
      padding: 12px 14px;
    }

    .technical-details summary {
      cursor: pointer;
      font-weight: 600;
      color: #334155;
    }


    .results-section {
      margin-top: 22px;
      padding: 18px;
      border: 1px solid #d6d3d1;
      border-radius: 16px;
      background: #fff;
    }

    .results-section-heading {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .results-section-heading h3 {
      margin: 0 0 6px;
      font-size: 19px;
      color: #1f2937;
    }

    .results-section-heading p {
      margin: 0;
      color: #64748b;
      font-size: 14px;
    }

    .metric-kpi-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 12px;
    }

    .metric-kpi {
      padding: 15px;
      border: 1px solid #e7e5e4;
      border-radius: 14px;
      background: #fafaf9;
    }

    .metric-kpi-label {
      display: block;
      color: #64748b;
      font-size: 13px;
      margin-bottom: 7px;
    }

    .metric-kpi strong {
      display: block;
      font-size: 23px;
      line-height: 1.2;
      color: #0f172a;
      margin-bottom: 6px;
    }

    .metric-kpi small {
      color: #78716c;
    }

    .metric-chart-list {
      display: grid;
      gap: 18px;
    }

    .metric-chart-row {
      display: grid;
      gap: 8px;
    }

    .metric-chart-heading,
    .gauge-label {
      display: flex;
      justify-content: space-between;
      gap: 14px;
      align-items: baseline;
    }

    .metric-chart-heading span {
      color: #64748b;
      font-size: 13px;
    }

    .stacked-bar,
    .metric-gauge {
      width: 100%;
      height: 18px;
      overflow: hidden;
      display: flex;
      border-radius: 999px;
      background: #e7e5e4;
    }

    .bar-segment {
      display: block;
      height: 100%;
      min-width: 0;
    }

    .bar-rx {
      background: #0f766e;
    }

    .bar-link-loss {
      background: #b45309;
    }

    .bar-random-loss {
      background: #9f1239;
    }

    .bar-legend {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      font-size: 12px;
      color: #57534e;
    }

    .bar-legend span {
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }

    .legend-dot {
      width: 9px;
      height: 9px;
      display: inline-block;
      border-radius: 50%;
    }

    .legend-rx {
      background: #0f766e;
    }

    .legend-link {
      background: #b45309;
    }

    .legend-random {
      background: #9f1239;
    }

    .radio-gauge-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
    }

    .radio-gauge-card {
      padding: 15px;
      border: 1px solid #e7e5e4;
      border-radius: 14px;
      background: #fafaf9;
    }

    .radio-gauge-card h4,
    .device-result-card h4 {
      margin: 0 0 14px;
      font-size: 16px;
      color: #1f2937;
    }

    .gauge-block + .gauge-block {
      margin-top: 15px;
    }

    .gauge-label {
      margin-bottom: 7px;
      font-size: 13px;
      color: #475569;
    }

    .gauge-label strong {
      color: #0f172a;
    }

    .metric-gauge {
      height: 11px;
    }

    .metric-gauge span {
      display: block;
      height: 100%;
      border-radius: inherit;
      background: #0f766e;
    }

    .gauge-block small {
      display: block;
      margin-top: 5px;
      color: #78716c;
      font-size: 11px;
    }

    .metrics-table-wrapper {
      overflow-x: auto;
      border: 1px solid #e7e5e4;
      border-radius: 12px;
    }

    .metrics-table {
      width: 100%;
      border-collapse: collapse;
      min-width: 920px;
      font-size: 13px;
    }

    .metrics-table th,
    .metrics-table td {
      padding: 10px 12px;
      text-align: left;
      border-bottom: 1px solid #e7e5e4;
      white-space: nowrap;
    }

    .metrics-table th {
      background: #f5f5f4;
      color: #334155;
      font-weight: 700;
    }

    .metrics-table tbody tr:last-child td {
      border-bottom: 0;
    }

    .device-results-grid {
      margin-top: 14px;
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
    }

    .device-result-card {
      border: 1px solid #e7e5e4;
      border-radius: 14px;
      padding: 14px;
      background: #fafaf9;
    }

    .device-result-card dl {
      margin: 0;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 7px 14px;
    }

    .device-result-card dl div {
      display: flex;
      justify-content: space-between;
      gap: 10px;
      padding-bottom: 6px;
      border-bottom: 1px dashed #e7e5e4;
    }

    .device-result-card dt {
      color: #64748b;
    }

    .device-result-card dd {
      margin: 0;
      color: #0f172a;
      font-weight: 600;
    }

    .results-note {
      margin-top: 18px;
      padding: 13px 15px;
      border-radius: 12px;
      background: #f0fdfa;
      border: 1px solid #99f6e4;
      color: #334155;
      font-size: 13px;
    }

    .results-note.warning {
      background: #fffbeb;
      border-color: #fde68a;
    }


    .comparison-save-panel {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 14px;
      align-items: end;
      margin-bottom: 16px;
      padding: 14px;
      border: 1px solid #e7e5e4;
      border-radius: 14px;
      background: #fafaf9;
    }

    .comparison-name-field {
      margin: 0;
    }

    .comparison-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      align-items: center;
    }

    .danger-action,
    .mini-action.danger {
      border-color: #fecaca;
      color: #991b1b;
      background: #fff;
    }

    .danger-action:hover,
    .mini-action.danger:hover {
      background: #fef2f2;
    }

    .mini-action {
      padding: 6px 9px;
      border: 1px solid #d6d3d1;
      border-radius: 8px;
      background: #fff;
      cursor: pointer;
      font-size: 12px;
    }

    .comparison-table-wrapper {
      overflow-x: auto;
      border: 1px solid #e7e5e4;
      border-radius: 12px;
      margin-bottom: 16px;
    }

    .comparison-table {
      min-width: 820px;
    }

    .comparison-table th,
    .comparison-table td {
      padding: 9px 8px;
      font-size: 12px;
      white-space: normal;
    }

    .comparison-chart-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
    }

    .comparison-chart-card {
      padding: 15px;
      border: 1px solid #e7e5e4;
      border-radius: 14px;
      background: #fafaf9;
    }

    .comparison-chart-card h4 {
      margin: 0 0 5px;
      color: #1f2937;
      font-size: 16px;
    }

    .comparison-chart-card > p {
      margin: 0 0 14px;
      color: #64748b;
      font-size: 13px;
      min-height: 36px;
    }

    .comparison-bar-list {
      display: grid;
      gap: 13px;
    }

    .comparison-bar-row {
      display: grid;
      gap: 6px;
    }

    .comparison-bar-heading {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: baseline;
      font-size: 12px;
    }

    .comparison-bar-heading strong {
      color: #334155;
    }

    .comparison-bar-heading span {
      color: #64748b;
      text-align: right;
    }

    .comparison-track {
      display: flex;
      width: 100%;
      height: 12px;
      overflow: hidden;
      border-radius: 999px;
      background: #e7e5e4;
    }

    .comparison-fill {
      display: block;
      height: 100%;
      border-radius: inherit;
      background: #0f766e;
    }

    .comparison-loss-track {
      background: #e7e5e4;
    }

    .comparison-loss-link {
      display: block;
      height: 100%;
      background: #b45309;
    }

    .comparison-loss-random {
      display: block;
      height: 100%;
      background: #9f1239;
    }

    .comparison-scale {
      display: block;
      margin-top: 10px;
      color: #78716c;
      font-size: 11px;
    }

    .response-details {
      margin-top: 10px;
    }

    .propagation-chart-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .propagation-chart-card {
      border: 1px solid #e7e5e4;
      border-radius: 14px;
      padding: 14px;
      background: #fafaf9;
    }

    .propagation-chart-title {
      margin-bottom: 10px;
    }

    .propagation-chart-title h4 {
      margin: 0 0 4px;
      color: #1f2937;
      font-size: 16px;
    }

    .propagation-chart-title small {
      color: #64748b;
    }

    .propagation-chart-scroll {
      overflow-x: auto;
    }

    .propagation-chart {
      width: 100%;
      min-width: 620px;
      display: block;
    }

    .chart-grid-line {
      stroke: #e7e5e4;
      stroke-width: 1;
    }

    .chart-axis-line {
      stroke: #78716c;
      stroke-width: 1.2;
    }

    .chart-data-line {
      fill: none;
      stroke: #0f766e;
      stroke-width: 3;
      stroke-linejoin: round;
      stroke-linecap: round;
    }

    .chart-point {
      fill: #0f766e;
      stroke: #ffffff;
      stroke-width: 2;
    }

    .chart-axis-text {
      fill: #64748b;
      font-size: 11px;
    }

    .chart-axis-title {
      fill: #334155;
      font-size: 12px;
      font-weight: 600;
    }


    .technical-details pre {
      margin-top: 12px;
      white-space: pre-wrap;
      word-break: break-word;
      font-size: 13px;
      color: #334155;
      max-height: 320px;
      overflow: auto;
    }

    .preview-table-wrap {
      margin-top: 14px;
      overflow: auto;
      border: 1px solid #d6d3d1;
      border-radius: 12px;
      background: #fff;
    }

    .preview-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
    }

    .preview-table th,
    .preview-table td {
      padding: 10px 12px;
      border-bottom: 1px solid #ece7e2;
      text-align: left;
      white-space: nowrap;
    }

    .preview-table th {
      background: #fafaf9;
      color: #374151;
      font-weight: 700;
    }

    .error-text {
      margin-top: 6px;
      color: #b91c1c;
      font-size: 13px;
    }

    .muted-text {
      color: #6b7280;
    }



    @media print {
      body {
        background: #ffffff !important;
      }

      .topbar,
      .step-indicator,
      .wizard-actions,
      .side-panel,
      .actions-row,
      .comparison-save-panel,
      .comparison-export-actions,
      .technical-details,
      .mini-action,
      .consistency-warning {
        display: none !important;
      }

      .app-shell {
        display: block !important;
        max-width: none !important;
        padding: 0 !important;
      }

      .wizard-panel,
      .wizard-step {
        border: 0 !important;
        box-shadow: none !important;
        padding: 0 !important;
        max-width: none !important;
      }

      .results-section,
      .summary-card,
      .metric-kpi,
      .radio-gauge-card,
      .propagation-chart-card,
      .comparison-chart-card,
      .device-result-card {
        break-inside: avoid;
        page-break-inside: avoid;
        box-shadow: none !important;
      }

      .propagation-chart {
        min-width: 0 !important;
      }

      .metrics-table,
      .comparison-table {
        font-size: 10px !important;
      }

      .results-note {
        break-inside: avoid;
      }
    }


    @media (max-width: 760px) {
      .responsive-data-table,
      .responsive-data-table tbody,
      .responsive-data-table tr,
      .responsive-data-table td {
        display: block;
        width: 100%;
      }

      .responsive-data-table {
        min-width: 0 !important;
      }

      .responsive-data-table thead {
        display: none;
      }

      .responsive-data-table tr {
        padding: 10px 12px;
        border-bottom: 1px solid #e7e5e4;
      }

      .responsive-data-table tr:last-child {
        border-bottom: 0;
      }

      .responsive-data-table td {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        gap: 14px;
        padding: 6px 0;
        border: 0;
        text-align: right;
      }

      .responsive-data-table td::before {
        content: attr(data-label);
        color: #64748b;
        font-weight: 600;
        text-align: left;
      }

      .compact-table-wrapper,
      .comparison-table-wrapper {
        overflow-x: visible;
      }

      .table-secondary {
        display: inline;
        margin-left: 5px;
      }
    }

    @media (max-width: 900px) {
      .metric-kpi-grid,
      .radio-gauge-grid,
      .device-results-grid,
      .comparison-chart-grid,
      .comparison-save-panel {
        grid-template-columns: 1fr;
      }

      .field-row,
      .results-grid {
        grid-template-columns: 1fr;
      }
    }
  `;

  document.head.appendChild(style);
}

function startApp() {
  console.log("startApp()");
  injectWizardStyles();
  restoreComparisonHistory();
  state.devices = buildDevices(state.topology.deviceCount, state.devices);
  bindEvents();
  render();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", startApp);
} else {
  startApp();
}