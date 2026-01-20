/**
 * Integrated Progress - SSE-based progress tracking for GeoJSON + Name Enrichment
 * Manages both phases with auto/manual enrichment triggering
 */

const IntegratedProgress = {
    // State management
    state: {
        phase: 'idle', // 'idle' | 'geojson' | 'enrichment' | 'completed'
        autoEnrich: false,
        eventSource: null,
        country: null,
        startTime: null,
        timerInterval: null,
        logCount: 0,
        // GeoJSON stats
        geoJson: {
            regions: [],
            totalRegions: 0,
            processed: 0,
            successCount: 0,
            failCount: 0,
        },
        // Enrichment stats
        enrichment: {
            total: 0,
            applied: 0,
            validated: 0,
            skipped: 0,
            failed: 0,
        },
    },

    // DOM element references
    getElements() {
        return {
            // Common
            log: document.getElementById('log'),
            logCount: document.getElementById('logCount'),
            logWrapper: document.getElementById('logWrapper'),
            startBtn: document.getElementById('startBtn'),
            stopBtn: document.getElementById('stopBtn'),
            autoEnrichCheckbox: document.getElementById('autoEnrichCheckbox'),

            // GeoJSON section
            geojsonSection: document.getElementById('geojsonSection'),
            progressFill: document.getElementById('progressFill'),
            status: document.getElementById('status'),
            regionCards: document.getElementById('regionCards'),
            totalStatDisplay: document.getElementById('totalStatDisplay'),
            successStatDisplay: document.getElementById('successStatDisplay'),
            failedStatDisplay: document.getElementById('failedStatDisplay'),
            timeStatDisplay: document.getElementById('timeStatDisplay'),

            // Enrichment section
            enrichmentSection: document.getElementById('enrichmentSection'),
            enrichmentPrompt: document.getElementById('enrichmentPrompt'),
            enrichmentProgressFill: document.getElementById('enrichmentProgressFill'),
            enrichmentStatus: document.getElementById('enrichmentStatus'),
            enrichmentTotalDisplay: document.getElementById('enrichmentTotalStatDisplay'),
            enrichmentAppliedDisplay: document.getElementById('enrichmentAppliedStatDisplay'),
            enrichmentValidatedDisplay: document.getElementById('enrichmentValidatedStatDisplay'),
            enrichmentSkippedDisplay: document.getElementById('enrichmentSkippedStatDisplay'),
            enrichmentFailedDisplay: document.getElementById('enrichmentFailedStatDisplay'),
            enrichmentResultsBody: document.getElementById('enrichmentResultsBody'),
            batchSize: document.getElementById('batchSize'),
        };
    },

    // Initialize
    init(country) {
        this.state.country = country;
        this.updateGeoJsonStatistics();
    },

    // ============= GeoJSON Processing =============

    startGeoJson() {
        const { status, autoEnrichCheckbox } = this.getElements();

        // Close existing connection
        if (this.state.eventSource) {
            this.state.eventSource.close();
        }

        // Reset state
        this.resetGeoJsonState();
        this.state.phase = 'geojson';
        this.state.autoEnrich = autoEnrichCheckbox?.checked || false;
        this.setButtonState(true);

        status.textContent = 'Connecting...';
        this.addLog('Starting GeoJSON processing for ' + this.state.country, 'info');

        this.connectGeoJsonSSE();
    },

    connectGeoJsonSSE() {
        this.state.eventSource = new EventSource('/geojson/' + this.state.country + '/stream');

        this.state.eventSource.addEventListener('progress', (e) => {
            const data = JSON.parse(e.data);
            this.handleGeoJsonEvent(data);
        });

        this.state.eventSource.onerror = () => {
            const { status } = this.getElements();
            status.textContent = 'Connection lost';
            this.addLog('Connection to server lost', 'error');
            this.state.eventSource.close();
            this.state.eventSource = null;
            this.stopTimer();
            this.setButtonState(false);
        };
    },

    handleGeoJsonEvent(data) {
        switch (data.type) {
            case 'started':
                this.handleGeoJsonStarted(data);
                break;
            case 'region_started':
                this.handleRegionStarted(data);
                break;
            case 'adm2_progress':
                this.handleAdm2Progress(data);
                break;
            case 'region_completed':
                this.handleRegionCompleted(data);
                break;
            case 'completed':
                this.handleGeoJsonCompleted(data);
                break;
            case 'error':
                this.handleGeoJsonError(data);
                break;
        }
    },

    handleGeoJsonStarted(data) {
        this.state.geoJson.totalRegions = data.totalRegions;
        this.updateGeoJsonStatistics();

        if (data.regions && data.regions.length > 0) {
            this.renderRegionCards(data.regions);
        }

        this.startTimer();
        this.addLog(`Started processing ${data.totalRegions} regions`, 'info');
    },

    handleRegionStarted(data) {
        this.updateCardState(data.index, 'processing', data);
        this.addLog(`[${data.index + 1}/${this.state.geoJson.totalRegions}] ${data.regionName} - Processing...`, 'info');
    },

    handleAdm2Progress(data) {
        this.updateAdm2Progress(data.adm1Index, data.processedCount, data.totalCount, data.currentAdm2Name);
    },

    handleRegionCompleted(data) {
        this.state.geoJson.processed++;

        if (data.success) {
            this.state.geoJson.successCount++;
            this.updateCardState(data.index, 'completed', data);
            const timeInfo = data.processingTimeMs ? ` (${this.formatProcessingTime(data.processingTimeMs)})` : '';
            this.addLog(`[${data.index + 1}/${this.state.geoJson.totalRegions}] ${data.regionName} - OK${timeInfo}`, 'success');
        } else {
            this.state.geoJson.failCount++;
            this.updateCardState(data.index, 'error', data);
            this.addLog(`[${data.index + 1}/${this.state.geoJson.totalRegions}] ${data.regionName} - Failed: ${data.errorMessage || 'Unknown error'}`, 'error');
        }

        this.updateGeoJsonProgress();
        this.updateGeoJsonStatistics();
    },

    handleGeoJsonCompleted(data) {
        const { progressFill, status } = this.getElements();

        this.stopTimer();
        progressFill.style.width = '100%';

        const totalTime = data.totalProcessingTimeMs ? ` in ${this.formatTime(data.totalProcessingTimeMs)}` : '';
        const adm2Info = data.adm2TotalCount > 0 ? ` (${data.adm2TotalCount} cities processed)` : '';
        status.textContent = `GeoJSON completed! Success: ${data.successCount}, Failed: ${data.failCount}${totalTime}${adm2Info}`;

        const logType = data.failCount > 0 ? 'error' : 'success';
        this.addLog(`GeoJSON completed. Success: ${data.successCount}, Failed: ${data.failCount}${totalTime}${adm2Info}`, logType);

        if (this.state.eventSource) {
            this.state.eventSource.close();
            this.state.eventSource = null;
        }

        // Trigger enrichment based on autoEnrich setting
        this.onGeoJsonCompleted();
    },

    handleGeoJsonError(data) {
        const { status } = this.getElements();

        this.stopTimer();
        status.textContent = 'Error: ' + data.message;
        this.addLog('Error: ' + data.message, 'error');

        if (this.state.eventSource) {
            this.state.eventSource.close();
            this.state.eventSource = null;
        }
        this.setButtonState(false);
    },

    onGeoJsonCompleted() {
        if (this.state.autoEnrich) {
            this.addLog('Auto-starting name enrichment...', 'info');
            this.showEnrichmentSection();
            this.startEnrichment();
        } else {
            this.showEnrichmentPrompt();
            this.setButtonState(false);
        }
    },

    // ============= Name Enrichment Processing =============

    startEnrichment(dryRun = false) {
        const { enrichmentStatus, batchSize } = this.getElements();

        // Close existing connection
        if (this.state.eventSource) {
            this.state.eventSource.close();
        }

        // Reset enrichment state
        this.resetEnrichmentState();
        this.state.phase = 'enrichment';
        this.setButtonState(true);

        this.showEnrichmentSection();
        this.hideEnrichmentPrompt();

        enrichmentStatus.textContent = 'Connecting...';
        this.addLog('Starting name enrichment for ' + this.state.country + (dryRun ? ' (dry run)' : ''), 'info');

        this.connectEnrichmentSSE(dryRun, batchSize?.value || 10);
    },

    connectEnrichmentSSE(dryRun, batchSize) {
        const url = `/geo-names/enrich/${this.state.country}/stream?dryRun=${dryRun}&batchSize=${batchSize}`;
        this.state.eventSource = new EventSource(url);

        this.state.eventSource.addEventListener('progress', (e) => {
            const data = JSON.parse(e.data);
            this.handleEnrichmentEvent(data, dryRun);
        });

        this.state.eventSource.onerror = () => {
            const { enrichmentStatus } = this.getElements();
            enrichmentStatus.textContent = 'Connection lost';
            this.addLog('Connection to server lost', 'error');
            this.state.eventSource.close();
            this.state.eventSource = null;
            this.setButtonState(false);
        };
    },

    handleEnrichmentEvent(data, dryRun) {
        switch (data.type) {
            case 'started':
                this.handleEnrichmentStarted(data);
                break;
            case 'batch_processed':
                this.handleEnrichmentBatchProcessed(data);
                break;
            case 'item_result':
                this.handleEnrichmentItemResult(data);
                break;
            case 'completed':
                this.handleEnrichmentCompleted(data, dryRun);
                break;
            case 'error':
                this.handleEnrichmentError(data);
                break;
        }
    },

    handleEnrichmentStarted(data) {
        const { enrichmentStatus } = this.getElements();
        this.state.enrichment.total = data.totalCount;
        this.updateEnrichmentStatistics();
        enrichmentStatus.textContent = `Processing ${data.totalCount} areas...`;
        this.addLog(`Found ${data.totalCount} areas to enrich`, 'info');
    },

    handleEnrichmentBatchProcessed(data) {
        const { enrichmentProgressFill, enrichmentStatus } = this.getElements();

        const progress = (data.batchIndex / data.totalBatches) * 100;
        enrichmentProgressFill.style.width = progress + '%';

        this.state.enrichment.applied = data.appliedCount;
        this.state.enrichment.validated = data.validatedCount;
        this.state.enrichment.skipped = data.skippedCount;
        this.updateEnrichmentStatistics();

        enrichmentStatus.textContent = `Batch ${data.batchIndex}/${data.totalBatches} completed`;
    },

    handleEnrichmentItemResult(data) {
        this.addEnrichmentResultRow(data);
    },

    handleEnrichmentCompleted(data, dryRun) {
        const { enrichmentProgressFill, enrichmentStatus } = this.getElements();

        enrichmentProgressFill.style.width = '100%';
        const dryRunLabel = dryRun ? ' (dry run)' : '';
        enrichmentStatus.textContent = `Completed! ${data.successCount} success, ${data.failedCount} failed${dryRunLabel}`;

        this.state.phase = 'completed';
        this.addLog(`Name enrichment completed in ${(data.elapsedMs / 1000).toFixed(1)}s${dryRunLabel}`, 'success');

        if (this.state.eventSource) {
            this.state.eventSource.close();
            this.state.eventSource = null;
        }
        this.setButtonState(false);
    },

    handleEnrichmentError(data) {
        this.state.enrichment.failed++;
        this.updateEnrichmentStatistics();
        this.addLog('Error: ' + data.message, 'error');
    },

    addEnrichmentResultRow(data) {
        const { enrichmentResultsBody } = this.getElements();
        if (!enrichmentResultsBody) return;

        const row = document.createElement('tr');
        row.className = 'border-t hover:bg-gray-50';

        const confidenceClass = data.confidence >= 0.8 ? 'confidence-high' :
                               data.confidence >= 0.5 ? 'confidence-medium' : 'confidence-low';
        const statusClass = 'status-' + data.status.toLowerCase();

        row.innerHTML = `
            <td class="px-4 py-2">${data.originalName}</td>
            <td class="px-4 py-2">${data.nameEn || '-'}</td>
            <td class="px-4 py-2">${data.nameJa || '-'}</td>
            <td class="px-4 py-2 text-center ${confidenceClass}">${(data.confidence * 100).toFixed(0)}%</td>
            <td class="px-4 py-2 text-center"><span class="px-2 py-1 rounded text-xs ${statusClass}">${data.status}</span></td>
        `;

        enrichmentResultsBody.appendChild(row);
        enrichmentResultsBody.scrollTop = enrichmentResultsBody.scrollHeight;
    },

    // ============= UI Management =============

    showEnrichmentSection() {
        const { enrichmentSection } = this.getElements();
        if (enrichmentSection) {
            enrichmentSection.classList.remove('hidden');
        }
    },

    hideEnrichmentSection() {
        const { enrichmentSection } = this.getElements();
        if (enrichmentSection) {
            enrichmentSection.classList.add('hidden');
        }
    },

    showEnrichmentPrompt() {
        const { enrichmentPrompt } = this.getElements();
        if (enrichmentPrompt) {
            enrichmentPrompt.classList.remove('hidden');
        }
    },

    hideEnrichmentPrompt() {
        const { enrichmentPrompt } = this.getElements();
        if (enrichmentPrompt) {
            enrichmentPrompt.classList.add('hidden');
        }
    },

    skipEnrichment() {
        this.hideEnrichmentPrompt();
        this.state.phase = 'completed';
        this.addLog('Name enrichment skipped by user', 'info');
        this.setButtonState(false);
    },

    // ============= Statistics & Progress =============

    updateGeoJsonStatistics() {
        const { totalStatDisplay, successStatDisplay, failedStatDisplay, timeStatDisplay } = this.getElements();

        if (totalStatDisplay) totalStatDisplay.textContent = this.state.geoJson.totalRegions;
        if (successStatDisplay) successStatDisplay.textContent = this.state.geoJson.successCount;
        if (failedStatDisplay) failedStatDisplay.textContent = this.state.geoJson.failCount;

        if (timeStatDisplay && this.state.startTime) {
            const elapsed = Date.now() - this.state.startTime;
            timeStatDisplay.textContent = this.formatTime(elapsed);
        }
    },

    updateGeoJsonProgress() {
        const { progressFill, status } = this.getElements();
        const s = this.state.geoJson;
        const percent = s.totalRegions > 0 ? Math.round((s.processed / s.totalRegions) * 100) : 0;
        progressFill.style.width = percent + '%';
        status.textContent = `Processing ${s.processed} / ${s.totalRegions} regions (${percent}%)`;
    },

    updateEnrichmentStatistics() {
        const els = this.getElements();
        const s = this.state.enrichment;

        if (els.enrichmentTotalDisplay) els.enrichmentTotalDisplay.textContent = s.total;
        if (els.enrichmentAppliedDisplay) els.enrichmentAppliedDisplay.textContent = s.applied;
        if (els.enrichmentValidatedDisplay) els.enrichmentValidatedDisplay.textContent = s.validated;
        if (els.enrichmentSkippedDisplay) els.enrichmentSkippedDisplay.textContent = s.skipped;
        if (els.enrichmentFailedDisplay) els.enrichmentFailedDisplay.textContent = s.failed;
    },

    // ============= Region Cards =============

    renderRegionCards(regions) {
        const { regionCards } = this.getElements();
        regionCards.innerHTML = '';

        regions.forEach(region => {
            const card = this.createRegionCard(region);
            regionCards.appendChild(card);
        });

        this.state.geoJson.regions = regions;
    },

    createRegionCard(region) {
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
    },

    updateCardState(index, cardState, data) {
        const card = document.getElementById(`region-card-${index}`);
        if (!card) return;

        card.setAttribute('data-state', cardState);
        const statusIndicator = card.querySelector('.status-indicator');

        switch (cardState) {
            case 'processing':
                statusIndicator.className = 'status-indicator text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-600 animate-pulse';
                statusIndicator.textContent = 'Processing...';
                statusIndicator.classList.remove('hidden');

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
                this.updateCardMetadata(card, data);
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
    },

    updateCardMetadata(card, data) {
        if (!data) return;

        const nameDetails = card.querySelector('.name-details');
        const names = [];
        if (data.nameJa) names.push(data.nameJa);
        if (data.nameEn && data.nameEn !== data.regionName) names.push(data.nameEn);
        if (names.length > 0) {
            nameDetails.textContent = names.join(' / ');
        }

        if (data.thumbnailUrl) {
            const thumbnailContainer = card.querySelector('.thumbnail-container');
            thumbnailContainer.innerHTML = `<img src="${data.thumbnailUrl}" alt="${data.regionName}" class="w-full h-full object-cover">`;
        }

        const metadata = card.querySelector('.metadata');
        metadata.classList.remove('hidden');

        if (data.isoCode) {
            card.querySelector('.iso-code').textContent = `ISO: ${data.isoCode}`;
        }

        if (data.processingTimeMs) {
            card.querySelector('.processing-time').textContent = this.formatProcessingTime(data.processingTimeMs);
        }

        if (data.wikipedia) {
            const wikiLink = card.querySelector('.wiki-link');
            wikiLink.innerHTML = `<a href="https://en.wikipedia.org/wiki/${data.wikipedia}" target="_blank" class="text-blue-500 hover:underline">📖 Wikipedia</a>`;
        }

        if (data.adm2TotalCount > 0) {
            const adm2Progress = card.querySelector('.adm2-progress');
            adm2Progress.classList.remove('hidden');
            card.querySelector('.adm2-count').textContent = `${data.adm2ProcessedCount}/${data.adm2TotalCount}`;
            card.querySelector('.adm2-bar').style.width = '100%';
        }
    },

    updateAdm2Progress(adm1Index, processedCount, totalCount, currentName) {
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
    },

    // ============= Timer & Utilities =============

    startTimer() {
        this.state.startTime = Date.now();
        this.state.timerInterval = setInterval(() => this.updateGeoJsonStatistics(), 1000);
    },

    stopTimer() {
        if (this.state.timerInterval) {
            clearInterval(this.state.timerInterval);
            this.state.timerInterval = null;
        }
    },

    formatTime(ms) {
        const seconds = Math.floor(ms / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);

        if (hours > 0) {
            return `${hours}:${String(minutes % 60).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
        }
        return `${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
    },

    formatProcessingTime(ms) {
        if (ms < 1000) {
            return `${ms}ms`;
        }
        return `${(ms / 1000).toFixed(1)}s`;
    },

    addLog(message, type = 'info') {
        const { log, logCount } = this.getElements();
        if (!log) return;

        const entry = document.createElement('div');
        entry.className = 'log-entry ' + type;
        entry.textContent = new Date().toLocaleTimeString() + ' - ' + message;
        log.appendChild(entry);
        log.scrollTop = log.scrollHeight;

        this.state.logCount++;
        if (logCount) {
            logCount.textContent = this.state.logCount + ' entries';
        }
    },

    setButtonState(isProcessing) {
        const { startBtn, stopBtn } = this.getElements();
        if (startBtn) startBtn.disabled = isProcessing;
        if (stopBtn) stopBtn.disabled = !isProcessing;
    },

    // ============= Reset States =============

    resetGeoJsonState() {
        const { progressFill, status, regionCards, log } = this.getElements();

        if (progressFill) progressFill.style.width = '0%';
        if (status) status.textContent = 'Ready to start';
        if (regionCards) regionCards.innerHTML = '';
        if (log) log.innerHTML = '';

        this.state.geoJson = {
            regions: [],
            totalRegions: 0,
            processed: 0,
            successCount: 0,
            failCount: 0,
        };
        this.state.startTime = null;
        this.state.logCount = 0;

        this.stopTimer();
        this.updateGeoJsonStatistics();
    },

    resetEnrichmentState() {
        const { enrichmentProgressFill, enrichmentStatus, enrichmentResultsBody } = this.getElements();

        if (enrichmentProgressFill) enrichmentProgressFill.style.width = '0%';
        if (enrichmentStatus) enrichmentStatus.textContent = 'Ready to start';
        if (enrichmentResultsBody) enrichmentResultsBody.innerHTML = '';

        this.state.enrichment = {
            total: 0,
            applied: 0,
            validated: 0,
            skipped: 0,
            failed: 0,
        };

        this.updateEnrichmentStatistics();
    },

    // ============= Stop Processing =============

    stop() {
        if (this.state.eventSource) {
            this.state.eventSource.close();
            this.state.eventSource = null;
        }

        this.stopTimer();

        const { status } = this.getElements();
        if (status) status.textContent = 'Stopped by user';
        this.addLog('Processing stopped by user', 'info');
        this.setButtonState(false);
    },
};

// ============= Global Functions (for onclick handlers) =============

function startProcessing(country) {
    IntegratedProgress.init(country);
    IntegratedProgress.startGeoJson();
}

function stopProcessing() {
    IntegratedProgress.stop();
}

function toggleLog() {
    const { logWrapper } = IntegratedProgress.getElements();
    if (logWrapper) {
        logWrapper.classList.toggle('log-collapsed');
    }
}

function startEnrichmentOnly(country, dryRun = false) {
    IntegratedProgress.init(country);
    IntegratedProgress.startEnrichment(dryRun);
}

function skipEnrichment() {
    IntegratedProgress.skipEnrichment();
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    IntegratedProgress.updateGeoJsonStatistics();
});
