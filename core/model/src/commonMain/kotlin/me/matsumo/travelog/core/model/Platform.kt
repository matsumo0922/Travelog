package me.matsumo.travelog.core.model

expect val currentPlatform: Platform

enum class Platform {
    Android,
    IOS,
    JVM,
}
