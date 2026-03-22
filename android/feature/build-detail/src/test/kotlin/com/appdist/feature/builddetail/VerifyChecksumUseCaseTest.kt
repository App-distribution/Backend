package com.appdist.feature.builddetail

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.builddetail.domain.VerifyChecksumUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest

class VerifyChecksumUseCaseTest {

    private val useCase = VerifyChecksumUseCase()

    @Test
    fun `returns Success when SHA256 matches`(@TempDir tempDir: File) {
        val file = File(tempDir, "test.apk").also { it.writeText("hello world") }
        val actual = computeSha256(file)
        val result = useCase(file, actual)
        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `returns ChecksumMismatch on wrong hash`(@TempDir tempDir: File) {
        val file = File(tempDir, "test.apk").also { it.writeText("hello world") }
        val result = useCase(file, "wrong_hash")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is AppError.ChecksumMismatch)
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var bytes = stream.read(buffer)
            while (bytes != -1) { digest.update(buffer, 0, bytes); bytes = stream.read(buffer) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
