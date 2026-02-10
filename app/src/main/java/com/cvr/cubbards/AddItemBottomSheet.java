package com.cvr.cubbards;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class AddItemBottomSheet extends BottomSheetDialogFragment {

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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.unit_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(adapter);
        spUnit.setSelection(0);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

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

            String unit = null;
            Object selected = spUnit.getSelectedItem();
            if (selected != null) {
                String s = selected.toString().trim();
                if (!TextUtils.isEmpty(s) && !"(none)".equalsIgnoreCase(s)) unit = s;
            }

            String normalized = rawName.toLowerCase().trim();
            final double finalQty = qty;
            final String finalUnit = unit;

            new Thread(() -> {
                AppDatabase db = DatabaseProvider.getDatabase(requireContext());
                IngredientDao ingredientDao = db.ingredientDao();
                GroceryListDao groceryDao = db.groceryListDao();

                long ingredientId = findIngredientIdByNormalized(ingredientDao, normalized);

                if (ingredientId == -1L) {
                    Ingredient ing = new Ingredient(rawName, normalized, System.currentTimeMillis());
                    ingredientId = ingredientDao.insert(ing);
                }

                groceryDao.insert(new GroceryListItem(
                        ingredientId,
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
