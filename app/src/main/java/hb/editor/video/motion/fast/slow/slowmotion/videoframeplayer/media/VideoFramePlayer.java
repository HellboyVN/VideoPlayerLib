package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media;

import android.content.Context;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.util.Log;
import android.view.Surface;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media.StreamDecoder.FrameAvailableListener;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media.StreamDecoder.OutputStreamChangeListener;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.MediaTimeClock;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VideoFramePlayer implements FrameAvailableListener, OutputStreamChangeListener {
    private static final String LOG_TAG = "VideoFramePlayer";
    public static final int MSG_DECODE = 4;
    public static final int MSG_ENABLE_AUDIO = 9;
    public static final int MSG_PAUSE = 7;
    public static final int MSG_PLAYBACK_SPEED = 8;
    public static final int MSG_PREPARE = 0;
    public static final int MSG_RELEASE = 3;
    public static final int MSG_RESUME = 11;
    public static final int MSG_SEEK = 6;
    public static final int MSG_SEEKING_DECODE = 14;
    public static final int MSG_SEEK_END = 13;
    public static final int MSG_SEEK_START = 12;
    public static final int MSG_SET_SURFACE = 5;
    public static final int MSG_START = 1;
    public static final int MSG_STOP = 2;
    public static final int STATE_IDLE = 1;
    public static final int STATE_PAUSE = 4;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PREPARED = 0;
    public static final int STATE_READY = 2;
    public static final int STATE_SEEKING = 5;
    public static final int STATE_STOPED = 6;
    public static final int STATE_UNINIT = -1;
    private MediaTimeClock clock = new MediaTimeClock();
    private StreamDecoder mAudioDecoder;
    private MediaExtractor mAudioExtractor;
    private FullAudioPlayer mAudioRenderer;
    private boolean mAudioSeen = false;
    private boolean mAudioStarted;
    private long mAudioSyncTimeUs = 0;
    private Handler mCallbackHandler;
    private long mCurrentPositionUs;
    private Callback mDecoderCallbacks = new Callback() {
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    VideoFramePlayer.this.PrepareImpl();
                    return true;
                case 1:
                    VideoFramePlayer.this.StartImpl();
                    return true;
                case 2:
                    VideoFramePlayer.this.StopImpl();
                    return true;
                case 3:
                    VideoFramePlayer.this.ReleaseImpl();
                    return true;
                case 4:
                    VideoFramePlayer.this.processDecoders();
                    return true;
                case 5:
                    VideoFramePlayer.this.setSurfaceImpl((Surface) msg.obj);
                    return true;
                case 6:
                    VideoFramePlayer.this.SeekImpl((Long) msg.obj);
                    return true;
                case 7:
                    VideoFramePlayer.this.PauseImpl();
                    return true;
                case 8:
                    VideoFramePlayer.this.PlaybackSpeedImpl();
                    break;
                case 9:
                    VideoFramePlayer.this.EnableAudioImpl(((Boolean) msg.obj).booleanValue());
                    return true;
                case 11:
                    VideoFramePlayer.this.ResumeImpl();
                    return true;
                case 12:
                    if (VideoFramePlayer.this.mAudioRenderer != null) {
                        VideoFramePlayer.this.mAudioRenderer.pause();
                        VideoFramePlayer.this.mAudioRenderer.flush();
                    }
                    VideoFramePlayer.this.mDecoderHandler.removeMessages(4);
                    return true;
                case 13:
                    VideoFramePlayer.this.mSeekPending = true;
                    VideoFramePlayer.this.mVideoSeen = false;
                    VideoFramePlayer.this.mAudioSeen = false;
                    VideoFramePlayer.this.mFirstFrameSeen = false;
                    VideoFramePlayer.this.mSyncNeeded = true;
                    VideoFramePlayer.this.mDecoderHandler.sendEmptyMessage(4);
                    return true;
                case 14:
                    VideoFramePlayer.this.RenderDuringSeek();
                    return true;
            }
            return false;
        }
    };
    private Handler mDecoderHandler;
    private HandlerThread mDecoderThread = new HandlerThread("Decoder") {
        public void run() {
            Process.setThreadPriority(-19);
            super.run();
        }
    };
    private long mDuration;
    private boolean mFirstFrameSeen = false;
    private boolean mHasAudio = false;
    private boolean mHasVideo = false;
    private Object mLock = new Object();
    private MediaStreamReader mMediaSourceReader;
    private long mNextAudioFrameTimeMs = 10;
    private long mNextVideoFrameTimeMs = 10;
    private float mPlaybackRate = 1.0f;
    private int mPlaybackSpeed = 0;
    private PlaybackStateListener mPlaybackStateListener = null;
    private int mRotation;
    private boolean mSeekPending = true;
    private long mSeekPositionUs = 0;
    private String mSourceUrl;
    private boolean mStartAsPaused = false;
    private int mState;
    private List<StreamDecoder> mStreamDecoders = new ArrayList();
    private boolean mSyncNeeded = true;
    private StreamDecoder mVideoDecoder;
    private MediaExtractor mVideoExtractor;
    private int mVideoHeight;
    private boolean mVideoSeen = false;
    private VideoSizeChangeListener mVideoSizeChangeListener = null;
    private long mVideoSyncTimeUs = 0;
    private int mVideoWidth;
    private WakeLock mWakeLock;
    boolean hasCropRightLeft;
    boolean hasCropBottomTop;
    int cropRight;
    int cropLeft;
    int cropBottom;
    int cropTop;

    public interface PlaybackStateListener {
        void onPlaybackStart();

        void onPlaybackStop();
    }

    public interface VideoSizeChangeListener {
        void onCropRectangleChange(int i, int i2, int i3, int i4, int i5, int i6);

        void onVideoSizeChange(int i, int i2, int i3);
    }

    public VideoFramePlayer(Context context, Handler handler) {
        this.mCallbackHandler = handler;
        this.mDecoderThread.start();
        this.mDecoderHandler = new Handler(this.mDecoderThread.getLooper(), this.mDecoderCallbacks);
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mState = -1;
        this.mRotation = 0;
        this.mAudioStarted = false;
    }

    public void Start(long startPos, float rate) {
        this.mPlaybackRate = rate;
        this.mCurrentPositionUs = 1000 * startPos;
        this.mDecoderHandler.sendEmptyMessage(1);
    }

    public void Pause() {
        this.mDecoderHandler.sendEmptyMessage(7);
    }

    public void Resume() {
        this.mDecoderHandler.sendEmptyMessage(11);
    }

    public void SeekStart() {
        this.mDecoderHandler.sendEmptyMessage(12);
    }

    public void Seek(long position) {
        this.mSeekPositionUs = 1000 * position;
        this.mDecoderHandler.obtainMessage(6, Long.valueOf(this.mSeekPositionUs)).sendToTarget();
    }

    public void SeekEnd() {
        this.mDecoderHandler.sendEmptyMessage(13);
    }

    public void Stop() {
        this.mDecoderHandler.sendEmptyMessage(2);
    }

    public void Release() {
        this.mDecoderHandler.sendEmptyMessage(3);
    }

    public void setSurface(Surface surface) {
        this.mDecoderHandler.obtainMessage(5, surface).sendToTarget();
    }

    private void Decode() {
        this.mDecoderHandler.obtainMessage(4).sendToTarget();
    }

    public void Prepare(String filename) {
        this.mSourceUrl = filename;
        this.mDecoderHandler.obtainMessage(0).sendToTarget();
    }

    public void EnableAudio(boolean enable) {
        this.mDecoderHandler.obtainMessage(9, new Boolean(enable)).sendToTarget();
    }

    public void setPlaybackSpeed(boolean up) {
        boolean send = false;
        if (up && this.mPlaybackRate < 3.0f) {
            this.mPlaybackRate += 0.125f;
            send = true;
        }
        if (!up && this.mPlaybackRate > 0.125f) {
            this.mPlaybackRate -= 0.125f;
            send = true;
        }
        if (send) {
            this.mDecoderHandler.obtainMessage(8).sendToTarget();
        }
    }

    private void stateChange(int to) {
        synchronized (this.mLock) {
            int from = this.mState;
            this.mState = to;
        }
    }

    private void extractVideoInfo() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(Uri.parse(this.mSourceUrl).getPath());
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "MediaMetadataRetriever error:" + e.toString());
        }
        String width = retriever.extractMetadata(18);
        String height = retriever.extractMetadata(19);
        String orint = retriever.extractMetadata(24);
        String duration = retriever.extractMetadata(9);
        retriever.release();
        if (width != null) {
            try {
                this.mVideoWidth = Integer.parseInt(width);
            } catch (NumberFormatException e2) {
                Log.e(LOG_TAG, "NumberFormatException error:" + e2.toString());
            }
        }
        if (height != null) {
            this.mVideoHeight = Integer.parseInt(height);
        }
        if (orint != null) {
            this.mRotation = Integer.parseInt(orint);
        }
        if (duration != null) {
            this.mDuration = Long.parseLong(duration);
        }
        if (!(this.mVideoWidth * this.mVideoHeight == 0 || this.mVideoSizeChangeListener == null)) {
            this.mCallbackHandler.post(new Runnable() {
                public void run() {
                    VideoFramePlayer.this.mVideoSizeChangeListener.onVideoSizeChange(VideoFramePlayer.this.mVideoWidth, VideoFramePlayer.this.mVideoHeight, VideoFramePlayer.this.mRotation);
                }
            });
        }
        Log.d(LOG_TAG, "extractVideoInfo VideoSize(" + this.mVideoWidth + "x" + this.mVideoHeight + ")");
    }

    private void PrepareImpl() {
        extractVideoInfo();
        this.mVideoExtractor = new MediaExtractor();
        try {
            this.mVideoExtractor.setDataSource(this.mSourceUrl);
        } catch (IOException e) {
            Log.e(LOG_TAG, "VideoExtractor error: " + e.toString());
        }
        this.mVideoDecoder = StreamDecoder.CreateVideoDecoder(this.mSourceUrl, this.mVideoExtractor, null);
        if (this.mVideoDecoder != null) {
            this.mVideoDecoder.setOutputStreamChangeListener(this);
            this.mVideoDecoder.setFrameAvailableListener(this);
            this.mStreamDecoders.add(this.mVideoDecoder);
            this.mHasVideo = true;
        }
        this.mAudioExtractor = new MediaExtractor();
        try {
            this.mAudioExtractor.setDataSource(this.mSourceUrl);
        } catch (IOException e2) {
            Log.e(LOG_TAG, "AudioExtractor error: " + e2.toString());
        }
        this.mAudioDecoder = StreamDecoder.CreateAudioDecoder(this.mSourceUrl, this.mAudioExtractor, null);
        if (this.mAudioDecoder != null) {
            this.mAudioDecoder.setOutputStreamChangeListener(this);
            this.mAudioDecoder.setFrameAvailableListener(this);
            this.mStreamDecoders.add(this.mAudioDecoder);
            this.mHasAudio = true;
        }
        stateChange(0);
    }

    private void processDecoders() {
        int size = this.mStreamDecoders.size();
        for (int i = 0; i < size; i++) {
            ((StreamDecoder) this.mStreamDecoders.get(i)).processBuffers();
        }
        if (this.mState == 3) {
            this.mDecoderHandler.sendEmptyMessageDelayed(4, this.mNextVideoFrameTimeMs < this.mNextAudioFrameTimeMs ? this.mNextVideoFrameTimeMs : this.mNextAudioFrameTimeMs);
        } else if (!this.mFirstFrameSeen) {
            this.mDecoderHandler.sendEmptyMessage(4);
        }
    }

    private void getVideoSize(MediaFormat format) {
        String KEY_CROP_LEFT = "crop-left";
        String KEY_CROP_RIGHT = "crop-right";
        String KEY_CROP_BOTTOM = "crop-bottom";
        String KEY_CROP_TOP = "crop-top";
        final int width = format.getInteger("width");
        final int height = format.getInteger("height");
        if (format.containsKey("crop-right")) {
            if (format.containsKey("crop-left")) {
                hasCropRightLeft = true;
                if (format.containsKey("crop-bottom")) {
                    if (format.containsKey("crop-top")) {
                        hasCropBottomTop = true;
                        if (hasCropRightLeft) {
                            cropRight = width;
                            cropLeft = 1;
                        } else {
                            cropRight = format.getInteger("crop-right");
                            cropLeft = format.getInteger("crop-left");
                        }
                        if (hasCropBottomTop) {
                            cropBottom = height;
                            cropTop = 1;
                        } else {
                            cropBottom = format.getInteger("crop-bottom");
                            cropTop = format.getInteger("crop-top");
                        }
                        this.mVideoWidth = (cropRight - cropLeft) + 1;
                        this.mVideoHeight = (cropBottom - cropTop) + 1;
                        if (!(this.mVideoSizeChangeListener == null || (this.mVideoWidth == width && this.mVideoHeight == height))) {
                            this.mCallbackHandler.post(new Runnable() {
                                public void run() {
                                    if (hasCropBottomTop && hasCropRightLeft) {
                                        VideoFramePlayer.this.mVideoSizeChangeListener.onCropRectangleChange(cropLeft, cropTop, cropBottom, cropRight, width, height);
                                    }
                                    VideoFramePlayer.this.mVideoSizeChangeListener.onVideoSizeChange(VideoFramePlayer.this.mVideoWidth, VideoFramePlayer.this.mVideoHeight, VideoFramePlayer.this.mRotation);
                                }
                            });
                        }
                        Log.d(LOG_TAG, "Video size changed (width=" + this.mVideoWidth + " height=" + this.mVideoHeight + ")");
                    }
                }
                hasCropBottomTop = false;
                if (hasCropRightLeft) {
                    cropRight = width;
                    cropLeft = 1;
                } else {
                    cropRight = format.getInteger("crop-right");
                    cropLeft = format.getInteger("crop-left");
                }
                if (hasCropBottomTop) {
                    cropBottom = height;
                    cropTop = 1;
                } else {
                    cropBottom = format.getInteger("crop-bottom");
                    cropTop = format.getInteger("crop-top");
                }
                this.mVideoWidth = (cropRight - cropLeft) + 1;
                this.mVideoHeight = (cropBottom - cropTop) + 1;
                this.mCallbackHandler.post(new Runnable() {
                    public void run() {
                        if (hasCropBottomTop && hasCropRightLeft) {
                            VideoFramePlayer.this.mVideoSizeChangeListener.onCropRectangleChange(cropLeft, cropTop, cropBottom, cropRight, width, height);
                        }
                        VideoFramePlayer.this.mVideoSizeChangeListener.onVideoSizeChange(VideoFramePlayer.this.mVideoWidth, VideoFramePlayer.this.mVideoHeight, VideoFramePlayer.this.mRotation);
                    }
                });
                Log.d(LOG_TAG, "Video size changed (width=" + this.mVideoWidth + " height=" + this.mVideoHeight + ")");
            }
        }
        hasCropRightLeft = false;
        if (format.containsKey("crop-bottom")) {
            if (format.containsKey("crop-top")) {
                hasCropBottomTop = true;
                if (hasCropRightLeft) {
                    cropRight = format.getInteger("crop-right");
                    cropLeft = format.getInteger("crop-left");
                } else {
                    cropRight = width;
                    cropLeft = 1;
                }
                if (hasCropBottomTop) {
                    cropBottom = format.getInteger("crop-bottom");
                    cropTop = format.getInteger("crop-top");
                } else {
                    cropBottom = height;
                    cropTop = 1;
                }
                this.mVideoWidth = (cropRight - cropLeft) + 1;
                this.mVideoHeight = (cropBottom - cropTop) + 1;
                this.mCallbackHandler.post(new Runnable() {
                    public void run() {
                        if (hasCropBottomTop && hasCropRightLeft) {
                            VideoFramePlayer.this.mVideoSizeChangeListener.onCropRectangleChange(cropLeft, cropTop, cropBottom, cropRight, width, height);
                        }
                        VideoFramePlayer.this.mVideoSizeChangeListener.onVideoSizeChange(VideoFramePlayer.this.mVideoWidth, VideoFramePlayer.this.mVideoHeight, VideoFramePlayer.this.mRotation);
                    }
                });
                Log.d(LOG_TAG, "Video size changed (width=" + this.mVideoWidth + " height=" + this.mVideoHeight + ")");
            }
        }
        hasCropBottomTop = false;
        if (hasCropRightLeft) {
            cropRight = width;
            cropLeft = 1;
        } else {
            cropRight = format.getInteger("crop-right");
            cropLeft = format.getInteger("crop-left");
        }
        if (hasCropBottomTop) {
            cropBottom = height;
            cropTop = 1;
        } else {
            cropBottom = format.getInteger("crop-bottom");
            cropTop = format.getInteger("crop-top");
        }
        this.mVideoWidth = (cropRight - cropLeft) + 1;
        this.mVideoHeight = (cropBottom - cropTop) + 1;
        this.mCallbackHandler.post(new Runnable() {
            public void run() {
                if (hasCropBottomTop && hasCropRightLeft) {
                    VideoFramePlayer.this.mVideoSizeChangeListener.onCropRectangleChange(cropLeft, cropTop, cropBottom, cropRight, width, height);
                }
                VideoFramePlayer.this.mVideoSizeChangeListener.onVideoSizeChange(VideoFramePlayer.this.mVideoWidth, VideoFramePlayer.this.mVideoHeight, VideoFramePlayer.this.mRotation);
            }
        });
        Log.d(LOG_TAG, "Video size changed (width=" + this.mVideoWidth + " height=" + this.mVideoHeight + ")");
    }

    private void writeAudio(ByteBuffer buffer, int size) {
        buffer.clear();
        buffer.position(0);
        if (this.mAudioRenderer != null) {
            this.mAudioRenderer.write(buffer, size);
        }
    }

    int firstFrameSeenForAllDecoder(StreamDecoder decoder, BufferInfo info, ByteBuffer buffer) {
        if (this.mSeekPending) {
            if (info.presentationTimeUs < this.mSeekPositionUs) {
                return 2;
            }
            if (decoder.isAudioTrack()) {
                this.mAudioSyncTimeUs = info.presentationTimeUs;
                this.mAudioSeen = true;
            } else if (decoder.isVideoTrack()) {
                this.mVideoSyncTimeUs = info.presentationTimeUs;
                this.mVideoSeen = true;
            }
        } else if (decoder.isAudioTrack()) {
            this.mAudioSeen = true;
        } else if (decoder.isVideoTrack()) {
            this.mVideoSeen = true;
        }
        if (this.mHasAudio && this.mHasVideo) {
            if (this.mAudioSeen && this.mVideoSeen) {
                if (!this.mSyncNeeded) {
                    this.mFirstFrameSeen = true;
                } else if (Math.abs(this.mAudioSyncTimeUs - this.mVideoSyncTimeUs) > 30) {
                    long maxSyncTime = this.mAudioSyncTimeUs > this.mVideoSyncTimeUs ? this.mAudioSyncTimeUs : this.mVideoSyncTimeUs;
                    this.mSeekPositionUs = maxSyncTime;
                    if (maxSyncTime == this.mAudioSyncTimeUs) {
                        this.mVideoSeen = false;
                    } else {
                        this.mAudioSeen = false;
                    }
                    this.mSyncNeeded = false;
                } else {
                    this.mFirstFrameSeen = true;
                }
            }
        } else if (this.mAudioSeen || this.mVideoSeen) {
            this.mFirstFrameSeen = true;
        }
        return 4;
    }

    private void SeekImpl(Long position) {
        this.mMediaSourceReader.flush();
        for (int i = 0; i < this.mStreamDecoders.size(); i++) {
            ((StreamDecoder) this.mStreamDecoders.get(i)).Flush();
        }
        this.clock.stop();
        this.mCurrentPositionUs = position.longValue();
        this.mAudioExtractor.seekTo(position.longValue(), 2);
        this.mVideoExtractor.seekTo(position.longValue(), 2);
        this.mDecoderHandler.removeMessages(4);
    }

    private void StartImpl() {
        int state;
        synchronized (this.mLock) {
            state = this.mState;
        }
        if (state == 0) {
            for (int i = 0; i < this.mStreamDecoders.size(); i++) {
                ((StreamDecoder) this.mStreamDecoders.get(i)).EnableDecoder();
            }
        }
        int audioBufferCount = 1;
        int videoBufferCount = 1;
        if (this.mHasVideo) {
            videoBufferCount = 1 + this.mVideoDecoder.getInputBufferCount();
        }
        if (this.mHasAudio) {
            audioBufferCount = 1 + this.mAudioDecoder.getInputBufferCount();
        }
        this.mMediaSourceReader = new MediaStreamReader(videoBufferCount, audioBufferCount);
        this.mMediaSourceReader.setVideoExtractor(this.mVideoExtractor);
        this.mMediaSourceReader.setAudioExtractor(this.mAudioExtractor);
        this.mMediaSourceReader.start();
        if (this.mHasVideo) {
            this.mVideoDecoder.setMediaSourceReader(this.mMediaSourceReader);
        }
        if (this.mHasAudio) {
            this.mAudioDecoder.setMediaSourceReader(this.mMediaSourceReader);
        }
        this.mAudioExtractor.seekTo(this.mCurrentPositionUs, 0);
        this.mVideoExtractor.seekTo(this.mCurrentPositionUs, 0);
        stateChange(3);
        this.mDecoderHandler.sendEmptyMessage(4);
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
    }

    private void ResumeImpl() {
        stateChange(3);
        if (this.mAudioRenderer != null) {
            this.mAudioRenderer.play();
        }
        this.clock.start();
        this.mDecoderHandler.sendEmptyMessage(4);
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
    }

    private void PauseImpl() {
        stateChange(4);
        this.clock.stop();
        if (this.mAudioRenderer != null) {
            this.mAudioRenderer.pause();
        }
        this.mDecoderHandler.removeMessages(4);
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    private void StopImpl() {
        this.mDecoderHandler.removeMessages(4);
        for (int i = 0; i < this.mStreamDecoders.size(); i++) {
            ((StreamDecoder) this.mStreamDecoders.get(i)).DisableDecoder();
        }
        this.clock.stop();
        stateChange(6);
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    public void ReleaseImpl() {
        this.mDecoderHandler.removeCallbacksAndMessages(null);
        for (int i = 0; i < this.mStreamDecoders.size(); i++) {
            ((StreamDecoder) this.mStreamDecoders.get(i)).ReleaseDecoder();
        }
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mMediaSourceReader != null) {
            this.mMediaSourceReader.interrupt();
            this.mMediaSourceReader = null;
        }
        if (this.mAudioRenderer != null) {
            this.mAudioRenderer.stop();
            this.mAudioRenderer.flush();
            this.mAudioRenderer.release();
            this.mAudioRenderer = null;
        }
        this.mDecoderHandler.getLooper().quit();
        stateChange(-1);
        synchronized (this) {
            notifyAll();
        }
    }

    private void setSurfaceImpl(Surface surface) {
        StreamDecoder decoder = this.mVideoDecoder;
        if (decoder != null) {
            decoder.Configure(surface);
        }
    }

    private void PlaybackSpeedImpl() {
        this.clock.setRate(this.mPlaybackRate);
        if (this.mAudioRenderer != null) {
            this.mAudioRenderer.setPlaybackRate(this.mPlaybackRate);
        }
    }

    public float getPlaybackRate() {
        return this.mPlaybackRate;
    }

    private void EnableAudioImpl(boolean enable) {
        for (StreamDecoder decoder : this.mStreamDecoders) {
            if (decoder.isAudioTrack()) {
                StreamDecoder audioDecoder = decoder;
                return;
            }
        }
    }

    void RenderDuringSeek() {
    }

    public long getDuration() {
        return this.mDuration;
    }

    public boolean isPlaying() {
        return this.mState == 3;
    }

    public long getCurrentPosition() {
        return this.mCurrentPositionUs / 1000;
    }

    public long getBufferPercentage() {
        if (this.mVideoExtractor != null) {
            return (this.mVideoExtractor.getCachedDuration() * 100) / this.mDuration;
        }
        return 0;
    }

    public int getVideoWidth() {
        return this.mVideoWidth;
    }

    public int getVideoHeight() {
        return this.mVideoHeight;
    }

    public int getRotation() {
        return this.mRotation;
    }

    int renderVideoFrame(BufferInfo info, ByteBuffer buffer) {
        long currentTime = this.clock.getMediaTime();
        if (this.mSeekPending) {
            this.clock.setMediaTime(info.presentationTimeUs);
            this.mSeekPending = false;
            currentTime = info.presentationTimeUs;
        }
        this.mNextVideoFrameTimeMs = 10;
        long earlyUs = info.presentationTimeUs - currentTime;
        if (earlyUs <= -70000) {
            Log.d(LOG_TAG, "Video late by (ms) " + ((-earlyUs) / 1000) + ", clock(ms)=" + (currentTime / 1000));
            this.clock.setMediaTime(info.presentationTimeUs);
            earlyUs = 0;
        }
        if (earlyUs > 11000) {
            this.mNextVideoFrameTimeMs = (earlyUs - 10000) / 1000;
            return 4;
        } else if (earlyUs <= -40000) {
            return 2;
        } else {
            if (this.mCurrentPositionUs < info.presentationTimeUs) {
                this.mCurrentPositionUs = info.presentationTimeUs;
            }
            return 1;
        }
    }

    int renderAudioFrame(BufferInfo info, ByteBuffer buffer) {
        long earlyUs = info.presentationTimeUs - this.clock.getMediaTime();
        this.mNextAudioFrameTimeMs = 10;
        if (earlyUs > 30000) {
            this.mNextAudioFrameTimeMs = (earlyUs - 10000) / 1000;
            return 4;
        }
        writeAudio(buffer, info.size);
        return 1;
    }

    public void setVideoSizeChangeListener(VideoSizeChangeListener listener) {
        this.mVideoSizeChangeListener = listener;
    }

    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.mPlaybackStateListener = listener;
    }

    public int onFrameAvailable(StreamDecoder decoder, BufferInfo info, ByteBuffer buffer) {
        int result;
        if (!this.mFirstFrameSeen) {
            result = firstFrameSeenForAllDecoder(decoder, info, buffer);
            if (!this.mFirstFrameSeen) {
                return result;
            }
            if (this.mPlaybackStateListener != null) {
                this.mCallbackHandler.post(new Runnable() {
                    public void run() {
                        VideoFramePlayer.this.mPlaybackStateListener.onPlaybackStart();
                    }
                });
            }
            if (this.mAudioRenderer != null) {
                this.mAudioRenderer.play();
            }
            this.clock.start();
            this.clock.setRate(this.mPlaybackRate);
            this.clock.setMediaTime(info.presentationTimeUs);
            for (StreamDecoder dec : this.mStreamDecoders) {
                if (dec != decoder && dec.isVideoTrack()) {
                    dec.renderFrame(true);
                }
            }
        }
        result = 4;
        if (decoder.isVideoTrack()) {
            result = renderVideoFrame(info, buffer);
        } else if (decoder.isAudioTrack() && this.mState != 4) {
            result = renderAudioFrame(info, buffer);
        }
        return result;
    }

    public void onOutputStreamFormatChange(StreamDecoder decoder) {
        if (decoder.isVideoTrack()) {
            getVideoSize(decoder.getMediaFormat());
        } else if (decoder.isAudioTrack()) {
            if (this.mAudioStarted) {
                this.mAudioRenderer.flush();
                this.mAudioRenderer.stop();
                this.mAudioRenderer.release();
                this.mAudioRenderer = null;
                this.mAudioStarted = false;
            }
            if (!this.mAudioStarted) {
                this.mAudioStarted = true;
                this.mAudioRenderer = new FullAudioPlayer(decoder.getMediaFormat().getInteger("sample-rate"), decoder.getMediaFormat().getInteger("channel-count"));
                this.mAudioRenderer.setStartRate(this.mPlaybackRate);
            }
        }
    }

    public void onOutputStreamEnded(StreamDecoder decoder) {
        if (this.mPlaybackStateListener != null) {
            boolean done = true;
            for (StreamDecoder d : this.mStreamDecoders) {
                if (!d.isOutputDone()) {
                    done = false;
                    break;
                }
            }
            if (done && this.mPlaybackStateListener != null) {
                this.mCallbackHandler.post(new Runnable() {
                    public void run() {
                        VideoFramePlayer.this.mPlaybackStateListener.onPlaybackStop();
                    }
                });
            }
        }
    }
}
