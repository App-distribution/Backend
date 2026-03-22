package com.appdist.feature.builddetail

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.appdist.core.common.model.InstallStatus
import com.appdist.feature.builddetail.domain.GetInstallStatusUseCase
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetInstallStatusUseCaseTest {

    private val pm = mockk<PackageManager>()
    private val useCase = GetInstallStatusUseCase(pm)

    @Test
    fun `returns NotInstalled when package not found`() {
        every { pm.getPackageInfo("com.test", any<Int>()) } throws PackageManager.NameNotFoundException()
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 0)
        assertEquals(InstallStatus.NotInstalled, status)
    }

    @Test
    fun `returns Installed when versionCodes match`() {
        val info = mockk<PackageInfo> { every { longVersionCode } returns 100L }
        every { pm.getPackageInfo("com.test", any<Int>()) } returns info
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 0)
        assertEquals(InstallStatus.Installed(100L), status)
    }

    @Test
    fun `returns UpdateAvailable when available versionCode is higher`() {
        val info = mockk<PackageInfo> { every { longVersionCode } returns 99L }
        every { pm.getPackageInfo("com.test", any<Int>()) } returns info
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 0)
        assertEquals(InstallStatus.UpdateAvailable(99L, 100L), status)
    }

    @Test
    fun `returns InstalledNewer when installed versionCode is higher`() {
        val info = mockk<PackageInfo> { every { longVersionCode } returns 101L }
        every { pm.getPackageInfo("com.test", any<Int>()) } returns info
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 0)
        assertEquals(InstallStatus.InstalledNewer(101L, 100L), status)
    }

    @Test
    fun `returns Incompatible when minSdk exceeds device SDK`() {
        val status = useCase("com.test", versionCode = 100L, certFingerprint = null, minSdk = 999)
        assertEquals(InstallStatus.Incompatible, status)
    }
}
