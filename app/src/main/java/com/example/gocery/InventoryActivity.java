package com.example.gocery;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryActivity extends AppCompatActivity {

    private Spinner storeSpinner;
    private LinearLayout inventoryLayout;
    private FirebaseFirestore db;

    private Map<String, String> storeNameToIdMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory); // Link the XML layout here

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        storeSpinner = findViewById(R.id.storeSpinner);
        inventoryLayout = findViewById(R.id.inventoryLayout);

        // Initialize store map
        storeNameToIdMap = new HashMap<>();

        // Fetch stores and populate the spinner
        fetchOwnedStores();

        storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedStoreName = (String) storeSpinner.getSelectedItem();
                if (selectedStoreName != null && storeNameToIdMap.containsKey(selectedStoreName)) {
                    fetchInventoryForStore(storeNameToIdMap.get(selectedStoreName));
                } else {
                    Toast.makeText(InventoryActivity.this, "Please select a valid store.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Optionally handle the case when no item is selected
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

    // Fetch inventory for selected store
    private void fetchInventoryForStore(String storeId) {
        db.collection("stores").document(storeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Access the 'products' array from the store document
                        List<Map<String, Object>> productList = (List<Map<String, Object>>) documentSnapshot.get("products");

                        if (productList != null && !productList.isEmpty()) {
                            inventoryLayout.removeAllViews(); // Clear previous inventory
                            for (Map<String, Object> product : productList) {
                                String productId = (String) product.get("productId");
                                Long inventoryCount = (Long) product.get("inventoryCount"); // Fetch inventory count

                                // Fetch product details using the productId
                                db.collection("products").document(productId).get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                String productName = productDoc.getString("productName");
                                                String price = productDoc.getString("price");

                                                // Display inventory details
                                                addInventoryItem(productName, price, String.valueOf(inventoryCount));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Failed to fetch product details.", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            inventoryLayout.removeAllViews(); // Clear inventory if no products
                            Toast.makeText(this, "No inventory found for this store.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch inventory.", Toast.LENGTH_SHORT).show();
                });
    }


    // Dynamically add an inventory item to the layout
    private void addInventoryItem(String productName, String price, String stock) {
        TextView itemView = new TextView(this);
        itemView.setText(
                "Name: " + productName + "\n" +
                        "Price: " + price + "\n" +
                        "Inventory Count: " + stock
        );
        itemView.setPadding(8, 8, 8, 8);
        itemView.setTextSize(16);
        inventoryLayout.addView(itemView);
    }
}
