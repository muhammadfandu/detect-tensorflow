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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.debugdetect.api.Classifier;
import com.example.debugdetect.api.ObjectDetectionModel;
import com.example.debugdetect.asynctask.DetectAsyncTasks;
import com.example.debugdetect.interfaces.AnalyzeOsaPlanoDelegate;
import com.example.debugdetect.util.BitmapProcessor;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Loading");

        photoView = (PhotoView) findViewById(R.id.imageView);
        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                selectImage(MainActivity.this);
            }
        });
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

    private void selectImage(Context context) {
        final CharSequence[] options = { "Take Photo", "Choose from Gallery","Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your profile picture");

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

                                new DetectAsyncTasks(bitmapOriginal, that).execute(that);

                                photoView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                                cursor.close();
                            }
                        }

                    }
                    break;
            }
        }
    }

    public void analyzeCompletionBitmapResult(final Bitmap result, List<Classifier.Recognition> recognitions) {
        Log.d("Cek", "analyzeCompletionBitmapResult: running");
        final MainActivity that = this;
        if (result == null || recognitions == null) {
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    pDialog.dismiss();
//                    new androidx.appcompat.app.AlertDialog.Builder(that)
//                            .setIcon(android.R.drawable.ic_dialog_alert)
//                            .setTitle("Pemberitahuan")
//                            .setMessage("Tidak dapat memproses gambar, harap coba lagi")
//                            .setPositiveButton("Oke", null)
//                            .show();
//                }
//            });
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
//            new androidx.appcompat.app.AlertDialog.Builder(that)
//                    .setIcon(android.R.drawable.ic_dialog_alert)
//                    .setTitle("Pemberitahuan")
//                    .setMessage("Tidak dapat memproses gambar, harap coba lagi")
//                    .setPositiveButton("Oke", null)
//                    .show();
//            return;
        }

        final Bitmap bitmapWithBoundingBox = tagRecognitionOnBitmap(this, bitmapOriginal, recognitionArrayList);
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Stuff that updates the UI
                photoView.setImageBitmap(bitmapWithBoundingBox);

            }
        });

//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                  photoView.setImageBitmap(bitmapWithBoundingBox);
////                llProgressContainer.setVisibility(View.GONE);
////                btnSave.setEnabled(true);
////                btnSave.setBackgroundResource(R.drawable.rounded_button_green);
//            }
//        });
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
