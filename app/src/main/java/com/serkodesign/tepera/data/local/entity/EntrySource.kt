package com.serkodesign.tepera.data.local.entity

/** Джерело запису активності — див. SRS FR-1.3, FR-2.3. */
enum class EntrySource {
    MANUAL,
    GAP_LABELED, // FR-D.5 (SRS v2.6): запис створено позначенням виявленої паузи
    HEALTH_CONNECT,
    SLEEP_API
}
