package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media;

import android.media.AudioTrack;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.proframeapps.videoframeplayer.util.NativeHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FullAudioPlayer {
    private static final String LOG_TAG = "FullAudioPlayer";
    private static final int MSG_FLUSH = 4;
    private static final int MSG_PLAYBACK_SPEED = 2;
    private static final int MSG_RELEASE = 3;
    private static final int MSG_WRITE_AUDIO = 1;
    private Handler mAudioHandler;
    private HandlerThread mAudioThread = new HandlerThread("AudioThread") {
        public void run() {
            Process.setThreadPriority(-19);
            super.run();
        }
    };
    private AudioTrack mAudioTrack;
    private int mBufferSizeInFrames;
    private int mChannels;
    private int mFrameSize;
    private Callback mHandlerCallbacks = new Callback() {
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    QueueElem elem = (QueueElem) FullAudioPlayer.this.mQueueElems.poll();
                    if (elem == null) {
                        return true;
                    }
                    if (FullAudioPlayer.this.mRate == 1.0f) {
                        FullAudioPlayer.this.mAudioTrack.write(elem.data, elem.offset, elem.size);
                    } else {
                        FullAudioPlayer.this.processAudio(elem);
                    }
                    if (FullAudioPlayer.this.mAudioHandler.hasMessages(1) || FullAudioPlayer.this.mQueueElems.peek() == null) {
                        return true;
                    }
                    FullAudioPlayer.this.mAudioHandler.sendEmptyMessage(1);
                    return true;
                case 2:
                    if (!FullAudioPlayer.this.mHelperInit) {
                        return true;
                    }
                    FullAudioPlayer.this.mRate = ((Float) msg.obj).floatValue();
                    FullAudioPlayer.this.mHelper.setRate(FullAudioPlayer.this.mRate);
                    if (FullAudioPlayer.this.mRate != 1.0f) {
                        return true;
                    }
                    FullAudioPlayer.this.flushAudioPipeline();
                    return true;
                case 3:
                    FullAudioPlayer.this.mAudioTrack.release();
                    FullAudioPlayer.this.mAudioTrack = null;
                    FullAudioPlayer.this.mAudioThread.quit();
                    FullAudioPlayer.this.mAudioThread.interrupt();
                    return true;
                case 4:
                    if (FullAudioPlayer.this.mAudioTrack.getPlayState() != 3) {
                        FullAudioPlayer.this.mAudioTrack.flush();
                    }
                    if (FullAudioPlayer.this.mRate != 1.0f) {
                        FullAudioPlayer.this.flushAudioPipeline();
                    }
                    FullAudioPlayer.this.mNumFramesSubmitted = 0;
                    FullAudioPlayer.this.mNumBytesQueued = 0;
                    return true;
                default:
                    return false;
            }
        }
    };
    private NativeHelper mHelper = null;
    private boolean mHelperInit = false;
    private int mNumBytesQueued = 0;
    private int mNumFramesSubmitted = 0;
    private ConcurrentLinkedQueue<QueueElem> mQueueElems = new ConcurrentLinkedQueue();
    private float mRate = 1.0f;
    private ByteBuffer mReceiveBuffer = null;
    private int mSampleRate;
    private short[] mShortArray = null;
    private float mStartRate = 1.0f;

    class QueueElem {
        byte[] data;
        int offset;
        int size;

        QueueElem() {
        }
    }

    public FullAudioPlayer(int sampleRate, int channelCount) {
        mHelper = new NativeHelper();
        int channelConfig;
        switch (channelCount) {
            case 1:
                channelConfig = 4;
                break;
            case 2:
                channelConfig = 12;
                break;
            case 6:
                channelConfig = 252;
                break;
            default:
                throw new IllegalArgumentException();
        }
        this.mChannels = channelCount;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, 2);
        this.mAudioTrack = new AudioTrack(3, sampleRate, channelConfig, 2, bufferSize * 2, 1);
        this.mSampleRate = sampleRate;
        this.mFrameSize = channelCount * 2;
        this.mBufferSizeInFrames = bufferSize / this.mFrameSize;
        this.mAudioThread.start();
        this.mAudioHandler = new Handler(this.mAudioThread.getLooper(), this.mHandlerCallbacks);
    }

    public void play() {
        if (this.mAudioTrack.getPlayState() != 3) {
            this.mAudioTrack.play();
            if (!this.mHelperInit) {
                this.mHelperInit = true;
                this.mHelper.initialize(this.mSampleRate, this.mChannels);
                this.mHelper.setSetting(NativeHelper.SETTING_SEQUENCE_MS, 30);
                this.mHelper.setRate(this.mStartRate);
                this.mRate = this.mStartRate;
            }
        }
    }

    public void stop() {
        if (this.mAudioTrack.getPlayState() == 3) {
            this.mAudioTrack.stop();
            this.mNumFramesSubmitted = 0;
            this.mNumBytesQueued = 0;
        }
    }

    public void pause() {
        if (this.mAudioTrack.getPlayState() == 3) {
            this.mAudioTrack.pause();
        }
    }

    public void flush() {
        this.mAudioHandler.sendEmptyMessage(4);
    }

    public void write(ByteBuffer buffer, int size) {
        QueueElem elm = new QueueElem();
        elm.data = new byte[size];
        buffer.get(elm.data, 0, size);
        elm.offset = 0;
        elm.size = size;
        this.mNumBytesQueued += size;
        this.mQueueElems.add(elm);
        this.mAudioHandler.sendEmptyMessage(1);
    }

    private void processAudio(QueueElem elem) {
        if (this.mReceiveBuffer == null || elem.size > this.mReceiveBuffer.capacity()) {
            this.mReceiveBuffer = ByteBuffer.allocateDirect(elem.size);
            this.mReceiveBuffer.order(ByteOrder.LITTLE_ENDIAN);
            Log.d(LOG_TAG, "SampleRate = " + this.mSampleRate + ", channels=" + this.mChannels);
        }
        this.mReceiveBuffer.put(elem.data);
        this.mHelper.putSamples(this.mReceiveBuffer, elem.size / this.mFrameSize);
        int receiveSize;
        do {
            receiveSize = this.mHelper.receiveSamples(this.mReceiveBuffer, elem.size / this.mFrameSize);
            this.mReceiveBuffer.clear();
            this.mReceiveBuffer.position(0);
            if (receiveSize != 0) {
                if (this.mShortArray == null || this.mShortArray.length < this.mChannels * receiveSize) {
                    this.mShortArray = new short[(this.mChannels * receiveSize)];
                }
                this.mReceiveBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(this.mShortArray);
                this.mAudioTrack.write(this.mShortArray, 0, this.mChannels * receiveSize);
                continue;
            }
        } while (receiveSize != 0);
    }

    private void flushAudioPipeline() {
        if (this.mReceiveBuffer != null && this.mReceiveBuffer.capacity() != 0) {
            this.mHelper.flush();
            do {
            } while (this.mHelper.receiveSamples(this.mReceiveBuffer, this.mReceiveBuffer.capacity() / this.mFrameSize) != 0);
        }
    }

    public void setStartRate(float rate) {
        this.mStartRate = rate;
    }

    public void release() {
        this.mAudioHandler.sendEmptyMessage(3);
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getChannelCount() {
        return this.mChannels;
    }

    public void setPlaybackRate(float rate) {
        this.mAudioHandler.obtainMessage(2, new Float(rate)).sendToTarget();
    }
}
