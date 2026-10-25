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
        val installedPackages = mutableListOf<String>()
        for (pkg in broadcaster.allPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                installedPackages.add(pkg)
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed, continue checking fallbacks
            }
        }

        return installedPackages.firstOrNull { pm.getLeanbackLaunchIntentForPackage(it) != null }
            ?: installedPackages.firstOrNull()
    }

    /**
     * Uses a verified channel link when available; otherwise opens the TV app and lets the
     * accessibility macro navigate its live-channel menus.
     */
    fun launchLiveProgramme(channel: Channel, programme: Programme): LaunchResult {
        // A tuner lineup row represents the broadcast stream the user selected.
        // If an official broadcaster app (BBC iPlayer, ITVX, etc.) is installed on the device,
        // prioritize it for overlapping channels per user preference (avoiding weak tuner antenna signal).
        // Otherwise, open the tuner stream.
        if (channel.broadcaster == Broadcaster.TUNER || channel.streamUrl != null) {
            val appChannel = channel.overlappingAppChannelId?.let { appChannelId ->
                Channel.ALL_CHANNELS.firstOrNull { it.id == appChannelId }
            }
            if (appChannel != null && findInstalledPackage(appChannel.broadcaster) != null) {
                return launchLiveProgramme(appChannel, programme)
            }

            val streamUrl = channel.streamUrl
            if (!streamUrl.isNullOrBlank()) {
                return launchTunerStream(streamUrl, channel, programme)
            }
            return LaunchResult.Failure("No tuner stream is available for ${channel.shortName}")
        }

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

        val canNavigateMenus =
            com.freeepg.tv.macro.MacroManager.isAccessibilityServiceEnabled(context) &&
                com.freeepg.tv.data.repository.SettingsRepository(context).isMacroAutoLaunchEnabled()

        // Clear any previous navigation before trying a direct link. Otherwise an old macro can
        // take over the broadcaster app after the requested channel has already opened.
        com.freeepg.tv.macro.MacroManager.clearPendingMacro()

        // Channels without a verified direct route need menu navigation to reach the live stream.
        if (channel.deepLinkUris.isEmpty() && !canNavigateMenus) {
            return LaunchResult.Failure(
                "${channel.broadcaster.displayName} needs automatic navigation enabled to select a live channel"
            )
        }

        // 1. Try specific deep link URIs targeted to the installed package
        for (uriString in channel.deepLinkUris) {
            try {
                val uri = Uri.parse(uriString)
                val isWebUri = uriString.startsWith("http://") || uriString.startsWith("https://")

                // Strategy A: If it's a web URI, test with CATEGORY_BROWSABLE and installed package
                if (isWebUri) {
                    val browsableIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(installedPackage)
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("channel_id", channel.id)
                        putExtra("channel_name", channel.name)
                        putExtra("channel", channel.shortName)
                        putExtra("live", true)
                    }
                    if (browsableIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(browsableIntent)
                        Log.d(TAG, "Successfully launched web deep link $uriString via $installedPackage")
                        return LaunchResult.Success(
                            destination = "${channel.name} (${channel.broadcaster.displayName})",
                            method = "Web DeepLink: $uriString"
                        )
                    }
                }

                // Strategy B: Custom scheme or web fallback with default categories (e.g. bbc://, bbciplayer://, itvx://, my5://)
                val directIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(installedPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("channel_id", channel.id)
                    putExtra("channel_name", channel.name)
                    putExtra("channel", channel.shortName)
                    putExtra("live", true)
                }
                if (directIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(directIntent)
                    Log.d(TAG, "Successfully launched direct deep link $uriString via $installedPackage")
                    return LaunchResult.Success(
                        destination = "${channel.name} (${channel.broadcaster.displayName})",
                        method = "Direct DeepLink: $uriString"
                    )
                }

                // Strategy C: Intent without explicit setPackage, verified against broadcaster packages
                val generalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("channel_id", channel.id)
                    putExtra("channel_name", channel.name)
                    putExtra("channel", channel.shortName)
                    putExtra("live", true)
                }
                val resolveInfo = context.packageManager.resolveActivity(generalIntent, 0)
                if (resolveInfo != null && channel.broadcaster.allPackages.contains(resolveInfo.activityInfo?.packageName)) {
                    generalIntent.setPackage(resolveInfo.activityInfo.packageName)
                    context.startActivity(generalIntent)
                    Log.d(TAG, "Successfully launched resolved deep link $uriString via ${resolveInfo.activityInfo.packageName}")
                    return LaunchResult.Success(
                        destination = "${channel.name} (${channel.broadcaster.displayName})",
                        method = "Resolved DeepLink: $uriString"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt with URI $uriString failed: ${e.message}")
            }
        }

        if (!canNavigateMenus) {
            return LaunchResult.Failure(
                "${channel.broadcaster.displayName} could not open ${channel.shortName} directly. Enable automatic navigation to continue."
            )
        }

        // The direct route was unavailable, so use the configured on-screen navigation steps.
        com.freeepg.tv.macro.MacroManager.armMacro(channel, installedPackage)

        // 2. Direct fallback launch of the installed broadcaster TV app
        try {
            val pm = context.packageManager
            var launchIntent = pm.getLeanbackLaunchIntentForPackage(installedPackage)
                ?: pm.getLaunchIntentForPackage(installedPackage)

            if (launchIntent == null) {
                if (installedPackage == "bbc.iplayer.android" || installedPackage == "uk.co.bbc.iplayer") {
                    launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        component = android.content.ComponentName(
                            installedPackage,
                            "external.androidtv.psbwrapper.deeplinking.DeepLinkActivity"
                        )
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                } else if (installedPackage == "air.ITVMobilePlayer" || installedPackage == "com.itv.tenft.itvhub") {
                    launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        component = android.content.ComponentName(
                            installedPackage,
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
                launchIntent.putExtra("channel_id", channel.id)
                launchIntent.putExtra("channel_name", channel.name)
                launchIntent.putExtra("channel", channel.shortName)
                launchIntent.putExtra("live", true)
                context.startActivity(launchIntent)
                Log.d(TAG, "Launched installed app $installedPackage")
                return LaunchResult.Success(
                    destination = channel.broadcaster.displayName,
                    method = if (channel.deepLinkUris.isEmpty()) {
                        "App Launcher + Accessibility Navigation: $installedPackage"
                    } else {
                        "App Launcher: $installedPackage"
                    }
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

    /** Opens tuner playback in-app so playback can show the programme information overlay. */
    fun launchTunerStream(streamUrl: String, channel: Channel, programme: Programme): LaunchResult {
        return try {
            val intent = Intent(context, com.freeepg.tv.ui.TunerPlayerActivity::class.java).apply {
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_STREAM_URL, streamUrl)
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_CHANNEL_NAME, channel.shortName)
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_CHANNEL_NUMBER, channel.channelNumber)
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_PROGRAMME_TITLE, programme.title)
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_PROGRAMME_SYNOPSIS, programme.synopsis)
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_PROGRAMME_START, programme.startTimeEpochSeconds)
                putExtra(com.freeepg.tv.ui.TunerPlayerActivity.EXTRA_PROGRAMME_DURATION, programme.durationSeconds)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Success(
                destination = channel.shortName,
                method = "In-app tuner player: $streamUrl"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start in-app tuner player for ${channel.name}", e)
            LaunchResult.Failure("Unable to play tuner stream for ${channel.shortName}: ${e.message}")
        }
    }

    /**
     * Plays an HDHomeRun live broadcast stream in an external media player (VLC, HDHomeRun View, Just Player, Nova).
     */
    fun launchExternalTunerStream(streamUrl: String): Boolean {
        val streamUri = Uri.parse(streamUrl)
        val pm = context.packageManager
        for (pkg in Broadcaster.TUNER.allPackages) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(streamUri, "video/*")
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return true
            }
        }
        val genericIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(streamUri, "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (genericIntent.resolveActivity(pm) != null) {
            context.startActivity(genericIntent)
            true
        } else {
            false
        }
    }
}
