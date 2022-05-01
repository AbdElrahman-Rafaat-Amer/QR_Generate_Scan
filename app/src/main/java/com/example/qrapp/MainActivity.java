package com.example.qrapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.provider.MediaStore;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BinaryBitmap;

import com.google.zxing.MultiFormatReader;

import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;

import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.util.Locale;


import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    public static final int PICK_IMAGE = 1;
    private EditText editText;
    private Button generateButton, scanButton, saveButton, galleryButton;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.text_code);
        generateButton = findViewById(R.id.generate_code);
        scanButton = findViewById(R.id.scan_code);
        saveButton = findViewById(R.id.save_image);
        galleryButton = findViewById(R.id.scan_from_gallery);
        imageView = findViewById(R.id.image_code);

        generateButton.setOnClickListener(view -> {
            String data = editText.getText().toString().trim();
            if (data.isEmpty()) {
                editText.setError(getString(R.string.data_required));
            } else {
                QRGEncoder encoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, 1000);
                try {
                    Bitmap bitmap = encoder.encodeAsBitmap();
                    imageView.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        });

        scanButton.setOnClickListener(view -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        saveButton.setOnClickListener(view -> {
            if (imageView.getDrawable() != null) {
                saveImage();
            } else {
                Snackbar.make(findViewById(R.id.root_layout), getString(R.string.not_found_image), Snackbar.LENGTH_SHORT).show();
            }
        });

        galleryButton.setOnClickListener(view -> {
            Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getIntent.setType("image/*");
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
            startActivityForResult(chooserIntent, PICK_IMAGE);
        });

    }

    private void saveImage() {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();
        String file;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        } else {
            file = Environment.getExternalStorageDirectory().toString();
        }

        File dir = new File(file + "/QRAPP");
        dir.mkdirs();

        String filename = setImageName();
        File outFile = new File(dir, filename);
        Log.i(TAG, "outFile: " + outFile);
        try {
            FileOutputStream outputStream = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            galleryAddPic(outFile.getAbsolutePath());
            outputStream.flush();
            outputStream.close();
            Log.i(TAG, "saveToGallery: saved Success");
            Log.i(TAG, "saveToGallery: outFile " + outFile);
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.saved_success), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.i(TAG, "Exception: " + e.getMessage());
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.error_in_save), Snackbar.LENGTH_SHORT).show();
        }
    }

    private String setImageName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d_MM_yyyy_HH_mm", Locale.ENGLISH);
        Date date = Calendar.getInstance().getTime();
        String formatDate = dateFormat.format(date);
        return editText.getText().toString() + "_" + formatDate + ".png";
    }

    private void galleryAddPic(String pathOfSavedImage) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(pathOfSavedImage);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intent = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE) {
            if (data == null) {
                Log.i(TAG, "onActivityResult: error in getting image from gallery");
            } else {
                setImageToImageView(data);
            }
        }
        if (intent != null) {
            if (intent.getContents() != null) {
                showDialog(intent.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setImageToImageView(Intent data) {
        Log.i(TAG, "onActivityResult:load image success");
        Log.i(TAG, "onActivityResult: " + data.getData());
        try {
            Bitmap bitmapImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            Log.i(TAG, "onActivityResult: bitmapImage " + bitmapImage);
            //  imageView.setImageBitmap(bitmapImage);
            convertImageToCode(bitmapImage);
        } catch (IOException e) {
            Log.i(TAG, "onActivityResult: IOException " + e.getMessage());
        }
    }

    private void convertImageToCode(Bitmap bitmapImage) {
        int width = bitmapImage.getWidth(), height = bitmapImage.getHeight();
        int[] pixels = new int[width * height];
        bitmapImage.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
        MultiFormatReader reader = new MultiFormatReader();
        try {
            Result result = reader.decode(bBitmap);
            showDialog(result.getText());
            Log.i(TAG, "convertImageToCode: result.getText() " + result.getText());
        } catch (NotFoundException e) {
            Log.i(TAG, "convertImageToCode: NotFoundException " + e.getMessage());
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.invalid_image), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(getString(R.string.scan_result));
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.copy),
                (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("scan result", message);
                    clipboard.setPrimaryClip(clip);
                    dialog.dismiss();
                }
        );
        alertDialog.show();
    }

}