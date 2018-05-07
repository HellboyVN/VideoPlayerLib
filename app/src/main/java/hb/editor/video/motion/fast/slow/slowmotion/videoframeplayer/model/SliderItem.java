package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model;

public class SliderItem {
    private int icon;
    private String title;

    public SliderItem(String title, int icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return this.title;
    }

    public int getIcon() {
        return this.icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
