package com.example.ar_muebles.ui.RA

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ar_muebles.R
import com.example.ar_muebles.db.DBManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot

class RAFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var autoCompleteModelos: AutoCompleteTextView
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var unanchorButton: ExtendedFloatingActionButton
    private lateinit var rotateLeftButton: MaterialButton
    private lateinit var rotateRightButton: MaterialButton
    private val dbManager = DBManager()

    private val nodeToModelUrlMap = mutableMapOf<ArModelNode, String>() // Mapa para rastrear la URL de cada modelo
    private val modelNodes = mutableListOf<ArModelNode>() // Lista de nodos de modelos para múltiples modelos
    private var selectedFurnitureModelUrl: String? = null
    private val userId = "test_user" // Reemplazar con el ID del usuario real.

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_principal, container, false)

        // Inicializar vistas y botones
        sceneView = view.findViewById(R.id.sceneView1)
        autoCompleteModelos = view.findViewById(R.id.autoCompleteModelos)
        placeButton = view.findViewById(R.id.place)
        unanchorButton = view.findViewById(R.id.unanchor)
        rotateLeftButton = view.findViewById(R.id.btnRotateLeft)
        rotateRightButton = view.findViewById(R.id.btnRotateRight)

        // Configuración inicial de la vista AR
        configureSceneView(sceneView)

        // Cargar modelos desde Firestore para mostrar en el AutoCompleteTextView
        loadFurnitureModels()

        // Cargar modelos previamente colocados
        loadSavedModelStates()

        // Configurar el evento de selección de modelos
        autoCompleteModelos.setOnItemClickListener { parent, _, position, _ ->
            val nombreModelo = parent.getItemAtPosition(position) as String
            fetchFurnitureDetails(nombreModelo)
        }

        // Mostrar siempre la lista cuando se haga clic en el campo
        autoCompleteModelos.setOnClickListener {
            autoCompleteModelos.showDropDown()
        }

        // Configurar los botones de rotación
        rotateLeftButton.setOnClickListener {
            rotateModel(-15f) // Rotar 15 grados hacia la izquierda
        }

        rotateRightButton.setOnClickListener {
            rotateModel(15f) // Rotar 15 grados hacia la derecha
        }

        // Configurar el evento para colocar el modelo en la escena AR
        placeButton.setOnClickListener {
            placeModel()
        }

        // Configurar el botón para desanclar el modelo
        unanchorButton.setOnClickListener {
            unanchorModel()
        }

        return view
    }

    // Método para configurar la escena AR (configuración simplificada)
    private fun configureSceneView(sceneView: ArSceneView) {
        // Configuración simplificada de la escena AR
    }

    // Cargar todos los modelos de muebles desde Firestore
    private fun loadFurnitureModels() {
        dbManager.getFurnitureCollection().get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (queryDocumentSnapshots.isEmpty) {
                    Log.w("RAFragment", "No se encontraron modelos para mostrar.")
                    Toast.makeText(context, "No hay modelos disponibles.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val listaModelos = ArrayList<String>()
                for (doc in queryDocumentSnapshots) {
                    val nombreModelo = doc.getString("name")
                    if (nombreModelo != null) {
                        listaModelos.add(nombreModelo)
                    }
                }

                if (listaModelos.isEmpty()) {
                    Log.w("RAFragment", "La lista de modelos está vacía después de procesar los documentos.")
                    Toast.makeText(context, "No hay modelos disponibles.", Toast.LENGTH_SHORT).show()
                } else {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listaModelos)
                    autoCompleteModelos.setAdapter(adapter)
                    autoCompleteModelos.showDropDown() // Mostrar el menú desplegable inicialmente si es necesario
                }
            }
            .addOnFailureListener { e ->
                Log.w("RAFragment", "Error al cargar los modelos de muebles", e)
                Toast.makeText(context, "Error al cargar los modelos", Toast.LENGTH_SHORT).show()
            }
    }

    // Obtener los detalles del mueble seleccionado y cargar el modelo automáticamente
    private fun fetchFurnitureDetails(nombreModelo: String) {
        dbManager.getFurnitureCollection().whereEqualTo("name", nombreModelo).get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty) {
                    val document = queryDocumentSnapshots.documents[0]
                    selectedFurnitureModelUrl = document.getString("modelUrl")

                    Log.d("RAFragment", "Detalles del modelo cargados para: $nombreModelo")
                    Toast.makeText(requireContext(), "Detalles del modelo cargados", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("RAFragment", "No se encontró ningún documento con el nombre: $nombreModelo")
                    Toast.makeText(requireContext(), "No se encontró el modelo seleccionado", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w("RAFragment", "Error al obtener los detalles del mueble", e)
                Toast.makeText(requireContext(), "Error al obtener los detalles", Toast.LENGTH_SHORT).show()
            }
    }

    // Método para cargar un modelo en la escena AR desde una URL
    private fun loadModelIntoScene(glbFileLocation: String, position: Position = Position(), rotation: Rotation = Rotation()) {
        val modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = glbFileLocation,
                scaleToUnits = 1f,
                centerOrigin = Position(-0.5f)
            ) {
                // Asegúrate de que el anclaje sea visible después de la carga del modelo
                sceneView.planeRenderer.isVisible = true
                Log.d("RAFragment", "Modelo cargado desde $glbFileLocation")
            }
            this.position = position
            this.rotation = rotation
        }

        // Añadir el nodo del modelo a la escena y registrar su URL
        sceneView.addChild(modelNode)
        modelNodes.add(modelNode)

        // Anclar el modelo inmediatamente después de agregarlo a la escena
        modelNode.anchor()
    }

    // Método para anclar el modelo en su posición actual
    private fun placeModel() {
        selectedFurnitureModelUrl?.let { url ->
            loadModelIntoScene(url)
            saveModelStateToDB() // Guardar el estado del modelo en Firestore
            Toast.makeText(requireContext(), "Modelo colocado en su posición", Toast.LENGTH_SHORT).show()
        } ?: showToast("Por favor selecciona un modelo primero.")
    }

    // Método para desanclar todos los modelos
    private fun unanchorModel() {
        modelNodes.forEach { it.detachAnchor() }
        Toast.makeText(requireContext(), "Todos los modelos desanclados", Toast.LENGTH_SHORT).show()
    }

    // Método para rotar el modelo en la escena
    private fun rotateModel(degrees: Float) {
        modelNodes.lastOrNull()?.let { node ->
            val currentRotation = node.rotation
            val newRotation = Rotation(
                currentRotation.x,
                currentRotation.y + degrees,
                currentRotation.z
            )
            node.rotation = newRotation
        }
    }

    // Método para cargar modelos previamente colocados al iniciar el fragmento (actualizado)
    private fun loadSavedModelStates() {
        dbManager.getSavedModelStates(userId, object : DBManager.FirestoreDataCallback {
            override fun onSuccess(data: Any?) {
                if (data is List<*>) {
                    data.forEach { modelState ->
                        if (modelState is Map<*, *>) {
                            val modelUrl = modelState["modelUrl"] as? String ?: return@forEach

                            // Obtener la posición y rotación del modelo desde el estado guardado
                            val positionData = modelState["position"] as? Map<String, Double> ?: return@forEach
                            val position = Position(
                                positionData["x"]?.toFloat() ?: 0f,
                                positionData["y"]?.toFloat() ?: 0f,
                                positionData["z"]?.toFloat() ?: 0f
                            )

                            val rotationData = modelState["rotation"] as? Map<String, Double> ?: return@forEach
                            val rotation = Rotation(
                                rotationData["x"]?.toFloat() ?: 0f,
                                rotationData["y"]?.toFloat() ?: 0f,
                                rotationData["z"]?.toFloat() ?: 0f
                            )

                            // Cargar el modelo en la escena con la posición y rotación obtenidas
                            loadModelIntoScene(modelUrl, position, rotation)
                        }
                    }
                } else {
                    Log.w("RAFragment", "Los datos obtenidos no son del tipo esperado List<Map>")
                }
            }

            override fun onFailure(e: Exception) {
                Log.w("RAFragment", "Error al cargar el estado de los modelos", e)
            }
        })
    }


    private fun saveModelStateToDB() {
        val modelStates = mutableListOf<Map<String, Any>>()  // Lista para almacenar los estados de los modelos

        modelNodes.forEach { node ->
            if (node.isAnchored) {
                val position = node.worldPosition
                val rotation = node.worldRotation
                val modelUrl = nodeToModelUrlMap[node] // Obtener la URL del mapa

                if (modelUrl != null) {
                    // Crear un mapa que represente el estado del modelo
                    val modelState = hashMapOf(
                        "modelUrl" to modelUrl,
                        "position" to mapOf(
                            "x" to position.x,
                            "y" to position.y,
                            "z" to position.z
                        ),
                        "rotation" to mapOf(
                            "x" to rotation.x,
                            "y" to rotation.y,
                            "z" to rotation.z
                        ),
                        "timestamp" to System.currentTimeMillis()  // Guarda el tiempo actual para registrar el estado
                    )

                    modelStates.add(modelState)  // Añadir el estado del modelo a la lista
                }
            }
        }

        // Guardar todos los estados en Firestore usando DBManager
        dbManager.saveModelStates(userId, modelStates)
    }



    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        saveModelStateToDB() // Guardar los estados de los modelos cuando se pausa el fragmento
    }
}
