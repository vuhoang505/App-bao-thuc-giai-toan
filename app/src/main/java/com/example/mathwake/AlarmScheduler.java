package com.example.mathwake;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class AlarmScheduler {
    public static final String EXTRA_ALARM_ID = "alarm_id";
    public static final String EXTRA_SNOOZE = "is_snooze";
    private static final int SNOOZE_OFFSET = 1_000_000;

    public static boolean schedule(Context context, AlarmModel alarm) {
        if (alarm == null || !alarm.isEnabled()) return false;
        return setExact(context, alarm.getId(), nextTriggerMillis(alarm), false);
    }

    public static boolean scheduleSnooze(Context context, long alarmId) {
        return setExact(context, alarmId, System.currentTimeMillis() + 5 * 60_000L, true);
    }

    private static boolean setExact(Context context, long alarmId, long atMillis, boolean snooze) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) return false;
        Intent receiverIntent = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ALARM_ID, alarmId).putExtra(EXTRA_SNOOZE, snooze);
        int requestCode = requestCode(alarmId, snooze);
        PendingIntent operation = PendingIntent.getBroadcast(context, requestCode, receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent showOperation = PendingIntent.getActivity(context, requestCode, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(atMillis, showOperation), operation);
        return true;
    }

    public static void cancel(Context context, long alarmId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent normal = pending(context, alarmId, false);
        PendingIntent snooze = pending(context, alarmId, true);
        if (normal != null) manager.cancel(normal);
        if (snooze != null) manager.cancel(snooze);
    }

    private static PendingIntent pending(Context context, long id, boolean snooze) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, requestCode(id, snooze), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(long id, boolean snooze) {
        long value = id % 900_000L + (snooze ? SNOOZE_OFFSET : 0);
        return (int) value;
    }

    public static long nextTriggerMillis(AlarmModel alarm) {
        Calendar now = Calendar.getInstance();
        Calendar candidate = (Calendar) now.clone();
        candidate.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        candidate.set(Calendar.MINUTE, alarm.getMinute());
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);
        Set<Integer> days = parseDays(alarm.getRepeatDays());
        if (days.isEmpty()) {
            if (candidate.getTimeInMillis() <= now.getTimeInMillis()) candidate.add(Calendar.DAY_OF_YEAR, 1);
            return candidate.getTimeInMillis();
        }
        for (int offset = 0; offset <= 7; offset++) {
            Calendar test = (Calendar) candidate.clone();
            test.add(Calendar.DAY_OF_YEAR, offset);
            if (days.contains(test.get(Calendar.DAY_OF_WEEK)) && test.getTimeInMillis() > now.getTimeInMillis())
                return test.getTimeInMillis();
        }
        candidate.add(Calendar.DAY_OF_YEAR, 7);
        return candidate.getTimeInMillis();
    }

    private static Set<Integer> parseDays(String repeatDays) {
        Set<Integer> result = new HashSet<>();
        if (repeatDays == null || repeatDays.trim().isEmpty()) return result;
        for (String part : repeatDays.split(",")) {
            try { result.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    public static Intent exactAlarmSettingsIntent(Context context) {
        // Dùng chuỗi action để lớp vẫn nạp an toàn trên Android 8-11.
        return new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM",
                android.net.Uri.parse("package:" + context.getPackageName()));
    }
}
