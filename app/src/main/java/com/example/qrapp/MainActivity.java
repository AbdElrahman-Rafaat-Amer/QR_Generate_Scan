package com.example.qrapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button generateButton, scanButton;
    private ImageView imageView;
    private String data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.text_code);
        generateButton = findViewById(R.id.generate_code);
        scanButton = findViewById(R.id.scan_code);
        imageView = findViewById(R.id.image_code);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                data = editText.getText().toString().trim();
                if (data.isEmpty()) {
                    editText.setError("Data Required");
                } else {
                    QRGEncoder encoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, 1000);
                    try {
                        Bitmap bitmap = encoder.encodeAsBitmap();
                        imageView.setImageBitmap(bitmap);
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
       IntentResult intent = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

       if (intent != null){
           if (intent.getContents() != null){
               editText.setText(intent.getContents());
           }
       }else {
           super.onActivityResult(requestCode, resultCode, data);

       }
    }
}