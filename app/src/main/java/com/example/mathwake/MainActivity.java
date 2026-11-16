package com.example.mathwake;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.provider.Settings;
import android.app.NotificationManager;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AlarmAdapter.Listener {
    private DatabaseHelper database;
    private AlarmAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = new DatabaseHelper(this);
        ListView list = findViewById(R.id.alarmList);
        list.setEmptyView(findViewById(R.id.emptyView));
        adapter = new AlarmAdapter(this, new ArrayList<>(), this);
        list.setAdapter(adapter);
        Button add = findViewById(R.id.buttonAdd);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddAlarmActivity.class)));
        requestNotificationPermission();
        requestFullScreenPermissionOnce();
    }

    @Override protected void onResume() { super.onResume(); reload(); }
    private void reload() { adapter.setAlarms(database.getAllAlarms()); }

    @Override
    public void onToggle(AlarmModel alarm, boolean enabled) {
        alarm.setEnabled(enabled);
        database.updateAlarm(alarm);
        if (enabled) {
            if (!AlarmScheduler.schedule(this, alarm)) {
                alarm.setEnabled(false); database.updateAlarm(alarm);
                showExactAlarmDialog();
            } else Toast.makeText(this, R.string.alarm_enabled_message, Toast.LENGTH_SHORT).show();
        } else {
            AlarmScheduler.cancel(this, alarm.getId());
            // Nếu báo thức này đang reo, tắt switch cũng phải dừng âm thanh.
            stopService(new Intent(this, AlarmService.class));
            Toast.makeText(this, R.string.alarm_disabled_message, Toast.LENGTH_SHORT).show();
        }
        reload();
    }

    @Override public void onEdit(AlarmModel alarm) {
        startActivity(new Intent(this, AddAlarmActivity.class).putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.getId()));
    }

    @Override public void onDelete(AlarmModel alarm) {
        String time = String.format(Locale.getDefault(), "%02d:%02d", alarm.getHour(), alarm.getMinute());
        new AlertDialog.Builder(this).setTitle(R.string.delete_alarm_title)
                .setMessage(getString(R.string.delete_alarm_message, time))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AlarmScheduler.cancel(this, alarm.getId()); database.deleteAlarm(alarm.getId()); reload();
                }).show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
    }

    private void showExactAlarmDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        new AlertDialog.Builder(this).setTitle(R.string.allow_exact_alarm_title)
                .setMessage(R.string.allow_exact_alarm_message)
                .setNegativeButton(R.string.later, null)
                .setPositiveButton(R.string.open_settings, (d, w) -> startActivity(AlarmScheduler.exactAlarmSettingsIntent(this))).show();
    }

    private void requestFullScreenPermissionOnce() {
        if (Build.VERSION.SDK_INT < 34) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.canUseFullScreenIntent()) return;
        if (getPreferences(MODE_PRIVATE).getBoolean("asked_full_screen", false)) return;
        getPreferences(MODE_PRIVATE).edit().putBoolean("asked_full_screen", true).apply();
        new AlertDialog.Builder(this).setTitle(R.string.full_screen_title)
                .setMessage(R.string.full_screen_message)
                .setNegativeButton(R.string.later, null)
                .setPositiveButton(R.string.open_settings, (d, w) -> {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(settings);
                }).show();
    }
}
