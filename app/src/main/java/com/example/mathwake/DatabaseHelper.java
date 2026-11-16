package com.example.mathwake;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "mathwake.db";
    private static final int DB_VERSION = 2;
    private static final String TABLE = "alarms";

    public DatabaseHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hour INTEGER NOT NULL, minute INTEGER NOT NULL," +
                "label TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 1," +
                "repeatDays TEXT NOT NULL DEFAULT '', difficulty TEXT NOT NULL," +
                "questionCount INTEGER NOT NULL, operationType TEXT NOT NULL," +
                "ringtone TEXT NOT NULL DEFAULT '', vibration INTEGER NOT NULL DEFAULT 1," +
                "snoozeEnabled INTEGER NOT NULL DEFAULT 1, volume INTEGER NOT NULL DEFAULT 100)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN volume INTEGER NOT NULL DEFAULT 100");
    }

    public long insertAlarm(AlarmModel alarm) {
        long id = getWritableDatabase().insert(TABLE, null, toValues(alarm));
        alarm.setId(id);
        return id;
    }

    public boolean updateAlarm(AlarmModel alarm) {
        return getWritableDatabase().update(TABLE, toValues(alarm), "id=?",
                new String[]{String.valueOf(alarm.getId())}) > 0;
    }

    public void setEnabled(long id, boolean enabled) {
        ContentValues values = new ContentValues();
        values.put("enabled", enabled ? 1 : 0);
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{String.valueOf(id)});
    }

    public boolean deleteAlarm(long id) {
        return getWritableDatabase().delete(TABLE, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public AlarmModel getAlarm(long id) {
        try (Cursor cursor = getReadableDatabase().query(TABLE, null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    public List<AlarmModel> getAllAlarms() {
        List<AlarmModel> alarms = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE, null, null, null,
                null, null, "hour ASC, minute ASC")) {
            while (cursor.moveToNext()) alarms.add(fromCursor(cursor));
        }
        return alarms;
    }

    public List<AlarmModel> getEnabledAlarms() {
        List<AlarmModel> alarms = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE, null, "enabled=1", null,
                null, null, "hour ASC, minute ASC")) {
            while (cursor.moveToNext()) alarms.add(fromCursor(cursor));
        }
        return alarms;
    }

    private ContentValues toValues(AlarmModel alarm) {
        ContentValues v = new ContentValues();
        v.put("hour", alarm.getHour()); v.put("minute", alarm.getMinute());
        v.put("label", alarm.getLabel()); v.put("enabled", alarm.isEnabled() ? 1 : 0);
        v.put("repeatDays", alarm.getRepeatDays()); v.put("difficulty", alarm.getDifficulty());
        v.put("questionCount", alarm.getQuestionCount()); v.put("operationType", alarm.getOperationType());
        v.put("ringtone", alarm.getRingtone()); v.put("vibration", alarm.isVibration() ? 1 : 0);
        v.put("snoozeEnabled", alarm.isSnoozeEnabled() ? 1 : 0); v.put("volume", alarm.getVolume());
        return v;
    }

    private AlarmModel fromCursor(Cursor c) {
        AlarmModel a = new AlarmModel();
        a.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        a.setHour(c.getInt(c.getColumnIndexOrThrow("hour")));
        a.setMinute(c.getInt(c.getColumnIndexOrThrow("minute")));
        a.setLabel(c.getString(c.getColumnIndexOrThrow("label")));
        a.setEnabled(c.getInt(c.getColumnIndexOrThrow("enabled")) == 1);
        a.setRepeatDays(c.getString(c.getColumnIndexOrThrow("repeatDays")));
        a.setDifficulty(c.getString(c.getColumnIndexOrThrow("difficulty")));
        a.setQuestionCount(c.getInt(c.getColumnIndexOrThrow("questionCount")));
        a.setOperationType(c.getString(c.getColumnIndexOrThrow("operationType")));
        a.setRingtone(c.getString(c.getColumnIndexOrThrow("ringtone")));
        a.setVibration(c.getInt(c.getColumnIndexOrThrow("vibration")) == 1);
        a.setSnoozeEnabled(c.getInt(c.getColumnIndexOrThrow("snoozeEnabled")) == 1);
        int volumeIndex = c.getColumnIndex("volume");
        a.setVolume(volumeIndex >= 0 ? c.getInt(volumeIndex) : 100);
        return a;
    }
}
