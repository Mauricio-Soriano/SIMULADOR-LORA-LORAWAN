console.log("simulator-dashboard.js limpio cargado");

const state = {
  currentStep: 1,
  file: {
    rawFile: null,
    inputPath: "",
    uploadedPath: "",
    fileName: "",
    fileToken: "",
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
    deviceCount: 2
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

function isSupportedFile(name) {
  return /\.(csv|txt)$/i.test(name || "");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function getAllowedTransports(deviceClass) {
  if (deviceClass === "US915_CLASS_A") return ["UDP"];
  if (deviceClass === "US915_CLASS_B" || deviceClass === "US915_CLASS_C") return ["TCP"];
  return ["UDP", "TCP"];
}

function normalizeTransportForClass(device) {
  const allowed = getAllowedTransports(device.config);
  if (!allowed.includes(device.transport)) {
    device.transport = allowed[0];
  }
}

function buildDevices(count, existing = []) {
  return Array.from({ length: count }, (_, index) => {
    const current = existing[index];
    const device = current || {
      deviceId: `device-${index + 1}`,
      config: index === 0 ? "US915_CLASS_A" : "US915_CLASS_B",
      transport: index === 0 ? "UDP" : "TCP",
      fPort: index + 1,
      columnIndexes: []
    };
    normalizeTransportForClass(device);
    return device;
  });
}

function ensureDevices() {
  state.devices = buildDevices(Number(state.topology.deviceCount || 0), state.devices);
}

function getResolvedInputFile() {
  if (state.file.uploadedPath) return state.file.uploadedPath;
  if (state.file.inputPath) return normalizeSlashes(state.file.inputPath);
  if (state.file.rawFile?.name) return state.file.rawFile.name;
  return "";
}

function onFileSelected(event) {
  const file = event.target.files[0];
  if (!file) return;

  state.file.rawFile = file;
  state.file.fileName = file.name;
  state.file.uploadedPath = "";
  state.file.fileToken = "";
  state.file.inputPath = file.name;
  state.file.columns = [];
  state.file.previewRows = [];
  state.file.rowCountEstimate = 0;
  state.result = null;
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
  state.errors = {};
  render();

  const response = await fetch("/api/files/upload", {
    method: "POST",
    body: formData
  });

  const result = await response.json();
  console.log("Respuesta upload:", result);

  if (!response.ok || !result.success) {
    throw new Error(result.message || "No se pudo subir el archivo.");
  }

  state.file.fileToken = String(result.fileToken || "").trim();
  state.file.uploadedPath = normalizeSlashes(result.path || "");
  state.file.inputPath = state.file.uploadedPath;
  state.file.fileName = state.file.rawFile.name;
  state.ui.uploadMessage = `Archivo cargado: ${state.file.uploadedPath}`;

  if (!state.file.fileToken) {
    state.errors = {
      inputFile: "El backend subió el archivo, pero no devolvió fileToken."
    };
  }

  render();
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

function validateFileStep() {
  const errors = {};
  if (!getResolvedInputFile()) {
    errors.inputFile = "Selecciona un archivo o escribe una ruta válida.";
  }
  if (state.file.rawFile && !isSupportedFile(state.file.rawFile.name)) {
    errors.inputFile = "Solo se permiten archivos .csv o .txt";
  }
  return errors;
}

function validateTopologyStep() {
  const errors = {};
  if (Number(state.topology.rowsToProcess) <= 0) errors.rowsToProcess = "Debe ser mayor que 0.";
  if (Number(state.topology.sendIntervalMs) < 0) errors.sendIntervalMs = "No puede ser negativo.";
  if (!String(state.topology.gateway.gatewayId || "").trim()) errors.gatewayId = "Gateway ID obligatorio.";
  if (Number(state.topology.gateway.udpPort) <= 0) errors.udpPort = "Puerto UDP inválido.";
  if (Number(state.topology.gateway.tcpPort) <= 0) errors.tcpPort = "Puerto TCP inválido.";
  if (Number(state.topology.deviceCount) <= 0) errors.deviceCount = "Debe haber al menos 1 dispositivo.";
  return errors;
}

function validateDevicesStep() {
  const errors = {};
  state.devices.forEach((device, index) => {
    if (!String(device.deviceId || "").trim()) errors[`deviceId_${index}`] = "ID obligatorio.";
    if (!String(device.config || "").trim()) errors[`config_${index}`] = "Config obligatoria.";
    if (Number(device.fPort) <= 0) errors[`fPort_${index}`] = "FPort inválido.";
    if (!Array.isArray(device.columnIndexes) || device.columnIndexes.length === 0) {
      errors[`columns_${index}`] = "Selecciona al menos una columna.";
    }
    const allowed = getAllowedTransports(device.config);
    if (!allowed.includes(device.transport)) {
      errors[`transport_${index}`] = `La configuración ${device.config} solo permite ${allowed.join(", ")}.`;
    }
  });
  return errors;
}

function buildSimulationPayload() {
  return {
    fileToken: state.file.fileToken,
    delimiter: state.file.delimiter,
    hasHeader: state.file.hasHeader,
    simulation: {
      rowsToProcess: Number(state.topology.rowsToProcess),
      sendIntervalMs: Number(state.topology.sendIntervalMs)
    },
    gateway: {
      gatewayId: state.topology.gateway.gatewayId,
      x: Number(state.topology.gateway.x),
      y: Number(state.topology.gateway.y),
      maxTxPowerDBm: Number(state.topology.gateway.maxTxPowerDBm),
      udpPort: Number(state.topology.gateway.udpPort),
      tcpPort: Number(state.topology.gateway.tcpPort)
    },
    devices: state.devices.map(device => ({
      deviceId: device.deviceId,
      deviceClass: device.config,
      transport: device.transport,
      fPort: Number(device.fPort),
      enabled: true,
      columnIndexes: [...device.columnIndexes]
    }))
  };
}

function renderStepPills() {
  const steps = [
    { n: 1, label: "Archivo" },
    { n: 2, label: "Topología" },
    { n: 3, label: "Dispositivos" },
    { n: 4, label: "Resultados" }
  ];

  return `
    <div class="step-pills">
      ${steps.map(step => `
        <button
          type="button"
          class="step-pill ${state.currentStep === step.n ? "active" : ""}"
          data-go-step="${step.n}">
          ${step.n}. ${step.label}
        </button>
      `).join("")}
    </div>
  `;
}

function renderPreviewTable() {
  if (!state.file.previewRows.length || !state.file.columns.length) {
    return `<p>Sin vista previa todavía.</p>`;
  }

  return `
    <div class="table-wrap">
      <table class="preview-table">
        <thead>
          <tr>
            ${state.file.columns.map(col => `<th>${escapeHtml(col.name)}</th>`).join("")}
          </tr>
        </thead>
        <tbody>
          ${state.file.previewRows.map(row => `
            <tr>
              ${state.file.columns.map((col, i) => `<td>${escapeHtml(row[i] ?? "")}</td>`).join("")}
            </tr>
          `).join("")}
        </tbody>
      </table>
    </div>
  `;
}

function renderColumnSelector(device, deviceIndex) {
  if (!state.file.columns.length) {
    return `<p class="muted">Primero analiza un archivo para habilitar columnas.</p>`;
  }

  return `
    <div class="columns-grid">
      ${state.file.columns.map(col => `
        <label class="column-item">
          <input
            type="checkbox"
            class="column-checkbox"
            data-device-index="${deviceIndex}"
            data-column-index="${col.index}"
            ${device.columnIndexes.includes(col.index) ? "checked" : ""}>
          <span>${escapeHtml(col.name)}</span>
        </label>
      `).join("")}
    </div>
  `;
}

function renderFileSummary() {
  return `
    <div class="card">
      <p><strong>Archivo:</strong> ${escapeHtml(state.file.fileName || "-")}</p>
      <p><strong>Ruta resuelta:</strong> ${escapeHtml(getResolvedInputFile() || "-")}</p>
      <p><strong>File token:</strong> ${escapeHtml(state.file.fileToken || "-")}</p>
      <p><strong>Estado:</strong> ${escapeHtml(state.ui.uploadMessage || "Sin analizar")}</p>
      <p><strong>Filas estimadas:</strong> ${state.file.rowCountEstimate || 0}</p>
      ${renderPreviewTable()}
    </div>
  `;
}

function renderFileStep() {
  return `
    <section class="step-panel">
      <h2>Archivo de entrada</h2>
      <p>Selecciona un archivo CSV/TXT, visualízalo y súbelo antes de continuar.</p>

      <div class="field-group">
        <label for="input-file">Archivo local</label>
        <input id="input-file" type="file" accept=".csv,.txt" />
      </div>

      <div class="field-group">
        <label for="input-path">Nombre o ruta</label>
        <input id="input-path" type="text" value="${escapeHtml(state.file.inputPath)}" placeholder="archivo.csv o data/archivo.csv" />
      </div>

      <div class="field-group">
        <label for="delimiter">Delimitador</label>
        <input id="delimiter" type="text" value="${escapeHtml(state.file.delimiter)}" maxlength="1" />
      </div>

      <div class="field">
        <label for="has-header-select">¿El archivo tiene encabezado?</label>
        <select id="has-header-select">
          <option value="true" ${state.file.hasHeader ? "selected" : ""}>Sí</option>
          <option value="false" ${!state.file.hasHeader ? "selected" : ""}>No</option>
        </select>
      </div>

      ${state.errors.inputFile ? `<div class="field-error">${escapeHtml(state.errors.inputFile)}</div>` : ""}

      <div class="button-row">
        <button type="button" class="btn btn-secondary" id="analyze-file-btn">Analizar archivo</button>
        <button type="button" class="btn btn-primary" id="upload-file-btn">Subir archivo</button>
      </div>

      ${renderFileSummary()}
    </section>
  `;
}

function renderTopologyStep() {
  return `
    <section class="step-panel">
      <h2>Topología y simulación</h2>
      <p>Configura filas a procesar, intervalo de envío y parámetros del gateway.</p>

      <div class="grid two-col">
        <div class="field-group">
          <label for="rows-to-process">Filas a procesar</label>
          <input id="rows-to-process" type="number" min="1" value="${state.topology.rowsToProcess}" />
        </div>

        <div class="field-group">
          <label for="send-interval-ms">Intervalo de envío (ms)</label>
          <input id="send-interval-ms" type="number" min="0" value="${state.topology.sendIntervalMs}" />
        </div>

        <div class="field-group">
          <label for="gateway-id">Gateway ID</label>
          <input id="gateway-id" type="text" value="${escapeHtml(state.topology.gateway.gatewayId)}" />
        </div>

        <div class="field-group">
          <label for="device-count">Número de dispositivos</label>
          <input id="device-count" type="number" min="1" value="${state.topology.deviceCount}" />
        </div>

        <div class="field-group">
          <label for="gateway-x">Gateway X</label>
          <input id="gateway-x" type="number" value="${state.topology.gateway.x}" />
        </div>

        <div class="field-group">
          <label for="gateway-y">Gateway Y</label>
          <input id="gateway-y" type="number" value="${state.topology.gateway.y}" />
        </div>

        <div class="field-group">
          <label for="gateway-power">Potencia máxima (dBm)</label>
          <input id="gateway-power" type="number" value="${state.topology.gateway.maxTxPowerDBm}" />
        </div>

        <div class="field-group">
          <label for="udp-port">Puerto UDP</label>
          <input id="udp-port" type="number" min="1" value="${state.topology.gateway.udpPort}" />
        </div>

        <div class="field-group">
          <label for="tcp-port">Puerto TCP</label>
          <input id="tcp-port" type="number" min="1" value="${state.topology.gateway.tcpPort}" />
        </div>
      </div>
    </section>
  `;
}

function renderDevicesStep() {
  ensureDevices();

  return `
    <section class="step-panel">
      <h2>Dispositivos</h2>
      <p>Configura cada dispositivo LoRaWAN antes de ejecutar la simulación.</p>

      <div class="devices-stack">
        ${state.devices.map((device, index) => `
          <article class="device-card">
            <h3>Dispositivo ${index + 1}</h3>

            <div class="grid two-col">
              <div class="field-group">
                <label>ID</label>
                <input type="text" data-device-field="deviceId" data-index="${index}" value="${escapeHtml(device.deviceId)}" />
              </div>

              <div class="field-group">
                <label>FPort</label>
                <input type="number" data-device-field="fPort" data-index="${index}" value="${device.fPort}" min="1" />
              </div>

              <div class="field-group">
                <label>Clase / Config</label>
                <select data-device-field="config" data-index="${index}">
                  ${["US915_CLASS_A", "US915_CLASS_B", "US915_CLASS_C"].map(option => `
                    <option value="${option}" ${device.config === option ? "selected" : ""}>${option}</option>
                  `).join("")}
                </select>
              </div>

              <div class="field-group">
                <label>Transporte</label>
                <select data-device-field="transport" data-index="${index}">
                  ${getAllowedTransports(device.config).map(option => `
                    <option value="${option}" ${device.transport === option ? "selected" : ""}>${option}</option>
                  `).join("")}
                </select>
              </div>
            </div>

            <div class="field-group">
              <label>Columnas para payload</label>
              ${renderColumnSelector(device, index)}
            </div>
          </article>
        `).join("")}
      </div>
    </section>
  `;
}

function renderEvents() {
  if (!state.result?.events?.length) {
    return `<p>Sin eventos todavía.</p>`;
  }

  return `
    <ul class="events-list">
      ${state.result.events.map(event => `
        <li>
          <strong>${escapeHtml(event.level || "INFO")}</strong> -
          ${escapeHtml(event.deviceId || "-")} -
          ${escapeHtml(event.message || "-")}
        </li>
      `).join("")}
    </ul>
  `;
}

function renderResultSummary(payload) {
  return `
    <div class="card">
      <h3>Resumen</h3>
      <ul>
        <li><strong>Archivo:</strong> ${escapeHtml(getResolvedInputFile() || "-")}</li>
        <li><strong>File token:</strong> ${escapeHtml(state.file.fileToken || "-")}</li>
        <li><strong>Filas a procesar:</strong> ${payload.simulation.rowsToProcess}</li>
        <li><strong>Intervalo:</strong> ${payload.simulation.sendIntervalMs} ms</li>
        <li><strong>Gateway:</strong> ${escapeHtml(payload.gateway.gatewayId)}</li>
        <li><strong>UDP:</strong> ${payload.gateway.udpPort}</li>
        <li><strong>TCP:</strong> ${payload.gateway.tcpPort}</li>
        <li><strong>Dispositivos:</strong> ${payload.devices.length}</li>
      </ul>

      <h3>Estado</h3>
      <p>${state.ui.busy ? "Ejecutando simulación..." : "Listo para ejecutar."}</p>

      ${
        state.result
          ? `
            <p><strong>Éxito:</strong> ${state.result.success ? "Sí" : "No"}</p>
            <p><strong>Mensaje:</strong> ${escapeHtml(state.result.message || "-")}</p>
          `
          : `<p>Aún no hay resultado.</p>`
      }
    </div>
  `;
}

function renderResultsStep() {
  const payload = buildSimulationPayload();

  return `
    <section class="step-panel">
      <h2>Resultados</h2>
      <p>Revisa el resumen y ejecuta la simulación del backend Java.</p>

      ${renderResultSummary(payload)}

      ${state.errors.inputFile ? `<div class="alert error">${escapeHtml(state.errors.inputFile)}</div>` : ""}

      <div class="button-row">
        <button type="button" class="btn btn-primary" id="run-simulation-btn" ${state.ui.busy ? "disabled" : ""}>
          ${state.ui.busy ? "Ejecutando..." : "Ejecutar simulación"}
        </button>
      </div>

      <details class="technical-json">
        <summary>Ver JSON técnico</summary>
        <pre>${escapeHtml(JSON.stringify(payload, null, 2))}</pre>
      </details>

      <div class="card">
        <h3>Eventos</h3>
        ${renderEvents()}
      </div>
    </section>
  `;
}

function syncStaticWizardControls() {
  document.querySelectorAll("[data-go-step]").forEach(button => {
    const step = Number(button.dataset.goStep);
    button.classList.toggle("active", step === state.currentStep);
  });

  const prevBtn = document.getElementById("prev-step-btn");
  const nextBtn = document.getElementById("next-step-btn");

  if (prevBtn) {
    prevBtn.disabled = state.currentStep === 1;
  }

  if (nextBtn) {
    if (state.currentStep === 4) {
      nextBtn.disabled = true;
      nextBtn.textContent = "Finalizado";
    } else {
      nextBtn.disabled = false;
      nextBtn.textContent = "Siguiente";
    }
  }
}


async function runSimulation() {
  const fileErrors = validateFileStep();
  const topologyErrors = validateTopologyStep();
  const deviceErrors = validateDevicesStep();

  state.errors = {
    ...fileErrors,
    ...topologyErrors,
    ...deviceErrors
  };

  if (Object.keys(state.errors).length > 0) {
    render();
    return;
  }

  if (!state.file.fileToken) {
    state.errors.inputFile = "Debes subir el archivo antes de ejecutar la simulación.";
    render();
    return;
  }

  const payload = buildSimulationPayload();

  state.ui.busy = true;
  state.result = null;
  render();

  try {
    const response = await fetch("/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    const result = await response.json();

    if (!response.ok) {
      throw new Error(result.message || "Error ejecutando la simulación.");
    }

    state.result = result;
    state.errors = {};
  } catch (error) {
    state.result = {
      success: false,
      message: error.message || "No se pudo ejecutar la simulación.",
      events: []
    };
  } finally {
    state.ui.busy = false;
    render();
  }
}

function goNext() {
  if (state.currentStep === 1) {
    state.errors = validateFileStep();
    if (Object.keys(state.errors).length) return render();
  }
  if (state.currentStep === 2) {
    state.errors = validateTopologyStep();
    if (Object.keys(state.errors).length) return render();
  }
  if (state.currentStep === 3) {
    state.errors = validateDevicesStep();
    if (Object.keys(state.errors).length) return render();
  }

  state.currentStep = Math.min(4, state.currentStep + 1);
  render();
}

function goBack() {
  state.currentStep = Math.max(1, state.currentStep - 1);
  render();
}

function renderSidePreview() {
  return `
    <section class="side-panel-card">
      <h2>Vista previa</h2>
      <h3>Vista previa</h3>
      <p><strong>Paso actual:</strong> ${state.currentStep}</p>
      <p><strong>Archivo:</strong> ${escapeHtml(state.file.fileName || "-")}</p>
      <p><strong>Ruta resuelta:</strong> ${escapeHtml(getResolvedInputFile() || "-")}</p>
      <p><strong>File token:</strong> ${escapeHtml(state.file.fileToken || "-")}</p>
      <p><strong>Estado:</strong> ${escapeHtml(state.ui.uploadMessage || "Sin analizar")}</p>
      <p><strong>Filas estimadas:</strong> ${state.file.rowCountEstimate || 0}</p>
      <p><strong>Dispositivos:</strong> ${state.topology.deviceCount || 0}</p>
      ${renderPreviewTable()}
    </section>
  `;
}

function renderMain() {
  if (state.currentStep === 1) return renderFileStep();
  if (state.currentStep === 2) return renderTopologyStep();
  if (state.currentStep === 3) return renderDevicesStep();
  return renderResultsStep();
}

function bindEvents() {
  const root = document.getElementById("step-content");

  if (root) {
    root.addEventListener("click", async e => {
      if (e.target.closest("#analyze-file-btn")) {
        analyzeLocalFile();
        return;
      }

      if (e.target.closest("#upload-file-btn")) {
        try {
          await uploadSelectedFile();
        } catch (error) {
          state.errors = { ...state.errors, inputFile: error.message || "Error subiendo archivo." };
          state.ui.uploadMessage = "Error en carga";
          render();
        }
        return;
      }

      if (e.target.closest("#run-simulation-btn")) {
        runSimulation();
        return;
      }
    });

    root.addEventListener("change", e => {
      if (e.target.matches("#input-file")) {
        onFileSelected(e);
        return;
      }

      if (e.target.matches("#has-header")) {
        state.file.hasHeader = e.target.checked;
        return;
      }

      if (e.target.matches("#has-header-select")) {
        state.file.hasHeader = e.target.value === "true";
        return;
      }

      if (e.target.matches("[data-device-field]")) {
        const index = Number(e.target.dataset.index);
        const field = e.target.dataset.deviceField;
        const value = field === "fPort" ? Number(e.target.value) : e.target.value;

        state.devices[index][field] = value;

        if (field === "config") {
          normalizeTransportForClass(state.devices[index]);
          render();
        }
      }
    });

    root.addEventListener("input", e => {
      if (e.target.matches("#input-path")) state.file.inputPath = e.target.value;
      if (e.target.matches("#delimiter")) state.file.delimiter = e.target.value || ",";
      if (e.target.matches("#rows-to-process")) state.topology.rowsToProcess = Number(e.target.value);
      if (e.target.matches("#send-interval-ms")) state.topology.sendIntervalMs = Number(e.target.value);
      if (e.target.matches("#gateway-id")) state.topology.gateway.gatewayId = e.target.value;

      if (e.target.matches("#device-count")) {
        state.topology.deviceCount = Number(e.target.value);
        ensureDevices();
        render();
      }

      if (e.target.matches("#gateway-x")) state.topology.gateway.x = Number(e.target.value);
      if (e.target.matches("#gateway-y")) state.topology.gateway.y = Number(e.target.value);
      if (e.target.matches("#gateway-power")) state.topology.gateway.maxTxPowerDBm = Number(e.target.value);
      if (e.target.matches("#udp-port")) state.topology.gateway.udpPort = Number(e.target.value);
      if (e.target.matches("#tcp-port")) state.topology.gateway.tcpPort = Number(e.target.value);
    });
  }

  document.querySelectorAll("[data-go-step]").forEach(button => {
    button.addEventListener("click", e => {
      state.currentStep = Number(e.currentTarget.dataset.goStep);
      render();
    });
  });

  const prevBtn = document.getElementById("prev-step-btn");
  if (prevBtn) {
    prevBtn.addEventListener("click", goBack);
  }

  const nextBtn = document.getElementById("next-step-btn");
  if (nextBtn) {
    nextBtn.addEventListener("click", goNext);
  }
}

function render() {
  const stepContent = document.getElementById("step-content");
  const sidePanelContent = document.getElementById("sidePanelContent");

  if (stepContent) {
    stepContent.innerHTML = renderMain();
  }

  if (sidePanelContent) {
    sidePanelContent.innerHTML = renderSidePreview();
  }

  syncStaticWizardControls();
}

function startApp() {
  ensureDevices();
  bindEvents();
  render();
}

document.addEventListener("DOMContentLoaded", startApp);