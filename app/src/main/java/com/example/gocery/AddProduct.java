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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddProduct extends AppCompatActivity {

    private EditText productName, productPrice, productWeight, productCategory, productInventoryCount;
    private Spinner storeSpinner;
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
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productWeight = findViewById(R.id.productWeight);
        productCategory = findViewById(R.id.productCategory);
        productInventoryCount = findViewById(R.id.productInventoryCount);
        submitProductButton = findViewById(R.id.submitProductButton);

        // Fetch the list of owned stores for the current user
        fetchOwnedStores();

        // Set the submit button click listener
        submitProductButton.setOnClickListener(v -> addProductToStore());
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

    // Fetch store names based on store IDs and map them to their IDs
    private void fetchStoreNamesAndIDs(List<String> storeIdsList) {
        storeNames = new ArrayList<>();
        storeIds = storeIdsList;
        final List<String> fetchedStoreNames = storeNames;

        // Iterate over the store IDs and fetch the store names
        for (String storeId : storeIdsList) {
            db.collection("stores").document(storeId).get()
                    .addOnSuccessListener(storeSnapshot -> {
                        if (storeSnapshot.exists()) {
                            String storeName = storeSnapshot.getString("storeName");
                            storeNames.add(storeName); // Add the store name to the list

                            // After fetching all store names, populate the spinner
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

    // Populate the store spinner and create a mapping from store name to store ID
    private void populateStoreSpinner(List<String> storeNames) {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, storeNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storeSpinner.setAdapter(spinnerAdapter);

        // Set an item selected listener to retrieve the selected store ID based on store name
        storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Get the store name selected by the user
                String selectedStoreName = (String) parentView.getItemAtPosition(position);

                // Find the corresponding store ID using the storeNames list
                int index = storeNames.indexOf(selectedStoreName);
                if (index != -1) {
                    selectedStoreId = storeIds.get(index); // Store the selected store's ID
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    // Method to add the product to the store with an inventory count
    private void addProductToStore() {
        String name = productName.getText().toString();
        String price = productPrice.getText().toString();
        String weight = productWeight.getText().toString();
        String category = productCategory.getText().toString();
        String selectedStore = storeSpinner.getSelectedItem().toString(); // Get the selected store name
        String inventoryCountString = productInventoryCount.getText().toString(); // Get inventory count
        int inventoryCount = Integer.parseInt(inventoryCountString);

        // Validate inputs
        if (name.isEmpty() || price.isEmpty() || weight.isEmpty() || category.isEmpty() || inventoryCountString.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Product object
        Product newProduct = new Product(name, price, weight, category);

        // Add the product to Firestore (in the 'products' collection)
        db.collection("products")
                .add(newProduct)
                .addOnSuccessListener(documentReference -> {
                    String productId = documentReference.getId(); // Get the newly created product ID

                    // Now add the product and its inventory count to the selected store's products array
                    db.collection("stores")
                            .whereEqualTo("storeName", selectedStore) // Search for the store by name
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    // Get the first store (assuming store names are unique)
                                    String storeId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                    // Create a map for the product and its inventory count
                                    Map<String, Object> productWithInventory = new HashMap<>();
                                    productWithInventory.put("productId", productId);
                                    productWithInventory.put("inventoryCount", inventoryCount);

                                    // Add the product and inventory count to the store's products array
                                    db.collection("stores")
                                            .document(storeId)
                                            .update("products", FieldValue.arrayUnion(productWithInventory))
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(AddProduct.this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(AddProduct.this, "Failed to update store with product.", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(AddProduct.this, "Store not found.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AddProduct.this, "Failed to fetch store details.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddProduct.this, "Failed to add product.", Toast.LENGTH_SHORT).show();
                });
    }

}
