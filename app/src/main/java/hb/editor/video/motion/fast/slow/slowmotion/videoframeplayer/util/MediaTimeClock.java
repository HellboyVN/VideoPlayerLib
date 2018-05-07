package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.os.SystemClock;

public class MediaTimeClock {
    private static final String LOG_TAG = "MediaTimeClock";
    private static final int STARTED = 1;
    private static final int STOPPED = 2;
    private long mMediaTime = 0;
    private float mRate = 1.0f;
    private boolean mRateIsOne = true;
    private long mStartTime = Long.MAX_VALUE;
    private int mState = 2;

    public void start() {
        if (this.mState != 1) {
            if (this.mState == 2) {
                this.mStartTime = SystemClock.elapsedRealtime() * 1000;
                this.mState = 1;
                return;
            }
            throwIllegalState("start");
        }
    }

    public void stop() {
        if (this.mState != 2) {
            if (this.mState == 1) {
                this.mMediaTime = getMediaTime();
                this.mState = 2;
                return;
            }
            throwIllegalState("stop");
        }
    }

    public long getMediaTime() {
        long now = SystemClock.elapsedRealtime() * 1000;
        if (this.mRateIsOne) {
            return (now - this.mStartTime) + this.mMediaTime;
        }
        return ((long) (((double) (now - this.mStartTime)) * ((double) this.mRate))) + this.mMediaTime;
    }

    public void setMediaTime(long mediaTime) {
        this.mMediaTime = mediaTime;
        this.mStartTime = SystemClock.elapsedRealtime() * 1000;
    }

    private void throwIllegalState(String from) {
        throw new IllegalStateException(from + ":state=" + this.mState);
    }

    public void setRate(float rate) {
        this.mMediaTime = getMediaTime();
        this.mStartTime = SystemClock.elapsedRealtime() * 1000;
        if (Math.abs(1.0f - rate) < 0.1f) {
            this.mRateIsOne = true;
            this.mRate = 1.0f;
            return;
        }
        this.mRateIsOne = false;
        this.mRate = rate;
    }
}
