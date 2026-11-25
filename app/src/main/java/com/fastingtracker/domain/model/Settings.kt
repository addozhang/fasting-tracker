package com.fastingtracker.domain.model

/**
 * App theme preference
 */
enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * App language/locale preference
 */
enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    CHINESE("zh")
}
