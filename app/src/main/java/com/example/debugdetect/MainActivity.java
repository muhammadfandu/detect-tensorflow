package com.example.debugdetect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import com.example.debugdetect.api.Classifier;
import com.example.debugdetect.api.ObjectDetectionModel;
import com.example.debugdetect.asynctask.DetectAsyncTasks;
import com.example.debugdetect.asynctask.DetectScanningAsyncTasks;
import com.example.debugdetect.interfaces.AnalyzeOsaPlanoDelegate;
import com.example.debugdetect.util.BitmapProcessor;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.debugdetect.util.Constants.MAX_ANALYZED_IMAGE_WIDTH;
import static com.example.debugdetect.util.ImageDetection.tagRecognitionOnBitmap;

public class MainActivity extends AppCompatActivity implements AnalyzeOsaPlanoDelegate {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};

    private PhotoView photoView;
    private Button btnSelect;

    private Bitmap bitmapOriginal;
    ArrayList<Classifier.Recognition> recognitionArrayList = new ArrayList<>();

    private ArrayList<String> imagePathList;
    private int imgId = 1;

    private boolean readFolder;

    private Button btnProcess;
    private Button chooseImage;
    private ToggleButton tbScanning;

    private int iteration = 0;
    private File[] files = null;
    private HashMap<String, String > detectionResultMap = new HashMap<>();

    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new InitializeModelAsyncTask().execute(MainActivity.this);

        if(allPermissionsGranted()){
            // some actions
        } else{
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


        photoView = (PhotoView) findViewById(R.id.imageView);
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading");

        btnProcess = findViewById(R.id.btn_process);
        tbScanning = findViewById(R.id.tb_scanning);
        chooseImage = findViewById(R.id.btn_select);

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readFolder = true;
                iteration = 0;
                files = getFiles();
                pDialog.setMessage(String.format("Loading %d / %d", iteration + 1, files.length));
                pDialog.show();
                if (tbScanning.isChecked()) {
                    // go to scanning process
                    iterateScanningProcessFile(iteration);
                    pDialog.dismiss();
                } else {
                    iterateProcessFile(iteration);
                    pDialog.dismiss();
                }
            }
        });

        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readFolder = false;
                final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Choose your image");

                builder.setItems(options, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int item) {

                        if (options[item].equals("Take Photo")) {
                            Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(takePicture, 0);

                        } else if (options[item].equals("Choose from Gallery")) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto , 1);

                        } else if (options[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("LOG", "onActivityResult: begin");
        final MainActivity that = this;
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        photoView.setImageBitmap(selectedImage);
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();

                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);

                                bitmapOriginal = BitmapFactory.decodeFile(picturePath);
                                bitmapOriginal = BitmapProcessor.downScaleImage(bitmapOriginal, MAX_ANALYZED_IMAGE_WIDTH);

                                if (tbScanning.isChecked()) {
                                    // go to scanning process
                                    new DetectScanningAsyncTasks(bitmapOriginal, that, readFolder).execute(that);
                                } else {
                                    new DetectAsyncTasks(bitmapOriginal, that, readFolder).execute(that);
                                }


                                photoView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                cursor.close();
                            }
                        }

                    }
                    break;
            }
        }
    }

    private class InitializeModelAsyncTask extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... contexts) {
            if (contexts == null
                    || contexts.length <= 0) {
                return false;
            }

            final Context context = contexts[0];

            final boolean ret = ObjectDetectionModel.initialize(context);
            Log.d("tensorflow", ret? "sukses inisiasi" : "error");

            return ret;
        }

    }

    private File[] getFiles() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir.exists()) {
            return storageDir.listFiles();
        }

        return null;
    }

    private void iterateProcessFile(int iteration) {
        Bitmap bitmap = BitmapFactory.decodeFile(files[iteration].getAbsolutePath());
        new DetectAsyncTasks(bitmap, this, readFolder).execute(this);
    }

    private void iterateScanningProcessFile(int iteration) {
        Bitmap bitmap = BitmapFactory.decodeFile(files[iteration].getAbsolutePath());
        new DetectScanningAsyncTasks(bitmap, this, readFolder).execute(this);
    }

    public void analyzeCompletionBitmapResult2(final Bitmap result, List<Classifier.Recognition> recognitions) {
        Log.d("Cek", "analyzeCompletionBitmapResult: running");
        final MainActivity that = this;
        if (result == null || recognitions == null) {

            return;
        }

        for (Classifier.Recognition recognition : recognitions) {
            recognitionArrayList.add(recognition);
        }

        pDialog.dismiss();
        File fileImage = BitmapProcessor.saveBitmap(bitmapOriginal, "temp_photo_grid_" + imgId, getApplicationContext(), 80);

        try{
            if(fileImage!=null){
                imagePathList.set(imgId, fileImage.getAbsolutePath());
            }
        }catch (Exception e){

        }

        final Bitmap bitmapWithBoundingBox = tagRecognitionOnBitmap(this, bitmapOriginal, recognitionArrayList);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                photoView.setImageBitmap(bitmapWithBoundingBox);
            }
        });
    }

    public void analyzeCompletionBitmapResult(final Bitmap result, List<Classifier.Recognition> recognitions) {
        Log.d("Cek", "analyzeCompletionBitmapResult: running");
        if (result == null || recognitions == null) return;

        HashMap<String, Integer> tempMap = new HashMap<>();

        for (Classifier.Recognition recognition : recognitions) {
            if (tempMap.containsKey(recognition.getTitle())) {
                tempMap.put(recognition.getTitle(), tempMap.get(recognition.getTitle()) + 1);
            } else {
                tempMap.put(recognition.getTitle(), 1);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : tempMap.entrySet()) {
            stringBuilder.append(String.format("%s : %d", entry.getKey(), entry.getValue())).append("\n");
        }

        detectionResultMap.put(files[iteration].getName(), stringBuilder.toString());

        iteration++;
        if (iteration < files.length) {
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    pDialog.setMessage(String.format("Loading %d / %d", iteration + 1, files.length));
                }
            });
            iterateProcessFile(iteration);
        } else {
            pDialog.dismiss();
            try {
                saveResultToFile();
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        showFinishAlert();
                    }
                });
            } catch (IOException e) {

            }
        }
    }

    private void showFinishAlert() {
        new AlertDialog.Builder(this)
            .setTitle("Pemberitahuan")
            .setMessage(String.format("Proses selesai. File hasil berada di %s/%s", getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "result.txt"))
            .setPositiveButton(android.R.string.yes, null)
            .show();
    }

    private void saveResultToFile() throws IOException {
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, "result.txt");

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : detectionResultMap.entrySet()) {
            stringBuilder.append(String.format("%s :", entry.getKey())).append("\n");
            stringBuilder.append(String.format("%s", entry.getValue())).append("\n");
        }
        stringBuilder.append("\n");

        FileOutputStream stream = new FileOutputStream(file);
        try {
            stream.write(stringBuilder.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stream.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_CODE_PERMISSIONS) {
                // some actions
            }
        }
    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
