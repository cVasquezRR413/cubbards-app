package com.cvr.cubbards.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "stores")
public class Store {

    @PrimaryKey(autoGenerate = true)
    private long storeId;

    private String name;

    // NEW: optional location
    private String location;

    private Long createdAt;

    /**
     * Room constructor (single unambiguous choice).
     */
    public Store(String name, String location, Long createdAt) {
        this.name = name;
        this.location = location;
        this.createdAt = createdAt;
    }

    /**
     * Convenience constructor for old call sites.
     */
    @Ignore
    public Store(String name, Long createdAt) {
        this(name, null, createdAt);
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

    // ADD: setter so Room / your code can mutate if needed
    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    // ADD: setter so Room can populate this field
    public void setLocation(String location) {
        this.location = location;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
