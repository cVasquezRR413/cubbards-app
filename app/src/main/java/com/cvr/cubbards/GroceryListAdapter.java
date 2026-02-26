package com.cvr.cubbards;

import android.graphics.Color; // ✅ added
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cvr.cubbards.data.GroceryRow;

import java.util.ArrayList;
import java.util.List;

public class GroceryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onItemClicked(GroceryRow row);      // close if open
        void onToggleCompleted(GroceryRow row);  // toggle completion if closed
        void onDeleteClicked(GroceryRow row);
        void onEditClicked(GroceryRow row);
    }

    // ---- UI Row model (headers + items) ----
    public interface UiRow {}

    public static class StoreHeaderRow implements UiRow {
        public final String storeName;       // nullable
        public final String storeLocation;   // nullable

        public StoreHeaderRow(String storeName, String storeLocation) {
            this.storeName = storeName;
            this.storeLocation = storeLocation;
        }
    }

    public static class ItemRow implements UiRow {
        public final GroceryRow row;

        public ItemRow(GroceryRow row) {
            this.row = row;
        }
    }

    private static final int VT_HEADER = 0;
    private static final int VT_ITEM = 1;

    private final Listener listener;
    private final List<UiRow> rows = new ArrayList<>();

    // Swipe reveal state
    private int openedPos = RecyclerView.NO_POSITION;
    private float revealWidthPx = 0f;

    // ✅ added (matches your XML defaults)
    private static final int COLOR_NAME_NORMAL = Color.parseColor("#1F1F1F");
    private static final int COLOR_NAME_COMPLETED = Color.parseColor("#9E9E9E");
    private static final int COLOR_PRICE_NORMAL = Color.parseColor("#777777");
    private static final int COLOR_PRICE_COMPLETED = Color.parseColor("#B0B0B0");

    public GroceryListAdapter(Listener listener) {
        this.listener = listener;
    }

    // Activity expects this name
    public void setRevealWidthPx(float px) {
        this.revealWidthPx = px;
    }

    // Activity expects this
    public void setOpenedPos(int pos) {
        this.openedPos = pos;
        notifyDataSetChanged();
    }

    // Activity expects this
    public int getOpenedPos() {
        return openedPos;
    }

    // Activity swipe code calls this
    public UiRow getUiRow(int adapterPos) {
        if (adapterPos < 0 || adapterPos >= rows.size()) return null;
        return rows.get(adapterPos);
    }

    // Activity refresh uses this
    public void setRows(List<UiRow> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);

        if (openedPos >= rows.size()) openedPos = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        UiRow r = rows.get(position);
        return (r instanceof StoreHeaderRow) ? VT_HEADER : VT_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VT_HEADER) {
            View v = inflater.inflate(R.layout.row_store_header, parent, false);
            return new StoreHeaderVH(v);
        } else {
            View v = inflater.inflate(R.layout.row_grocery_item, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        UiRow r = rows.get(position);

        if (holder instanceof StoreHeaderVH) {
            ((StoreHeaderVH) holder).bind((StoreHeaderRow) r);
            return;
        }

        ItemVH vh = (ItemVH) holder;
        GroceryRow row = ((ItemRow) r).row;

        // --- Name (now includes "(N) " inline so wrapping looks natural) ---
        int buyQty = row.buyQuantity;
        String prefix = (buyQty > 1) ? "(" + buyQty + ") " : "";
        String name = (row.name == null) ? "" : row.name;
        String full = prefix + name;

        SpannableString ss = new SpannableString(full);

        // --- Completed UI (strike-through NAME only, not prefix) ---
        if (row.isCompleted) {
            int startName = prefix.length();
            int endName = full.length();
            if (endName > startName) {
                ss.setSpan(new StrikethroughSpan(), startName, endName, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            vh.tvName.setSingleLine(true);
            vh.tvName.setEllipsize(TextUtils.TruncateAt.END);
        } else {
            vh.tvName.setSingleLine(false);
            vh.tvName.setEllipsize(null);
            vh.tvName.setMaxLines(3);
        }

        vh.tvName.setText(ss);

        // ✅ added: grey out top line when completed (name + "(N)" prefix + price)
        if (row.isCompleted) {
            vh.tvName.setTextColor(COLOR_NAME_COMPLETED);
            vh.tvPrice.setTextColor(COLOR_PRICE_COMPLETED);
        } else {
            vh.tvName.setTextColor(COLOR_NAME_NORMAL);
            vh.tvPrice.setTextColor(COLOR_PRICE_NORMAL);
        }

        // --- Price ---
        if (row.priceCents != null) {
            int abs = Math.abs(row.priceCents);
            int dollars = abs / 100;
            int cents = abs % 100;

            String price = "$" + dollars + "." +
                    (cents < 10 ? "0" + cents : String.valueOf(cents));

            vh.tvPrice.setText(price);
            vh.tvPrice.setVisibility(View.VISIBLE);
        } else {
            vh.tvPrice.setText(""); // avoid stale recycled value
            vh.tvPrice.setVisibility(View.INVISIBLE); // IMPORTANT: keep layout space reserved
        }

        // --- Details ---
        String details = "";
        if (row.quantity > 0) {
            String q = (row.quantity == Math.rint(row.quantity))
                    ? String.valueOf((long) row.quantity)
                    : String.valueOf(row.quantity);

            if (row.unit != null && !row.unit.trim().isEmpty()) {
                details = q + " " + row.unit.trim();
            } else {
                details = q;
            }
        }

        if (!details.isEmpty()) {
            vh.tvDetails.setText(details);
            vh.tvDetails.setVisibility(View.VISIBLE);
        } else {
            vh.tvDetails.setText(""); // avoid stale recycled value
            vh.tvDetails.setVisibility(View.GONE);
        }

        // --- Swipe reveal persistence (items only) ---
        if (position == openedPos) {
            vh.itemForeground.setTranslationX(-revealWidthPx);
        } else {
            vh.itemForeground.setTranslationX(0f);
        }

        // --- Clicks ---
        vh.itemForeground.setOnClickListener(v -> {
            int pos = vh.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos == openedPos) {
                listener.onItemClicked(row); // close reveal
            } else {
                listener.onToggleCompleted(row);
            }
        });

        vh.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(row));
        vh.btnEdit.setOnClickListener(v -> listener.onEditClicked(row));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    // ---------------- ViewHolders ----------------

    static class ItemVH extends RecyclerView.ViewHolder {

        final TextView tvName;
        final TextView tvDetails;
        final TextView tvPrice;
        final FrameLayout btnEdit;
        final TextView btnDelete;
        final View itemForeground;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            itemForeground = itemView.findViewById(R.id.itemForeground);
        }
    }

    static class StoreHeaderVH extends RecyclerView.ViewHolder {

        final TextView tvStoreChip;
        final TextView tvStoreLocation;

        StoreHeaderVH(@NonNull View itemView) {
            super(itemView);
            tvStoreChip = itemView.findViewById(R.id.tvStoreChip);
            tvStoreLocation = itemView.findViewById(R.id.tvStoreLocation);
        }

        void bind(StoreHeaderRow row) {
            String name = (row.storeName == null || row.storeName.trim().isEmpty())
                    ? "No store"
                    : row.storeName.trim();

            tvStoreChip.setText(name);

            if (row.storeLocation != null && !row.storeLocation.trim().isEmpty()) {
                tvStoreLocation.setText(row.storeLocation.trim());
                tvStoreLocation.setVisibility(View.VISIBLE);
            } else {
                tvStoreLocation.setText("");
                tvStoreLocation.setVisibility(View.GONE);
            }
        }
    }
}