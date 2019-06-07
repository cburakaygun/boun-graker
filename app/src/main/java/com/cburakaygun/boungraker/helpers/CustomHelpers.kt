package com.cburakaygun.boungraker.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException


object Constants {
    const val APP_PACKAGE_NAME = "com.cburakaygun.boungraker"

    // BOUN REGISTRATION LINKS
    const val BOUN_LOGIN_URL = "https://registration.boun.edu.tr/buis/Login.aspx"
    const val BOUN_GRADES_URL = "https://registration.boun.edu.tr/buis/students/academicyeargrades.aspx"

    // SHARED PREFERENCES
    const val SHAR_PREF_USER_DATA = "$APP_PACKAGE_NAME.USER_DATA"
    const val SHAR_PREF_USER_DATA_ID_KEY = "STU_ID"
    const val SHAR_PREF_USER_DATA_PW_KEY = "STU_PW"

    const val SHAR_PREF_TERMS_DATA = "$APP_PACKAGE_NAME.TERMS_DATA"

    const val COURSE_GRADE_DELIMITER = "!" // <course>!<grade>
    const val COURSE_GRADE_PAIR_DELIMITER = "$" // <course1>!<grade1>$<course2>!<grade2>$...

    // WORKERS
    const val WORKER_TERM_INFO_ID_KEY = SHAR_PREF_USER_DATA_ID_KEY
    const val WORKER_TERM_INFO_PW_KEY = SHAR_PREF_USER_DATA_PW_KEY
    const val WORKER_TERM_INFO_TERM_KEY = "TERM"

    // INTENTS
    const val INTENT_LOGIN_ID_KEY = SHAR_PREF_USER_DATA_ID_KEY
    const val INTENT_LOGIN_PW_KEY = SHAR_PREF_USER_DATA_PW_KEY

    const val INTENT_LOGIN_RESULT = "$APP_PACKAGE_NAME.INTENT_LOGIN_RESULT"
    const val INTENT_LOGIN_RESULT_KEY = "INTENT_LOGIN_RESULT_KEY"
    const val INTENT_LOGIN_RESULT_VAL_SUCCESS = 0
    const val INTENT_LOGIN_RESULT_VAL_LOGIN_FAIL = 1
    const val INTENT_LOGIN_RESULT_VAL_TERMS_INFO_FAIL = 2
}


fun isNetworkConnected(context: Context): Boolean {
    val activeNetworkInfo: NetworkInfo? =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo

    return (activeNetworkInfo != null && activeNetworkInfo.isConnected)
}


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


private fun bounGetGradesPage(loginCookies: Map<String, String>?): Connection.Response {
    return Jsoup.connect(Constants.BOUN_GRADES_URL)
        .method(Connection.Method.GET)
        .ignoreHttpErrors(true)
        .followRedirects(true)
        .cookies(loginCookies)
        .execute()
}


// Returns a List of Strings where each element is a term, e.g. 2018/2019-2
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


// Returns a HashMap which maps courses (and SPA & GPA) to the corresponding grades for given `term`
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
            val grade = gradesTableTrTds[3].ownText()
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
