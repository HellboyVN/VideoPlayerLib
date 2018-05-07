package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media;

import android.media.MediaExtractor;
import android.os.Process;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MediaStreamReader extends Thread {
    private static final String LOG_TAG = "MediaStreamReader";
    public static final int audioId = 2;
    public static final int videoId = 1;
    private MediaExtractor audioExtractor;
    private final BlockingQueue<QueueItem> audioFinishedRequest;
    private final QueueItem exitSentinel = new QueueItem(-1);
    private final QueueItem flushSentinel = new QueueItem(-2);
    private final BlockingDeque<QueueItem> pendingRequest;
    private boolean quit = false;
    private MediaExtractor videoExtractor;
    private final BlockingQueue<QueueItem> videoFinishedRequest;

    public static class QueueItem {
        public ByteBuffer buffer;
        public int flags;
        public int identifier;
        public int index;
        public long sampleTimeUs;
        public int size;

        QueueItem() {
        }

        QueueItem(int id) {
            this.identifier = id;
        }
    }

    MediaStreamReader(int videoQueueSize, int audioQueueSize) {
        super("ReaderThread");
        this.pendingRequest = new LinkedBlockingDeque(videoQueueSize + audioQueueSize);
        this.videoFinishedRequest = new ArrayBlockingQueue(videoQueueSize);
        this.audioFinishedRequest = new ArrayBlockingQueue(audioQueueSize);
    }

    public void setVideoExtractor(MediaExtractor extractor) {
        this.videoExtractor = extractor;
    }

    public void setAudioExtractor(MediaExtractor extractor) {
        this.audioExtractor = extractor;
    }

    public int remainingCapacity() {
        return this.pendingRequest.remainingCapacity();
    }

    public void queueWork(QueueItem item) {
        try {
            this.pendingRequest.put(item);
        } catch (InterruptedException e) {
            Log.d(LOG_TAG, "readRequest: " + e.getMessage());
        }
    }

    public QueueItem getQueueItem(int id) {
        BlockingQueue<QueueItem> queue = null;
        if (id == 1) {
            queue = this.videoFinishedRequest;
        } else if (id == 2) {
            queue = this.audioFinishedRequest;
        }
        if (queue == null || queue.peek() == null) {
            return null;
        }
        return (QueueItem) queue.remove();
    }

    public void interrupt() {
        queueWork(this.exitSentinel);
        super.interrupt();
    }

    public void run() {
        Process.setThreadPriority(-16);
        while (!isInterrupted()) {
            try {
                QueueItem item = (QueueItem) this.pendingRequest.take();
                BlockingQueue queue = null;
                MediaExtractor extractor = null;
                if (item.identifier != 1) {
                    if (item.identifier != 2) {
                        if (item.identifier != -2) {
                            if (item.identifier == -1) {
                                break;
                            }
                        }
                        this.pendingRequest.clear();
                        this.videoFinishedRequest.clear();
                        this.audioFinishedRequest.clear();
                    } else {
                        extractor = this.audioExtractor;
                        queue = this.audioFinishedRequest;
                    }
                } else {
                    extractor = this.videoExtractor;
                    queue = this.videoFinishedRequest;
                }
                if (queue != null) {
                    item.buffer.clear();
                    item.flags = extractor.getSampleFlags();
                    item.sampleTimeUs = extractor.getSampleTime();
                    int size = extractor.readSampleData(item.buffer, 0);
                    extractor.advance();
                    item.size = size;
                    if (item.size <= 0) {
                        item.flags = 4;
                        item.size = 0;
                    }
                    queue.put(item);
                }
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, e.toString());
                interrupt();
                return;
            }
        }
        if (this.videoExtractor != null) {
            this.videoExtractor.release();
        }
        if (this.audioExtractor != null) {
            this.audioExtractor.release();
        }
    }

    public void flush() {
        this.pendingRequest.addFirst(this.flushSentinel);
    }
}
