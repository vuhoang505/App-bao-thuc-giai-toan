package com.example.mathwake;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import android.util.Log;

public class AlarmService extends Service {
    public static final String ACTION_STOP = "com.example.mathwake.STOP_ALARM";
    private static final String CHANNEL_ID = "mathwake_alarm";
    private static final int NOTIFICATION_ID = 4102;
    private static final long WAKE_LOCK_TIMEOUT_MS = 30 * 60_000L;
    private static final String TAG = "MathWakeService";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private float configuredVolume = 1f;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel(this);
        PowerManager powerManager = getSystemService(PowerManager.class);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MathWake:AlarmWakeLock");
        wakeLock.setReferenceCounted(false);
        audioManager = getSystemService(AudioManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlarm();
            return START_NOT_STICKY;
        }
        long alarmId = intent == null ? -1 : intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        AlarmModel alarm = new DatabaseHelper(this).getAlarm(alarmId);
        if (alarm == null) { stopSelf(); return START_NOT_STICKY; }
        if (!wakeLock.isHeld()) wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        startForeground(NOTIFICATION_ID, buildNotification(this, alarm));
        play(alarm);
        return START_NOT_STICKY;
    }

    private static Notification buildNotification(Context context, AlarmModel alarm) {
        PendingIntent fullScreen = AlarmScreenLauncher.createPendingIntent(context, alarm.getId());
        return new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(alarm.getLabel().isEmpty() ? context.getString(R.string.app_name) : alarm.getLabel())
                .setContentText(context.getString(R.string.notification_content))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .addAction(new Notification.Action.Builder(null, context.getString(R.string.notification_open_math), fullScreen).build())
                .build();
    }

    public static void showFallbackNotification(Context context, AlarmModel alarm) {
        createChannel(context);
        context.getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification(context, alarm));
    }

    private void play(AlarmModel alarm) {
        if (mediaPlayer != null) return;
        Uri uri;
        try { uri = alarm.getRingtone().isEmpty() ? null : Uri.parse(alarm.getRingtone()); }
        catch (Exception ignored) { uri = null; }
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        configuredVolume = alarm.getVolume() / 100f;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(this::onAudioFocusChanged)
                .setAcceptsDelayedFocusGain(false)
                .build();
        int focusResult = audioManager.requestAudioFocus(audioFocusRequest);
        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (!startPlayer(uri, attributes, configuredVolume)) {
                Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (fallback == null) fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                startPlayer(fallback, attributes, configuredVolume);
            }
        } else {
            Log.w(TAG, "Audio focus bị từ chối; chỉ rung và hiển thị thông báo");
        }
        if (alarm.isVibration()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vm.getDefaultVibrator();
            } else vibrator = getSystemService(Vibrator.class);
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 500}, 0));
        }
    }

    private boolean startPlayer(Uri uri, AudioAttributes attributes, float volume) {
        if (uri == null) return false;
        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(attributes);
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Không thể phát URI nhạc chuông", e);
            releasePlayer();
            return false;
        }
    }

    private void onAudioFocusChanged(int focusChange) {
        if (mediaPlayer == null) return;
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            mediaPlayer.setVolume(configuredVolume, configuredVolume);
            if (!mediaPlayer.isPlaying()) mediaPlayer.start();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            mediaPlayer.setVolume(configuredVolume * 0.2f, configuredVolume * 0.2f);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
        }
    }

    private static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notification_channel_description));
        channel.setSound(null, null);
        channel.enableVibration(false);
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private void stopAlarm() {
        releasePlayer();
        if (audioManager != null && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            audioFocusRequest = null;
        }
        if (vibrator != null) { vibrator.cancel(); vibrator = null; }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releasePlayer() {
        if (mediaPlayer == null) return;
        try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (IllegalStateException ignored) { }
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override public void onDestroy() { stopAlarm(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
