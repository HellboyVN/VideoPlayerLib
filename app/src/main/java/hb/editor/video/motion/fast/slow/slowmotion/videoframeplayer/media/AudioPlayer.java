package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media;

import android.media.AudioTrack;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.LinkedList;

public class AudioPlayer {
    static final /* synthetic */ boolean $assertionsDisabled = (!AudioPlayer.class.desiredAssertionStatus());
    private static final String LOG_TAG = "AudioPlayer";
    private AudioTrack mAudioTrack;
    private int mBufferSizeInFrames;
    private int mChannels;
    private int mFrameSize;
    private LinkedList<QueueElem> mInputQueue = new LinkedList();
    private int mNumBytesQueued = 0;
    private int mNumFramesSubmitted = 0;
    private LinkedList<QueueElem> mOutputQueue = new LinkedList();
    private float mRate = 1.0f;
    private int mSampleRate;
    private boolean mWriteMorePending = false;

    class QueueElem {
        byte[] data;
        int offset;
        int size;

        QueueElem() {
        }
    }

    public AudioPlayer(int sampleRate, int channelCount) {
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
        Log.d(LOG_TAG, "Buffer size=" + bufferSize);
        this.mAudioTrack = new AudioTrack(3, sampleRate, channelConfig, 2, bufferSize, 1);
        this.mSampleRate = sampleRate;
        this.mFrameSize = channelCount * 2;
        this.mBufferSizeInFrames = bufferSize / this.mFrameSize;
    }

    public long getAudioTimeUs() {
        return (((long) this.mAudioTrack.getPlaybackHeadPosition()) * 1000000) / ((long) this.mSampleRate);
    }

    public int getNumBytesQueued() {
        return this.mNumBytesQueued;
    }

    public void play() {
        this.mAudioTrack.play();
    }

    public void stop() {
        cancelWriteMore();
        this.mAudioTrack.stop();
        this.mNumFramesSubmitted = 0;
        this.mInputQueue.clear();
        this.mOutputQueue.clear();
        this.mNumBytesQueued = 0;
    }

    public void pause() {
        cancelWriteMore();
        this.mAudioTrack.pause();
    }

    public void flush() {
        if (this.mAudioTrack.getPlayState() != 3) {
            this.mAudioTrack.flush();
            this.mNumFramesSubmitted = 0;
            this.mInputQueue.clear();
            this.mOutputQueue.clear();
            this.mNumBytesQueued = 0;
        }
    }

    public void release() {
        cancelWriteMore();
        this.mAudioTrack.release();
        this.mAudioTrack = null;
    }

    public void process() {
        this.mWriteMorePending = false;
        writeMore();
    }

    public int getPlayState() {
        return this.mAudioTrack.getPlayState();
    }

    private void writeMore() {
        if (!this.mOutputQueue.isEmpty()) {
            int numBytesAvailableToWrite = (this.mBufferSizeInFrames - (this.mNumFramesSubmitted - this.mAudioTrack.getPlaybackHeadPosition())) * this.mFrameSize;
            while (numBytesAvailableToWrite > 0) {
                QueueElem elem = (QueueElem) this.mOutputQueue.peekFirst();
                int numBytes = elem.size;
                if (numBytes > numBytesAvailableToWrite) {
                    numBytes = numBytesAvailableToWrite;
                }
                int written = this.mAudioTrack.write(elem.data, elem.offset, numBytes);
                if ($assertionsDisabled || written == numBytes) {
                    this.mNumFramesSubmitted += written / this.mFrameSize;
                    elem.size -= numBytes;
                    numBytesAvailableToWrite -= numBytes;
                    this.mNumBytesQueued -= numBytes;
                    if (elem.size == 0) {
                        this.mInputQueue.add(this.mOutputQueue.removeFirst());
                        if (this.mOutputQueue.isEmpty()) {
                            break;
                        }
                    } else {
                        elem.offset += numBytes;
                    }
                } else {
                    throw new AssertionError();
                }
            }
            if (!this.mOutputQueue.isEmpty()) {
                scheduleWriteMore();
            }
        }
    }

    private void scheduleWriteMore() {
        if (!this.mWriteMorePending) {
            int pendingDurationMs = ((this.mNumFramesSubmitted - this.mAudioTrack.getPlaybackHeadPosition()) * 1000) / this.mSampleRate;
            this.mWriteMorePending = true;
        }
    }

    private void cancelWriteMore() {
        this.mWriteMorePending = false;
    }

    public void write(ByteBuffer buffer, int size) {
        QueueElem elem;
        if (this.mInputQueue.isEmpty()) {
            elem = new QueueElem();
            elem.data = new byte[size];
        } else {
            elem = (QueueElem) this.mInputQueue.remove();
            if (elem.data.length < size) {
                elem.data = new byte[size];
            }
        }
        buffer.get(elem.data, 0, size);
        elem.offset = 0;
        elem.size = size;
        this.mNumBytesQueued += size;
        this.mOutputQueue.add(elem);
    }

    public int getSampleRate() {
        return this.mSampleRate;
    }

    public int getChannelCount() {
        return this.mChannels;
    }

    private void setPlaybackRate(float rate) {
        this.mRate = rate;
    }
}
