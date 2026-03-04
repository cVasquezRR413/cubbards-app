package com.cvr.cubbards;

import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.cvr.cubbards.data.AppDatabase;
import com.cvr.cubbards.data.DatabaseProvider;
import com.cvr.cubbards.data.GroceryListDao;
import com.cvr.cubbards.data.GroceryRow;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroceryListActivity extends AppCompatActivity {

    private static final String TAG = "GROCERY";

    private RecyclerView groceryRecycler;
    private GroceryListAdapter adapter;

    private float deleteBtnWidthPx;

    private int touchSlop;
    private float downX, downY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_grocery_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        deleteBtnWidthPx = dp(110);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        groceryRecycler = findViewById(R.id.groceryRecycler);
        groceryRecycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GroceryListAdapter(new GroceryListAdapter.Listener() {
            @Override
            public void onItemClicked(GroceryRow row) {
                closeOpenedRow();
            }

            @Override
            public void onToggleCompleted(GroceryRow row) {
                toggleCompleted(row);
            }

            @Override
            public void onDeleteClicked(GroceryRow row) {
                deleteRow(row);
            }

            @Override
            public void onEditClicked(GroceryRow row) {
                closeOpenedRow();
                Log.d(TAG, "EDIT clicked: " + row.name
                        + " groceryItemId=" + row.groceryItemId);

                AddItemBottomSheet sheet = AddItemBottomSheet.newEditInstance(row);
                sheet.show(getSupportFragmentManager(), "EditItemBottomSheet");
            }
        });

        adapter.setRevealWidthPx(deleteBtnWidthPx);
        groceryRecycler.setAdapter(adapter);

        if (groceryRecycler.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) groceryRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        attachSwipeRevealDelete();

        FloatingActionButton fab = findViewById(R.id.fabAddItem);
        fab.setOnClickListener(v -> {
            AddItemBottomSheet sheet = new AddItemBottomSheet();
            sheet.show(getSupportFragmentManager(), "AddItemBottomSheet");
        });

        refreshGroceryUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshGroceryUI();
    }

    private void deleteRow(GroceryRow row) {
        new Thread(() -> {
            AppDatabase db2 = DatabaseProvider.getDatabase(GroceryListActivity.this);
            GroceryListDao dao2 = db2.groceryListDao();

            int removed = dao2.deleteById(row.groceryItemId);
            Log.d(TAG, "Delete: " + row.name + " removed=" + removed);

            runOnUiThread(() -> {
                adapter.setOpenedPos(RecyclerView.NO_POSITION);
                refreshGroceryUI();
            });
        }).start();
    }

    private void toggleCompleted(GroceryRow row) {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(GroceryListActivity.this);
            GroceryListDao dao = db.groceryListDao();

            boolean newValue = !row.isCompleted;
            int updated = dao.setCompleted(row.groceryItemId, newValue);

            Log.d(TAG, "Toggle completed: " + row.name
                    + " id=" + row.groceryItemId
                    + " -> " + newValue
                    + " updated=" + updated);

            runOnUiThread(() -> {
                // if anything was open, close it (safe + consistent)
                closeOpenedRow();
                refreshGroceryUI();
            });
        }).start();
    }

    private void attachSwipeRevealDelete() {

        ItemTouchHelper.SimpleCallback swipeCb =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                        int pos = viewHolder.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return 0;

                        if (!(adapter.getUiRow(pos) instanceof GroceryListAdapter.ItemRow)) return 0;

                        return ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                    }

                    @Override
                    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                        super.onSelectedChanged(viewHolder, actionState);

                        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder != null) {
                            View fg = viewHolder.itemView.findViewById(R.id.itemForeground);
                            if (fg != null) {
                                float startTx = fg.getTranslationX();
                                fg.setTag(startTx);
                                Log.d(TAG, "SWIPE START pos=" + viewHolder.getAdapterPosition()
                                        + " startTx=" + startTx);
                            }
                        }
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    // Disable fling-to-dismiss, treat this purely as a drag reveal
                    @Override
                    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
                        return 10f;
                    }

                    @Override
                    public float getSwipeEscapeVelocity(float defaultValue) {
                        return Float.MAX_VALUE;
                    }

                    @Override
                    public float getSwipeVelocityThreshold(float defaultValue) {
                        return Float.MAX_VALUE;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int pos = viewHolder.getAdapterPosition();
                        Log.d(TAG, "onSwiped fired pos=" + pos + " dir=" + direction + " -> FORCING CLOSE");

                        if (pos != RecyclerView.NO_POSITION) {
                            adapter.setOpenedPos(RecyclerView.NO_POSITION);

                            View fg = viewHolder.itemView.findViewById(R.id.itemForeground);
                            if (fg != null) {
                                fg.setTranslationX(0f);
                                fg.setTag(0f);
                            }

                            adapter.notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);

                        int pos = viewHolder.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;

                        View fg = viewHolder.itemView.findViewById(R.id.itemForeground);
                        if (fg == null) return;

                        float tx = fg.getTranslationX();

                        float startTx = 0f;
                        Object tag = fg.getTag();
                        if (tag instanceof Float) startTx = (Float) tag;

                        boolean startedOpen = (startTx < 0f);

                        Log.d(TAG, "clearView pos=" + pos
                                + " startTx=" + startTx
                                + " tx=" + tx
                                + " startedOpen=" + startedOpen);

                        if (startedOpen) {
                            if (tx >= -deleteBtnWidthPx * 0.5f) {
                                Log.d(TAG, "DECISION: CLOSE (startedOpen)");
                                adapter.setOpenedPos(RecyclerView.NO_POSITION);
                                fg.setTranslationX(0f);
                                fg.setTag(0f);
                            } else {
                                Log.d(TAG, "DECISION: KEEP OPEN (startedOpen)");
                                adapter.setOpenedPos(pos);
                                fg.setTranslationX(-deleteBtnWidthPx);
                                fg.setTag(-deleteBtnWidthPx);
                            }
                        } else {
                            if (tx <= -deleteBtnWidthPx * 0.35f) {
                                Log.d(TAG, "DECISION: OPEN (startedClosed)");
                                adapter.setOpenedPos(pos);
                                fg.setTranslationX(-deleteBtnWidthPx);
                                fg.setTag(-deleteBtnWidthPx);
                            } else {
                                Log.d(TAG, "DECISION: CLOSE (startedClosed)");
                                adapter.setOpenedPos(RecyclerView.NO_POSITION);
                                fg.setTranslationX(0f);
                                fg.setTag(0f);
                            }
                        }
                    }

                    @Override
                    public void onChildDraw(Canvas c,
                                            RecyclerView recyclerView,
                                            RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY,
                                            int actionState,
                                            boolean isCurrentlyActive) {

                        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                            return;
                        }

                        int pos = viewHolder.getAdapterPosition();
                        View fg = viewHolder.itemView.findViewById(R.id.itemForeground);
                        if (fg == null || pos == RecyclerView.NO_POSITION) return;

                        if (!isCurrentlyActive) return;

                        float startTx = 0f;
                        Object tag = fg.getTag();
                        if (tag instanceof Float) startTx = (Float) tag;

                        float newTx = startTx + dX;

                        newTx = Math.max(newTx, -deleteBtnWidthPx);
                        newTx = Math.min(newTx, 0f);

                        fg.setTranslationX(newTx);
                    }
                };

        new ItemTouchHelper(swipeCb).attachToRecyclerView(groceryRecycler);
    }

    private void closeOpenedRow() {
        int pos = adapter.getOpenedPos();
        if (pos == RecyclerView.NO_POSITION) return;

        RecyclerView.ViewHolder vh = groceryRecycler.findViewHolderForAdapterPosition(pos);
        if (vh != null) {
            View fg = vh.itemView.findViewById(R.id.itemForeground);
            if (fg != null) {
                fg.animate().translationX(0f).setDuration(120).start();
                fg.setTag(0f);
            }
        }
        adapter.setOpenedPos(RecyclerView.NO_POSITION);
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void refreshGroceryUI() {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            GroceryListDao groceryDao = db.groceryListDao();

            List<GroceryRow> rows = groceryDao.getAll();
            List<GroceryListAdapter.UiRow> uiRows = new ArrayList<>();

            HashMap<String, Integer> nameCounts = new HashMap<>();
            for (GroceryRow r : rows) {
                if (r.storeName != null && !r.storeName.trim().isEmpty()) {
                    String nameKey = r.storeName.trim();
                    nameCounts.put(nameKey, nameCounts.getOrDefault(nameKey, 0) + 1);
                }
            }

            String lastStoreKey = null;

            for (GroceryRow row : rows) {

                String storeKey;
                if (row.storeName == null) {
                    storeKey = "__NO_STORE__";
                } else {
                    storeKey = row.storeName + "|" +
                            (row.storeLocation == null ? "" : row.storeLocation);
                }

                if (lastStoreKey == null || !storeKey.equals(lastStoreKey)) {

                    String headerLocation = null;
                    if (row.storeName != null && !row.storeName.trim().isEmpty()) {

                        // Only show location in header when multiple stores share the same name
                        int count = nameCounts.getOrDefault(row.storeName.trim(), 0);
                        if (count > 1 && row.storeLocation != null && !row.storeLocation.trim().isEmpty()) {
                            headerLocation = row.storeLocation.trim();
                        }
                    }

                    uiRows.add(new GroceryListAdapter.StoreHeaderRow(row.storeName, headerLocation));
                    lastStoreKey = storeKey;
                }

                uiRows.add(new GroceryListAdapter.ItemRow(row));
            }

            runOnUiThread(() -> adapter.setRows(uiRows));
        }).start();
    }
}