package com.example.ar_muebles.db;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBManager {

    private static final String TAG = "DBManager";
    private final FirebaseFirestore db;
    private final CollectionReference usersCollection;
    private final CollectionReference furnitureCollection;
    private final CollectionReference furnitureStatesCollection; // Nueva colección para almacenar los estados de los modelos

    // Callback para manejar respuestas de Firestore (sin datos adicionales)
    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Callback para manejar respuestas de Firestore que incluyen datos
    public interface FirestoreDataCallback {
        void onSuccess(Object data);
        void onFailure(Exception e);
    }

    // Constructor para inicializar Firestore
    public DBManager() {
        db = FirebaseFirestore.getInstance();
        usersCollection = db.collection("users");
        furnitureCollection = db.collection("furniture");
        furnitureStatesCollection = db.collection("furnitureStates"); // Inicializar la nueva colección
    }

    // Métodos públicos para obtener las colecciones
    public CollectionReference getUsersCollection() {
        return usersCollection;
    }

    public CollectionReference getFurnitureCollection() {
        return furnitureCollection;
    }

    // Método para añadir un nuevo mueble a Firestore
    public void addFurniture(String furnitureId, String userId, String name, String description, String modelUrl, FirestoreCallback callback) {
        if (furnitureId == null || userId == null || name == null || description == null || modelUrl == null) {
            String errorMessage = "Parámetros inválidos al intentar agregar un mueble.";
            Log.w(TAG, errorMessage);
            callback.onFailure(new IllegalArgumentException(errorMessage));
            return;
        }

        // Crear el mapa del mueble con los datos
        Map<String, Object> furniture = new HashMap<>();
        furniture.put("furnitureId", furnitureId);
        furniture.put("userId", userId);
        furniture.put("name", name);
        furniture.put("description", description);
        furniture.put("modelUrl", modelUrl);

        // Intentar agregar el mueble a Firestore
        furnitureCollection.document(furnitureId).set(furniture)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mueble agregado correctamente con ID: " + furnitureId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al agregar el mueble: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // Método para obtener todos los muebles de un usuario específico
    public void getFurnitureForUser(String userId, FirestoreDataCallback callback) {
        if (userId == null) {
            String errorMessage = "userId es null. No se puede obtener muebles sin un ID de usuario.";
            Log.w(TAG, errorMessage);
            callback.onFailure(new IllegalArgumentException(errorMessage));
            return;
        }

        // Consulta para obtener los muebles asociados a un usuario específico
        furnitureCollection.whereEqualTo("userId", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        String errorMessage = "No se encontraron muebles para el usuario con ID: " + userId;
                        Log.w(TAG, errorMessage);
                        callback.onFailure(new Exception(errorMessage));
                    } else {
                        Log.d(TAG, "Muebles obtenidos para el usuario con ID: " + userId);
                        callback.onSuccess(queryDocumentSnapshots.getDocuments());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al obtener los muebles: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // Método para guardar los estados de los modelos AR en Firestore
    public void saveModelStates(String userId, List<Map<String, Object>> modelStates) {
        if (userId == null) {
            Log.w(TAG, "saveModelStates: userId es null. No se pueden guardar los estados sin un ID de usuario válido.");
            return;
        }

        // Crear un mapa para los datos que se van a guardar en Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("modelStates", modelStates);

        // Guardar el estado con un documento único asociado al userId
        furnitureStatesCollection.document(userId).set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Estados de los modelos guardados correctamente para el usuario con ID: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Error al guardar los estados de los modelos: " + e.getMessage()));
    }

    // Método para recuperar los estados de los modelos AR almacenados en Firestore para un usuario
    public void getSavedModelStates(String userId, FirestoreDataCallback callback) {
        if (userId == null) {
            String errorMessage = "getSavedModelStates: userId es null. No se puede obtener los estados de los modelos sin un ID de usuario.";
            Log.w(TAG, errorMessage);
            callback.onFailure(new IllegalArgumentException(errorMessage));
            return;
        }

        // Obtener el documento de estados de modelos asociado al userId
        furnitureStatesCollection.document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Extraer el campo "modelStates" del documento y devolverlo en el callback
                        List<Map<String, Object>> modelStates = (List<Map<String, Object>>) documentSnapshot.get("modelStates");
                        Log.d(TAG, "Estados de los modelos obtenidos correctamente para el usuario con ID: " + userId);
                        callback.onSuccess(modelStates);
                    } else {
                        String errorMessage = "No se encontraron estados de los modelos para el usuario con ID: " + userId;
                        Log.w(TAG, errorMessage);
                        callback.onFailure(new Exception(errorMessage));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al obtener los estados de los modelos: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // Método para actualizar un mueble existente en Firestore
    public void updateFurniture(String furnitureId, String name, String description, String modelUrl, FirestoreCallback callback) {
        if (furnitureId == null) {
            String errorMessage = "furnitureId es null. No se puede actualizar un mueble sin un ID válido.";
            Log.w(TAG, errorMessage);
            callback.onFailure(new IllegalArgumentException(errorMessage));
            return;
        }

        DocumentReference furnitureRef = furnitureCollection.document(furnitureId);
        Map<String, Object> updates = new HashMap<>();

        // Añadir campos que se van a actualizar
        if (name != null) updates.put("name", name);
        if (description != null) updates.put("description", description);
        if (modelUrl != null) updates.put("modelUrl", modelUrl);

        if (updates.isEmpty()) {
            String errorMessage = "No hay datos para actualizar para el mueble con ID: " + furnitureId;
            Log.w(TAG, errorMessage);
            callback.onFailure(new IllegalArgumentException(errorMessage));
            return;
        }

        // Intentar actualizar el mueble en Firestore
        furnitureRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mueble actualizado correctamente con ID: " + furnitureId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al actualizar el mueble: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // Método para eliminar un mueble de Firestore
    public void deleteFurniture(String furnitureId, FirestoreCallback callback) {
        if (furnitureId == null) {
            String errorMessage = "furnitureId es null. No se puede eliminar un mueble sin un ID válido.";
            Log.w(TAG, errorMessage);
            callback.onFailure(new IllegalArgumentException(errorMessage));
            return;
        }

        // Intentar eliminar el mueble de Firestore
        furnitureCollection.document(furnitureId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mueble eliminado correctamente con ID: " + furnitureId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al eliminar el mueble: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // Métodos para el manejo de usuarios (restaurados a la versión que ya funcionaba)
    public void addUser(String userId, String name, String email, boolean isAdmin, String password, FirestoreCallback callback) {
        if (userId == null || name == null || email == null || password == null) {
            Log.w(TAG, "addUser: Parámetros inválidos.");
            callback.onFailure(new IllegalArgumentException("Parámetros inválidos."));
            return;
        }

        // Crear un mapa con los datos del usuario
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("name", name);
        user.put("email", email);
        user.put("isAdmin", isAdmin);
        user.put("password", password);

        // Intentar agregar el usuario a la colección "users"
        usersCollection.document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario agregado correctamente con ID: " + userId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al agregar el usuario", e);
                    callback.onFailure(e);
                });
    }
}
