package hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.R;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.adapter.ListFilterAdapter;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media.VideoFramePlayer;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media.VideoFramePlayer.PlaybackStateListener;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.media.VideoFramePlayer.VideoSizeChangeListener;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.model.FilterItem;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.AccessStorage;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.AppConstant;
import hb.editor.video.motion.fast.slow.slowmotion.videoframeplayer.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoFramePlayerActivity extends Activity implements OnFrameAvailableListener, Renderer, PlaybackStateListener, VideoSizeChangeListener {
    private static final int FADE_OUT = 1;
    private static String LOG_TAG = "VideoFramePlayerActivity";
    public static final int PLAYBACK_FINISHED = 3;
    public static final int PLAYBACK_STARTED = 4;
    private static final int REQUEST_CODE_STORAGE_RW = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int sDefaultTimeout = 3000;
    private static final int sInfiniteTimeout = 0;
    private static final long sPlayPauseDuration = 200;
    private FrameLayout frameLayout;
    boolean isResumed = false;
    private AnimatorSet mAnimatorSet;
    private ImageView mBackButton;
    private ViewGroup mBottomOverlay;
    private int mCropBottom;
    private int mCropLeft;
    private int mCropRight;
    private int mCropTop;
    private int mCropingHeight;
    private int mCropingWidth;
    private TextView mCurrentSpeed;
    private ImageView mDecreaseSpeed;
    private RecyclerView listFilter;
    private List<FilterItem> filterItems = new ArrayList<>();
    private ListFilterAdapter filterAdapter;
    private AdView mAdView;
    private OnClickListener mDefaultImageViewButton = new OnClickListener() {
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_back:
                    VideoFramePlayerActivity.this.finish();
                    break;
                case R.id.button_filter:
                    screenshot = true;
                    Toast.makeText(getApplicationContext(),"Screenshot save at: \n"+ Environment.getExternalStorageDirectory()+"/SlowMotion/",Toast.LENGTH_SHORT).show();
                    break;
            }
            VideoFramePlayerActivity.this.showOverlay(3000);
        }
    };
    private OnTouchListener mDefaultTouchListener = new OnTouchListener() {
        public boolean onTouch(View view, MotionEvent e) {
            if (e.getAction() == 0 && VideoFramePlayerActivity.this.mPlaybackStarted) {
                VideoFramePlayerActivity.this.showOverlay(3000);
            }
            return true;
        }
    };
    private boolean mDragging;
    private String mFilename;
    private ImageView mFilters;
    private GLESRenderer mGLESRenderer;
    private ImageView mGrabImage;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VideoFramePlayerActivity.this.hideOverlay();
                    return;
                case 2:
                    long pos = VideoFramePlayerActivity.this.setProgress();
                    if (!VideoFramePlayerActivity.this.mDragging && VideoFramePlayerActivity.this.mVisible && VideoFramePlayerActivity.this.mPlayer != null && VideoFramePlayerActivity.this.mPlayer.isPlaying()) {
                        sendMessageDelayed(obtainMessage(2), 1000 - (pos % 1000));
                        return;
                    }
                    return;
                case 3:
                    VideoFramePlayerActivity.this.finish();
                    break;
                case 4:
                    break;
                default:
                    return;
            }
            VideoFramePlayerActivity.this.showOverlay(3000);
        }
    };
    boolean mHasValidSurface = false;
    private ImageView mIncreaseSpeed;
    private long mLastPlayingTimeMs;
    boolean mNeedCrop = false;
    private ImageView mPlayPause;
    private AnimationDrawable mPlayPauseDrawable;
    private OnTouchListener mPlayPauseListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == 1) {
                VideoFramePlayerActivity.this.flipPlayPause(true);
            }
            return true;
        }
    };
    private OnClickListener mPlaybackSpeedControl = new OnClickListener() {
        public void onClick(View view) {
            if (VideoFramePlayerActivity.this.mPlayer != null) {
                switch (view.getId()) {
                    case R.id.add:
                        VideoFramePlayerActivity.this.mPlayer.setPlaybackSpeed(true);
                        break;
                    case R.id.minus:
                        VideoFramePlayerActivity.this.mPlayer.setPlaybackSpeed(false);
                        break;
                }
                VideoFramePlayerActivity.this.mCurrentSpeed.setText(String.format("%1.2f", new Object[]{Float.valueOf(VideoFramePlayerActivity.this.mPlayer.getPlaybackRate())}) + "x");
            }
            VideoFramePlayerActivity.this.showOverlay(3000);
        }
    };
    boolean mPlaybackStarted = false;
    private GLSurfaceView mPlaybackView;
    private VideoFramePlayer mPlayer;
    private SeekBar mProgressBar;
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar seekBar) {
            VideoFramePlayerActivity.this.mDragging = true;
            VideoFramePlayerActivity.this.showOverlay(0);
            VideoFramePlayerActivity.this.mHandler.removeMessages(2);
            if (VideoFramePlayerActivity.this.mPlayer != null) {
                VideoFramePlayerActivity.this.mPlayer.SeekStart();
            }
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromuser) {
            if (fromuser && VideoFramePlayerActivity.this.mPlayer != null) {
                long durationMs = VideoFramePlayerActivity.this.mPlayer.getDuration();
                VideoFramePlayerActivity.this.mLastPlayingTimeMs = VideoFramePlayerActivity.this.mPlayer.getCurrentPosition();
                VideoFramePlayerActivity.this.mSeekPositionMs = (((long) progress) * durationMs) / 1000;
                VideoFramePlayerActivity.this.mPlayer.Seek(VideoFramePlayerActivity.this.mSeekPositionMs);
                VideoFramePlayerActivity.this.mSeeking = true;
                if (VideoFramePlayerActivity.this.mTextViewTime != null) {
                    VideoFramePlayerActivity.this.mTextViewTime.setText(VideoFramePlayerActivity.this.getTime((long) ((int) VideoFramePlayerActivity.this.mSeekPositionMs)));
                }
            }
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            VideoFramePlayerActivity.this.mDragging = false;
            if (VideoFramePlayerActivity.this.mPlayer != null) {
                VideoFramePlayerActivity.this.mPlayer.SeekEnd();
            }
            VideoFramePlayerActivity.this.showOverlay(3000);
        }
    };
    private long mSeekPositionMs;
    private boolean mSeeking = false;
    private ViewGroup mSlowMotionOverlay;
    private boolean mSlowUIEnabled = true;
    boolean mStartPlayer = true;
    long mStartPosition = 0;
    float mStartRate = 1.0f;
    boolean mStarted;
    Surface mSurface = null;
    private SurfaceTexture mSurfaceTexture;
    private TextView mTextViewDuration;
    private TextView mTextViewTime;
    private TextView mTextViewTitle;
    private ViewGroup mTopOverlay;
    private OnTouchListener mTouchListener = new OnTouchListener() {
        public boolean onTouch(View view, MotionEvent e) {
            if (e.getAction() == 0 && VideoFramePlayerActivity.this.mPlaybackStarted) {
                if (VideoFramePlayerActivity.this.mVisible) {
                    VideoFramePlayerActivity.this.hideOverlay();
                } else {
                    VideoFramePlayerActivity.this.showOverlay(3000);
                }
            }
            return false;
        }
    };
    boolean mVideoSizeChanged;
    private boolean mVisible;
    boolean updateSurface;
    boolean screenshot;

    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        this.mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("3F90C25D51474DDEA1C2B2319E1ADE19").build();
        this.mAdView.setAdListener(new AdListener() {
            public void onAdClosed() {
                super.onAdClosed();
            }

            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                VideoFramePlayerActivity.this.mAdView.setVisibility(8);
            }

            public void onAdLoaded() {
                super.onAdLoaded();
                VideoFramePlayerActivity.this.mAdView.setVisibility(0);
            }
        });
        if(!AppConstant.isRemoveAds(getApplicationContext())) {
            Log.e("hellboy","ads load");
            this.mAdView.loadAd(adRequest);
        }

        this.mSlowUIEnabled = sp.getBoolean("slomo_control", true);
        boolean snap_ui = sp.getBoolean("snap_control", false);
        boolean filter_ui = sp.getBoolean("filter_control", true);
        this.mPlaybackView = (GLSurfaceView) findViewById(R.id.PlaybackView);
        this.frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        this.mBottomOverlay = (ViewGroup) findViewById(R.id.bottom_overlay);
        this.mTextViewTime = (TextView) findViewById(R.id.player_overlay_time);
        this.mProgressBar = (SeekBar) findViewById(R.id.player_seekbar);
        this.mTextViewDuration = (TextView) findViewById(R.id.player_overlay_length);
        this.mBottomOverlay.setVisibility(View.GONE);
        if (this.mProgressBar != null) {
            this.mProgressBar.setOnSeekBarChangeListener(this.mSeekListener);
            this.mProgressBar.setMax(1000);
        }
        this.mTopOverlay = (ViewGroup) findViewById(R.id.top_overlay);
        this.mTextViewTitle = (TextView) findViewById(R.id.video_filename);
        this.mFilters = (ImageView) findViewById(R.id.button_filter);
        this.mGrabImage = (ImageView) findViewById(R.id.button_snap);
        this.mBackButton = (ImageView) findViewById(R.id.button_back);
        this.mTopOverlay.setVisibility(View.GONE);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        this.listFilter = (RecyclerView) findViewById(R.id.lv_filter);
        this.listFilter.setLayoutManager(layoutManager);
        prepareFilterList();
        filterAdapter = new ListFilterAdapter(filterItems);
        filterAdapter.setMyViewHolderClickListener(new ListFilterAdapter.MyViewHolder.MyViewHolderClickListener() {
            @Override
            public void onImageClick(View view, final int position) {
                Random rand = new Random();
                int n = rand.nextInt(4)+1;
                Log.e("levan_new","show + n= "+ String.valueOf(n));
                if(n==2) {
                    AppConstant appConstant = new AppConstant();
                    appConstant.showInterFacebook(getApplicationContext());
                }
                        synchronized (VideoFramePlayerActivity.this.mPlaybackView) {
                            VideoFramePlayerActivity.this.mPlaybackView.post(new Runnable() {
                                public void run() {
                                    VideoFramePlayerActivity.this.mGLESRenderer.changeFragmentShader(position);
                                    VideoFramePlayerActivity.this.mPlaybackView.requestRender();
                                }
                            });
                        }


                VideoFramePlayerActivity.this.showOverlay(3000);
            }
        });
        this.listFilter.setAdapter(filterAdapter);
        if (filter_ui) {
            this.mFilters.setOnClickListener(this.mDefaultImageViewButton);
        } else {
            this.mFilters.setVisibility(View.INVISIBLE);
        }
        if (snap_ui) {
            this.mGrabImage.setOnClickListener(this.mDefaultImageViewButton);
        } else {
            this.mGrabImage.setVisibility(View.GONE);
        }
        if (!(filter_ui || snap_ui)) {
            this.mTextViewTitle.setX(this.mTextViewTime.getX() - (getResources().getDisplayMetrics().density * 28.0f));
        }
        this.mBackButton.setOnClickListener(this.mDefaultImageViewButton);
        this.mPlayPause = (ImageView) findViewById(R.id.player_play_pause);
        this.mPlayPause.setOnTouchListener(this.mPlayPauseListener);
        this.mPlayPauseDrawable = new AnimationDrawable(this);
        this.mPlayPause.setImageDrawable(this.mPlayPauseDrawable);
        this.mSlowMotionOverlay = (ViewGroup) findViewById(R.id.slomo_control);
        if (this.mSlowUIEnabled) {
            this.mIncreaseSpeed = (ImageView) findViewById(R.id.add);
            this.mCurrentSpeed = (TextView) findViewById(R.id.playback_speed);
            this.mDecreaseSpeed = (ImageView) findViewById(R.id.minus);
            this.mCurrentSpeed.setText("1.00x");
            this.mSlowMotionOverlay.setVisibility(View.GONE);
            this.mDecreaseSpeed.setOnClickListener(this.mPlaybackSpeedControl);
            this.mIncreaseSpeed.setOnClickListener(this.mPlaybackSpeedControl);
        } else {
            this.mSlowMotionOverlay.setVisibility(View.GONE);
        }
        this.mPlaybackView.setEGLContextClientVersion(2);
        this.mPlaybackView.setPreserveEGLContextOnPause(true);
        this.mPlaybackView.setRenderer(this);
        this.mPlaybackView.setRenderMode(0);
        this.mGLESRenderer = new GLESRenderer(getWindow().getDecorView().getWidth(), getWindow().getDecorView().getHeight());
        this.mStarted = false;
        this.mVideoSizeChanged = false;
        this.mVisible = false;
        this.mDragging = false;
        VFApplication.sendScreenView(LOG_TAG);
