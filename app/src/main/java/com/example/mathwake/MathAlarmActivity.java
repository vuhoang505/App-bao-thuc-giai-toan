package com.example.mathwake;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MathAlarmActivity extends AppCompatActivity {
    private static final String STATE_COMPLETED = "completed";
    private static final String STATE_QUESTION = "question";
    private static final String STATE_CORRECT_ANSWER = "correct_answer";
    private static final String STATE_OPERATION = "operation";
    private static final String STATE_TYPED_ANSWER = "typed_answer";
    private static final String STATE_FEEDBACK = "feedback";
    private AlarmModel alarm;
    private MathQuestionGenerator generator;
    private MathQuestionGenerator.Question current;
    private int completed;
    private TextView progress, question, feedback;
    private EditText answer;

    @SuppressWarnings("deprecation") // API 26 cần hai window flag cũ; API 27+ dùng phương thức mới.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguard.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.activity_math_alarm);
        long id = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        alarm = new DatabaseHelper(this).getAlarm(id);
        if (alarm == null) { stopService(new Intent(this, AlarmService.class)); finish(); return; }
        ensureAlarmService(id);
        ((TextView) findViewById(R.id.textCurrentTime)).setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        ((TextView) findViewById(R.id.textAlarmLabel)).setText(alarm.getLabel());
        progress = findViewById(R.id.textProgress); question = findViewById(R.id.textQuestion);
        feedback = findViewById(R.id.textFeedback); answer = findViewById(R.id.editAnswer);
        generator = new MathQuestionGenerator();
        findViewById(R.id.buttonCheck).setOnClickListener(v -> checkAnswer());
        answer.setOnEditorActionListener((v, actionId, event) -> { if (actionId == EditorInfo.IME_ACTION_DONE) { checkAnswer(); return true; } return false; });
        Button snooze = findViewById(R.id.buttonSnooze);
        snooze.setVisibility(alarm.isSnoozeEnabled() ? android.view.View.VISIBLE : android.view.View.GONE);
        snooze.setOnClickListener(v -> snooze());
        findViewById(R.id.buttonEmergency).setOnClickListener(v -> emergencyStop());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Toast.makeText(MathAlarmActivity.this, R.string.back_blocked_message, Toast.LENGTH_SHORT).show();
            }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_QUESTION)) {
            completed = savedInstanceState.getInt(STATE_COMPLETED, 0);
            current = new MathQuestionGenerator.Question(
                    savedInstanceState.getString(STATE_QUESTION, ""),
                    savedInstanceState.getInt(STATE_CORRECT_ANSWER),
                    savedInstanceState.getString(STATE_OPERATION, AlarmModel.OP_RANDOM));
            generator.restorePreviousText(current.getText());
            renderCurrentQuestion();
            answer.setText(savedInstanceState.getString(STATE_TYPED_ANSWER, ""));
            feedback.setText(savedInstanceState.getString(STATE_FEEDBACK, ""));
            answer.setSelection(answer.length());
            showKeyboard();
        } else {
            nextQuestion();
        }
    }

    private void nextQuestion() {
        current = generator.generate(alarm.getDifficulty(), alarm.getOperationType());
        renderCurrentQuestion();
        answer.getText().clear();
        feedback.setText(null);
        showKeyboard();
    }

    private void renderCurrentQuestion() {
        progress.setText(getString(R.string.question_progress, completed + 1, alarm.getQuestionCount()));
        question.setText(current.getText());
        answer.requestFocus();
    }

    private void showKeyboard() {
        answer.postDelayed(() -> {
            answer.requestFocus();
            InputMethodManager input = getSystemService(InputMethodManager.class);
            input.showSoftInput(answer, 0);
        }, 180);
    }

    private void ensureAlarmService(long alarmId) {
        try {
            startForegroundService(new Intent(this, AlarmService.class)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId));
        } catch (RuntimeException ignored) {
            // Notification do receiver tạo vẫn cho phép người dùng mở lại màn hình.
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_COMPLETED, completed);
        if (current != null) {
            outState.putString(STATE_QUESTION, current.getText());
            outState.putInt(STATE_CORRECT_ANSWER, current.getAnswer());
            outState.putString(STATE_OPERATION, current.getOperation());
        }
        outState.putString(STATE_TYPED_ANSWER, answer == null ? "" : answer.getText().toString());
        outState.putString(STATE_FEEDBACK, feedback == null ? "" : feedback.getText().toString());
    }

    private void checkAnswer() {
        String value = answer.getText().toString().trim();
        if (value.isEmpty()) { feedback.setText(R.string.enter_answer); return; }
        try {
            if (Integer.parseInt(value) == current.getAnswer()) {
                completed++;
                if (completed >= alarm.getQuestionCount()) finishAlarm(true);
                else { feedback.setText(R.string.correct_answer); nextQuestion(); }
            } else feedback.setText(R.string.incorrect_answer);
        } catch (NumberFormatException e) { feedback.setText(R.string.integer_answer_required); }
    }

    private void finishAlarm(boolean completedNormally) {
        stopService(new Intent(this, AlarmService.class).setAction(AlarmService.ACTION_STOP));
        Toast.makeText(this, completedNormally ? R.string.completed_message : R.string.emergency_stopped_message,
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void snooze() {
        if (AlarmScheduler.scheduleSnooze(this, alarm.getId())) {
            stopService(new Intent(this, AlarmService.class).setAction(AlarmService.ACTION_STOP));
            Toast.makeText(this, R.string.snoozed_message, Toast.LENGTH_LONG).show(); finish();
        } else Toast.makeText(this, R.string.snooze_permission_required, Toast.LENGTH_LONG).show();
    }

    private void emergencyStop() {
        new AlertDialog.Builder(this).setTitle(R.string.emergency_dialog_title)
                .setMessage(R.string.emergency_dialog_message)
                .setNegativeButton(R.string.continue_solving, null)
                .setPositiveButton(R.string.stop_alarm, (d, w) -> finishAlarm(false)).show();
    }

}
