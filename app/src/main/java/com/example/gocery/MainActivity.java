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

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    TextView name, mail;
    FirebaseFirestore db;
    String selectedUserType = ""; // Variable to store the selected user type

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {

                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                            Log.d("MainActivity", "Google sign-in successful: " + signInAccount.getEmail());

                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        saveUserData(user.getUid(), user.getDisplayName(), user.getEmail());

                                        // Navigate to the appropriate activity based on user type
                                        if ("Admin".equalsIgnoreCase(selectedUserType)) {
                                            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                                            startActivity(intent);
                                        } else if ("Customer".equalsIgnoreCase(selectedUserType)) {
                                            Intent intent = new Intent(MainActivity.this, ConsumerActivity.class);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(MainActivity.this, "Please select a valid user type", Toast.LENGTH_SHORT).show();
                                        }
                                        finish();
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
        getSupportActionBar().hide();  // Hides the action bar

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(MainActivity.this, options);

        // Initialize the dropdown
        AutoCompleteTextView actvUserType = findViewById(R.id.actvUserType);
        String[] userTypes = {"Admin", "Customer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userTypes);
        actvUserType.setAdapter(adapter);

        // Capture the selected user type
        actvUserType.setOnItemClickListener((parent, view, position, id) -> selectedUserType = (String) parent.getItemAtPosition(position));

        // Set up the Google Sign-In button
        SignInButton signInButton = findViewById(R.id.signIn);
        signInButton.setOnClickListener(view -> {
            if (selectedUserType.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please select a user type", Toast.LENGTH_SHORT).show();
                return;
            }

            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            });
        });
    }

    private void saveUserData(String userId, String username, String email) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("MainActivity", "User already exists in Firestore, not overriding data.");
                        Toast.makeText(MainActivity.this, "User data already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        List<String> ownedStores = Arrays.asList();
                        User user = new User(username, email, ownedStores);
                        db.collection("users").document(userId).set(user)
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
