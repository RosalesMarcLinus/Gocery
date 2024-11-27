package com.example.gocery;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShowReceipt extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView receiptTextView;
    private Button updateInventoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_receipt);

        db = FirebaseFirestore.getInstance();

        String receiptId = getIntent().getStringExtra("scanned_product_id");

        receiptTextView = findViewById(R.id.receipt_text_view);
        updateInventoryButton = findViewById(R.id.button_update_inventory);

        fetchReceiptData(receiptId);

        updateInventoryButton.setOnClickListener(v -> {
            updateInventory(receiptId);
        });
    }

    private void fetchReceiptData(String receiptId) {
        db.collection("reports").document(receiptId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> receiptData = documentSnapshot.getData();

                        if (receiptData != null) {
                            try {
                                String buyerId = (String) receiptData.get("buyerId");
                                String sellerId = (String) receiptData.get("sellerId");

                                List<Object> productIdObjects = (List<Object>) receiptData.get("productIds");
                                List<String> productIds = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    productIds = productIdObjects != null ?
                                            productIdObjects.stream().map(Object::toString).toList() : null;
                                }

                                List<Object> productsBoughtObjects = (List<Object>) receiptData.get("productsBought");
                                List<String> productsBought = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    productsBought = productsBoughtObjects != null ?
                                            productsBoughtObjects.stream().map(Object::toString).toList() : null;
                                }

                                List<Object> productsCostObjects = (List<Object>) receiptData.get("productsCost");
                                List<Long> productsCost = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    productsCost = productsCostObjects != null ?
                                            productsCostObjects.stream().map(obj -> ((Number) obj).longValue()).toList() : null;
                                }

                                List<Object> productsCountObjects = (List<Object>) receiptData.get("productsCount");
                                List<Long> productsCount = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    productsCount = productsCountObjects != null ?
                                            productsCountObjects.stream().map(obj -> ((Number) obj).longValue()).toList() : null;
                                }

                                Long totalCost = receiptData.get("totalCost") != null ?
                                        ((Number) receiptData.get("totalCost")).longValue() : null;

                                if (buyerId == null || sellerId == null || productIds == null ||
                                        productsBought == null || productsCost == null || productsCount == null || totalCost == null) {
                                    throw new IllegalArgumentException("Missing or invalid fields in receipt data.");
                                }

                                StringBuilder receiptDetails = new StringBuilder();
                                receiptDetails.append("Buyer: ").append(buyerId).append("\n");
                                receiptDetails.append("Seller: ").append(sellerId).append("\n");
                                receiptDetails.append("Total Cost: ").append(totalCost).append("\n\n");
                                receiptDetails.append("Products:\n");

                                fetchProductDetails(productIds, productsBought, productsCost, productsCount, receiptDetails);
                            } catch (Exception e) {
                                Log.e("ShowReceipt", "Error parsing receipt data: ", e);
                                Toast.makeText(this, "Error parsing receipt data.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Receipt data is null.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "No receipt found for ID: " + receiptId, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ShowReceipt", "Error fetching receipt data: ", e);
                    Toast.makeText(this, "Error fetching receipt data.", Toast.LENGTH_LONG).show();
                });
    }


    private void fetchProductDetails(List<String> productIds, List<String> productsBought,
                                     List<Long> productsCost, List<Long> productsCount,
                                     StringBuilder receiptDetails) {
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            String productName = productsBought.get(i);
            long productCost = productsCost.get(i);
            long productCount = productsCount.get(i);

            receiptDetails.append(i + 1).append(". ").append(productName).append("\n")
                    .append("   Quantity: ").append(productCount).append("\n")
                    .append("   Cost: ").append(productCost).append("\n\n");

            receiptTextView.setText(receiptDetails.toString());
        }
    }

    private void updateInventory(String receiptId) {
        db.collection("reports").document(receiptId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> receiptData = documentSnapshot.getData();
                        List<Object> productIdObjects = (List<Object>) receiptData.get("productIds");
                        List<String> productIds = new ArrayList<>();
                        for (Object productId : productIdObjects) {
                            productIds.add((String) productId);
                        }

                        List<Long> productsCount = new ArrayList<>();
                        List<Object> productsCountObjects = (List<Object>) receiptData.get("productsCount");
                        for (Object productCount : productsCountObjects) {
                            productsCount.add(((Number) productCount).longValue());
                        }

                        Toast.makeText(this, "Found " + productIds.size() + " products to update.", Toast.LENGTH_LONG).show();

                        updateNextProduct(0, productIds, productsCount);
                    } else {
                        Toast.makeText(this, "Receipt document not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch receipt data.", Toast.LENGTH_SHORT).show();
                    Log.e("ShowReceipt", "Error fetching receipt data: ", e);
                });
    }

    private void updateNextProduct(int index, List<String> productIds, List<Long> productsCount) {
        if (index < productIds.size()) {
            String productId = productIds.get(index);
            long productCount = productsCount.get(index);

            db.collection("stores")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot storeDoc : queryDocumentSnapshots) {
                                Map<String, Object> storeData = storeDoc.getData();
                                List<Map<String, Object>> products = (List<Map<String, Object>>) storeData.get("products");

                                boolean productFound = false;

                                for (Map<String, Object> product : products) {
                                    String storeProductId = (String) product.get("productId");

                                    if (storeProductId.equals(productId)) {
                                        long inventoryCount = (long) product.get("inventoryCount");

                                        long newInventoryCount = inventoryCount - productCount;

                                        if (newInventoryCount < 0) {
                                            Toast.makeText(this, "Invalid inventory update (Inventory would go below 0): " + productId, Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        product.put("inventoryCount", newInventoryCount);

                                        storeDoc.getReference()
                                                .update("products", products)
                                                .addOnSuccessListener(aVoid -> {

                                                    TextView inventoryTextView = findViewById(R.id.inventory_count_text);

                                                    String textToAppend = "Product: " + productId + " -> Before Inventory Count: " + inventoryCount + " -> After Inventory Count: " + newInventoryCount + "\n";

                                                    inventoryTextView.append(textToAppend);
                                                    updateNextProduct(index + 1, productIds, productsCount);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(ShowReceipt.this, "Failed to update inventory for product: " + productId, Toast.LENGTH_SHORT).show();
                                                    Log.e("ShowReceipt", "Error updating inventory for product: " + productId, e);
                                                });

                                        productFound = true;
                                        break;
                                    }
                                }

                                if (!productFound) {
                                    Toast.makeText(this, "Product " + productId + " not found in store " + storeDoc.getId(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(this, "No stores found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error querying store data.", Toast.LENGTH_SHORT).show();
                        Log.e("ShowReceipt", "Error querying store data: ", e);
                    });
        }
    }



}