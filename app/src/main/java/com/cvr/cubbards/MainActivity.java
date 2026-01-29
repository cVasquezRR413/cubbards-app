package com.cvr.cubbards;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cvr.cubbards.data.AppDatabase;
import com.cvr.cubbards.data.DatabaseProvider;
import com.cvr.cubbards.data.Ingredient;
import com.cvr.cubbards.data.IngredientDao;
import com.cvr.cubbards.data.PantryItem;
import com.cvr.cubbards.data.PantryItemDao;
import com.cvr.cubbards.data.PantryRow;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvPantry;
    private Button btnAddMilk;
    private Button btnUseMilk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvPantry = findViewById(R.id.tvPantry);
        btnAddMilk = findViewById(R.id.btnAddMilk);
        btnUseMilk = findViewById(R.id.btnUseMilk);

        refreshPantryText();

        btnAddMilk.setOnClickListener(v -> new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            IngredientDao ingredientDao = db.ingredientDao();
            PantryItemDao pantryDao = db.pantryItemDao();

            ensureMilkIngredientExists(ingredientDao);

            long milkIngredientId = findMilkIngredientId(ingredientDao);
            if (milkIngredientId == -1) return;

            // Insert if missing; otherwise increment
            PantryItem milkPantry = new PantryItem(milkIngredientId, 1.0, "gallon");
            try {
                pantryDao.insert(milkPantry);
            } catch (Exception e) {
                PantryItem existing = findPantryItemByIngredientId(pantryDao, milkIngredientId);
                if (existing != null) {
                    existing.quantity = existing.quantity + 1.0;
                    if (existing.unit == null || existing.unit.trim().isEmpty()) {
                        existing.unit = "gallon";
                    }
                    pantryDao.update(existing);
                }
            }

            runOnUiThread(this::refreshPantryText);
        }).start());

        btnUseMilk.setOnClickListener(v -> new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            IngredientDao ingredientDao = db.ingredientDao();
            PantryItemDao pantryDao = db.pantryItemDao();

            long milkIngredientId = findMilkIngredientId(ingredientDao);
            if (milkIngredientId == -1) return;

            PantryItem existing = findPantryItemByIngredientId(pantryDao, milkIngredientId);
            if (existing == null) return;

            existing.quantity = existing.quantity - 1.0;

            if (existing.quantity <= 0.0) {
                pantryDao.delete(existing);
            } else {
                pantryDao.update(existing);
            }

            runOnUiThread(this::refreshPantryText);
        }).start());
    }

    private void refreshPantryText() {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            PantryItemDao pantryDao = db.pantryItemDao();

            List<PantryRow> rows = pantryDao.getPantryRows();

            StringBuilder sb = new StringBuilder();
            sb.append("Pantry:\n\n");

            if (rows.isEmpty()) {
                sb.append("(Empty)");
            } else {
                for (PantryRow row : rows) {
                    sb.append("- ")
                            .append(row.ingredientName)
                            .append(": ")
                            .append(row.quantity)
                            .append(" ")
                            .append(row.unit == null ? "" : row.unit)
                            .append("\n");
                }
            }

            runOnUiThread(() -> tvPantry.setText(sb.toString()));
        }).start();
    }

    private void ensureMilkIngredientExists(IngredientDao ingredientDao) {
        Ingredient milk = new Ingredient("Milk", "milk", System.currentTimeMillis());
        try {
            ingredientDao.insert(milk);
        } catch (Exception ignored) {
            // already exists
        }
    }

    private long findMilkIngredientId(IngredientDao ingredientDao) {
        List<Ingredient> allIngredients = ingredientDao.getAll();
        for (Ingredient ing : allIngredients) {
            if ("milk".equals(ing.getNameNormalized())) {
                return ing.getIngredientId();
            }
        }
        return -1;
    }

    private PantryItem findPantryItemByIngredientId(PantryItemDao pantryDao, long ingredientId) {
        List<PantryItem> allPantry = pantryDao.getAll();
        for (PantryItem pi : allPantry) {
            if (pi.ingredientId == ingredientId) {
                return pi;
            }
        }
        return null;
    }
}

