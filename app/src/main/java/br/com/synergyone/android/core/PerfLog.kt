package br.com.synergyone.android.core

import android.util.Log
import br.com.synergyone.android.BuildConfig

/** Medições de desempenho, só em build debug (`adb logcat -s SynergyPerf`). Sem token, query nem corpo. */
object PerfLog {
    fun d(message: String) {
        if (BuildConfig.PERF_LOG) runCatching { Log.d("SynergyPerf", message) }
    }
}
