package ir.meros.qrscanner.qr;

import android.graphics.Color;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * The look of a generated QR code: module colour, background, module shape and
 * error-correction level. Held by the Generate screen and handed to
 * {@link QrGenerator#generate(String, int, QrStyle)} on every render, so the
 * user can restyle a code without retyping its content.
 */
public final class QrStyle {

    /** How each dark module is painted. */
    public enum Shape {SQUARE, ROUNDED, DOT}

    /**
     * Error-correction level. Higher levels survive more damage (and leave room
     * for a logo) at the cost of a denser, larger code.
     */
    public enum Ecc {
        L(ErrorCorrectionLevel.L),
        M(ErrorCorrectionLevel.M),
        Q(ErrorCorrectionLevel.Q),
        H(ErrorCorrectionLevel.H);

        private final ErrorCorrectionLevel level;

        Ecc(ErrorCorrectionLevel level) {
            this.level = level;
        }

        ErrorCorrectionLevel level() {
            return level;
        }
    }

    /** Quiet zone in modules. The spec asks for 4; less and scanners struggle. */
    static final int QUIET_ZONE = 4;

    private int foreground = Color.BLACK;
    private int background = Color.WHITE;
    private Shape shape = Shape.SQUARE;
    private Ecc ecc = Ecc.M;

    public int foreground() {
        return foreground;
    }

    public int background() {
        return background;
    }

    public Shape shape() {
        return shape;
    }

    public Ecc ecc() {
        return ecc;
    }

    public QrStyle foreground(int color) {
        this.foreground = color;
        return this;
    }

    public QrStyle background(int color) {
        this.background = color;
        return this;
    }

    public QrStyle shape(Shape shape) {
        this.shape = shape;
        return this;
    }

    public QrStyle ecc(Ecc ecc) {
        this.ecc = ecc;
        return this;
    }

    /**
     * Whether the chosen colours are far enough apart for a camera to tell the
     * modules from the background. A transparent background is judged against
     * white, which is what it will sit on in every preview and most viewers.
     */
    public boolean hasReadableContrast() {
        int bg = Color.alpha(background) == 0 ? Color.WHITE : background;
        double lighter = Math.max(luminance(foreground), luminance(bg));
        double darker = Math.min(luminance(foreground), luminance(bg));
        return (lighter + 0.05) / (darker + 0.05) >= 3.0;
    }

    /** Relative luminance, per WCAG. */
    private static double luminance(int color) {
        return 0.2126 * channel(Color.red(color))
                + 0.7152 * channel(Color.green(color))
                + 0.0722 * channel(Color.blue(color));
    }

    private static double channel(int value) {
        double c = value / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
