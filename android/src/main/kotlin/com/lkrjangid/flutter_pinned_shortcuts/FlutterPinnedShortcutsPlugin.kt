package com.lkrjangid.flutter_pinned_shortcuts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.NewIntentListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * FlutterPinnedShortcutsPlugin - A Flutter plugin for managing Android pinned shortcuts
 */
class FlutterPinnedShortcutsPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, NewIntentListener {

    companion object {
        private const val TAG = "PinnedShortcutsPlugin"
        private const val CHANNEL = "flutter_pinned_shortcuts"
        private const val SHORTCUT_CLICKED_ACTION = "com.lkrjangid.flutter_pinned_shortcuts.SHORTCUT_CLICKED"
        private const val EXTRA_SHORTCUT_ID = "shortcut_id"
        private const val EXTRA_SHORTCUT_DATA = "shortcut_data"

        // Legacy action constants for compatibility with older Android versions
        private const val INSTALL_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT"
        private const val UNINSTALL_SHORTCUT_ACTION = "com.android.launcher.action.UNINSTALL_SHORTCUT"
    }

    // Plugin properties
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Store the launch shortcut ID to retrieve it later
    private var launchShortcutId: String? = null

    //----------------------------------------------------------------------------------------------
    // FlutterPlugin interface implementation
    //----------------------------------------------------------------------------------------------

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        Log.d(TAG, "Plugin attached to engine")
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        Log.d(TAG, "Plugin detached from engine")
    }

    //----------------------------------------------------------------------------------------------
    // ActivityAware interface implementation
    //----------------------------------------------------------------------------------------------

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(TAG, "Plugin attached to activity")
        activity = binding.activity
        binding.addOnNewIntentListener(this)

        // Check if the activity was launched from a shortcut
        val intent = activity?.intent
        if (intent != null) {
            if (handleIntent(intent)) {
                // Store the shortcut ID for retrieval when Flutter is ready
                launchShortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID)
                Log.d(TAG, "Activity was launched from shortcut: $launchShortcutId")
            }
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d(TAG, "Plugin detached from activity for config changes")
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(TAG, "Plugin reattached to activity for config changes")
        activity = binding.activity
        binding.addOnNewIntentListener(this)
    }

    override fun onDetachedFromActivity() {
        Log.d(TAG, "Plugin detached from activity")
        activity = null
    }

    //----------------------------------------------------------------------------------------------
    // NewIntentListener interface implementation
    //----------------------------------------------------------------------------------------------

    override fun onNewIntent(intent: Intent): Boolean {
        Log.d(TAG, "New intent received: ${intent.action}")
        return handleIntent(intent)
    }

    //----------------------------------------------------------------------------------------------
    // MethodCallHandler interface implementation
    //----------------------------------------------------------------------------------------------

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d(TAG, "Method call received: ${call.method}")

        when (call.method) {
            "isSupported" -> {
                val isSupported = isShortcutSupported()
                Log.d(TAG, "Shortcuts supported: $isSupported")
                result.success(isSupported)
            }

            "createPinnedShortcut" -> {
                val id = call.argument<String>("id") ?: ""
                val label = call.argument<String>("label") ?: ""
                val longLabel = call.argument<String>("longLabel") ?: label
                val imageSource = call.argument<String>("imageSource") ?: ""
                val imageSourceType = call.argument<String>("imageSourceType") ?: "asset"
                val extraDataString = call.argument<String>("extraData")

                Log.d(TAG, "Creating shortcut: $id, type: $imageSourceType")

                scope.launch {
                    val success = createShortcut(
                            id, label, longLabel, imageSource, imageSourceType, extraDataString
                    )
                    result.success(success)
                }
            }

            "isPinned" -> {
                val id = call.argument<String>("id") ?: ""
                Log.d(TAG, "Checking if shortcut is pinned: $id")

                val isPinned = isShortcutPinned(id)
                result.success(isPinned)
            }

            else -> {
                Log.w(TAG, "Method not implemented: ${call.method}")
                result.notImplemented()
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Private methods
    //----------------------------------------------------------------------------------------------

    /**
     * Handle the intent that may have been triggered by a shortcut click
     */
    private fun handleIntent(intent: Intent?): Boolean {
        if (intent?.action == SHORTCUT_CLICKED_ACTION || intent?.action == Intent.ACTION_MAIN) {
            val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID)
            if (shortcutId != null) {
                // Send the shortcut click event to Flutter
                try {
                    Log.d(TAG, "Shortcut clicked: $shortcutId")
                    val returnMap = HashMap<String, String?>()
                    returnMap[EXTRA_SHORTCUT_ID] = intent.getStringExtra(EXTRA_SHORTCUT_ID)
                    returnMap[EXTRA_SHORTCUT_DATA] = intent.getStringExtra(EXTRA_SHORTCUT_DATA)
                    channel.invokeMethod("onShortcutClick", returnMap)
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error invoking method: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        return false
    }

    /**
     * Check if pinned shortcuts are supported on the device
     */
    private fun isShortcutSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.isRequestPinShortcutSupported ?: false
        } else {
            ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        }
    }

    /**
     * Create a new pinned shortcut
     */
    private suspend fun createShortcut(
            id: String,
            label: String,
            longLabel: String,
            imageSource: String,
            imageSourceType: String,
            extraDataString: String?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating shortcut: $id")

                if (!isShortcutSupported()) {
                    Log.e(TAG, "Shortcuts not supported on this device")
                    return@withContext false
                }

                // Create the intent that will be launched when the shortcut is clicked
                val intent = Intent(context, activity?.javaClass)
                intent.action = SHORTCUT_CLICKED_ACTION
                intent.putExtra(EXTRA_SHORTCUT_ID, id)

                if (extraDataString != null) {
                    intent.putExtra(EXTRA_SHORTCUT_DATA, extraDataString)
                }

                // Don't use CLEAR_TASK flag as it will destroy the activity and may cause issues with the event
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

                // Load the icon based on the source type
                val icon = loadIconFromSource(imageSource, imageSourceType)
                        ?: return@withContext false

                // Create the shortcut
                val shortcutInfo = ShortcutInfoCompat.Builder(context, id)
                        .setShortLabel(label)
                        .setLongLabel(longLabel)
                        .setIcon(icon)
                        .setIntent(intent)
                        .build()

                // Save shortcut information for future reference
                val settings = FlutterPinnedShortcutsSettings(context)
                settings.saveShortcut(id, label, longLabel, extraDataString)

                // For API 26+, also add it as a dynamic shortcut first
                // This helps with tracking and updates
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

                        // Convert IconCompat to Icon
                        val drawable = icon.toIcon(context).loadDrawable(context)
                        val bitmap = Bitmap.createBitmap(
                                drawable!!.intrinsicWidth,
                                drawable.intrinsicHeight,
                                Bitmap.Config.ARGB_8888
                        )

                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)

                        val shortcutInfoApi26 = ShortcutInfo.Builder(context, id)
                                .setShortLabel(label)
                                .setLongLabel(longLabel)
                                .setIcon(Icon.createWithBitmap(bitmap))
                                .setIntent(intent)
                                .build()

                        shortcutManager?.addDynamicShortcuts(listOf(shortcutInfoApi26))
                        Log.d(TAG, "Added dynamic shortcut: $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding dynamic shortcut: ${e.message}")
                        // Continue with pinned shortcut even if dynamic fails
                    }
                }

                // Request to pin the shortcut
                val success = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
                Log.d(TAG, "Shortcut creation requested: $id, result: $success")

                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "Error creating shortcut: ${e.message}")
                e.printStackTrace()
                return@withContext false
            }
        }
    }


    /**
     * Check if a shortcut is pinned
     */
    private fun isShortcutPinned(id: String): Boolean {
        Log.d(TAG, "Checking if shortcut is pinned: $id")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)

                // Check if it's in the pinned shortcuts
                val isPinned = shortcutManager?.pinnedShortcuts?.any { it.id == id } ?: false

                // If we couldn't find it in the pinned shortcuts, check our settings
                if (!isPinned) {
                    val settings = FlutterPinnedShortcutsSettings(context)
                    return settings.hasShortcut(id)
                }

                return isPinned
            } else {
                // For older Android versions, we can only check our own settings
                val settings = FlutterPinnedShortcutsSettings(context)
                return settings.hasShortcut(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if shortcut is pinned: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Load an icon from various sources (asset, resource, file, etc.)
     */
    private suspend fun loadIconFromSource(
            source: String,
            sourceType: String
    ): IconCompat? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading icon from $sourceType: $source")

                when (sourceType.lowercase()) {
                    "asset" -> {
                        // For Flutter assets, we need to use the asset manager
                        val assetManager = context.assets
                        val inputStream = assetManager.open("flutter_assets/$source")
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()

                        if (bitmap != null) {
                            Log.d(TAG, "Asset icon loaded successfully: $source")
                            IconCompat.createWithBitmap(bitmap)
                        } else {
                            Log.e(TAG, "Failed to decode asset bitmap: $source")
                            null
                        }
                    }

                    "resource" -> {
                        // For Android resources
                        val resourceId = context.resources.getIdentifier(
                                source, "drawable", context.packageName
                        )

                        if (resourceId != 0) {
                            Log.d(TAG, "Resource icon loaded successfully: $source (ID: $resourceId)")
                            IconCompat.createWithResource(context, resourceId)
                        } else {
                            Log.e(TAG, "Resource not found: $source")
                            null
                        }
                    }

                    "network", "file" -> {
                        // For file paths (including downloaded network images)
                        val file = File(source)
                        if (file.exists()) {
                            val inputStream = FileInputStream(file)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()

                            if (bitmap != null) {
                                Log.d(TAG, "File icon loaded successfully: $source")
                                IconCompat.createWithBitmap(bitmap)
                            } else {
                                Log.e(TAG, "Failed to decode file bitmap: $source")
                                null
                            }
                        } else {
                            Log.e(TAG, "File does not exist: $source")
                            null
                        }
                    }

                    else -> {
                        Log.e(TAG, "Unsupported icon source type: $sourceType")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading icon: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}