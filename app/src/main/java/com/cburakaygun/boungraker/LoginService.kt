package com.cburakaygun.boungraker

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.cburakaygun.boungraker.helpers.bounLogin
import org.jsoup.Connection


class LoginService : IntentService("LoginService") {

    override fun onHandleIntent(intent: Intent?) {
        val loginStatusIntent = Intent(getString(R.string.INTENT_LOGIN_STATUS))
        var loginStatusValue: Int = R.string.INTENT_LOGIN_STATUS_VAL_FAIL

        try {
            val stuID = intent?.extras?.get(getString(R.string.INTENT_LOGIN_ID_KEY)).toString()
            val stuPW = intent?.extras?.get(getString(R.string.INTENT_LOGIN_PW_KEY)).toString()

            val loginResponse: Connection.Response = bounLogin(stuID, stuPW)

            if (loginResponse.statusCode() == 302) { // Successful request is responded with 302
                loginStatusValue = R.string.INTENT_LOGIN_STATUS_VAL_SUCCESS
            }

        } catch (e: Exception) {
            // Ignore
        }

        loginStatusIntent.putExtra(getString(R.string.INTENT_LOGIN_STATUS_KEY), getString(loginStatusValue))
        LocalBroadcastManager.getInstance(this).sendBroadcast(loginStatusIntent)
    }
}
