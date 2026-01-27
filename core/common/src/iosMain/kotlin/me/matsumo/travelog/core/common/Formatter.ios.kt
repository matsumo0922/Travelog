package me.matsumo.travelog.core.common

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun formatCacheSize(bytes: Long?): String {
    if (bytes == null) return "..."
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> NSString.stringWithFormat("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> NSString.stringWithFormat("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> NSString.stringWithFormat("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
