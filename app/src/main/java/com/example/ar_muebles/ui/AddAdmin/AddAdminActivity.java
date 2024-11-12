package com.example.ar_muebles.ui.AddAdmin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddAdminActivity extends Fragment {

    private static final String TAG = "AddAdminFragment";
    private static final int PICK_MODEL_REQUEST = 1;

    // Elementos de la UI
    private TextInputEditText editNombreModelo, editDescripcionModelo;
    private Uri modelUri;  // Para almacenar la Uri del archivo del modelo seleccionado
    private DBManager dbManager;

    // Firebase Storage
    private StorageReference storageReference;

    // ProgressDialog para indicar el progreso de la subida
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_agregar_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar Firebase
        FirebaseApp.initializeApp(getContext());

        // Inicializar elementos de la UI
        editNombreModelo = view.findViewById(R.id.editNombreModelo);
        editDescripcionModelo = view.findViewById(R.id.editDescripcionModelo);
        MaterialButton addButton = view.findViewById(R.id.btnAdd); // Botón para agregar el modelo
        MaterialButton chooseFileButton = view.findViewById(R.id.btnChooseFile); // Botón para elegir el archivo

        // Inicializar DBManager y Firebase Storage
        dbManager = new DBManager();
        storageReference = FirebaseStorage.getInstance().getReference("furniture_models");

        // Inicializar ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Subiendo modelo, por favor espera...");
        progressDialog.setCancelable(false);

        // Configurar botón para abrir el selector de archivos y seleccionar un modelo 3D
        chooseFileButton.setOnClickListener(v -> openFileChooser());

        // Configurar botón de agregar para subir los detalles del mueble
        addButton.setOnClickListener(v -> addNewFurniture());
    }

    // Abrir el selector de archivos para seleccionar un modelo 3D
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"model/gltf+json", "model/gltf-binary", "application/octet-stream"});
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_MODEL_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MODEL_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            modelUri = data.getData();
            Toast.makeText(getContext(), "Modelo seleccionado: " + modelUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
        }
    }

    // Método para agregar un nuevo modelo de mueble
    private void addNewFurniture() {
        String nombreModelo = editNombreModelo.getText().toString().trim();
        String descripcionModelo = editDescripcionModelo.getText().toString().trim();

        if (nombreModelo.isEmpty() || descripcionModelo.isEmpty() || modelUri == null) {
            Toast.makeText(getContext(), "Por favor completa todos los campos y selecciona un archivo de modelo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar el ProgressDialog
        progressDialog.show();

        // Generar un ID único para el mueble
        String furnitureId = UUID.randomUUID().toString();

        // Subir el archivo del modelo a Firebase Storage
        StorageReference fileReference = storageReference.child(furnitureId + ".glb");
        fileReference.putFile(modelUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Obtener la URL de descarga del archivo del modelo
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String modelUrl = uri.toString();
                        // Agregar los detalles del mueble a Firestore
                        addFurnitureToFirestore(furnitureId, nombreModelo, descripcionModelo, modelUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.w(TAG, "Error al subir el archivo del modelo", e);
                    Toast.makeText(getContext(), "Error al subir el archivo del modelo", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para agregar el mueble a Firestore
    private void addFurnitureToFirestore(String furnitureId, String nombre, String descripcion, String modelUrl) {
        Map<String, Object> furnitureData = new HashMap<>();
        furnitureData.put("name", nombre);
        furnitureData.put("description", descripcion);
        furnitureData.put("modelUrl", modelUrl);

        dbManager.getFurnitureCollection().document(furnitureId)
                .set(furnitureData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Log.d(TAG, "Mueble agregado correctamente con ID: " + furnitureId);
                    Toast.makeText(getContext(), "Mueble agregado exitosamente", Toast.LENGTH_SHORT).show();

                    // Limpiar los campos después de agregar
                    editNombreModelo.setText("");
                    editDescripcionModelo.setText("");
                    modelUri = null;
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.w(TAG, "Error al agregar el mueble", e);
                    Toast.makeText(getContext(), "Error al agregar el mueble", Toast.LENGTH_SHORT).show();
                });
    }
}
