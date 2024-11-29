package com.example.gocery;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public class AddStore extends AppCompatActivity {

    private EditText storeName;
    private EditText storeLocation;
    private EditText storePhone;

    // Initialize Firestore
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_store);  // Make sure you have the correct layout
        getSupportActionBar().hide();  // Hides the action bar
        FirebaseApp.initializeApp(AddStore.this);

        // Initialize Firestore and FirebaseAuth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        storeName = findViewById(R.id.storeName);
        storeLocation = findViewById(R.id.storeLocation);
        storePhone = findViewById(R.id.storePhone);

        Button submitButton = findViewById(R.id.btnSubmitStore);

        submitButton.setOnClickListener(v -> {
            // Validate inputs
            if (validateInputs()) {
                String name = storeName.getText().toString();
                String location = storeLocation.getText().toString();
                String phone = storePhone.getText().toString();

                // Get the current user's ID
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(AddStore.this, "No user signed in", Toast.LENGTH_SHORT).show();
                    return;
                }
                String userId = user.getUid();  // Current user's ID

                // Create a Store object
                Store newStore = new Store(name, location, phone, userId);

                // Add the new store to Firestore
                db.collection("stores")  // "stores" is the collection in Firestore
                        .add(newStore)  // Add the store object as a new document
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                // Get the new store's document ID
                                String storeId = documentReference.getId();
                                updateUserOwnedStores(userId, storeId);
                                Toast.makeText(AddStore.this, "Store Registered Successfully!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Failure logic here
                                Toast.makeText(AddStore.this, "Failed to Register Store", Toast.LENGTH_SHORT).show();
                                Log.e("Firebase", "Error adding document", e);
                            }
                        });
            }
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> finish());

    }

    // Validate that all fields are filled out
    private boolean validateInputs() {
        boolean valid = true;

        String name = storeName.getText().toString();
        String location = storeLocation.getText().toString();
        String phone = storePhone.getText().toString();

        if (name.isEmpty()) {
            storeName.setError("Store Name is required");
            valid = false;
        } else {
            storeName.setError(null);
        }

        if (location.isEmpty()) {
            storeLocation.setError("Store Location is required");
            valid = false;
        } else {
            storeLocation.setError(null);
        }

        if (phone.isEmpty()) {
            storePhone.setError("Store Phone is required");
            valid = false;
        } else {
            storePhone.setError(null);
        }

        return valid;
    }

    // Update the user's ownedStores array in Firestore
    private void updateUserOwnedStores(String userId, String storeId) {
        // Reference to the user document in Firestore
        DocumentReference userRef = db.collection("users").document(userId);

        // Add the new store ID to the ownedStores array
        userRef.update("ownedStores", FieldValue.arrayUnion(storeId))
                .addOnSuccessListener(aVoid -> {
                    // Successfully added the store ID to the user's ownedStores
                    Log.d("Firebase", "Store added to user's ownedStores");
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    Log.e("Firebase", "Error updating user's ownedStores", e);
                });
    }


}
