package com.cvr.cubbards;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cvr.cubbards.data.AppDatabase;
import com.cvr.cubbards.data.DatabaseProvider;
import com.cvr.cubbards.data.GroceryListDao;
import com.cvr.cubbards.data.GroceryListItem;
import com.cvr.cubbards.data.GroceryRow;
import com.cvr.cubbards.data.Ingredient;
import com.cvr.cubbards.data.IngredientDao;
import com.cvr.cubbards.data.PantryItem;
import com.cvr.cubbards.data.PantryItemDao;
import com.cvr.cubbards.data.PantryRow;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private TextView tvPantry;
    private LinearLayout lowContainer;
    private LinearLayout expiringContainer;
    private LinearLayout frequentContainer; // NEW

    private Button btnAddMilk;
    private Button btnUseMilk;
    private Button btnBuyMilk;
    private Button btnOpenGrocery;

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
        lowContainer = findViewById(R.id.lowContainer);
        expiringContainer = findViewById(R.id.expiringContainer);
        frequentContainer = findViewById(R.id.frequentContainer); // NEW

        btnAddMilk = findViewById(R.id.btnAddMilk);
        btnUseMilk = findViewById(R.id.btnUseMilk);
        btnBuyMilk = findViewById(R.id.btnBuyMilk);
        btnOpenGrocery = findViewById(R.id.btnOpenGrocery);

        refreshPantryText();
        refreshLowUI();
        refreshExpiringUI();
        refreshFrequentUI(); // NEW

        btnOpenGrocery.setOnClickListener(v -> {
            startActivity(new Intent(this, GroceryListActivity.class));
        });

        btnAddMilk.setOnClickListener(v -> new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            IngredientDao ingredientDao = db.ingredientDao();
            PantryItemDao pantryDao = db.pantryItemDao();

            ensureMilkIngredientExists(ingredientDao);

            //ADD TEST HOOK HERE
            List<Ingredient> all = ingredientDao.getAll();
            for (Ingredient ing : all) {
                if ("milk".equals(ing.getNameNormalized()) && !ing.isFrequent()) {
                    ing.setFrequent(true);
                    ingredientDao.update(ing);
                    break;
                }
            }

            long milkIngredientId = findMilkIngredientId(ingredientDao);
            if (milkIngredientId == -1) return;

            PantryItem milkPantry = new PantryItem(milkIngredientId, 1.0, "gallon");

            // TEST HOOK: give milk an expiry so "Expiring Soon" isn't empty
            milkPantry.expiresAt = System.currentTimeMillis() + (3L * DAY_MILLIS);

            try {
                pantryDao.insert(milkPantry);
            } catch (Exception e) {
                PantryItem existing = findPantryItemByIngredientId(pantryDao, milkIngredientId);
                if (existing != null) {
                    existing.quantity = existing.quantity + 1.0;

                    if (existing.unit == null || existing.unit.trim().isEmpty()) {
                        existing.unit = "gallon";
                    }

                    if (existing.expiresAt == null) {
                        existing.expiresAt = System.currentTimeMillis() + (3L * DAY_MILLIS);
                    }

                    pantryDao.update(existing);
                }
            }

            runOnUiThread(() -> {
                refreshPantryText();
                refreshLowUI();
                refreshExpiringUI();
                refreshFrequentUI(); // NEW
            });
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

            runOnUiThread(() -> {
                refreshPantryText();
                refreshLowUI();
                refreshExpiringUI();
                refreshFrequentUI(); // NEW
            });
        }).start());

        btnBuyMilk.setOnClickListener(v -> new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            IngredientDao ingredientDao = db.ingredientDao();
            GroceryListDao groceryDao = db.groceryListDao();

            ensureMilkIngredientExists(ingredientDao);

            long milkIngredientId = findMilkIngredientId(ingredientDao);
            if (milkIngredientId == -1) return;

            long result = groceryDao.insert(new GroceryListItem(milkIngredientId, System.currentTimeMillis()));
            if (result == -1L) {
                Log.d("GROCERY", "Milk already in grocery list (ignored duplicate tap)");
            } else {
                Log.d("GROCERY", "Milk added to grocery list (rowId=" + result + ")");
            }

            List<GroceryRow> grocery = groceryDao.getAll();
            Log.d("GROCERY", "grocery size = " + grocery.size());
            for (GroceryRow row : grocery) {
                Log.d("GROCERY", row.ingredientName + " (ingredientId=" + row.ingredientId + ")");
            }
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

    private void refreshLowUI() {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            PantryItemDao pantryDao = db.pantryItemDao();
            GroceryListDao groceryDao = db.groceryListDao();

            List<PantryRow> low = pantryDao.getLowItems();

            runOnUiThread(() -> {
                lowContainer.removeAllViews();

                if (low.isEmpty()) {
                    TextView empty = new TextView(this);
                    empty.setText("(Empty)");
                    lowContainer.addView(empty);
                    return;
                }

                for (PantryRow row : low) {
                    Button b = new Button(this);
                    b.setText("Add to grocery: " + row.ingredientName);

                    b.setOnClickListener(v -> new Thread(() -> {
                        long result = groceryDao.insert(new GroceryListItem(row.ingredientId, System.currentTimeMillis()));
                        if (result == -1L) {
                            Log.d("GROCERY", row.ingredientName + " already in grocery list (tap ignored)");
                        } else {
                            Log.d("GROCERY", row.ingredientName + " added to grocery list (rowId=" + result + ")");
                        }
                    }).start());

                    lowContainer.addView(b);
                }
            });
        }).start();
    }

    private void refreshExpiringUI() {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            PantryItemDao pantryDao = db.pantryItemDao();
            GroceryListDao groceryDao = db.groceryListDao();

            long cutoff = System.currentTimeMillis() + (7L * DAY_MILLIS);
            List<PantryRow> expiring = pantryDao.getExpiringSoon(cutoff);

            runOnUiThread(() -> {
                expiringContainer.removeAllViews();

                if (expiring.isEmpty()) {
                    TextView empty = new TextView(this);
                    empty.setText("(Empty)");
                    expiringContainer.addView(empty);
                    return;
                }

                for (PantryRow row : expiring) {
                    Button b = new Button(this);
                    b.setText("Add to grocery: " + row.ingredientName);

                    b.setOnClickListener(v -> new Thread(() -> {
                        long result = groceryDao.insert(new GroceryListItem(row.ingredientId, System.currentTimeMillis()));
                        if (result == -1L) {
                            Log.d("GROCERY", row.ingredientName + " already in grocery list (tap ignored)");
                        } else {
                            Log.d("GROCERY", row.ingredientName + " added to grocery list (rowId=" + result + ")");
                        }
                    }).start());

                    expiringContainer.addView(b);
                }
            });
        }).start();
    }

    // NEW: Frequently Replaced section
    private void refreshFrequentUI() {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            PantryItemDao pantryDao = db.pantryItemDao();
            GroceryListDao groceryDao = db.groceryListDao();

            List<PantryRow> frequent = pantryDao.getFrequentItems();

            runOnUiThread(() -> {
                frequentContainer.removeAllViews();

                if (frequent.isEmpty()) {
                    TextView empty = new TextView(this);
                    empty.setText("(Empty)");
                    frequentContainer.addView(empty);
                    return;
                }

                for (PantryRow row : frequent) {
                    Button b = new Button(this);
                    b.setText("Add to grocery: " + row.ingredientName);

                    b.setOnClickListener(v -> new Thread(() -> {
                        long result = groceryDao.insert(new GroceryListItem(row.ingredientId, System.currentTimeMillis()));
                        if (result == -1L) {
                            Log.d("GROCERY", row.ingredientName + " already in grocery list (tap ignored)");
                        } else {
                            Log.d("GROCERY", row.ingredientName + " added to grocery list (rowId=" + result + ")");
                        }
                    }).start());

                    frequentContainer.addView(b);
                }
            });
        }).start();
    }

    private void ensureMilkIngredientExists(IngredientDao ingredientDao) {
        // NOTE: after adding isFrequent, this will default to false unless you update it.
        Ingredient milk = new Ingredient("Milk", "milk", System.currentTimeMillis());
        try {
            ingredientDao.insert(milk);
        } catch (Exception ignored) {
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
