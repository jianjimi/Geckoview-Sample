# 前置作業

### build.gradle (app) 依賴

```
implementation 'org.mozilla.geckoview:geckoview:115.0.20230726201356'
```

### build.gradle (project)

```
maven {
    url "https://maven.mozilla.org/maven2/"
}
```

### AndroidManifest.xml 權限

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

# 開始實作

## 第一步：建立簡單的 Web View 顯示網頁

commit: 715768f54275de382629e006e1a59b9fbc8e2b94

```kotlin
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
```

## 第二步：建立讓 Web 和 App 溝通的機制

commit: a9926e4b8afa9c2a0553ee9b2e7fa4099e0cd8cf

```kotlin
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
```

## 第三步：建立一個簡單的網頁

請看 /app/src/main/assets/html/index.html

## 第四步：抓取網頁的 Console 顯示於 App Logcat 中

```kotlin
// 初始化 GeckoView 設定
var settings = GeckoRuntimeSettings.Builder()
    .consoleOutput(true)  // 啟用 console 輸出到 logcat
    .build()
```

## 第五步：使用 Angular 開發網頁和 App 溝通

請看 /app/src/main/assets/angular