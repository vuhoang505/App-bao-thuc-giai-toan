package com.example.mathwake;

public class AlarmModel {
    public static final String DIFFICULTY_EASY = "EASY";
    public static final String DIFFICULTY_MEDIUM = "MEDIUM";
    public static final String DIFFICULTY_HARD = "HARD";
    public static final String OP_RANDOM = "RANDOM";
    public static final String OP_ADD = "ADD";
    public static final String OP_SUBTRACT = "SUBTRACT";
    public static final String OP_MULTIPLY = "MULTIPLY";
    public static final String OP_DIVIDE = "DIVIDE";

    private long id = -1;
    private int hour;
    private int minute;
    private String label = "Báo thức";
    private boolean enabled = true;
    private String repeatDays = "";
    private String difficulty = DIFFICULTY_EASY;
    private int questionCount = 3;
    private String operationType = OP_RANDOM;
    private String ringtone = "";
    private boolean vibration = true;
    private boolean snoozeEnabled = true;
    private int volume = 100;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label == null ? "" : label; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getRepeatDays() { return repeatDays; }
    public void setRepeatDays(String repeatDays) { this.repeatDays = repeatDays == null ? "" : repeatDays; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getRingtone() { return ringtone; }
    public void setRingtone(String ringtone) { this.ringtone = ringtone == null ? "" : ringtone; }
    public boolean isVibration() { return vibration; }
    public void setVibration(boolean vibration) { this.vibration = vibration; }
    public boolean isSnoozeEnabled() { return snoozeEnabled; }
    public void setSnoozeEnabled(boolean snoozeEnabled) { this.snoozeEnabled = snoozeEnabled; }
    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = Math.max(0, Math.min(100, volume)); }
}
