package com.example.gocery;

public class Product {
    private String productName;
    private String price;
    private String weight;
    private String category;

    public Product(String productName, String price, String weight, String category) {
        this.productName = productName;
        this.price = price;
        this.weight = weight;
        this.category = category;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
