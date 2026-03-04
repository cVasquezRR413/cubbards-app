package com.cvr.cubbards.data;

public class GroceryRow {

    public long groceryItemId;

    public String name;
    public String nameNormalized;

    public long addedAt;

    public double quantity;
    public String unit;

    // how many the user plans to buy (for "(2) Almond milk")
    public int buyQuantity;

    public Long storeId;
    public String storeName;
    public String storeLocation;

    public Integer priceCents;

    public boolean isCompleted;
}
