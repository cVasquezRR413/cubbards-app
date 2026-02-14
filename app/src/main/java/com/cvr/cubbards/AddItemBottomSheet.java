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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cvr.cubbards.data.AppDatabase;
import com.cvr.cubbards.data.DatabaseProvider;
import com.cvr.cubbards.data.GroceryListDao;
import com.cvr.cubbards.data.GroceryListItem;
import com.cvr.cubbards.data.GroceryRow;
import com.cvr.cubbards.data.Ingredient;
import com.cvr.cubbards.data.IngredientDao;
import com.cvr.cubbards.data.Store;
import com.cvr.cubbards.data.StoreDao;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AddItemBottomSheet extends BottomSheetDialogFragment {

    private static final String STORE_NONE = "(none)";
    private static final String STORE_NEW = "New store…";

    // ---- edit-mode args ----
    private static final String ARG_IS_EDIT = "is_edit";
    private static final String ARG_GROCERY_ITEM_ID = "grocery_item_id";
    private static final String ARG_INGREDIENT_NAME = "ingredient_name";
    private static final String ARG_QUANTITY = "quantity";
    private static final String ARG_UNIT = "unit";
    private static final String ARG_STORE_ID = "store_id";

    // Keep the real Store objects aligned with the spinner entries
    // Index meanings:
    // 0 -> (none)
    // 1 -> New store…
    // 2+ -> stores.get(i-2)
    private List<Store> stores = new ArrayList<>();
    private ArrayAdapter<String> storeAdapter;

    // cached edit info (if applicable)
    private boolean isEdit = false;
    private long editGroceryItemId = -1L;
    private String editIngredientName = null;
    private double editQty = 0.0;
    private String editUnit = null;
    private Long editStoreId = null;

    public static AddItemBottomSheet newEditInstance(GroceryRow row) {
        AddItemBottomSheet sheet = new AddItemBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_EDIT, true);
        b.putLong(ARG_GROCERY_ITEM_ID, row.groceryItemId);
        b.putString(ARG_INGREDIENT_NAME, row.ingredientName);
        b.putDouble(ARG_QUANTITY, row.quantity);
        b.putString(ARG_UNIT, row.unit);
        if (row.storeId != null) {
            b.putLong(ARG_STORE_ID, row.storeId);
        }
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.sheet_add_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---- read args ----
        Bundle args = getArguments();
        isEdit = args != null && args.getBoolean(ARG_IS_EDIT, false);

        if (isEdit && args != null) {
            editGroceryItemId = args.getLong(ARG_GROCERY_ITEM_ID, -1L);
            editIngredientName = args.getString(ARG_INGREDIENT_NAME, null);
            editQty = args.getDouble(ARG_QUANTITY, 0.0);
            editUnit = args.getString(ARG_UNIT, null);
            if (args.containsKey(ARG_STORE_ID)) {
                editStoreId = args.getLong(ARG_STORE_ID);
            } else {
                editStoreId = null;
            }
        }

        EditText etName = view.findViewById(R.id.etName);
        EditText etQuantity = view.findViewById(R.id.etQuantity);

        Spinner spUnit = view.findViewById(R.id.spUnit);
        Spinner spStore = view.findViewById(R.id.spStore);

        EditText etStoreName = view.findViewById(R.id.etStoreName);
        EditText etStoreLocation = view.findViewById(R.id.etStoreLocation);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // ---- Prefill (edit mode) ----
        if (isEdit) {
            if (editIngredientName != null) etName.setText(editIngredientName);

            // name editing is NOT supported by the current DAO update (it updates qty/unit/store only)
            etName.setEnabled(false);
            etName.setFocusable(false);

            if (editQty > 0) {
                // keep it simple; your adapter already formats nicely for display
                etQuantity.setText(String.valueOf(editQty == Math.rint(editQty) ? (long) editQty : editQty));
            } else {
                etQuantity.setText("");
            }
        }

        // Units spinner
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.unit_options,
                android.R.layout.simple_spinner_item
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);
        spUnit.setSelection(0);

        // If edit, set unit selection after adapter is set
        if (isEdit && editUnit != null) {
            int unitIndex = 0;
            for (int i = 0; i < unitAdapter.getCount(); i++) {
                CharSequence v = unitAdapter.getItem(i);
                if (v != null && editUnit.trim().equalsIgnoreCase(v.toString().trim())) {
                    unitIndex = i;
                    break;
                }
            }
            spUnit.setSelection(unitIndex);
        }

        // Store spinner (starts with placeholders; we’ll populate after DB read)
        storeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStore.setAdapter(storeAdapter);

        // When user chooses "New store…", show fields
        spStore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                boolean isNew = (position == 1);
                etStoreName.setVisibility(isNew ? View.VISIBLE : View.GONE);
                etStoreLocation.setVisibility(isNew ? View.VISIBLE : View.GONE);

                if (!isNew) {
                    etStoreName.setText("");
                    etStoreLocation.setText("");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Load stores from DB
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(requireContext());
            StoreDao storeDao = db.storeDao();
            List<Store> dbStores = storeDao.getAll();

            requireActivity().runOnUiThread(() -> {
                stores = dbStores == null ? new ArrayList<>() : dbStores;

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

                // If edit, select the existing store in the spinner (if present)
                if (isEdit) {
                    int sel = 0; // none by default
                    if (editStoreId != null) {
                        for (int i = 0; i < stores.size(); i++) {
                            if (stores.get(i).getStoreId() == editStoreId) {
                                sel = i + 2; // offset for (none) + New store…
                                break;
                            }
                        }
                    }
                    spStore.setSelection(sel);
                } else {
                    spStore.setSelection(0);
                }
            });
        }).start();

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {

            // name is required for add-mode only
            String rawName = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (!isEdit && TextUtils.isEmpty(rawName)) return;

            String qtyRaw = etQuantity.getText() == null ? "" : etQuantity.getText().toString().trim();
            double qty = 0.0;
            if (!TextUtils.isEmpty(qtyRaw)) {
                try {
                    qty = Double.parseDouble(qtyRaw);
                } catch (NumberFormatException e) {
                    return;
                }
            }

            // Unit
            String unit = null;
            Object selectedUnit = spUnit.getSelectedItem();
            if (selectedUnit != null) {
                String s = selectedUnit.toString().trim();
                if (!TextUtils.isEmpty(s) && !"(none)".equalsIgnoreCase(s)) unit = s;
            }

            // Store choice
            int storePos = spStore.getSelectedItemPosition();
            final boolean wantsNewStore = (storePos == 1);

            final String newStoreName = etStoreName.getText() == null ? "" : etStoreName.getText().toString().trim();
            final String newStoreLocationRaw = etStoreLocation.getText() == null ? "" : etStoreLocation.getText().toString().trim();
            final String newStoreLocation = TextUtils.isEmpty(newStoreLocationRaw) ? null : newStoreLocationRaw;

            if (wantsNewStore && TextUtils.isEmpty(newStoreName)) {
                return; // user selected New store but didn't give a name
            }

            // Determine selected existing storeId (if any)
            final Long selectedStoreId;
            if (storePos <= 0) { // (none)
                selectedStoreId = null;
            } else if (wantsNewStore) {
                selectedStoreId = null; // will be created in DB thread
            } else {
                // storePos 2+ corresponds to stores.get(storePos - 2)
                int idx = storePos - 2;
                selectedStoreId = (idx >= 0 && idx < stores.size()) ? stores.get(idx).getStoreId() : null;
            }

            final double finalQty = qty;
            final String finalUnit = unit;

            new Thread(() -> {
                AppDatabase db = DatabaseProvider.getDatabase(requireContext());
                IngredientDao ingredientDao = db.ingredientDao();
                GroceryListDao groceryDao = db.groceryListDao();
                StoreDao storeDao = db.storeDao();

                // store (optional)
                Long storeIdToUse = selectedStoreId;
                if (wantsNewStore) {
                    Store existing = storeDao.getByNameAndLocation(newStoreName, newStoreLocation);
                    if (existing != null) {
                        storeIdToUse = existing.getStoreId();
                    } else {
                        Store s = new Store(newStoreName, newStoreLocation, System.currentTimeMillis());
                        long newId = storeDao.insert(s);
                        storeIdToUse = newId;
                    }
                }

                if (isEdit) {
                    // EDIT MODE: update existing grocery_list_items row
                    if (editGroceryItemId > 0) {
                        groceryDao.updateItem(editGroceryItemId, finalQty, finalUnit, storeIdToUse);
                    }
                } else {
                    // ADD MODE: ingredient
                    String normalized = rawName.toLowerCase().trim();

                    long ingredientId = findIngredientIdByNormalized(ingredientDao, normalized);
                    if (ingredientId == -1L) {
                        Ingredient ing = new Ingredient(rawName, normalized, System.currentTimeMillis());
                        ingredientId = ingredientDao.insert(ing);
                    }

                    groceryDao.insert(new GroceryListItem(
                            ingredientId,
                            storeIdToUse,
                            System.currentTimeMillis(),
                            finalQty,
                            finalUnit
                    ));
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

    private long findIngredientIdByNormalized(IngredientDao ingredientDao, String normalized) {
        List<Ingredient> all = ingredientDao.getAll();
        for (Ingredient ing : all) {
            if (normalized.equals(ing.getNameNormalized())) {
                return ing.getIngredientId();
            }
        }
        return -1L;
    }
}