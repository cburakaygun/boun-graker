package com.cburakaygun.boungraker

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.cancelPeriodicWorker
import com.cburakaygun.boungraker.helpers.schedulePeriodicWorker

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<SwitchPreference>(getString(R.string.SETTINGS_SYNC_SWITCH_KEY))?.apply {
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    if (newValue as Boolean) {
                        schedulePeriodicWorker(
                            activity?.getSharedPreferences(Constants.SHAR_PREF_USER_DATA, Context.MODE_PRIVATE),
                            activity?.getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE)
                        )

                    } else {
                        cancelPeriodicWorker()
                    }

                    true
                }
        }
    }

}
