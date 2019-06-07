package com.cburakaygun.boungraker.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cburakaygun.boungraker.helpers.Constants
import com.cburakaygun.boungraker.helpers.bounLogin
import com.cburakaygun.boungraker.helpers.bounTermInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class TermInfoWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val stuID: String = inputData.getString(Constants.WORKER_TERM_INFO_ID_KEY) ?: return Result.failure()
        val stuPW: String = inputData.getString(Constants.WORKER_TERM_INFO_PW_KEY) ?: return Result.failure()
        val term: String = inputData.getString(Constants.WORKER_TERM_INFO_TERM_KEY) ?: return Result.failure()

        var termInfoResultSuccessful = false

        val loginCookies: HashMap<String, String>? = bounLogin(stuID, stuPW)

        if (loginCookies != null) {
            val termInfoMap: HashMap<String, String>? = bounTermInfo(loginCookies, term)

            if (termInfoMap != null) {
                var termInfo = termInfoMap.keys.sorted().map { termCourse ->
                    "$termCourse${Constants.COURSE_GRADE_DELIMITER}${termInfoMap[termCourse]}"
                }.joinToString(separator = Constants.COURSE_GRADE_PAIR_DELIMITER)

                val currentDateTime = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().time)

                termInfo =
                    "$termInfo${Constants.COURSE_GRADE_PAIR_DELIMITER}LAST_CHECK${Constants.COURSE_GRADE_DELIMITER}$currentDateTime"

                applicationContext.getSharedPreferences(Constants.SHAR_PREF_TERMS_DATA, Context.MODE_PRIVATE).edit().
                    putString(term, termInfo).apply()

                termInfoResultSuccessful = true
            }
        }

        return if (termInfoResultSuccessful) Result.success() else Result.failure()
    }
}
