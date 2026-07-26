package ir.meros.qrscanner.qr;

import android.text.TextUtils;

/**
 * Builds the raw string payloads that go inside a QR code for each supported
 * type (URL, WiFi, contact, …), following the de-facto encoding conventions
 * that scanner apps understand.
 */
public final class QrPayload {

    private QrPayload() {
    }

    /** Ensures a URL carries a scheme so scanners treat it as a link. */
    public static String url(String url) {
        String u = safe(url);
        if (u.isEmpty()) {
            return u;
        }
        if (!u.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            return "https://" + u;
        }
        return u;
    }

    public static String phone(String number) {
        return "tel:" + safe(number);
    }

    public static String sms(String number, String message) {
        String base = "smsto:" + safe(number);
        return TextUtils.isEmpty(message) ? base : base + ":" + message.trim();
    }

    public static String email(String address, String subject, String body) {
        StringBuilder sb = new StringBuilder("mailto:").append(safe(address));
        boolean hasSubject = !TextUtils.isEmpty(subject);
        boolean hasBody = !TextUtils.isEmpty(body);
        if (hasSubject || hasBody) {
            sb.append('?');
            if (hasSubject) {
                sb.append("subject=").append(enc(subject));
            }
            if (hasBody) {
                sb.append(hasSubject ? "&" : "").append("body=").append(enc(body));
            }
        }
        return sb.toString();
    }

    /** {@code auth} is one of WPA / WEP / nopass. */
    public static String wifi(String ssid, String password, String auth) {
        String a = TextUtils.isEmpty(auth) ? "WPA" : auth;
        return "WIFI:T:" + a
                + ";S:" + escapeWifi(safe(ssid))
                + ";P:" + escapeWifi(safe(password))
                + ";;";
    }

    public static String vCard(String name, String phone, String email) {
        return "BEGIN:VCARD\nVERSION:3.0\n"
                + "N:" + safe(name) + "\n"
                + "FN:" + safe(name) + "\n"
                + "TEL:" + safe(phone) + "\n"
                + "EMAIL:" + safe(email) + "\n"
                + "END:VCARD";
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String enc(String s) {
        return android.net.Uri.encode(s.trim());
    }

    /** WiFi payloads must escape \ ; , : and " with a backslash. */
    private static String escapeWifi(String s) {
        return s.replaceAll("([\\\\;,:\"])", "\\\\$1");
    }
}
