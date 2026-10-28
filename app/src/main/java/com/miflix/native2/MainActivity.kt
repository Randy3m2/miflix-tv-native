package com.miflix.native2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.miflix.native2.ui.MiFlixNativeApp
import com.miflix.native2.data.AppUsageTracker

class MainActivity : ComponentActivity() {
    private var partyCodeRP43 by mutableStateOf<String?>(null)
    private var contentLinkRP140 by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.miflix.native2.data.CrashReportRP135.install(applicationContext)
        // Covers an old launcher task left over from versions before singleTask.
        if(!isTaskRoot && intent?.action==Intent.ACTION_MAIN &&
            (intent?.hasCategory(Intent.CATEGORY_LAUNCHER)==true || intent?.hasCategory("android.intent.category.LEANBACK_LAUNCHER")==true)) {
            finish();return
        }
        val television=(getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if(television) requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window,false)
        if(television) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        } else {
            androidx.core.view.WindowInsetsControllerCompat(window,window.decorView).apply {isAppearanceLightStatusBars=false;isAppearanceLightNavigationBars=false}
        }
        partyCodeRP43 = intent?.data?.takeIf { it.scheme == "miflix" && it.host == "watch-party" }?.getQueryParameter("code")
        contentLinkRP140=intent?.dataString
        setContent { MiFlixNativeApp(partyCodeRP43,contentLinkRP140) {contentLinkRP140=null;intent?.data=null} }
        window.decorView.post {com.miflix.native2.data.CrashReportRP135.showPrevious(this)}
    }

    override fun onStart() {
        super.onStart()
        AppUsageTracker.start(this)
    }

    override fun onStop() {
        AppUsageTracker.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        contentLinkRP140=intent.dataString
        intent.data?.takeIf { it.scheme == "miflix" && it.host == "watch-party" }?.getQueryParameter("code")?.let { code ->
            partyCodeRP43=code
        }
    }
}
