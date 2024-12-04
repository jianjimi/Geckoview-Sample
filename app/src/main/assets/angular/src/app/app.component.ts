import { CommonModule } from '@angular/common';
import { Component, NgZone } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  public result: string = '';

  constructor(private ngZone: NgZone) {
    // 在 window 物件上定義回調函數
    (window as any).handleCardResponse = (response: string) => {
      this.result = response;
      // 因為這是從 Android 調用，需要手動觸發變更檢測
      // 如果使用 NgZone，可以注入並使用 ngZone.run()
      this.ngZone.run(() => {
        this.result = response;
        console.info('docarduid handleCardResponse response:', response);
      });
    };
  }

  ngOnInit(): void {
    // 檢查是否在 Android WebView 環境中
    if ((window as any).initialsetting) {
      console.log('Running in Android WebView');
    }
  }

  callAndroid(): void {
    try {
      // 呼叫 Android 的 docarduid 方法，並傳入回調函數名稱
      (window as any).initialsetting.docarduid('handleCardResponse');
    } catch (error) {
      console.error('Error calling Android method:', error);
    }
  }
}
