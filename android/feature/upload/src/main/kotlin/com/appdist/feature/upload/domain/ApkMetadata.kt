package com.appdist.feature.upload.domain

data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int
)
