package com.example.debugdetect.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import com.dailystudio.app.utils.TextUtils;
import com.example.debugdetect.R;
import com.example.debugdetect.api.Classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageDetection {
    private static final String TAG = "Image Detection";
    private final static int[] FRAME_COLORS = {
            R.color.md_red_400,
            R.color.md_orange_400,
            R.color.md_amber_400,
            R.color.md_green_400,
            R.color.md_teal_400,
            R.color.md_blue_400,
            R.color.md_purple_400,
    };

    public static Bitmap tagRecognitionOnBitmap(Context context, Bitmap origBitmap, List<Classifier.Recognition> recognitions) {
        if (context == null
                || origBitmap == null
                || recognitions == null
                || recognitions.size() <= 0) {
            return origBitmap;
        }

        final Resources res = context.getResources();
        if (res == null) {
            return origBitmap;
        }

        origBitmap = origBitmap.copy(Bitmap.Config.ARGB_8888, true);
        final int width = origBitmap.getWidth();
        final int height = origBitmap.getHeight();

        final Canvas canvas = new Canvas(origBitmap);

        final int corner = res.getDimensionPixelSize(R.dimen.detect_info_round_corner);

        final int N = recognitions.size();
        int colorIndex = 0;

        final Paint framePaint = new Paint();
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(10.0f);

        Classifier.Recognition r;
        for (int i = 0; i < N; i++) {
            r = recognitions.get(i);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                framePaint.setColor(res.getColor(FRAME_COLORS[colorIndex],
                        context.getTheme()));
            }

            final RectF location = r.getLocation();
            final String objectName = r.getTitle();
            canvas.drawRoundRect(location, corner, corner, framePaint);

            colorIndex++;
            if (colorIndex >= FRAME_COLORS.length) {
                colorIndex = 0;
            }

            Log.i(TAG, "found object " + objectName + " in " + location);
        }

        final Paint textPaint = new Paint();
        int legendFontSize = res.getDimensionPixelSize(R.dimen.detect_info_font_size);
        int legendIndSize = res.getDimensionPixelSize(R.dimen.detect_info_font_size);
        int legendFramePadding = res.getDimensionPixelSize(R.dimen.detect_info_padding);
        textPaint.setTextSize(legendFontSize);

        RectF legendIndFrame = new RectF();

        final float legendTextWidth = width * .4f;
        final float legendHeight = legendIndSize * 1.2f;
        final float legendEnd = width * .5f;
//        final float legendIndStart = legendEnd - legendTextWidth - legendIndSize * 1.2f;
        final float legendIndStart = legendEnd - legendTextWidth - legendIndSize * 1.2f;
        float legendBottom = height * .95f;
        float legendTop = legendBottom - colorIndex * legendHeight;

        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            framePaint.setColor(res.getColor(R.color.semi_black,
                    context.getTheme()));
        }

        canvas.drawRoundRect(legendIndStart - legendFramePadding, legendTop - legendFramePadding,
                legendEnd + legendFramePadding, legendBottom + legendFramePadding, corner, corner, framePaint);
        float legendTextStart;
        float legendTextTop;
        float legendIndTop;

        colorIndex = 0;

        String legendText;
        for (int i = 0; i < N; i++) {
            r = recognitions.get(i);

            legendIndTop = legendTop + 0.1f * legendIndSize;
            legendTextStart = legendEnd - legendTextWidth;
            legendTextTop = legendIndTop + (legendIndSize - (textPaint.descent() + textPaint.ascent())) / 2;

            legendIndFrame.set(legendIndStart, legendIndTop,
                    legendIndStart + legendIndSize, legendIndTop + legendIndSize);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textPaint.setColor(res.getColor(FRAME_COLORS[colorIndex],
                        context.getTheme()));
            }

            legendText = String.format("%s (%3.1f%%)",
                    TextUtils.capitalize(r.getTitle()),
                    r.getConfidence() * 100);
            canvas.drawRoundRect(legendIndFrame, corner, corner, textPaint);
            canvas.drawText(legendText, legendTextStart, legendTextTop,
                    textPaint);
            colorIndex++;
            if (colorIndex >= FRAME_COLORS.length) {
                colorIndex = 0;
            }

            legendTop += legendHeight;
        }

        return origBitmap;
    }

    public static ArrayList<Classifier.Recognition> getSortedRecognitionArrayList (ArrayList<Classifier.Recognition> recognitionArrayList) {
        ArrayList<Classifier.Recognition> tempRecognitionArrayList = new ArrayList<>();
        tempRecognitionArrayList.addAll(recognitionArrayList);

        Collections.sort(tempRecognitionArrayList, new Comparator<Classifier.Recognition>() {
            @Override
            public int compare(Classifier.Recognition o1, Classifier.Recognition o2) {
                int o1Left = (int) Math.floor(o1.getLocation().left);
                int o1Right = (int) Math.floor(o1.getLocation().right);

                int o2Left = (int) Math.floor(o2.getLocation().left);
                int o2Right = (int) Math.floor(o2.getLocation().right);
                return (o1Left + o1Right) / 2 - (o2Left + o2Right) / 2;
            }
        });
        return tempRecognitionArrayList;
    }

    public static ArrayList<Classifier.Recognition> filterSimiliarBoundingBox(ArrayList<Classifier.Recognition> recognitionArrayList) {
        ArrayList<Classifier.Recognition> tempRecognitionArrayList = new ArrayList<>();
        tempRecognitionArrayList.addAll(recognitionArrayList);

        for (int i = 0; i < tempRecognitionArrayList.size() - 1; i++) {
            if (isSameBoundingBox(tempRecognitionArrayList.get(i), tempRecognitionArrayList.get(i + 1))) {
                tempRecognitionArrayList.remove(i);
                i--;
            }
        }
        return tempRecognitionArrayList;
    }

    public static boolean isSameBoundingBox(Classifier.Recognition r1, Classifier.Recognition r2) {
        RectF r1Coordinate = r1.getLocation();
        RectF r2Coordinate = r2.getLocation();

        if (r1Coordinate.contains(r2Coordinate) || r2Coordinate.contains(r1Coordinate)) return true;

        if (RectF.intersects(r1Coordinate, r2Coordinate)) {
            float areaR1 = r1Coordinate.width() * r1Coordinate.height();
            float areaR2 = r2Coordinate.width() * r2Coordinate.height();

            RectF intersectResult = new RectF();
            intersectResult.setIntersect(r1Coordinate, r2Coordinate);

            float areaIntersect = intersectResult.width() * intersectResult.height();
            float areaUnion = areaR1 + areaR2 - areaIntersect;

            float iou = areaIntersect / areaUnion;
            float iouR1 = areaIntersect / areaR1;
            float iouR2 = areaIntersect / areaR2;

            Log.d("intersect", iou + "");

            return iou >= 0.8 || iouR1 >= 0.8 || iouR2 >= 0.8;
        }
        return false;
    }
}
