// messaging.js
console.log("Extension content script loaded");

// 發送原生消息
//browser.runtime.sendNativeMessage("browser", {
//    type: "PageInfo",
//    title: "test title"
//});

// 注入腳本到頁面上下文
const script = document.createElement('script');
script.textContent = `
    window.initialsetting = {
        "docarduid": function (fn) {
            console.log("Web called docarduid with:", fn);
            // 發送自定義事件給 content script
            window.dispatchEvent(new CustomEvent('docarduid-called', {
                detail: fn
            }));
        }
    };
`;
(document.head || document.documentElement).appendChild(script);
script.remove();

// 監聽網頁端的調用
window.addEventListener('docarduid-called', function(e) {
    console.log('Content script received docarduid call with:', e.detail);
    // 發送消息給 Android
    browser.runtime.sendNativeMessage("browser", {
        type: "DocarduidCall",
        callback: e.detail
    });
});
