package model;

import java.util.Base64;

public class Product {
    private int productId;
    private int categoryId;
    private String productName;
    private String description;
    private double price;
    private int stock;
    // changed: store raw image bytes
    private byte[] imageData;
    private String categoryname;

    public Product() {
    }

    // constructor without productId (for insert)
    public Product(int categoryId, String productName, String description, double price, int stock, byte[] imageData) {
        this.categoryId = categoryId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageData = imageData;
    }

    // full constructor with productId (for reads)
    public Product(int productId, int categoryId, String productName, String description, double price, int stock, byte[] imageData) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageData = imageData;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getCategoryname() {
        return categoryname;
    }

    public void setCategoryname(String categoryname) {
        this.categoryname = categoryname;
    }

    public String getImageBase64() {
        if (imageData == null) return "";
        return Base64.getEncoder().encodeToString(imageData);
    }
}
