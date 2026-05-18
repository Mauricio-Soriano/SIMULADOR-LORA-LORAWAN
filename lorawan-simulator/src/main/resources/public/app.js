const form = document.getElementById('simulation-form');
const resultBox = document.getElementById('resultBox');
const statusBox = document.getElementById('statusBox');
const themeToggle = document.querySelector('[data-theme-toggle]');

(function initTheme() {
  let theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', theme);

  themeToggle.addEventListener('click', () => {
    theme = theme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', theme);
  });
})();

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  const payload = buildPayload();
  setStatus('Ejecutando simulación...', 'running');
  resultBox.textContent = JSON.stringify(payload, null, 2);

  try {
    const response = await fetch('/api/simulations/run', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Error en la solicitud.');
    }

    setStatus('Simulación completada.', 'success');
    resultBox.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    setStatus('La ejecución falló.', 'error');
    resultBox.textContent = error.message;
  }
});

function buildPayload() {
  const deviceCards = [...document.querySelectorAll('.device-card')];

  const deviceIds = ['dev-geo', 'dev-ambiental', 'dev-imu', 'dev-gyro'];
  const fPorts = [1, 2, 3, 4];

  const devices = deviceCards.map((card, index) => {
    const config = card.querySelector('[data-device-config]').value;
    const columns = card.querySelector('[data-device-columns]').value
      .split(',')
      .map(value => Number(value.trim()))
      .filter(value => !Number.isNaN(value));
    const x = Number(card.querySelector('[data-device-x]').value);
    const y = Number(card.querySelector('[data-device-y]').value);

    return {
      deviceId: deviceIds[index],
      config,
      fPort: fPorts[index],
      x,
      y,
      enabled: true,
      columnIndexes: columns
    };
  });

  return {
    inputFile: document.getElementById('inputFile').value,
    rowsToProcess: Number(document.getElementById('rowsToProcess').value),
    sendIntervalMs: Number(document.getElementById('sendIntervalMs').value),
    gateway: {
      gatewayId: document.getElementById('gatewayId').value,
      x: Number(document.getElementById('gatewayX').value),
      y: Number(document.getElementById('gatewayY').value),
      maxTxPowerDBm: Number(document.getElementById('maxTxPowerDBm').value),
      udpPort: Number(document.getElementById('udpPort').value),
      tcpPort: Number(document.getElementById('tcpPort').value)
    },
    devices
  };
}

function setStatus(message, type) {
  statusBox.textContent = message;
  statusBox.className = `status status-${type}`;
}