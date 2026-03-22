package com.appdist.infrastructure.storage

import com.appdist.config.AppConfig
import io.minio.*
import io.minio.http.Method
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MinioStorageClient(private val config: AppConfig.StorageConfig) : StorageClient {
    private val client = MinioClient.builder()
        .endpoint(config.endpoint)
        .credentials(config.accessKey, config.secretKey)
        .build()

    init {
        ensureBucketExists(config.bucket)
    }

    override fun ensureBucketExists(bucket: String) {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    override fun upload(key: String, inputStream: InputStream, size: Long, contentType: String) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(config.bucket)
                .`object`(key)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        )
    }

    override fun generateDownloadUrl(key: String, ttlMinutes: Long): String =
        client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(config.bucket)
                .`object`(key)
                .expiry(ttlMinutes.toInt(), TimeUnit.MINUTES)
                .build()
        )

    override fun delete(key: String) {
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(config.bucket)
                .`object`(key)
                .build()
        )
    }
}
