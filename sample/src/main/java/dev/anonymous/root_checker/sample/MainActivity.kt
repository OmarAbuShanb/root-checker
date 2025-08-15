package dev.anonymous.root_checker.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dev.anonymous.rootcheckerapp.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        Log.d("RootChecker", "isEmulator: " + RootChecker.isEmulator(baseContext))
//        Log.d("RootChecker", "isDeviceRooted: " + RootChecker.isDeviceRooted(baseContext))
//
//        RootCheckerTest.isEmulator(baseContext)
//        RootCheckerTest.isDeviceRooted(baseContext)

//        Log.d(TAG, "onCreate: RootCheckerTest.checkProcVersion() ${RootCheckerTest.checkProcVersion()}")
//        Log.d(TAG, "onCreate: RootCheckerTest.checkForRootHidingLibs() ${RootCheckerTest.Companion.checkForRootHidingLibs()}")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}