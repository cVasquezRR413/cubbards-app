package com.cvr.cubbards;

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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cvr.cubbards.data.GroceryRow;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the grocery list screen.
 *
 * This adapter renders a "sectioned" list:
 * - Store header rows (store name + optional location)
 * - Grocery item rows (name, quantity/unit details, optional price)
 *
 * Design boundary:
 * The adapter does not modify data; it delegates actions via Listener callbacks.
 *
 * RecyclerView note:
 * Because views are recycled, every bind path must fully reset view state
 * (text, visibility, translation, colors) to avoid stale UI.
 */
public class GroceryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * UI events emitted from the adapter.
     *
     * Contract:
     * - Taps on an opened row close the swipe reveal.
     * - Taps on a closed row toggle completion.
     * - Edit/Delete are explicit button actions.
     */
    public interface Listener {
        void onItemClicked(GroceryRow row);      // close if open
        void onToggleCompleted(GroceryRow row);  // toggle completion if closed
        void onDeleteClicked(GroceryRow row);
        void onEditClicked(GroceryRow row);
    }

    /**
     * UI row model used by the adapter.
     * We keep headers and items in a single list so RecyclerView can render them in order.
     */
    public interface UiRow {}

    /** Header row that represents a store grouping. Fields may be null when not present. */
    public static class StoreHeaderRow implements UiRow {
        public final String storeName;       // nullable
        public final String storeLocation;   // nullable

        public StoreHeaderRow(String storeName, String storeLocation) {
            this.storeName = storeName;
            this.storeLocation = storeLocation;
        }
    }

    /** Wrapper for a grocery item row. */
    public static class ItemRow implements UiRow {
        public final GroceryRow row;

        public ItemRow(GroceryRow row) {
            this.row = row;
        }
    }

    // View types for header vs item rows.
    private static final int VT_HEADER = 0;
    private static final int VT_ITEM = 1;

    private final Listener listener;
    private final List<UiRow> rows = new ArrayList<>();

    /**
     * Swipe-reveal state.
     * openedPos tracks the *single* currently revealed row (adapter position).
     * revealWidthPx is injected by the parent based on measured layout width.
     */
    private int openedPos = RecyclerView.NO_POSITION;
    private float revealWidthPx = 0f;

    public GroceryListAdapter(Listener listener) {
        this.listener = listener;
    }

    /** Parent provides the width to translate the foreground when the row is revealed. */
    public void setRevealWidthPx(float px) {
        this.revealWidthPx = px;
    }

    /**
     * Full refresh is intentional to keep swipe-reveal state consistent
     * and avoid RecyclerView animation edge cases.
     */
    public void setOpenedPos(int pos) {
        this.openedPos = pos;
        notifyDataSetChanged();
    }

    /** Exposed for swipe helpers to read current reveal state. */
    public int getOpenedPos() {
        return openedPos;
    }

    /**
     * Safe lookup used by swipe helpers.
     * Returns null when the adapter position is invalid (data changed mid-gesture).
     */
    public UiRow getUiRow(int adapterPos) {
        if (adapterPos < 0 || adapterPos >= rows.size()) return null;
        return rows.get(adapterPos);
    }

    /**
     * Replaces adapter data.
     * If the previously opened position is now out of range, we clear the reveal state.
     */
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

        /**
         * Name formatting:
         * We inline the buy-quantity prefix as "(N) " so text wrapping behaves naturally.
         * Strike-through is applied to the *name only* (not the prefix) to keep quantity legible.
         */
        int buyQty = row.buyQuantity;
        String prefix = (buyQty > 1) ? "(" + buyQty + ") " : "";
        String name = (row.name == null) ? "" : row.name;
        String full = prefix + name;

        SpannableString ss = new SpannableString(full);

        // Completed styling: strike-through only the name portion.
        if (row.isCompleted) {
            int startName = prefix.length();
            int endName = full.length();
            if (endName > startName) {
                ss.setSpan(new StrikethroughSpan(), startName, endName, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Completed rows collapse to one line to reduce visual noise in "done" items.
            vh.tvName.setSingleLine(true);
            vh.tvName.setEllipsize(TextUtils.TruncateAt.END);
        } else {
            // Active rows can wrap for readability.
            vh.tvName.setSingleLine(false);
            vh.tvName.setEllipsize(null);
            vh.tvName.setMaxLines(3);
        }

        vh.tvName.setText(ss);

        /**
         * Color handling:
         * Always set colors during bind to prevent recycled rows from "leaking" old colors.
         */
        final int primary = ContextCompat.getColor(vh.itemView.getContext(), R.color.row_text_primary);
        final int secondary = ContextCompat.getColor(vh.itemView.getContext(), R.color.row_text_secondary);

        // Details are always secondary (even when active).
        vh.tvDetails.setTextColor(secondary);

        if (row.isCompleted) {
            vh.tvName.setTextColor(secondary);
            vh.tvPrice.setTextColor(secondary);
        } else {
            vh.tvName.setTextColor(primary);
            vh.tvPrice.setTextColor(secondary);
        }

        /**
         * Price:
         * When missing, clear text to avoid recycled stale values.
         * Use INVISIBLE (not GONE) so the row layout doesn't "jump" between items.
         */
        if (row.priceCents != null) {
            int abs = Math.abs(row.priceCents);
            int dollars = abs / 100;
            int cents = abs % 100;

            String price = "$" + dollars + "." +
                    (cents < 10 ? "0" + cents : String.valueOf(cents));

            vh.tvPrice.setText(price);
            vh.tvPrice.setVisibility(View.VISIBLE);
        } else {
            vh.tvPrice.setText("");
            vh.tvPrice.setVisibility(View.INVISIBLE);
        }

        /**
         * Details (quantity + unit):
         * Hide completely when empty (GONE) since details are optional and should not reserve space.
         * Clear text on the empty path to avoid recycled values.
         */
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
            vh.tvDetails.setText("");
            vh.tvDetails.setVisibility(View.GONE);
        }

        /**
         * Swipe reveal persistence:
         * We translate the foreground view so only one row stays "open" at a time.
         * Must reset translation on all other rows due to RecyclerView recycling.
         */
        if (position == openedPos) {
            vh.itemForeground.setTranslationX(-revealWidthPx);
        } else {
            vh.itemForeground.setTranslationX(0f);
        }

        /**
         * Click behavior:
         * - If this row is currently revealed, a tap closes it (delegated to parent).
         * - Otherwise, a tap toggles completion.
         */
        vh.itemForeground.setOnClickListener(v -> {
            int pos = vh.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos == openedPos) {
                listener.onItemClicked(row);
            } else {
                listener.onToggleCompleted(row);
            }
        });

        // Explicit actions.
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

        /**
         * Binds store grouping UI.
         * Shows "No store" when items are unassigned to a store.
         */
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