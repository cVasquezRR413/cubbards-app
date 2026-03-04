package com.cvr.cubbards;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView; // minimal: used to scroll focused fields into view

import com.cvr.cubbards.data.AppDatabase;
import com.cvr.cubbards.data.DatabaseProvider;
import com.cvr.cubbards.data.GroceryListDao;
import com.cvr.cubbards.data.GroceryListItem;
import com.cvr.cubbards.data.GroceryRow;
import com.cvr.cubbards.data.Store;
import com.cvr.cubbards.data.StoreDao;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet used to add or edit a grocery list item.
 *
 * Responsibilities:
 * - Validate user input (name, quantity/unit, optional price, buyQuantity)
 * - Resolve store selection (existing store vs create new store)
 * - Persist changes via Room DAOs off the main thread
 *
 * This UI component does not refresh the list directly; it requests a refresh
 * from the owning Activity after persistence completes.
 */
public class AddItemBottomSheet extends BottomSheetDialogFragment {

    // Store spinner sentinel options.
    private static final String STORE_NONE = "(none)";
    private static final String STORE_NEW = "New store…";

    // Basic input constraint.
    private static final int MAX_NAME_LENGTH = 60;

    // Buy-quantity ("AMT") constraints.
    private static final int AMT_MIN = 1;
    private static final int AMT_MAX = 999;

    // Argument keys for edit-mode prefill.
    private static final String ARG_IS_EDIT = "is_edit";
    private static final String ARG_GROCERY_ITEM_ID = "grocery_item_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_QUANTITY = "quantity";
    private static final String ARG_UNIT = "unit";
    private static final String ARG_STORE_ID = "store_id";
    private static final String ARG_PRICE_CENTS = "price_cents";
    private static final String ARG_BUY_QTY = "buy_qty";

    // Store spinner backing data (DB stores + sentinel options).
    private List<Store> stores = new ArrayList<>();
    private ArrayAdapter<String> storeAdapter;

    // Edit-mode fields prefilled from arguments.
    private boolean isEdit = false;
    private long editGroceryItemId = -1L;
    private String editName = null;
    private double editQty = 0.0;
    private String editUnit = null;
    private Long editStoreId = null;
    @Nullable private Integer editPriceCents = null;
    private int editBuyQty = AMT_MIN;

    /**
     * Constructs an edit-mode instance with fields pre-populated from the selected row.
     * This keeps the sheet stateless across configuration changes by relying on arguments.
     */
    public static AddItemBottomSheet newEditInstance(GroceryRow row) {
        AddItemBottomSheet sheet = new AddItemBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_EDIT, true);
        b.putLong(ARG_GROCERY_ITEM_ID, row.groceryItemId);
        b.putString(ARG_NAME, row.name);
        b.putDouble(ARG_QUANTITY, row.quantity);
        b.putString(ARG_UNIT, row.unit);
        if (row.storeId != null) b.putLong(ARG_STORE_ID, row.storeId);

        // Preserve optional fields so edits don't clear them unintentionally.
        if (row.priceCents != null) b.putInt(ARG_PRICE_CENTS, row.priceCents);

        // Pass buyQuantity so the AMT field can be prefilled in edit mode.
        b.putInt(ARG_BUY_QTY, row.buyQuantity);

