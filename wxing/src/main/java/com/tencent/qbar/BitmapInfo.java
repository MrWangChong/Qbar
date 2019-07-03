package com.tencent.qbar;

import android.graphics.Bitmap;

public class BitmapInfo {
    public int height;
    public int width;
    private static int nOM = 10;
    private byte[] nOL;

    public BitmapInfo(Bitmap paramBitmap, int paramInt1, int paramInt2) {
        width = paramBitmap.getWidth() - paramInt1;
        height = paramBitmap.getHeight() - paramInt2;
        int[] arrayOfInt = new int[width * height];
        paramBitmap.getPixels(arrayOfInt, 0, width, paramInt1 / 2, paramInt2 / 2, width, height);
        this.nOL = new byte[width * height];
        int a = 0;
        while (a < height) {
            int k = a * width;
            int b = 0;
            while (b < width) {
                int i1 = arrayOfInt[(k + b)];
                int m = i1 >> 16 & 0xFF;
                int n = i1 >> 8 & 0xFF;
                i1 &= 0xFF;
                if ((m == n) && (n == i1)) {
                    this.nOL[(k + b)] = ((byte) m);
                }else {
                    this.nOL[(k + paramInt2)] = ((byte)(i1 + (m + n + n) >> 2));
                }
                b += 1;
            }
            a += 1;
        }
    }

    public final byte[] byj() {
        return this.nOL;
    }

    public final byte[] n(int paramInt, byte[] paramArrayOfByte) {
        if ((paramInt < 0) || (paramInt >= this.height)) {
            throw new IllegalArgumentException("Requested row is outside the image: " + paramInt);
        }
        int i = this.width;
        byte[] arrayOfByte;
        if (paramArrayOfByte != null) {
            arrayOfByte = paramArrayOfByte;
            if (paramArrayOfByte.length >= i) {
            }
        } else {
            arrayOfByte = new byte[i];
        }
        System.arraycopy(this.nOL, paramInt * i, arrayOfByte, 0, i);
        return arrayOfByte;
    }

//    public String toString() {
//        byte[] arrayOfByte = new byte[this.width];
//        StringBuilder localStringBuilder = new StringBuilder(this.height * (this.width + 1));
//        int i = 0;
//        int j;
//        for (; ; ) {
//            if (i >= this.height) {
//                return localStringBuilder.toString();
//            }
//            arrayOfByte = n(i, arrayOfByte);
//            j = 0;
//            if (j < this.width) {
//                break;
//            }
//            localStringBuilder.append('\n');
//            i += 1;
//        }
//        int k = arrayOfByte[j] & 0xFF;
//        char c;
//        if (k < 64) {
//            c = '#';
//        }
//        for (; ; ) {
//            localStringBuilder.append(c);
//            j += 1;
//            break;
//            if (k < 128) {
//                c = '+';
//            } else if (k < 192) {
//                c = '.';
//            } else {
//                c = ' ';
//            }
//        }
//    }
}

