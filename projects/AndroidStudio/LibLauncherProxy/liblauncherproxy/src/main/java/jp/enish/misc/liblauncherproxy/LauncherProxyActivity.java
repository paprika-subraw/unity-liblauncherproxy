package jp.enish.misc.liblauncherproxy;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Unity アクティビティの代わりに基本インテントを受け取って
 * Unity のタスクのトップアクティビティに投げるアクティビティです。
 */
public class LauncherProxyActivity extends Activity {
    /**
     * 前面に出したいタスクを探すとき、
     * そのトップアクティビティの情報を入れるためのクラスです。
     */
    private static class TopActivityInfo {
        /**
         * このアクティビティが属するタスクの ID です。
         */
        final int taskId;

        /**
         * アクティビティ名です。
         */
        final ComponentName componentName;

        /**
         * インスタンスを作成します。 (ロリポップ以前)
         *
         * @param taskInfo タスク情報
         */
        TopActivityInfo(ActivityManager.RunningTaskInfo taskInfo) {
            this.taskId = taskInfo.id;
            this.componentName = taskInfo.topActivity;
        }

        /**
         * インスタンスを作成します。 (マシュマロ以降)
         *
         * @param taskInfo タスク情報
         */
        @TargetApi(Build.VERSION_CODES.M)
        TopActivityInfo(ActivityManager.RecentTaskInfo taskInfo) {
            this.taskId = taskInfo.id;
            this.componentName = taskInfo.topActivity;
        }
    }

    /**
     * アクティビティ開始・再開時の処理です。
     */
    @Override
    protected void onResume() {
        super.onResume();
        showUnityPlayerTask();
        this.finish();
    }

    /**
     * UnityPlayerActivity を前面へ出します。
     */
    private void showUnityPlayerTask() {
        Context appContext = this.getApplicationContext();
        PackageManager packageManager = appContext.getPackageManager();
        ActivityInfo destActivity = null;

        // Unity アクティビティ
        // (メタデータ {"unityplayer.UnityActivity":true} を含むアクティビティ)
        // を探す
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    appContext.getPackageName(),
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA
            );

            if (packageInfo.activities != null) {
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    // メタデータを1つも持ってないと null になる?
                    if (activityInfo.metaData == null) {
                        continue;
                    }
                    if (activityInfo.metaData.getBoolean(
                            "unityplayer.UnityActivity"
                    )) {
                        destActivity = activityInfo;
                        break;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (destActivity != null) {
            // 見つけたアクティビティをルートとするタスクを検索
            TopActivityInfo topActivityInfo =
                    this.searchTask(appContext, destActivity);

            // 見つかったら、それを前面に出す
            if (topActivityInfo != null) {
                // このアクティビティ自体の所属タスクがそれであれば閉じる
                if (getTaskId() == topActivityInfo.taskId) {
                    Log.d("LibLauncherProxy", "Found my own task");
                    return;
                }
                // ハニカム以降で REORDER_TASK が許可されていれば
                // moveTaskToFront で前に出す
                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.HONEYCOMB &&
                        topActivityInfo.taskId != -1 &&
                        packageManager.checkPermission(
                                Manifest.permission.REORDER_TASKS,
                                appContext.getPackageName()
                        ) == PackageManager.PERMISSION_GRANTED) {
                    ActivityManager activityManager = (ActivityManager)
                            this.getSystemService(ACTIVITY_SERVICE);

                    activityManager.moveTaskToFront(topActivityInfo.taskId,
                            0);
                    Log.d("LibLauncherProxy", "Found an old task");
                    return;
                }
                // moveTaskToFront できなければ直接インテント送る
                if (topActivityInfo.componentName != null) {
                    this.startActivity(new Intent()
                            .setComponent(topActivityInfo.componentName));
                    Log.d("LibLauncherProxy", "Made a new task");
                    return;
                }
            }

            // Unity アクティビティに渡すべきでないフラグを除き、
            // その他の情報は丸投げ
            Intent intent = new Intent(this.getIntent());
            intent
                    .setComponent(new ComponentName(
                            destActivity.applicationInfo.packageName,
                            destActivity.name
                    ))
                    .setFlags(
                            intent.getFlags() & ~(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            ) | Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    );

            this.startActivity(intent);
            Log.d("LibLauncherProxy", "Sent intent");
        }
    }

    /**
     * 指定アクティビティがルートになっているタスクを探します。
     *
     * @param appContext 自アプリのコンテキスト
     * @param activity   探すアクティビティ
     * @return 発見したタスクのトップアクティビティ (見つからなければ{@code null})
     */
    private TopActivityInfo searchTask(Context appContext, ActivityInfo activity) {
        ActivityManager activityManager = (ActivityManager)
                appContext.getSystemService(ACTIVITY_SERVICE);

        // ロリポップ以前の場合
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // GET_TASKS パーミッションがなければ見つからない
            {
                @SuppressWarnings("deprecation")
                final String permission = Manifest.permission.GET_TASKS;
                if (appContext.getPackageManager().checkPermission(
                        permission,
                        appContext.getPackageName()
                ) != PackageManager.PERMISSION_GRANTED) {
                    return null;
                }
            }

            // 遅いと言われているので本当はあまりよろしくないが他に手段がない…
            // 起動中のタスク (他アプリのも含む) を取得し、目当てのタスクを探す
            // ※ロリポップでは自分のタスクとホーム画面しかもらえないみたい
            @SuppressWarnings("deprecation")
            List<ActivityManager.RunningTaskInfo> taskInfoList =
                    activityManager.getRunningTasks(Integer.MAX_VALUE);

            for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                if (taskInfo != null && taskInfo.baseActivity != null &&
                        taskInfo.baseActivity.getPackageName()
                                .equals(activity.applicationInfo.packageName) &&
                        taskInfo.baseActivity.getClassName()
                                .equals(activity.name)) {
                    // タスクが見つかりはしたけど既に終了しているケース
                    if (taskInfo.numRunning == 0) {
                        return null;
                    }
                    return new TopActivityInfo(taskInfo);
                }
            }
        } else {
            // マシュマロからは自アプリのタスクだけを取得するメソッドで
            // topActivity を取得できるようになりました!
            List<ActivityManager.AppTask> taskInfoList =
                    activityManager.getAppTasks();

            for (ActivityManager.AppTask task :
                    taskInfoList) {
                ActivityManager.RecentTaskInfo taskInfo = task.getTaskInfo();

                if (taskInfo != null && taskInfo.baseActivity != null &&
                        taskInfo.baseActivity.getPackageName()
                                .equals(activity.applicationInfo.packageName) &&
                        taskInfo.baseActivity.getClassName()
                                .equals(activity.name)) {
                    return new TopActivityInfo(taskInfo);
                }
            }
        }
        return null;
    }
}
