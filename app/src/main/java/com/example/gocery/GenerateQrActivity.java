package com.example.gocery;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateQrActivity extends AppCompatActivity {

    private Spinner storeSpinner, productSpinner;
    private TextView productInfoText;
    private Button btnShowProductInfo;
    private ImageView qrCodeImageView;

    private FirebaseFirestore db;
    private Map<String, String> storeNameToIdMap;
    private Map<String, String> productNameToIdMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        storeSpinner = findViewById(R.id.storeSpinner);
        productSpinner = findViewById(R.id.productSpinner);
        productInfoText = findViewById(R.id.productInfoText);
        btnShowProductInfo = findViewById(R.id.btnShowProductInfo);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);  // Initialize ImageView for QR code

        // Initialize maps
        storeNameToIdMap = new HashMap<>();
        productNameToIdMap = new HashMap<>();

        // Fetch stores from Firestore
        fetchOwnedStores();

        // Show product info button click handler
        btnShowProductInfo.setOnClickListener(v -> {
            String selectedProductName = (String) productSpinner.getSelectedItem();

            if (selectedProductName != null && productNameToIdMap.containsKey(selectedProductName)) {
                // Fetch product info and display QR code
                fetchProductInfo(productNameToIdMap.get(selectedProductName));
            } else {
                Toast.makeText(this, "Please select a valid product.", Toast.LENGTH_SHORT).show();
            }
        });

        // Store spinner selection handler
        storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedStoreName = (String) storeSpinner.getSelectedItem();

                if (selectedStoreName != null && storeNameToIdMap.containsKey(selectedStoreName)) {
                    fetchProductsForStore(storeNameToIdMap.get(selectedStoreName));
                } else {
                    Toast.makeText(GenerateQrActivity.this, "Please select a valid store.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Handle case when nothing is selected
            }
        });
    }

    // Fetch store list from Firestore
    private void fetchOwnedStores() {
        db.collection("stores")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> storeNames = new ArrayList<>();
                    storeNameToIdMap.clear();

                    querySnapshot.forEach(documentSnapshot -> {
                        String storeId = documentSnapshot.getId();
                        String storeName = documentSnapshot.getString("storeName");

                        if (storeName != null) {
                            storeNames.add(storeName);
                            storeNameToIdMap.put(storeName, storeId);
                        }
                    });

                    populateStoreSpinner(storeNames);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch stores.", Toast.LENGTH_SHORT).show();
                });
    }

    // Populate store spinner with fetched store names
    private void populateStoreSpinner(List<String> storeNames) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, storeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storeSpinner.setAdapter(adapter);
    }

    // Fetch products for selected store
    private void fetchProductsForStore(String storeId) {
        db.collection("stores").document(storeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> productList = (List<Map<String, Object>>) documentSnapshot.get("products");

                        if (productList != null && !productList.isEmpty()) {
                            List<String> productNames = new ArrayList<>();
                            productNameToIdMap.clear();

                            for (Map<String, Object> product : productList) {
                                String productId = (String) product.get("productId");

                                db.collection("products").document(productId).get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                String productName = productDoc.getString("productName");
                                                if (productName != null) {
                                                    productNames.add(productName);
                                                    productNameToIdMap.put(productName, productId);

                                                    if (productNames.size() == productList.size()) {
                                                        populateProductSpinner(productNames);
                                                    }
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to fetch product details.", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            populateProductSpinner(new ArrayList<>());
                            Toast.makeText(this, "No products found for this store.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch products.", Toast.LENGTH_SHORT).show();
                });
    }

    // Populate product spinner with product names
    private void populateProductSpinner(List<String> productNames) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productSpinner.setAdapter(adapter);

        if (productNames.isEmpty()) {
            productSpinner.setEnabled(false);
            productInfoText.setText("");
        } else {
            productSpinner.setEnabled(true);
        }
    }

    // Fetch product info and generate QR code
    private void fetchProductInfo(String productId) {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String productName = documentSnapshot.getString("productName");
                        String price = documentSnapshot.getString("price");
                        String weight = documentSnapshot.getString("weight");
                        String category = documentSnapshot.getString("category");

                        productInfoText.setText(
                                "Name: " + productName + "\n" +
                                        "Price: " + price + "\n" +
                                        "Weight: " + weight + "\n" +
                                        "Category: " + category
                        );

                        // Generate QR code URL based on product info
                        //String qrCodeData = "Product: " + productName + "\nPrice: " + price + "\nCategory: " + category;
                        String qrCodeData = productId;

                        generateQrCode(qrCodeData);  // Call the method to generate QR code
                    } else {
                        Toast.makeText(this, "Product not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch product info.", Toast.LENGTH_SHORT).show();
                });
    }

    // Generate QR code using GoQR.me API
    private void generateQrCode(String textToEncode) {
        try {
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=" + textToEncode + "&size=200x200";

            // Use BarcodeEncoder to create Bitmap from the QR code URL
            Glide.with(this)
                    .load(qrCodeUrl)
                    .into(qrCodeImageView);
            // Display QR code in ImageView
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
