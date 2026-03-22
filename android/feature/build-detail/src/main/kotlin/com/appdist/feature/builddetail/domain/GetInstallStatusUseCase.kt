package com.appdist.feature.builddetail.domain

import android.content.pm.PackageManager
import android.os.Build
import com.appdist.core.common.model.InstallStatus
import javax.inject.Inject

class GetInstallStatusUseCase @Inject constructor(
    private val packageManager: PackageManager
) {
    operator fun invoke(
        packageName: String,
        versionCode: Long,
        certFingerprint: String?,
        minSdk: Int
    ): InstallStatus {
        if (minSdk > Build.VERSION.SDK_INT) return InstallStatus.Incompatible

        val installedInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } catch (e: PackageManager.NameNotFoundException) {
            return InstallStatus.NotInstalled
        }

        val installedVersionCode = installedInfo.longVersionCode

        if (certFingerprint != null) {
            val signingInfo = installedInfo.signingInfo
            val installedFingerprint = signingInfo?.apkContentsSigners
                ?.firstOrNull()
                ?.let { sig ->
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(sig.toByteArray())
                        .joinToString(":") { "%02X".format(it) }
                }
            if (installedFingerprint != null && installedFingerprint != certFingerprint) {
                return InstallStatus.SignatureMismatch(installedFingerprint)
            }
        }

        return when {
            installedVersionCode == versionCode -> InstallStatus.Installed(installedVersionCode)
            installedVersionCode < versionCode -> InstallStatus.UpdateAvailable(installedVersionCode, versionCode)
            else -> InstallStatus.InstalledNewer(installedVersionCode, versionCode)
        }
    }
}
