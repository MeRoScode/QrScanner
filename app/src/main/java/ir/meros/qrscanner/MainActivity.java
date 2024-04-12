package ir.meros.qrscanner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    Button btn_scan;
    ImageView img_qr;
    private String seatType;
    private String seatFullName;
    AlertDialog alertDialog;

    PersonAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = new PersonAdapter(new ArrayList<>(), this);
        RecyclerView recyclerView = findViewById(R.id.rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        Set<String> scannedNumbers = sharedPreferences.getStringSet("scanned_codes", new HashSet<String>());
        List<String> list = new ArrayList<String>(scannedNumbers);
        Log.i("sdgfdsg", "extractSeatInfo: " + scannedNumbers);
        Log.i("sdgfdsg", "extractSeatInfo: " + list);
        for (String tikedId: list) {
            extractSeatInfo(MainActivity.this, tikedId);

            Person person = new Person();
            person.setSeatType(getSeatType());
            person.setTicketId(tikedId);
            person.setSeatFullName(getSeatFullName());
            adapter.add(person);
        }

        EditText edtSearch = findViewById(R.id.search);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                adapter.search(charSequence.toString());
                if (adapter.getItemCount() == 0) {

                }else {

                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

            initViews();
    }





    ActivityResultLauncher<ScanOptions> barLuncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null&&result.getContents().contains("supreme7")) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View customView = inflater.inflate(R.layout.custom_dialog, null);

            LayoutInflater inflater2 = LayoutInflater.from(MainActivity.this);
            View used_dialog = inflater2.inflate(R.layout.used_dialog, null);

            Uri uri = Uri.parse(result.getContents());
            String ticketId = uri.getQueryParameter("seat_id");


            SharedPreferences sharedPreferences = getSharedPreferences("MyPref", MODE_PRIVATE);
            Set<String> scannedCodes = new HashSet<>(sharedPreferences.getStringSet("scanned_codes", new HashSet<String>()));


            if (!scannedCodes.contains(ticketId)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(customView);
                alertDialog = builder.create(); // Create AlertDialog object
                alertDialog.show();
                Button Cancel_Button = customView.findViewById(R.id.btn_cancel);
                Cancel_Button.setOnClickListener(v -> {

                    Toast.makeText(MainActivity.this, "Ticket Canceled", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                });

                Button Approve_Button = customView.findViewById(R.id.btn_approve);
                Approve_Button.setOnClickListener(view -> {
                    Toast.makeText(MainActivity.this, "Ticket Approved", Toast.LENGTH_SHORT).show();

                    scannedCodes.add(ticketId);
                    Person person = new Person();
                    person.setSeatType(getSeatType());
                    person.setTicketId(ticketId);
                    person.setSeatFullName(getSeatFullName());
                    adapter.add(person);
                    // Update SharedPreferences with the new Set
                    sharedPreferences.edit().putStringSet("scanned_codes", scannedCodes).apply();
                    alertDialog.dismiss();
                });


            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(used_dialog);
                alertDialog = builder.create(); // Create AlertDialog object
                alertDialog.show();


            }


            // Extract seat information
            extractSeatInfo(MainActivity.this, ticketId);
            String seatType = getSeatType();
            String seatFullName = getSeatFullName();

            TextView seatHolder = customView.findViewById(R.id.txv_seatHolder);
            TextView seatID = customView.findViewById(R.id.txv_seatID);
            TextView seatCat = customView.findViewById(R.id.txv_seatCat);

            seatHolder.setText(seatFullName);
            seatID.setText(ticketId);
            seatCat.setText(seatType);

            TextView seatHolder2 = used_dialog.findViewById(R.id.txv_seatHolder2);
            TextView seatID2 = used_dialog.findViewById(R.id.txv_seatID2);
            TextView seatCat2 = used_dialog.findViewById(R.id.txv_seatCat2);
            Button btn_cancel2 = used_dialog.findViewById(R.id.btn_cancel2);
            btn_cancel2.setOnClickListener(view -> alertDialog.dismiss());
            seatHolder2.setText(seatFullName);
            seatID2.setText(ticketId);
            seatCat2.setText(seatType);

        }else {
            Toast.makeText(this, "Unknown QR", Toast.LENGTH_SHORT).show();
        }
    });

    private void scanCode() {

        ScanOptions options = new ScanOptions();
        options.setPrompt("Press Volume Up for Flashlight");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLuncher.launch(options);
    }

    public void extractSeatInfo(Context context, String ticketId) {
        // Read the JSON file from the raw folder
        String jsonData = readJsonFromRaw(context);


        try {
            JSONArray jsonArray = new JSONArray(jsonData);

            JSONObject jsonObject = jsonArray.getJSONObject(2);
            JSONArray data = jsonObject.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {

                JSONObject person = data.getJSONObject(i);
//                Log.i("Adwdawd", "extractSeatInfo: " +person );
                if (person.has("ticket_id") && person.getString("ticket_id").equals(ticketId.trim())) {
                    Log.i("Adwdawd", "extractSeatInfo: " + 111);
                    seatType = person.getString("seat_type");
                    seatFullName = person.getString("seat_full_name");
                    // Print or use the extracted data

                    Log.i("Adwdawd", "extractSeatInfo: " + seatType);
                    Log.i("Adwdawd", "extractSeatInfo: " + seatFullName);
                    return; // Exit the loop once the data is found
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String getSeatType() {
        return seatType;
    }

    public String getSeatFullName() {
        return seatFullName;
    }

    private static String readJsonFromRaw(Context context) {
        StringBuilder json = new StringBuilder();
        try {
            Resources resources = context.getResources();
            InputStream inputStream = resources.openRawResource(R.raw.tickets); // Replace with your JSON file name
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                json.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    private void initViews() {
        img_qr = findViewById(R.id.img_qr);
        btn_scan = findViewById(R.id.btn_scan);

        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode();
            }
        });


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


//    private void showTop() {
//        TapsellPlus.showStandardBannerAd(this, standardBannerResponseId,
//                findViewById(R.id.standardBannerTop),
//                new AdShowListener() {
//                    @Override
//                    public void onOpened(TapsellPlusAdModel tapsellPlusAdModel) {
//                        super.onOpened(tapsellPlusAdModel);
//                    }
//
//                    @Override
//                    public void onError(TapsellPlusErrorModel tapsellPlusErrorModel) {
//                        super.onError(tapsellPlusErrorModel);
//                    }
//                });
//    }

//    private void showBottom() {
//        TapsellPlus.showStandardBannerAd(this, standardBannerResponseId,
//                findViewById(R.id.standardBannerBottom),
//                new AdShowListener() {
//                    @Override
//                    public void onOpened(TapsellPlusAdModel tapsellPlusAdModel) {
//                        super.onOpened(tapsellPlusAdModel);
//                    }
//
//                    @Override
//                    public void onError(TapsellPlusErrorModel tapsellPlusErrorModel) {
//                        super.onError(tapsellPlusErrorModel);
//                    }
//                });
//    }


//    private void tapSellStartTop() {
//        TapsellPlus.requestStandardBannerAd(
//                this, "640f82079119f1455f27a430",
//                TapsellPlusBannerType.BANNER_320x50,
//                new AdRequestCallback() {
//                    @Override
//                    public void response(TapsellPlusAdModel tapsellPlusAdModel) {
//                        super.response(tapsellPlusAdModel);
//
//                        //Ad is ready to show
//                        //Put the ad's responseId to your responseId variable
//                        standardBannerResponseId = tapsellPlusAdModel.getResponseId();
//                        showTop();
//                    }
//
//                    @Override
//                    public void error(@NonNull String message) {
//                    }
//                });
//    }
//
//    private void tapSellStartBottom() {
//        TapsellPlus.requestStandardBannerAd(
//                this, "650ef1f0b8c2e8295be32d59",
//                TapsellPlusBannerType.BANNER_320x50,
//                new AdRequestCallback() {
//                    @Override
//                    public void response(TapsellPlusAdModel tapsellPlusAdModel) {
//                        super.response(tapsellPlusAdModel);
//
//                        //Ad is ready to show
//                        //Put the ad's responseId to your responseId variable
//                        standardBannerResponseId = tapsellPlusAdModel.getResponseId();
//                        showBottom();
//                    }
//
//                    @Override
//                    public void error(@NonNull String message) {
//                    }
//                });
//    }
//
//    private void tapSell_int() {
//        TapsellPlus.initialize(this, "kthbsidtcatdtimgqrorarnsfhmimofodeegfjdpallbjafnlleksfkcqaodhksrldlqff",
//                new TapsellPlusInitListener() {
//                    @Override
//                    public void onInitializeSuccess(AdNetworks adNetworks) {
//                        Log.d("onInitializeSuccess", adNetworks.name());
//                    }
//
//                    @Override
//                    public void onInitializeFailed(AdNetworks adNetworks,
//                                                   AdNetworkError adNetworkError) {
//                        Log.e("onInitializeFailed", "ad network: " + adNetworks.name() + ", error: " + adNetworkError.getErrorMessage());
//                    }
//                });
//    }