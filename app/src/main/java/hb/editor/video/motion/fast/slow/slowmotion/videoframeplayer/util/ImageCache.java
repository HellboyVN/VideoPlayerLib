package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DiskLruCache.Editor;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.DiskLruCache.Snapshot;


public class ImageCache {
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final int DEFAULT_DISK_CACHE_SIZE = 10485760;
    private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = true;
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final int DEFAULT_MEM_CACHE_SIZE = 5120;
    private static final int DISK_CACHE_INDEX = 0;
    private static final String TAG = "ImageCache";
    private ImageCacheParams mCacheParams;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private DiskLruCache mDiskLruCache;
    private LruCache<String, BitmapDrawable> mMemoryCache;
    private Set<SoftReference<Bitmap>> mReusableBitmaps;

    public static class ImageCacheParams {
        public CompressFormat compressFormat = ImageCache.DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = 70;
        public File diskCacheDir;
        public boolean diskCacheEnabled = true;
        public int diskCacheSize = ImageCache.DEFAULT_DISK_CACHE_SIZE;
        public boolean initDiskCacheOnCreate = true;
        public int memCacheSize = ImageCache.DEFAULT_MEM_CACHE_SIZE;
        public boolean memoryCacheEnabled = true;

        public ImageCacheParams(Context context, String diskCacheDirectoryName) {
            this.diskCacheDir = ImageCache.getDiskCacheDir(context, diskCacheDirectoryName);
        }

        public void setMemCacheSizePercent(float percent) {
            if (percent < 0.01f || percent > 0.8f) {
                throw new IllegalArgumentException("setMemCacheSizePercent - percent must be between 0.01 and 0.8 (inclusive)");
            }
            this.memCacheSize = Math.round((((float) Runtime.getRuntime().maxMemory()) * percent) / 1024.0f);
        }
    }

    public static class RetainFragment extends Fragment {
        private Object mObject;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public void setObject(Object object) {
            this.mObject = object;
        }

        public Object getObject() {
            return this.mObject;
        }
    }

    private ImageCache(ImageCacheParams cacheParams) {
        init(cacheParams);
    }

    public static ImageCache getInstance(FragmentManager fragmentManager, ImageCacheParams cacheParams) {
        RetainFragment mRetainFragment = findOrCreateRetainFragment(fragmentManager);
        ImageCache imageCache = (ImageCache) mRetainFragment.getObject();
        if (imageCache != null) {
            return imageCache;
        }
        imageCache = new ImageCache(cacheParams);
        mRetainFragment.setObject(imageCache);
        return imageCache;
    }

