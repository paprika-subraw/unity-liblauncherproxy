LibLauncherProxy
===============================================================================

LibLauncherProxy とは、Unity の Android ビルドで別のアクティビティの呼び出しに対応するためのプラグインです。

アプリの起動アイコンを押した時や通知の開封時に送られるインテントを `UnityPlayerActivity` の代わりに受け取るというロジックに因んで名付けられました。

用途
-------------------------------------------------------------------------------

Unity 製アプリで複数のアクティビティを使おうとすると、 `UnityPlayerActivity`
以外のアクティビティが起動した状態で HOME キーで中断 &rarr;
アプリ起動アイコンを押して再開すると `UnityPlayerActivity` に戻ってしまいます。
この問題を解決するために作成したプラグインです。

動作環境
-------------------------------------------------------------------------------

* Unity 5.x
* Android バージョン不問…?

動作確認済み環境:

* Android 4.0 以上 公式エミュレーター
* Android 4.2.2, 6.0 実機

処理内容的には大きく分けて以下のパターンで違うことをやってるので、それぞれテスト推奨です。

* Android 3 未満 (API Lv. 10 Gingerbread MR1 以下)
* Android 3〜4 (API Lv. 11 Honeycomb 〜 19 KitKat)
* Android 5.x (API Lv. 21 Lollipop 〜 Lv. 22 Lollipop MR1)
* Android 6 以上 (API Lv. 23 Marshmallow 以上)

※ Android 4.4W (API Lv. 20 KitKat Watch) はウェアラブル端末用らしいので保証対象外

内容物
-------------------------------------------------------------------------------

* __projects/AndroidStudio/LibLauncherProxy/__ … Android AAR のソース (Gradle@AndroidStudio)
* __projects/Unity/LibLauncherProxy/__ … AAR をアセット化してエクスポートするための Unity プロジェクト
* __LibLauncherProxy.unitypackage__ … ビルド済みのアセット (インストールに使います)
* __LibLauncherProxyForMarshmallow.unitypackage__ … ビルド済みのアセット (Android 6 以上しかサポートしないアプリ用)
* __LICENSE__ … ライセンス
* __README.md__ … これ
* __build__ … ビルドスクリプト (macOS 用)
* __build.bat__ … ビルドスクリプト (Windows 用; バッチスクリプト版)
* __build.ps1__ … ビルドスクリプト (Windows 用; PowerShell 版)

Install
-------------------------------------------------------------------------------

1. 'LibLauncherProxy.unitypackage' を Unity プロジェクトにインポート。
1. 同プロジェクト内 'AndroidManifest.xml' から以下のような記述を含んだ `<activity>` 要素を探し…

    ```xml
    <meta-data android:name="unityplayer.UnityActivity" android:value="true" />
    ```

    その `<activity>` 要素の内側の以下のような記述を削除する。

    ```xml
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    ```

    ```xml
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.INFO" />
    </intent-filter>
    ```

    'AndroidManifest.xml' が見つからない場合は以下のような内容で Assets/Plugins/Android に新規作成すれば多分動きます。

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <manifest
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:tools="http://schemas.android.com/tools">
        <application
            android:label="@string/app_name"
            android:icon="@drawable/app_icon">
            <activity android:name="com.unity3d.player.UnityPlayerActivity">
                <!-- ↓ LibLauncherProxy に含まれるため不要
                <intent-filter>
                    <action android:name="android.intent.action.MAIN" />
                    <category android:name="android.intent.category.LAUNCHER" />
                </intent-filter>
                -->
                <meta-data
                    android:name="unityplayer.UnityActivity"
                    android:value="true" />
            </activity>
        </application>
    </manifest>
    ```

Uninstall
-------------------------------------------------------------------------------

基本は Assets/LibLauncherProxy を消して AndroidManifest.xml を元に戻せば OK ですが、
すでに LibLauncherProxy 導入済バージョンを本番リリースしてしまっている場合はもう一手間必要です。

AndroidManifest.xml の `<application>` 要素の中に以下のタグを追加してください。

```xml
<!-- LibLauncherProxy 導入跡 -->
<activity-alias
    android:name="jp.enish.misc.liblauncherproxy.LauncherProxyActivity"
    android:targetActivity="com.unity3d.player.UnityPlayerActivity">
</activity-alias>
```

一旦公開したアクティビティは跡形もなく消してはいけないことになってるらしいので…。
消すアクティビティを UnityPlayerActivity の別名にします。

※ 独自に起動アクティビティを定義していた場合、そのアクティビティの別名に設定してください。

Permissions
-------------------------------------------------------------------------------

このプラグインを入れると、アプリが以下のパーミッションを要求するようになります。
（いずれも dangerous ではありません。)

* `android.permission.GET_TASKS` … 実行中のアプリの取得
    * Android 6 (API Lv. 23) 以上では不要なので、そのバージョンしかサポートしないアプリに導入するなら改造推奨。
* `android.permission.REORDER_TASKS` … 実行中のアプリの順序変更
    * Android 3 (API Lv. 11) 未満では不要らしいが、今時 Android 3 以上をサポート対象外にすることは考え難いためほぼ必須。

ビルド方法
-------------------------------------------------------------------------------

`build` ないし `build.bat`、`build.ps1` のうち好きなスクリプトをテキストエディタで開いて環境変数 UNITY に Unity のインストール先を指定してください。

あとはそれを実行すればこのディレクトリに `LibLauncherProxy.unitypackage` ができます。
