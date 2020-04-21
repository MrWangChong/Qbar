package com.wc.qbar.scan;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wc.utils.DisplayUtils;

import java.io.File;
import java.util.Hashtable;


/**
 * Created by yb on 2016/10/19.
 * 二维码生成工具
 */

public class ZxingUtils {

    /**
     * 生成二维码 要转换的地址或字符串,可以是中文
     *
     * @param url
     * @param width
     * @param height
     * @param margin 周围白边大小 px
     * @return
     */
    public static Bitmap createQRImage(String url, final int width, final int height, int margin) {
        try {
            // 判断URL合法性
            if (url == null || "".equals(url) || url.length() < 1) {
                return null;
            }
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            if (margin != -1) {
                hints.put(EncodeHintType.MARGIN, margin);
            }
            // 图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, width, height, hints);
            int[] pixels = new int[width * height];
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = 0xff000000;
                    } else {
                        pixels[y * width + x] = 0xffffffff;
                    }
                }
            }
            // 生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return cornerBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }


    //生成圆角图片
    private static Bitmap cornerBitmap(Bitmap bitmap) {
        try {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());
            final RectF rectF = new RectF(new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight()));
            final float roundPx = DisplayUtils.dip2px(8);
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            final Rect src = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());
            canvas.drawBitmap(bitmap, src, rect, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public static String decodeQRImage(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            bitmap = getSmallerBitmap(bitmap);
//            String path = getRealFilePath(context, uri);
//            Bitmap bitmap = decodeFile(path);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Log.i("DecodeFile", "bitmap width " + bitmap.getWidth() + ", bitmap height " + bitmap.getHeight());
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            QRCodeReader reader = new QRCodeReader();
            Hashtable<DecodeHintType, String> hints = new Hashtable<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF8"); //设置二维码内容的编码
            Result result = reader.decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
            bitmap.recycle();
            bitmap = null;
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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


    public static Bitmap decodeFile(String paramString) {
        if ((paramString == null) || (paramString.length() <= 0)) {
            Log.e("DecodeFile", "in decodeFile, file == null");
        }
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inJustDecodeBounds = false;
        localOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(paramString, localOptions);
        localOptions.outHeight = bitmap.getHeight();
        localOptions.outWidth = bitmap.getWidth();
        if (localOptions.outWidth * localOptions.outHeight * 8 * 4L > 268435456L) {
            Log.i("DecodeFile", "initial width " + localOptions.outWidth + ", initial height " + localOptions.outHeight);
            localOptions.inSampleSize = 4;
            bitmap = BitmapFactory.decodeFile(paramString, localOptions);
        }
        Log.i("DecodeFile", "bitmap width " + bitmap.getWidth() + ", bitmap height " + bitmap.getHeight());
        return bitmap;
    }

    public static String getRealFilePath(Context context, final Uri uri) {
        if (null == uri)
            return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }

                }
                cursor.close();
            }
            if (data == null) {
                data = getImageAbsolutePath(context, uri);
            }

        }
        return data;
    }

    public static Uri getUri(final String filePath) {
        return Uri.fromFile(new File(filePath));
    }

    /**
     * 根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换
     *
     * @param context
     * @param imageUri
     * @author yaoxing
     * @date 2014-10-12
     */
    @TargetApi(19)
    public static String getImageAbsolutePath(Context context, Uri imageUri) {
        if (context == null || imageUri == null)
            return null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(imageUri))
                return imageUri.getLastPathSegment();
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
//    public static BitmapFactory.Options YU(String paramString) {
//        if (paramString == null || paramString.length() <= 0) {
//            Log.e("MicroMsg.BitmapUtil", "getImageOptions invalid path");
//            return null;
//        }
//        try {
//            paramString = e.openRead(paramString);
//            return D(paramString);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        return new BitmapFactory.Options();
//    }
//
//    public static InputStream openRead(String paramString) {
//        BitmapFactory.Options localOptions = new BitmapFactory.Options();
//        localOptions.inJustDecodeBounds = true;
//
//        return b(aeZ(paramString), null);
//    }

    static Uri aeZ(String paramString) {
        Uri.Builder localBuilder = new Uri.Builder();
        int j = paramString.indexOf(':');
        if (j < 0) {
            localBuilder.path(paramString);
        }
        //for (; ; ) {
        localBuilder.scheme(paramString.substring(0, j));
        int k = paramString.length();
        if ((k > j + 2) && (paramString.charAt(j + 1) == '/') && (paramString.charAt(j + 2) == '/')) {
            int i = j + 3;
            while (i < k) {
                switch (paramString.charAt(i)) {
                    default:
                        i += 1;
                }
            }
            localBuilder.authority(paramString.substring(j + 3, i));
            if (i < k) {
                localBuilder.path(paramString.substring(i + 1));
            }
        } else {
            localBuilder.path(paramString.substring(j + 1));
        }
        //}
        return localBuilder.build();
    }
}
