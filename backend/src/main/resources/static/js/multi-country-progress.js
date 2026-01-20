/**
 * Multi-Country Progress - SSE-based progress tracking for All Countries processing
 * Supports both GeoJSON and Geo-Names enrichment modes
 */

const MultiCountryProgress = {
    // State management
    state: {
        mode: null, // 'geojson' | 'geo-names'
        eventSource: null,
        startTime: null,
        timerInterval: null,
        logCount: 0,
        totalCountries: 0,
        completedCountries: 0,
        successCountries: 0,
        failCountries: 0,
        currentCountryIndex: -1,
        currentCountryTotalRegions: 0,
        currentCountryProcessedRegions: 0,
        autoEnrich: false,
    },

    // DOM element references
    getElements() {
        return {
            // Control buttons
            startBtn: document.getElementById('startBtn'),
            stopBtn: document.getElementById('stopBtn'),
            autoEnrichCheckbox: document.getElementById('autoEnrichCheckbox'),
            batchSize: document.getElementById('batchSize'),

            // Overall progress
            overallProgressFill: document.getElementById('overallProgressFill'),
            overallProgressText: document.getElementById('overallProgressText'),
            overallStatus: document.getElementById('overallStatus'),

            // Current country section
            currentCountrySection: document.getElementById('currentCountrySection'),
            currentCountryName: document.getElementById('currentCountryName'),
            currentCountryProgressFill: document.getElementById('currentCountryProgressFill'),
            currentCountryStatus: document.getElementById('currentCountryStatus'),

            // Statistics
            totalStatDisplay: document.getElementById('totalStatDisplay'),
            successStatDisplay: document.getElementById('successStatDisplay'),
            failedStatDisplay: document.getElementById('failedStatDisplay'),
            timeStatDisplay: document.getElementById('timeStatDisplay'),

            // Log
            log: document.getElementById('log'),
            logCount: document.getElementById('logCount'),
            logWrapper: document.getElementById('logWrapper'),
        };
    },

    // ============= Main Control =============

    start(mode) {
        const { autoEnrichCheckbox } = this.getElements();

        // Close existing connection
        if (this.state.eventSource) {
            this.state.eventSource.close();
        }

        // Reset state
        this.resetState();
        this.state.mode = mode;
        this.state.autoEnrich = autoEnrichCheckbox?.checked || false;
        this.setButtonState(true);

        this.updateOverallStatus('Connecting...');
        this.addLog(`Starting ${mode} processing for all countries`, 'info');

        this.connectSSE(mode);
    },

    stop() {
        if (this.state.eventSource) {
            this.state.eventSource.close();
            this.state.eventSource = null;
        }

        this.stopTimer();
        this.updateOverallStatus('Stopped by user');
        this.addLog('Processing stopped by user', 'warning');
        this.setButtonState(false);
    },

    connectSSE(mode) {
        let url;
        if (mode === 'geojson') {
            url = '/geojson/all/stream';
        } else {
            const batchSize = this.getElements().batchSize?.value || 10;
            url = `/geo-names/enrich/all/stream?batchSize=${batchSize}`;
        }

        this.state.eventSource = new EventSource(url);

        this.state.eventSource.addEventListener('progress', (e) => {
            const data = JSON.parse(e.data);
            this.handleEvent(data);
        });

        this.state.eventSource.onerror = () => {
            this.updateOverallStatus('Connection lost');
            this.addLog('Connection to server lost', 'error');
            this.state.eventSource.close();
            this.state.eventSource = null;
            this.stopTimer();
            this.setButtonState(false);
        };
    },

    // ============= Event Handlers =============

    handleEvent(data) {
        switch (data.type) {
            case 'all_started':
                this.handleAllStarted(data);
                break;
            case 'country_started':
                this.handleCountryStarted(data);
                break;
            case 'country_progress':
                this.handleCountryProgress(data);
                break;
            case 'country_completed':
                this.handleCountryCompleted(data);
                break;
            case 'all_completed':
                this.handleAllCompleted(data);
                break;
            case 'error':
                this.handleError(data);
                break;
        }
    },

    handleAllStarted(data) {
        this.state.totalCountries = data.totalCountries;
        this.updateStatistics();
        this.startTimer();
        this.addLog(`Started processing ${data.totalCountries} countries`, 'info');
    },

    handleCountryStarted(data) {
        this.state.currentCountryIndex = data.countryIndex;
        this.state.currentCountryTotalRegions = 0;
        this.state.currentCountryProcessedRegions = 0;

        // Update country card state
        this.updateCountryCardState(data.countryIndex, 'processing');

        // Show current country section
        const { currentCountrySection, currentCountryName, currentCountryProgressFill, currentCountryStatus } = this.getElements();
        if (currentCountrySection) {
            currentCountrySection.classList.remove('hidden');
        }
        if (currentCountryName) {
            currentCountryName.textContent = data.countryName;
        }
        if (currentCountryProgressFill) {
            currentCountryProgressFill.style.width = '0%';
        }
        if (currentCountryStatus) {
            currentCountryStatus.textContent = 'Starting...';
        }

        this.updateOverallStatus(`Processing ${data.countryName} (${data.countryIndex + 1}/${this.state.totalCountries})`);
        this.addLog(`[${data.countryIndex + 1}/${this.state.totalCountries}] ${data.countryName} - Starting...`, 'info');
    },

    handleCountryProgress(data) {
        // Parse the inner event
        const innerEvent = JSON.parse(data.innerEventJson);

        switch (data.innerEventType) {
            case 'started':
                this.state.currentCountryTotalRegions = innerEvent.totalRegions;
                this.updateCurrentCountryStatus(`0/${innerEvent.totalRegions} regions`);
                break;

            case 'region_started':
                this.updateCurrentCountryStatus(`Processing ${innerEvent.regionName}...`);
                break;

            case 'adm2_progress':
                // Update minor progress
                break;

            case 'region_completed':
                this.state.currentCountryProcessedRegions++;
                const percent = Math.round((this.state.currentCountryProcessedRegions / this.state.currentCountryTotalRegions) * 100);
                this.updateCurrentCountryProgress(percent);
                this.updateCurrentCountryStatus(`${this.state.currentCountryProcessedRegions}/${this.state.currentCountryTotalRegions} regions`);

                // Also update the country card's inner progress
                this.updateCountryCardProgress(
                    data.countryIndex,
                    this.state.currentCountryProcessedRegions,
                    this.state.currentCountryTotalRegions
                );
                break;

            // For geo-names enrichment
            case 'batch_processed':
                const batchPercent = Math.round((innerEvent.batchIndex / innerEvent.totalBatches) * 100);
                this.updateCurrentCountryProgress(batchPercent);
                this.updateCurrentCountryStatus(`Batch ${innerEvent.batchIndex}/${innerEvent.totalBatches}`);
                this.updateCountryCardProgress(data.countryIndex, innerEvent.batchIndex, innerEvent.totalBatches);
                break;

            case 'completed':
                // Inner completion (will be followed by country_completed)
                break;
        }
    },

    handleCountryCompleted(data) {
        this.state.completedCountries++;

        if (data.success) {
            this.state.successCountries++;
            this.updateCountryCardState(data.countryIndex, 'completed');
            const timeInfo = data.processingTimeMs ? ` (${this.formatProcessingTime(data.processingTimeMs)})` : '';
            this.addLog(`[${data.countryIndex + 1}/${this.state.totalCountries}] ${data.countryName} - Completed${timeInfo}`, 'success');
        } else {
            this.state.failCountries++;
            this.updateCountryCardState(data.countryIndex, 'error');
            this.addLog(`[${data.countryIndex + 1}/${this.state.totalCountries}] ${data.countryName} - Failed: ${data.errorMessage || 'Unknown error'}`, 'error');
        }

        // Update overall progress
        const overallPercent = Math.round((this.state.completedCountries / this.state.totalCountries) * 100);
        this.updateOverallProgress(overallPercent);
        this.updateStatistics();

        // Hide current country section if this was the last country
        if (this.state.completedCountries === this.state.totalCountries) {
            const { currentCountrySection } = this.getElements();
            if (currentCountrySection) {
                currentCountrySection.classList.add('hidden');
            }
        }
    },

    handleAllCompleted(data) {
        this.stopTimer();
        this.state.eventSource?.close();
        this.state.eventSource = null;

        const totalTime = this.formatTime(data.totalTimeMs);
        const logType = data.failCount > 0 ? 'warning' : 'success';

        this.updateOverallProgress(100);
        this.updateOverallStatus(`Completed! Success: ${data.successCount}, Failed: ${data.failCount} (${totalTime})`);
        this.addLog(`All countries completed. Success: ${data.successCount}, Failed: ${data.failCount} (${totalTime})`, logType);

        // Handle auto-enrich for geojson mode
        if (this.state.mode === 'geojson' && this.state.autoEnrich) {
            this.addLog('Auto-starting name enrichment for all countries...', 'info');
            // Redirect to geo-names all countries page
            setTimeout(() => {
                window.location.href = '/geo-names/enrich/all';
            }, 2000);
        } else {
            this.setButtonState(false);
        }
    },

    handleError(data) {
        this.addLog(`Error: ${data.message}${data.countryCode ? ` (${data.countryCode})` : ''}`, 'error');
    },

    // ============= UI Updates =============

    updateCountryCardState(index, cardState) {
        const card = document.getElementById(`country-card-${index}`);
        if (!card) return;

        card.setAttribute('data-state', cardState);

        const statusIcon = document.getElementById(`country-status-${index}`);
        if (statusIcon) {
            statusIcon.classList.remove('hidden');
            switch (cardState) {
                case 'processing':
                    statusIcon.textContent = '⟳';
                    statusIcon.className = 'status-icon text-lg text-blue-500 animate-pulse';
                    break;
                case 'completed':
                    statusIcon.textContent = '✓';
                    statusIcon.className = 'status-icon text-lg text-green-500';
                    break;
                case 'error':
                    statusIcon.textContent = '✗';
                    statusIcon.className = 'status-icon text-lg text-red-500';
                    break;
            }
        }

        // Show/hide progress bar based on state
        const progressContainer = document.getElementById(`country-progress-${index}`);
        if (progressContainer) {
            if (cardState === 'processing') {
                progressContainer.classList.remove('hidden');
            } else if (cardState === 'completed' || cardState === 'error') {
                // Keep visible to show final state
            }
        }
    },

    updateCountryCardProgress(index, current, total) {
        const progressBar = document.getElementById(`country-progress-bar-${index}`);
        const progressText = document.getElementById(`country-progress-text-${index}`);

        if (progressBar) {
            const percent = total > 0 ? Math.round((current / total) * 100) : 0;
            progressBar.style.width = `${percent}%`;
        }
        if (progressText) {
            progressText.textContent = `${current}/${total}`;
        }
    },

    updateOverallProgress(percent) {
        const { overallProgressFill, overallProgressText } = this.getElements();
        if (overallProgressFill) {
            overallProgressFill.style.width = `${percent}%`;
        }
        if (overallProgressText) {
            overallProgressText.textContent = `${this.state.completedCountries} / ${this.state.totalCountries} countries`;
        }
    },

    updateOverallStatus(message) {
        const { overallStatus } = this.getElements();
        if (overallStatus) {
            overallStatus.textContent = message;
        }
    },

    updateCurrentCountryProgress(percent) {
        const { currentCountryProgressFill } = this.getElements();
        if (currentCountryProgressFill) {
            currentCountryProgressFill.style.width = `${percent}%`;
        }
    },

    updateCurrentCountryStatus(message) {
        const { currentCountryStatus } = this.getElements();
        if (currentCountryStatus) {
            currentCountryStatus.textContent = message;
        }
    },

    updateStatistics() {
        const { totalStatDisplay, successStatDisplay, failedStatDisplay, timeStatDisplay } = this.getElements();

        if (totalStatDisplay) totalStatDisplay.textContent = this.state.totalCountries;
        if (successStatDisplay) successStatDisplay.textContent = this.state.successCountries;
        if (failedStatDisplay) failedStatDisplay.textContent = this.state.failCountries;

        if (timeStatDisplay && this.state.startTime) {
            const elapsed = Date.now() - this.state.startTime;
            timeStatDisplay.textContent = this.formatTime(elapsed);
        }
    },

    // ============= Timer & Utilities =============

    startTimer() {
        this.state.startTime = Date.now();
        this.state.timerInterval = setInterval(() => this.updateStatistics(), 1000);
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

    resetState() {
        const { overallProgressFill, overallStatus, log, currentCountrySection } = this.getElements();

        if (overallProgressFill) overallProgressFill.style.width = '0%';
        if (overallStatus) overallStatus.textContent = 'Ready to start';
        if (log) log.innerHTML = '';
        if (currentCountrySection) currentCountrySection.classList.add('hidden');

        // Reset all country cards
        document.querySelectorAll('.country-card').forEach((card, index) => {
            card.setAttribute('data-state', 'pending');
            const statusIcon = document.getElementById(`country-status-${index}`);
            if (statusIcon) {
                statusIcon.classList.add('hidden');
            }
            const progressContainer = document.getElementById(`country-progress-${index}`);
            if (progressContainer) {
                progressContainer.classList.add('hidden');
            }
            const progressBar = document.getElementById(`country-progress-bar-${index}`);
            if (progressBar) {
                progressBar.style.width = '0%';
            }
        });

        this.state = {
            mode: null,
            eventSource: null,
            startTime: null,
            timerInterval: null,
            logCount: 0,
            totalCountries: 0,
            completedCountries: 0,
            successCountries: 0,
            failCountries: 0,
            currentCountryIndex: -1,
            currentCountryTotalRegions: 0,
            currentCountryProcessedRegions: 0,
            autoEnrich: false,
        };

        this.stopTimer();
    },
};

// ============= Global Functions (for onclick handlers) =============

function startAllCountriesProcessing(mode) {
    MultiCountryProgress.start(mode);
}

function stopAllCountriesProcessing() {
    MultiCountryProgress.stop();
}

function toggleLog() {
    const { logWrapper } = MultiCountryProgress.getElements();
    if (logWrapper) {
        logWrapper.classList.toggle('log-collapsed');
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    MultiCountryProgress.updateStatistics();
});
