package com.cvr.cubbards;

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
import com.cvr.cubbards.data.GroceryRow;

import java.util.List;

public class GroceryListActivity extends AppCompatActivity {

    private static final String TAG = "GROCERY";

    private LinearLayout groceryContainer;
    private TextView tvTitle;

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

        tvTitle = findViewById(R.id.tvTitle);
        groceryContainer = findViewById(R.id.groceryContainer);

        refreshGroceryUI();
    }

    private void refreshGroceryUI() {
        new Thread(() -> {
            AppDatabase db = DatabaseProvider.getDatabase(this);
            GroceryListDao groceryDao = db.groceryListDao();

            List<GroceryRow> rows = groceryDao.getAll();

            runOnUiThread(() -> {
                groceryContainer.removeAllViews();

                if (rows.isEmpty()) {
                    TextView empty = new TextView(this);
                    empty.setText("(Empty)");
                    empty.setTextSize(16f);
                    groceryContainer.addView(empty);
                    return;
                }

                for (GroceryRow row : rows) {
                    Button b = new Button(this);
                    b.setAllCaps(false);
                    b.setText(row.ingredientName + "  (tap to remove)");

                    b.setOnClickListener(v -> {
                        // Do DB delete on background thread
                        new Thread(() -> {
                            AppDatabase db2 = DatabaseProvider.getDatabase(this);
                            GroceryListDao dao2 = db2.groceryListDao();

                            int removed = dao2.removeByIngredientId(row.ingredientId);
                            Log.d(TAG, "Removed " + row.ingredientName + " removed=" + removed);

                            // Refresh screen
                            refreshGroceryUI();
                        }).start();
                    });

                    groceryContainer.addView(b);
                }
            });
        }).start();
    }
}
