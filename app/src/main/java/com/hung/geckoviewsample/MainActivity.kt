package com.hung.geckoviewsample

import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.MessageDelegate
import org.mozilla.geckoview.WebExtension.MessageSender

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
    val TAG = "GeckoViewSample"
    val mContext = LocalContext.current
    val EXTENSION_LOCATION = "resource://android/assets/messaging/"
    val mainHandler = android.os.Handler(Looper.getMainLooper())

    // 初始化 GeckoView 設定
    var settings = GeckoRuntimeSettings.Builder()
        .consoleOutput(true)  // 啟用 console 輸出到 logcat
        .debugLogging(true)   // 啟用除錯日誌
        .javaScriptEnabled(true) // 啟用 JavaScript
        .build()

    // 創建並記住 GeckoRuntime 實例
    val runtime = remember {
        GeckoRuntime.create(mContext, settings)
    }

    // 創建並記住 GeckoSession 實例
    val session = remember {
        GeckoSession().apply {
            open(runtime)
//            loadUri("https://www.hung.services/size/")
            loadUri("https://liuxiaogang.cn/ua.html")
        }
    }

    val messageDelegate: MessageDelegate = object : MessageDelegate {
        @Nullable
        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: MessageSender
        ): GeckoResult<Any>? {
            Log.d(TAG, "onMessage, nativeApp: $nativeApp, message: $message")

            if (message is JSONObject) {
                // Do something with message
                val json = message as JSONObject
                try {
                    val type = json.getString("type")
                    if ("DocarduidCall" == type) {
                        val callback = json.getString("callback")
                        Log.i("MessageDelegate", "Calling docarduid with: $callback")
                        mainHandler.post {
                            session.loadUri("javascript:$callback('1234567890')");
                        }
                    }
                } catch (ex: JSONException) {
                    Log.e("MessageDelegate", "Error processing message", ex)
                }
            }
            return null
        }
    }

    runtime.webExtensionController
        .ensureBuiltIn(EXTENSION_LOCATION, "messaging@example.com")
        .accept( // Set delegate that will receive messages coming from this extension.
            { extension: WebExtension? ->
                if (extension != null) {
                    Log.d("MessageDelegate", "Extension registered")
                    mainHandler.post{
                        session.webExtensionController
                            .setMessageDelegate(extension, messageDelegate, "browser")
                    }
                }
            },  // Something bad happened, let's log an error
            { e: Throwable? ->
                Log.e(
                    "MessageDelegate",
                    "Error registering extension",
                    e
                )
            }
        )

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