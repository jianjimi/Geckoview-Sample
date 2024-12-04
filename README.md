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

# 筆記

Reference: [GeckoView WebExtensions 雙向通信總結 - Monica AI Chat](https://monica.im/share/chat?shareId=OfV5IMBNhjUFXJMH)

## GeckoView 建立 javascript interface 的做法

因為 WebView 已經封裝好許多邏輯，所以用法相對簡單，但是在 GeckoView 中，我們需要自己實作一個 WebExtension 來達到和網頁溝通的目的。
所以在這個範例中，我們建立了一個 WebExtension，並且在 App 中實作一個 MessageDelegate 來接收網頁傳來的訊息。
需要 messaging.js 和 background.js 這兩個檔案，這兩個檔案是 WebExtension 的基本檔案，可以參考 [WebExtension](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions) 來了解更多。

## new CustomEvent 的 detail 不可以改

`detail` 是 CustomEvent 中的一個特殊屬性名稱。這是 DOM 規範中定義的標準屬性。