package com.appdist.infrastructure.storage

import java.io.InputStream

interface StorageClient {
    fun upload(key: String, inputStream: InputStream, size: Long, contentType: String)
    fun generateDownloadUrl(key: String, ttlMinutes: Long): String
    fun delete(key: String)
    fun ensureBucketExists(bucket: String)
}
