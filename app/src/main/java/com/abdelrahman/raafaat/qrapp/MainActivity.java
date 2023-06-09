package com.abdelrahman.rafaat.qrapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.provider.ContactsContract;
import android.provider.MediaStore;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BinaryBitmap;

import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;

import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

        linkifyMethod();

        generateButton.setOnClickListener(view -> {
            String data = editText.getText().toString().trim();
            if (data.isEmpty()) {
                editText.setError(getString(R.string.data_required));
            }else if (data.length() < 5) {
                editText.setError(getString(R.string.data_not_complete));
            } else {
                QRGEncoder encoder = new QRGEncoder(data, null, QRGContents.Type.TEXT, 1000);
                try {
                    Bitmap bitmap = encoder.encodeAsBitmap();
                    imageView.setImageBitmap(bitmap);
                } catch (WriterException e) {
                    Log.i(TAG, "onCreate: WriterException---------> " + e.getMessage());
                    Snackbar.make(findViewById(R.id.root_layout), getString(R.string.error_in_generate), Snackbar.LENGTH_SHORT).show();
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
                prepareFile();
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

    private void linkifyMethod() {
        String text = "number:01151883341 name:abdo amer";
        String number = text.substring(7, 18);
        String name = text.substring(24);
        Log.i(TAG, "linkifyMethod: number-----> " + number);
        Log.i(TAG, "linkifyMethod: name-------> " + name);

    }

    private void prepareFile() {
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

        String filename = setImageName(editText.getText().toString());
        File outFile = new File(dir, filename);
        Log.i(TAG, "outFile: " + outFile);
        boolean isExist = outFile.exists();
        Log.i(TAG, "prepareFile: isExist-------> " + isExist);
        try {
            saveImage(outFile, bitmap);
            Log.i(TAG, "prepareFile: saveImage success");
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.saved_success), Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.i(TAG, "Exception: IOException" + e.getMessage());
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.error_in_save), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void saveImage(File outFile, Bitmap bitmap) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        galleryAddPic(outFile.getAbsolutePath());
        outputStream.flush();
        outputStream.close();
        Log.i(TAG, "saveToGallery: saved Success");
        Log.i(TAG, "saveToGallery: outFile " + outFile);
    }

    private String setImageName(String imageName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d_MM_yyyy_HH_mm_ss", Locale.ENGLISH);
        Date date = Calendar.getInstance().getTime();
        String formatDate = dateFormat.format(date);
        Log.i(TAG, "setImageName: imageName before edit ----------> " + imageName);
        imageName = imageName.substring(0, 5);
        imageName = imageName.replace("/", "");
        imageName = imageName.replace(":", "");
        imageName = imageName.replace("\"", "");
        Log.i(TAG, "setImageName: imageName after edit ----------> " + imageName);
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
        } catch (Exception e) {
            Log.i(TAG, "convertImageToCode: Exception " + e.getMessage());
            Snackbar.make(findViewById(R.id.root_layout), getString(R.string.invalid_image), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showDialog(String message) {

        MessageType messageType = checkMessage(message);
        Log.i(TAG, "showDialog: messageType-------> " + messageType);
        addDialogNote(messageType, message);
    }

    private MessageType checkMessage(String message) {
        MessageType type = MessageType.TEXT;
        final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(message);
        Log.i(TAG, "checkMessage: message-------> " + message);
        if (m.find()) {
            System.out.println("String contains URL");
            Log.i(TAG, "checkMessage: link -------> " + message);
            try {
                URL url = new URL(message);
                Log.i(TAG, "checkMessage: link success-------> " + url);
                type = MessageType.LINK;
            } catch (MalformedURLException e) {
                // e.printStackTrace();
                Log.i(TAG, "checkMessage: link failed");
                Log.i(TAG, "checkMessage: link failed" + e.getMessage());
            }
        }

        if (Patterns.EMAIL_ADDRESS.matcher(message).matches()) {
            Log.i(TAG, "checkMessage: EMAIL_ADDRESS------> " + message);
            type = MessageType.EMAIL;
        }

        if (Patterns.PHONE.matcher(message).matches()) {
            Log.i(TAG, "checkMessage: PHONE------> " + message);
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
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM);

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
            Log.i(TAG, "addDialogNote: addToContactsButton-----> " + message);
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
        Log.i(TAG, "addDialogNote: copied success");
    }

    /*public void getOpenWhatsIntent() {
        String url = "https://api.whatsapp.com/send?phone=" + "+02011518483341";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
*/

}