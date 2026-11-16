package com.example.mathwake;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.UriPermission;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.appcompat.widget.SwitchCompat;

public class AddAlarmActivity extends AppCompatActivity {
    private static final int PICK_RINGTONE = 50;
    private static final int PICK_AUDIO_FILE = 51;
    private final int[] dayValues = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
    private final int[] dayIds = {R.id.dayMon, R.id.dayTue, R.id.dayWed, R.id.dayThu, R.id.dayFri, R.id.daySat, R.id.daySun};
    private DatabaseHelper database;
    private AlarmModel alarm;
    private Uri selectedRingtone;
    private boolean applyingPreset;
    private TimePicker timePicker;
    private EditText label;
    private Spinner difficulty, count, operation, repeatPreset;
    private SwitchCompat vibration, snooze;
    private SeekBar volume;
    private TextView ringtoneName, nextAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_add_alarm);
        database = new DatabaseHelper(this);
        long id = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        alarm = id >= 0 ? database.getAlarm(id) : new AlarmModel();
        if (alarm == null) { finish(); return; }
        bindViews(); setupSpinners(); populate();
        for (int dayId : dayIds) {
            ((CheckBox) findViewById(dayId)).setOnCheckedChangeListener((button, checked) -> {
                if (applyingPreset) return;
                repeatPreset.setSelection(3);
                updateNextPreview();
            });
        }
        findViewById(R.id.buttonRingtone).setOnClickListener(v -> chooseRingtone());
        findViewById(R.id.buttonSave).setOnClickListener(v -> save());
        findViewById(R.id.buttonCancel).setOnClickListener(v -> finish());
        repeatPreset.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> applyPreset(position)));
        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> updateNextPreview());
    }

    private void bindViews() {
        timePicker = findViewById(R.id.timePicker); label = findViewById(R.id.editLabel);
        difficulty = findViewById(R.id.spinnerDifficulty); count = findViewById(R.id.spinnerCount);
        operation = findViewById(R.id.spinnerOperation); repeatPreset = findViewById(R.id.spinnerRepeat);
        vibration = findViewById(R.id.switchVibration); snooze = findViewById(R.id.switchSnooze);
        volume = findViewById(R.id.seekVolume); ringtoneName = findViewById(R.id.textRingtone);
        nextAlarm = findViewById(R.id.textNextAlarm); timePicker.setIs24HourView(true);
    }

    private void setupSpinners() {
        setItems(difficulty, getResources().getStringArray(R.array.difficulty_labels));
        setItems(count, getResources().getStringArray(R.array.question_count_labels));
        setItems(operation, getResources().getStringArray(R.array.operation_labels));
        setItems(repeatPreset, getResources().getStringArray(R.array.repeat_labels));
    }

    private void setItems(Spinner spinner, String[] values) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(a);
    }

    private void populate() {
        if (alarm.getId() < 0) {
            Calendar now = Calendar.getInstance(); now.add(Calendar.MINUTE, 2);
            alarm.setHour(now.get(Calendar.HOUR_OF_DAY)); alarm.setMinute(now.get(Calendar.MINUTE));
        }
        timePicker.setHour(alarm.getHour()); timePicker.setMinute(alarm.getMinute()); label.setText(alarm.getLabel());
        difficulty.setSelection(indexOf(new String[]{AlarmModel.DIFFICULTY_EASY, AlarmModel.DIFFICULTY_MEDIUM, AlarmModel.DIFFICULTY_HARD}, alarm.getDifficulty()));
        count.setSelection(alarm.getQuestionCount() == 1 ? 0 : alarm.getQuestionCount() == 5 ? 2 : 1);
        operation.setSelection(indexOf(new String[]{AlarmModel.OP_RANDOM, AlarmModel.OP_ADD, AlarmModel.OP_SUBTRACT, AlarmModel.OP_MULTIPLY, AlarmModel.OP_DIVIDE}, alarm.getOperationType()));
        vibration.setChecked(alarm.isVibration()); snooze.setChecked(alarm.isSnoozeEnabled()); volume.setProgress(alarm.getVolume());
        selectedRingtone = alarm.getRingtone().isEmpty() ? null : Uri.parse(alarm.getRingtone());
        ringtoneName.setText(selectedRingtone == null ? getString(R.string.default_alarm_sound) : ringtoneTitle(selectedRingtone));
        setDayChecks(alarm.getRepeatDays());
        if (alarm.getRepeatDays().isEmpty()) repeatPreset.setSelection(0);
        else if (alarm.getRepeatDays().equals("1,2,3,4,5,6,7")) repeatPreset.setSelection(1);
        else if (alarm.getRepeatDays().equals("2,3,4,5,6")) repeatPreset.setSelection(2);
        else repeatPreset.setSelection(3);
        updateNextPreview();
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i; return 0;
    }

    private void applyPreset(int position) {
        applyingPreset = true;
        try {
            if (position == 0) setAllDays(false);
            else if (position == 1) setAllDays(true);
            else if (position == 2) {
                setAllDays(false);
                for (int i = 0; i < 5; i++) ((CheckBox) findViewById(dayIds[i])).setChecked(true);
            }
        } finally {
            applyingPreset = false;
        }
        updateNextPreview();
    }

    private void setAllDays(boolean checked) { for (int id : dayIds) ((CheckBox) findViewById(id)).setChecked(checked); }
    private void setDayChecks(String days) {
        setAllDays(false); if (days == null) return;
        for (int i = 0; i < dayValues.length; i++) if (containsDay(days, dayValues[i])) ((CheckBox) findViewById(dayIds[i])).setChecked(true);
    }
    private boolean containsDay(String days, int day) {
        for (String p : days.split(",")) if (p.trim().equals(String.valueOf(day))) return true; return false;
    }

    private String collectDays() {
        List<Integer> values = new ArrayList<>();
        // Lưu theo thứ tự Calendar: CN, T2 ... T7 để nhận diện preset ổn định.
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            for (int i = 0; i < dayValues.length; i++)
                if (dayValues[i] == day && ((CheckBox) findViewById(dayIds[i])).isChecked()) values.add(day);
        }
        StringBuilder s = new StringBuilder();
        for (int v : values) { if (s.length() > 0) s.append(','); s.append(v); }
        return s.toString();
    }

    private void chooseRingtone() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_ringtone)
                .setItems(R.array.ringtone_source_labels, (dialog, which) -> {
                    if (which == 0) openSystemRingtonePicker();
                    else openAudioFilePicker();
                }).show();
    }

    @SuppressWarnings("deprecation")
    private void openSystemRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtone);
        startActivityForResult(intent, PICK_RINGTONE);
    }

    @SuppressWarnings("deprecation")
    private void openAudioFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("audio/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_AUDIO_FILE);
    }

    @SuppressWarnings("deprecation")
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                selectedRingtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
            } else {
                selectedRingtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            }
            ringtoneName.setText(selectedRingtone == null ? getString(R.string.default_alarm_sound) : ringtoneTitle(selectedRingtone));
        } else if (requestCode == PICK_AUDIO_FILE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri newUri = data.getData();
            releaseOldPersistedPermission(newUri);
            try {
                getContentResolver().takePersistableUriPermission(
                        newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException | IllegalArgumentException ignored) {
                // Provider không hỗ trợ quyền bền vững; service sẽ fallback nếu URI hết hạn.
            }
            selectedRingtone = newUri;
            ringtoneName.setText(ringtoneTitle(selectedRingtone));
        }
    }

    private void save() {
        alarm.setHour(timePicker.getHour()); alarm.setMinute(timePicker.getMinute());
        alarm.setLabel(label.getText().toString().trim().isEmpty() ? getString(R.string.alarm_default_label) : label.getText().toString().trim());
        alarm.setRepeatDays(collectDays());
        alarm.setDifficulty(new String[]{AlarmModel.DIFFICULTY_EASY, AlarmModel.DIFFICULTY_MEDIUM, AlarmModel.DIFFICULTY_HARD}[difficulty.getSelectedItemPosition()]);
        alarm.setQuestionCount(new int[]{1, 3, 5}[count.getSelectedItemPosition()]);
        alarm.setOperationType(new String[]{AlarmModel.OP_RANDOM, AlarmModel.OP_ADD, AlarmModel.OP_SUBTRACT, AlarmModel.OP_MULTIPLY, AlarmModel.OP_DIVIDE}[operation.getSelectedItemPosition()]);
        alarm.setVibration(vibration.isChecked()); alarm.setSnoozeEnabled(snooze.isChecked()); alarm.setVolume(volume.getProgress());
        alarm.setRingtone(selectedRingtone == null ? "" : selectedRingtone.toString()); alarm.setEnabled(true);
        if (alarm.getId() < 0) database.insertAlarm(alarm); else { AlarmScheduler.cancel(this, alarm.getId()); database.updateAlarm(alarm); }
        if (!AlarmScheduler.schedule(this, alarm)) {
            alarm.setEnabled(false); database.updateAlarm(alarm);
            new AlertDialog.Builder(this).setTitle(R.string.alarm_cannot_enable_title)
                    .setMessage(R.string.alarm_cannot_enable_message)
                    .setPositiveButton(R.string.open_settings, (d, w) -> startActivity(AlarmScheduler.exactAlarmSettingsIntent(this)))
                    .setNegativeButton(R.string.close, (d, w) -> finish()).show();
        } else { finishAfterSave(); }
    }

    private void updateNextPreview() {
        if (nextAlarm == null) return;
        AlarmModel preview = new AlarmModel(); preview.setHour(timePicker.getHour()); preview.setMinute(timePicker.getMinute()); preview.setRepeatDays(collectDays());
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(AlarmScheduler.nextTriggerMillis(preview));
        nextAlarm.setText(getString(R.string.next_alarm, date));
    }

    private String ringtoneTitle(Uri uri) {
        try {
            android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            return ringtone == null ? getString(R.string.default_alarm_sound) : ringtone.getTitle(this);
        } catch (SecurityException e) {
            return getString(R.string.default_alarm_sound);
        }
    }

    private void releaseOldPersistedPermission(Uri newUri) {
        if (selectedRingtone == null || selectedRingtone.equals(newUri)) return;
        for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
            if (permission.getUri().equals(selectedRingtone) && permission.isReadPermission()) {
                try {
                    getContentResolver().releasePersistableUriPermission(
                            selectedRingtone, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) { }
                return;
            }
        }
    }

    private void finishAfterSave() {
        Toast.makeText(this, R.string.alarm_saved_message, Toast.LENGTH_SHORT).show();
        AudioManager audio = getSystemService(AudioManager.class);
        boolean systemAlarmSilent = audio.getStreamVolume(AudioManager.STREAM_ALARM) == 0
                || audio.isStreamMute(AudioManager.STREAM_ALARM);
        if (alarm.getVolume() > 0 && systemAlarmSilent) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.system_alarm_muted_title)
                    .setMessage(R.string.system_alarm_muted_message)
                    .setNegativeButton(R.string.close, (dialog, which) -> finish())
                    .setPositiveButton(R.string.open_sound_settings, (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
                        finish();
                    })
                    .setOnCancelListener(dialog -> finish())
                    .show();
        } else {
            finish();
        }
    }
}
