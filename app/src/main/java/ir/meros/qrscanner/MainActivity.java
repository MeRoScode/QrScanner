package ir.meros.qrscanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ir.tapsell.plus.AdRequestCallback;
import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusBannerType;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworkError;
import ir.tapsell.plus.model.AdNetworks;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

public class MainActivity extends AppCompatActivity {
    Button btn_scan, btn_create, btn_share;
    String standardBannerResponseId;
    ImageView img_qr;
    EditText edt_link;
    Bitmap qrCodeBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

//        tapSell_int();
//        tapSellStartTop();
//        tapSellStartBottom();
    }

    private void initViews() {
        img_qr = findViewById(R.id.img_qr);
        btn_scan = findViewById(R.id.btn_scan);
        edt_link = findViewById(R.id.edt_link);
        btn_create = findViewById(R.id.btn_create);
        btn_share = findViewById(R.id.btn_share);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode();
            }
        });
//        btn_create.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (edt_link.getText() != null) {
//                    qrCodeBitmap = generateQRCode(edt_link.getText().toString(), 500, 500);
//
//                    // Display the QR code in the ImageView
//                    img_qr.setImageBitmap(qrCodeBitmap);
//                }
//
//
//            }
//        });

        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                share_qr();
            }
        });

    }

    private boolean share_qr() {
        if (qrCodeBitmap != null) {
            saveBitmapToFile(qrCodeBitmap);

            // Save the QR code Bitmap to a file
            Uri imageUri = saveBitmapToFile(qrCodeBitmap);

            // Share the saved image using an Intent
            if (imageUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png"); // Specify the MIME type of the image
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, "تو هم با این برنامه بارکد خودتو بساز \n https://myket.ir/app/com.meros.qrscanner");

                startActivity(Intent.createChooser(shareIntent, "اشتراک گذاری بارکد"));
            }
        } else
            Toast.makeText(this, "عکسی برای اشتراک گذاری وجود ندارد", Toast.LENGTH_SHORT).show();

        return false;
    }

    private Uri saveBitmapToFile(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); // Make sure the directory exists

            File imageFile = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            return FileProvider.getUriForFile(this, "ir.meros.qrscanner.fileprovider", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


//    public static Bitmap generateQRCode(String qrCodeText, int width, int height) {
//        try {
//            QRCodeWriter qrCodeWriter = new QRCodeWriter();
//            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, width, height);
//
//            int matrixWidth = bitMatrix.getWidth();
//            int matrixHeight = bitMatrix.getHeight();
//            int[] pixels = new int[matrixWidth * matrixHeight];
//
//            for (int y = 0; y < matrixHeight; y++) {
//                for (int x = 0; x < matrixWidth; x++) {
//                    if (bitMatrix.get(x, y)) {
//                        pixels[y * matrixWidth + x] = Color.BLACK;
//                    } else {
//                        pixels[y * matrixWidth + x] = Color.WHITE;
//                    }
//                }
//            }
//
//            Bitmap bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888);
//            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight);
//            return bitmap;
//        } catch (WriterException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }


    private void scanCode() {

        ScanOptions options = new ScanOptions();
        options.setPrompt("برای روشن شدن فلش دکمه افزایش صدا را بزنید");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLuncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLuncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View customView = inflater.inflate(R.layout.custom_dialog, null);

            TextView textView = customView.findViewById(R.id.textView);
            textView.setText(result.getContents());

            Button copyButton = customView.findViewById(R.id.copyButton);
            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("label", result.getContents());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(MainActivity.this, "متن در کلیپ بورد ذخیره شد", Toast.LENGTH_SHORT).show();
            });

            Button openLinkButton = customView.findViewById(R.id.openLinkButton);
            openLinkButton.setOnClickListener(v -> {
                if (result.getContents().startsWith("http")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(result.getContents()));
                    startActivity(intent);
                } else {
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("label", result.getContents());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(MainActivity.this, "متن در کلیپ بورد ذخیره شد", Toast.LENGTH_SHORT).show();
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("نتیجه");
            builder.setView(customView);
            builder.show();
        }
    });

    private void tapSellStartTop() {
        TapsellPlus.requestStandardBannerAd(
                this, "640f82079119f1455f27a430",
                TapsellPlusBannerType.BANNER_320x50,
                new AdRequestCallback() {
                    @Override
                    public void response(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.response(tapsellPlusAdModel);

                        //Ad is ready to show
                        //Put the ad's responseId to your responseId variable
                        standardBannerResponseId = tapsellPlusAdModel.getResponseId();
                        showTop();
                    }

                    @Override
                    public void error(@NonNull String message) {
                    }
                });
    }

    private void tapSellStartBottom() {
        TapsellPlus.requestStandardBannerAd(
                this, "650ef1f0b8c2e8295be32d59",
                TapsellPlusBannerType.BANNER_320x50,
                new AdRequestCallback() {
                    @Override
                    public void response(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.response(tapsellPlusAdModel);

                        //Ad is ready to show
                        //Put the ad's responseId to your responseId variable
                        standardBannerResponseId = tapsellPlusAdModel.getResponseId();
                        showBottom();
                    }

                    @Override
                    public void error(@NonNull String message) {
                    }
                });
    }

    private void tapSell_int() {
        TapsellPlus.initialize(this, "kthbsidtcatdtimgqrorarnsfhmimofodeegfjdpallbjafnlleksfkcqaodhksrldlqff",
                new TapsellPlusInitListener() {
                    @Override
                    public void onInitializeSuccess(AdNetworks adNetworks) {
                        Log.d("onInitializeSuccess", adNetworks.name());
                    }

                    @Override
                    public void onInitializeFailed(AdNetworks adNetworks,
                                                   AdNetworkError adNetworkError) {
                        Log.e("onInitializeFailed", "ad network: " + adNetworks.name() + ", error: " + adNetworkError.getErrorMessage());
                    }
                });
    }

    private void showTop() {
        TapsellPlus.showStandardBannerAd(this, standardBannerResponseId,
                findViewById(R.id.standardBannerTop),
                new AdShowListener() {
                    @Override
                    public void onOpened(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.onOpened(tapsellPlusAdModel);
                    }

                    @Override
                    public void onError(TapsellPlusErrorModel tapsellPlusErrorModel) {
                        super.onError(tapsellPlusErrorModel);
                    }
                });
    }

    private void showBottom() {
        TapsellPlus.showStandardBannerAd(this, standardBannerResponseId,
                findViewById(R.id.standardBannerBottom),
                new AdShowListener() {
                    @Override
                    public void onOpened(TapsellPlusAdModel tapsellPlusAdModel) {
                        super.onOpened(tapsellPlusAdModel);
                    }

                    @Override
                    public void onError(TapsellPlusErrorModel tapsellPlusErrorModel) {
                        super.onError(tapsellPlusErrorModel);
                    }
                });
    }


}