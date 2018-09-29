package com.v_lbluo.audiovideo;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDemo implements Runnable {

    private static final String TAG = "luo";

    private String mime = MediaFormat.MIMETYPE_AUDIO_AAC;
    private AudioRecord audioRecord;
    private MediaCodec mediaCodec;
    private int rat = 256000;

    private int sampleRate = 44100;
    private int channleCount = 2;
    private int channeleConfig = AudioFormat.CHANNEL_IN_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private FileOutputStream fos;

    private boolean isRecording;
    private Thread mThread;
    private int buffSize;

    private String savePath;

    private static AudioDemo instance;

    private AudioDemo() {

    }

    public static synchronized AudioDemo getInstance() {
        if (instance == null) {
            instance = new AudioDemo();
        }
        return instance;
    }

    public void setmSavePath(String savePath) {
        this.savePath = savePath;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void prepare() throws IOException {
        //音频编码
        MediaFormat format = MediaFormat.createAudioFormat(mime, sampleRate, channleCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, rat);
        mediaCodec = MediaCodec.createEncoderByType(mime);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //音频录制
        buffSize = AudioRecord.getMinBufferSize(sampleRate, channeleConfig, audioFormat) * 2;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channeleConfig, audioFormat, buffSize);
    }

    @SuppressLint("NewApi")
    public void start() throws InterruptedException {
        File file = new File(savePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mediaCodec.start();
        audioRecord.startRecording();
        if (mThread != null && mThread.isAlive()) {
            isRecording = false;
            mThread.join();
        }
        isRecording = true;
        mThread = new Thread(this);
        mThread.start();
    }

    @SuppressLint("NewApi")
    public void stop() throws InterruptedException {
        isRecording = false;
        if (mThread != null) {
            mThread.join();
        }
        audioRecord.stop();
        mediaCodec.stop();
        mediaCodec.release();
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void run() {
        while (isRecording) {
            try {
                readOutputData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void readOutputData() throws IOException {

        int index = mediaCodec.dequeueInputBuffer(-1);
        if (index >= 0) {
            final ByteBuffer buffer = getInputBuffer(index);
            buffer.clear();
            int length = audioRecord.read(buffer, buffSize);
            if (length > 0) {
                mediaCodec.queueInputBuffer(index, 0, length, System.nanoTime() / 1000, 0);
            } else {
                Log.d(TAG, "readOutputData: length = " + length);
            }
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outIndex;
        do {
            outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
            Log.d(TAG, "audio flag---->" + info.flags + "/" + outIndex);
            if (outIndex >= 0) {
                ByteBuffer buffer = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buffer = mediaCodec.getOutputBuffer(outIndex);
                } else {
                    buffer = mediaCodec.getOutputBuffers()[outIndex];
                }
                buffer.position(info.offset);
                byte[] temp = new byte[info.size + 7];
                buffer.get(temp, 7, info.size);
                addADTStoPacket(temp, temp.length);
                fos.write(temp);
                mediaCodec.releaseOutputBuffer(outIndex, false);
            }
        } while (outIndex >= 0);

    }

    @SuppressLint("NewApi")
    private ByteBuffer getInputBuffer(int index) {
        ByteBuffer buffer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buffer = mediaCodec.getInputBuffer(index);
        } else {
            buffer = mediaCodec.getInputBuffers()[index];
        }
        return buffer;
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;
        int freqIdx = 4;
        int chanCfg = 2;
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
