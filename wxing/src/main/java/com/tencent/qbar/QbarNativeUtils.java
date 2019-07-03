package com.tencent.qbar;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

public class QbarNativeUtils {
    private static QbarNativeUtils sQbarNativeUtils;
    private static int sFlag = -1;//-1空闲中，0扫码中，1解码中
    private QbarNative mQbarNative;
    private List<QbarNative.QBarCodeDetectInfo> mQBarCodeDetectInfos;
    private List<QbarNative.QBarPoint> mQBarPoints;
    private Context mContext;

    private QbarNativeUtils() {

    }

    public static synchronized QbarNativeUtils getInstance() {
        if (sQbarNativeUtils == null) {
            sQbarNativeUtils = new QbarNativeUtils();
        }
        return sQbarNativeUtils;
    }

    public void initContext(Context context) {
        if (context instanceof Application) {
            mContext = context;
        } else {
            mContext = context.getApplicationContext();
        }
    }

    public synchronized List<QbarNative.QBarCodeDetectInfo> getQBarCodeDetectInfos() {
        if (mQBarCodeDetectInfos == null) {
            mQBarCodeDetectInfos = new ArrayList<>();
        }
        return mQBarCodeDetectInfos;
    }

    public synchronized List<QbarNative.QBarPoint> getQBarPoints() {
        if (mQBarPoints == null) {
            mQBarPoints = new ArrayList<>();
        }
        return mQBarPoints;
    }


    public synchronized QbarNative getScanQbarNative() {
        if (mQbarNative == null) {
            mQbarNative = new QbarNative();
            int init = mQbarNative.init(0, "ANY", "UTF-8", QbarNative.getAiModelParam(mContext));
            Log.e("QbarNativeUtils", "QbarNative.Init = " + init + " version = " + QbarNative.getVersion());
            int[] arrayOfInt = new int[5];
            arrayOfInt[0] = 4;
            arrayOfInt[1] = 1;
            arrayOfInt[2] = 5;
            arrayOfInt[3] = 3;
            arrayOfInt[4] = 2;
            int readers = mQbarNative.setReaders(arrayOfInt, arrayOfInt.length);
            Log.d("QbarNativeUtils", "readers = " + readers);
            sFlag = 0;
        }
        return mQbarNative;
    }

    private synchronized QbarNative getDecodeQbarNative() {
        if (mQbarNative == null) {
            mQbarNative = new QbarNative();
            int init = mQbarNative.init(0, "ANY", "UTF-8", QbarNative.getAiModelParam(mContext));
            Log.e("QbarNativeUtils", "QbarNative.Init = " + init + " version = " + QbarNative.getVersion());
            int[] arrayOfInt = new int[4];
            arrayOfInt[0] = 4;
            arrayOfInt[1] = 1;
            arrayOfInt[2] = 3;
            arrayOfInt[3] = 2;
            int readers = mQbarNative.setReaders(arrayOfInt, arrayOfInt.length);
            Log.d("QbarNativeUtils", "readers = " + readers);
            sFlag = 1;
        }
        return mQbarNative;
    }

    public synchronized void releaseQbarNative() {
        if (mQbarNative != null) {
            if (mQBarCodeDetectInfos != null) {
                mQBarCodeDetectInfos.clear();
            }
            if (mQBarPoints != null) {
                mQBarPoints.clear();
            }
            mQbarNative.release();
            mQbarNative = null;
            sFlag = -1;
        }
    }

    public synchronized static List<QbarNative.QBarResult> scanImage(byte[] data, int width, int height) {
        if (sFlag == -1 || sFlag == 0) {
            List<QbarNative.QBarResult> list = QbarNativeUtils.getInstance().getScanQbarNative().scanImage(data, width, height);
            System.currentTimeMillis();
            if (list.size() <= 0) {
                QbarNativeUtils.getInstance().getScanQbarNative().getCodeDetectInfo(QbarNativeUtils.getInstance().getQBarCodeDetectInfos(), QbarNativeUtils.getInstance().getQBarPoints());
            }
            return list;
        } else {
            return new ArrayList<>();
        }
    }

    public static String decodeImage(Bitmap paramBitmap) {
        QbarNativeUtils.getInstance().releaseQbarNative();
        int paramInt = 3;
        BitmapInfo bitmapInfo = new BitmapInfo(paramBitmap, paramInt, paramInt);
        if ((bitmapInfo.byj() == null) || (bitmapInfo.width <= 0) || (bitmapInfo.height <= 0)) {
            return "";
        }
        List<QbarNative.QBarResult> list = QbarNativeUtils.getInstance().getDecodeQbarNative()
                .scanImage(bitmapInfo.byj(), bitmapInfo.width, bitmapInfo.height);
        String data = "";
        if (list.size() > 0) {
            data = list.get(0).data;
        }
        QbarNativeUtils.getInstance().releaseQbarNative();
        return data;
    }

    public static Bitmap getSmallerBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() * bitmap.getHeight() * 8 * 4L > 268435456L) {
            Matrix matrix = new Matrix();
            matrix.postScale(1 / 4f, 1 / 4f);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    /***
     * 获取url 指定name的value;
     * @param url 链接
     * @param name 名字
     */
    public static String getValueByName(String url, String name) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        String result = "";
        int index = url.indexOf("?");
        String temp = url.substring(index + 1);
        String[] keyValue = temp.split("&");
        for (String str : keyValue) {
            if (str.contains(name)) {
                result = str.replace(name + "=", "");
                break;
            }
        }
        return result;
    }
}
