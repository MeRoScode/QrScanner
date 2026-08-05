package ir.meros.qrscanner.ui.generate;

import android.app.Application;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ir.meros.qrscanner.R;
import ir.meros.qrscanner.ads.AdGate;
import ir.meros.qrscanner.data.HistoryRepository;
import ir.meros.qrscanner.model.QrItem;
import ir.meros.qrscanner.qr.QrGenerator;
import ir.meros.qrscanner.qr.QrStyle;

/**
 * Owns QR generation state for {@link GenerateActivity}. The View passes a fully
 * built payload to {@link #generate(String, QrStyle)} and observes
 * {@link #getQrBitmap()} / {@link #getError()}. Successful generations are
 * recorded in the history; restyling an existing code is not, since the content
 * is already there.
 */
public class GenerateViewModel extends AndroidViewModel {

    private static final String TAG = "GenerateViewModel";
    private static final int QR_SIZE = 800;

    /**
     * Longest content we hand to the encoder. A QR tops out around 2953 bytes
     * and far less at the higher error-correction levels, so anything past this
     * is certain to fail — better a clear message than a generic error.
     */
    private static final int MAX_INPUT_CHARS = 1200;

    private final MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private final MutableLiveData<Integer> error = new MutableLiveData<>();
    private final HistoryRepository history;

    /**
     * The look applied to every render. Kept here rather than in the Activity so
     * a rotation does not silently reset colours the user picked while the code
     * on screen still shows them.
     */
    private final QrStyle style = new QrStyle();

    /** The payload currently encoded, retained so sharing can reference it. */
    private String encodedText;

    public GenerateViewModel(@NonNull Application application) {
        super(application);
        this.history = new HistoryRepository(application);
    }

    public LiveData<Bitmap> getQrBitmap() {
        return qrBitmap;
    }

    /** Emits the string resource of the failure to show, or null on success. */
    public LiveData<Integer> getError() {
        return error;
    }

    public String getEncodedText() {
        return encodedText;
    }

    /** The live style object; the options panel edits it in place. */
    public QrStyle getStyle() {
        return style;
    }

    /** Marks the last failure as delivered, so rotation does not replay it. */
    public void errorShown() {
        error.setValue(null);
    }

    /** Encodes {@code payload} into a QR bitmap and records it in the history. */
    public void generate(String payload) {
        if (TextUtils.isEmpty(payload) || payload.trim().isEmpty()) {
            error.setValue(R.string.empty_input_error);
            return;
        }
        String text = payload.trim();
        if (encode(text)) {
            encodedText = text;
            history.add(QrItem.of(text, true));
            // Counted here rather than when the bitmap arrives, so that
            // restyling a code — or a rotation replaying the last bitmap —
            // does not spend ad credit the user never earned. The ad itself is
            // never shown here: it would cover a code the user wants to save.
            AdGate.recordAction(getApplication());
        }
    }

    /**
     * Re-renders the code already on screen with a new look. Does nothing when
     * nothing has been generated yet.
     */
    public void restyle() {
        if (!TextUtils.isEmpty(encodedText)) {
            encode(encodedText);
        }
    }

    /** Renders one bitmap, reporting any failure instead of letting it escape. */
    private boolean encode(String text) {
        if (text.length() > MAX_INPUT_CHARS) {
            error.setValue(R.string.input_too_long);
            return false;
        }
        try {
            qrBitmap.setValue(QrGenerator.generate(text, QR_SIZE, style));
            return true;
        } catch (OutOfMemoryError e) {
            // Bitmaps this size are the biggest allocation the app makes; on a
            // low-memory device one can fail without the process being doomed.
            Log.e(TAG, "Out of memory encoding QR", e);
            error.setValue(R.string.qr_generate_error);
            return false;
        } catch (Exception e) {
            // ZXing signals "does not fit" with a checked WriterException but
            // throws unchecked IllegalArgumentException / index errors for
            // other unencodable input. Neither should take the app down.
            Log.e(TAG, "Failed to encode QR", e);
            error.setValue(isTooLong(e) ? R.string.input_too_long : R.string.qr_generate_error);
            return false;
        }
    }

    private static boolean isTooLong(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("Data too big");
    }
}
