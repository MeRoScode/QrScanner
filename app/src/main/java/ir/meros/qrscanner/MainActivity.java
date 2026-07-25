package ir.meros.qrscanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import ir.meros.qrscanner.ads.TapsellAdManager;
import ir.meros.qrscanner.model.QrItem;
import ir.meros.qrscanner.qr.QrDecoder;
import ir.meros.qrscanner.ui.SystemBars;
import ir.meros.qrscanner.ui.generate.GenerateActivity;
import ir.meros.qrscanner.ui.main.HistoryAdapter;
import ir.meros.qrscanner.ui.main.MainViewModel;
import ir.meros.qrscanner.ui.main.ResultSheet;

/**
 * Home screen: launches camera/image scanning and QR creation, and shows the
 * scan/create history. Scan results open in a {@link ResultSheet}. All state
 * lives in {@link MainViewModel}; this class only wires views to it.
 */
public class MainActivity extends AppCompatActivity {

    /** BCP-47 tags, in the same order as the R.array.language_names entries. */
    private static final String[] LANG_TAGS = {"fa", "ckb", "tr", "en"};

    private MainViewModel viewModel;
    private HistoryAdapter adapter;
    private View emptyState;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result ->
                    handleScanned(result != null ? result.getContents() : null));

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handlePickedImage);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        emptyState = findViewById(R.id.empty_state);
        SystemBars.pad(findViewById(R.id.main_root));

        setupRecycler();
        setupButtons();
        observeViewModel();
        setupAds();

        findViewById(R.id.main_root)
                .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_up));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Picks up items created on the Generate screen.
        viewModel.reload();
    }

    private void setupRecycler() {
        adapter = new HistoryAdapter(new HistoryAdapter.Listener() {
            @Override
            public void onOpen(QrItem item) {
                ResultSheet.show(MainActivity.this, item);
            }

            @Override
            public void onDelete(QrItem item) {
                viewModel.delete(item);
            }
        });
        RecyclerView recyclerView = findViewById(R.id.rec);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        findViewById(R.id.btn_scan).setOnClickListener(v -> scanCode());
        findViewById(R.id.btn_create_qr).setOnClickListener(v ->
                startActivity(new Intent(this, GenerateActivity.class)));
        findViewById(R.id.btn_scan_image).setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_clear).setOnClickListener(v -> viewModel.clear());
        findViewById(R.id.btn_language).setOnClickListener(v -> showLanguageDialog());
    }

    private void showLanguageDialog() {
        String[] names = getResources().getStringArray(R.array.language_names);
        int current = indexOfCurrentLanguage();
        new AlertDialog.Builder(this)
                .setTitle(R.string.language)
                .setSingleChoiceItems(names, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) {
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(LANG_TAGS[which]));
                    }
                })
                .show();
    }

    /** Index into {@link #LANG_TAGS} for the active language, or -1 if none matches. */
    private int indexOfCurrentLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return -1;
        }
        String lang = locales.get(0).getLanguage();
        for (int i = 0; i < LANG_TAGS.length; i++) {
            if (LANG_TAGS[i].equals(lang)) {
                return i;
            }
        }
        return -1;
    }

    private void observeViewModel() {
        viewModel.getHistory().observe(this, items -> {
            adapter.submit(items);
            boolean empty = items == null || items.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void setupAds() {
        TapsellAdManager.init(this);
        ViewGroup topBanner = findViewById(R.id.banner_top);
        ViewGroup bottomBanner = findViewById(R.id.banner_bottom);
        TapsellAdManager.loadBanner(this, topBanner, TapsellAdManager.ZONE_TOP);
        TapsellAdManager.loadBanner(this, bottomBanner, TapsellAdManager.ZONE_BOTTOM);
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        scanLauncher.launch(options);
    }

    private void handleScanned(String contents) {
        if (contents == null) {
            return;
        }
        QrItem item = viewModel.recordScan(contents);
        ResultSheet.show(this, item);
    }

    private void handlePickedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        String contents = QrDecoder.decode(this, uri);
        if (contents == null) {
            Toast.makeText(this, R.string.no_qr_found, Toast.LENGTH_SHORT).show();
            return;
        }
        QrItem item = viewModel.recordScan(contents);
        ResultSheet.show(this, item);
    }
}
