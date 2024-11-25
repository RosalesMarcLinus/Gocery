package com.example.gocery;

import java.util.List;

public class Store {
    private String storeName;
    private String storeLocation;
    private String storePhone;
    private String ownerId; // Owner's userId
    private List<Product> products; // List of products available in the store

    // Constructor
    public Store(String storeName, String storeLocation, String storePhone, String ownerId) {
        this.storeName = storeName;
        this.storeLocation = storeLocation;
        this.storePhone = storePhone;
        this.ownerId = ownerId;
    }

    // Getters and setters
    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }

    public String getStorePhone() {
        return storePhone;
    }

    public void setStorePhone(String storePhone) {
        this.storePhone = storePhone;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
