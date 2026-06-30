package com.omarchy.launcher.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle

/**
 * A launcher needs to ask the system "let the user pick a widget", then
 * (if that widget needs configuration) launch its config activity, and
 * only then report success back to whoever asked for a widget. That's
 * three Activity-result round-trips chained together -- isolating all
 * of it in its own transparent Activity keeps HomeActivity's
 * onActivityResult from turning into a tangle of widget-specific state.
 */
class WidgetPickerActivity : Activity() {

    private lateinit var widgetHostManager: WidgetHostManager
    private var pendingAppWidgetId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHostManager = WidgetHostManager(this)
        widgetHostManager.startListening()
        startPickFlow()
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHostManager.stopListening()
    }

    private fun startPickFlow() {
        val appWidgetId = widgetHostManager.allocateAppWidgetId()
        pendingAppWidgetId = appWidgetId

        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putParcelableArrayListExtra(
                AppWidgetManager.EXTRA_CUSTOM_INFO,
                arrayListOf()
            )
        }
        startActivityForResult(pickIntent, REQUEST_PICK_WIDGET)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_PICK_WIDGET -> {
                if (resultCode != RESULT_OK) {
                    cleanupAndFinish(RESULT_CANCELED)
                    return
                }
                val appWidgetId = data?.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId
                ) ?: pendingAppWidgetId

                val providerInfo = widgetHostManager.getProviderInfo(appWidgetId)
                if (providerInfo?.configure != null) {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = providerInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    try {
                        startActivityForResult(configIntent, REQUEST_CONFIGURE_WIDGET)
                    } catch (e: Exception) {
                        showPreviewAndConfirm(appWidgetId, providerInfo)
                    }
                } else if (providerInfo != null) {
                    showPreviewAndConfirm(appWidgetId, providerInfo)
                } else {
                    cleanupAndFinish(RESULT_CANCELED)
                }
            }
            REQUEST_CONFIGURE_WIDGET -> {
                if (resultCode == RESULT_OK) {
                    val providerInfo = widgetHostManager.getProviderInfo(pendingAppWidgetId)
                    if (providerInfo != null) {
                        showPreviewAndConfirm(pendingAppWidgetId, providerInfo)
                    } else {
                        finishWithWidget(pendingAppWidgetId)
                    }
                } else {
                    widgetHostManager.deleteAppWidgetId(pendingAppWidgetId)
                    cleanupAndFinish(RESULT_CANCELED)
                }
            }
        }
    }

    /**
     * Renders the actual widget via AppWidgetHostView -- the same view
     * type the home screen will use -- in a small confirm screen, so
     * the user sees exactly how it will look before it's added, rather
     * than guessing from just the widget's name/icon in the system
     * picker list.
     */
    private fun showPreviewAndConfirm(appWidgetId: Int, providerInfo: android.appwidget.AppWidgetProviderInfo) {
        setContentView(com.omarchy.launcher.R.layout.activity_widget_preview)

        val hostView = try {
            widgetHostManager.createHostView(appWidgetId, providerInfo)
        } catch (e: Exception) {
            finishWithWidget(appWidgetId)
            return
        }

        val container = findViewById<android.widget.FrameLayout>(com.omarchy.launcher.R.id.widgetPreviewContainer)
        val density = resources.displayMetrics.density
        val minWidthPx = (providerInfo.minWidth * density).toInt().coerceAtLeast(150)
        val minHeightPx = (providerInfo.minHeight * density).toInt().coerceAtLeast(150)
        hostView.layoutParams = android.widget.FrameLayout.LayoutParams(minWidthPx, minHeightPx)
        container.addView(hostView)

        findViewById<android.widget.TextView>(com.omarchy.launcher.R.id.widgetPreviewLabel).text =
            providerInfo.loadLabel(packageManager)

        findViewById<android.widget.TextView>(com.omarchy.launcher.R.id.widgetPreviewConfirm).setOnClickListener {
            finishWithWidget(appWidgetId)
        }
        findViewById<android.widget.TextView>(com.omarchy.launcher.R.id.widgetPreviewCancel).setOnClickListener {
            widgetHostManager.deleteAppWidgetId(appWidgetId)
            cleanupAndFinish(RESULT_CANCELED)
        }
    }

    private fun finishWithWidget(appWidgetId: Int) {
        val result = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun cleanupAndFinish(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    companion object {
        const val REQUEST_PICK_WIDGET = 9001
        const val REQUEST_CONFIGURE_WIDGET = 9002
    }
}
