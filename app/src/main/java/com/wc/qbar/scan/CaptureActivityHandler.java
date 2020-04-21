/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wc.qbar.scan;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.util.Log;

import com.tencent.qbar.QbarNative;
import com.tencent.qbar.QbarNativeUtils;
import com.wc.qbar.R;
import com.wc.qbar.scan.camera.CameraManager;
import com.wc.qbar.scan.decode.DecodeThread;


/**
 * This class handles all the messaging which comprises the state machine for
 * capture. 该类用于处理有关拍摄状态的所有信息
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {

    private static final String TAG = CaptureActivityHandler.class
            .getSimpleName();

    private final CaptureActivity activity;
    private final DecodeThread decodeThread;
    private State state;
    private final CameraManager cameraManager;
    private long mLastFocusTime;


    private enum State {
        PREVIEW, SUCCESS, DONE
    }

    public CaptureActivityHandler(CaptureActivity activity, CameraManager cameraManager) {
        this.activity = activity;
        decodeThread = new DecodeThread(activity);
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        // 开始拍摄预览和解码
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.restart_preview) {
            // 重新预览
            restartPreviewAndDecode();
        } else if (message.what == R.id.decode_environment) {
            activity.handleDecode((Boolean) message.obj);
        } else if (message.what == R.id.decode_succeeded) {
            // 解码成功
            state = State.SUCCESS;
            activity.handleDecode((QbarNative.QBarResult) message.obj);
        } else if (message.what == R.id.decode_failed) {
            //有二维码元素拉近镜头
            if (mLastFocusTime == 0) {
                mLastFocusTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - mLastFocusTime > 2000 && QbarNativeUtils.getInstance().getQBarCodeDetectInfos().size() > 0) {
                    if (QbarNativeUtils.getInstance().getQBarPoints().size() > 0) {
                        activity.adjustFocus(QbarNativeUtils.getInstance().getQBarCodeDetectInfos().get(0), QbarNativeUtils.getInstance().getQBarPoints().get(0));
                        mLastFocusTime = System.currentTimeMillis();
                    }
                }
            }
            // We're decoding as fast as possible, so when one decode fails,
            // start another.
            // 尽可能快的解码，以便可以在解码失败时，开始另一次解码
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(),
                    R.id.decode);
        } else if (message.what == R.id.return_scan_result) {
            //扫描结果，返回CaptureActivity处理
            activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
            activity.finish();
        } else if (message.what == R.id.launch_product_query) {
            String url = (String) message.obj;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setData(Uri.parse(url));

            ResolveInfo resolveInfo = activity.getPackageManager()
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            String browserPackageName = null;
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                browserPackageName = resolveInfo.activityInfo.packageName;
                Log.d(TAG, "Using browser in package " + browserPackageName);
            }
            // Needed for default Android browser / Chrome only apparently
            //需要默认的Android浏览器或者Google
            if ("com.android.browser".equals(browserPackageName)
                    || "com.android.chrome".equals(browserPackageName)) {
                intent.setPackage(browserPackageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID,
                        browserPackageName);
            }
            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                Log.w(TAG, "Can't find anything to handle VIEW of URI " + url);
            }
        }
    }

    /**
     * 完全退出
     */
    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause()
            // will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        //确保不会发送任何队列消息
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    public void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(),
                    R.id.decode);
            activity.drawViewfinder();
        }
    }
}
