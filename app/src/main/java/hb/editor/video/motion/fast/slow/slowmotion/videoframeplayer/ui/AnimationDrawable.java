package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.Property;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;

public class AnimationDrawable extends Drawable {
    private static final String LOG_TAG = "PlayPauseAnim";
    private static final Property<AnimationDrawable, Float> PROGRESS = new Property<AnimationDrawable, Float>(Float.class, "progress") {
        public Float get(AnimationDrawable d) {
            return Float.valueOf(d.getProgress());
        }

        public void set(AnimationDrawable d, Float value) {
            d.setProgress(value.floatValue());
        }
    };
    private final RectF mBounds = new RectF();
    private float mHeight;
    private boolean mIsPlay;
    private final Path mLeftPauseBar = new Path();
    private final Paint mPaintFill = new Paint();
    private final Paint mPaintStroke = new Paint();
    private final float mPauseBarDistance;
    private final float mPauseBarHeight;
    private final float mPauseBarPadding;
    private final float mPauseBarWidth;
    private final float mPlayBarHeight;
    private float mProgress;
    private final Path mRightPauseBar = new Path();
    private float mWidth;

    public AnimationDrawable(Context context) {
        Resources res = context.getResources();
        this.mPaintFill.setAntiAlias(true);
        this.mPaintFill.setStyle(Style.FILL);
        this.mPaintFill.setColor(-1);
        this.mPaintFill.setAlpha(170);
        this.mPaintStroke.setAntiAlias(true);
        this.mPaintStroke.setStyle(Style.STROKE);
        this.mPaintStroke.setColor(ViewCompat.MEASURED_STATE_MASK);
        this.mPaintStroke.setAlpha(170);
        this.mPauseBarWidth = (float) res.getDimensionPixelSize(R.dimen.pause_bar_width);
        this.mPauseBarHeight = (float) res.getDimensionPixelSize(R.dimen.pause_bar_height);
        this.mPauseBarDistance = (float) res.getDimensionPixelSize(R.dimen.pause_bar_distance);
        this.mPauseBarPadding = (float) res.getDimensionPixelSize(R.dimen.pause_bar_padding);
        this.mPlayBarHeight = this.mPauseBarHeight * 0.866f;
    }

    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.mBounds.set(bounds);
        this.mWidth = this.mBounds.width();
        this.mHeight = this.mBounds.height();
    }

    public void draw(Canvas canvas) {
        float startingRotation = 0.0f;
        this.mLeftPauseBar.rewind();
        this.mRightPauseBar.rewind();
        float barDist = lerp(this.mPauseBarDistance, 0.0f, this.mProgress);
        float barWidth = lerp(this.mPauseBarWidth, this.mPauseBarHeight / 2.0f, this.mProgress);
        float firstBarTopLeft = lerp(0.0f, barWidth, this.mProgress);
        float secondBarTopRight = lerp((2.0f * barWidth) + barDist, barWidth + barDist, this.mProgress);
        float newHeight = lerp(this.mPauseBarHeight, this.mPlayBarHeight, this.mProgress);
        if (firstBarTopLeft == secondBarTopRight) {
            this.mLeftPauseBar.moveTo(0.0f, 0.0f);
            this.mLeftPauseBar.lineTo((2.0f * barWidth) + barDist, 0.0f);
            this.mLeftPauseBar.lineTo(firstBarTopLeft, -newHeight);
            this.mLeftPauseBar.close();
        } else {
            this.mLeftPauseBar.moveTo(0.0f, 0.0f);
            this.mLeftPauseBar.lineTo(firstBarTopLeft, -newHeight);
            this.mLeftPauseBar.lineTo(barWidth, -newHeight);
            this.mLeftPauseBar.lineTo(barWidth, 0.0f);
            this.mLeftPauseBar.close();
            this.mRightPauseBar.moveTo(barWidth + barDist, 0.0f);
            this.mRightPauseBar.lineTo(barWidth + barDist, -newHeight);
            this.mRightPauseBar.lineTo(secondBarTopRight, -newHeight);
            this.mRightPauseBar.lineTo((2.0f * barWidth) + barDist, 0.0f);
            this.mRightPauseBar.close();
        }
        canvas.save();
        canvas.translate(lerp(0.0f, newHeight / 8.0f, this.mProgress), 0.0f);
        float rotationProgress = this.mIsPlay ? 1.0f - this.mProgress : this.mProgress;
        if (this.mIsPlay) {
            startingRotation = 90.0f;
        }
        canvas.rotate(lerp(startingRotation, 90.0f + startingRotation, rotationProgress), this.mWidth / 2.0f, this.mHeight / 2.0f);
        canvas.translate((this.mWidth / 2.0f) - (((2.0f * barWidth) + barDist) / 2.0f), (this.mHeight / 2.0f) + (newHeight / 2.0f));
        if (firstBarTopLeft == secondBarTopRight) {
            canvas.drawPath(this.mLeftPauseBar, this.mPaintFill);
            canvas.drawPath(this.mLeftPauseBar, this.mPaintStroke);
        } else {
            canvas.drawPath(this.mLeftPauseBar, this.mPaintFill);
            canvas.drawPath(this.mLeftPauseBar, this.mPaintStroke);
            canvas.drawPath(this.mRightPauseBar, this.mPaintFill);
            canvas.drawPath(this.mRightPauseBar, this.mPaintStroke);
        }
        canvas.restore();
    }

    public Animator getPausePlayAnimator() {
        float f;
        float f2 = 0.0f;
        Property property = PROGRESS;
        float[] fArr = new float[2];
        if (this.mIsPlay) {
            f = 1.0f;
        } else {
            f = 0.0f;
        }
        fArr[0] = f;
        if (!this.mIsPlay) {
            f2 = 1.0f;
        }
        fArr[1] = f2;
        Animator anim = ObjectAnimator.ofFloat(this, property, fArr);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                AnimationDrawable.this.mIsPlay = !AnimationDrawable.this.mIsPlay;
            }
        });
        return anim;
    }

    public boolean isPlay() {
        return this.mIsPlay;
    }

    private void setProgress(float progress) {
        this.mProgress = progress;
        invalidateSelf();
    }

    private float getProgress() {
        return this.mProgress;
    }

    public void setAlpha(int alpha) {
        this.mPaintFill.setAlpha(alpha);
        invalidateSelf();
    }

    public void setColorFilter(ColorFilter cf) {
        this.mPaintFill.setColorFilter(cf);
        invalidateSelf();
    }

    public int getOpacity() {
        return -3;
    }

    public int getIntrinsicHeight() {
        return getSize();
    }

    public int getIntrinsicWidth() {
        return getSize();
    }

    public int getMinimumHeight() {
        return getSize();
    }

    public int getMinimumWidth() {
        return getSize();
    }

    private int getSize() {
        return (int) (this.mPauseBarPadding + this.mPauseBarHeight);
    }

    private static float lerp(float a, float b, float t) {
        return ((b - a) * t) + a;
    }
}
