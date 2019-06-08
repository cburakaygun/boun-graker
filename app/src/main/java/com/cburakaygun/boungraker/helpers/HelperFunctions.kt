package com.cburakaygun.boungraker.helpers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.*
import com.cburakaygun.boungraker.MainActivity
import com.cburakaygun.boungraker.R
import com.cburakaygun.boungraker.workers.TermInfoWorker
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


fun issueNotification(appContext: Context, channelID: String, notifID: Int, title: String, text: String) {
    val pIndent: PendingIntent =
        PendingIntent.getActivity(appContext, 0, Intent(appContext, MainActivity::class.java), 0)

    val builder = NotificationCompat.Builder(appContext, channelID).apply {
        setSmallIcon(R.drawable.ic_launcher_background)
        setContentIntent(pIndent)
        setContentTitle(title)
        setContentText(text)
        setStyle(NotificationCompat.BigTextStyle().bigText(text))
        setAutoCancel(true)
        setDefaults(Notification.DEFAULT_ALL)
        setOnlyAlertOnce(true)
        priority = NotificationCompat.PRIORITY_DEFAULT
        setShowWhen(true)
        setWhen(Calendar.getInstance().timeInMillis)
    }

    NotificationManagerCompat.from(appContext).notify(notifID, builder.build())
}


fun createTermInfoWorkRequest(userDataSharPref: SharedPreferences, term: String, periodic: Boolean): WorkRequest {
    val stuID = userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_ID_KEY, "")
    val stuPW = userDataSharPref.getString(Constants.SHAR_PREF_USER_DATA_PW_KEY, "")

    val termInfoWorkInputData = Data.Builder().run {
        putString(Constants.WORKER_TERM_INFO_ID_KEY, stuID)
        putString(Constants.WORKER_TERM_INFO_PW_KEY, stuPW)
        putString(Constants.WORKER_TERM_INFO_TERM_KEY, term)
        putBoolean(Constants.WORKER_TERM_INFO_PERIODIC_KEY, periodic)
        build()
    }

    val workRequestBuilder =
        if (periodic) {
            PeriodicWorkRequestBuilder<TermInfoWorker>(20, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        } else {
            OneTimeWorkRequestBuilder<TermInfoWorker>()
        }

    return workRequestBuilder.setInputData(termInfoWorkInputData).build()
}


fun schedulePeriodicWorker(userDataSharPref: SharedPreferences?, termsDataSharPref: SharedPreferences?) {
    if (userDataSharPref == null || termsDataSharPref == null) return

    val lastTerm = termsDataSharPref.all.keys.sortedDescending().first()
    val termInfoWorkRequest = createTermInfoWorkRequest(userDataSharPref, lastTerm, true)

    WorkManager.getInstance().enqueueUniquePeriodicWork(
        Constants.WORKER_TERM_INFO_PERIODIC_UNIQUE_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        termInfoWorkRequest as PeriodicWorkRequest
    )
}


fun cancelPeriodicWorker() = WorkManager.getInstance().cancelUniqueWork(Constants.WORKER_TERM_INFO_PERIODIC_UNIQUE_NAME)


/**
 * Returns true if device is connected to a network.
 * Otherwise, returns false.
 */
fun isNetworkConnected(appContext: Context?): Boolean {
    val activeNetworkInfo: NetworkInfo? =
        (appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo

    return (activeNetworkInfo != null && activeNetworkInfo.isConnected)
}


/**
 * Returns true if background SYNC service is enabled in Settings.
 * Otherwise, returns false.
 */
fun isSyncEnabled(appContext: Context?): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(appContext)
        .getBoolean(appContext?.getString(R.string.SETTINGS_SYNC_SWITCH_KEY), false)
}


/**
 * Logs in to BUIS and returns a HashMap of cookies to be used to browse other web pages.
 */
