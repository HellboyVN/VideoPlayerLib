package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model;

import android.graphics.drawable.Drawable;

public class NavigationItem {
    private Drawable mDrawable;
    private String mText;

    public NavigationItem(String text, Drawable drawable) {
        this.mText = text;
        this.mDrawable = drawable;
    }

    public String getText() {
        return this.mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public Drawable getDrawable() {
        return this.mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }
}
