package com.example.gocery;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
        getSupportActionBar().hide();  // Hides the action bar

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
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> finish());

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


    private void addInventoryItem(String productName, String price, String stock) {
        // Create a new CardView
        CardView cardView = new CardView(this);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        cardView.setRadius(8f);
        cardView.setCardElevation(4f);
        cardView.setUseCompatPadding(true);
        cardView.setContentPadding(16, 16, 16, 16);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ) {{
            setMargins(8, 8, 8, 8);
        }});

        // Create a container for text views
        LinearLayout itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);

        // Product Name
        TextView nameView = new TextView(this);
        nameView.setText("Product: " + productName);
        nameView.setTextSize(18f);
        nameView.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        nameView.setTypeface(null, Typeface.BOLD);

        // Price
        TextView priceView = new TextView(this);
        priceView.setText("Price: " + price);
        priceView.setTextSize(16f);
        priceView.setTextColor(getResources().getColor(R.color.colorAccent));

        // Stock Count
        TextView stockView = new TextView(this);
        stockView.setText("Stock: " + stock);
        stockView.setTextSize(16f);
        stockView.setTextColor(getResources().getColor(R.color.colorSecondary));

        // Add all views to the container
        itemContainer.addView(nameView);
        itemContainer.addView(priceView);
        itemContainer.addView(stockView);

        // Add the container to the CardView
        cardView.addView(itemContainer);

        // Add the CardView to the inventory layout
        inventoryLayout.addView(cardView);
    }

}
