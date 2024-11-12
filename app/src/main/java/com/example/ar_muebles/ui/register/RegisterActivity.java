package com.example.ar_muebles.ui.register;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ar_muebles.db.DBManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.ar_muebles.R;

import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private CheckBox checkBoxAdmin;
    private MaterialButton buttonRegister;

    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_register);

        // Inicializar las vistas correctas
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        checkBoxAdmin = findViewById(R.id.checkBoxAdmin);
        buttonRegister = findViewById(R.id.buttonRegister);

        // Inicializar DBManager para manejar Firestore
        dbManager = new DBManager();

        // Establecer el Listener para el botón "Registrar"
        buttonRegister.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        // Obtener los valores de los campos
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        boolean isAdmin = checkBoxAdmin.isChecked();

        // Validar los campos
        if (name.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su nombre.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su correo electrónico.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese una contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generar un ID único para el usuario
        String userId = UUID.randomUUID().toString();

        dbManager.addUser(userId, name, email, isAdmin, password, new DBManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RegisterActivity.this, "Usuario registrado correctamente.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // Mostrar mensaje de error
                Toast.makeText(RegisterActivity.this, "Error al registrar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
