package com.oidolibre.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public class HearingAmplifierService extends Service {
    public static final String ACTION_START = "com.oidolibre.app.START_AMPLIFIER";
    public static final String ACTION_UPDATE = "com.oidolibre.app.UPDATE_AMPLIFIER";
    public static final String ACTION_STOP = "com.oidolibre.app.STOP_AMPLIFIER";
    public static final String EXTRA_LEFT_LEVEL = "leftLevel";
    public static final String EXTRA_RIGHT_LEVEL = "rightLevel";

    private static final String CHANNEL_ID = "oido_libre_active";
    private static final int NOTIFICATION_ID = 1501;
    private static final int SAMPLE_RATE = 48000;
    private static final float MAX_GAIN = 4.0f;

    private static volatile boolean running = false;
    private volatile int leftLevel = 0;
    private volatile int rightLevel = 0;
    private volatile boolean audioLoopRunning = false;

    private AudioRecord recorder;
    private AudioTrack player;
    private Thread audioThread;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioDeviceCallback deviceCallback;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerAudioRouteGuard();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopAmplifier();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            leftLevel = safeLevel(intent.getIntExtra(EXTRA_LEFT_LEVEL, leftLevel));
            rightLevel = safeLevel(intent.getIntExtra(EXTRA_RIGHT_LEVEL, rightLevel));
        }

        if (ACTION_UPDATE.equals(action)) return START_STICKY;

        startInForeground();
        if (!running) startAmplifier();
        return START_STICKY;
    }

    private void startInForeground() {
        Notification notification = buildNotification();
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE : 0;
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type);
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this, 1, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, HearingAmplifierService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Oído Libre está activo")
                .setContentText("Amplificación por audífonos en funcionamiento")
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(0, "Detener", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Oído Libre activo", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Indica que la amplificación auditiva continúa activa");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startAmplifier() {
        int inputMin = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int outputMin = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int frames = Math.max(1024, Math.max(inputMin / 2, outputMin / 4));

        recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(frames * 2)
                .build();

        player = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(frames * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED || player.getState() != AudioTrack.STATE_INITIALIZED) {
            stopAmplifier();
            return;
        }

        acquireWakeLock();
        recorder.startRecording();
        player.play();
        running = true;
        audioLoopRunning = true;

        final int bufferFrames = frames;
        audioThread = new Thread(() -> runAudioLoop(bufferFrames), "OidoLibreAudio");
        audioThread.start();
    }

    private void runAudioLoop(int frames) {
        short[] mono = new short[frames];
        short[] stereo = new short[frames * 2];

        while (audioLoopRunning) {
            int read = recorder.read(mono, 0, mono.length, AudioRecord.READ_BLOCKING);
            if (read <= 0) continue;

            float leftGain = levelToGain(leftLevel);
            float rightGain = levelToGain(rightLevel);
            for (int i = 0; i < read; i++) {
                stereo[i * 2] = amplify(mono[i], leftGain);
                stereo[i * 2 + 1] = amplify(mono[i], rightGain);
            }
            player.write(stereo, 0, read * 2, AudioTrack.WRITE_BLOCKING);
        }
    }

    private short amplify(short sample, float gain) {
        int value = Math.round(sample * gain);
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    private float levelToGain(int level) {
        if (level <= 0) return 0.0001f;
        float normalized = safeLevel(level) / 45.0f;
        return Math.min(1.0f + (MAX_GAIN - 1.0f) * normalized * normalized, MAX_GAIN);
    }

    private int safeLevel(int level) {
        return Math.max(0, Math.min(level, 45));
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OidoLibre:Amplifier");
        wakeLock.acquire();
    }

    private void registerAudioRouteGuard() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        deviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                if (running && !hasHeadphones()) stopAmplifier();
            }
        };
        audioManager.registerAudioDeviceCallback(deviceCallback, null);
    }

    private boolean hasHeadphones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && type == AudioDeviceInfo.TYPE_USB_HEADSET)
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && type == AudioDeviceInfo.TYPE_HEARING_AID)
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void stopAmplifier() {
        audioLoopRunning = false;
        running = false;
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) {}
            recorder.release();
            recorder = null;
        }
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        audioLoopRunning = false;
        running = false;
        if (audioManager != null && deviceCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.unregisterAudioDeviceCallback(deviceCallback);
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
