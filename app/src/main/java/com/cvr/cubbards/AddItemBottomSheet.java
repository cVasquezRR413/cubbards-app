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

    // Keep the real Store objects aligned with the spinner entries
    // Index meanings:
    // 0 -> (none)
    // 1 -> New store…
    // 2+ -> stores.get(i-2)
    private List<Store> stores = new ArrayList<>();
    private ArrayAdapter<String> storeAdapter;

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

        EditText etName = view.findViewById(R.id.etName);
        EditText etQuantity = view.findViewById(R.id.etQuantity);

        Spinner spUnit = view.findViewById(R.id.spUnit);
        Spinner spStore = view.findViewById(R.id.spStore);

        EditText etStoreName = view.findViewById(R.id.etStoreName);
        EditText etStoreLocation = view.findViewById(R.id.etStoreLocation);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // Units spinner
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.unit_options,
                android.R.layout.simple_spinner_item
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);
        spUnit.setSelection(0);

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
                spStore.setSelection(0);
            });
        }).start();

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String rawName = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (TextUtils.isEmpty(rawName)) return;

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

            String normalized = rawName.toLowerCase().trim();
            final double finalQty = qty;
            final String finalUnit = unit;

            new Thread(() -> {
                AppDatabase db = DatabaseProvider.getDatabase(requireContext());
                IngredientDao ingredientDao = db.ingredientDao();
                GroceryListDao groceryDao = db.groceryListDao();
                StoreDao storeDao = db.storeDao();

                // ingredient
                long ingredientId = findIngredientIdByNormalized(ingredientDao, normalized);
                if (ingredientId == -1L) {
                    Ingredient ing = new Ingredient(rawName, normalized, System.currentTimeMillis());
                    ingredientId = ingredientDao.insert(ing);
                }

                // store (optional)
                Long storeIdToUse = selectedStoreId;
                if (wantsNewStore) {
                    // avoid duplicates by name+location
                    Store existing = storeDao.getByNameAndLocation(newStoreName, newStoreLocation);
                    if (existing != null) {
                        storeIdToUse = existing.getStoreId();
                    } else {
                        Store s = new Store(newStoreName, newStoreLocation, System.currentTimeMillis());
                        long newId = storeDao.insert(s);
                        storeIdToUse = newId;
                    }
                }

                groceryDao.insert(new GroceryListItem(
                        ingredientId,
                        storeIdToUse,
                        System.currentTimeMillis(),
                        finalQty,
                        finalUnit
                ));

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
