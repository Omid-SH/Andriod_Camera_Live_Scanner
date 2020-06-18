package com.adityaarora.liveedgedetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.adityaarora.liveedgedetection.activity.ScanActivity;
import com.adityaarora.liveedgedetection.constants.ScanConstants;
import com.adityaarora.liveedgedetection.util.ScanUtils;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.FileHandler;

import javax.xml.datatype.Duration;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 101;
    private ImageView scannedImageView;
    private ArrayList<Bitmap> bitmaps = new ArrayList<>();
    private int currentImg;
    private EditText filename;
    final int MY_PERMISSIONS_REQUEST_WRITE_DATA = 132;
    final int SAVE_PDF = 1;
    final int SAVE_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        scannedImageView = findViewById(R.id.scanned_image);
        filename = findViewById(R.id.file_name);

        startScan();
    }

    private void startScan() {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                if(null != data && null != data.getExtras()) {
                    String filePath = data.getExtras().getString(ScanConstants.SCANNED_RESULT);
                    Bitmap baseBitmap = ScanUtils.decodeBitmapFromFile(filePath, ScanConstants.IMAGE_NAME);
                    bitmaps.add(baseBitmap);
                    setImg(bitmaps.size() - 1);
                }
            } else if(resultCode == Activity.RESULT_CANCELED) {
                setImg(bitmaps.size() - 1);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createPdf(String filename) {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.Page page = null;

            // crate a page description
            for (int i = 0; i < bitmaps.size(); i++) {
                Bitmap bitmap = bitmaps.get(i);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i).create();
                // start a page
                page = document.startPage(pageInfo);
                if (page == null) {
                    return;
                }
                Canvas canvas = page.getCanvas();
                canvas.drawBitmap(bitmap, 0, 0, null);
                document.finishPage(page);
            }

            // write the document content

            File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "LiveEdgeFiles");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("App", "failed to create directory");
                }
            }

            File file = new File(mediaStorageDir + "/"+filename+".pdf");
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.d("App", "failed to create file");
                }
            }

            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(file);
            } catch (Exception e) {
                Log.e("TAG", e.getMessage());
                return;
            }

            try {
                document.writeTo(fOut);
            } catch (IOException e) {
                Log.e("createPdf()", "Error");
            }

            // close the document
            document.close();
            fOut.close();

            Toast.makeText(this,"PDF file has been saved.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("onActivityResult","Can not make PDF.");
            Toast.makeText(this,"Directory has problem. Can not make PDF!", Toast.LENGTH_LONG).show();
        }
    }

    private void setImg(int index) {
        if(bitmaps.size() == 0) {
            startScan();
        } else if(index < 0) {
            currentImg = 0;
            scannedImageView.setImageBitmap(bitmaps.get(currentImg));
        } else if(index >= bitmaps.size()) {
            currentImg = bitmaps.size() - 1;
            scannedImageView.setImageBitmap(bitmaps.get(currentImg));
        } else {
            currentImg = index;
            scannedImageView.setImageBitmap(bitmaps.get(currentImg));
        }
    }

    public void movePrevious(View view) {
        setImg(currentImg - 1);
    }

    public void moveNext(View view) {
        setImg(currentImg + 1);
    }

    public void ignoreCurrentImg(View view) {
        if(bitmaps.size() != 0) {
            bitmaps.remove(currentImg);
            setImg(currentImg - 1);
        }
    }

    public void continueGettingImg(View view) {
        startScan();
    }

    public void addToGallery(View view) {
        checkFilePermission(SAVE_IMAGE);
    }

    public void addPDF(View view) {
        checkFilePermission(SAVE_PDF);
    }

    private void checkFilePermission (int action) {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Permission required")
                        .setMessage("Give access to files is required in this app to save data!")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_REQUEST_WRITE_DATA);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_DATA);
            }
        } else {
            // Permission has already been granted
            if(action == SAVE_PDF)
                savePdf();
            else
                saveImg();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_DATA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We prefer to do nothing and user again click on th button
                } else {
                    // We do nothing
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void savePdf() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_file_name, null);
        final EditText filename = dialogLayout.findViewById(R.id.file_name);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Select your pdf file name")
                .setMessage("Select your pdf file name. This file will be saved in LiveEdgeFiles directory.")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createPdf(filename.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setView(dialogLayout)
                .show();
    }

    private void saveImg() {
        MediaStore.Images.Media.insertImage(getContentResolver(), bitmaps.get(currentImg), "Scan" , "Live_edge");
        Toast.makeText(this,"Image saved in the gallery.", Toast.LENGTH_LONG).show();
    }


}
