package com.cburakaygun.boungraker.workers

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cburakaygun.boungraker.R
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.bounLogin
import com.cburakaygun.boungraker.helpers.bounTermInfo
import com.cburakaygun.boungraker.helpers.issueNotification
import java.text.SimpleDateFormat
import java.util.*


/**
 * This worker is responsible for retrieving the information (course grades, SPA and GPA) of a given term.
 * Expects the following input data:
 *   - <Student ID>: String
 *   - <Student Password>: String
 *   - <Term>: String (e.g., '2018/2019-1')
 *   - <Periodic>: Boolean
 *
 * There are two versions of this Worker: Periodic and OneTime
 *   - OneTime version retrieves term information from the server and saves the result to local (SharedPreferences)
 *   - Periodic version does the same job as OneTime version (at about every 20min). Additionally, it issues a
 *     Notification if new grades are discovered for given term.
 */
class TermInfoWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val stuID: String = inputData.getString(Constants.WORKER_TERM_INFO_ID_KEY) ?: return Result.failure()
        val stuPW: String = inputData.getString(Constants.WORKER_TERM_INFO_PW_KEY) ?: return Result.failure()
        val term: String = inputData.getString(Constants.WORKER_TERM_INFO_TERM_KEY) ?: return Result.failure()
        val periodic: Boolean = inputData.getBoolean(Constants.WORKER_TERM_INFO_PERIODIC_KEY, false)

        val oldTermInfo = applicationContext.getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE).
            getString(term, "")

        // Periodic version is meant to run after initial term information is retrieved
        if (periodic && oldTermInfo.isNullOrEmpty()) return Result.success()


        var termInfoResultSuccessful = false

        val loginCookies: HashMap<String, String>? = bounLogin(stuID, stuPW)

        if (loginCookies != null) {

            val termInfoMap: HashMap<String, String>? = bounTermInfo(loginCookies, term)

            if (termInfoMap != null) {
                val xpa = "${termInfoMap.remove("SPA")}${Constants.XPA_DELIMITER}${termInfoMap.remove("GPA")}"

                var termInfo = termInfoMap.keys.sorted().map { termCourse ->
                    "$termCourse${Constants.COURSE_GRADE_DELIMITER}${termInfoMap[termCourse]}"
                }.joinToString(separator = Constants.COURSE_GRADE_PAIR_DELIMITER)

                val currentDateTime = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().time)

                termInfo =
                    "$termInfo${Constants.COURSE_GRADE_PAIR_DELIMITER}XPA${Constants.COURSE_GRADE_DELIMITER}$xpa" +
                            "${Constants.COURSE_GRADE_PAIR_DELIMITER}LAST_CHECK${Constants.COURSE_GRADE_DELIMITER}$currentDateTime"

                applicationContext.getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE).
                    edit().putString(term, termInfo).apply()

                if (periodic && notifyNewGrades(oldTermInfo as String, termInfo)) {
                    LocalBroadcastManager.getInstance(applicationContext).
                        sendBroadcast(Intent(Constants.INTENT_NEW_GRADES))
                }

                termInfoResultSuccessful = true
            }
        }

        return if (termInfoResultSuccessful) Result.success() else Result.failure()
    }


    /**
     * Issues a notification if new grades are discovered.
     * @param oldInfo Term info string before the worker run
     * @param newInfo Term info string after the worker run
     * @return True, if new grades are found. False, otherwise.
     */
    private fun notifyNewGrades(oldInfo: String, newInfo: String): Boolean {
        val oldCourseGradeInfo = oldInfo.split("${Constants.COURSE_GRADE_PAIR_DELIMITER}XPA")[0]
        val newCourseGradeInfo = newInfo.split("${Constants.COURSE_GRADE_PAIR_DELIMITER}XPA")[0]

        if (oldCourseGradeInfo == newCourseGradeInfo) return false

        val oldCourseGradePairs = oldCourseGradeInfo.split(Constants.COURSE_GRADE_PAIR_DELIMITER)
        val newCourseGradePairs = newCourseGradeInfo.split(Constants.COURSE_GRADE_PAIR_DELIMITER)

        var notificationText = ""

        for (i in 0 until newCourseGradePairs.size) {
            val courseGradeList = newCourseGradePairs[i].split(Constants.COURSE_GRADE_DELIMITER)

            val newGrade = courseGradeList[1]
            val oldGrade = oldCourseGradePairs[i].split(Constants.COURSE_GRADE_DELIMITER)[1]

            if (oldGrade != newGrade) {
                notificationText += "${courseGradeList[0]} ($newGrade)   "
            }
        }

        if (notificationText.isNotEmpty()) {
            issueNotification(
                applicationContext,
                Constants.NOTIFICATION_CHANNEL_NEW_GRADES_ID,
                Constants.NOTIFICATION_NEW_GRADES_ID,
                applicationContext.getString(R.string.NOTIFICATION_NEW_GRADES_TITLE),
                notificationText
            )

            return true
        }

        return false
    }

}
