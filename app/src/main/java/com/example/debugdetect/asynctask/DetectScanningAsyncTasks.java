package com.example.debugdetect.asynctask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;

import com.example.debugdetect.api.Classifier;
import com.example.debugdetect.api.ObjectDetectionModel;
import com.example.debugdetect.interfaces.AnalyzeOsaPlanoDelegate;
import com.example.debugdetect.ui.ImageDetectionEvent;
import com.example.debugdetect.util.BitmapProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.debugdetect.util.ImageDetection.tagRecognitionOnBitmap;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.swap;

public class DetectScanningAsyncTasks extends AsyncTask<Context, Void, List<Classifier.Recognition>> {
    private static final String TAG = "ImageDetectionStatus";
    private AnalyzeOsaPlanoDelegate callback;
    private int detectionType;
    private Bitmap srcBitmap;

    public DetectScanningAsyncTasks(Bitmap srcBitmap, AnalyzeOsaPlanoDelegate callback) {
        this.srcBitmap = srcBitmap;
        this.callback = callback;
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
            bitmap = BitmapProcessor.downScaleImage(srcBitmap, 1024);
        } catch (OutOfMemoryError e) {

//            Logger.error("decode and crop image[%s] failed: %s", e.toString());
            return null;
        }

        notifyDetectionState(ImageDetectionEvent.State.DETECTING);

        List<Classifier.Recognition> results =
                ObjectDetectionModel.detectImage(bitmap, .0f);

//        Log.d("RIANDEBUGRESULT", ""+results.toString());
        Log.d("RIANDEBUG","SSSSSTTTAAARRTTTTTT ############----######------------#");

//        ArrayList<Bitmap> bmplist = new ArrayList<Bitmap>();
        List<Classifier.Recognition>  bmpresults= new ArrayList<>(results);
        for(int o = 1; o <= 2; o++) {

            int wadd = o*(bitmap.getWidth()/2); // 640 if image width 1280
            int hadd = o*(bitmap.getHeight()/2); // 360 if image height 720
            int wstride = bitmap.getWidth()/2;
            int hstride = bitmap.getHeight()/2;

            for (int i = 0; i < bitmap.getWidth(); i += wstride) {
                for (int j = 0; j < bitmap.getHeight(); j += hstride) {
                    if(bitmap.getWidth() - i < wadd || bitmap.getHeight() - j < hadd) continue;
                    int addWidth = wadd;
                    int addHeight = hadd;
                    Log.d("RIANDEBUG1", i + " " + j + " " + addWidth + " " + addHeight);

                    // CUT
                    Bitmap bmp2 = Bitmap.createBitmap(bitmap, i, j, addWidth, addHeight);
//                    bmplist.add(bmp2);

                    // DETECT
                    List<Classifier.Recognition> bmpres =
                            ObjectDetectionModel.detectImage(bmp2, .0f);

                    // NORMALIZE THE RECTF
                    for (int k = 0; k < bmpres.size(); k++) {
                        RectF tmploc = bmpres.get(k).getLocation();
                        tmploc.top += j;
                        tmploc.bottom += j;
                        tmploc.left += i;
                        tmploc.right += i;
                        bmpres.get(k).setLocation(tmploc);
                        bmpresults.add(bmpres.get(k));
                    }
                }
            }
        }

        Log.d("RIANDEBUG","EEEEEEENNNNNNNNNNNNDDDDDDDDDDD ############----######------------#");
        Log.d("RIANDEBUGRESULT", "" + bmpresults.toString());

        List<Classifier.Recognition> bults = new ArrayList<Classifier.Recognition>();
        if(bmpresults.size() > 0)
            bmpresults = NMSfunc(bmpresults, (float) 0.33);

        Log.d("RIANNMS","DDDDOOOONNEEEE ############----######------------#");

        notifyDetectionState(ImageDetectionEvent.State.TAGGING);

        final Bitmap tagBitmap = tagRecognitionOnBitmap(context, bitmap, bmpresults);
        if (tagBitmap != null) {

            callback.analyzeCompletionBitmapResult(tagBitmap, bmpresults);

        } else callback.analyzeCompletionBitmapResult(bitmap, bmpresults);

        return bmpresults;
    }



    @Override
    protected void onPostExecute(List<Classifier.Recognition> recognitions) {
        super.onPostExecute(recognitions);

        notifyDetectionState(ImageDetectionEvent.State.DONE);

//        Logger.debug("recognitions: %s", recognitions);
    }

    private void notifyDetectionState(ImageDetectionEvent.State state) {
//        EventBus.getDefault().post(new ImageDetectionEvent(state));
    }

    private List<Classifier.Recognition> NMSfunc(List<Classifier.Recognition> results, float threshold){
        float[] x1 = new float[1000];
        float[] y1 = new float[1000];
        float[] x2 = new float[1000];
        float[] y2 = new float[1000];
        float[] area = new float[1000];
        ArrayList<Integer> idxs = new ArrayList<Integer>();
        List<Classifier.Recognition> ret = new ArrayList<>();

        Log.d("NMSres", results.toString());

        for(int i=0;i<results.size();i++){
            x1[i] = results.get(i).getLocation().left;
            y1[i] =  results.get(i).getLocation().top;
            x2[i] = results.get(i).getLocation().right;
            y2[i] = results.get(i).getLocation().bottom;
            area[i] = (x2[i] - x1[i] + 1) * (y2[i] - y1[i] + 1);
            idxs.add(i);
        }

        Log.d("RIANNMSDEBUGx1", Arrays.toString(x1));
        Log.d("RIANNMSDEBUGy1", Arrays.toString(y1));
        Log.d("RIANNMSDEBUGx2", Arrays.toString(x2));
        Log.d("RIANNMSDEBUGy2", Arrays.toString(y2));
        Log.d("RIANNMSDEBUGarea", Arrays.toString(area));
        Log.d("RIANNMSDEBUGidxs", Arrays.toString(idxs.toArray()));

        for(int i=0;i<results.size()-1;i++){
            for(int j=i+1;j<results.size();j++){
                if(y2[idxs.get(j)] < y2[idxs.get(i)]){
                    swap(idxs, i, j);
                }
            }
        }

        Log.d("RIANNMSDEBUGidxs", Arrays.toString(idxs.toArray()));

        while(idxs.size() > 0){
            Log.d("RIANNMSIDXSIZE", idxs.size() + "");
            int last = idxs.size()-1;
            int ii = idxs.get(last);
            ret.add(results.get(ii));

            float[] xx1 = new float[1000];
            float[] xx2 = new float[1000];
            float[] yy1 = new float[1000];
            float[] yy2 = new float[1000];
            for(int i=0;i<last;i++){
                xx1[i] = max(x1[ii], x1[idxs.get(i)]);
                yy1[i] = max(y1[ii], y1[idxs.get(i)]);
                xx2[i] = min(x2[ii], x2[idxs.get(i)]);
                yy2[i] = min(y2[ii], y2[idxs.get(i)]);
            }

            float[] w = new float[1000];
            float[] h = new float[1000];
            for(int i=0;i<last;i++) {
                w[i] = max(0, xx2[i] - xx1[i] + 1);
                h[i] = max(0, yy2[i] - yy1[i] + 1);
            }

            float[] overlap = new float[1000];
            for(int i=0;i<last;i++)
                overlap[i] = (w[i] * h[i]) / area[idxs.get(i)];

            for(int i=last;i>=0;i--){
                if(i==last || overlap[i] > threshold)
                    idxs.remove(i);
            }
        }

        return ret;
    }
}

