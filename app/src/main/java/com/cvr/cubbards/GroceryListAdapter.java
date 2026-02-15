package com.cvr.cubbards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cvr.cubbards.data.GroceryRow;

import java.util.ArrayList;
import java.util.List;

public class GroceryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VT_STORE_HEADER = 1;
    private static final int VT_ITEM = 2;

    public interface Listener {
        void onItemClicked(GroceryRow row);
        void onDeleteClicked(GroceryRow row);
        void onEditClicked(GroceryRow row); // NEW
    }

    // --- UI rows (store headers + items) ---
    public static abstract class UiRow {
        abstract int viewType();
    }

    public static class StoreHeaderRow extends UiRow {
        public final String storeName;      // null => "No store"
        public final String storeLocation;  // can be null/empty

        public StoreHeaderRow(String storeName, String storeLocation) {
            this.storeName = storeName;
            this.storeLocation = storeLocation;
        }

        @Override int viewType() { return VT_STORE_HEADER; }
    }

    public static class ItemRow extends UiRow {
        public final GroceryRow row;
        public ItemRow(GroceryRow row) { this.row = row; }
        @Override int viewType() { return VT_ITEM; }
    }

    private final Listener listener;
    private final List<UiRow> rows = new ArrayList<>();

    // swipe-reveal state (managed by Activity + persisted here for binding)
    private int openedPos = RecyclerView.NO_POSITION;
    private float revealWidthPx = 0f;

    public GroceryListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<UiRow> newRows) {
        rows.clear();
        rows.addAll(newRows);
        openedPos = RecyclerView.NO_POSITION; // safer when dataset changes
        notifyDataSetChanged();
    }

    public UiRow getUiRow(int position) {
        return rows.get(position);
    }

    public int getOpenedPos() {
        return openedPos;
    }

    public void setOpenedPos(int newPos) {
        int old = openedPos;
        openedPos = newPos;

        // redraw both the old and new positions so translation updates
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
        if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos);
    }

    public void setRevealWidthPx(float px) {
        revealWidthPx = px;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).viewType();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());

        if (viewType == VT_STORE_HEADER) {
            View v = inf.inflate(R.layout.row_store_header, parent, false);
            return new StoreHeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.row_grocery_item, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UiRow ui = rows.get(position);

        if (holder instanceof StoreHeaderVH) {
            ((StoreHeaderVH) holder).bind((StoreHeaderRow) ui);
        } else if (holder instanceof ItemVH) {
            ((ItemVH) holder).bind(((ItemRow) ui).row);
        }
    }

    // --- VHs ---

    static class StoreHeaderVH extends RecyclerView.ViewHolder {
        TextView chip, loc;

        StoreHeaderVH(@NonNull View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.tvStoreChip);
            loc = itemView.findViewById(R.id.tvStoreLocation);
        }

        void bind(StoreHeaderRow r) {
            if (r.storeName == null) {
                chip.setText("No store");
                loc.setVisibility(View.GONE);
            } else {
                chip.setText(r.storeName);

                if (r.storeLocation != null && !r.storeLocation.trim().isEmpty()) {
                    loc.setText(r.storeLocation.trim());
                    loc.setVisibility(View.VISIBLE);
                } else {
                    loc.setVisibility(View.GONE);
                }
            }
        }
    }

    class ItemVH extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
        }

        void bind(GroceryRow row) {
            // --- text ---
            tvName.setText(row.name);

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
                tvDetails.setText(details);
                tvDetails.setVisibility(View.VISIBLE);
            } else {
                tvDetails.setVisibility(View.GONE);
            }

            // --- IMPORTANT: persist reveal position on bind ---
            View fg = itemView.findViewById(R.id.itemForeground);
            if (fg != null) {
                int pos = getAdapterPosition(); // works on all versions
                if (pos != RecyclerView.NO_POSITION && pos == openedPos) {
                    fg.setTranslationX(-revealWidthPx);
                } else {
                    fg.setTranslationX(0f);
                }
            }

            // --- clicks ---
            View fgClick = itemView.findViewById(R.id.itemForeground);
            if (fgClick != null) {
                fgClick.setOnClickListener(v -> listener.onItemClicked(row));
            } else {
                itemView.setOnClickListener(v -> listener.onItemClicked(row));
            }

            View del = itemView.findViewById(R.id.btnDelete);
            if (del != null) {
                del.setOnClickListener(v -> listener.onDeleteClicked(row));
            }

            // NEW: edit (pencil) click
            View edit = itemView.findViewById(R.id.btnEdit);
            if (edit != null) {
                edit.setOnClickListener(v -> listener.onEditClicked(row));
            }
        }
    }
}