        sheet.setArguments(b);
        return sheet;
    }

    /**
     * Parses a user-entered price string into integer cents.
     * Returns null when the field is empty or invalid.
     *
     * Accepted formats: "12", "12.", "12.3", "12.34" (0–9999.99).
     */
    @Nullable
    private Integer parsePriceToCents(@Nullable String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Enforce: 0–9999.99
        if (!s.matches("^\\d{1,4}(\\.\\d{0,2})?$")) return null;

        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);

        String[] parts = s.split("\\.");
        int dollars;
        try {
            dollars = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (dollars < 0 || dollars > 9999) return null;

        int cents = 0;
        if (parts.length == 2) {
            String frac = parts[1];
            if (frac.length() == 1) frac = frac + "0";

            if (frac.length() == 2) {
                try {
                    cents = Integer.parseInt(frac);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (cents < 0 || cents > 99) return null;
            }
        }

        return dollars * 100 + cents;
    }

    /** Shared clamp helper for bounded integer fields (e.g., AMT). */
    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    /**
     * Controls visibility of the "new store" form fields based on store spinner selection.
     * Centralizing this avoids duplicated position checks scattered across the class.
     */
    private void syncNewStoreVisibility(Spinner spStore, View newStoreFields) {
        int pos = spStore.getSelectedItemPosition();
        boolean isNew = (pos == 1);
        newStoreFields.setVisibility(isNew ? View.VISIBLE : View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_add_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Read arguments once and cache values for prefill logic.
        Bundle args = getArguments();
        isEdit = args != null && args.getBoolean(ARG_IS_EDIT, false);

        if (isEdit && args != null) {
            editGroceryItemId = args.getLong(ARG_GROCERY_ITEM_ID, -1L);
            editName = args.getString(ARG_NAME);
            editQty = args.getDouble(ARG_QUANTITY, 0.0);
            editUnit = args.getString(ARG_UNIT);
            if (args.containsKey(ARG_STORE_ID)) {
                editStoreId = args.getLong(ARG_STORE_ID);
            }
            if (args.containsKey(ARG_PRICE_CENTS)) {
                editPriceCents = args.getInt(ARG_PRICE_CENTS);
            }
            if (args.containsKey(ARG_BUY_QTY)) {
                editBuyQty = args.getInt(ARG_BUY_QTY, AMT_MIN);
            }
        }

        EditText etName = view.findViewById(R.id.etName);
        EditText etQuantity = view.findViewById(R.id.etQuantity);
        EditText etPrice = view.findViewById(R.id.etPrice);
        Spinner spUnit = view.findViewById(R.id.spUnit);
        Spinner spStore = view.findViewById(R.id.spStore);

        EditText etStoreName = view.findViewById(R.id.etStoreName);
        EditText etStoreLocation = view.findViewById(R.id.etStoreLocation);
        View newStoreFields = view.findViewById(R.id.newStoreFields);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // When the keyboard opens, keep lower input fields visible by scrolling them into view.
        NestedScrollView scroll = (NestedScrollView) view;
        View.OnFocusChangeListener scrollOnFocus = (v, hasFocus) -> {
            if (!hasFocus) return;
            v.post(() -> scroll.smoothScrollTo(0, v.getBottom()));
        };
        etStoreName.setOnFocusChangeListener(scrollOnFocus);
        etStoreLocation.setOnFocusChangeListener(scrollOnFocus);

        // AMT UI: direct entry with clamping, plus/minus controls.
        TextView btnAmtPlus = view.findViewById(R.id.btnAmtPlus);
        TextView btnAmtMinus = view.findViewById(R.id.btnAmtMinus);
        EditText etAmtValue = view.findViewById(R.id.tvAmtValue);

        // Normalizes AMT field to a valid integer within [AMT_MIN, AMT_MAX].
        Runnable clampAmt = () -> {
            String s = etAmtValue.getText() == null ? "" : etAmtValue.getText().toString().trim();
            if (s.isEmpty()) {
                etAmtValue.setText(String.valueOf(AMT_MIN));
                return;
            }

            try {
                int v = Integer.parseInt(s);
                v = clampInt(v, AMT_MIN, AMT_MAX);
                etAmtValue.setText(String.valueOf(v));
            } catch (NumberFormatException e) {
                etAmtValue.setText(String.valueOf(AMT_MIN));
            }
        };

        // Prefill AMT when editing.
        if (isEdit) {
            int prefill = clampInt(editBuyQty, AMT_MIN, AMT_MAX);
            etAmtValue.setText(String.valueOf(prefill));
        }

        // Ensure initial AMT value is valid before user interactions.
        clampAmt.run();

        btnAmtPlus.setOnClickListener(v -> {
            clampAmt.run();
            int current = Integer.parseInt(etAmtValue.getText().toString());
            if (current < AMT_MAX) {
                etAmtValue.setText(String.valueOf(current + 1));
            }
        });

        btnAmtMinus.setOnClickListener(v -> {
            clampAmt.run();
            int current = Integer.parseInt(etAmtValue.getText().toString());
            if (current > AMT_MIN) {
                etAmtValue.setText(String.valueOf(current - 1));
            }
        });

        // Clamp when the user leaves the field to avoid persisting invalid values.
        etAmtValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) clampAmt.run();
        });

        if (isEdit) {
            if (editName != null) etName.setText(editName);
            if (editQty > 0) etQuantity.setText(String.valueOf(editQty));

            // Prefill price so edits don't clear an existing value.
            if (editPriceCents != null) {
                int abs = Math.abs(editPriceCents);
                int dollars = abs / 100;
                int cents = abs % 100;
                String priceText = dollars + "." + (cents < 10 ? "0" + cents : String.valueOf(cents));
                etPrice.setText(priceText);
            }
        }

        // Unit spinner is sourced from a fixed resource list.
        ArrayAdapter<CharSequence> unitAdapter =
                ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.unit_options,
                        android.R.layout.simple_spinner_item
                );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        // Restore unit selection when editing.
        if (isEdit && editUnit != null) {
            for (int i = 0; i < unitAdapter.getCount(); i++) {
                if (editUnit.equalsIgnoreCase(unitAdapter.getItem(i).toString())) {
                    spUnit.setSelection(i);
                    break;
                }
            }
        }

        // Store spinner labels are built from DB stores plus sentinel options at the top.
        storeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStore.setAdapter(storeAdapter);

        // Keep "new store" fields in sync as the user changes store selection.
        spStore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                boolean isNew = (position == 1);
                newStoreFields.setVisibility(isNew ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load stores asynchronously to avoid blocking the UI thread.
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(requireContext());
            StoreDao storeDao = db.storeDao();
            List<Store> dbStores = storeDao.getAll();

            requireActivity().runOnUiThread(() -> {
                stores = dbStores == null ? new ArrayList<>() : dbStores;

                // Spinner order matters: positions 0 and 1 are reserved sentinel options.
                List<String> labels = new ArrayList<>();
                labels.add(STORE_NONE);
                labels.add(STORE_NEW);

                for (Store s : stores) {
                    String label = s.getName();
                    if (s.getLocation() != null && !s.getLocation().trim().isEmpty()) {
                        label += " — " + s.getLocation().trim();
                    }
                    labels.add(label);
                }

                storeAdapter.clear();
                storeAdapter.addAll(labels);
                storeAdapter.notifyDataSetChanged();

                // Restore store selection when editing (offset by sentinel options).
                if (isEdit && editStoreId != null) {
                    for (int i = 0; i < stores.size(); i++) {
                        if (stores.get(i).getStoreId() == editStoreId) {
                            spStore.setSelection(i + 2);
                            break;
                        }
                    }
                }

                // Ensure "new store" fields reflect the final selection after async load.
                syncNewStoreVisibility(spStore, newStoreFields);
            });
        }).start();

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {

            String rawName = etName.getText() == null ? "" :
                    etName.getText().toString().trim();
            if (TextUtils.isEmpty(rawName)) return;

            if (rawName.length() > MAX_NAME_LENGTH) {
                etName.setError("Name must be " + MAX_NAME_LENGTH + " characters or fewer");
                return;
            }

            // Normalized name supports consistent lookup/sorting while preserving display name.
            String normalized = rawName.toLowerCase().trim();

            String qtyRaw = etQuantity.getText() == null ? "" :
                    etQuantity.getText().toString().trim();

            double qty = 0.0;
            if (!TextUtils.isEmpty(qtyRaw)) {
                try {
                    qty = Double.parseDouble(qtyRaw);
                } catch (NumberFormatException e) {
                    etQuantity.setError("Enter a valid number");
                    return;
                }
            }

            // Price is optional. If present, it must parse to cents within the allowed range.
            String priceRaw = etPrice.getText() == null ? "" : etPrice.getText().toString();
            Integer priceCents = parsePriceToCents(priceRaw);
            if (priceRaw != null && !priceRaw.trim().isEmpty() && priceCents == null) {
                etPrice.setError("Enter a valid price (0–9999.99)");
                return;
            }

            String unit = null;
            if (spUnit.getSelectedItem() != null) {
                String s = spUnit.getSelectedItem().toString().trim();
                if (!"(none)".equalsIgnoreCase(s)) unit = s;
            }

            int storePos = spStore.getSelectedItemPosition();
            final boolean wantsNewStore = (storePos == 1);

            // Map spinner position back to DB store list (offset by sentinel options).
            final Long selectedStoreId =
                    (storePos <= 0 || wantsNewStore) ? null :
                            stores.get(storePos - 2).getStoreId();

            // buyQuantity comes from AMT field and is clamped before persistence.
            clampAmt.run();
            int buyQty;
            try {
                buyQty = Integer.parseInt(etAmtValue.getText().toString().trim());
            } catch (NumberFormatException e) {
                buyQty = AMT_MIN;
            }
            buyQty = clampInt(buyQty, AMT_MIN, AMT_MAX);

            // Capture values for background thread.
            final String finalName = rawName;
            final String finalNormalized = normalized;
            final double finalQty = qty;
            final String finalUnit = unit;
            final int finalBuyQty = buyQty;
            @Nullable final Integer finalPriceCents = priceCents;

            new Thread(() -> {
                AppDatabase db = DatabaseProvider.getDatabase(requireContext());
                GroceryListDao groceryDao = db.groceryListDao();
                StoreDao storeDao = db.storeDao();

                Long storeIdToUse = selectedStoreId;

                // If user chose "New store…", create it first and use its generated ID.
                if (wantsNewStore) {
                    String newName = etStoreName.getText().toString().trim();
                    String newLoc = etStoreLocation.getText().toString().trim();
                    Store s = new Store(newName,
                            TextUtils.isEmpty(newLoc) ? null : newLoc,
                            System.currentTimeMillis());
                    storeIdToUse = storeDao.insert(s);
                }

                // Update existing item or insert a new one.
                if (isEdit) {
                    groceryDao.updateItem(
                            editGroceryItemId,
                            finalName,
                            finalNormalized,
                            finalQty,
                            finalUnit,
                            finalBuyQty,
                            finalPriceCents,
                            storeIdToUse
                    );
                } else {
                    GroceryListItem item = new GroceryListItem(
                            finalName,
                            finalNormalized,
                            storeIdToUse,
                            System.currentTimeMillis(),
                            finalQty,
                            finalUnit,
                            finalPriceCents
                    );
                    item.buyQuantity = finalBuyQty;
                    groceryDao.insert(item);
                }

                requireActivity().runOnUiThread(() -> {
                    if (requireActivity() instanceof GroceryListActivity) {
                        ((GroceryListActivity) requireActivity()).refreshGroceryUI();
                    }
                    dismiss();
                });
            }).start();
        });
    }
}