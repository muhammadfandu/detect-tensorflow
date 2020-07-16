package com.example.debugdetect.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import com.dailystudio.development.Logger;
import com.example.debugdetect.util.ImageUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ObjectDetectionModel {

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 1024;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_MODEL_FILE = "detect.tflite";
    private static final String TF_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    private static Classifier sNestleDetector = null;

    private static Bitmap croppedBitmap = null;

    private static Matrix frameToCropTransform;
    private static Matrix cropToFrameTransform;


    public synchronized static boolean isInitialized() {
        return (sNestleDetector != null);
    }

    public static boolean initialize(Context context) {
        if (context == null) {
            return false;
        }

        boolean success = false;
        try {
            sNestleDetector = TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_MODEL_FILE,
                            TF_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);

            success = true;
        } catch (final IOException e) {
            Logger.error("Initializing classifier failed: %s", e.toString());
            Log.d("LOG", "ObjectDetectionModel-initialize: error initializing");

            sNestleDetector = null;
            success = false;
        }

        croppedBitmap = Bitmap.createBitmap(
                TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                Bitmap.Config.ARGB_8888);

        return success;
    }

    public static List<Classifier.Recognition> detectImage(Bitmap bitmap) {
        return detectImage(bitmap, 0);
    }

    public static List<Classifier.Recognition> detectImage(Bitmap bitmap, float minimumConfidence) {
        Log.d("LOG", "ObjectDetectionModel-initialize: start detectImage");

        if (bitmap == null) {
            Log.d("LOG", "ObjectDetectionModel-initialize: object bitmap is null");

            return null;
        }

        if (minimumConfidence <= 0
                || minimumConfidence > 1) {
            minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        }

        if (!ObjectDetectionModel.isInitialized()) {
            Logger.warn("object detection model is NOT initialized yet.");
            Log.d("LOG", "ObjectDetectionModel-initialize:object detection model is NOT initialized yet.");

            return null;
        }

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        // change maintainAspectRatio to false so the image doesnt cropped
        frameToCropTransform = ImageUtils.getTransformationMatrix(
                        width, height,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        0, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(bitmap, frameToCropTransform, null);

        List<Classifier.Recognition> results = new LinkedList<>();
        results = sNestleDetector.recognizeImage(croppedBitmap);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<>();

        for (final Classifier.Recognition recognition: results) {
            final RectF loc = recognition.getLocation();
            if (loc != null && recognition.getConfidence() >= minimumConfidence) {
                cropToFrameTransform.mapRect(loc);
                recognition.setLocation(loc);
                mappedRecognitions.add(recognition);
                Logger.debug("add satisfied recognition: %s", recognition);
            } else {
                Logger.warn("skip unsatisfied recognition: %s", recognition);
            }
        }

        return mappedRecognitions;
    }
}