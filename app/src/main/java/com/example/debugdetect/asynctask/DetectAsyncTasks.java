package com.example.debugdetect.asynctask;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.dailystudio.development.Logger;
import com.example.debugdetect.api.Classifier;
import com.example.debugdetect.api.ObjectDetectionModel;
import com.example.debugdetect.interfaces.AnalyzeOsaPlanoDelegate;
import com.example.debugdetect.ui.ImageDetectionEvent;
import com.example.debugdetect.util.BitmapProcessor;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static com.example.debugdetect.util.Constants.MAX_ANALYZED_IMAGE_WIDTH;
import static com.example.debugdetect.util.ImageDetection.tagRecognitionOnBitmap;

public class DetectAsyncTasks extends AsyncTask<Context, Void, List<Classifier.Recognition>> {
    private static final String TAG = "ImageDetectionStatus";
    private AnalyzeOsaPlanoDelegate callback;
    private Bitmap srcBitmap;
    private boolean readFolder;

    public DetectAsyncTasks(Bitmap srcBitmap, AnalyzeOsaPlanoDelegate callback, boolean readFolder) {
        this.srcBitmap = srcBitmap;
        this.callback = callback;
        this.readFolder = readFolder;
    }

    @Override
    protected List<Classifier.Recognition> doInBackground(Context... contexts) {
        if (contexts == null
                || contexts.length <= 0) {
            return null;
        }

        notifyDetectionState(ImageDetectionEvent.State.DECODING);
        final Context context = contexts[0];

        Bitmap bitmap;
        try {
            bitmap = BitmapProcessor.downScaleImage(srcBitmap, MAX_ANALYZED_IMAGE_WIDTH);
        } catch (OutOfMemoryError e) {
            Logger.error("decode and crop image[%s] failed: %s", e.toString());

            return null;
        }

        notifyDetectionState(ImageDetectionEvent.State.DETECTING);

        List<Classifier.Recognition> results =
                ObjectDetectionModel.detectImage(bitmap, .0f);

        notifyDetectionState(ImageDetectionEvent.State.TAGGING);

        final Bitmap tagBitmap = tagRecognitionOnBitmap(context, bitmap, results);
        if (tagBitmap != null) {

            if(readFolder){
                callback.analyzeCompletionBitmapResult(tagBitmap, results);
            }else{
                callback.analyzeCompletionBitmapResult2(tagBitmap, results);
            }

        } else {
            if(readFolder){
                callback.analyzeCompletionBitmapResult(bitmap, results);
            }else{
                callback.analyzeCompletionBitmapResult2(bitmap, results);
            }
        }

        return results;
    }



    @Override
    protected void onPostExecute(List<Classifier.Recognition> recognitions) {
        super.onPostExecute(recognitions);

        notifyDetectionState(ImageDetectionEvent.State.DONE);

        Logger.debug("recognitions: %s", recognitions);
    }

    private void notifyDetectionState(ImageDetectionEvent.State state) {
        EventBus.getDefault().post(new ImageDetectionEvent(state));
    }

}