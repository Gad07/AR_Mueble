package com.example.ar_muebles.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ar_muebles.MainActivity;
import com.example.ar_muebles.R;
import com.example.ar_muebles.ui.register.RegisterActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private MaterialButton buttonLogin;
    private ImageButton buttonGoogleSignIn;
    private TextView textViewRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login);

        // Inicializar los componentes de la interfaz
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoogleSignIn = findViewById(R.id.googleLoginButton);
        textViewRegister = findViewById(R.id.registerTextView);

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Listener para iniciar sesión con Firestore
        buttonLogin.setOnClickListener(view -> loginUser());

        // Listener para registrarse
        textViewRegister.setOnClickListener(view -> {
            Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        });

        // Listener para el botón de Google Sign-In
        buttonGoogleSignIn.setOnClickListener(view -> signInWithGoogle());
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validaciones de campos vacíos
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su correo electrónico.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar credenciales con Firestore
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        String storedPassword = userDoc.getString("password");
                        Boolean isAdmin = userDoc.getBoolean("isAdmin");

                        if (storedPassword != null && storedPassword.equals(password)) {
                            // Credenciales válidas
                            Log.d(TAG, "Login exitoso para: " + email);
                            updateUI(email, isAdmin);
                        } else {
                            // Contraseña incorrecta
                            Log.w(TAG, "Contraseña incorrecta para: " + email);
                            Toast.makeText(LoginActivity.this, "Contraseña incorrecta. Verifique sus credenciales.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Usuario no encontrado
                        Log.w(TAG, "No se encontró el usuario: " + email);
                        Toast.makeText(LoginActivity.this, "Usuario no encontrado. Verifique sus credenciales.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al consultar Firestore", e);
                    Toast.makeText(LoginActivity.this, "Error al iniciar sesión. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para iniciar sesión con Google
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Inicio de sesión con Google fue exitoso
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Si falla el inicio de sesión, muestra un mensaje al usuario.
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Error al iniciar sesión con Google. Intente nuevamente.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        String email = account.getEmail();
                        String userId = mAuth.getCurrentUser().getUid();

                        // Comprobar si el usuario ya existe en Firestore
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    Boolean isAdmin = false;
                                    if (!documentSnapshot.exists()) {
                                        Map<String, Object> newUser = new HashMap<>();
                                        newUser.put("userId", userId);
                                        newUser.put("email", email);
                                        newUser.put("isAdmin", false); // Crear el usuario con rol "normal"

                                        db.collection("users").document(userId)
                                                .set(newUser)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Usuario Google creado en Firestore: " + email))
                                                .addOnFailureListener(e -> Log.w(TAG, "Error al crear usuario Google en Firestore", e));
                                    } else {
                                        isAdmin = documentSnapshot.getBoolean("isAdmin");
                                    }
                                    updateUI(email, isAdmin);
                                })
                                .addOnFailureListener(e -> Log.w(TAG, "Error al obtener el documento de usuario", e));
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Error al autenticar con Google.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(String email, Boolean isAdmin) {
        if (email != null) {
            // Guardar el correo electrónico en SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userEmail", email);
            editor.apply();

            // Continuar con la lógica de actualización de la UI
            Toast.makeText(LoginActivity.this, "Bienvenido, " + email, Toast.LENGTH_SHORT).show();
            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
            mainIntent.putExtra("isAdmin", isAdmin);
            mainIntent.putExtra("userEmail", email);
            startActivity(mainIntent);
            finish();
        }
    }

}