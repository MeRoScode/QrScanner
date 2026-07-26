package ir.meros.qrscanner.ui.main;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import ir.meros.qrscanner.R;
import ir.meros.qrscanner.model.QrItem;
import ir.meros.qrscanner.model.QrType;
import ir.meros.qrscanner.ui.QrTypeUi;
import ir.meros.qrscanner.ui.StoreLinks;

/**
 * Bottom sheet that presents a scanned/opened QR item: its type, content, and a
 * type-aware primary action (open link, dial, email …) plus copy and share.
 */
public final class ResultSheet {

    private ResultSheet() {
    }

    public static void show(Activity activity, QrItem item) {
        show(activity, item, null);
    }

    /**
     * Same as {@link #show(Activity, QrItem)}, but runs {@code onDismiss} once
     * the sheet closes — the natural break point for anything that would
     * otherwise cover the result.
     */
    public static void show(Activity activity, QrItem item, Runnable onDismiss) {
        View view = LayoutInflater.from(activity).inflate(R.layout.sheet_result, null);
        QrType type = item.getType();
        String content = item.getContent();

        ((ImageView) view.findViewById(R.id.img_type))
                .setImageResource(QrTypeUi.icon(type));
        ((TextView) view.findViewById(R.id.txv_type))
                .setText(QrTypeUi.label(activity, type));
        ((TextView) view.findViewById(R.id.txv_content)).setText(content);

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        parent.setBackground(new ColorDrawable(Color.TRANSPARENT));

        AppCompatButton primary = view.findViewById(R.id.btn_primary);
        configurePrimary(activity, primary, type, content);

        view.findViewById(R.id.btn_copy).setOnClickListener(v ->
                copy(activity, content));
        view.findViewById(R.id.btn_share).setOnClickListener(v ->
                share(activity, content));

        if (onDismiss != null) {
            dialog.setOnDismissListener(d -> onDismiss.run());
        }
        dialog.show();
    }

    private static void configurePrimary(Activity activity, AppCompatButton primary,
                                         QrType type, String content) {
        Intent intent = primaryIntent(type, content);
        int labelRes = primaryLabel(type);
        if (intent == null || labelRes == 0) {
            primary.setVisibility(View.GONE);
            return;
        }
        primary.setText(labelRes);
        primary.setOnClickListener(v -> launch(activity, intent));
    }

    private static Intent primaryIntent(QrType type, String content) {
        switch (type) {
            case URL:
                return new Intent(Intent.ACTION_VIEW, Uri.parse(withScheme(content)));
            case PHONE:
                return new Intent(Intent.ACTION_DIAL, Uri.parse(ensurePrefix(content, "tel:")));
            case EMAIL:
                return new Intent(Intent.ACTION_SENDTO,
                        Uri.parse(ensurePrefix(content, "mailto:")));
            case SMS:
                return new Intent(Intent.ACTION_SENDTO,
                        Uri.parse(smsUri(content)));
            case GEO:
                return new Intent(Intent.ACTION_VIEW, Uri.parse(content));
            default:
                return null;
        }
    }

    private static int primaryLabel(QrType type) {
        switch (type) {
            case URL:
                return R.string.action_open;
            case PHONE:
                return R.string.action_call;
            case EMAIL:
                return R.string.action_email;
            case SMS:
                return R.string.action_sms;
            case GEO:
                return R.string.action_map;
            default:
                return 0;
        }
    }

    private static String withScheme(String url) {
        String u = url.trim();
        if (u.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            return u;
        }
        return "https://" + u;
    }

    private static String ensurePrefix(String value, String prefix) {
        return value.toLowerCase().startsWith(prefix) ? value : prefix + value.trim();
    }

    private static String smsUri(String content) {
        if (content.toLowerCase().startsWith("sms:")) {
            return "smsto:" + content.substring(4);
        }
        return ensurePrefix(content, "smsto:");
    }

    private static void launch(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    private static void copy(Context context, String content) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("qr", content));
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static void share(Activity activity, String content) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        // Generic blurb, not share_description: this content was scanned, not
        // made with the app, so "created with QR Scanner" would be a false claim.
        share.putExtra(Intent.EXTRA_TEXT, content + "\n\n"
                + activity.getString(R.string.share_app_promo)
                + "\n" + StoreLinks.appUrl(activity));
        activity.startActivity(Intent.createChooser(share,
                activity.getString(R.string.action_share)));
    }
}
