package com.example.debugdetect.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapProcessor {
    public static Bitmap getThumbnailImage(Bitmap image) {
        return downScaleImage(image, 160);
    }

    public static Bitmap downScaleImage(Bitmap orgImage, int maxSize){
        int outWidth;
        int outHeight;
        int inWidth = orgImage.getWidth();
        int inHeight = orgImage.getHeight();
        if(inWidth > inHeight){
            outWidth = maxSize;
            outHeight = (inHeight * maxSize) / inWidth;
        } else {
            outHeight = maxSize;
            outWidth = (inWidth * maxSize) / inHeight;
        }

        Bitmap imCompress = Bitmap.createScaledBitmap(orgImage, outWidth, outHeight, false);
        return imCompress;
    }

    public static File saveBitmap(Bitmap bitmap, String filename, Context context, int quality) {
        if (bitmap == null || context == null) return null;
        File root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(root, filename + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Bitmap Failed", e.getMessage());
            return null;
        }
    }

    public static File saveBitmap(Bitmap bitmap, String subfolder, String filename, Context context, int quality) {
        if (bitmap == null  || context == null) return null;
        File root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES + subfolder);
        File file = new File(root, filename + ".jpg");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Bitmap Failed", e.getMessage());
            return null;
        }
    }

    public static File saveBitmap(Bitmap bitmap, File file, int quality) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Bitmap Failed", e.getMessage());
            return null;
        }
    }

    public static Bitmap normalizeRotatedCameraPortraitResult(Bitmap srcBitmap, String imagePath) throws IOException {
        ExifInterface ei = new ExifInterface(imagePath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateImage(srcBitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateImage(srcBitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateImage(srcBitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = srcBitmap;
        }
        return rotatedBitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
