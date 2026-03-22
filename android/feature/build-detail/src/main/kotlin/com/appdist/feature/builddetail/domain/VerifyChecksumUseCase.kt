package com.appdist.feature.builddetail.domain

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class VerifyChecksumUseCase @Inject constructor() {
    operator fun invoke(file: File, expectedSha256: String): Result<Unit> {
        val actual = file.computeSha256()
        return if (actual.equals(expectedSha256, ignoreCase = true)) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.ChecksumMismatch(expectedSha256, actual))
        }
    }

    private fun File.computeSha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
