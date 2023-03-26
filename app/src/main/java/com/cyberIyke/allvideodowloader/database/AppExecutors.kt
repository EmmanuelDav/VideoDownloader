package com.cyberIyke.allvideodowloader.database

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AppExecutors private constructor(
    private val diskIO: Executor,
    private val networkIO: Executor,
    private val mainThread: Executor
) {
    fun diskIO(): Executor {
        return diskIO
    }

    fun mainThread(): Executor {
        return mainThread
    }

    fun networkIO(): Executor {
        return networkIO
    }

    private class MainThreadExecutor constructor() : Executor {
        private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
        public override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

    companion object {
        private val LOCK: Any = Any()
        private val sInstance: AppExecutors? = null
        val instance: AppExecutors
            get() {
                if (AppExecutors.Companion.sInstance == null) {
                    synchronized(AppExecutors.Companion.LOCK, {
                        AppExecutors.Companion.sInstance = AppExecutors(
                            Executors.newSingleThreadExecutor(),
                            Executors.newFixedThreadPool(3),
                            AppExecutors.MainThreadExecutor()
                        )
                    })
                }
                return AppExecutors.Companion.sInstance
            }
    }
}