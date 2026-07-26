package ir.meros.qrscanner.ui.generate;

import android.app.Application;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.zxing.WriterException;

import ir.meros.qrscanner.data.HistoryRepository;
import ir.meros.qrscanner.model.QrItem;
import ir.meros.qrscanner.qr.QrGenerator;

/**
 * Owns QR generation state for {@link GenerateActivity}. The View passes a fully
 * built payload to {@link #generate(String)} and observes {@link #getQrBitmap()}
 * / {@link #getError()}. Successful generations are recorded in the history.
 */
public class GenerateViewModel extends AndroidViewModel {

    private static final String TAG = "GenerateViewModel";
    private static final int QR_SIZE = 800;

    private final MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private final MutableLiveData<Boolean> error = new MutableLiveData<>();
    private final HistoryRepository history;

    /** The payload currently encoded, retained so sharing can reference it. */
    private String encodedText;

    public GenerateViewModel(@NonNull Application application) {
        super(application);
        this.history = new HistoryRepository(application);
    }

    public LiveData<Bitmap> getQrBitmap() {
        return qrBitmap;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public String getEncodedText() {
        return encodedText;
    }

    /** Encodes {@code payload} into a QR bitmap and records it in the history. */
    public void generate(String payload) {
        if (TextUtils.isEmpty(payload) || payload.trim().isEmpty()) {
            error.setValue(true);
            return;
        }
        try {
            Bitmap bitmap = QrGenerator.generate(payload.trim(), QR_SIZE);
            encodedText = payload.trim();
            qrBitmap.setValue(bitmap);
            history.add(QrItem.of(encodedText, true));
        } catch (WriterException e) {
            Log.e(TAG, "Failed to encode QR", e);
            error.setValue(true);
        }
    }
}
