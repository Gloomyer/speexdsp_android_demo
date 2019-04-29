package com.gloomyer.myspeex;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.gloomyer.myspeex.interfaces.SpeexJNIBridge;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    //来源：麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 44100;
    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    // 音频格式：PCM编码
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    AudioRecord mAudioRecord;
    AudioTrack mAudioTrack;
    boolean isRecord;
    int bufferSize;
    byte[] buffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 123);
    }

    public void normal(View view) {
        record(false);
    }

    public void speex(View view) {
        record(true);
    }

    public void stop(View view) {
        isRecord = false;
        mAudioRecord.stop();
        mAudioRecord.release();

        mAudioTrack.stop();
        mAudioTrack.release();
    }


    /**
     * 是否启用jni层处理功能
     *
     * @param denoise
     */
    private void record(boolean denoise) {
        if (isRecord) {
            Toast.makeText(this, "请先停止", Toast.LENGTH_SHORT).show();
            return;
        }
        bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL, AUDIO_FORMAT);
        mAudioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL, AUDIO_FORMAT, bufferSize);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL, AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);

        if (denoise) {
            SpeexJNIBridge.init(bufferSize, AUDIO_SAMPLE_RATE);
        }

        buffer = new byte[bufferSize];
        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            mAudioRecord = null;
            Toast.makeText(this, "初始化失败!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAudioRecord.startRecording();
        mAudioTrack.play();
        new Thread(new RecordTask(denoise)).start();
    }


    private class RecordTask implements Runnable {

        private final boolean denoise;

        public RecordTask(boolean denoise) {
            this.denoise = denoise;

        }

        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            isRecord = true;
            while (isRecord) {
                int read = mAudioRecord.read(buffer, 0, bufferSize);
                if (read >= 2) {
                    if (denoise) {
                        SpeexJNIBridge.denoise(buffer);
                    }
                    mAudioTrack.write(buffer, 0, read);
                }
            }

            if (denoise)
                SpeexJNIBridge.destory();
        }
    }
}
