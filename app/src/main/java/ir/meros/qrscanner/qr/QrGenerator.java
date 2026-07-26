package ir.meros.qrscanner.qr;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Stateless helper that turns text into a QR {@link Bitmap} and, when the user
 * wants to share it, writes that bitmap into the app cache so a
 * {@link FileProvider} content Uri can be handed to other apps.
 */
public final class QrGenerator {

    private static final String AUTHORITY = "ir.meros.qrscanner.fileprovider";
    private static final String SHARE_DIR = "images";

    private QrGenerator() {
    }

    /** Encodes {@code text} as a square QR bitmap, or throws if it cannot. */
    public static Bitmap generate(String text, int size) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * Persists {@code bitmap} into the cache directory and returns a shareable
     * content Uri. The path ({@code cache/images}) matches file_paths.xml.
     */
    public static Uri saveForSharing(Context context, Bitmap bitmap) throws IOException {
        File dir = new File(context.getCacheDir(), SHARE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create share directory");
        }
        File file = new File(dir, "qr_share.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        return FileProvider.getUriForFile(context, AUTHORITY, file);
    }

    /**
     * Saves {@code bitmap} into the device's public Pictures/QrScanner album via
     * MediaStore. On API 29+ this needs no runtime permission; on older versions
     * the caller must already hold WRITE_EXTERNAL_STORAGE. Returns the media Uri.
     */
    public static Uri saveToGallery(Context context, Bitmap bitmap) throws IOException {
        String name = "qr_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/QrScanner");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = context.getContentResolver().insert(collection, values);
        if (uri == null) {
            throw new IOException("Could not create MediaStore entry");
        }
        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw new IOException("Could not write bitmap to gallery");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(uri, values, null, null);
        }
        return uri;
    }
}
