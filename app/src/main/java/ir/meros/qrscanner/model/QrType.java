package ir.meros.qrscanner.model;

import android.util.Patterns;

/**
 * The kind of payload a QR code carries. {@link #detect(String)} classifies raw
 * scanned/typed content so the UI can offer the right action (open, dial, …) and
 * the matching icon.
 */
public enum QrType {
    URL,
    PHONE,
    EMAIL,
    SMS,
    WIFI,
    GEO,
    CONTACT,
    TEXT;

    /** Best-effort classification of {@code content} into a {@link QrType}. */
    public static QrType detect(String content) {
        if (content == null) {
            return TEXT;
        }
        String c = content.trim();
        String lower = c.toLowerCase();

        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("www.")) {
            return URL;
        }
        if (lower.startsWith("wifi:")) {
            return WIFI;
        }
        if (lower.startsWith("tel:")) {
            return PHONE;
        }
        if (lower.startsWith("smsto:") || lower.startsWith("sms:")) {
            return SMS;
        }
        if (lower.startsWith("mailto:") || lower.startsWith("matmsg:")) {
            return EMAIL;
        }
        if (lower.startsWith("begin:vcard") || lower.startsWith("mecard:")) {
            return CONTACT;
        }
        if (lower.startsWith("geo:")) {
            return GEO;
        }
        if (Patterns.WEB_URL.matcher(c).matches()) {
            return URL;
        }
        if (Patterns.EMAIL_ADDRESS.matcher(c).matches()) {
            return EMAIL;
        }
        if (Patterns.PHONE.matcher(c).matches() && c.length() >= 5) {
            return PHONE;
        }
        return TEXT;
    }
}
