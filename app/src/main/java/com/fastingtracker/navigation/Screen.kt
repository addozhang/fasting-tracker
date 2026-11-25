package com.fastingtracker.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe top-level routes for the 3-Tab Organic Minimal navigation architecture.
 */
@Serializable
data object TimerRoute

@Serializable
data object StatsRoute

@Serializable
data object SettingsRoute
