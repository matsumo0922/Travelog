/**
 * GeoJSON Progress - SSE-based progress tracking for GeoJSON processing
 * Card-based UI with detailed progress display
 */

// Global state
const state = {
    eventSource: null,
    regions: [],
    totalRegions: 0,
    processed: 0,
    successCount: 0,
    failCount: 0,
    startTime: null,
    timerInterval: null,
    logCount: 0,
};

// DOM element references
function getElements() {
    return {
        log: document.getElementById('log'),
        logCount: document.getElementById('logCount'),
        logWrapper: document.getElementById('logWrapper'),
        progressFill: document.getElementById('progressFill'),
        status: document.getElementById('status'),
        startBtn: document.getElementById('startBtn'),
        stopBtn: document.getElementById('stopBtn'),
        regionCards: document.getElementById('regionCards'),
        totalStatDisplay: document.getElementById('totalStatDisplay'),
        successStatDisplay: document.getElementById('successStatDisplay'),
        failedStatDisplay: document.getElementById('failedStatDisplay'),
        timeStatDisplay: document.getElementById('timeStatDisplay'),
    };
}

// Utility functions
function formatTime(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
        return `${hours}:${String(minutes % 60).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
    }
    return `${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}

function formatProcessingTime(ms) {
    if (ms < 1000) {
        return `${ms}ms`;
    }
    return `${(ms / 1000).toFixed(1)}s`;
}

// Logging functions
function addLog(message, type) {
    type = type || 'info';
    const { log, logCount } = getElements();
    const entry = document.createElement('div');
    entry.className = 'log-entry ' + type;
    entry.textContent = new Date().toLocaleTimeString() + ' - ' + message;
    log.appendChild(entry);
    log.scrollTop = log.scrollHeight;

    state.logCount++;
    logCount.textContent = state.logCount + ' entries';
}

function toggleLog() {
    const { logWrapper } = getElements();
    logWrapper.classList.toggle('log-collapsed');
}

// Button state management
function setButtonState(isProcessing) {
    const { startBtn, stopBtn } = getElements();
    startBtn.disabled = isProcessing;
    stopBtn.disabled = !isProcessing;
}

// Statistics update
function updateStatistics() {
    const { totalStatDisplay, successStatDisplay, failedStatDisplay, timeStatDisplay } = getElements();

    totalStatDisplay.textContent = state.totalRegions;
    successStatDisplay.textContent = state.successCount;
    failedStatDisplay.textContent = state.failCount;

    if (state.startTime) {
        const elapsed = Date.now() - state.startTime;
        timeStatDisplay.textContent = formatTime(elapsed);
    }
}

function startTimer() {
    state.startTime = Date.now();
    state.timerInterval = setInterval(updateStatistics, 1000);
}

function stopTimer() {
    if (state.timerInterval) {
        clearInterval(state.timerInterval);
        state.timerInterval = null;
    }
}

// Progress update
function updateProgress() {
    const { progressFill, status } = getElements();
    const percent = state.totalRegions > 0 ? Math.round((state.processed / state.totalRegions) * 100) : 0;
    progressFill.style.width = percent + '%';
    status.textContent = `Processing ${state.processed} / ${state.totalRegions} regions (${percent}%)`;
}

// Card rendering
function createRegionCard(region) {
    const card = document.createElement('div');
    card.className = 'region-card bg-white rounded-lg p-4 shadow-sm';
    card.id = `region-card-${region.index}`;
    card.setAttribute('data-state', 'pending');

    const levelLabel = region.level === 0 ? 'Country' : 'ADM1';
    const adm2Info = region.level === 1 && region.adm2Count > 0
        ? `<div class="text-xs text-gray-400">${region.adm2Count} cities</div>`
        : '';

    card.innerHTML = `
        <div class="flex items-start gap-3">
            <div class="thumbnail-container w-12 h-12 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
                ${region.thumbnailUrl
                    ? `<img src="${region.thumbnailUrl}" alt="${region.name}" class="w-full h-full object-cover">`
                    : `<div class="w-full h-full flex items-center justify-center text-gray-400 text-xl">📍</div>`
                }
            </div>
            <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                    <span class="status-badge text-xs px-2 py-0.5 rounded-full bg-gray-200 text-gray-600">
                        ${levelLabel}
                    </span>
                    <span class="status-indicator hidden text-xs px-2 py-0.5 rounded-full"></span>
                </div>
                <div class="font-semibold text-gray-800 truncate mt-1" title="${region.name}">
                    ${region.name}
                </div>
                <div class="name-details text-xs text-gray-500 truncate"></div>
                ${adm2Info}
            </div>
        </div>
        <div class="adm2-progress mt-3 hidden">
            <div class="flex justify-between text-xs text-gray-500 mb-1">
                <span class="adm2-label">Cities</span>
                <span class="adm2-count">0/0</span>
            </div>
            <div class="bg-gray-200 rounded-full h-1.5 overflow-hidden">
                <div class="adm2-bar bg-blue-400 h-full transition-all duration-300" style="width: 0%"></div>
            </div>
        </div>
        <div class="metadata mt-3 hidden">
            <div class="grid grid-cols-2 gap-2 text-xs">
                <div class="iso-code text-gray-500"></div>
                <div class="processing-time text-gray-500 text-right"></div>
            </div>
            <div class="wiki-link text-xs mt-1"></div>
        </div>
    `;

    return card;
}

function renderRegionCards(regions) {
    const { regionCards } = getElements();
    regionCards.innerHTML = '';

    regions.forEach(region => {
        const card = createRegionCard(region);
        regionCards.appendChild(card);
    });

    state.regions = regions;
}

function updateCardState(index, cardState, data) {
    const card = document.getElementById(`region-card-${index}`);
    if (!card) return;

    card.setAttribute('data-state', cardState);

    const statusIndicator = card.querySelector('.status-indicator');

    switch (cardState) {
        case 'processing':
            statusIndicator.className = 'status-indicator text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-600 animate-pulse';
            statusIndicator.textContent = 'Processing...';
            statusIndicator.classList.remove('hidden');

            // Show ADM2 progress for ADM1 regions
            if (data && data.level === 1 && data.adm2Count > 0) {
                const adm2Progress = card.querySelector('.adm2-progress');
                adm2Progress.classList.remove('hidden');
                card.querySelector('.adm2-count').textContent = `0/${data.adm2Count}`;
            }
            break;

        case 'completed':
            statusIndicator.className = 'status-indicator text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-600';
            statusIndicator.textContent = '✓ Completed';
            statusIndicator.classList.remove('hidden');
            updateCardMetadata(card, data);
            break;

        case 'error':
            statusIndicator.className = 'status-indicator text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-600';
            statusIndicator.textContent = '✗ Failed';
            statusIndicator.classList.remove('hidden');
            if (data && data.errorMessage) {
                const metadata = card.querySelector('.metadata');
                metadata.classList.remove('hidden');
                metadata.innerHTML = `<div class="text-red-500 text-xs">${data.errorMessage}</div>`;
            }
            break;
    }
}

function updateCardMetadata(card, data) {
    if (!data) return;

    // Update name details
    const nameDetails = card.querySelector('.name-details');
    const names = [];
    if (data.nameJa) names.push(data.nameJa);
    if (data.nameEn && data.nameEn !== data.regionName) names.push(data.nameEn);
    if (names.length > 0) {
        nameDetails.textContent = names.join(' / ');
    }

    // Update thumbnail if available
    if (data.thumbnailUrl) {
        const thumbnailContainer = card.querySelector('.thumbnail-container');
        thumbnailContainer.innerHTML = `<img src="${data.thumbnailUrl}" alt="${data.regionName}" class="w-full h-full object-cover">`;
    }

    // Show metadata section
    const metadata = card.querySelector('.metadata');
    metadata.classList.remove('hidden');

    // ISO code
    if (data.isoCode) {
        card.querySelector('.iso-code').textContent = `ISO: ${data.isoCode}`;
    }

    // Processing time
    if (data.processingTimeMs) {
        card.querySelector('.processing-time').textContent = formatProcessingTime(data.processingTimeMs);
    }

    // Wikipedia link
    if (data.wikipedia) {
        const wikiLink = card.querySelector('.wiki-link');
        wikiLink.innerHTML = `<a href="https://en.wikipedia.org/wiki/${data.wikipedia}" target="_blank" class="text-blue-500 hover:underline">📖 Wikipedia</a>`;
    }

    // Update ADM2 progress to completed
    if (data.adm2TotalCount > 0) {
        const adm2Progress = card.querySelector('.adm2-progress');
        adm2Progress.classList.remove('hidden');
        card.querySelector('.adm2-count').textContent = `${data.adm2ProcessedCount}/${data.adm2TotalCount}`;
        card.querySelector('.adm2-bar').style.width = '100%';
    }
}

function updateAdm2Progress(adm1Index, processedCount, totalCount, currentName) {
    const card = document.getElementById(`region-card-${adm1Index}`);
    if (!card) return;

    const adm2Progress = card.querySelector('.adm2-progress');
    adm2Progress.classList.remove('hidden');

    const percent = totalCount > 0 ? Math.round((processedCount / totalCount) * 100) : 0;
    card.querySelector('.adm2-count').textContent = `${processedCount}/${totalCount}`;
    card.querySelector('.adm2-bar').style.width = `${percent}%`;

    if (currentName) {
        card.querySelector('.adm2-label').textContent = currentName;
    }
}

// Reset state
function resetState() {
    const { progressFill, status, regionCards } = getElements();

    progressFill.style.width = '0%';
    status.textContent = 'Ready to start';
    regionCards.innerHTML = '';

    state.regions = [];
    state.totalRegions = 0;
    state.processed = 0;
    state.successCount = 0;
    state.failCount = 0;
    state.startTime = null;
    state.logCount = 0;

    stopTimer();
    updateStatistics();
}

// Event handlers
function handleStarted(data) {
    state.totalRegions = data.totalRegions;
    updateStatistics();

    if (data.regions && data.regions.length > 0) {
        renderRegionCards(data.regions);
    }

    startTimer();
    addLog(`Started processing ${data.totalRegions} regions`, 'info');
}

function handleRegionStarted(data) {
    updateCardState(data.index, 'processing', data);
    addLog(`[${data.index + 1}/${state.totalRegions}] ${data.regionName} - Processing...`, 'info');
}

function handleAdm2Progress(data) {
    updateAdm2Progress(data.adm1Index, data.processedCount, data.totalCount, data.currentAdm2Name);
}

function handleRegionCompleted(data) {
    state.processed++;

    if (data.success) {
        state.successCount++;
        updateCardState(data.index, 'completed', data);
        const timeInfo = data.processingTimeMs ? ` (${formatProcessingTime(data.processingTimeMs)})` : '';
        addLog(`[${data.index + 1}/${state.totalRegions}] ${data.regionName} - OK${timeInfo}`, 'success');
    } else {
        state.failCount++;
        updateCardState(data.index, 'error', data);
        addLog(`[${data.index + 1}/${state.totalRegions}] ${data.regionName} - Failed: ${data.errorMessage || 'Unknown error'}`, 'error');
    }

    updateProgress();
    updateStatistics();
}

function handleCompleted(data) {
    const { progressFill, status } = getElements();

    stopTimer();
    progressFill.style.width = '100%';

    const totalTime = data.totalProcessingTimeMs ? ` in ${formatTime(data.totalProcessingTimeMs)}` : '';
    const adm2Info = data.adm2TotalCount > 0 ? ` (${data.adm2TotalCount} cities processed)` : '';
    status.textContent = `Completed! Success: ${data.successCount}, Failed: ${data.failCount}${totalTime}${adm2Info}`;

    const logType = data.failCount > 0 ? 'error' : 'success';
    addLog(`Processing completed. Success: ${data.successCount}, Failed: ${data.failCount}${totalTime}${adm2Info}`, logType);

    if (state.eventSource) {
        state.eventSource.close();
        state.eventSource = null;
    }
    setButtonState(false);
}

function handleError(data) {
    const { status } = getElements();

    stopTimer();
    status.textContent = 'Error: ' + data.message;
    addLog('Error: ' + data.message, 'error');

    if (state.eventSource) {
        state.eventSource.close();
        state.eventSource = null;
    }
    setButtonState(false);
}

function handleEvent(data) {
    switch (data.type) {
        case 'started':
            handleStarted(data);
            break;
        case 'region_started':
            handleRegionStarted(data);
            break;
        case 'adm2_progress':
            handleAdm2Progress(data);
            break;
        case 'region_completed':
            handleRegionCompleted(data);
            break;
        case 'completed':
            handleCompleted(data);
            break;
        case 'error':
            handleError(data);
            break;
    }
}

// Main processing functions
function startProcessing(country) {
    const { status } = getElements();

    // Close existing connection if any
    if (state.eventSource) {
        state.eventSource.close();
    }

    // Reset state
    resetState();
    setButtonState(true);

    status.textContent = 'Connecting...';
    addLog('Starting GeoJSON processing for ' + country, 'info');

    state.eventSource = new EventSource('/geojson/' + country + '/stream');

    state.eventSource.addEventListener('progress', function(e) {
        const data = JSON.parse(e.data);
        handleEvent(data);
    });

    state.eventSource.onerror = function() {
        const { status } = getElements();
        status.textContent = 'Connection lost';
        addLog('Connection to server lost', 'error');
        state.eventSource.close();
        state.eventSource = null;
        stopTimer();
        setButtonState(false);
    };
}

function stopProcessing() {
    if (state.eventSource) {
        state.eventSource.close();
        state.eventSource = null;
    }

    stopTimer();

    const { status } = getElements();
    status.textContent = 'Stopped by user';
    addLog('Processing stopped by user', 'info');
    setButtonState(false);
}

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    updateStatistics();
});
