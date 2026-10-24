package com.freeepg.tv.deeplink

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme

sealed class LaunchResult {
    data class Success(val destination: String, val method: String) : LaunchResult()
    data class AppNotInstalled(
        val broadcaster: Broadcaster,
        val channel: Channel,
        val programme: Programme,
        val primaryPackage: String,
        val playStoreUri: Uri
    ) : LaunchResult()
    data class Failure(val reason: String) : LaunchResult()
}

class DeepLinkManager(private val context: Context) {

    companion object {
        private const val TAG = "DeepLinkManager"
    }

    /**
     * Checks which (if any) package for this broadcaster is currently installed on this TV.
     */
    fun findInstalledPackage(broadcaster: Broadcaster): String? {
        val pm = context.packageManager
        for (pkg in broadcaster.allPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed, continue checking fallbacks
            }
        }
        return null
    }

    /**
     * Launches directly into the broadcaster app's live stream for this channel and programme.
     */
    fun launchLiveProgramme(channel: Channel, programme: Programme): LaunchResult {
        val installedPackage = findInstalledPackage(channel.broadcaster)

        if (installedPackage == null) {
            val storeUri = Uri.parse("market://details?id=${channel.broadcaster.primaryPackage}")
            return LaunchResult.AppNotInstalled(
                broadcaster = channel.broadcaster,
                channel = channel,
                programme = programme,
                primaryPackage = channel.broadcaster.primaryPackage,
                playStoreUri = storeUri
            )
        }

        // Arm the accessibility macro only if auto-navigation is enabled and accessibility service is active
        if (com.freeepg.tv.macro.MacroManager.isAccessibilityServiceEnabled(context) &&
            com.freeepg.tv.data.repository.SettingsRepository(context).isMacroAutoLaunchEnabled()
        ) {
            com.freeepg.tv.macro.MacroManager.armMacro(channel, installedPackage)
        } else {
            com.freeepg.tv.macro.MacroManager.clearPendingMacro()
        }

        // 1. Try specific deep link URIs targeted to the installed package
        for (uriString in channel.deepLinkUris) {
            try {
                val uri = Uri.parse(uriString)
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(installedPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Successfully launched deep link $uriString via $installedPackage")
                    return LaunchResult.Success(
                        destination = "${channel.name} (${channel.broadcaster.displayName})",
                        method = "DeepLink: $uriString"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt with URI $uriString failed: ${e.message}")
            }
        }

        // 2. Direct fallback launch of the installed broadcaster TV app
        try {
            val pm = context.packageManager
            var launchIntent = pm.getLeanbackLaunchIntentForPackage(installedPackage)
                ?: pm.getLaunchIntentForPackage(installedPackage)

            if (launchIntent == null) {
                if (installedPackage == "bbc.iplayer.android") {
                    launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        component = android.content.ComponentName(
                            "bbc.iplayer.android",
                            "external.androidtv.psbwrapper.deeplinking.DeepLinkActivity"
                        )
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                } else if (installedPackage == "air.ITVMobilePlayer") {
                    launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        component = android.content.ComponentName(
                            "air.ITVMobilePlayer",
                            "com.itv.tenft.itvhub.ItvActivity"
                        )
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                } else {
                    val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                        `package` = installedPackage
                    }
                    val resolveInfo = pm.queryIntentActivities(mainIntent, 0).firstOrNull()
                        ?: pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply { `package` = installedPackage }, 0).firstOrNull()
                    resolveInfo?.activityInfo?.let { act ->
                        launchIntent = Intent(Intent.ACTION_MAIN).apply {
                            setClassName(act.packageName, act.name)
                            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                        }
                    }
                }
            }

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched installed app $installedPackage")
                return LaunchResult.Success(
                    destination = channel.broadcaster.displayName,
                    method = "App Launcher: $installedPackage"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package $installedPackage: ${e.message}")
        }

        return LaunchResult.Failure("Unable to launch ${channel.broadcaster.displayName}")
    }

    /**
     * Opens Google Play Store to the broadcaster app page so the user can easily install it.
     */
    fun openPlayStore(packageName: String): Boolean {
        return try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            true
        } catch (e: ActivityNotFoundException) {
            try {
                val webStoreIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webStoreIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Opens fallback web stream in browser
     */
    fun openWebLiveStream(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
