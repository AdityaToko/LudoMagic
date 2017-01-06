package com.nuggetchat.messenger.activities;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.nuggetchat.messenger.BuildConfig;
import com.nuggetchat.messenger.R;

import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer implements MediaPlayer.OnErrorListener{

    static final String LOG_TAG = AudioPlayer.class.getSimpleName();

    private Context mContext;

    private MediaPlayer mPlayer;

    private AudioTrack mProgressTone;

    private final static int SAMPLE_RATE = 16000;

    public AudioPlayer(Context context) {
        this.mContext = context.getApplicationContext();
        Log.d(LOG_TAG, "AUDIOPLAYER setup");
    }

    public void playRingtone() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        Log.d(LOG_TAG, "AUDIOPLAYER Playing");

        // Honour silent mode
        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                Log.d(LOG_TAG, "AUDIOPLAYER Normal mode");
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_RING);

                try {
                    mPlayer.setDataSource(mContext,
                            Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.progress_tone));
                    mPlayer.prepare();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Could not setup media player for ringtone");
                    mPlayer = null;
                    return;
                }
                mPlayer.setLooping(true);
                mPlayer.start();
                break;
        }
    }

    public void stopRingtone() {
        Log.d(LOG_TAG, "AUDIOPLAYER Stop called");
        if (mPlayer != null) {
            Log.d(LOG_TAG, "AUDIOPLAYER player not null while stopping");
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    public void playProgressTone() {
        Log.d(LOG_TAG, "AUDIOPLAYER Play Progress Tone");
        stopProgressTone();
        try {
            mProgressTone = createProgressTone(mContext);
            mProgressTone.play();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not play progress tone", e);
        }
    }

    public void stopProgressTone() {
        Log.d(LOG_TAG, "AUDIOPLAYER Stop progress tone");
        if (mProgressTone != null) {
            mProgressTone.stop();
            mProgressTone.release();
            mProgressTone = null;
        }
    }

    private static AudioTrack createProgressTone(Context context) throws IOException {
        Log.d(LOG_TAG, "AUDIOPLAYER Creatign progress tone");
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
        Log.e(LOG_TAG, "AUDIOPLAYER Error in media player what-" + i + " extra-" + i1);
        mPlayer.stop();
        mPlayer.release();
        mediaPlayer.release();
        return true;
    }
}
