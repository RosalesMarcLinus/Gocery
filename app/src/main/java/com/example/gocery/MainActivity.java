package com.example.gocery;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    TextView name, mail;
    FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {

                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            // Attempt to sign in with the account returned from Google
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                            Log.d("MainActivity", "Google sign-in successful: " + signInAccount.getEmail());

                            // Use the ID token to authenticate with Firebase
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        // Display user info in the TextViews
                                        name.setText(user.getDisplayName());
                                        mail.setText(user.getEmail());
                                        Log.d("MainActivity", "User signed in: " + user.getDisplayName());

                                        // Save the user's data to Firestore
                                        saveUserData(user.getUid(), user.getDisplayName(), user.getEmail());

                                        // Navigate to AddStore activity after successful sign-in
                                        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                                        startActivity(intent);
                                        finish();  // Close MainActivity to prevent going back
                                    }
                                } else {
                                    Log.e("MainActivity", "Authentication failed", task.getException());
                                    Toast.makeText(MainActivity.this, "Sign-in failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (ApiException e) {
                            Log.e("MainActivity", "Google sign-in failed", e);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // Google Sign-In configuration
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))  // Use your actual client_id here
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(MainActivity.this, options);

        // Initialize UI elements
        name = findViewById(R.id.nameTV);
        mail = findViewById(R.id.mailTV);

        // Set up the Google Sign-In button
        SignInButton signInButton = findViewById(R.id.signIn);
        signInButton.setOnClickListener(view -> {
            // Sign out before starting the sign-in process to allow choosing a different account
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Start the Google sign-in intent
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            });
        });
    }

    private void saveUserData(String userId, String username, String email) {
        // Reference to the user document in the 'users' collection
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Check if the document exists
                    if (documentSnapshot.exists()) {
                        // Document exists, don't override the data
                        Log.d("MainActivity", "User already exists in Firestore, not overriding data.");
                        Toast.makeText(MainActivity.this, "User data already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        // Document does not exist, save the new user data
                        List<String> ownedStores = Arrays.asList();  // Start with an empty list of owned stores

                        // Create a map of user data
                        User user = new User(username, email, ownedStores);

                        // Save user data to Firestore
                        db.collection("users")
                                .document(userId)  // Use the user ID as the document ID
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("MainActivity", "User data saved successfully");
                                    Toast.makeText(MainActivity.this, "User data saved", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MainActivity", "Error saving user data", e);
                                    Toast.makeText(MainActivity.this, "Error saving user data", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error checking if user exists", e);
                    Toast.makeText(MainActivity.this, "Error checking user existence", Toast.LENGTH_SHORT).show();
                });
    }

}
