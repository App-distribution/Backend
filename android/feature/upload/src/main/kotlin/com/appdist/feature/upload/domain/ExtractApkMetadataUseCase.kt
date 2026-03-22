package com.appdist.feature.upload.domain

import android.content.Context
import android.content.pm.PackageManager
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class ExtractApkMetadataUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(apkFile: File): Result<ApkMetadata> = try {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_META_DATA)
            ?: return Result.Error(AppError.Unknown("Cannot parse APK"))

        info.applicationInfo?.sourceDir = apkFile.absolutePath
        info.applicationInfo?.publicSourceDir = apkFile.absolutePath

        Result.Success(ApkMetadata(
            packageName = info.packageName,
            versionName = info.versionName ?: "unknown",
            versionCode = info.longVersionCode,
            minSdk = info.applicationInfo?.minSdkVersion ?: 1,
            targetSdk = info.applicationInfo?.targetSdkVersion ?: 1
        ))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown("APK parse error: ${e.message}"))
    }
}
