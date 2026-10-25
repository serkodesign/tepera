package com.serkodesign.tepera.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * v3.0 MVP, Етап 4: будильники губляться після перезавантаження й оновлення застосунку, а зміна часу чи поясу
 * зсуває «о котрій» — у всіх цих випадках нагадування плануються заново.
 */
class NudgeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                NudgeScheduler.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
