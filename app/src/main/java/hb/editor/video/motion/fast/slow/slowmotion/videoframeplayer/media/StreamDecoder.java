package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.view.Surface;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media.MediaStreamReader.QueueItem;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ReportError;
import java.io.IOException;
import java.nio.ByteBuffer;

public class StreamDecoder {
    private static final String LOG_TAG = "StreamDecoder";
    public static final int TYPE_AUDIO = 2;
    public static final int TYPE_VIDEO = 1;
    private boolean mEnabled;
    private FrameAvailableListener mFrameAvailableListener;
    private int mFrameCount = 0;
    private int mFrameRate = 0;
    private final BufferInfo mInfo = new BufferInfo();
    private ByteBuffer[] mInputBuffers;
    private boolean mInputEOS = false;
    private int mInputIndex = -1;
    private long mLastFrameTimestamp = 0;
    private MediaCodec mMediaCodec;
    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;
    private ByteBuffer[] mOutputBuffers;
    private boolean mOutputEOS = false;
    private int mOutputIndex = -1;
    private OutputStreamChangeListener mOutputStreamChangeListener;
    private boolean mSeekPending = false;
    private MediaStreamReader mSourceReader;
    private int mTimeAdjustmentUs = 0;
    private Integer mTrackIndex;
    private int mType;

    public interface FrameAvailableListener {
        public static final int DELAYED = 4;
        public static final int DROP = 2;
        public static final int RENDER = 1;

        int onFrameAvailable(StreamDecoder streamDecoder, BufferInfo bufferInfo, ByteBuffer byteBuffer);
    }

    public interface OutputStreamChangeListener {
        void onOutputStreamEnded(StreamDecoder streamDecoder);

        void onOutputStreamFormatChange(StreamDecoder streamDecoder);
    }