//        ////
//        mInputSurface.makeCurrent(); // if its not already current
//        loadTexture(bitmap);
//        GLES20.glViewport(0, 0, viewWidth, viewHeight);
//        mFullFrameRect.drawFrame(mTextureId, GlUtil.IDENTITY_MATRIX);
//        mInputSurface.setPresentationTime(pts);
//        mInputSurface.swapBuffers();
    }


    public void loadTexture(Bitmap bitmap)
    {
        int mTextureId = -1;
        if (mTextureId != -1) {
            int[] textureHandle = new int[1];

            GLES20.glGenTextures(1, textureHandle, 0);


            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        }

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }
    private void prepareFilterList(){
        FilterItem filterItem = new FilterItem("Normal", getResources().getDrawable(R.drawable.img_normal));
        filterItems.add(filterItem);

        filterItem = new FilterItem("Black & White", getResources().getDrawable(R.drawable.img_bw));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Blur", getResources().getDrawable(R.drawable.img_blur));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Edge", getResources().getDrawable(R.drawable.img_edge));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Emboss", getResources().getDrawable(R.drawable.img_emboss));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Contrast", getResources().getDrawable(R.drawable.img_contrast));
        filterItems.add(filterItem);
        filterItem = new FilterItem("FlipVertical", getResources().getDrawable(R.drawable.img_flip));
        filterItems.add(filterItem);
        filterItem = new FilterItem("HueShift", getResources().getDrawable(R.drawable.img_hueshift));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Luminance", getResources().getDrawable(R.drawable.img_luminance));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Negative", getResources().getDrawable(R.drawable.img_negative));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Toon", getResources().getDrawable(R.drawable.img_toon));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Twirl", getResources().getDrawable(R.drawable.img_twirl));
        filterItems.add(filterItem);
        filterItem = new FilterItem("Warp", getResources().getDrawable(R.drawable.img_warp));
        filterItems.add(filterItem);
        filterItem = new FilterItem("FlipHorizontal", getResources().getDrawable(R.drawable.img_flip_hz));
        filterItems.add(filterItem);
    }
    private synchronized void startPlayback() {
        if (this.mStarted) {
            Log.d(LOG_TAG, "mStarted=" + this.mStarted);
        } else if (!this.mHasValidSurface) {
            Log.d(LOG_TAG, "Surface is not yet ready");
        } else if (!needStoragePermission()) {
            final String filepath = handleIntent();
            if (filepath == null) {
                Log.d(LOG_TAG, "Could not resolve the file name from intent");
                finish();
            } else if (new File(filepath).exists()) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        VideoFramePlayerActivity.this.mTextViewTitle.setText(Utils.getRemovedExtensionName(Utils.getFilename(filepath)));
                    }
                });
                this.mFilename = filepath;
                Log.i(LOG_TAG, "Filename: " + this.mFilename);
                if (this.mSurfaceTexture != null) {
                    this.mSurfaceTexture.release();
                }
                if (this.mSurface != null) {
                    this.mSurface.release();
                }
                this.mSurfaceTexture = new SurfaceTexture(this.mGLESRenderer.getTextureId());
                this.mSurfaceTexture.setOnFrameAvailableListener(this);
                this.mSurface = new Surface(this.mSurfaceTexture);
                this.mPlayer = new VideoFramePlayer(this, this.mHandler);
                this.mPlayer.setPlaybackStateListener(this);
                this.mPlayer.setVideoSizeChangeListener(this);
                this.mPlayer.Prepare(this.mFilename);
                this.mPlayer.setSurface(this.mSurface);
                this.mPlayer.Start(this.mStartPosition, this.mStartRate);
                this.mHandler.sendEmptyMessage(2);
                this.mStarted = true;
                this.updateSurface = false;
                this.screenshot = false;
            } else {
                Log.d(LOG_TAG, "Invalid file:" + filepath);
                finish();
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        this.mGLESRenderer.surfaceCreated();
        this.frameLayout.setOnTouchListener(this.mTouchListener);
        this.mBottomOverlay.setOnTouchListener(this.mDefaultTouchListener);
        this.mTopOverlay.setOnTouchListener(this.mDefaultTouchListener);
        this.mSlowMotionOverlay.setOnTouchListener(this.mDefaultTouchListener);
        this.mHasValidSurface = true;
        startPlayback();
    }

    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        synchronized (this) {
            this.mGLESRenderer.setViewport(0, 0, width, height);
        }
    }

    public void onDrawFrame(GL10 gl10) {
        int x, y, w, h;
        x = 0;
        y = 0;

        Display disp = getWindowManager().getDefaultDisplay();

        w = disp.getWidth();
        h = disp.getHeight();
        synchronized (this) {
            if (this.updateSurface) {
                this.mSurfaceTexture.updateTexImage();
                this.updateSurface = false;
            }
        }
        if (this.mNeedCrop) {
            this.mNeedCrop = false;
            this.mGLESRenderer.setCropRectangle(this.mCropTop, this.mCropLeft, this.mCropBottom, this.mCropRight, this.mCropingWidth, this.mCropingHeight);
        }
        if (this.mVideoSizeChanged && this.mPlayer != null) {
            this.mGLESRenderer.setVideoFrameSize(this.mPlayer.getVideoWidth(), this.mPlayer.getVideoHeight(), this.mPlayer.getRotation());
            this.mVideoSizeChanged = false;
        }
        this.mGLESRenderer.drawFrame(this.mSurfaceTexture);
        synchronized (this) {
            if (this.screenshot) {
            int b[] = new int[w * (y + h)];

            int bt[] = new int[w * h];

            IntBuffer ib = IntBuffer.wrap(b);

            ib.position(0);

            gl10.glReadPixels(x, 0, w, y + h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

            for (int i = 0, k = 0; i < h; i++, k++) {
                for (int j = 0; j < w; j++) {

                    int pix = b[i * w + j];

                    int pb = (pix >> 16) & 0xff;

                    int pr = (pix << 16) & 0x00ff0000;

                    int pix1 = (pix & 0xff00ff00) | pr | pb;

                    bt[(h - k - 1) * w + j] = pix1;

                }
            }
            Bitmap bmp = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
            try {
                File directory = new File(Environment.getExternalStorageDirectory()+File.separator+"SlowMotion");
                if(!directory.exists()) directory.mkdirs();
                File f = new File(Environment.getExternalStorageDirectory()+"/SlowMotion/SlowMotion"+ Calendar.getInstance().getTime().toString()+".png");
                f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                try {
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
                this.screenshot = false;
        }
    }
    }

    public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.updateSurface = true;
        this.mPlaybackView.requestRender();
    }

    public void onCropRectangleChange(int cropTop, int cropLeft, int cropBottom, int cropRight, int width, int height) {
        this.mCropLeft = cropLeft;
        this.mCropRight = cropRight;
        this.mCropBottom = cropBottom;
        this.mCropTop = cropTop;
        this.mCropingWidth = width;
        this.mCropingHeight = height;
        this.mNeedCrop = true;
    }

    public void onVideoSizeChange(int width, int height, int rotation) {
        this.mVideoSizeChanged = true;
    }

    private String handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action == null || !"android.intent.action.VIEW".equals(action)) {
            return intent.getDataString();
        }
        return AccessStorage.getPath(getApplicationContext(), intent.getData());
    }

    public void showOverlay(int timeout) {
        if (!this.mVisible) {
            this.mBottomOverlay.setVisibility(View.VISIBLE);
            this.mTopOverlay.setVisibility(View.VISIBLE);
            this.mPlayPause.setVisibility(View.VISIBLE);
            if (this.mSlowUIEnabled) {
                this.mSlowMotionOverlay.setVisibility(View.VISIBLE);
            }
            this.mVisible = true;
        }
        setProgress();
        this.mHandler.sendEmptyMessage(2);
        this.mHandler.removeMessages(1);
        if (timeout != 0) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), (long) timeout);
        }
    }

    public void hideOverlay() {
        if (this.mVisible) {
            this.mBottomOverlay.setVisibility(View.GONE);
            this.mTopOverlay.setVisibility(View.GONE);
            this.mPlayPause.setVisibility(View.GONE);
            if (this.mSlowUIEnabled) {
                this.mSlowMotionOverlay.setVisibility(View.GONE);
            }
            this.mVisible = false;
        }
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(1);
    }

    private void slideToBottom(View view) {
        TranslateAnimation animate = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) view.getHeight());
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.GONE);
    }

    private void slideToTop(ViewGroup view) {
        TranslateAnimation animate = new TranslateAnimation(0.0f, 0.0f, 0.0f, -64.0f);
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.GONE);
    }

    private void newSlideInAnimation(ViewGroup view) {
        ObjectAnimator mSlidInAnimator = ObjectAnimator.ofFloat(view, "translationY", new float[]{1280.0f});
        mSlidInAnimator.setDuration(500);
        mSlidInAnimator.start();
        view.setVisibility(View.GONE);
    }

    private void newSlideOutAnimation(ViewGroup view) {
        ObjectAnimator mSlidOutAnimator = ObjectAnimator.ofFloat(view, "translationY", new float[]{-64.0f});
        mSlidOutAnimator.setDuration(500);
        mSlidOutAnimator.start();
    }

    private String getTime(long timeMs) {
        String info = "";
        long ms = timeMs / 1000;
        long seconds = ms % 60;
        ms /= 60;
        long min = ms % 60;
        if (ms / 60 > 0) {
            return String.format("%02d:%02d:%02d", new Object[]{Long.valueOf(ms / 60), Long.valueOf(min), Long.valueOf(seconds)});
        }
        return String.format("%02d:%02d", new Object[]{Long.valueOf(min), Long.valueOf(seconds)});
    }

    private long setProgress() {
        if (this.mPlayer == null || this.mDragging) {
            return 0;
        }
        long position = this.mPlayer.getCurrentPosition();
        long duration = this.mPlayer.getDuration();
        if (this.mSeeking) {
            if (this.mSeekPositionMs < this.mLastPlayingTimeMs) {
                if (position >= this.mLastPlayingTimeMs) {
                    position = this.mSeekPositionMs;
                } else {
                    this.mSeeking = false;
                }
            } else if (position < this.mSeekPositionMs) {
                position = this.mSeekPositionMs;
            } else {
                this.mSeeking = false;
            }
        }
        if (this.mProgressBar != null && duration > 0) {
            this.mProgressBar.setProgress((int) ((1000 * position) / duration));
            this.mProgressBar.setSecondaryProgress(((int) this.mPlayer.getBufferPercentage()) * 10);
        }
        if (this.mTextViewDuration != null) {
            this.mTextViewDuration.setText(getTime(duration));
        }
        if (this.mTextViewTime == null) {
            return position;
        }
        this.mTextViewTime.setText(getTime(position));
        return position;
    }

    private void flipPlayPause(boolean updatePlayer) {
        if (this.mAnimatorSet != null) {
            this.mAnimatorSet.cancel();
        }
        if (this.mPlayPauseDrawable.isPlay()) {
            showOverlay(3000);
        } else {
            showOverlay(0);
        }
        if (updatePlayer && this.mPlayer != null) {
            if (this.mPlayPauseDrawable.isPlay()) {
                this.mPlayer.Resume();
            } else {
                this.mPlayer.Pause();
            }
        }
        this.mAnimatorSet = new AnimatorSet();
        Animator pausePlayAnim = this.mPlayPauseDrawable.getPausePlayAnimator();
        this.mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        this.mAnimatorSet.setDuration(sPlayPauseDuration);
        this.mAnimatorSet.play(pausePlayAnim);
        this.mAnimatorSet.start();
    }

    private void showPlay() {
        if (!this.mPlayPauseDrawable.isPlay()) {
            flipPlayPause(false);
        }
    }

    private void showPause() {
        if (this.mPlayPauseDrawable.isPlay()) {
            flipPlayPause(false);
        }
    }

    public void onPlaybackStart() {
        this.mPlaybackStarted = true;
        if (this.isResumed) {
            if (!(this.mStartPlayer || this.mPlayer == null)) {
                this.mPlayer.Pause();
                showOverlay(0);
            }
            if (this.mVisible) {
                showOverlay(3000);
                return;
            }
            return;
        }
        showPause();
    }

    public void onPlaybackStop() {
        finish();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                boolean granted = true;
                for (int i : grantResults) {
                    if (i != 0) {
                        granted = false;
                        if (granted) {
                            Toast.makeText(getApplicationContext(), "Permission for Reading/Writing Storage denied!", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        } else if (this.mSurface != null) {
                            startPlayback();
                            return;
                        } else {
                            return;
                        }
                    }
                }
                if (granted) {
                    Toast.makeText(getApplicationContext(), "Permission for Reading/Writing Storage denied!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                } else if (this.mSurface != null) {
                    startPlayback();
                    return;
                } else {
                    return;
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }

    public boolean needStoragePermission() {
        List<String> permissionsList = new ArrayList();
        addPermission(permissionsList, "android.permission.READ_EXTERNAL_STORAGE");
        addPermission(permissionsList, "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permissionsList.size() <= 0) {
            return false;
        }
        ActivityCompat.requestPermissions(this, (String[]) permissionsList.toArray(new String[permissionsList.size()]), 1);
        return true;
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != 0) {
            permissionsList.add(permission);
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private synchronized void releaseResources() {
        Log.d(LOG_TAG, "releaseResources");
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mPlayer != null) {
            this.mStartPosition = this.mPlayer.getCurrentPosition();
            this.mStartRate = this.mPlayer.getPlaybackRate();
            this.mStartPlayer = this.mPlayer.isPlaying();
            this.mPlayer.Stop();
            this.mPlayer.Release();
            this.mPlayer = null;
            this.mPlaybackStarted = false;
            Log.d(LOG_TAG, "App paused media time" + this.mStartPosition);
        }
        this.mPlaybackView.onPause();
        this.mStarted = false;
    }

    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
    }

    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        this.isResumed = true;
        this.mPlaybackView.onResume();
        startPlayback();
        if (this.mAdView != null) {
            this.mAdView.resume();
        }
    }

    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
        releaseResources();
        if (this.mAdView != null) {
            this.mAdView.pause();
        }
    }

    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
    }

    protected void onDestroy() {
        if (this.mAdView != null) {
            this.mAdView.destroy();
        }
        super.onDestroy();
        Log.d(LOG_TAG, "OnDestroy");
    }
}
