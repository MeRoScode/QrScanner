package ir.meros.qrscanner.ui;

import android.content.Context;

import androidx.annotation.DrawableRes;

import ir.meros.qrscanner.R;
import ir.meros.qrscanner.model.QrType;

/**
 * Maps a {@link QrType} to its icon and localized label, shared by the history
 * list and the result sheet so the two stay visually consistent.
 */
public final class QrTypeUi {

    private QrTypeUi() {
    }

    @DrawableRes
    public static int icon(QrType type) {
        switch (type) {
            case URL:
                return R.drawable.ic_link;
            case PHONE:
                return R.drawable.ic_phone;
            case EMAIL:
                return R.drawable.ic_email;
            case SMS:
                return R.drawable.ic_sms;
            case WIFI:
                return R.drawable.ic_wifi;
            case GEO:
                return R.drawable.ic_location;
            case CONTACT:
                return R.drawable.ic_contact;
            case TEXT:
            default:
                return R.drawable.ic_text;
        }
    }

    public static String label(Context context, QrType type) {
        int res;
        switch (type) {
            case URL:
                res = R.string.type_url;
                break;
            case PHONE:
                res = R.string.type_phone;
                break;
            case EMAIL:
                res = R.string.type_email;
                break;
            case SMS:
                res = R.string.type_sms;
                break;
            case WIFI:
                res = R.string.type_wifi;
                break;
            case GEO:
                res = R.string.type_location;
                break;
            case CONTACT:
                res = R.string.type_contact;
                break;
            case TEXT:
            default:
                res = R.string.type_text;
                break;
        }
        return context.getString(res);
    }
}
