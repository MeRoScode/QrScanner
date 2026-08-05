package ir.meros.qrscanner.ui.generate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ArrayRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.ChipGroup;

import java.io.IOException;

import ir.meros.qrscanner.R;
import ir.meros.qrscanner.qr.QrGenerator;
import ir.meros.qrscanner.qr.QrPayload;
import ir.meros.qrscanner.qr.QrStyle;
import ir.meros.qrscanner.ui.StoreLinks;

/**
 * Creates a QR code from one of several input types (text, URL, WiFi, contact,
 * phone, SMS, email), then lets the user save it to the gallery or share it.
 * Encoding + history live in {@link GenerateViewModel}; this View builds the
 * payload string for the selected type and owns the save/share intents.
 */
public class GenerateActivity extends AppCompatActivity {

    private enum GenType {TEXT, URL, WIFI, CONTACT, PHONE, SMS, EMAIL}

    private GenerateViewModel viewModel;
    private GenType current = GenType.TEXT;

    private ImageView imgQr;
    private Button btnShare;
    private Button btnSave;
    private View optionsGroup;
    private ImageView optionsChevron;
    private TextView contrastWarning;
    private Bitmap currentQr;

    private final ActivityResultLauncher<String> storagePermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    saveToGallery();
                } else {
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);

        viewModel = new ViewModelProvider(this).get(GenerateViewModel.class);

        imgQr = findViewById(R.id.img_generated_qr);
        btnShare = findViewById(R.id.btn_share_qr);
        btnSave = findViewById(R.id.btn_save_qr);
        optionsGroup = findViewById(R.id.group_options);
        optionsChevron = findViewById(R.id.img_options_chevron);
        contrastWarning = findViewById(R.id.txv_contrast_warning);

        setupTypeChips();
        setupAppearanceOptions();
        findViewById(R.id.btn_generate).setOnClickListener(v -> generate());
        btnShare.setOnClickListener(v -> shareQr());
        btnSave.setOnClickListener(v -> onSaveClicked());

        observeViewModel();

        findViewById(R.id.generate_root)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_up));
    }

    private void setupTypeChips() {
        ChipGroup group = findViewById(R.id.chip_group_type);
        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            current = typeFor(checkedIds.get(0));
            showFieldsFor(current);
        });
        showFieldsFor(current);
    }

    private GenType typeFor(int chipId) {
        if (chipId == R.id.chip_url) return GenType.URL;
        if (chipId == R.id.chip_wifi) return GenType.WIFI;
        if (chipId == R.id.chip_contact) return GenType.CONTACT;
        if (chipId == R.id.chip_phone) return GenType.PHONE;
        if (chipId == R.id.chip_sms) return GenType.SMS;
        if (chipId == R.id.chip_email) return GenType.EMAIL;
        return GenType.TEXT;
    }

    private void showFieldsFor(GenType type) {
        boolean textLike = type == GenType.TEXT || type == GenType.URL;
        show(R.id.edt_text, textLike);
        show(R.id.group_wifi, type == GenType.WIFI);
        show(R.id.group_contact, type == GenType.CONTACT);
        show(R.id.edt_phone, type == GenType.PHONE);
        show(R.id.group_sms, type == GenType.SMS);
        show(R.id.group_email, type == GenType.EMAIL);

        if (textLike) {
            ((EditText) findViewById(R.id.edt_text)).setHint(
                    type == GenType.URL ? R.string.type_url : R.string.qr_input_hint);
        }
    }

    private void show(int viewId, boolean visible) {
        findViewById(viewId).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // ---- Appearance options -------------------------------------------------

    private void setupAppearanceOptions() {
        QrStyle style = viewModel.getStyle();
        findViewById(R.id.btn_toggle_options).setOnClickListener(v -> toggleOptions());

        ColorSwatchRow.into(findViewById(R.id.row_fg_colors),
                palette(R.array.qr_fg_colors), style.foreground(), color -> {
                    style.foreground(color);
                    onStyleChanged();
                });
        ColorSwatchRow.into(findViewById(R.id.row_bg_colors),
                palette(R.array.qr_bg_colors), style.background(), color -> {
                    style.background(color);
                    onStyleChanged();
                });

        ChipGroup shapes = findViewById(R.id.chip_group_shape);
        shapes.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            style.shape(shapeFor(checkedIds.get(0)));
            onStyleChanged();
        });

        ChipGroup levels = findViewById(R.id.chip_group_ecc);
        levels.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            style.ecc(eccFor(checkedIds.get(0)));
            onStyleChanged();
        });

        contrastWarning.setVisibility(style.hasReadableContrast() ? View.GONE : View.VISIBLE);
    }

    private void toggleOptions() {
        boolean opening = optionsGroup.getVisibility() != View.VISIBLE;
        optionsGroup.setVisibility(opening ? View.VISIBLE : View.GONE);
        optionsChevron.setRotation(opening ? 180f : 0f);
    }

    /** Re-renders the code already on screen, if any, and re-checks contrast. */
    private void onStyleChanged() {
        contrastWarning.setVisibility(
                viewModel.getStyle().hasReadableContrast() ? View.GONE : View.VISIBLE);
        viewModel.restyle();
    }

    /** Reads a colour array from resources into the plain int[] the row wants. */
    private int[] palette(@ArrayRes int arrayRes) {
        TypedArray array = getResources().obtainTypedArray(arrayRes);
        try {
            int[] colors = new int[array.length()];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = array.getColor(i, 0);
            }
            return colors;
        } finally {
            array.recycle();
        }
    }

    private QrStyle.Shape shapeFor(int chipId) {
        if (chipId == R.id.chip_shape_rounded) return QrStyle.Shape.ROUNDED;
        if (chipId == R.id.chip_shape_dot) return QrStyle.Shape.DOT;
        return QrStyle.Shape.SQUARE;
    }

    private QrStyle.Ecc eccFor(int chipId) {
        if (chipId == R.id.chip_ecc_l) return QrStyle.Ecc.L;
        if (chipId == R.id.chip_ecc_q) return QrStyle.Ecc.Q;
        if (chipId == R.id.chip_ecc_h) return QrStyle.Ecc.H;
        return QrStyle.Ecc.M;
    }

    private void generate() {
        String payload = buildPayload();
        if (payload == null) {
            Toast.makeText(this, R.string.empty_input_error, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.generate(payload);
    }

    /** Builds the encoded string for the current type, or null if required input is missing. */
    private String buildPayload() {
        switch (current) {
            case URL: {
                String url = text(R.id.edt_text);
                return url.isEmpty() ? null : QrPayload.url(url);
            }
            case WIFI: {
                String ssid = text(R.id.edt_wifi_ssid);
                if (ssid.isEmpty()) return null;
                return QrPayload.wifi(ssid, text(R.id.edt_wifi_pass), wifiAuth());
            }
            case CONTACT: {
                String name = text(R.id.edt_c_name);
                String phone = text(R.id.edt_c_phone);
                if (name.isEmpty() && phone.isEmpty()) return null;
                return QrPayload.vCard(name, phone, text(R.id.edt_c_email));
            }
            case PHONE: {
                String phone = text(R.id.edt_phone);
                return phone.isEmpty() ? null : QrPayload.phone(phone);
            }
            case SMS: {
                String number = text(R.id.edt_sms_number);
                if (number.isEmpty()) return null;
                return QrPayload.sms(number, text(R.id.edt_sms_message));
            }
            case EMAIL: {
                String addr = text(R.id.edt_email_addr);
                if (addr.isEmpty()) return null;
                return QrPayload.email(addr, text(R.id.edt_email_subject),
                        text(R.id.edt_email_body));
            }
            case TEXT:
            default: {
                String t = text(R.id.edt_text);
                return t.isEmpty() ? null : t;
            }
        }
    }

    private String wifiAuth() {
        int pos = ((Spinner) findViewById(R.id.spn_wifi_auth)).getSelectedItemPosition();
        switch (pos) {
            case 1:
                return "WEP";
            case 2:
                return "nopass";
            case 0:
            default:
                return "WPA";
        }
    }

    private String text(int viewId) {
        return ((EditText) findViewById(viewId)).getText().toString().trim();
    }

    private void observeViewModel() {
        viewModel.getQrBitmap().observe(this, bitmap -> {
            Bitmap previous = currentQr;
            currentQr = bitmap;
            imgQr.setImageBitmap(bitmap);
            // Restyling re-renders on every tap, and each bitmap is a couple of
            // megabytes. The old one is only let go once the ImageView holds the
            // new one, so nothing can be drawn after it is recycled.
            if (previous != null && previous != bitmap && !previous.isRecycled()) {
                previous.recycle();
            }
            btnShare.setEnabled(true);
            btnSave.setEnabled(true);
        });
        viewModel.getError().observe(this, messageRes -> {
            if (messageRes == null) {
                return;
            }
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
            viewModel.errorShown();
        });
    }

    private void onSaveClicked() {
        if (currentQr == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        saveToGallery();
    }

    private void saveToGallery() {
        if (currentQr == null) {
            return;
        }
        try {
            QrGenerator.saveToGallery(this, currentQr);
            Toast.makeText(this, R.string.saved_to_gallery, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("GenerateActivity", "Failed to save QR", e);
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQr() {
        if (currentQr == null) {
            return;
        }
        try {
            Uri uri = QrGenerator.saveForSharing(this, currentQr);
            String description = getString(R.string.share_description)
                    + "\n" + StoreLinks.appUrl(this);
            if (!TextUtils.isEmpty(viewModel.getEncodedText())) {
                description = viewModel.getEncodedText() + "\n\n" + description;
            }

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, description);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.share_via)));
        } catch (IOException e) {
            Log.e("GenerateActivity", "Failed to share QR", e);
            Toast.makeText(this, R.string.qr_generate_error, Toast.LENGTH_SHORT).show();
        }
    }
}
