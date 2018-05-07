package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model;

public class VideoListEntry {
    private long _id;
    private long dateTaken;
    private String dimension;
    private long duration;
    private String mediaFilename;
    private long playedTime;
    private long res_id;
    private long size;
    private String title;

    public VideoListEntry() {
        this.mediaFilename = "";
        this.duration = 0;
        this.playedTime = 0;
        this.dateTaken = 0;
        this._id = 0;
        this.dimension = "0x0";
    }

    public VideoListEntry(long res_id, String filename, long duration, String dimension, long dateTaken, long playedTime, long size) {
        this.mediaFilename = filename;
        this.duration = duration;
        this.dimension = dimension;
        this.playedTime = playedTime;
        this.dateTaken = dateTaken;
        this.size = size;
        this.res_id = res_id;
    }

    public String getFilename() {
        return this.mediaFilename;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setFilename(String filename) {
        this.mediaFilename = filename;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration.longValue();
    }

    public String getDimension() {
        return this.dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public long getPlayedTime() {
        return this.playedTime;
    }

    public void setPlayedTime(Long playedTime) {
        this.playedTime = playedTime.longValue();
    }

    public long getDateTaken() {
        return this.dateTaken;
    }

    public void setDateTaken(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getId() {
        return this._id;
    }

    public void setId(long _id) {
        this._id = _id;
    }

    public long getResId() {
        return this.res_id;
    }

    public void setResId(long id) {
        this.res_id = id;
    }

    public String getTime() {
        String info = "";
        int ms = (int) (this.duration / 1000);
        int seconds = ms % 60;
        ms /= 60;
        int min = ms % 60;
        if (ms / 60 > 0) {
            return String.format("%02d:%02d:%02d", new Object[]{Integer.valueOf(ms / 60), Integer.valueOf(min), Integer.valueOf(seconds)});
        }
        return String.format("%02d:%02d", new Object[]{Integer.valueOf(min), Integer.valueOf(seconds)});
    }
}
