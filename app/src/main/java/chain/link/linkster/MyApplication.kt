package chain.link.linkster

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RandomStringManager.initialize(this)
    }
}