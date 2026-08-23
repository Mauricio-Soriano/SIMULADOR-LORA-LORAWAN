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

  linkBudget: {
       scenarioName: "COST231_FAVORABLE_NLOS",


      adrEnabled: true,
      randomLossEnabled: false,
      randomLossProbability: 0.0,

      frequencyMHz: 915.0,
      los: false,
      streetWidthMeters: 25.0,
      hbMeters: 50.0,
      hrMeters: 1.5,
      loriDb: 0.0,
      buildingSeparationMeters: 80.0,
      kaDb: 30.0,
      kdDb: 10.0,
      kfDb: -4.0,
      lbshDb: 0.0
    },

  devices: [],

  result: null,
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

      scenarioName: String(
        state.linkBudget.scenarioName || "Escenario personalizado"
      ),

      simulation: {
        rowsToProcess: Number(state.topology.rowsToProcess),
        sendIntervalMs: Number(state.topology.sendIntervalMs)
      },

      adrEnabled: Boolean(state.linkBudget.adrEnabled),
      randomLossEnabled: Boolean(state.linkBudget.randomLossEnabled),
      randomLossProbability: Number(state.linkBudget.randomLossProbability),

      linkBudgetParameters: {
        frequencyMHz: Number(state.linkBudget.frequencyMHz),
        los: Boolean(state.linkBudget.los),
        streetWidthMeters: Number(state.linkBudget.streetWidthMeters),
        hbMeters: Number(state.linkBudget.hbMeters),
        hrMeters: Number(state.linkBudget.hrMeters),
        loriDb: Number(state.linkBudget.loriDb),
        buildingSeparationMeters: Number(state.linkBudget.buildingSeparationMeters),
        kaDb: Number(state.linkBudget.kaDb),
        kdDb: Number(state.linkBudget.kdDb),
        kfDb: Number(state.linkBudget.kfDb),
        lbshDb: Number(state.linkBudget.lbshDb)
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

        transport: device.transport,

        deviceClass: device.config.includes("CLASS_A")
          ? "CLASS_A"
          : device.config.includes("CLASS_B")
            ? "CLASS_B"
            : "CLASS_C",

        config: device.config,

        fPort: Number(device.fPort),

        columnIndexes: [...device.columnIndexes],

        position: {
          x: 0,
          y: 0
        }
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

function buildDevices(count, existing = []) {
  return Array.from({ length: count }, (_, index) => {
    const current = existing[index];
    return current || {
      deviceId: `device-${index + 1}`,
      config: "US915_CLASS_A",
      transport: index % 2 === 0 ? "UDP" : "TCP",
      fPort: index + 1,
      columnIndexes: []
    };
  });
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

  if (!String(state.linkBudget.scenarioName || "").trim()) {
    errors.scenarioName = "El nombre del escenario es obligatorio.";
  }

    if (Number(state.linkBudget.frequencyMHz) <= 0) {
    errors.frequencyMHz = "La frecuencia debe ser mayor que 0.";
  }

  if (Number(state.linkBudget.streetWidthMeters) <= 0) {
    errors.streetWidthMeters = "El ancho de calle debe ser mayor que 0.";
  }

  if (Number(state.linkBudget.hbMeters) <= 0) {
    errors.hbMeters = "La altura hb debe ser mayor que 0.";
  }

  if (Number(state.linkBudget.hrMeters) <= 0) {
    errors.hrMeters = "La altura hr debe ser mayor que 0.";
  }

  if (Number(state.linkBudget.buildingSeparationMeters) <= 0) {
    errors.buildingSeparationMeters = "La separación entre edificios debe ser mayor que 0.";
  }

  if (
    Number(state.linkBudget.randomLossProbability) < 0 ||
    Number(state.linkBudget.randomLossProbability) > 1
  ) {
    errors.randomLossProbability = "La probabilidad debe estar entre 0 y 1.";
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

function renderCost231HelpSection() {
  return `
    <details class="parameter-help">
      <summary>¿Qué significan los parámetros COST231 Walfisch-Ikegami?</summary>

      <div class="help-content">
        <p>
          El modelo COST231 Walfisch-Ikegami permite estimar la pérdida de propagación
          en escenarios urbanos. En este simulador, sus parámetros se usan para calcular
          la potencia recibida por el gateway y decidir si un paquete LoRaWAN puede ser
          recibido correctamente.
        </p>

        <div class="help-grid">
          <div class="help-item">
            <h4>Frecuencia (MHz)</h4>
            <p>
              Frecuencia de operación del enlace. En general, una frecuencia mayor puede
              incrementar la pérdida de propagación.
            </p>
          </div>

          <div class="help-item">
            <h4>Condición LOS/NLOS</h4>
            <p>
              LOS indica línea de vista directa entre dispositivo y gateway. NLOS representa
              un entorno con obstáculos, como edificios u otras estructuras urbanas.
            </p>
          </div>

          <div class="help-item">
            <h4>Ancho de calle</h4>
            <p>
              Representa el ancho promedio de la calle o corredor urbano. Afecta el término
              de difracción desde los edificios hacia el nivel de calle.
            </p>
          </div>

          <div class="help-item">
            <h4>Separación entre edificios</h4>
            <p>
              Distancia promedio entre edificaciones. Influye en la pérdida adicional
              asociada a múltiples obstáculos urbanos.
            </p>
          </div>

          <div class="help-item">
            <h4>Altura hb</h4>
            <p>
              Altura de la antena del gateway o estación base. Modifica la relación entre
              la antena transmisora/receptora y el entorno urbano.
            </p>
          </div>

          <div class="help-item">
            <h4>Altura hr</h4>
            <p>
              Altura de la antena del dispositivo final. Normalmente representa un nodo IoT
              ubicado cerca del nivel del suelo.
            </p>
          </div>

          <div class="help-item">
            <h4>L_ori</h4>
            <p>
              Pérdida por orientación de la calle. Representa el efecto del ángulo entre
              la dirección de propagación y la orientación de la vía urbana.
            </p>
          </div>

          <div class="help-item">
            <h4>L_bsh</h4>
            <p>
              Corrección asociada a la altura de edificios y a la relación con la estación
              base. Se usa dentro del término de difracción multiscreen.
            </p>
          </div>

          <div class="help-item">
            <h4>ka</h4>
            <p>
              Coeficiente de ajuste del entorno urbano. Permite modificar la severidad
              base de la pérdida adicional del modelo.
            </p>
          </div>

          <div class="help-item">
            <h4>kd</h4>
            <p>
              Coeficiente asociado a la distancia. Controla cuánto cambia la pérdida
              conforme aumenta o disminuye la distancia entre dispositivo y gateway.
            </p>
          </div>

          <div class="help-item">
            <h4>kf</h4>
            <p>
              Coeficiente asociado a la frecuencia. Ajusta la influencia de la frecuencia
              sobre la pérdida adicional del entorno urbano.
            </p>
          </div>

          <div class="help-item">
            <h4>Pérdida aleatoria</h4>
            <p>
              Simula pérdidas adicionales no explicadas por el presupuesto de enlace,
              como interferencia, colisiones o errores aleatorios.
            </p>
          </div>
        </div>

        <p class="help-note">
          Flujo usado por el simulador:
          parámetros físicos → pérdida de propagación → potencia recibida →
          margen de enlace → paquete recibido o perdido.
        </p>
      </div>
    </details>
  `;
}


function renderTopologyStep() {
  return `
    <section class="wizard-step">
      <h2>Topología</h2>
      <p>Configura la simulación, el gateway y la cantidad de dispositivos.</p>

      <div class="field-row">
        <div class="field">
          <label for="rowsToProcess">Filas a procesar</label>
          <input id="rowsToProcess" type="number" min="1" value="${state.topology.rowsToProcess}" />
          ${state.errors.rowsToProcess ? `<p class="error-text">${escapeHtml(state.errors.rowsToProcess)}</p>` : ""}
        </div>

        <div class="field">
          <label for="sendIntervalMs">Intervalo de envío (ms)</label>
          <input id="sendIntervalMs" type="number" min="0" value="${state.topology.sendIntervalMs}" />
          ${state.errors.sendIntervalMs ? `<p class="error-text">${escapeHtml(state.errors.sendIntervalMs)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="gatewayId">Gateway ID</label>
          <input id="gatewayId" type="text" value="${escapeHtml(state.topology.gateway.gatewayId)}" />
          ${state.errors.gatewayId ? `<p class="error-text">${escapeHtml(state.errors.gatewayId)}</p>` : ""}
        </div>

        <div class="field">
          <label for="deviceCount">Cantidad de dispositivos</label>
          <input id="deviceCount" type="number" min="1" value="${state.topology.deviceCount}" />
          ${state.errors.deviceCount ? `<p class="error-text">${escapeHtml(state.errors.deviceCount)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="gatewayX">Gateway X</label>
          <input id="gatewayX" type="number" value="${state.topology.gateway.x}" />
        </div>

        <div class="field">
          <label for="gatewayY">Gateway Y</label>
          <input id="gatewayY" type="number" value="${state.topology.gateway.y}" />
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="maxTxPowerDBm">Max TX Power (dBm)</label>
          <input id="maxTxPowerDBm" type="number" value="${state.topology.gateway.maxTxPowerDBm}" />
        </div>

        <div class="field">
          <label for="layoutMode">Layout</label>
          <select id="layoutMode">
            <option value="linear" ${state.topology.layout.mode === "linear" ? "selected" : ""}>Linear</option>
            <option value="grid" ${state.topology.layout.mode === "grid" ? "selected" : ""}>Grid</option>
          </select>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="baseX">Base X</label>
          <input id="baseX" type="number" value="${state.topology.layout.baseX}" />
        </div>

        <div class="field">
          <label for="baseY">Base Y</label>
          <input id="baseY" type="number" value="${state.topology.layout.baseY}" />
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="distanceMeters">Distancia entre nodos (m)</label>
          <input id="distanceMeters" type="number" min="1" value="${state.topology.layout.distanceMeters}" />
        </div>

        <div class="field">
          <label for="udpPort">UDP Port</label>
          <input id="udpPort" type="number" min="1" value="${state.topology.gateway.udpPort}" />
          ${state.errors.udpPort ? `<p class="error-text">${escapeHtml(state.errors.udpPort)}</p>` : ""}
        </div>
      </div>

      <div class="field">
        <label for="tcpPort">TCP Port</label>
        <input id="tcpPort" type="number" min="1" value="${state.topology.gateway.tcpPort}" />
        ${state.errors.tcpPort ? `<p class="error-text">${escapeHtml(state.errors.tcpPort)}</p>` : ""}
      </div>

      <hr />

      <h3>Parámetros COST231 Walfisch-Ikegami</h3>
      <p class="hint">
        Estos valores serán utilizados para calcular la pérdida de propagación y el presupuesto de enlace.
      </p>

      <div class="field">
        <label for="scenarioName">Nombre del escenario</label>
        <input
          id="scenarioName"
          type="text"
          value="${escapeHtml(state.linkBudget.scenarioName)}"
          placeholder="Ej. COST231_FAVORABLE_NLOS"
        />
        ${state.errors.scenarioName ? `<p class="error-text">${escapeHtml(state.errors.scenarioName)}</p>` : ""}
      </div>

      ${renderCost231HelpSection()}

      <div class="field-row">
        <div class="field">
          <label for="adrEnabled">ADR habilitado</label>
          <select id="adrEnabled">
            <option value="true" ${state.linkBudget.adrEnabled ? "selected" : ""}>Sí</option>
            <option value="false" ${!state.linkBudget.adrEnabled ? "selected" : ""}>No</option>
          </select>
        </div>

        <div class="field">
          <label for="randomLossEnabled">Pérdida aleatoria</label>
          <select id="randomLossEnabled">
            <option value="true" ${state.linkBudget.randomLossEnabled ? "selected" : ""}>Sí</option>
            <option value="false" ${!state.linkBudget.randomLossEnabled ? "selected" : ""}>No</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label for="randomLossProbability">Probabilidad de pérdida aleatoria</label>
        <input id="randomLossProbability" type="number" min="0" max="1" step="0.01" value="${state.linkBudget.randomLossProbability}" />
        ${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
      </div>

      <div class="field-row">
        <div class="field">
          <label for="frequencyMHz">Frecuencia (MHz)</label>
          <input id="frequencyMHz" type="number" min="1" step="0.1" value="${state.linkBudget.frequencyMHz}" />
${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
        </div>

        <div class="field">
          <label for="los">Condición de propagación</label>
          <select id="los">
            <option value="true" ${state.linkBudget.los ? "selected" : ""}>LOS</option>
            <option value="false" ${!state.linkBudget.los ? "selected" : ""}>NLOS</option>
          </select>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="streetWidthMeters">Ancho de calle (m)</label>
          <input id="streetWidthMeters" type="number" min="1" step="0.1" value="${state.linkBudget.streetWidthMeters}" />
          ${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
        </div>

        <div class="field">
          <label for="buildingSeparationMeters">Separación entre edificios (m)</label>
          <input id="buildingSeparationMeters" type="number" min="1" step="0.1" value="${state.linkBudget.buildingSeparationMeters}" />
          ${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="hbMeters">Altura gateway/base station hb (m)</label>
          <input id="hbMeters" type="number" min="0.1" step="0.1" value="${state.linkBudget.hbMeters}" />
          ${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
        </div>

        <div class="field">
          <label for="hrMeters">Altura dispositivo hr (m)</label>
          <input id="hrMeters" type="number" min="0.1" step="0.1" value="${state.linkBudget.hrMeters}" />
          ${state.errors.frequencyMHz ? `<p class="error-text">${escapeHtml(state.errors.frequencyMHz)}</p>` : ""}
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="loriDb">Pérdida por orientación L_ori (dB)</label>
          <input id="loriDb" type="number" step="0.1" value="${state.linkBudget.loriDb}" />
        </div>

        <div class="field">
          <label for="lbshDb">Pérdida L_bsh (dB)</label>
          <input id="lbshDb" type="number" step="0.1" value="${state.linkBudget.lbshDb}" />
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label for="kaDb">ka</label>
          <input id="kaDb" type="number" step="0.1" value="${state.linkBudget.kaDb}" />
        </div>

        <div class="field">
          <label for="kdDb">kd</label>
          <input id="kdDb" type="number" step="0.1" value="${state.linkBudget.kdDb}" />
        </div>
      </div>

      <div class="field">
        <label for="kfDb">kf</label>
        <input id="kfDb" type="number" step="0.1" value="${state.linkBudget.kfDb}" />
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
                <label for="transport_${index}">Transporte</label>
                <select id="transport_${index}" data-index="${index}" data-field="transport">
                  <option value="UDP" ${device.transport === "UDP" ? "selected" : ""}>UDP</option>
                  <option value="TCP" ${device.transport === "TCP" ? "selected" : ""}>TCP</option>
                </select>
              </div>
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

function renderResultsStep() {
  const payload = buildSimulationPayload();

  return `
    <section class="wizard-step">
      <h2>Resultados</h2>
      <p>Revisa el resumen y ejecuta la simulación del backend Java.</p>

      <div class="results-grid">
        <article class="summary-card">
          <h3>Resumen</h3>
          <ul class="summary-list">
            <li><strong>Archivo:</strong> ${escapeHtml(payload.inputFile || "-")}</li>
            <li><strong>Escenario:</strong> ${escapeHtml(payload.scenarioName || "-")}</li>
            <li><strong>Condición:</strong> ${payload.linkBudgetParameters.los ? "LOS" : "NLOS"}</li>
            <li><strong>Frecuencia:</strong> ${payload.linkBudgetParameters.frequencyMHz} MHz</li>
            <li><strong>Filas a procesar:</strong> ${payload.simulation.rowsToProcess}</li>
            <li><strong>Intervalo:</strong> ${payload.simulation.sendIntervalMs} ms</li>
            <li><strong>Gateway:</strong> ${escapeHtml(payload.gateway.gatewayId)}</li>
            <li><strong>UDP:</strong> ${payload.gateway.udpPort}</li>
            <li><strong>TCP:</strong> ${payload.gateway.tcpPort}</li>
            <li><strong>Dispositivos:</strong> ${payload.devices.length}</li>
          </ul>
        </article>

        <article class="summary-card">
          <h3>Estado</h3>
          <p>${state.ui.busy ? "Ejecutando simulación..." : "Listo para ejecutar."}</p>
          ${state.result ? `
            <div class="result-box ${state.result.success ? "success" : "error"}">
              <p><strong>Éxito:</strong> ${state.result.success ? "Sí" : "No"}</p>
              <p><strong>Procesadas:</strong> ${state.result.rowsProcessed ?? 0}</p>
              <p><strong>Omitidas:</strong> ${state.result.rowsSkipped ?? 0}</p>
              <p><strong>Dispositivos:</strong> ${state.result.devicesConfigured ?? payload.devices.length}</p>
              <p><strong>Mensaje:</strong> ${escapeHtml(state.result.message || "-")}</p>
            </div>
          ` : `<p>Aún no hay resultado.</p>`}
        </article>
      </div>

      <div class="actions-row">
        <button id="runSimulationBtn" type="button" class="primary-action" ${state.ui.busy ? "disabled" : ""}>
          ${state.ui.busy ? "Ejecutando..." : "Ejecutar simulación"}
        </button>
      </div>

      <details class="technical-details">
        <summary>Ver JSON técnico</summary>
        <pre>${escapeHtml(JSON.stringify(payload, null, 2))}</pre>
      </details>
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

  document.getElementById("scenarioName")?.addEventListener("input", e => {
    state.linkBudget.scenarioName = e.target.value;
    renderSidePanel();
  });


    const linkBudgetFields = [
      "randomLossProbability",
      "frequencyMHz",
      "streetWidthMeters",
      "hbMeters",
      "hrMeters",
      "loriDb",
      "buildingSeparationMeters",
      "kaDb",
      "kdDb",
      "kfDb",
      "lbshDb"
    ];

    linkBudgetFields.forEach(field => {
      const element = document.getElementById(field);

      if (element) {
        element.addEventListener("input", event => {
          state.linkBudget[field] = Number(event.target.value);
          renderSidePanel();
        });
      }
    });

    const adrEnabled = document.getElementById("adrEnabled");
    if (adrEnabled) {
      adrEnabled.addEventListener("change", event => {
        state.linkBudget.adrEnabled = event.target.value === "true";
      });
    }

    const randomLossEnabled = document.getElementById("randomLossEnabled");
    if (randomLossEnabled) {
      randomLossEnabled.addEventListener("change", event => {
        state.linkBudget.randomLossEnabled = event.target.value === "true";
      });
    }

    const los = document.getElementById("los");
    if (los) {
      los.addEventListener("change", event => {
        state.linkBudget.los = event.target.value === "true";
        renderSidePanel();
      });
    }

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
}

function bindDevicesEvents() {
  document.querySelectorAll("[data-field]").forEach(element => {
    const eventName = element.tagName === "SELECT" ? "change" : "input";

    element.addEventListener(eventName, e => {
      const index = Number(e.target.dataset.index);
      const field = e.target.dataset.field;

      if (field === "fPort") {
        state.devices[index].fPort = Number(e.target.value);
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
  document.getElementById("runSimulationBtn")?.addEventListener("click", runSimulation);
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

        .parameter-help {
      margin: 0 0 18px;
      padding: 14px 16px;
      border: 1px solid #d6d3d1;
      border-radius: 14px;
      background: #fafaf9;
    }

    .parameter-help summary {
      cursor: pointer;
      font-weight: 700;
      color: #0f172a;
    }

    .help-content {
      margin-top: 14px;
      color: #334155;
    }

    .help-content p {
      margin: 0 0 14px;
      line-height: 1.5;
    }

    .help-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
      margin-top: 12px;
      margin-bottom: 12px;
    }

    .help-item {
      padding: 12px;
      border: 1px solid #e7e5e4;
      border-radius: 12px;
      background: #fff;
    }

    .help-item h4 {
      margin: 0 0 6px;
      font-size: 14px;
      color: #0f172a;
    }

    .help-item p {
      margin: 0;
      font-size: 13px;
      color: #475569;
    }

    .help-note {
      padding: 12px;
      border-left: 4px solid #0f766e;
      background: #ecfdf5;
      border-radius: 10px;
      font-size: 14px;
    }

    @media (max-width: 900px) {
      .field-row,
      .results-grid 
      .help-grid {
      grid-template-columns: 1fr;
      }
    }
  `;

  document.head.appendChild(style);
}

function startApp() {
  console.log("startApp()");
  injectWizardStyles();
  state.devices = buildDevices(state.topology.deviceCount, state.devices);
  bindEvents();
  render();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", startApp);
} else {
  startApp();
}