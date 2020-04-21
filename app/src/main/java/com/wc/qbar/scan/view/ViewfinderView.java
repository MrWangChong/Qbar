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

package com.wc.qbar.scan.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.wc.qbar.R;
import com.wc.qbar.scan.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points. 这是一个位于相机顶部的预览view,它增加了一个外部部分透明的取景框，以及激光扫描动画和结果组件
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {
    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192,
            128, 64};
    private static final long ANIMATION_DELAY = 30L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private static final int FREQUENCY = 3;
    private int frequency_num = 0;

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor; // 取景框外的背景颜色
    private final int resultColor;// result Bitmap的颜色
    private final int laserColor; // 红色扫描线的颜色
    private final int resultPointColor; // 特征点的颜色
    private final int statusColor; // 提示文字颜色
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    // 扫描线移动的y
    private int scanLineTop;
    // 扫描线移动速度
    private final int SCAN_VELOCITY = 8;
    // 扫描线
    private Bitmap scanLight;
    private int scanLightHeight;
    private Bitmap scanBg;
    private int scanBgHeight;
    private int measuredHeight, measuredWidth;
    private Rect scanRect;
    private Rect lineRect;
    private ValueAnimator mAnimator;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every
        // time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        statusColor = resources.getColor(R.color.status_text);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<ResultPoint>(5);
        lastPossibleResultPoints = null;
        scanLight = BitmapFactory.decodeResource(resources,
                R.drawable.scan_light);
        scanBg = BitmapFactory.decodeResource(resources,
                R.drawable.icon_scan_bg);
        scanBgHeight = scanBg.getHeight();
        scanLightHeight = (int) ((scanBgHeight + scanBg.getHeight() - 8) * 0.754f);
        mAnimator = ValueAnimator.ofInt(0, scanBgHeight + scanBg.getHeight() + 500);
        mAnimator.setDuration(2600);
        mAnimator.setStartDelay(1000);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.addUpdateListener(animation -> {
            if (scanRect != null) {
                scanLineTop = (scanRect.top - scanLightHeight - 150) + (int) animation.getAnimatedValue();
                postInvalidate(scanRect.left, scanRect.top, scanRect.right, scanRect.bottom);
            }
        });
        scanLineTop = -scanBgHeight;
    }

//    public void setCameraManager(CameraManager cameraManager) {
//        this.cameraManager = cameraManager;
//    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measuredWidth = getMeasuredWidth();
        measuredHeight = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (measuredWidth != 0 && measuredHeight != 0) {
            if (scanBg != null) {
                if (scanRect == null) {
                    int left = (measuredWidth - scanBgHeight) / 2;
                    int top = (measuredHeight - scanBgHeight) / 2;
                    scanRect = new Rect(left, top, left + scanBgHeight,
                            top + scanBgHeight);
                }
                paint.setColor(resultBitmap != null ? resultColor : maskColor);
                canvas.drawRect(0, 0, measuredWidth, scanRect.top + 6, paint);// Rect_1
                canvas.drawRect(0, scanRect.top + 6, scanRect.left + 6, scanRect.bottom - 6, paint); // Rect_2
                canvas.drawRect(scanRect.right - 6, scanRect.top + 6, measuredWidth, scanRect.bottom - 6, paint); // Rect_3
                canvas.drawRect(0, scanRect.bottom - 6, measuredWidth, measuredHeight, paint); // Rect_4
                paint.setColor(Color.WHITE);
                canvas.drawBitmap(scanBg, null, scanRect, paint);
            }
            if (scanLight != null && scanRect != null) {
                int top = scanLineTop < scanRect.top ? scanRect.top : scanLineTop;
                int bottom = scanLineTop + scanLightHeight;
                if (lineRect == null) {
                    lineRect = new Rect(scanRect.left + 7, top, scanRect.right - 7,
                            bottom > scanRect.bottom ? scanRect.bottom : bottom);
                } else {
                    lineRect.set(scanRect.left + 7, top + 7, scanRect.right - 7, bottom > scanRect.bottom ? scanRect.bottom - 7 : bottom - 7);
                }
                canvas.drawBitmap(scanLight, null, lineRect, paint);
            }
        }
    }

    public void pause() {
        if (mAnimator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mAnimator.pause();
            } else {
                mAnimator.cancel();
            }
        }
    }

    public void start() {
        if (mAnimator != null) {
            mAnimator.start();
        }
    }

    public void destroy() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        scanBg.recycle();
        scanBg = null;
        scanLight.recycle();
        scanLight = null;
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
