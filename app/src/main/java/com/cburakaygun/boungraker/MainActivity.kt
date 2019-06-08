package com.cburakaygun.boungraker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.cancelPeriodicWorker
import com.cburakaygun.boungraker.helpers.createTermInfoWorkRequest
import com.cburakaygun.boungraker.helpers.isSyncEnabled


class MainActivity : AppCompatActivity() {

    lateinit var stuIDTextView: TextView
    lateinit var termsSpinner: Spinner
    lateinit var mainTextView: TextView
    lateinit var lastCheckTextView: TextView
    lateinit var termUpdateButton: Button
    lateinit var syncStatusTextView: TextView

    lateinit var userDataSharPref: SharedPreferences
    lateinit var termsDataSharPref: SharedPreferences

    val newGradesReceiver: BroadcastReceiver = NewGradesReceiver()

    var termsSpinnerItemSelectedBySystem = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userDataSharPref = getSharedPreferences(Constants.SHAR_PREF_USER_DATA, Context.MODE_PRIVATE)
        termsDataSharPref = getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        if (!isUserLoggedIn()) {
            loadLoginActivity()
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.main_toolbar))
        createNotificationChannel()

        stuIDTextView = findViewById(R.id.stu_id_textview)
        termsSpinner = findViewById(R.id.terms_spinner)
        mainTextView = findViewById(R.id.main_textview)
        lastCheckTextView = findViewById(R.id.last_check_textview)
        termUpdateButton = findViewById(R.id.term_update_button)
        syncStatusTextView = findViewById(R.id.sync_status_textview)

        stuIDTextView.text = userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "")
        setUpTermsSpinner()
    }

    // All the lifecycle methods except `onCreate` and `onDestroy` are meant to be executed only when the user is logged in.

    override fun onStart() {
        super.onStart()
        updateSyncStatusText()
        LocalBroadcastManager.getInstance(this).registerReceiver(newGradesReceiver, IntentFilter(Constants.INTENT_NEW_GRADES))
    }


    override fun onResume() {
        super.onResume()
        displayTermInfo()
    }


    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(newGradesReceiver)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId){
            R.id.menu_logout -> {
                logUserOut()
                true
            }

            R.id.menu_settings -> {
                startActivity(Intent(this , SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun isUserLoggedIn() =
        !(userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "").isNullOrEmpty())


    private fun loadLoginActivity() =
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        })


    private fun logUserOut() {
        userDataSharPref.edit().clear().apply()
        termsDataSharPref.edit().clear().apply()

        NotificationManagerCompat.from(this).cancel(Constants.NOTIFICATION_NEW_GRADES_ID)
        cancelPeriodicWorker()

        loadLoginActivity()
        finish()
    }


    private fun setUpTermsSpinner() {
        termsSpinnerItemSelectedBySystem = true

        termsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (termsSpinnerItemSelectedBySystem) {
                    termsSpinnerItemSelectedBySystem = false
                } else {
                    displayTermInfo()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        termsSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
            termsDataSharPref.all.keys.sortedDescending()).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }


    /**
     * Displays term info stored in SharedPreferences on the UI.
     */
    private fun displayTermInfo() {
        val selectedTerm = termsSpinner.selectedItem ?: return

        val termInfo = termsDataSharPref.getString(selectedTerm.toString(), "")

        // If no info is found on local, retrieves it from the server.
        if (termInfo.isNullOrEmpty()) {
            termUpdateButtonOnClick(termUpdateButton)
            return
        }

        var gradesText = ""
        lateinit var xpaInfo: String

        for (courseGradeInfo in termInfo.split(Constants.COURSE_GRADE_PAIR_DELIMITER)) {
            val courseGradeList = courseGradeInfo.split(Constants.COURSE_GRADE_DELIMITER)
            val course = courseGradeList[0]
            val grade = courseGradeList[1]

            when (course) {
                "XPA" -> {
                    val spaGPA = grade.split(Constants.XPA_DELIMITER)
                    xpaInfo = "SPA: ${spaGPA[0]} | GPA: ${spaGPA[1]}"
                }

                "LAST_CHECK" -> { lastCheckTextView.text = getString(R.string.MAIN_LAST_CHECK_TEXT).format(grade) }

                else -> { gradesText += "${course.padStart(8,' ')}: $grade\n" }
            }
        }

        mainTextView.text = getString(R.string.MAIN_TEXT_GRADES).format(gradesText, xpaInfo)
    }


    fun termUpdateButtonOnClick(view: View) {
        mainTextView.text = getString(R.string.MAIN_TEXT_GRADES_WILL_APPEAR)
        lastCheckTextView.text = ""
        termUpdateButton.text = getString(R.string.MAIN_TERM_UPDATE_BUTTON_TEXT_UPDATING)
        termUpdateButton.isEnabled = false
        termsSpinner.isEnabled = false

        val selectedTerm: String = termsSpinner.selectedItem.toString()

        val termInfoWorkRequest = createTermInfoWorkRequest(userDataSharPref, selectedTerm, false)

        WorkManager.getInstance().getWorkInfoByIdLiveData(termInfoWorkRequest.id).observe(this,
            Observer { workInfo ->
                if (workInfo != null &&
                    (workInfo.state == WorkInfo.State.SUCCEEDED || workInfo.state == WorkInfo.State.FAILED)) {
                    termUpdateButton.text = getString(R.string.MAIN_TERM_UPDATE_BUTTON_TEXT_UPDATE)
                    termUpdateButton.isEnabled = true
                    termsSpinner.isEnabled = true
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> displayTermInfo()
                        WorkInfo.State.FAILED -> mainTextView.text = getString(R.string.MAIN_TEXT_TERM_INFO_FAIL)
                        else -> {}
                    }
                }
            })

        WorkManager.getInstance().enqueue(termInfoWorkRequest)

        Toast.makeText(this, "Retrieving term info...", Toast.LENGTH_SHORT).show()
    }


    private fun updateSyncStatusText() {
        if (isSyncEnabled(this)) {
            syncStatusTextView.apply {
                text = getString(R.string.MAIN_SYNC_STATUS_TEXT).format("enabled")
                setTextColor(Color.parseColor("#00AA00"))
            }
        } else {
            syncStatusTextView.apply {
                text = getString(R.string.MAIN_SYNC_STATUS_TEXT).format("disabled")
                setTextColor(Color.parseColor("#AA0000"))
            }
        }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_NEW_GRADES_ID, getString(R.string.NOTIFICATION_CHANNEL_NEW_GRADES_NAME), importance
            ).apply {
                description = getString(R.string.NOTIFICATION_CHANNEL_NEW_GRADES_DESC)
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            // Register the channel with the system
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }


    /**
     * This BroadcastReceiver is executed if the periodic worker discovers new grades while the UI is active.
     * UI gets updated with new grades.
     */
    private inner class NewGradesReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            displayTermInfo()
        }
    }

}
