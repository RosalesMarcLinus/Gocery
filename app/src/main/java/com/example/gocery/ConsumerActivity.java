package com.example.gocery;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConsumerActivity extends AppCompatActivity implements CategoryProductAdapter.OnProductUpdateListener {
    private static final int SCANNER_REQUEST_CODE = 100;
    private FirebaseFirestore db;
    private ExpandableListView expandableListView;
    private TextView totalPriceText;
    private Button scanButton;
    private Button checkoutButton;
    private List<GetProduct> productList;
    private CategoryProductAdapter adapter;
    private double totalPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer);

        db = FirebaseFirestore.getInstance();

        expandableListView = findViewById(R.id.expandableListView);
        totalPriceText = findViewById(R.id.totalPriceText);
        scanButton = findViewById(R.id.scanButton);
        checkoutButton = findViewById(R.id.checkoutButton);

        productList = new ArrayList<>();
        adapter = new CategoryProductAdapter(this, productList, this);
        expandableListView.setAdapter(adapter);

        scanButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConsumerActivity.this, ScannerActivity.class);
            startActivityForResult(intent, SCANNER_REQUEST_CODE);
        });

        checkoutButton.setOnClickListener(v -> handleCheckout());
        updateTotalPrice();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCANNER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String productId = data.getStringExtra("scanned_product_id");
            if (productId != null) {
                fetchProductDetails(productId);
            }
        }
    }

    private void fetchProductDetails(String productId) {
        db.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        GetProduct product = new GetProduct();
                        product.setId(productId);
                        product.setProductName(documentSnapshot.getString("productName"));
                        product.setCategory(documentSnapshot.getString("category"));
                        product.setPrice(documentSnapshot.getString("price"));
                        product.setWeight(documentSnapshot.getString("weight"));

                        findProductInventory(product);
                    } else {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching product", Toast.LENGTH_SHORT).show());
    }

    private void findProductInventory(GetProduct product) {
        db.collection("stores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        List<Object> products = (List<Object>) document.get("products");
                        if (products != null) {
                            for (Object obj : products) {
                                if (obj instanceof Map) {
                                    Map<String, Object> productMap = (Map<String, Object>) obj;
                                    if (product.getId().equals(productMap.get("productId"))) {
                                        Long inventoryCount = (Long) productMap.get("inventoryCount");
                                        product.setInventoryCount(inventoryCount.intValue());
                                        addProductToCart(product);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    Toast.makeText(this, "Product not found in any store", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking inventory", Toast.LENGTH_SHORT).show());
    }

    private void addProductToCart(GetProduct product) {
        for (GetProduct p : productList) {
            if (p.getId().equals(product.getId())) {
                Toast.makeText(this, "Product already in cart", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        productList.add(product);
        adapter.updateProducts(productList);
        updateTotalPrice();
    }

    @Override
    public void onQuantityChanged() {
        updateTotalPrice();
    }

    @Override
    public void onProductRemoved(GetProduct product) {
        productList.remove(product);
        adapter.updateProducts(productList);
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (GetProduct product : productList) {
            totalPrice += product.getPriceAsDouble() * product.getSelectedQuantity();
        }
        totalPriceText.setText(String.format("Total: $%.2f", totalPrice));
    }

    private void handleCheckout() {
        if (productList.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String buyerUid = currentUser.getUid(); // Get current user's UID

        List<String> productsBought = new ArrayList<>();
        List<Double> productsCost = new ArrayList<>();
        List<Integer> productsCount = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> productIds = new ArrayList<>();

        for (GetProduct product : productList) {
            if (product.getSelectedQuantity() > 0) {
                productsBought.add(product.getProductName());
                productsCost.add(product.getPriceAsDouble());
                productsCount.add(product.getSelectedQuantity());
                categories.add(product.getCategory());
                productIds.add(product.getId());
            }
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Find the sellerId based on productIds
        db.collection("stores")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String sellerId = null;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<Map<String, Object>> products = (List<Map<String, Object>>) document.get("products");
                            if (products != null) {
                                for (Map<String, Object> product : products) {
                                    String productId = (String) product.get("productId");
                                    if (productIds.contains(productId)) {
                                        sellerId = document.getString("ownerId");
                                        break;
                                    }
                                }
                            }
                            if (sellerId != null) break;
                        }

                        if (sellerId == null) {
                            Toast.makeText(this, "Seller not found for the products", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create report and navigate
                        Map<String, Object> report = new HashMap<>();
                        report.put("buyerId", buyerUid);
                        report.put("sellerId", sellerId);
                        report.put("date", new Date());
                        report.put("productsBought", productsBought);
                        report.put("productsCost", productsCost);
                        report.put("productsCount", productsCount);
                        report.put("categories", categories);
                        report.put("totalCost", totalPrice);
                        report.put("productIds", productIds);

                        String finalSellerId = sellerId;
                        db.collection("reports")
                                .add(report)
                                .addOnSuccessListener(documentReference -> {
                                    String documentId = documentReference.getId();
                                    Intent intent = new Intent(ConsumerActivity.this, ReceiptActivity.class); // Use explicit context
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm:ss a", Locale.getDefault());
                                    intent.putExtra("date", sdf.format(new Date()));
                                    intent.putExtra("sellerId", finalSellerId); // Ensure sellerId is not null
                                    intent.putExtra("total", totalPrice);
                                    intent.putStringArrayListExtra("products", new ArrayList<>(productsBought));
                                    intent.putStringArrayListExtra("categories", new ArrayList<>(categories));
                                    intent.putExtra("costs", productsCost.stream().mapToDouble(Double::doubleValue).toArray());
                                    intent.putExtra("counts", productsCount.stream().mapToInt(Integer::intValue).toArray());
                                    intent.putExtra("documentId", documentId);

                                    startActivity(intent);

                                    productList.clear();
                                    adapter.updateProducts(productList);
                                    updateTotalPrice();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error creating receipt", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Failed to fetch store data", Toast.LENGTH_SHORT).show();
                    }
                });

    }




}