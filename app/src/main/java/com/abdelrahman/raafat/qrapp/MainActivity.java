package com.abdelrahman.raafat.qrapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import android.provider.ContactsContract;

import android.text.method.ScrollingMovementMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.abdelrahman.raafat.qrapp.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;

import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;

import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private Bitmap lastCreatedBitmap = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), imageUri -> {
                if (imageUri != null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        convertImageToCode(bitmap);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.generateCode.setOnClickListener(view -> {
            String data = binding.textCode.getText().toString().trim();
            if (data.isEmpty()) {
                binding.textCode.setError(getString(R.string.data_required));
            } else if (data.length() < 5) {
                binding.textCode.setError(getString(R.string.data_not_complete));
            } else {
                try {
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    lastCreatedBitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 1000, 1000);
                    binding.imageCode.setImageBitmap(lastCreatedBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    Snackbar.make(findViewById(R.id.root_layout), getString(R.string.error_in_generate), Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        binding.scanCode.setOnClickListener(view -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        binding.saveImage.setOnClickListener(view -> {
            if (binding.imageCode.getDrawable() != null) {
                prepareFile();
            } else {
                Snackbar.make(findViewById(R.id.root_layout), getString(R.string.not_found_image), Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.scanFromGallery.setOnClickListener(view -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

    }

    private void prepareFile() {
        String file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        File dir = new File(file + "/QRAPP");
        dir.mkdirs();

        String filename = setImageName(binding.textCode.getText().toString());
        File outFile = new File(dir, filename);
        if (lastCreatedBitmap == null) {
            return;
        }
        try {
            saveImage(outFile, lastCreatedBitmap);
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.saved_success), Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.error_in_save), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void saveImage(File outFile, Bitmap bitmap) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        galleryAddPic(outFile.getAbsolutePath());
        outputStream.flush();
        outputStream.close();
        ;
    }

    private String setImageName(String imageName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d_MM_yyyy_HH_mm_ss", Locale.ENGLISH);
        Date date = Calendar.getInstance().getTime();
        String formatDate = dateFormat.format(date);
        imageName = imageName.substring(0, 5);
        imageName = imageName.replace("/", "");
        imageName = imageName.replace(":", "");
        imageName = imageName.replace("\"", "");
        return imageName + "_" + formatDate + ".png";
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

        if (intent != null) {
            if (intent.getContents() != null) {
                showDialog(intent.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.invalid_image), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showDialog(String message) {

        MessageType messageType = checkMessage(message);
        addDialogNote(messageType, message);
    }

    private MessageType checkMessage(String message) {
        MessageType type = MessageType.TEXT;
        final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(message);
        if (m.find()) {
            type = MessageType.LINK;
        }

        if (Patterns.EMAIL_ADDRESS.matcher(message).matches()) {
            type = MessageType.EMAIL;
        }

        if (Patterns.PHONE.matcher(message).matches()) {
            type = MessageType.PHONE;
        }
        return type;
    }


    private void addDialogNote(MessageType messageType, String message) {
        View view = getLayoutInflater().inflate(R.layout.scan_resulat_menu, null);
        TextView resultScan = view.findViewById(R.id.scan_result_textView);
        Button copyButton = view.findViewById(R.id.copy_Button);
        Button callButton = view.findViewById(R.id.call_Button);
        Button addToContactsButton = view.findViewById(R.id.addToContacts_Button);
        Button openLinkButton = view.findViewById(R.id.open_link_Button);
        Button sendEmailButton = view.findViewById(R.id.send_email_Button);
        Button searchButton = view.findViewById(R.id.search_Button);
        Button cancelButton = view.findViewById(R.id.cancel_Button);


        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        resultScan.setText(message);
        resultScan.setMovementMethod(new ScrollingMovementMethod());

        copyButton.setOnClickListener(v -> {
            copyText(message);
            Toast.makeText(this, getString(R.string.copied_success), Toast.LENGTH_SHORT).show();
        });
        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        switch (messageType) {
            case LINK:
                openLinkButton.setVisibility(View.VISIBLE);
                callButton.setVisibility(View.GONE);
                addToContactsButton.setVisibility(View.GONE);
                sendEmailButton.setVisibility(View.GONE);
                searchButton.setVisibility(View.GONE);
                break;
            case EMAIL:
                openLinkButton.setVisibility(View.GONE);
                callButton.setVisibility(View.GONE);
                addToContactsButton.setVisibility(View.GONE);
                sendEmailButton.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.GONE);
                break;
            case PHONE:
                openLinkButton.setVisibility(View.GONE);
                callButton.setVisibility(View.VISIBLE);
                addToContactsButton.setVisibility(View.VISIBLE);
                sendEmailButton.setVisibility(View.GONE);
                searchButton.setVisibility(View.GONE);
                break;
            case TEXT:
                openLinkButton.setVisibility(View.GONE);
                callButton.setVisibility(View.GONE);
                addToContactsButton.setVisibility(View.GONE);
                sendEmailButton.setVisibility(View.GONE);
                searchButton.setVisibility(View.VISIBLE);
                break;
        }
        openLinkButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(message))));

        sendEmailButton.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + message)))
        );

        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, message);
            startActivity(intent);
        });

        callButton.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + message)))
        );

        addToContactsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, message);
            startActivity(intent);
        });

    }

    private void copyText(String resultScan) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("result_scan", resultScan);
        manager.setPrimaryClip(clipData);
    }

}