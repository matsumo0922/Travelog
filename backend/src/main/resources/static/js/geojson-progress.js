/**
 * GeoJSON Progress - SSE-based progress tracking for GeoJSON processing
 */
function initGeoJsonProgress(country) {
    const eventSource = new EventSource('/geojson/' + country + '/stream');
    const log = document.getElementById('log');
    const progressFill = document.getElementById('progressFill');
    const status = document.getElementById('status');
    let totalRegions = 0;
    let processed = 0;

    function addLog(message, type) {
        type = type || 'info';
        const entry = document.createElement('div');
        entry.className = 'log-entry ' + type;
        entry.textContent = new Date().toLocaleTimeString() + ' - ' + message;
        log.appendChild(entry);
        log.scrollTop = log.scrollHeight;
    }

    eventSource.addEventListener('progress', function(e) {
        const data = JSON.parse(e.data);

        if (data.type === 'started') {
            totalRegions = data.totalRegions;
            status.textContent = 'Processing 0 / ' + totalRegions + ' regions...';
            addLog('Started processing ' + totalRegions + ' regions', 'info');
        } else if (data.type === 'region_completed') {
            processed++;
            const percent = Math.round((processed / totalRegions) * 100);
            progressFill.style.width = percent + '%';
            status.textContent = 'Processing ' + processed + ' / ' + totalRegions + ' regions...';
            if (data.success) {
                addLog('[' + (data.index + 1) + '/' + totalRegions + '] ' + data.regionName + ' - OK', 'success');
            } else {
                addLog('[' + (data.index + 1) + '/' + totalRegions + '] ' + data.regionName + ' - Failed: ' + (data.errorMessage || 'Unknown error'), 'error');
            }
        } else if (data.type === 'completed') {
            progressFill.style.width = '100%';
            status.textContent = 'Completed! Success: ' + data.successCount + ', Failed: ' + data.failCount;
            addLog('Processing completed. Success: ' + data.successCount + ', Failed: ' + data.failCount, data.failCount > 0 ? 'error' : 'success');
            eventSource.close();
        } else if (data.type === 'error') {
            status.textContent = 'Error: ' + data.message;
            addLog('Error: ' + data.message, 'error');
            eventSource.close();
        }
    });

    eventSource.onerror = function() {
        status.textContent = 'Connection lost';
        addLog('Connection to server lost', 'error');
    };
}
