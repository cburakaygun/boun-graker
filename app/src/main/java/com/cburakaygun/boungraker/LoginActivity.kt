package com.cburakaygun.boungraker

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.isNetworkConnected
import com.cburakaygun.boungraker.services.LoginService


class LoginActivity : AppCompatActivity() {

    lateinit var idEditText: EditText
    lateinit var pwEditText: EditText
    lateinit var loginButton: Button
    lateinit var loginInfoTextView: TextView

    lateinit var userDataSharPref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        idEditText = findViewById(R.id.login_stu_id_edittext)
        pwEditText = findViewById(R.id.login_stu_pw_edittext)
        loginButton = findViewById(R.id.login_button)
        loginInfoTextView = findViewById(R.id.login_info_textview)

        userDataSharPref = getSharedPreferences(Constants.SHAR_PREF_USER_DATA , Context.MODE_PRIVATE)

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(LoginResultReceiver(),
            IntentFilter(Constants.INTENT_LOGIN_RESULT))
    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(LoginResultReceiver())
    }


    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString("LOGIN_INFO_TEXT_SAVED" , loginInfoTextView.text.toString())
        outState?.putString("ID_TEXT_SAVED" , idEditText.text.toString())
        outState?.putString("PW_TEXT_SAVED" , pwEditText.text.toString())
        super.onSaveInstanceState(outState)
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        loginInfoTextView.text = savedInstanceState?.getString("LOGIN_INFO_TEXT_SAVED")
        idEditText.setText(savedInstanceState?.getString("ID_TEXT_SAVED") , TextView.BufferType.EDITABLE)
        pwEditText.setText(savedInstanceState?.getString("PW_TEXT_SAVED") , TextView.BufferType.EDITABLE)
    }


    fun loginButtonOnClick(view: View) {
        val stuID = idEditText.text.toString()
        val stuPW = pwEditText.text.toString()

        if (!isNetworkConnected(this)) {
            loginInfoTextView.text = getString(R.string.LOGIN_INFO_ERR_NO_INTERNET)

        } else if (stuID.isBlank()) {
            loginInfoTextView.text = getString(R.string.LOGIN_INFO_ERR_ID_BLANK)

        } else if (stuPW.isBlank()) {
            loginInfoTextView.text = getString(R.string.LOGIN_INFO_ERR_PW_BLANK)

        } else {
            loginButton.isEnabled = false
            idEditText.isEnabled = false
            pwEditText.isEnabled = false

            loginInfoTextView.text = getString(R.string.LOGIN_INFO_PLEASE_WAIT)

            val intent = Intent(this, LoginService::class.java)
            intent.putExtra(Constants.INTENT_LOGIN_ID_KEY, stuID)
            intent.putExtra(Constants.INTENT_LOGIN_PW_KEY, stuPW)
            startService(intent)
        }
    }


    private inner class LoginResultReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            when(intent.extras?.getInt(Constants.INTENT_LOGIN_RESULT_KEY)) {
                Constants.INTENT_LOGIN_RESULT_VAL_LOGIN_FAIL -> {
                    loginInfoTextView.text = getString(R.string.LOGIN_INFO_LOGIN_FAIL)
                }
                Constants.INTENT_LOGIN_RESULT_VAL_TERMS_INFO_FAIL -> {
                    loginInfoTextView.text = getString(R.string.LOGIN_INFO_TERMS_INFO_FAIL)
                }
                else -> {
                    loginInfoTextView.text = getString(R.string.LOGIN_INFO_SUCCESS)
                    finish()
                }
            }

            loginButton.isEnabled = true
            idEditText.isEnabled = true
            pwEditText.isEnabled = true
        }
    }

}
