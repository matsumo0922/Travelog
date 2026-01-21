package ui.common

/**
 * 共通スタイル定義
 */
object PageStyles {
    /**
     * ログエントリのスタイル
     */
    val logStyles = """
        .log-entry.success { color: #4CAF50; }
        .log-entry.error { color: #f44336; }
        .log-entry.info { color: #2196F3; }
        .log-entry.warning { color: #FF9800; }

        .log-collapsed .log-content { display: none; }
        .log-toggle-icon { transition: transform 0.2s; }
        .log-collapsed .log-toggle-icon { transform: rotate(-90deg); }
    """.trimIndent()

    /**
     * リージョンカードの状態スタイル
     */
    val regionCardStateStyles = """
        /* Card states */
        .region-card[data-state="pending"] {
            border: 2px solid transparent;
            opacity: 0.7;
        }
        .region-card[data-state="processing"] {
            border: 2px solid #3B82F6;
            box-shadow: 0 0 10px rgba(59, 130, 246, 0.3);
        }
        .region-card[data-state="completed"] {
            border: 2px solid #10B981;
        }
        .region-card[data-state="error"] {
            border: 2px solid #EF4444;
        }
    """.trimIndent()

    /**
     * 国カードの状態スタイル
     */
    val countryCardStateStyles = """
        /* Country card states */
        .country-card[data-state="pending"] {
            opacity: 0.6;
        }
        .country-card[data-state="processing"] {
            border: 2px solid #3B82F6;
            box-shadow: 0 0 10px rgba(59, 130, 246, 0.3);
            opacity: 1;
        }
        .country-card[data-state="completed"] {
            border: 2px solid #10B981;
            opacity: 1;
        }
        .country-card[data-state="error"] {
            border: 2px solid #EF4444;
            opacity: 1;
        }
    """.trimIndent()

    /**
     * 名前補完用の国カードスタイル（紫系）
     */
    val enrichmentCountryCardStateStyles = """
        /* Country card states */
        .country-card[data-state="pending"] {
            opacity: 0.6;
        }
        .country-card[data-state="processing"] {
            border: 2px solid #A855F7;
            box-shadow: 0 0 10px rgba(168, 85, 247, 0.3);
            opacity: 1;
        }
        .country-card[data-state="completed"] {
            border: 2px solid #10B981;
            opacity: 1;
        }
        .country-card[data-state="error"] {
            border: 2px solid #EF4444;
            opacity: 1;
        }
    """.trimIndent()

    /**
     * Enrichment ステータスバッジのスタイル
     */
    val enrichmentStatusStyles = """
        .status-applied { background-color: #10B981; color: white; }
        .status-validated { background-color: #3B82F6; color: white; }
        .status-skipped { background-color: #F59E0B; color: white; }
        .status-error { background-color: #EF4444; color: white; }

        /* Confidence colors */
        .confidence-high { color: #10B981; }
        .confidence-medium { color: #F59E0B; }
        .confidence-low { color: #EF4444; }
    """.trimIndent()

    /**
     * アニメーションスタイル
     */
    val animationStyles = """
        /* Pulse animation */
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
        .animate-pulse { animation: pulse 2s infinite; }

        /* Progress bar animation */
        .progress-fill {
            transition: width 0.3s ease-out;
        }
    """.trimIndent()
}
