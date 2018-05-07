package kotlinhb.com.kotkindemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "MicTest";
    AudioRecord mRecorder;
    AudioTrack mTrack;
    byte mBuffer[];
    boolean mEchoing;
    FileOutputStream os = null;
    String filePath = "";
    String filePathOut = "";
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        mEchoing = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEchoing = false;
    }
    private static final int RECORDER_BPP = 16;
    private static final int SAMPLE_RATE = 16000;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_STEREO;
    private static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_STEREO;

    public void onEchoClicked(View v) {
        Log.i(TAG, "onEchoClicked()");
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.wav";
        filePathOut = Environment.getExternalStorageDirectory().getAbsolutePath() + "/play.wav";
        if (mEchoing) return;
        Log.e("Hellboy", "Path: "+ filePath +"\n"+filePathOut);
        mEchoing = true;

        int bufSize = getBufferSize();

        mBuffer = new byte[bufSize];

        mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
                CHANNEL_IN, ENCODING, mBuffer.length);

        mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                CHANNEL_OUT, ENCODING, mBuffer.length, AudioTrack.MODE_STREAM);

        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
               // mTrack.play();
                mRecorder.startRecording();

                while (mEchoing) {
                    mRecorder.read(mBuffer, 0, mBuffer.length);

                    try {
                        os.write(mBuffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // mTrack.write(mBuffer, 0, mBuffer.length);
                }
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                copyWaveFile(filePath, filePathOut);
                deleteFile(filePath);
                mRecorder.stop();
                mRecorder.release();
                testMic(filePathOut);
               // mTrack.stop();
            }
        }).start();
    }
    public void testMic(final String filePath){
        final MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(filePath);
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mp.stop();
                    mp.release();
                    deleteFile(filePath);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public boolean deleteFile(String filePath){
        File file = new File(filePath);
        boolean deleted = file.delete();
        Log.e("Hellboy", "deleteFile: " + filePath);
        return deleted;
    }
    public void onStopClicked(View v) {
        Log.i(TAG, "onStopClicked()");
        mEchoing = false;
    }

    private int getBufferSize() {
        int outSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING);
        outSize = Integer.highestOneBit(outSize) * 2;

        int inSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING);
        inSize = Integer.highestOneBit(inSize) * 2;

        int bufSize = (outSize > inSize) ? outSize : inSize;

        Log.i(TAG, "bufferSize " + bufSize);
        return bufSize;
    }
    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = SAMPLE_RATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * SAMPLE_RATE * channels/8;

        byte[] data = new byte[getBufferSize()];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.e("hellboy","File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException
    {
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }


}
