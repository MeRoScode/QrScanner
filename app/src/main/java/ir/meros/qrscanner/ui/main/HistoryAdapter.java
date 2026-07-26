package ir.meros.qrscanner.ui.main;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ir.meros.qrscanner.R;
import ir.meros.qrscanner.model.QrItem;
import ir.meros.qrscanner.ui.QrTypeUi;

/**
 * Renders the QR history list. Each row shows the content, a type icon, a
 * scanned/created + relative-time subtitle, and a delete button. Row taps and
 * deletes are surfaced through {@link Listener}.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface Listener {
        void onOpen(QrItem item);

        void onDelete(QrItem item);
    }

    private final List<QrItem> items = new ArrayList<>();
    private final Listener listener;

    public HistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<QrItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        QrItem item = items.get(position);
        holder.content.setText(item.getContent());
        holder.icon.setImageResource(QrTypeUi.icon(item.getType()));

        String kind = holder.itemView.getContext().getString(
                item.isGenerated() ? R.string.create_qr : R.string.scan_qr);
        String time = DateUtils.getRelativeTimeSpanString(item.getTimestamp()).toString();
        String type = QrTypeUi.label(holder.itemView.getContext(), item.getType());
        holder.meta.setText(type + " · " + kind + " · " + time);

        holder.itemView.setOnClickListener(v -> listener.onOpen(item));
        holder.delete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView content;
        final TextView meta;
        final ImageButton delete;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.img_type);
            content = itemView.findViewById(R.id.txv_content);
            meta = itemView.findViewById(R.id.txv_meta);
            delete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
