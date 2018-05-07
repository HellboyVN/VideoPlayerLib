package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.VideoListEntry;


public class VideoListReaderWriter {
    private static String FILENAME = "mediaList.json";
    private static VideoListReaderWriter instance;
    private Context context;
    private Object lock = new Object();

    public VideoListReaderWriter(Context context) {
        this.context = context;
    }

    public static VideoListReaderWriter getVideoListWriter(Context context) {
        if (instance != null) {
            return instance;
        }
        instance = new VideoListReaderWriter(context);
        return instance;
    }


    private void writeVideoListEntries(JsonWriter writer, ArrayList<VideoListEntry> videoListEntries) throws IOException {
        writer.beginArray();
        Iterator i$ = videoListEntries.iterator();
        while (i$.hasNext()) {
            writeVideoListEntry(writer, (VideoListEntry) i$.next());
        }
        writer.endArray();
    }

    private void writeVideoListEntry(JsonWriter writer, VideoListEntry entry) throws IOException {
        writer.beginObject();
        writer.name("file").value(entry.getFilename());
        writer.name(DatabaseHelper.KEY_DURATION).value(entry.getDuration());
        writer.name("dimention").value(entry.getDimension());
        writer.name(DatabaseHelper.KEY_DATE_TAKEN).value(entry.getDateTaken());
        writer.name(DatabaseHelper.KEY_PLAYED_TIME).value(entry.getPlayedTime());
        writer.name(DatabaseHelper.KEY_SIZE).value(entry.getSize());
        writer.endObject();
    }


    private ArrayList<VideoListEntry> readVideoListArray(JsonReader reader) throws IOException {
        ArrayList<VideoListEntry> videoListEntries = new ArrayList();
        reader.beginArray();
        while (reader.hasNext()) {
            videoListEntries.add(readVideoListEntry(reader));
        }
        reader.endArray();
        return videoListEntries;
    }

    private VideoListEntry readVideoListEntry(JsonReader reader) throws IOException {
        String filename = "";
        String dimension = "";
        long duration = 0;
        long playedTime = 0;
        long dataTaken = 0;
        long size = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("file")) {
                filename = reader.nextString();
            } else if (name.equals(DatabaseHelper.KEY_DURATION)) {
                duration = reader.nextLong();
            } else if (name.equals("dimention")) {
                dimension = reader.nextString();
            } else if (name.equals(DatabaseHelper.KEY_DATE_TAKEN)) {
                dataTaken = reader.nextLong();
            } else if (name.equals(DatabaseHelper.KEY_PLAYED_TIME)) {
                playedTime = reader.nextLong();
            } else if (name.equals(DatabaseHelper.KEY_SIZE)) {
                size = reader.nextLong();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new VideoListEntry(0, filename, duration, dimension, dataTaken, playedTime, size);
    }
}
