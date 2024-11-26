package com.example.gocery;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddProduct extends AppCompatActivity {

    private EditText productName, productPrice, productWeight, productInventoryCount;
    private Spinner storeSpinner, productCategorySpinner;
    private Button submitProductButton;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private List<String> ownedStores; // List of store IDs
    private List<String> storeNames; // List of store names
    private List<String> storeIds; // List of store IDs corresponding to the names
    private String selectedStoreId; // Store ID for the selected store

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Initialize Firebase Firestore and FirebaseAuth
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize UI elements
        storeSpinner = findViewById(R.id.storeSpinner);
        productCategorySpinner = findViewById(R.id.productCategorySpinner);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productWeight = findViewById(R.id.productWeight);
        productInventoryCount = findViewById(R.id.productInventoryCount);
        submitProductButton = findViewById(R.id.submitProductButton);

        // Populate categories for the product category spinner
        populateCategorySpinner();

        // Fetch the list of owned stores for the current user
        fetchOwnedStores();

        // Set the submit button click listener
        submitProductButton.setOnClickListener(v -> addProductToStore());
    }

    private void populateCategorySpinner() {
        // Define categories
        List<String> categories = new ArrayList<>();
        categories.add("Fruits");
        categories.add("Vegetables");
        categories.add("Clothing");
        categories.add("Electronics");
        categories.add("Medicine");
        categories.add("Sports");
        categories.add("Toys");
        categories.add("Books");
        categories.add("Gaming");
        categories.add("Other");

        // Set up adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productCategorySpinner.setAdapter(adapter);
    }

    private void fetchOwnedStores() {
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Fetch the list of owned store IDs from the 'users' collection
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the list of store IDs
                            ownedStores = (List<String>) documentSnapshot.get("ownedStores");

                            if (ownedStores != null && !ownedStores.isEmpty()) {
                                // Fetch store names based on store IDs
                                fetchStoreNamesAndIDs(ownedStores);
                            } else {
                                Toast.makeText(AddProduct.this, "No stores found.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddProduct.this, "Failed to fetch owned stores.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchStoreNamesAndIDs(List<String> storeIdsList) {
        storeNames = new ArrayList<>();
        storeIds = storeIdsList;
        final List<String> fetchedStoreNames = storeNames;

        for (String storeId : storeIdsList) {
            db.collection("stores").document(storeId).get()
                    .addOnSuccessListener(storeSnapshot -> {
                        if (storeSnapshot.exists()) {
                            String storeName = storeSnapshot.getString("storeName");
                            storeNames.add(storeName);

                            if (storeNames.size() == storeIdsList.size()) {
                                populateStoreSpinner(storeNames);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddProduct.this, "Failed to fetch store details.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void populateStoreSpinner(List<String> storeNames) {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, storeNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storeSpinner.setAdapter(spinnerAdapter);

        storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedStoreName = (String) parentView.getItemAtPosition(position);

                int index = storeNames.indexOf(selectedStoreName);
                if (index != -1) {
                    selectedStoreId = storeIds.get(index);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });
    }

    private void addProductToStore() {
        String name = productName.getText().toString();
        String price = productPrice.getText().toString();
        String weight = productWeight.getText().toString();
        String category = productCategorySpinner.getSelectedItem().toString();
        String inventoryCountString = productInventoryCount.getText().toString();

        if (name.isEmpty() || price.isEmpty() || weight.isEmpty() || inventoryCountString.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        int inventoryCount = Integer.parseInt(inventoryCountString);
        Product newProduct = new Product(name, price, weight, category);

        db.collection("products")
                .add(newProduct)
                .addOnSuccessListener(documentReference -> {
                    String productId = documentReference.getId();

                    db.collection("stores")
                            .document(selectedStoreId)
                            .update("products", FieldValue.arrayUnion(new HashMap<String, Object>() {{
                                put("productId", productId);
                                put("inventoryCount", inventoryCount);
                            }}))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AddProduct.this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AddProduct.this, "Failed to update store with product.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddProduct.this, "Failed to add product.", Toast.LENGTH_SHORT).show();
                });
    }
}
