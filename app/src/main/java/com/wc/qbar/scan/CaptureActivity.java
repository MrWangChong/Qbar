package com.wc.qbar.scan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;


import com.tencent.qbar.QbarNative;
import com.tencent.qbar.QbarNativeUtils;
import com.wc.qbar.R;
import com.wc.qbar.scan.camera.CameraManager;
import com.wc.qbar.scan.view.ViewfinderView;
import com.wc.utils.ToastUtils;

import java.io.IOException;

/**
 * 这个activity打开相机，在后台线程做常规的扫描；它绘制了一个结果view来帮助正确地显示条形码，在扫描的时候显示反馈信息，
 * 然后在扫描成功的时候覆盖扫描结果
 */
public final class CaptureActivity extends AppCompatActivity implements
        SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    // 相机控制
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    // 电量控制
    private InactivityTimer inactivityTimer;
    // 声音、震动控制
    private BeepManager beepManager;
    private TextView tv_flash_mode;

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }


    /**
     * OnCreate中初始化一些辅助类，如InactivityTimer（休眠）、Beep（声音）以及AmbientLight（闪光灯）
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // 保持Activity处于唤醒状态
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.capture);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        findViewById(R.id.iv_close).setOnClickListener(v -> finish());
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        tv_flash_mode = findViewById(R.id.tv_flash_mode);
        tv_flash_mode.setOnClickListener(v -> {
            if (cameraManager.isOpenFlash()) {
                cameraManager.closeFlash();
            } else {
                cameraManager.openFlash();
            }
            setImage();
        });
        QbarNativeUtils.getInstance().initContext(this);
    }

    private void setImage() {
        if (cameraManager.isOpenFlash()) {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.icon_scan_guang_open);
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                tv_flash_mode.setCompoundDrawables(null, drawable, null, null);
            }
            tv_flash_mode.setText("轻触关闭");
        } else {
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.icon_scan_guang);
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                tv_flash_mode.setCompoundDrawables(null, drawable, null, null);
            }
            tv_flash_mode.setText("轻触照亮");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewfinderView.start();
        // CameraManager必须在这里初始化，而不是在onCreate()中。
        // 这是必须的，因为当我们第一次进入时需要显示帮助页，我们并不想打开Camera,测量屏幕大小
        // 当扫描框的尺寸不正确时会出现bug
        cameraManager = new CameraManager(getApplication());
        handler = null;
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // activity在paused时但不会stopped,因此surface仍旧存在；
            // surfaceCreated()不会调用，因此在这里初始化camera
            initCamera(surfaceHolder);
        } else {
            // 重置callback，等待surfaceCreated()来初始化camera
            surfaceHolder.addCallback(this);
        }

        beepManager.updatePrefs();
        inactivityTimer.onResume();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
        }
        viewfinderView.pause();
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        viewfinderView.destroy();
        QbarNativeUtils.getInstance().releaseQbarNative();
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    public void adjustFocus(QbarNative.QBarCodeDetectInfo info, QbarNative.QBarPoint point) {
        if (info != null && point != null && cameraManager != null) {
            //左上右下,(x0,y0) (x1,y1) (x2,y2) (x3,y3)
//            if (info.prob > 0.2f && isInRect(viewfinderView.getScanRect(), point)) {
//                cameraManager.startSmoothZoom(30);
//            }
            if (info.prob > 0.2f) {
                float f = (point.x1 - point.x0) / 720f;
                float y = (point.y3 - point.y0) / 720f;
                if (f < 0.3 && y < 0.3) {
                    double d = Math.min((0.3 - f), (0.3 - y));
                    cameraManager.startSmoothZoom((int) (30 * d * 10));
                    resetTiming();
//                    Log.d("adjustFocus", "f = " + f);
//                    Log.d("adjustFocus", "y = " + y);
                }
            }
        }
    }

    private int resetTime = 8;
    private boolean isRunning;
    private Runnable resetRunnable = () -> {
        while (resetTime > 0 && isRunning) {
            try {
                resetTime--;
                Thread.sleep(1000);
                if (resetTime == 0 && isRunning) {
                    isRunning = false;
                    runOnUiThread(() -> {
                        if (cameraManager != null) {
                            cameraManager.startSmoothZoom(-1);
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    //8秒重置摄像头焦距
    private synchronized void resetTiming() {
        resetTime = 8;
        if (!isRunning) {
            isRunning = true;
            new Thread(resetRunnable).start();
        }
    }

    /**
     * 扫描成功，处理反馈信息
     */
    public void handleDecode(QbarNative.QBarResult rawResult) {
        inactivityTimer.onActivity();
        //这里处理解码完成后的结果，此处将参数回传到Activity处理
        isRunning = false;
        if (rawResult != null) {
            ToastUtils.showToast(CaptureActivity.this, "二维码扫描成功");
            beepManager.playBeepSoundAndVibrate();
            String content = rawResult.data;
            Intent intent = getIntent();
            intent.putExtra("codedContent", content);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    /**
     * 扫描环境变化
     */
    public void handleDecode(boolean isDarkEnv) {
        if (!cameraManager.isOpenFlash()) {
            if (isDarkEnv) {
                tv_flash_mode.setVisibility(View.VISIBLE);
            } else {
                tv_flash_mode.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 初始化Camera
     *
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    /**
     * 显示底层错误信息并退出应用
     */
    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("对不起，Android相机遇到了一个问题。您可能需要重新启动设备。");
        builder.setPositiveButton("OK", new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
//                case ImageUtils.IMAGE:// 从相册返回的数据
//                    if (data != null) {
//                        handleAlbumPic(data.getData());
//                    }
//                    break;
            }
        }
    }

    /**
     * 处理选择的图片
     */
    private void handleAlbumPic(final Uri uri) {
        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                bitmap = QbarNativeUtils.getSmallerBitmap(bitmap);
                String codedContent = QbarNativeUtils.decodeImage(bitmap);
                //获取选中图片的路径
                runOnUiThread(() -> {
                    ToastUtils.showToast(CaptureActivity.this, "二维码识别成功");
                    Intent intent = getIntent();
                    intent.putExtra("codedContent", codedContent);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
//            String codedContent = ZxingUtils.decodeQRImage(CaptureActivity.this, uri);
        }).start();
    }
}
