package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stores")
public class Store {

    @PrimaryKey(autoGenerate = true)
    private long storeId;

    private String name;

    private Long createdAt;

    public Store(String name, Long createdAt) {
        this.name = name;
        this.createdAt = createdAt;
    }

    public long getStoreId() {
        return storeId;
    }

    public void setStoreId(long storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public Long getCreatedAt() {
        return createdAt;
    }
}
