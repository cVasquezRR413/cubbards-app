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
    private LinearLayout frequentContainer;

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
        frequentContainer = findViewById(R.id.frequentContainer);

        btnAddMilk = findViewById(R.id.btnAddMilk);
        btnUseMilk = findViewById(R.id.btnUseMilk);
        btnBuyMilk = findViewById(R.id.btnBuyMilk);
        btnOpenGrocery = findViewById(R.id.btnOpenGrocery);

        refreshPantryText();
        refreshLowUI();
        refreshExpiringUI();
        refreshFrequentUI();

        btnOpenGrocery.setOnClickListener(v -> {
            startActivity(new Intent(this, GroceryListActivity.class));
        });

        btnAddMilk.setOnClickListener(v -> new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            IngredientDao ingredientDao = db.ingredientDao();
            PantryItemDao pantryDao = db.pantryItemDao();

            ensureMilkIngredientExists(ingredientDao);

            // (your test hook)
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
                refreshFrequentUI();
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
                refreshFrequentUI();
            });
        }).start());

        // IMPORTANT: grocery list is now decoupled, so this just inserts a grocery item by name.
        btnBuyMilk.setOnClickListener(v -> new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            GroceryListDao groceryDao = db.groceryListDao();

            String rawName = "Milk";
            String normalized = "milk";

            long result = groceryDao.insert(new GroceryListItem(
                    rawName,
                    normalized,
                    null,
                    System.currentTimeMillis(),
                    0.0,
                    null
            ));

            Log.d("GROCERY", "Milk inserted rowId=" + result);

            List<GroceryRow> grocery = groceryDao.getAll();
            Log.d("GROCERY", "grocery size = " + grocery.size());
            for (GroceryRow row : grocery) {
                Log.d("GROCERY", row.name + " (id=" + row.groceryItemId + ")");
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
                        String rawName = row.ingredientName;
                        String normalized = rawName == null ? "" : rawName.toLowerCase().trim();

                        long result = groceryDao.insert(new GroceryListItem(
                                rawName,
                                normalized,
                                null,
                                System.currentTimeMillis(),
                                0.0,
                                null
                        ));

                        Log.d("GROCERY", "Inserted from LOW: " + rawName + " rowId=" + result);
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
                        String rawName = row.ingredientName;
                        String normalized = rawName == null ? "" : rawName.toLowerCase().trim();

                        long result = groceryDao.insert(new GroceryListItem(
                                rawName,
                                normalized,
                                null,
                                System.currentTimeMillis(),
                                0.0,
                                null
                        ));

                        Log.d("GROCERY", "Inserted from EXPIRING: " + rawName + " rowId=" + result);
                    }).start());

                    expiringContainer.addView(b);
                }
            });
        }).start();
    }

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
                        String rawName = row.ingredientName;
                        String normalized = rawName == null ? "" : rawName.toLowerCase().trim();

                        long result = groceryDao.insert(new GroceryListItem(
                                rawName,
                                normalized,
                                null,
                                System.currentTimeMillis(),
                                0.0,
                                null
                        ));

                        Log.d("GROCERY", "Inserted from FREQUENT: " + rawName + " rowId=" + result);
                    }).start());

                    frequentContainer.addView(b);
                }
            });
        }).start();
    }

    private void ensureMilkIngredientExists(IngredientDao ingredientDao) {
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