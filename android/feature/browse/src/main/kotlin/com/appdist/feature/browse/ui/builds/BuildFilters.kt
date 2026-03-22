package com.appdist.feature.browse.ui.builds

data class BuildFilters(
    val channel: String? = null,
    val environment: String? = null,
    val searchQuery: String = ""
) {
    val isEmpty get() = channel == null && environment == null && searchQuery.isBlank()
}
