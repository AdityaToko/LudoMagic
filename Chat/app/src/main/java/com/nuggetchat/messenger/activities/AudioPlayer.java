package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import com.nuggetchat.messenger.BuildConfig;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.utils.MyLog;

import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer implements MediaPlayer.OnErrorListener{

    static final String LOG_TAG = AudioPlayer.class.getSimpleName();
    public static final String BUSYTONE = "busy_tone";
    public static final String RINGTONE = "ringtone";
    public static final String CALLER_TUNE = "caller_tune";
    private static AudioPlayer audioPlayer;

    private Context context;

    private MediaPlayer mediaPlayer;

    private AudioTrack audioTrack;

    private final static int SAMPLE_RATE = 16000;
    private AudioManager audioManager;

    private AudioPlayer(Context context) {
        this.context = context.getApplicationContext();
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        MyLog.d(LOG_TAG, "AUDIOPLAYER setup");
    }

    public static AudioPlayer getInstance(Context context) {
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(context);
        }
        return audioPlayer;
    }

    public void playRingtone(String type) {
        stopRingtone();
        /*audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);*/
        MyLog.d(LOG_TAG, "AUDIOPLAYER Playing");
        Uri toneUri;
        boolean looping = false;
        if (type.equals(BUSYTONE)) {
            toneUri = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.busy_tone);
        } else if (type.equals(CALLER_TUNE)) {
            toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        } else {
            toneUri = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.progress_tone);
            looping = true;
        }

        // Honour silent mode
        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                MyLog.d(LOG_TAG, "AUDIOPLAYER Normal mode");
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);

                try {
                    mediaPlayer.setDataSource(context, toneUri);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    MyLog.e(LOG_TAG, "Could not setup media player for ringtone");
                    mediaPlayer = null;
                    return;
                }
                mediaPlayer.setLooping(looping);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        stopRingtone();
                    }
                });
                mediaPlayer.start();
                break;
        }
    }

    public void stopRingtone() {
        MyLog.d(LOG_TAG, "AUDIOPLAYER Stop called");
        if (mediaPlayer != null) {
            MyLog.d(LOG_TAG, "AUDIOPLAYER player not null while stopping");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void playProgressTone() {
        MyLog.d(LOG_TAG, "AUDIOPLAYER Play Progress Tone");
        stopProgressTone();
        try {
            audioTrack = createProgressTone(context);
            audioTrack.play();
        } catch (Exception e) {
            MyLog.e(LOG_TAG, "Could not play progress tone", e);
        }
    }

    public void stopProgressTone() {
        MyLog.d(LOG_TAG, "AUDIOPLAYER Stop progress tone");
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    private static AudioTrack createProgressTone(Context context) throws IOException {
        MyLog.d(LOG_TAG, "AUDIOPLAYER Creatign progress tone");
        AssetFileDescriptor fd = context.getResources().openRawResourceFd(R.raw.progress_tone);
        int length = (int) fd.getLength();

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 10000, AudioTrack.MODE_STATIC);

        byte[] data = new byte[length];
        readFileToBytes(fd, data);

        audioTrack.write(data, 0, data.length);
        audioTrack.setLoopPoints(0, data.length / 2, 30);

        return audioTrack;
    }

    private static void readFileToBytes(AssetFileDescriptor fd, byte[] data) throws IOException {
        FileInputStream inputStream = fd.createInputStream();

        int bytesRead = 0;
        while (bytesRead < data.length) {
            int res = inputStream.read(data, bytesRead, (data.length - bytesRead));
            if (res == -1) {
                break;
            }
            bytesRead += res;
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        MyLog.e(LOG_TAG, "AUDIOPLAYER Error in media player what-" + i + " extra-" + i1);
        this.mediaPlayer.stop();
        this.mediaPlayer.release();
        mediaPlayer.release();
        return true;
    }

    public void requestAudioFocus() {
        int result = audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result ==  AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            MyLog.d(LOG_TAG, "audio focus granted");
        } else {
            MyLog.d(LOG_TAG, "audio focus not granted");
        }
    }

    public void releaseAudioFocus(){
        audioManager.abandonAudioFocus(null);
        MyLog.d(LOG_TAG, "audio focus released");
    }
}
