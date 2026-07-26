package ir.meros.qrscanner.qr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Decodes a QR/barcode out of an image the user picked from the gallery, so
 * scanning is not limited to the live camera.
 */
public final class QrDecoder {

    private QrDecoder() {
    }

    /** Returns the decoded text, or {@code null} if no code was found. */
    public static String decode(Context context, Uri imageUri) {
        Bitmap bitmap = loadBitmap(context, imageUri);
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();

        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binary = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        try {
            Result result = new MultiFormatReader().decode(binary, hints);
            return result != null ? result.getText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap loadBitmap(Context context, Uri uri) {
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream);
        } catch (Exception e) {
            return null;
        }
    }
}
