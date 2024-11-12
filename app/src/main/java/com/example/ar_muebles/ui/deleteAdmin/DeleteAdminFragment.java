package com.example.ar_muebles.ui.deleteAdmin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ar_muebles.R;
import com.example.ar_muebles.db.DBManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class DeleteAdminFragment extends Fragment {

    private static final String TAG = "DeleteAdminFragment";

    // Elementos de la UI
    private AutoCompleteTextView autoCompleteModelos;
    private TextView nombreModeloTextView, descripcionModeloTextView;
    private DBManager dbManager;
    private String selectedFurnitureId = null;
    private String selectedFurnitureImageUrl = null;

    // Firebase Storage
    private StorageReference storageReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_eliminar_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar elementos de la UI
        autoCompleteModelos = view.findViewById(R.id.autoCompleteModelos);
        nombreModeloTextView = view.findViewById(R.id.nombreModelo); // TextView para mostrar el nombre del modelo
        descripcionModeloTextView = view.findViewById(R.id.descripcionModelo); // TextView para mostrar la descripción
        MaterialButton deleteButton = view.findViewById(R.id.btnDelete); // Botón para eliminar el modelo

        // Inicializar DBManager y Firebase Storage
        dbManager = new DBManager();
        storageReference = FirebaseStorage.getInstance().getReference("furniture");

        // Cargar los modelos de muebles desde Firestore
        loadFurnitureModels();

        // Configurar AutoComplete para escuchar la selección de un modelo
        autoCompleteModelos.setOnItemClickListener((adapterView, view1, position, id) -> {
            String modeloSeleccionado = (String) adapterView.getItemAtPosition(position);
            fetchFurnitureDetails(modeloSeleccionado);
        });

        // Forzar la visualización del menú desplegable cuando el usuario hace clic en el campo de texto
        autoCompleteModelos.setOnClickListener(v -> autoCompleteModelos.showDropDown());

        // Configurar botón de eliminar
        deleteButton.setOnClickListener(v -> {
            if (selectedFurnitureId != null) {
                deleteSelectedFurniture();
            } else {
                Toast.makeText(getContext(), "Por favor selecciona un modelo para eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para cargar los modelos de muebles disponibles desde Firestore
    private void loadFurnitureModels() {
        dbManager.getFurnitureCollection().get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> listaModelos = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String nombreModelo = doc.getString("name");
                        if (nombreModelo != null) {
                            listaModelos.add(nombreModelo);
                        }
                    }
                    if (listaModelos.isEmpty()) {
                        Toast.makeText(getContext(), "No hay modelos disponibles.", Toast.LENGTH_SHORT).show();
                    } else {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, listaModelos);
                        autoCompleteModelos.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al cargar los modelos de muebles", e);
                    Toast.makeText(getContext(), "Error al cargar los modelos", Toast.LENGTH_SHORT).show();
                });
    }

    // Obtener los detalles del mueble seleccionado
    private void fetchFurnitureDetails(String nombreModeloSeleccionado) {
        dbManager.getFurnitureCollection().whereEqualTo("name", nombreModeloSeleccionado).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        selectedFurnitureId = document.getId();
                        selectedFurnitureImageUrl = document.getString("modelUrl");

                        // Establecer el nombre del modelo
                        String nombre = document.getString("name");
                        if (nombre != null) {
                            nombreModeloTextView.setText(nombre);  // Actualizar el campo de nombre en el TextView
                        }

                        // Establecer la descripción del modelo
                        String descripcion = document.getString("description");
                        if (descripcion != null) {
                            descripcionModeloTextView.setText(descripcion);  // Actualizar el campo de descripción en el TextView
                        }

                        Log.d(TAG, "Detalles del modelo cargados correctamente para: " + nombre);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al obtener los detalles del mueble", e);
                    Toast.makeText(getContext(), "Error al obtener los detalles", Toast.LENGTH_SHORT).show();
                });
    }

    // Método para eliminar el mueble seleccionado de Firestore y Firebase Storage
    private void deleteSelectedFurniture() {
        // Primero eliminar la imagen asociada en Firebase Storage si existe
        if (selectedFurnitureImageUrl != null) {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(selectedFurnitureImageUrl);
            imageRef.delete().addOnSuccessListener(aVoid -> {
                // Imagen eliminada, proceder a eliminar el documento en Firestore
                deleteFurnitureFromFirestore();
            }).addOnFailureListener(e -> {
                Log.w(TAG, "Error al eliminar la imagen del mueble", e);
                Toast.makeText(getContext(), "Error al eliminar la imagen del mueble", Toast.LENGTH_SHORT).show();
            });
        } else {
            // Si no hay imagen, eliminar solo de Firestore
            deleteFurnitureFromFirestore();
        }
    }

    // Método para eliminar el mueble de Firestore
    private void deleteFurnitureFromFirestore() {
        dbManager.getFurnitureCollection().document(selectedFurnitureId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mueble eliminado exitosamente", Toast.LENGTH_SHORT).show();
                    // Limpiar la UI después de eliminar
                    autoCompleteModelos.setText("");
                    nombreModeloTextView.setText("Nombre del modelo");
                    descripcionModeloTextView.setText("Descripción breve del modelo");
                    selectedFurnitureId = null;
                    selectedFurnitureImageUrl = null;

                    // Actualizar la lista de modelos después de eliminar
                    loadFurnitureModels();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al eliminar el mueble", e);
                    Toast.makeText(getContext(), "Error al eliminar el mueble", Toast.LENGTH_SHORT).show();
                });
    }
}
