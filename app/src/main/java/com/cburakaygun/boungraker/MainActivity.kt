package com.cburakaygun.boungraker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.workers.TermInfoWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    lateinit var stuIDTextView: TextView
    lateinit var termsSpinner: Spinner
    lateinit var mainTextView: TextView
    lateinit var lastCheckTextView: TextView
    lateinit var termUpdateButton: Button

    lateinit var userDataSharPref: SharedPreferences
    lateinit var termsDataSharPref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        createNotificationChannel()

        stuIDTextView = findViewById(R.id.stu_id_textview)
        termsSpinner = findViewById(R.id.terms_spinner)
        mainTextView = findViewById(R.id.main_textview)
        lastCheckTextView = findViewById(R.id.last_check_textview)
        termUpdateButton = findViewById(R.id.term_update_button)

        userDataSharPref = getSharedPreferences(Constants.SHAR_PREF_USER_DATA, Context.MODE_PRIVATE)
        termsDataSharPref = getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE)

        if (!isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        termsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                displayTermInfo()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).
            registerReceiver(NewGradesReceiver(), IntentFilter(Constants.INTENT_NEW_GRADES))

        if (isUserLoggedIn()) {
            val termInfoWorkRequest = createTermInfoWorkRequest(getLastTerm(), true)

            WorkManager.getInstance().enqueueUniquePeriodicWork(
                Constants.WORKER_TERM_INFO_PERIODIC_UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                termInfoWorkRequest as PeriodicWorkRequest
            )
        }
    }


    override fun onResume() {
        super.onResume()
        updateUI()
    }


    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(NewGradesReceiver())
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.login_menu)?.title = if (isUserLoggedIn()) getString(R.string.MENU_LOGOUT) else getString(R.string.MENU_LOGIN)
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId){
            R.id.login_menu -> {
                if (isUserLoggedIn()) logUserOut() else startActivity(Intent(this, LoginActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun isUserLoggedIn() =
        !(userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "").isNullOrEmpty())


    private fun logUserOut() {
        userDataSharPref.edit().clear().apply()
        termsDataSharPref.edit().clear().apply()

        NotificationManagerCompat.from(this).cancel(Constants.NOTIFICATION_NEW_GRADES_ID)
        WorkManager.getInstance().cancelUniqueWork(Constants.WORKER_TERM_INFO_PERIODIC_UNIQUE_NAME)

        updateUI()
    }


    /**
     * Updates UI according to the login status of the user.
     */
    private fun updateUI() {
        if (isUserLoggedIn()) {
            stuIDTextView.text = userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "")
            termUpdateButton.visibility = View.VISIBLE

            if (termsSpinner.adapter == null) {
                termsSpinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                    termsDataSharPref.all.keys.sortedDescending()).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
            termsSpinner.visibility = View.VISIBLE

        } else {
            stuIDTextView.text = ""
            mainTextView.text = getString(R.string.MAIN_TEXT_NEED_TO_LOGIN)
            lastCheckTextView.text = ""
            termUpdateButton.visibility = View.INVISIBLE

            termsSpinner.adapter = null
            termsSpinner.visibility = View.INVISIBLE
        }

        invalidateOptionsMenu()
    }


    /**
     * Returns the most recent term.
     */
    private fun getLastTerm(): String {
        return termsDataSharPref.all.keys.sortedDescending().first()
    }


    /**
     * Displays term info stored in Shared Preferences on the UI.
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

        val termInfoWorkRequest = createTermInfoWorkRequest(selectedTerm, false)

        WorkManager.getInstance().getWorkInfoByIdLiveData(termInfoWorkRequest.id).observe(this,
            Observer { workInfo ->
                if (workInfo != null && (workInfo.state == WorkInfo.State.SUCCEEDED || workInfo.state == WorkInfo.State.FAILED)) {
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


    private fun createTermInfoWorkRequest(term: String, periodic: Boolean): WorkRequest {
        val termInfoWorkInputData = Data.Builder().run {
            putString(Constants.WORKER_TERM_INFO_ID_KEY, userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, ""))
            putString(Constants.WORKER_TERM_INFO_PW_KEY, userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_PW_KEY, ""))
            putString(Constants.WORKER_TERM_INFO_TERM_KEY, term)
            putBoolean(Constants.WORKER_TERM_INFO_PERIODIC_KEY, periodic)
            build()
        }

        val workRequestBuilder =
            if (periodic) {
                PeriodicWorkRequestBuilder<TermInfoWorker>(20, TimeUnit.MINUTES).
                    setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            } else {
                OneTimeWorkRequestBuilder<TermInfoWorker>()
            }

        return workRequestBuilder.setInputData(termInfoWorkInputData).build()
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
            if ((termsSpinner.selectedItem as String) == getLastTerm()) {
                displayTermInfo()
            }
        }
    }

}
