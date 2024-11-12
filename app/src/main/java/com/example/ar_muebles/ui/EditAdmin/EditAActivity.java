package com.example.ar_muebles.ui.EditAdmin;

import com.example.ar_muebles.R;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ar_muebles.db.DBManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EditAActivity extends Fragment {

    private static final String TAG = "EditAdminFragment";

    // Elementos de la UI
    private TextInputEditText editNombreModelo, editDescripcionModelo;
    private AutoCompleteTextView autoCompleteModelos;
    private String selectedFurnitureId;  // Para almacenar el ID del mueble seleccionado
    private String selectedFurnitureModelUrl;  // Para almacenar la URL del modelo seleccionado

    // Firebase
    private DBManager dbManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_editar_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar elementos de la UI
        editNombreModelo = view.findViewById(R.id.editNombreModelo);
        editDescripcionModelo = view.findViewById(R.id.editDescripcionModelo);
        autoCompleteModelos = view.findViewById(R.id.autoCompleteModelos);
        MaterialButton updateButton = view.findViewById(R.id.btnUpdate); // Botón para actualizar el modelo

        // Inicializar DBManager
        dbManager = new DBManager();

        // Configurar botón para actualizar los detalles del mueble
        updateButton.setOnClickListener(v -> updateFurnitureDetails());

        // Cargar todos los modelos de muebles
        loadFurnitureModels();

        // Configurar listener para el AutoCompleteTextView cuando el usuario selecciona un mueble
        autoCompleteModelos.setOnItemClickListener((parent, view1, position, id) -> {
            String nombreModelo = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Modelo seleccionado: " + nombreModelo);
            fetchFurnitureDetails(nombreModelo);
        });

        // Forzar la visualización del menú desplegable cuando el usuario hace clic en el campo de texto
        autoCompleteModelos.setOnClickListener(v -> autoCompleteModelos.showDropDown());
    }

    // Cargar todos los modelos de muebles desde Firestore
    private void loadFurnitureModels() {
        dbManager.getFurnitureCollection().get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "No se encontraron modelos para mostrar.");
                        Toast.makeText(getContext(), "No hay modelos disponibles.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> listaModelos = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String nombreModelo = doc.getString("name");
                        if (nombreModelo != null) {
                            listaModelos.add(nombreModelo);
                            Log.d(TAG, "Modelo encontrado: " + nombreModelo);
                        }
                    }

                    if (listaModelos.isEmpty()) {
                        Log.w(TAG, "La lista de modelos está vacía después de procesar los documentos.");
                        Toast.makeText(getContext(), "No hay modelos disponibles.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Número de modelos cargados: " + listaModelos.size());
                        // Actualizar el adaptador con los modelos cargados
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, listaModelos);
                        autoCompleteModelos.setAdapter(adapter);
                        autoCompleteModelos.setOnClickListener(v -> autoCompleteModelos.showDropDown());  // Mostrar siempre el desplegable
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al cargar los modelos de muebles", e);
                    Toast.makeText(getContext(), "Error al cargar los modelos", Toast.LENGTH_SHORT).show();
                });
    }

    // Obtener los detalles del mueble seleccionado
    private void fetchFurnitureDetails(String nombreModelo) {
        dbManager.getFurnitureCollection().whereEqualTo("name", nombreModelo).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Número de documentos encontrados: " + queryDocumentSnapshots.size());
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        selectedFurnitureId = document.getId();
                        editNombreModelo.setText(document.getString("name"));
                        editDescripcionModelo.setText(document.getString("description"));

                        // Guardar la URL del modelo 3D para posteriores actualizaciones
                        selectedFurnitureModelUrl = document.getString("modelUrl");

                        Log.d(TAG, "Detalles del modelo cargados para: " + nombreModelo);
                        Toast.makeText(requireContext(), "Detalles del modelo cargados", Toast.LENGTH_SHORT).show();

                        // Mostrar de nuevo el menú desplegable en caso de querer cambiar
                        autoCompleteModelos.setOnClickListener(v -> autoCompleteModelos.showDropDown());
                    } else {
                        Log.w(TAG, "No se encontró ningún documento con el nombre: " + nombreModelo);
                        Toast.makeText(requireContext(), "No se encontró el modelo seleccionado", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al obtener los detalles del mueble", e);
                    Toast.makeText(requireContext(), "Error al obtener los detalles", Toast.LENGTH_SHORT).show();
                });
    }

    // Actualizar los detalles del mueble
    private void updateFurnitureDetails() {
        if (selectedFurnitureId == null) {
            Toast.makeText(getContext(), "Por favor, selecciona un modelo de mueble", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombreActualizado = editNombreModelo.getText().toString().trim();
        String descripcionActualizada = editDescripcionModelo.getText().toString().trim();

        if (nombreActualizado.isEmpty() || descripcionActualizada.isEmpty()) {
            Toast.makeText(getContext(), "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar el mueble en Firestore
        updateFurnitureInFirestore(nombreActualizado, descripcionActualizada, selectedFurnitureModelUrl);
    }

    // Actualizar el mueble en Firestore con la URL del modelo 3D
    private void updateFurnitureInFirestore(String nombre, String descripcion, @Nullable String modelUrl) {
        DBManager.FirestoreCallback updateCallback = new DBManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Mueble actualizado exitosamente", Toast.LENGTH_SHORT).show();
                // Actualizar la lista de modelos después de la actualización
                loadFurnitureModels();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Error al actualizar el mueble", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Error al actualizar el mueble: ", e);
            }
        };

        // Llamar al método de DBManager para actualizar el mueble en Firestore
        dbManager.updateFurniture(selectedFurnitureId, nombre, descripcion, modelUrl, updateCallback);
    }
}
