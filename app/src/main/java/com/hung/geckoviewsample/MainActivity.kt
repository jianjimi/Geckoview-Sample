package com.hung.geckoviewsample

import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.annotation.NonNull
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import android.view.KeyEvent
import android.view.InputDevice

class MainActivity : ComponentActivity() {
    // 声明一个变量来保存GeckoView引用
    private var geckoView: GeckoView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GeckoViewScreen { view ->
                // 保存GeckoView的引用
                geckoView = view
            }
        }
    }
    
    // 处理按键事件，确保TV遥控器和键盘事件能被传递到GeckoView
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        
        // 检查特殊按键（如TV遥控器的D-pad按键）
        val isDirectionOrEnterKey = keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        
        // 如果是方向键或确认键，直接传递给GeckoView
        if (isDirectionOrEnterKey) {
            // 将事件直接分发给GeckoView
            geckoView?.let { view ->
                return view.dispatchKeyEvent(event)
            }
        }
        
        // 其他按键使用默认处理
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun GeckoViewScreen(onGeckoViewCreated: (GeckoView) -> Unit = {}) {
    val TAG = "GeckoViewSample"
    val mContext = LocalContext.current
    val EXTENSION_LOCATION = "resource://android/assets/messaging/"
    val mainHandler = android.os.Handler(Looper.getMainLooper())
    
    // 跟踪导航状态
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

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
            loadUri("https://bi.bvtcc.com/dev")
            
            // 设置权限代理，允许视频自动播放
            setPermissionDelegate(object : PermissionDelegate {
                @Nullable
                override fun onContentPermissionRequest(@NonNull session: GeckoSession, @NonNull perm: ContentPermission): GeckoResult<Int>? {
                    // 允许所有权限请求，包括媒体自动播放
                    return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW)
                }
                
                // 实现onMediaPermissionRequest方法，但我们不需要此功能
                override fun onMediaPermissionRequest(
                    session: GeckoSession,
                    uri: String,
                    video: Array<PermissionDelegate.MediaSource>?,
                    audio: Array<PermissionDelegate.MediaSource>?,
                    callback: PermissionDelegate.MediaCallback
                ) {
                    // 自动允许媒体权限请求
                    if (video != null && video.isNotEmpty()) {
                        callback.grant(video[0], audio?.getOrNull(0))
                    } else {
                        callback.grant(null, audio?.getOrNull(0))
                    }
                }
            })
            
            // 设置导航代理来跟踪导航状态
            setNavigationDelegate(object : NavigationDelegate {
                override fun onCanGoBack(session: GeckoSession, enabled: Boolean) {
                    canGoBack = enabled
                }
                
                override fun onCanGoForward(session: GeckoSession, enabled: Boolean) {
                    canGoForward = enabled
                }
            })
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
                
                // 设置视图获取焦点，这样可以接收键盘事件
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
                
                // 返回键处理
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                        if (canGoBack) {
                            session.goBack()
                            true // 事件已处理
                        } else {
                            false // 继续默认的返回操作
                        }
                    } else {
                        false
                    }
                }
                
                // 调用回调，传递GeckoView引用
                onGeckoViewCreated(this)
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