package ir.meros.qrscanner;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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
    Button btn_scan;
    String standardBannerResponseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_scan = findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode();
            }
        });


        tapSell_int();
        tapSellStartTop();
        tapSellStartBottom();
    }

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

    private void scanCode() {

        ScanOptions options = new ScanOptions();
        options.setPrompt("برای روشن شدن فلش دکمه افزایش صدا را بزنید");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLuncher.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLuncher = registerForActivityResult(new ScanContract()
            , result -> {
                if (result.getContents() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("نتیجه");
                    builder.setMessage(result.getContents());
                    builder.setNegativeButton("بستن", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.setPositiveButton("بازکردن لینک", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(result.getContents()));
                                    startActivity(intent);
                                }
                            }

                    ).show();
                }
            });

}