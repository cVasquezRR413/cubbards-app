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
import com.cvr.cubbards.data.Store;
import com.cvr.cubbards.data.StoreDao;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AddItemBottomSheet extends BottomSheetDialogFragment {

    private static final String STORE_NONE = "(none)";
    private static final String STORE_NEW = "New store…";

    private static final String ARG_IS_EDIT = "is_edit";
    private static final String ARG_GROCERY_ITEM_ID = "grocery_item_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_QUANTITY = "quantity";
    private static final String ARG_UNIT = "unit";
    private static final String ARG_STORE_ID = "store_id";

    private List<Store> stores = new ArrayList<>();
    private ArrayAdapter<String> storeAdapter;

    private boolean isEdit = false;
    private long editGroceryItemId = -1L;
    private String editName = null;
    private double editQty = 0.0;
    private String editUnit = null;
    private Long editStoreId = null;

    public static AddItemBottomSheet newEditInstance(GroceryRow row) {
        AddItemBottomSheet sheet = new AddItemBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_EDIT, true);
        b.putLong(ARG_GROCERY_ITEM_ID, row.groceryItemId);
        b.putString(ARG_NAME, row.name);
        b.putDouble(ARG_QUANTITY, row.quantity);
        b.putString(ARG_UNIT, row.unit);
        if (row.storeId != null) b.putLong(ARG_STORE_ID, row.storeId);
        sheet.setArguments(b);
        return sheet;
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
        }

        EditText etName = view.findViewById(R.id.etName);
        EditText etQuantity = view.findViewById(R.id.etQuantity);
        Spinner spUnit = view.findViewById(R.id.spUnit);
        Spinner spStore = view.findViewById(R.id.spStore);
        EditText etStoreName = view.findViewById(R.id.etStoreName);
        EditText etStoreLocation = view.findViewById(R.id.etStoreLocation);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        if (isEdit) {
            if (editName != null) etName.setText(editName);
            if (editQty > 0) etQuantity.setText(String.valueOf(editQty));
        }

        ArrayAdapter<CharSequence> unitAdapter =
                ArrayAdapter.createFromResource(
                        requireContext(),
                        R.array.unit_options,
                        android.R.layout.simple_spinner_item
                );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        if (isEdit && editUnit != null) {
            for (int i = 0; i < unitAdapter.getCount(); i++) {
                if (editUnit.equalsIgnoreCase(unitAdapter.getItem(i).toString())) {
                    spUnit.setSelection(i);
                    break;
                }
            }
        }

        storeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStore.setAdapter(storeAdapter);

        spStore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                boolean isNew = (position == 1);
                etStoreName.setVisibility(isNew ? View.VISIBLE : View.GONE);
                etStoreLocation.setVisibility(isNew ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

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

                if (isEdit && editStoreId != null) {
                    for (int i = 0; i < stores.size(); i++) {
                        if (stores.get(i).getStoreId() == editStoreId) {
                            spStore.setSelection(i + 2);
                            break;
                        }
                    }
                }
            });
        }).start();

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {

            String rawName = etName.getText() == null ? "" :
                    etName.getText().toString().trim();
            if (TextUtils.isEmpty(rawName)) return;

            String normalized = rawName.toLowerCase().trim();

            String qtyRaw = etQuantity.getText() == null ? "" :
                    etQuantity.getText().toString().trim();
            double qty = TextUtils.isEmpty(qtyRaw) ? 0.0 : Double.parseDouble(qtyRaw);

            String unit = null;
            if (spUnit.getSelectedItem() != null) {
                String s = spUnit.getSelectedItem().toString().trim();
                if (!"(none)".equalsIgnoreCase(s)) unit = s;
            }

            int storePos = spStore.getSelectedItemPosition();
            final boolean wantsNewStore = (storePos == 1);

            final Long selectedStoreId =
                    (storePos <= 0 || wantsNewStore) ? null :
                            stores.get(storePos - 2).getStoreId();

            // ✅ make everything effectively final
            final String finalName = rawName;
            final String finalNormalized = normalized;
            final double finalQty = qty;
            final String finalUnit = unit;

            new Thread(() -> {
                AppDatabase db = DatabaseProvider.getDatabase(requireContext());
                GroceryListDao groceryDao = db.groceryListDao();
                StoreDao storeDao = db.storeDao();

                Long storeIdToUse = selectedStoreId;

                if (wantsNewStore) {
                    String newName = etStoreName.getText().toString().trim();
                    String newLoc = etStoreLocation.getText().toString().trim();
                    Store s = new Store(newName,
                            TextUtils.isEmpty(newLoc) ? null : newLoc,
                            System.currentTimeMillis());
                    storeIdToUse = storeDao.insert(s);
                }

                if (isEdit) {
                    groceryDao.updateItem(
                            editGroceryItemId,
                            finalName,
                            finalNormalized,
                            finalQty,
                            finalUnit,
                            storeIdToUse
                    );
                } else {
                    groceryDao.insert(new GroceryListItem(
                            finalName,
                            finalNormalized,
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
}