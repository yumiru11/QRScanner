package com.qrscanner.qrscanner

import android.app.Application
import com.qrscanner.qrscanner.data.ScanHistoryDatabase

class MainApplication : Application() {
    val database: ScanHistoryDatabase by lazy {
        ScanHistoryDatabase.getInstance(this)
    }
}
