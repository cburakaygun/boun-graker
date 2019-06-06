package com.cburakaygun.boungraker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import com.cburakaygun.boungraker.helpers.Constants


class MainActivity : AppCompatActivity() {

    lateinit var stuIDTextView: TextView
    lateinit var termsSpinner: Spinner
    lateinit var mainTextView: TextView
    lateinit var workingStatTextView: TextView

    lateinit var userDataSharPref: SharedPreferences

    var loginMenuItem: MenuItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        stuIDTextView = findViewById(R.id.stu_id_textview)
        termsSpinner = findViewById(R.id.terms_spinner)
        mainTextView = findViewById(R.id.main_textview)
        workingStatTextView = findViewById(R.id.working_stat_textview)

        userDataSharPref = getSharedPreferences(Constants.SHAR_PREF_USER_DATA, Context.MODE_PRIVATE)

        if (!userLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }


    override fun onStart() {
        super.onStart()
        loginInfoChanged()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        loginMenuItem = menu?.findItem(R.id.login_menu)
        loginMenuItem?.title = if (userLoggedIn()) getString(R.string.MENU_LOGOUT) else getString(R.string.MENU_LOGIN)
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId){
            R.id.login_menu -> {
                if (userLoggedIn()) {
                    logUserOut()
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun userLoggedIn() =
        !(userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "").isNullOrEmpty())


    fun logUserOut() {
        userDataSharPref.edit().putString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "")
            .putString(Constants.SHAR_PREF_USER_DATA_PW_KEY, "")
            .apply()
        loginInfoChanged()
    }


    fun loginInfoChanged() {
        val stuID = userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "")

        if (stuID.isNullOrEmpty()) {
            stuIDTextView.text = ""
            termsSpinner.visibility = View.INVISIBLE
            mainTextView.text = getString(R.string.MAIN_TEXT_NEED_TO_LOGIN)

        } else {
            stuIDTextView.text = stuID
            termsSpinner.visibility = View.VISIBLE
            mainTextView.text = getString(R.string.MAIN_TEXT_GRADES_WILL_APPEAR)
        }

        invalidateOptionsMenu()
    }

}
