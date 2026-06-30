package com.omarchy.launcher.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.omarchy.launcher.LauncherApplication
import com.omarchy.launcher.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsFragmentContainer, LauncherPreferenceFragment())
                .commit()
        }
    }

    /**
     * Backup/restore live here as plain Preference click handlers rather
     * than a dedicated screen -- exporting just hands the user a copyable
     * string (toast + clipboard) since there's no file-picker dependency
     * to wire up for a feature this small.
     */
    class LauncherPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.launcher_preferences, rootKey)

            findPreference<Preference>("pref_backup_layout")?.setOnPreferenceClickListener {
                val app = LauncherApplication.from(requireContext())
                val exported = app.layoutStore.exportLayout()
                copyToClipboard(exported)
                Toast.makeText(requireContext(), "Layout in Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
                true
            }

            findPreference<Preference>("pref_restore_layout")?.setOnPreferenceClickListener {
                val clip = readFromClipboard()
                if (clip == null) {
                    Toast.makeText(requireContext(), "Zwischenablage ist leer", Toast.LENGTH_SHORT).show()
                    return@setOnPreferenceClickListener true
                }
                val app = LauncherApplication.from(requireContext())
                val success = app.layoutStore.importLayout(clip)
                val message = if (success) "Layout wiederhergestellt" else "Ungültiges Layout-Format"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                true
            }
        }

        private fun copyToClipboard(text: String) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("omr_launcher_layout", text))
        }

        private fun readFromClipboard(): String? {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip == null || clip.itemCount == 0) return null
            return clip.getItemAt(0).text?.toString()
        }
    }
}
