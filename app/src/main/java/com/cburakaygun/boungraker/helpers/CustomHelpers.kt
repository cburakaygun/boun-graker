package com.cburakaygun.boungraker.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import org.jsoup.Connection
import org.jsoup.Jsoup


object Constants {
    const val APP_PACKAGE_NAME = "com.cburakaygun.boungraker"

    //BOUN REGISTRATION LINKS
    const val BOUN_LOGIN_URL = "https://registration.boun.edu.tr/buis/Login.aspx"
    const val BOUN_ACADEMIC_RECORDS_URL = "https://registration.boun.edu.tr/scripts/stuinfar.asp"

    // SHARED PREFERENCES
    const val SHAR_PREF_USER_DATA = "$APP_PACKAGE_NAME.USER_DATA"
    const val SHAR_PREF_USER_DATA_ID_KEY = "STU_ID"
    const val SHAR_PREF_USER_DATA_PW_KEY = "STU_PW"

    // INTENTS
    const val INTENT_LOGIN_ID_KEY = SHAR_PREF_USER_DATA_ID_KEY
    const val INTENT_LOGIN_PW_KEY = SHAR_PREF_USER_DATA_PW_KEY

    const val INTENT_LOGIN_STATUS = "$APP_PACKAGE_NAME.INTENT_LOGIN_STATUS"
    const val INTENT_LOGIN_STATUS_KEY = "INTENT_LOGIN_STATUS_KEY"
}


fun bounLogin(stuID: String, stuPW: String): Connection.Response {
    // Connects to the login page to get some required POST parameters
    val loginResponse: Connection.Response = Jsoup.connect(Constants.BOUN_LOGIN_URL)
        .method(Connection.Method.GET)
        .ignoreHttpErrors(true)
        .followRedirects(true)
        .execute()

    val doc = Jsoup.parse(loginResponse.body())

    val __VIEWSTATE = doc.getElementById("__VIEWSTATE").`val`()
    val __VIEWSTATEGENERATOR = doc.getElementById("__VIEWSTATEGENERATOR").`val`()
    val __EVENTVALIDATION = doc.getElementById("__EVENTVALIDATION").`val`()


    // Makes a login request to check if provided credentials are correct or not
    return Jsoup.connect(Constants.BOUN_LOGIN_URL)
        .data("__VIEWSTATE", __VIEWSTATE, "__VIEWSTATEGENERATOR", __VIEWSTATEGENERATOR,
            "__EVENTVALIDATION", __EVENTVALIDATION, "btnLogin", "Login",
            "txtUsername", stuID, "txtPassword", stuPW)
        .method(Connection.Method.POST)
        .ignoreHttpErrors(true)
        .followRedirects(false)  // Successful request is responded with 302
        .execute()
}


fun isNetworkConnected(context: Context): Boolean {
    val activeNetworkInfo: NetworkInfo? =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo

    return (activeNetworkInfo != null && activeNetworkInfo.isConnected)
}
