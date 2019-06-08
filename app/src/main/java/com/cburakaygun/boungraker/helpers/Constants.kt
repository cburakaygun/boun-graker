package com.cburakaygun.boungraker.helpers


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
    const val XPA_DELIMITER = "&" // <SPA>&<GPA>

    // WORKERS
    const val WORKER_TERM_INFO_ID_KEY = SHAR_PREF_USER_DATA_ID_KEY
    const val WORKER_TERM_INFO_PW_KEY = SHAR_PREF_USER_DATA_PW_KEY
    const val WORKER_TERM_INFO_TERM_KEY = "TERM"
    const val WORKER_TERM_INFO_PERIODIC_KEY = "PERIODIC"
    const val WORKER_TERM_INFO_PERIODIC_UNIQUE_NAME = "$APP_PACKAGE_NAME.WORKER_TERM_INFO_PERIODIC"

    // INTENTS
    const val INTENT_LOGIN_ID_KEY = SHAR_PREF_USER_DATA_ID_KEY
    const val INTENT_LOGIN_PW_KEY = SHAR_PREF_USER_DATA_PW_KEY

    const val INTENT_LOGIN_RESULT = "$APP_PACKAGE_NAME.INTENT_LOGIN_RESULT"
    const val INTENT_LOGIN_RESULT_KEY = "INTENT_LOGIN_RESULT_KEY"
    const val INTENT_LOGIN_RESULT_VAL_SUCCESS = 0
    const val INTENT_LOGIN_RESULT_VAL_LOGIN_FAIL = 1
    const val INTENT_LOGIN_RESULT_VAL_TERMS_INFO_FAIL = 2

    const val INTENT_NEW_GRADES = "$APP_PACKAGE_NAME.INTENT_NEW_GRADES"

    // NOTIFICATIONS
    const val NOTIFICATION_CHANNEL_NEW_GRADES_ID = "BOUN_GRAKER_NOTIF_CHANNEL_NEW_GRADES"
    const val NOTIFICATION_NEW_GRADES_ID = 1
}
