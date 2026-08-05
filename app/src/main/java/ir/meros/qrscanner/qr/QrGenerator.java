package ir.meros.qrscanner.qr;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Stateless helper that turns text into a QR {@link Bitmap} and, when the user
 * wants to share it, writes that bitmap into the app cache so a
 * {@link FileProvider} content Uri can be handed to other apps.
 */
public final class QrGenerator {

    private static final String AUTHORITY = "ir.meros.qrscanner.fileprovider";
    private static final String SHARE_DIR = "images";

    /** The three 7x7 alignment squares scanners lock onto, one per corner. */
    private static final int FINDER_MODULES = 7;

    private QrGenerator() {
    }

    /** Encodes {@code text} as a square QR bitmap in the default look. */
    public static Bitmap generate(String text, int size) throws WriterException {
        return generate(text, size, new QrStyle());
    }

    /**
     * Encodes {@code text} as a square QR bitmap painted in {@code style}.
     *
     * <p>The module grid is rendered by hand rather than through
     * {@code QRCodeWriter} so each module can be drawn as a square, a rounded
     * square or a dot, and so the quiet zone stays exact at any bitmap size.</p>
     *
     * @param size the desired edge in pixels; the result is rounded down to a
     *             whole number of pixels per module, so it may be slightly less.
     * @throws WriterException if the content does not fit in a QR code.
     */
    public static Bitmap generate(String text, int size, QrStyle style) throws WriterException {
        // UTF-8 (with an ECI header) rather than ZXing's ISO-8859-1 default —
        // without it Persian, Kurdish and Turkish content comes back mangled.
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCode code = Encoder.encode(text, style.ecc().level(), hints);
        ByteMatrix matrix = code.getMatrix();
        if (matrix == null) {
            throw new WriterException("Encoder produced no matrix");
        }

        int modules = matrix.getWidth();
        int total = modules + QrStyle.QUIET_ZONE * 2;
        int scale = Math.max(1, size / total);
        int edge = scale * total;

        Bitmap bitmap = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (Color.alpha(style.background()) != 0) {
            canvas.drawColor(style.background());
        }

        QrStyle.Shape shape = style.shape();
        Paint paint = new Paint();
        paint.setAntiAlias(shape != QrStyle.Shape.SQUARE);
        paint.setColor(style.foreground());
        paint.setStyle(Paint.Style.FILL);

        // Styled shapes draw the finder squares separately: rendered module by
        // module they would come out as a grid of dots that scanners misread.
        boolean styledFinders = shape != QrStyle.Shape.SQUARE;
        for (int y = 0; y < modules; y++) {
            for (int x = 0; x < modules; x++) {
                if (matrix.get(x, y) != 1) {
                    continue;
                }
                if (styledFinders && isInFinder(x, y, modules)) {
                    continue;
                }
                drawModule(canvas, paint, shape,
                        (QrStyle.QUIET_ZONE + x) * scale,
                        (QrStyle.QUIET_ZONE + y) * scale,
                        scale);
            }
        }
        if (styledFinders) {
            int far = (QrStyle.QUIET_ZONE + modules - FINDER_MODULES) * scale;
            int near = QrStyle.QUIET_ZONE * scale;
            drawFinder(canvas, paint, near, near, scale, shape);
            drawFinder(canvas, paint, far, near, scale, shape);
            drawFinder(canvas, paint, near, far, scale, shape);
        }
        return bitmap;
    }

    private static void drawModule(Canvas canvas, Paint paint, QrStyle.Shape shape,
                                   int left, int top, int scale) {
        switch (shape) {
            case DOT:
                canvas.drawCircle(left + scale / 2f, top + scale / 2f, scale * 0.46f, paint);
                break;
            case ROUNDED:
                canvas.drawRoundRect(new RectF(left, top, left + scale, top + scale),
                        scale * 0.3f, scale * 0.3f, paint);
                break;
            case SQUARE:
            default:
                canvas.drawRect(left, top, left + scale, top + scale, paint);
                break;
        }
    }

    /**
     * Paints one finder pattern as a ring plus a centre blob, matching the
     * roundness of the module shape. The ring is stroked rather than filled so
     * nothing has to be punched back out — which would be impossible over a
     * transparent background.
     */
    private static void drawFinder(Canvas canvas, Paint paint, int left, int top,
                                   int scale, QrStyle.Shape shape) {
        float outerRadius = shape == QrStyle.Shape.DOT ? scale * 3.5f : scale * 2f;
        float innerRadius = shape == QrStyle.Shape.DOT ? scale * 1.5f : scale * 0.9f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(scale);
        // Inset by half a module so the stroke covers the outermost ring exactly.
        canvas.drawRoundRect(
                new RectF(left + scale * 0.5f, top + scale * 0.5f,
                        left + scale * 6.5f, top + scale * 6.5f),
                outerRadius, outerRadius, paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(
                new RectF(left + scale * 2f, top + scale * 2f,
                        left + scale * 5f, top + scale * 5f),
                innerRadius, innerRadius, paint);
    }

    private static boolean isInFinder(int x, int y, int modules) {
        int far = modules - FINDER_MODULES;
        return (x < FINDER_MODULES && y < FINDER_MODULES)
                || (x >= far && y < FINDER_MODULES)
                || (x < FINDER_MODULES && y >= far);
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
