/**
 * GeoJSON Progress - SSE-based progress tracking for GeoJSON processing
 */

// Global state
let eventSource = null;
let totalRegions = 0;
let processed = 0;

function getElements() {
    return {
        log: document.getElementById('log'),
        progressFill: document.getElementById('progressFill'),
        status: document.getElementById('status'),
        startBtn: document.getElementById('startBtn'),
        stopBtn: document.getElementById('stopBtn'),
    };
}

function addLog(message, type) {
    type = type || 'info';
    const log = document.getElementById('log');
    const entry = document.createElement('div');
    entry.className = 'log-entry ' + type;
    entry.textContent = new Date().toLocaleTimeString() + ' - ' + message;
    log.appendChild(entry);
    log.scrollTop = log.scrollHeight;
}

function setButtonState(isProcessing) {
    const { startBtn, stopBtn } = getElements();
    startBtn.disabled = isProcessing;
    stopBtn.disabled = !isProcessing;
}

function resetProgress() {
    const { progressFill, status } = getElements();
    progressFill.style.width = '0%';
    status.textContent = 'Ready to start';
    totalRegions = 0;
    processed = 0;
}

function startProcessing(country) {
    const { status } = getElements();

    // Close existing connection if any
    if (eventSource) {
        eventSource.close();
    }

    // Reset state
    resetProgress();
    setButtonState(true);

    status.textContent = 'Connecting...';
    addLog('Starting GeoJSON processing for ' + country, 'info');

    eventSource = new EventSource('/geojson/' + country + '/stream');

    eventSource.addEventListener('progress', function(e) {
        const data = JSON.parse(e.data);

        if (data.type === 'started') {
            totalRegions = data.totalRegions;
            status.textContent = 'Processing 0 / ' + totalRegions + ' regions...';
            addLog('Started processing ' + totalRegions + ' regions', 'info');
        } else if (data.type === 'region_completed') {
            processed++;
            const percent = Math.round((processed / totalRegions) * 100);
            const { progressFill, status } = getElements();
            progressFill.style.width = percent + '%';
            status.textContent = 'Processing ' + processed + ' / ' + totalRegions + ' regions...';
            if (data.success) {
                addLog('[' + (data.index + 1) + '/' + totalRegions + '] ' + data.regionName + ' - OK', 'success');
            } else {
                addLog('[' + (data.index + 1) + '/' + totalRegions + '] ' + data.regionName + ' - Failed: ' + (data.errorMessage || 'Unknown error'), 'error');
            }
        } else if (data.type === 'completed') {
            const { progressFill, status } = getElements();
            progressFill.style.width = '100%';
            status.textContent = 'Completed! Success: ' + data.successCount + ', Failed: ' + data.failCount;
            addLog('Processing completed. Success: ' + data.successCount + ', Failed: ' + data.failCount, data.failCount > 0 ? 'error' : 'success');
            eventSource.close();
            eventSource = null;
            setButtonState(false);
        } else if (data.type === 'error') {
            const { status } = getElements();
            status.textContent = 'Error: ' + data.message;
            addLog('Error: ' + data.message, 'error');
            eventSource.close();
            eventSource = null;
            setButtonState(false);
        }
    });

    eventSource.onerror = function() {
        const { status } = getElements();
        status.textContent = 'Connection lost';
        addLog('Connection to server lost', 'error');
        eventSource.close();
        eventSource = null;
        setButtonState(false);
    };
}

function stopProcessing() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }

    const { status } = getElements();
    status.textContent = 'Stopped by user';
    addLog('Processing stopped by user', 'info');
    setButtonState(false);
}
