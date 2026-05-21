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

  return {
    inputFile,
    rowsToProcess: Number(state.topology.rowsToProcess),
    sendIntervalMs: Number(state.topology.sendIntervalMs),
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
      config: device.config,
      deviceClass: device.deviceClass,
      transport: device.transport,
      fPort: Number(device.fPort),
      columns: [...device.columnIndexes]
    }))
  };
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