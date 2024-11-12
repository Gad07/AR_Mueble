package com.example.ar_muebles.ui.editarPerfil;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ar_muebles.R;
import com.example.ar_muebles.db.DBManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class editarPActivity extends Fragment {

    private static final String TAG = "EditarPActivity";

    // Elementos de la UI
    private TextInputEditText editCorreo, editPasswordActual, editNewPassword, editConfirmPassword;
    private DBManager dbManager;

    // Variable para almacenar el correo electrónico
    private String userCorreo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_editar_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar elementos de la UI
        editCorreo = view.findViewById(R.id.editCorreo);
        editPasswordActual = view.findViewById(R.id.editPasswordActual);
        editNewPassword = view.findViewById(R.id.editNewPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);
        MaterialButton saveButton = view.findViewById(R.id.btnSave);

        // Inicializar DBManager
        dbManager = new DBManager();

        // Obtener el correo electrónico del usuario desde SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        userCorreo = sharedPreferences.getString("userEmail", null);

        if (TextUtils.isEmpty(userCorreo)) {
            Toast.makeText(getContext(), "Error: No se pudo obtener el correo del usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cargar la información del perfil actual (correo y contraseña)
        cargarDatosPerfil(userCorreo);

        // Configurar el botón de guardar para actualizar la información del perfil
        saveButton.setOnClickListener(v -> updateProfileData());
    }

    // Método para cargar la información del perfil del usuario desde Firestore usando el correo
    private void cargarDatosPerfil(String correo) {
        dbManager.getUsersCollection()
                .whereEqualTo("email", correo)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Obtener el primer documento que coincide con el correo
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Log.d(TAG, "Usuario encontrado con ID: " + userId);

                        // Extraer la información del documento
                        editCorreo.setText(correo);
                        editCorreo.setEnabled(false); // Desactivar el campo para que no pueda ser editado
                    } else {
                        Toast.makeText(getContext(), "No se encontró un usuario con ese correo", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al buscar el usuario por correo", e);
                    Toast.makeText(getContext(), "Error al buscar el usuario", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para actualizar la información del perfil del usuario en Firestore
    private void updateProfileData() {
        String passwordActual = editPasswordActual.getText().toString().trim();
        String nuevaContraseña = editNewPassword.getText().toString().trim();
        String confirmarContraseña = editConfirmPassword.getText().toString().trim();

        // Validación de campos vacíos
        if (TextUtils.isEmpty(passwordActual) || TextUtils.isEmpty(nuevaContraseña) || TextUtils.isEmpty(confirmarContraseña)) {
            Toast.makeText(getContext(), "Por favor completa todos los campos de contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que las nuevas contraseñas coincidan
        if (!nuevaContraseña.equals(confirmarContraseña)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lógica para verificar la contraseña actual y luego actualizar
        dbManager.getUsersCollection().whereEqualTo("email", userCorreo).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String storedPassword = queryDocumentSnapshots.getDocuments().get(0).getString("password");
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        if (storedPassword != null && storedPassword.equals(passwordActual)) {
                            // Actualizar la contraseña
                            updatePassword(userId, nuevaContraseña);
                        } else {
                            Toast.makeText(getContext(), "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al verificar la contraseña actual", e);
                    Toast.makeText(getContext(), "Error al verificar la contraseña actual", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para actualizar la contraseña en la base de datos
    private void updatePassword(String userId, String nuevaContraseña) {
        dbManager.getUsersCollection().document(userId)
                .update("password", nuevaContraseña)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar la contraseña", e);
                    Toast.makeText(getContext(), "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show();
                });
    }
}