    private void init(ImageCacheParams cacheParams) {
        this.mCacheParams = cacheParams;
        if (this.mCacheParams.memoryCacheEnabled) {
            if (Utils.hasHoneycomb()) {
                this.mReusableBitmaps = Collections.synchronizedSet(new HashSet());
            }
            this.mMemoryCache = new LruCache<String, BitmapDrawable>(this.mCacheParams.memCacheSize) {
                protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                    if (RecyclingBitmapDrawable.class.isInstance(oldValue)) {
                        ((RecyclingBitmapDrawable) oldValue).setIsCached(false);
                    } else if (Utils.hasHoneycomb()) {
                        ImageCache.this.mReusableBitmaps.add(new SoftReference(oldValue.getBitmap()));
                    }
                }

                protected int sizeOf(String key, BitmapDrawable value) {
                    int bitmapSize = ImageCache.getBitmapSize(value) / 1024;
                    return bitmapSize == 0 ? 1 : bitmapSize;
                }
            };
        }
        if (cacheParams.initDiskCacheOnCreate) {
            initDiskCache();
        }
    }

    public void initDiskCache() {
        synchronized (this.mDiskCacheLock) {
            if (this.mDiskLruCache == null || this.mDiskLruCache.isClosed()) {
                File diskCacheDir = this.mCacheParams.diskCacheDir;
                if (this.mCacheParams.diskCacheEnabled && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs();
                    }
                    if (getUsableSpace(diskCacheDir) > ((long) this.mCacheParams.diskCacheSize)) {
                        try {
                            this.mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, (long) this.mCacheParams.diskCacheSize);
                        } catch (IOException e) {
                            this.mCacheParams.diskCacheDir = null;
                            Log.e(TAG, "initDiskCache - " + e);
                        }
                    }
                }
            }
            this.mDiskCacheStarting = false;
            this.mDiskCacheLock.notifyAll();
        }
    }

    public void addBitmapToCache(String data, BitmapDrawable value) {
        if (data != null && value != null) {
            if (this.mMemoryCache != null) {
                if (RecyclingBitmapDrawable.class.isInstance(value)) {
                    ((RecyclingBitmapDrawable) value).setIsCached(true);
                }
                this.mMemoryCache.put(data, value);
            }
            synchronized (this.mDiskCacheLock) {
                if (this.mDiskLruCache != null) {
                    String key = hashKeyForDisk(data);
                    OutputStream out = null;
                    try {
                        Snapshot snapshot = this.mDiskLruCache.get(key);
                        if (snapshot == null) {
                            Editor editor = this.mDiskLruCache.edit(key);
                            if (editor != null) {
                                out = editor.newOutputStream(0);
                                value.getBitmap().compress(this.mCacheParams.compressFormat, this.mCacheParams.compressQuality, out);
                                editor.commit();
                                out.close();
                            }
                        } else {
                            snapshot.getInputStream(0).close();
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                            }
                        }
                    } catch (IOException e2) {
                        Log.e(TAG, "addBitmapToCache - " + e2);
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e3) {
                            }
                        }
                    } catch (Exception e4) {
                        Log.e(TAG, "addBitmapToCache - " + e4);
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e5) {
                            }
                        }
                    } catch (Throwable th) {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e6) {
                            }
                        }
                    }
                }
            }
            return;
        }
        return;
    }

    public BitmapDrawable getBitmapFromMemCache(String data) {
        if (this.mMemoryCache != null) {
            return (BitmapDrawable) this.mMemoryCache.get(data);
        }
        return null;
    }

    public Bitmap getBitmapFromDiskCache(String data) {
        String key = hashKeyForDisk(data);
        Bitmap bitmap = null;
        synchronized (this.mDiskCacheLock) {
            while (this.mDiskCacheStarting) {
                try {
                    this.mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                }
            }
            if (this.mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    Snapshot snapshot = this.mDiskLruCache.get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(0);
                        if (inputStream != null) {
                            bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(((FileInputStream) inputStream).getFD(), Integer.MAX_VALUE, Integer.MAX_VALUE, this);
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e2) {
                        }
                    }
                } catch (IOException e3) {
                    Log.e(TAG, "getBitmapFromDiskCache - " + e3);
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (Throwable th) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e5) {
                        }
                    }
                }
            }
        }
        return bitmap;
    }

    protected Bitmap getBitmapFromReusableSet(Options options) {
        Bitmap bitmap = null;
        if (!(this.mReusableBitmaps == null || this.mReusableBitmaps.isEmpty())) {
            synchronized (this.mReusableBitmaps) {
                Iterator<SoftReference<Bitmap>> iterator = this.mReusableBitmaps.iterator();
                while (iterator.hasNext()) {
                    Bitmap item = (Bitmap) ((SoftReference) iterator.next()).get();
                    if (item == null || !item.isMutable()) {
                        iterator.remove();
                    } else if (canUseForInBitmap(item, options)) {
                        bitmap = item;
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        return bitmap;
    }

    void removeEntry(String data) {
        if (data != null) {
            if (this.mMemoryCache != null) {
                this.mMemoryCache.remove(data);
            }
            if (this.mDiskLruCache != null) {
                try {
                    this.mDiskLruCache.remove(hashKeyForDisk(data));
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }
    }

    public void clearCache() {
        if (this.mMemoryCache != null) {
            this.mMemoryCache.evictAll();
        }
        synchronized (this.mDiskCacheLock) {
            this.mDiskCacheStarting = true;
            if (!(this.mDiskLruCache == null || this.mDiskLruCache.isClosed())) {
                try {
                    this.mDiskLruCache.delete();
                } catch (IOException e) {
                    Log.e(TAG, "clearCache - " + e);
                }
                this.mDiskLruCache = null;
                initDiskCache();
            }
        }
    }

    public void flush() {
        synchronized (this.mDiskCacheLock) {
            if (this.mDiskLruCache != null) {
                try {
                    this.mDiskLruCache.flush();
                } catch (IOException e) {
                    Log.e(TAG, "flush - " + e);
                }
            }
        }
    }

    public void close() {
        synchronized (this.mDiskCacheLock) {
            if (this.mDiskLruCache != null) {
                try {
                    if (!this.mDiskLruCache.isClosed()) {
                        this.mDiskLruCache.close();
                        this.mDiskLruCache = null;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close - " + e);
                }
            }
        }
    }

    @TargetApi(19)
    private static boolean canUseForInBitmap(Bitmap candidate, Options targetOptions) {
        if (Utils.hasKitKat()) {
            if (((targetOptions.outWidth / targetOptions.inSampleSize) * (targetOptions.outHeight / targetOptions.inSampleSize)) * getBytesPerPixel(candidate.getConfig()) > candidate.getAllocationByteCount()) {
                return false;
            }
            return true;
        } else if (candidate.getWidth() == targetOptions.outWidth && candidate.getHeight() == targetOptions.outHeight && targetOptions.inSampleSize == 1) {
            return true;
        } else {
            return false;
        }
    }

    private static int getBytesPerPixel(Config config) {
        if (config == Config.ARGB_8888) {
            return 4;
        }
        if (config == Config.RGB_565 || config == Config.ARGB_4444) {
            return 2;
        }
        if (config == Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath = ("mounted".equals(Environment.getExternalStorageState()) || !isExternalStorageRemovable()) ? getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();
        return new File(cachePath + File.separator + uniqueName);
    }

    public static String hashKeyForDisk(String key) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            return bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(key.hashCode());
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 255);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    @TargetApi(19)
    public static int getBitmapSize(BitmapDrawable value) {
        Bitmap bitmap = value.getBitmap();
        if (Utils.hasKitKat()) {
            return bitmap.getAllocationByteCount();
        }
        if (Utils.hasHoneycombMR1()) {
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    @TargetApi(9)
    public static boolean isExternalStorageRemovable() {
        if (Utils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    @TargetApi(8)
    public static File getExternalCacheDir(Context context) {
        if (Utils.hasFroyo()) {
            File result = context.getExternalCacheDir();
            if (result == null) {
                return context.getCacheDir();
            }
            return result;
        }
        return new File(Environment.getExternalStorageDirectory().getPath() + ("/Android/data/" + context.getPackageName() + "/cache/"));
    }

    @TargetApi(9)
    public static long getUsableSpace(File path) {
        if (Utils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        StatFs stats = new StatFs(path.getPath());
        return ((long) stats.getBlockSize()) * ((long) stats.getAvailableBlocks());
    }

    private static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (mRetainFragment != null) {
            return mRetainFragment;
        }
        Fragment mRetainFragment2 = new RetainFragment();
        fm.beginTransaction().add(mRetainFragment2, TAG).commitAllowingStateLoss();
        return (RetainFragment) mRetainFragment2;
    }
}
