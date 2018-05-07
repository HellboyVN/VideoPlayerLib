package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.util.Log;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DiskLruCache.Editor;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DiskLruCache.Snapshot;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageFetcher extends ImageResizer {
    private static final int DISK_CACHE_INDEX = 0;
    private static final String TAG = "ImageFetcher";
    private static final String THUMBNAIL_CACHE_DIR = "thumbnail";
    private static final int THUMBNAIL_CACHE_SIZE = 10485760;
    private File mThumbnailCacheDir;
    private DiskLruCache mThumbnailDiskCache;
    private final Object mThumbnailDiskCacheLock = new Object();
    private boolean mThumbnailDiskCacheStarting = true;

    public ImageFetcher(Context context, int imageWidth, int imageHeight) {
        super(context, imageWidth, imageHeight);
        init(context);
    }

    public ImageFetcher(Context context, int imageSize) {
        super(context, imageSize);
        init(context);
    }

    private void init(Context context) {
        this.mThumbnailCacheDir = ImageCache.getDiskCacheDir(context, THUMBNAIL_CACHE_DIR);
    }

    protected void initDiskCacheInternal() {
        super.initDiskCacheInternal();
        initThumbnailDiskCache();
    }

    private void initThumbnailDiskCache() {
        if (!this.mThumbnailCacheDir.exists()) {
            this.mThumbnailCacheDir.mkdirs();
        }
        synchronized (this.mThumbnailDiskCacheLock) {
            if (ImageCache.getUsableSpace(this.mThumbnailCacheDir) > 10485760) {
                try {
                    this.mThumbnailDiskCache = DiskLruCache.open(this.mThumbnailCacheDir, 1, 1, 10485760);
                } catch (IOException e) {
                    this.mThumbnailDiskCache = null;
                }
            }
            this.mThumbnailDiskCacheStarting = false;
            this.mThumbnailDiskCacheLock.notifyAll();
        }
    }

    protected void clearCacheInternal() {
        super.clearCacheInternal();
        synchronized (this.mThumbnailDiskCacheLock) {
            if (!(this.mThumbnailDiskCache == null || this.mThumbnailDiskCache.isClosed())) {
                try {
                    this.mThumbnailDiskCache.delete();
                } catch (IOException e) {
                    Log.e(TAG, "clearCacheInternal - " + e);
                }
                this.mThumbnailDiskCache = null;
                this.mThumbnailDiskCacheStarting = true;
                initThumbnailDiskCache();
            }
        }
    }

    protected void flushCacheInternal() {
        super.flushCacheInternal();
        synchronized (this.mThumbnailDiskCacheLock) {
            if (this.mThumbnailDiskCache != null) {
                try {
                    this.mThumbnailDiskCache.flush();
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }

    protected void closeCacheInternal() {
        super.closeCacheInternal();
        synchronized (this.mThumbnailDiskCacheLock) {
            if (this.mThumbnailDiskCache != null) {
                try {
                    if (!this.mThumbnailDiskCache.isClosed()) {
                        this.mThumbnailDiskCache.close();
                        this.mThumbnailDiskCache = null;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "closeCacheInternal - " + e);
                }
            }
        }
    }

    private Bitmap processBitmap(String data) {
        String key = ImageCache.hashKeyForDisk(data);
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        synchronized (this.mThumbnailDiskCacheLock) {
            while (this.mThumbnailDiskCacheStarting) {
                try {
                    this.mThumbnailDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }
            if (this.mThumbnailDiskCache != null) {
                try {
                    Snapshot snapshot = this.mThumbnailDiskCache.get(key);
                    if (snapshot == null) {
                        Editor editor = this.mThumbnailDiskCache.edit(key);
                        if (editor != null) {
                            if (thumbnailUriToStream(data, editor.newOutputStream(0))) {
                                editor.commit();
                            } else {
                                editor.abort();
                            }
                        }
                        snapshot = this.mThumbnailDiskCache.get(key);
                    }
                    if (snapshot != null) {
                        fileInputStream = (FileInputStream) snapshot.getInputStream(0);
                        fileDescriptor = fileInputStream.getFD();
                    }
                    if (fileDescriptor == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e2) {
                        }
                    }
                } catch (IOException e3) {
                    Log.e(TAG, "processBitmap - " + e3);
                    if (null == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (IllegalStateException e5) {
                    Log.e(TAG, "processBitmap - " + e5);
                    if (null == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e6) {
                        }
                    }
                } catch (Throwable th) {
                    if (null == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e7) {
                        }
                    }
                }
            }
        }
        Bitmap bitmap = null;
        if (fileDescriptor != null) {
            bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(fileDescriptor, this.mImageWidth, this.mImageHeight, getImageCache());
        }
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e8) {
            }
        }
        return bitmap;
    }

    protected Bitmap processBitmap(Object data) {
        return processBitmap(String.valueOf(data));
    }

    public boolean thumbnailUriToStream(String urlString, OutputStream outputStream) {
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(urlString, 1);
        if (thumbnail == null) {
            return false;
        }
        ThumbnailUtils.extractThumbnail(thumbnail, this.mImageWidth, this.mImageHeight).compress(CompressFormat.JPEG, 80, outputStream);
        return true;
    }
}
