/*
 * Copyright (C) 2010 ZXing authors
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

package com.wc.qbar.scan.decode;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;


import com.tencent.qbar.QbarNative;
import com.tencent.qbar.QbarNativeUtils;
import com.wc.qbar.R;
import com.wc.qbar.scan.CaptureActivity;

import java.util.List;

public final class DecodeHandler extends Handler {
    //上次记录的索引
    private int darkIndex = 0;
    //一个历史记录的数组，255是代表亮度最大值
    private long[] darkList = new long[]{255, 255, 255, 255};
    //亮度低的阀值
    private int darkValue = 60;
    private long lastTime = 0;

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final CaptureActivity activity;
    private boolean running = true;
    private byte[] gph;
    byte[] gpi;

    DecodeHandler(CaptureActivity activity) {
        this.activity = activity;
    }

    @Override
    public void handleMessage(@NonNull Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        long start = System.currentTimeMillis();
        //光暗检测（控制闪光灯开关）
        if (lastTime == 0) {
            lastTime = start;
        } else if (start - lastTime > 500) {//
            lastTime = start;
            //像素点的总亮度
            long pixelLightCount = 0L;
            //像素点的总数
            long pixeCount = width * height;
            //采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
            int step = 10;
            if (Math.abs(data.length - pixeCount * 1.5f) < 0.00001f) {
                for (int i = 0; i < pixeCount; i += step) {
                    //如果直接加是不行的，因为data[i]记录的是色值并不是数值，byte的范围是+127到—128，
                    // 而亮度FFFFFF是11111111是-127，所以这里需要先转为无符号unsigned long参考Byte.toUnsignedLong()
                    pixelLightCount += ((long) data[i]) & 0xffL;
                }
            }
            //平均亮度
            long cameraLight = pixelLightCount / (pixeCount / step);
            //更新历史记录
            int lightSize = darkList.length;
            darkList[darkIndex = darkIndex % lightSize] = cameraLight;
            darkIndex++;
            boolean isDarkEnv = true;
            //判断在时间范围waitScanTime * lightSize内是不是亮度过暗
            for (int i = 0; i < lightSize; i++) {
                if (darkList[i] > darkValue) {
                    isDarkEnv = false;
                }
            }
            Handler handler = activity.getHandler();
            Message message = Message.obtain(handler,
                    R.id.decode_environment, isDarkEnv);
            message.sendToTarget();
        }

        //wechat解码逻辑
        if (data == null) {
            Log.e("DecodeHandler", "scan, wrong data, data is null");
            return;
        }
        int cameraRotation = 0;
        int[] localObject1 = new int[2];
        localObject1[0] = width;
        localObject1[1] = height;
        this.gph = new byte[width * height * 3 / 2];
        this.gpi = new byte[width * height];
        int grayRotate = QbarNative.grayRotateCropSub(this.gph, localObject1, data, width, height, height / 2 + 50, 170, 732, 732, cameraRotation);
        Log.e("DecodeHandler", "grayRotate = " + grayRotate);
        if (grayRotate == 0) {
            if (this.gph.length != width * height * 3 / 2) {
                this.gph = null;
                this.gph = new byte[width * height * 3 / 2];
                this.gpi = null;
                this.gpi = new byte[width * height];
                Log.d("DecodeHandler", "tempOutBytes size change, new byte " + this.gph.length);
            }
        }
        System.arraycopy(this.gph, 0, this.gpi, 0, this.gpi.length);
        if (this.gpi != null) {
            List<QbarNative.QBarResult> list = QbarNativeUtils.scanImage(this.gpi, localObject1[0], localObject1[1]);
            Handler handler = activity.getHandler();
            if (list.size() > 0) {
                if (handler != null) {
                    Message message = Message.obtain(handler,
                            R.id.decode_succeeded, list.get(0));
                    Bundle bundle = new Bundle();
                    message.setData(bundle);
                    message.sendToTarget();
                }
            } else {
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_failed);
                    message.sendToTarget();
                }
            }
        }
    }
}
