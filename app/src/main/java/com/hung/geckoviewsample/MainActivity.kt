package com.hung.geckoviewsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.hung.geckoviewsample.ui.theme.GeckoviewSampleTheme
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeckoViewScreen()
        }
    }
}

@Composable
fun GeckoViewScreen() {
    val context = LocalContext.current

    // 初始化 GeckoView 設定
    var settings = GeckoRuntimeSettings.Builder()
        .consoleOutput(true)  // 啟用 console 輸出到 logcat
        .debugLogging(true)   // 啟用除錯日誌
        .javaScriptEnabled(true) // 啟用 JavaScript
        .build()

    // 創建並記住 GeckoRuntime 實例
    val runtime = remember {
        GeckoRuntime.create(context, settings)
    }

    // 創建並記住 GeckoSession 實例
    val session = remember {
        GeckoSession().apply {
            open(runtime)
            loadUri("https://www.hung.services/size/")
        }
    }

    // 使用 AndroidView 來包裝 GeckoView
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            GeckoView(context).apply {
                setSession(session)
            }
        }
    )

    // 當 Composable 被處置時清理資源
    DisposableEffect(Unit) {
        onDispose {
            session.close()
            runtime.shutdown()
        }
    }
}