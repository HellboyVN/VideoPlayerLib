package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.ImageView;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.ImageCache.ImageCacheParams;
import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

public abstract class ImageWorker {
    private static final int FADE_IN_TIME = 200;
    private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_CLOSE = 3;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final String TAG = "ImageWorker";
    private boolean mExitTasksEarly = false;
    private boolean mFadeInBitmap = true;
    private ImageCache mImageCache;
    private ImageCacheParams mImageCacheParams;
    private Bitmap mLoadingBitmap;
    protected boolean mPauseWork = false;
    private final Object mPauseWorkLock = new Object();
    protected Resources mResources;

    private static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            this.bitmapWorkerTaskReference = new WeakReference(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return (BitmapWorkerTask) this.bitmapWorkerTaskReference.get();
        }
    }

    private class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapDrawable> {
        private final WeakReference<ImageView> imageViewReference;
        private Object mData;

        public BitmapWorkerTask(Object data, ImageView imageView) {
            this.mData = data;
            this.imageViewReference = new WeakReference(imageView);
        }

        protected BitmapDrawable doInBackground(Void... params) {
            String dataString = String.valueOf(this.mData);
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;
            synchronized (ImageWorker.this.mPauseWorkLock) {
                while (ImageWorker.this.mPauseWork && !isCancelled()) {
                    try {
                        ImageWorker.this.mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (!(ImageWorker.this.mImageCache == null || isCancelled() || getAttachedImageView() == null || ImageWorker.this.mExitTasksEarly)) {
                bitmap = ImageWorker.this.mImageCache.getBitmapFromDiskCache(dataString);
            }
            if (!(bitmap != null || isCancelled() || getAttachedImageView() == null || ImageWorker.this.mExitTasksEarly)) {
                bitmap = ImageWorker.this.processBitmap(this.mData);
            }
            if (bitmap != null) {
                if (Utils.hasHoneycomb()) {
                    drawable = new BitmapDrawable(ImageWorker.this.mResources, bitmap);
                } else {
                    drawable = new RecyclingBitmapDrawable(ImageWorker.this.mResources, bitmap);
                }
                if (ImageWorker.this.mImageCache != null) {
                    ImageWorker.this.mImageCache.addBitmapToCache(dataString, drawable);
                }
            }
            return drawable;
        }

        protected void onPostExecute(BitmapDrawable value) {
            if (isCancelled() || ImageWorker.this.mExitTasksEarly) {
                value = null;
            }
            ImageView imageView = getAttachedImageView();
            if (value != null && imageView != null) {
                ImageWorker.this.setImageDrawable(imageView, value);
            }
        }

        protected void onCancelled(BitmapDrawable value) {
            super.onCancelled(value);
            synchronized (ImageWorker.this.mPauseWorkLock) {
                ImageWorker.this.mPauseWorkLock.notifyAll();
            }
        }

        private ImageView getAttachedImageView() {
            ImageView imageView = (ImageView) this.imageViewReference.get();
            return this == ImageWorker.getBitmapWorkerTask(imageView) ? imageView : null;
        }
    }

    protected class CacheAsyncTask extends AsyncTask<Object, Void, Void> {
        protected CacheAsyncTask() {
        }

        protected Void doInBackground(Object... params) {
            switch (((Integer) params[0]).intValue()) {
                case 0:
                    ImageWorker.this.clearCacheInternal();
                    break;
                case 1:
                    ImageWorker.this.initDiskCacheInternal();
                    break;
                case 2:
                    ImageWorker.this.flushCacheInternal();
                    break;
                case 3:
                    ImageWorker.this.closeCacheInternal();
                    break;
            }
            return null;
        }
    }

    protected abstract Bitmap processBitmap(Object obj);

    protected ImageWorker(Context context) {
        this.mResources = context.getResources();
    }

    public void loadImage(Object data, ImageView imageView) {
        if (data != null) {
            BitmapDrawable value = null;
            if (this.mImageCache != null) {
                value = this.mImageCache.getBitmapFromMemCache(String.valueOf(data));
            }
            if (value != null) {
                imageView.setImageDrawable(value);
            } else if (cancelPotentialWork(data, imageView)) {
                BitmapWorkerTask task = new BitmapWorkerTask(data, imageView);
                imageView.setImageDrawable(new AsyncDrawable(this.mResources, this.mLoadingBitmap, task));
                try {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                } catch (RejectedExecutionException e) {
                }
            }
        }
    }

    public void setLoadingImage(Bitmap bitmap) {
        this.mLoadingBitmap = bitmap;
    }

    public void setLoadingImage(int resId) {
        this.mLoadingBitmap = BitmapFactory.decodeResource(this.mResources, resId);
    }

    public void addImageCache(FragmentManager fragmentManager, ImageCacheParams cacheParams) {
        this.mImageCacheParams = cacheParams;
        this.mImageCache = ImageCache.getInstance(fragmentManager, this.mImageCacheParams);
        new CacheAsyncTask().execute(new Object[]{Integer.valueOf(1)});
    }

    public void addImageCache(FragmentActivity activity, String diskCacheDirectoryName) {
        this.mImageCacheParams = new ImageCacheParams(activity, diskCacheDirectoryName);
        this.mImageCache = ImageCache.getInstance(activity.getSupportFragmentManager(), this.mImageCacheParams);
        new CacheAsyncTask().execute(new Object[]{Integer.valueOf(1)});
    }

    public void setImageFadeIn(boolean fadeIn) {
        this.mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        this.mExitTasksEarly = exitTasksEarly;
        setPauseWork(false);
    }

    protected ImageCache getImageCache() {
        return this.mImageCache;
    }

    public static void cancelWork(ImageView imageView) {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
        }
    }

    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask == null) {
            return true;
        }
        Object bitmapData = bitmapWorkerTask.mData;
        if (bitmapData != null && bitmapData.equals(data)) {
            return false;
        }
        bitmapWorkerTask.cancel(true);
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                return ((AsyncDrawable) drawable).getBitmapWorkerTask();
            }
        }
        return null;
    }

    public void removeEntry(String data) {
        this.mImageCache.removeEntry(data);
    }

    private void setImageDrawable(ImageView imageView, Drawable drawable) {
        if (this.mFadeInBitmap) {
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{new ColorDrawable(this.mResources.getColor(17170445)), drawable});
            if (Utils.hasJellyBean()) {
                imageView.setBackground(new BitmapDrawable(this.mResources, this.mLoadingBitmap));
            } else {
                imageView.setBackgroundDrawable(new BitmapDrawable(this.mResources, this.mLoadingBitmap));
            }
            imageView.setImageDrawable(td);
            td.startTransition(200);
            return;
        }
        imageView.setImageDrawable(drawable);
    }

    public void setPauseWork(boolean pauseWork) {
        synchronized (this.mPauseWorkLock) {
            this.mPauseWork = pauseWork;
            if (!this.mPauseWork) {
                this.mPauseWorkLock.notifyAll();
            }
        }
    }

    protected void initDiskCacheInternal() {
        if (this.mImageCache != null) {
            this.mImageCache.initDiskCache();
        }
    }

    protected void clearCacheInternal() {
        if (this.mImageCache != null) {
            this.mImageCache.clearCache();
        }
    }

    protected void flushCacheInternal() {
        if (this.mImageCache != null) {
            this.mImageCache.flush();
        }
    }

    protected void closeCacheInternal() {
        if (this.mImageCache != null) {
            this.mImageCache.close();
            this.mImageCache = null;
        }
    }

    public void clearCache() {
        new CacheAsyncTask().execute(new Object[]{Integer.valueOf(0)});
    }

    public void flushCache() {
        new CacheAsyncTask().execute(new Object[]{Integer.valueOf(2)});
    }

    public void closeCache() {
        new CacheAsyncTask().execute(new Object[]{Integer.valueOf(3)});
    }
}