fun bounLogin(stuID: String, stuPW: String): HashMap<String, String>? {
    try {
        // Connects to the login page to get some required POST parameters
        var httpResponse: Connection.Response = Jsoup.connect(Constants.BOUN_LOGIN_URL)
            .method(Connection.Method.GET)
            .ignoreHttpErrors(true)
            .followRedirects(true)
            .execute()

        if (httpResponse.statusCode() != 200) return null
        val cookies = httpResponse.cookies() as HashMap

        val doc = httpResponse.parse()

        val __VIEWSTATE = doc.getElementById("__VIEWSTATE").`val`()
        val __VIEWSTATEGENERATOR = doc.getElementById("__VIEWSTATEGENERATOR").`val`()
        val __EVENTVALIDATION = doc.getElementById("__EVENTVALIDATION").`val`()


        // Makes a login request to check if provided credentials are correct or not
        httpResponse = Jsoup.connect(Constants.BOUN_LOGIN_URL)
            .data("__VIEWSTATE", __VIEWSTATE, "__VIEWSTATEGENERATOR", __VIEWSTATEGENERATOR,
                "__EVENTVALIDATION", __EVENTVALIDATION, "btnLogin", "Login",
                "txtUsername", stuID, "txtPassword", stuPW)
            .method(Connection.Method.POST)
            .ignoreHttpErrors(true)
            .followRedirects(false)  // Successful request is responded with 302
            .cookies(cookies)
            .execute()

        if (httpResponse.statusCode() != 302) return null
        for ((k, v) in (httpResponse.cookies() as HashMap)) cookies[k] = v

        // Gets cookie ASPSESSIONIDSQCQQBTC
        httpResponse = Jsoup.connect("https://registration.boun.edu.tr/scripts/buis_obikas_entrance.asp?ono=$stuID&sno=&obno=&BKKA=$stuID&dno=&clp=0")
            .method(Connection.Method.GET)
            .ignoreHttpErrors(true)
            .followRedirects(true)
            .cookies(cookies)
            .execute()

        if (httpResponse.statusCode() != 200) return null
        for ((k, v) in (httpResponse.cookies() as HashMap)) cookies[k] = v

        cookies["sId"] = stuID

        return cookies

    } catch (e: IOException) {
        // Ignore
    }

    return null
}


/**
 * Returns HTTP response of BUIS grades web page.
 */
private fun bounGetGradesPage(loginCookies: Map<String, String>?): Connection.Response {
    return Jsoup.connect(Constants.BOUN_GRADES_URL)
        .method(Connection.Method.GET)
        .ignoreHttpErrors(true)
        .followRedirects(true)
        .cookies(loginCookies)
        .execute()
}


/**
 * Returns a List of Strings where each element is a term, e.g. 2018/2019-2
 */
fun bounTermsInfo(loginCookies: Map<String, String>?): List<String>? {
    try {
        val httpResponse = bounGetGradesPage(loginCookies)

        if (httpResponse.statusCode() == 200) {
            val termOptions = httpResponse.parse().select("select.dropdown > option")

            if (termOptions != null && termOptions.size > 1) {
                return termOptions.subList(1, termOptions.size).map { termOption -> termOption.`val`() }
            }
        }

    } catch (e: IOException) {
        // Ignore
    }

    return null
}


/**
 * Returns a HashMap which maps courses (and SPA & GPA) to the corresponding grades for given `term`
 */
fun bounTermInfo(loginCookies: Map<String, String>?, term: String): HashMap<String, String>? {
    try {
        var httpResponse = bounGetGradesPage(loginCookies)
        if (httpResponse.statusCode() != 200) return null

        val doc = httpResponse.parse()

        val __EVENTTARGET = doc.getElementById("__EVENTTARGET").`val`()
        val __EVENTARGUMENT = doc.getElementById("__EVENTARGUMENT").`val`()
        val __LASTFOCUS = doc.getElementById("__LASTFOCUS").`val`()
        val __VIEWSTATE = doc.getElementById("__VIEWSTATE").`val`()
        val __VIEWSTATEGENERATOR = doc.getElementById("__VIEWSTATEGENERATOR").`val`()
        val __EVENTVALIDATION = doc.getElementById("__EVENTVALIDATION").`val`()

        httpResponse = Jsoup.connect(Constants.BOUN_GRADES_URL)
            .data("__EVENTTARGET", __EVENTTARGET, "__EVENTARGUMENT", __EVENTARGUMENT, "__LASTFOCUS", __LASTFOCUS,
                "__VIEWSTATE", __VIEWSTATE, "__VIEWSTATEGENERATOR", __VIEWSTATEGENERATOR, "__EVENTVALIDATION", __EVENTVALIDATION,
                "ctl00\$ctl00\$cphMainContent\$cphMainContent\$ddlTerms", term)
            .method(Connection.Method.POST)
            .ignoreHttpErrors(true)
            .followRedirects(true)
            .cookies(loginCookies)
            .execute()

        if (httpResponse.statusCode() != 200) return null

        val tables = httpResponse.parse().select("table > tbody")

        val gradesTableTrs = tables[0].select("tr")

        val result = HashMap<String, String>()

        for (gradesTableTr in gradesTableTrs) {
            val gradesTableTrTds = gradesTableTr.select("td")
            val course = gradesTableTrTds[0].ownText().split(".")[0].replace("\\s+".toRegex(), "")
            var grade = gradesTableTrTds[3].ownText()
            if (grade.isNullOrEmpty()) grade = "??"
            result[course] = grade
        }

        if (tables.size > 1) {
            val xpaTableTrs = tables[1].select("tr")

            result["SPA"] = xpaTableTrs[0].select("td")[2].ownText().split(":")[1].trim()
            result["GPA"] = xpaTableTrs[1].select("td")[2].ownText().split(":")[1].trim()
        } else {
            result["SPA"] = "--"
            result["GPA"] = "--"
        }

        return result

    } catch (e: IOException) {
        // Ignore
    } catch (e: IndexOutOfBoundsException) {
        // Ignore
    }

    return null
}