    private static String listCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        String name = null;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                if (name == null) {
                    name = codecInfo.getName();
                }
                String[] types = codecInfo.getSupportedTypes();
                int j = 0;
                while (j < types.length) {
                    j = types[j].equalsIgnoreCase(mimeType) ? j + 1 : j + 1;
                }
            }
        }
        return name;
    }

    private static String getCodecByMime(String mimeType) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                String[] types = codecInfo.getSupportedTypes();
                for (String equalsIgnoreCase : types) {
                    if (equalsIgnoreCase.equalsIgnoreCase(mimeType)) {
                        return codecInfo.getName();
                    }
                }
                continue;
            }
        }
        return null;
    }

    private static StreamDecoder CreateDecoderByMime(String mime, MediaExtractor extractor, MediaStreamReader reader) {
        StreamDecoder decoder = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mimeType = format.getString("mime");
            if (mimeType.contains(mime)) {
                MediaCodec codec = null;
                String name = getCodecByMime(mimeType);
                if (name != null) {
                    try {
                        ReportError.d(LOG_TAG, "Created decoder instance:" + name);
                        codec = MediaCodec.createByCodecName(name);
                    } catch (IOException e) {
                        ReportError.e(LOG_TAG, "Mediacodec io exception");
                    } catch (RuntimeException e2) {
                        ReportError.e(LOG_TAG, "Mediacodec runtime exception");
                    }
                    if (codec != null) {
                        decoder = new StreamDecoder(codec, extractor, format, Integer.valueOf(i), reader);
                    }
                } else {
                    ReportError.e(LOG_TAG, "No suitable H/W decoder found!");
                }
            }
            extractor.unselectTrack(i);
        }
        return decoder;
    }

    private static StreamDecoder CreateDecoder(String mimeSubstr, String sourceUrl, MediaExtractor extractor, MediaStreamReader reader) {
        if (extractor == null) {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(sourceUrl);
            } catch (IOException e) {
                return null;
            }
        }
        return CreateDecoderByMime(mimeSubstr, extractor, reader);
    }

    public static StreamDecoder CreateVideoDecoder(String sourceUrl, MediaExtractor extractor, MediaStreamReader reader) {
        StreamDecoder decoder = CreateDecoder("video/", sourceUrl, extractor, reader);
        if (decoder != null) {
            decoder.setDecoderType(1);
        }
        return decoder;
    }

    public static StreamDecoder CreateAudioDecoder(String sourceUrl, MediaExtractor extractor, MediaStreamReader reader) {
        StreamDecoder decoder = CreateDecoder("audio/", sourceUrl, extractor, reader);
        if (decoder != null) {
            decoder.setDecoderType(2);
            decoder.Configure(null);
        }
        return decoder;
    }

    StreamDecoder(MediaCodec codec, MediaExtractor extractor, MediaFormat format, Integer track, MediaStreamReader reader) {
        this.mMediaCodec = codec;
        this.mMediaExtractor = extractor;
        this.mMediaFormat = format;
        this.mTrackIndex = track;
        this.mEnabled = false;
        this.mLastFrameTimestamp = 0;
        this.mSourceReader = reader;
    }

    public void EnableDecoder() {
        if (!this.mEnabled) {
            if (this.mMediaFormat.containsKey("frame-rate")) {
                this.mFrameRate = this.mMediaFormat.getInteger("frame-rate");
            }
            this.mMediaExtractor.selectTrack(this.mTrackIndex.intValue());
            this.mMediaCodec.start();
            this.mInputBuffers = this.mMediaCodec.getInputBuffers();
            this.mOutputBuffers = this.mMediaCodec.getOutputBuffers();
            this.mEnabled = true;
        }
    }

    public void DisableDecoder() {
        if (this.mEnabled) {
            this.mOutputBuffers = null;
            this.mInputBuffers = null;
            this.mMediaCodec.stop();
            this.mMediaExtractor.unselectTrack(this.mTrackIndex.intValue());
            this.mEnabled = false;
        }
    }

    public void setMediaSourceReader(MediaStreamReader reader) {
        this.mSourceReader = reader;
    }

    public void setFrameAvailableListener(FrameAvailableListener listener) {
        this.mFrameAvailableListener = listener;
    }

    public void setOutputStreamChangeListener(OutputStreamChangeListener listener) {
        this.mOutputStreamChangeListener = listener;
    }

    public int getInputBufferCount() {
        return this.mInputBuffers.length;
    }

    public void ReleaseDecoder() {
        if (this.mEnabled) {
            this.mMediaCodec.stop();
        }
        this.mEnabled = false;
        this.mMediaCodec.release();
    }

    public void Configure(Surface surface) {
        this.mMediaCodec.configure(this.mMediaFormat, surface, null, 0);
    }

    private void setDecoderType(int type) {
        this.mType = type;
        if (type == 1) {
            this.mMediaCodec.setVideoScalingMode(2);
        }
    }

    public int GetType() {
        return this.mType;
    }

    public void processBuffers() {
        do {
        } while (processOutputBuffer());
        do {
        } while (processInputBuffer());
    }

    public boolean processInputBuffer() {
        if (this.mInputEOS) {
            return false;
        }
        QueueItem item = this.mSourceReader.getQueueItem(this.mType);
        if (item != null) {
            try {
                this.mMediaCodec.queueInputBuffer(item.index, 0, item.size, item.sampleTimeUs, item.flags);
            } catch (IllegalStateException e) {
                ReportError.e(LOG_TAG, "Mediacodec.queueInputBuffer IllegalStateException");
            } catch (RuntimeException e2) {
                ReportError.e(LOG_TAG, "Mediacodec.queueInputBuffer RuntimeException");
            }
            if (item.flags != 4) {
                return true;
            }
            this.mInputEOS = true;
            return false;
        }
        if (this.mInputIndex < 0) {
            try {
                this.mInputIndex = this.mMediaCodec.dequeueInputBuffer(0);
            } catch (IllegalStateException e3) {
                ReportError.e(LOG_TAG, "Mediacodec.dequeueInputBuffer IllegalStateException");
            } catch (RuntimeException e4) {
                ReportError.e(LOG_TAG, "Mediacodec.dequeueInputBuffer RuntimeException");
            }
        }
        if (this.mInputIndex < 0) {
            return false;
        }
        if (this.mInputIndex < 0) {
            return false;
        }
        if (this.mSourceReader.remainingCapacity() <= 0) {
            return false;
        }
        item = new QueueItem(this.mType);
        item.buffer = this.mInputBuffers[this.mInputIndex];
        item.index = this.mInputIndex;
        this.mSourceReader.queueWork(item);
        this.mInputIndex = -1;
        return true;
    }

    public boolean processOutputBuffer() {
        if (this.mOutputEOS) {
            return false;
        }
        if (this.mOutputIndex < 0) {
            this.mOutputIndex = this.mMediaCodec.dequeueOutputBuffer(this.mInfo, 0);
            if (this.mType == 1 && this.mOutputIndex >= 0) {
                needFrameTimeAdjustment();
            }
        }
        if (this.mOutputIndex == -3) {
            this.mOutputBuffers = this.mMediaCodec.getOutputBuffers();
            return true;
        } else if (this.mOutputIndex == -2) {
            this.mMediaFormat = this.mMediaCodec.getOutputFormat();
            if (this.mType == 1 && this.mMediaFormat.containsKey("frame-rate")) {
                this.mFrameRate = this.mMediaFormat.getInteger("frame-rate");
            }
            if (this.mOutputStreamChangeListener != null) {
                this.mOutputStreamChangeListener.onOutputStreamFormatChange(this);
            }
            return true;
        } else if (this.mOutputIndex < 0) {
            return false;
        } else {
            if ((this.mInfo.flags & 4) == 4) {
                this.mOutputEOS = true;
                this.mInputEOS = true;
                if (this.mOutputStreamChangeListener == null) {
                    return false;
                }
                this.mOutputStreamChangeListener.onOutputStreamEnded(this);
                return false;
            } else if (this.mFrameAvailableListener == null) {
                return false;
            } else {
                boolean release;
                boolean render;
                int result = this.mFrameAvailableListener.onFrameAvailable(this, this.mInfo, this.mOutputBuffers[this.mOutputIndex]);
                if (result != 4) {
                    release = true;
                } else {
                    release = false;
                }
                if (result == 1) {
                    render = true;
                } else {
                    render = false;
                }
                if (!release) {
                    return false;
                }
                this.mMediaCodec.releaseOutputBuffer(this.mOutputIndex, render);
                this.mOutputIndex = -1;
                return true;
            }
        }
    }

    public long getPts() {
        return this.mInfo.presentationTimeUs;
    }

    void renderFrame(boolean render) {
        if (this.mOutputIndex >= 0) {
            this.mMediaCodec.releaseOutputBuffer(this.mOutputIndex, render);
            this.mOutputIndex = -1;
        }
    }

    void needFrameTimeAdjustment() {
        this.mFrameCount++;
        if (this.mFrameRate == 0 && this.mInfo.presentationTimeUs >= 1000000) {
            this.mFrameRate = this.mFrameCount;
            int i = (int) (1000000 / (this.mInfo.presentationTimeUs - this.mLastFrameTimestamp));
        }
        if (this.mTimeAdjustmentUs == 0 && this.mLastFrameTimestamp > this.mInfo.presentationTimeUs && this.mFrameRate > 0) {
            this.mTimeAdjustmentUs = 1000000 / this.mFrameRate;
        }
        if (this.mTimeAdjustmentUs > 0) {
            this.mLastFrameTimestamp += (long) this.mTimeAdjustmentUs;
            this.mInfo.presentationTimeUs = this.mLastFrameTimestamp;
            return;
        }
        this.mLastFrameTimestamp = this.mInfo.presentationTimeUs;
    }

    public boolean isInputDone() {
        return this.mInputEOS;
    }

    public boolean isOutputDone() {
        return this.mOutputEOS;
    }

    public long getSampleTime() {
        return this.mMediaExtractor.getSampleTime();
    }

    public long getCachedDuration() {
        return this.mMediaExtractor.getCachedDuration();
    }

    public void Flush() {
        if (this.mEnabled) {
            synchronized (this.mMediaCodec) {
                this.mMediaCodec.flush();
                this.mInputIndex = -1;
                this.mOutputIndex = -1;
                this.mLastFrameTimestamp = 0;
                this.mTimeAdjustmentUs = 0;
                this.mOutputEOS = false;
                this.mInputEOS = false;
            }
        }
    }

    public MediaFormat getMediaFormat() {
        return this.mMediaFormat;
    }

    public String getTypeString() {
        if (this.mType == 2) {
            return "Audio Decoder";
        }
        return "Video Decoder";
    }

    public boolean isAudioTrack() {
        return this.mType == 2;
    }

    public boolean isVideoTrack() {
        return this.mType == 1;
    }

    public int getTrackIndex() {
        return this.mTrackIndex.intValue();
    }

    public void setSeekPending(boolean pending) {
        this.mSeekPending = pending;
    }

    public boolean getSeekPending() {
        return this.mSeekPending;
    }
}
