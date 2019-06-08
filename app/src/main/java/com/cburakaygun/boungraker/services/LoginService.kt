package com.cburakaygun.boungraker.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.bounLogin
import com.cburakaygun.boungraker.helpers.bounTermsInfo


/**
 * This service is responsible for logging the user in and retrieving the list of terms from the server.
 * Expects the following input data:
 *   - <Student ID>: String
 *   - <Student Password>: String
 *
 * If successful, user credentials and terms are saved to local (SharedPreferences).
 */
class LoginService : IntentService("LoginService") {

    override fun onHandleIntent(intent: Intent?) {
        val stuID = intent?.extras?.getString(Constants.INTENT_LOGIN_ID_KEY) ?: return
        val stuPW = intent.extras?.getString(Constants.INTENT_LOGIN_PW_KEY) ?: return

        var loginResult = Constants.INTENT_LOGIN_RESULT_VAL_SUCCESS

        val loginCookies: HashMap<String, String>? = bounLogin(stuID, stuPW)

        if (loginCookies == null) {
            loginResult = Constants.INTENT_LOGIN_RESULT_VAL_LOGIN_FAIL

        } else {
            val termsList: List<String>? = bounTermsInfo(loginCookies)

            if (termsList.isNullOrEmpty()) {
                loginResult = Constants.INTENT_LOGIN_RESULT_VAL_TERMS_INFO_FAIL

            } else {
                // Saves student ID and Password to SharedPreferences
                with (applicationContext.getSharedPreferences(Constants.SHAR_PREF_USER_DATA, Context.MODE_PRIVATE).edit()) {
                    putString(Constants.SHAR_PREF_USER_DATA_ID_KEY, stuID)
                    putString(Constants.SHAR_PREF_USER_DATA_PW_KEY, stuPW)
                    apply()
                }

                // Saves each term to SharedPreferences with an empty string
                with (applicationContext.getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE).edit()) {
                    clear()
                    termsList.forEach { putString(it, "") }
                    apply()
                }
            }
        }

        Intent(Constants.INTENT_LOGIN_RESULT).let {
            it.putExtra(Constants.INTENT_LOGIN_RESULT_KEY, loginResult)
            LocalBroadcastManager.getInstance(this).sendBroadcast(it)
        }
    }

}
