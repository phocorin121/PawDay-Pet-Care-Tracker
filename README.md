# PawDay – Pet Care Tracker

犬・猫・その他のペットのお世話記録。既存HTMLの画面を維持した、オフライン動作のAndroid実機テスト版です。

## Android APK

- Actions: `.github/workflows/android.yml`
- Artifact: `PawDay-debug-<commit>` → `PawDay-debug.apk`
- Application ID: `com.phocorin.pawday`
- versionCode: `1` / versionName: `0.1.0-debug`
- Android 8.0以降。公開用の署名・Google Play公開はまだ行っていません。

GitHub Actionsがアカウントの支払い・利用上限で開始を拒否する場合、コードを修正してもArtifactsは生成されません。課金設定の変更は所有者が判断してください。

## ローカルビルド（GUI不要）

JDK 17、Android SDK 35、Gradle 8.11.1を使います。

```
node --test tests/prototype.test.cjs
gradle assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`。エミュレータ接続時は `gradle connectedDebugAndroidTest` で保存再読込と通知スケジュールのAndroidテストを実行できます。

## 実機テスト

ダウンロード・インストール・10項目のチェック手順・制約は [TESTING.md](TESTING.md) を参照してください。実機での起動・写真・通知到達・共有などは別途確認が必要です。

## Web試作品

`ペット.html` をブラウザで開けます。Android同梱版は `app/src/main/assets/index.html` です。同内容を保持しています。ブラウザとAndroidの保存場所は異なります。
