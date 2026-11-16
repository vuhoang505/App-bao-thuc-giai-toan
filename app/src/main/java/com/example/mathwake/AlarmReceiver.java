package com.example.mathwake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "MathWakeReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        boolean snooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false);
        if (id < 0) return;
        DatabaseHelper db = new DatabaseHelper(context);
        AlarmModel alarm = db.getAlarm(id);
        if (alarm == null || (!alarm.isEnabled() && !snooze)) return;

        // Lên lịch lần lặp kế tiếp trước khi phát chuông.
        if (!snooze) {
            if (alarm.getRepeatDays().isEmpty()) db.setEnabled(id, false);
            else AlarmScheduler.schedule(context, alarm);
        }
        Intent service = new Intent(context, AlarmService.class)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, id);
        try {
            context.startForegroundService(service);
        } catch (RuntimeException e) {
            // Exact alarm thường được miễn hạn chế FGS; ROM tùy biến vẫn có thể chặn.
            Log.e(TAG, "Không thể khởi chạy foreground service", e);
            AlarmService.showFallbackNotification(context, alarm);
        }
        AlarmScreenLauncher.launch(context, id);
    }
}
