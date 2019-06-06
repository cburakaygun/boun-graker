package com.cburakaygun.boungraker

import android.app.IntentService
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.bounLogin
import org.jsoup.Connection
import java.io.IOException


class LoginService : IntentService("LoginService") {

    override fun onHandleIntent(intent: Intent?) {
        val loginStatusIntent = Intent(Constants.INTENT_LOGIN_STATUS)
        var loginSuccessful = false

        try {
            val stuID = intent?.extras?.getString(Constants.INTENT_LOGIN_ID_KEY) ?: return
            val stuPW = intent.extras?.getString(Constants.INTENT_LOGIN_PW_KEY) ?: return

            val loginResponse: Connection.Response = bounLogin(stuID, stuPW)

            if (loginResponse.statusCode() == 302) { // Successful request is responded with 302
                loginSuccessful = true
            }

        } catch (e: IOException) {
            // Ignore
        }

        loginStatusIntent.putExtra(Constants.INTENT_LOGIN_STATUS_KEY, loginSuccessful)
        LocalBroadcastManager.getInstance(this).sendBroadcast(loginStatusIntent)
    }
}
