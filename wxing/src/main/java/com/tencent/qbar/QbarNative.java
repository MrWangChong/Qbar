package com.tencent.qbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class QbarNative {
    public static boolean isCopy;
    public byte[] data = new byte[3000];
    public byte[] type = new byte[100];
    public byte[] wKc = new byte[100];
    public int[] wKd = new int[4];
    public byte[] wKe = new byte[300];
    public int[] wKf = new int[2];
    public int wKg = -1;

    static {
        System.loadLibrary("QrMod");
    }

    private static native int Encode(byte[] paramArrayOfByte, int[] paramArrayOfInt, String paramString1, int paramInt1, int paramInt2, String paramString2, int paramInt3);

    private static native int EncodeBitmap(String paramString1, Bitmap paramBitmap, int paramInt1, int paramInt2, int paramInt3, int paramInt4, String paramString2, int paramInt5);

    public static native int FocusInit(int paramInt1, int paramInt2, boolean paramBoolean, int paramInt3, int paramInt4);

    public static native boolean FocusPro(byte[] paramArrayOfByte, boolean paramBoolean, boolean[] paramArrayOfBoolean);

    public static native int FocusRelease();

    private native int GetCodeDetectInfo(QBarCodeDetectInfo[] paramArrayOfQBarCodeDetectInfo, QBarPoint[] paramArrayOfQBarPoint, int paramInt);

    private native int GetDetailResults(QBarResultJNI[] paramArrayOfQBarResultJNI, QBarPoint[] paramArrayOfQBarPoint, QBarReportMsg[] paramArrayOfQBarReportMsg, int paramInt);

    private native int GetDetectInfoByFrames(QBarCodeDetectInfo paramQBarCodeDetectInfo, int paramInt);

    private native int GetOneResult(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int[] paramArrayOfInt, int paramInt);

    private native int GetOneResultReport(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, int[] paramArrayOfInt1, int[] paramArrayOfInt2, int paramInt);

    private native int GetResults(QBarResultJNI[] paramArrayOfQBarResultJNI, int paramInt);

    private static native String GetVersion();

    private native int Init(int paramInt1, int paramInt2, String paramString1, String paramString2);

    private native int Init(int paramInt1, int paramInt2, String paramString1, String paramString2, QbarAiModelParam paramQbarAiModelParam);

    public static native int QIPUtilYUVCrop(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);

    private native int Release(int paramInt);

    private native int ScanImage(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3, int paramInt4);

    private native int SetReaders(int[] paramArrayOfInt, int paramInt1, int paramInt2);

    public static native int focusedEngineForBankcardInit(int paramInt1, int paramInt2, int paramInt3, boolean paramBoolean);

    public static native int focusedEngineGetVersion();

    public static native int focusedEngineProcess(byte[] paramArrayOfByte);

    public static native int focusedEngineRelease();

    public static String getVersion() {
        return GetVersion();
    }

    private static native int nativeArrayConvert(int paramInt1, int paramInt2, byte[] paramArrayOfByte, int[] paramArrayOfInt);

    private static native int nativeCropGray2(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, int paramInt2, int paramInt3);

    private static native int nativeGrayRotateCropSub(byte[] paramArrayOfByte1, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte2, int[] paramArrayOfInt, int paramInt7, int paramInt8);

    public static native int nativeRelease();

    private static native int nativeTransBytes(int[] paramArrayOfInt, byte[] paramArrayOfByte, int paramInt1, int paramInt2);

    private static native int nativeTransPixels(int[] paramArrayOfInt, byte[] paramArrayOfByte, int paramInt1, int paramInt2);

    private static native int nativeYUVrotate(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, int paramInt2);

    private static native int nativeYUVrotateLess(byte[] paramArrayOfByte, int paramInt1, int paramInt2);

    private static native int nativeYuvToCropIntArray(byte[] paramArrayOfByte, int[] paramArrayOfInt, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6);

    public native int GetZoomInfo(QBarZoomInfo paramQBarZoomInfo, int paramInt);

    public native int SetCenterCoordinate(int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5);

    public final int init(int paramInt, String paramString1, String paramString2, QbarAiModelParam paramQbarAiModelParam) {
        this.wKg = Init(1, paramInt, paramString1, paramString2, paramQbarAiModelParam);
        if (this.wKg < 0) {
            this.wKg = Init(1, paramInt, paramString1, paramString2);
        }
        return wKg;
    }

    public static int grayRotateCropSub(byte[] paramArrayOfByte1, int[] paramArrayOfInt, byte[] paramArrayOfByte2, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6, int paramInt7) {
        if ((paramArrayOfByte1 == null) || (paramArrayOfByte2 == null)) {
            return -1;
        }
        return nativeGrayRotateCropSub(paramArrayOfByte2, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6, paramArrayOfByte1, paramArrayOfInt, paramInt7, 0);
    }

    public static int toYuv(byte[] paramArrayOfByte, int[] paramArrayOfInt, int paramInt1, int paramInt2, int paramInt3, int paramInt4, int paramInt5, int paramInt6) {
        if (paramArrayOfByte == null) {
            return -1;
        }
        return nativeYuvToCropIntArray(paramArrayOfByte, paramArrayOfInt, paramInt1, paramInt2, paramInt3, paramInt4, paramInt5, paramInt6);
    }

    public final int setReaders(int[] paramArrayOfInt, int paramInt) {
        return SetReaders(paramArrayOfInt, paramInt, this.wKg);
    }

    public final List<QBarResult> scanImage(byte[] paramArrayOfByte, int paramInt1, int paramInt2) {
        ScanImage(paramArrayOfByte, paramInt1, paramInt2, 0, this.wKg);
        return cOp();
    }

    public final int getCodeDetectInfo(List<QBarCodeDetectInfo> paramList, List<QBarPoint> paramList1) {
        if (this.wKg < 0) {
            return 0;
        }
        QbarNative.QBarCodeDetectInfo[] arrayOfQBarCodeDetectInfo = new QbarNative.QBarCodeDetectInfo[3];
        QbarNative.QBarPoint[] arrayOfQBarPoint = new QbarNative.QBarPoint[3];
        int i = 0;
        while (i < 3) {
            arrayOfQBarCodeDetectInfo[i] = new QbarNative.QBarCodeDetectInfo();
            arrayOfQBarPoint[i] = new QbarNative.QBarPoint();
            i += 1;
        }
        paramList.clear();
        paramList1.clear();
        GetCodeDetectInfo(arrayOfQBarCodeDetectInfo, arrayOfQBarPoint, this.wKg);
        i = 0;
        while (i < 3) {
            QbarNative.QBarCodeDetectInfo localQBarCodeDetectInfo = arrayOfQBarCodeDetectInfo[i];
            if (localQBarCodeDetectInfo.readerId > 0) {
                paramList.add(localQBarCodeDetectInfo);
            }
            i += 1;
        }
        i = 0;
        while (i < 3) {
            QBarPoint qBarPoint = arrayOfQBarPoint[i];
            if (qBarPoint.point_cnt != 0) {
                paramList1.add(qBarPoint);
            }
            i += 1;
        }
        return paramList.size();
    }

    public final List<QBarResult> cOp() {
        QBarResultJNI[] arrayOfQBarResultJNI = new QBarResultJNI[3];
        int i = 0;
        while (i < 3) {
            arrayOfQBarResultJNI[i] = new QBarResultJNI();
            arrayOfQBarResultJNI[i].charset = new String();
            arrayOfQBarResultJNI[i].data = new byte[1024];
            arrayOfQBarResultJNI[i].typeName = new String();
            i += 1;
        }
        GetResults(arrayOfQBarResultJNI, this.wKg);
        ArrayList<QBarResult> localArrayList = new ArrayList<>();
        i = 0;
        while (i < 3) {
            QBarResultJNI localQBarResultJNI = arrayOfQBarResultJNI[i];
            try {
                if (localQBarResultJNI.typeName != null && localQBarResultJNI.typeName.length() > 0) {
                    QBarResult localQBarResult = new QBarResult();
                    localQBarResult.charset = localQBarResultJNI.charset;
                    localQBarResult.typeID = localQBarResultJNI.typeID;
                    localQBarResult.typeName = localQBarResultJNI.typeName;
                    localQBarResult.rawData = localQBarResultJNI.data;
                    if (localQBarResult.charset.equals("ANY")) {
                        localQBarResult.data = new String(localQBarResultJNI.data, "UTF-8");
                    } else if (localQBarResult.data != null && localQBarResult.data.length() != 0) {
                        localQBarResult.data = new String(localQBarResultJNI.data, "ASCII");
                    } else {
                        localQBarResult.data = new String(localQBarResultJNI.data, localQBarResult.charset);
                    }
                    localArrayList.add(localQBarResult);
//                    for (localQBarResult.data = new String(localQBarResultJNI.data, "ASCII"); ; localQBarResult.data = new String(localQBarResultJNI.data, localQBarResult.charset)) {
//                        localArrayList.add(localQBarResult);
//                        break;
//                    }
                    return localArrayList;
                }
            } catch (Exception e) {
                Log.e("QbarNative", "GetResults exp:" + e.getMessage());
            }
            i += 1;
        }
        return localArrayList;
    }

    public final int release() {
        int i = Release(this.wKg);
        this.wKg = -1;
        return i;
    }

    public static class QBarResultJNI {
        public String charset;
        public byte[] data;
        public int typeID;
        public String typeName;
    }

    public static class QBarCodeDetectInfo {
        public float prob;
        public int readerId;
    }

    public static class QBarPoint {
        public int point_cnt;
        public float x0;
        public float x1;
        public float x2;
        public float x3;
        public float y0;
        public float y1;
        public float y2;
        public float y3;
    }

    public static class QBarReportMsg {
        public String binaryMethod;
        public String charsetMode;
        public float decodeScale;
        public int detectTime;
        public String ecLevel;
        public int pyramidLv;
        public int qrcodeVersion;
        public String scaleList;
        public int srTime;
    }

    public static class QbarAiModelParam {
        public String detect_model_bin_path_;
        public String detect_model_param_path_;
        public String superresolution_model_bin_path_;
        public String superresolution_model_param_path_;
    }

    public static class QBarZoomInfo {
        public boolean isZoom;
        public float zoomFactor;
    }

    public class QBarResult {
        public String charset;
        public String data;
        public byte[] rawData;
        public int typeID;
        public String typeName;
    }


    public static QbarAiModelParam getAiModelParam(Context context) {
        if (!isCopy && context != null) {
            copy(context, "qbar/detect_model.bin", "detect_model.bin");
            copy(context, "qbar/detect_model.param", "detect_model.param");
            copy(context, "qbar/srnet.bin", "srnet.bin");
            copy(context, "qbar/srnet.param", "srnet.param");
            isCopy = true;
        }
        String filePath = getCodeDir();
        QbarAiModelParam qbarAiModelParam = new QbarAiModelParam();
        qbarAiModelParam.detect_model_bin_path_ = filePath + "detect_model.bin";
        qbarAiModelParam.detect_model_param_path_ = filePath + "detect_model.param";
        qbarAiModelParam.superresolution_model_bin_path_ = filePath + "srnet.bin";
        qbarAiModelParam.superresolution_model_param_path_ = filePath + "srnet.param";
        return qbarAiModelParam;
    }

    private static final String DATA_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/aimode";
    private static final String CODE_DIR = "/code/";// 头像目录

    private static String getCodeDir() {
        String path = DATA_DIRECTORY + CODE_DIR;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    private static void copy(Context context, String assets_name, String saveName) {
        String fileName = QbarNative.getCodeDir() + saveName;
        if (!(new File(fileName)).exists()) {
            try {
                InputStream is = context.getResources().getAssets().open(assets_name);
                FileOutputStream fos = new FileOutputStream(fileName);
                byte[] buffer = new byte[7168];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
