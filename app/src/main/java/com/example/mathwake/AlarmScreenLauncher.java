package com.example.mathwake;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** Mở màn hình báo thức theo cơ chế PendingIntent được Android cho phép. */
public final class AlarmScreenLauncher {
    private static final String TAG = "MathWakeScreen";
    private static final int SCREEN_OFFSET = 2_000_000;

    private AlarmScreenLauncher() { }

    public static PendingIntent createPendingIntent(Context context, long alarmId) {
        Intent screen = new Intent(context, MathAlarmActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, requestCode(alarmId), screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE,
                backgroundStartOptions(true).toBundle());
    }

    public static void launch(Context context, long alarmId) {
        PendingIntent pendingIntent = createPendingIntent(context, alarmId);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions options = backgroundStartOptions(false);
                pendingIntent.send(context, 0, null, null, null, null, options.toBundle());
            } else {
                pendingIntent.send();
            }
        } catch (PendingIntent.CanceledException | SecurityException e) {
            // Notification toàn màn hình trong AlarmService vẫn là đường mở dự phòng.
            Log.e(TAG, context.getString(R.string.screen_launch_error), e);
        }
    }

    private static int requestCode(long id) {
        return SCREEN_OFFSET + (int) (id % 900_000L);
    }

    @SuppressWarnings("deprecation") // Android 14-15 chỉ cung cấp MODE_*_ALLOWED cũ.
    private static ActivityOptions backgroundStartOptions(boolean creator) {
        ActivityOptions options = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT < 34) return options;
        int mode = Build.VERSION.SDK_INT >= 36
                ? ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                : ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED;
        if (creator) options.setPendingIntentCreatorBackgroundActivityStartMode(mode);
        else options.setPendingIntentBackgroundActivityStartMode(mode);
        return options;
    }
}
