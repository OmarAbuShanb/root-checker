package dev.anonymous.root_checker.sample

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.anonymous.root_checher.RootChecker
import dev.anonymous.rootcheckerapp.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // تفعيل وضع التصحيح للتحقق من السجلات في Logcat
        RootChecker.isDebugMode = true

        val textView = findViewById<TextView>(R.id.tv)

        textView.setOnClickListener {
            // الحصول على نتيجة الفحص التفصيلية للروت
            val rootResult = RootChecker.getRootCheckResult(baseContext)
            
            // الحصول على نتيجة الفحص التفصيلية للمحاكي
            val emulatorResult = RootChecker.getEmulatorCheckResult(baseContext)

            val logMessage = StringBuilder()
            logMessage.append("Is Rooted: ${rootResult.isRooted}\n")
            if (rootResult.isRooted) {
                logMessage.append("Root Reasons:\n")
                rootResult.reasons.forEach { reason ->
                    logMessage.append(" - ${reason.name}: ${reason.description}\n")
                }
            } else {
                logMessage.append("No root detected.\n")
            }

            logMessage.append("\nIs Emulator: ${emulatorResult.isEmulator}\n")
            if (emulatorResult.isEmulator) {
                logMessage.append("Emulator Reasons:\n")
                emulatorResult.reasons.forEach { reason ->
                    logMessage.append(" - ${reason.name}: ${reason.description}\n")
                }
            } else {
                logMessage.append("No emulator detected.\n")
            }

            Log.d(TAG, logMessage.toString())
            textView.text = logMessage.toString()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}