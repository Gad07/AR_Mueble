package com.example.ar_muebles;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Obtener datos del Intent
        Intent intent = getIntent();
        boolean isAdmin = intent.getBooleanExtra("isAdmin", false);
        String userEmail = intent.getStringExtra("userEmail");

        // Mostrar un Toast con la información del usuario que ha iniciado sesión
        if (userEmail != null) {
            Toast.makeText(MainActivity.this, "Usuario en sesión: " + userEmail, Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "Usuario en sesión: " + userEmail);
        } else {
            Toast.makeText(MainActivity.this, "Usuario en sesión desconocido", Toast.LENGTH_LONG).show();
            Log.w("MainActivity", "Usuario en sesión desconocido");
        }

        // Inicializar componentes de la UI
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Inicializar la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configurar NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_home);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Configurar AppBarConfiguration para manejar la flecha de navegación
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph())
                    .setOpenableLayout(drawerLayout)
                    .build();

            // Configurar la Toolbar con NavController
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }

        // Configurar el menú según el rol del usuario
        setMenuVisibility(navigationView.getMenu(), isAdmin);
    }

    private void setMenuVisibility(Menu menu, boolean isAdmin) {
        // Asegurarse de que el menú no sea null antes de realizar cambios
        if (menu == null) return;

        // Opciones del menú de administrador
        MenuItem navAgregarAdmin = menu.findItem(R.id.nav_agregarAdmin);
        MenuItem navEditarAdmin = menu.findItem(R.id.nav_editarAdmin);
        MenuItem navEliminarAdmin = menu.findItem(R.id.nav_eliminarAdmin);

        // Opciones generales (disponibles para todos los usuarios)
        MenuItem navRealidadAumentada = menu.findItem(R.id.nav_realidadAumentada);
        MenuItem navAcercaDe = menu.findItem(R.id.nav_acercaDe);

        // Configurar visibilidad del menú según el rol
        if (isAdmin) {
            Toast.makeText(this, "Configurando opciones de Administrador", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "Configurando opciones de administrador");
            if (navAgregarAdmin != null) navAgregarAdmin.setVisible(true);
            if (navEditarAdmin != null) navEditarAdmin.setVisible(true);
            if (navEliminarAdmin != null) navEliminarAdmin.setVisible(true);
        } else {
            Toast.makeText(this, "Configurando opciones de Usuario Regular", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "Configurando opciones de usuario regular");
            if (navAgregarAdmin != null) navAgregarAdmin.setVisible(false);
            if (navEditarAdmin != null) navEditarAdmin.setVisible(false);
            if (navEliminarAdmin != null) navEliminarAdmin.setVisible(false);
        }

        // Opciones disponibles para todos los usuarios
        if (navRealidadAumentada != null) navRealidadAumentada.setVisible(true);
        if (navAcercaDe != null) navAcercaDe.setVisible(true);

        // Asegurarse de que las vistas se actualicen
        navigationView.invalidate();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (navController == null) {
            return false;
        }

        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            navController.navigate(R.id.nav_home);
        } else if (itemId == R.id.nav_realidadAumentada) {
            navController.navigate(R.id.nav_realidadAumentada);
        } else if (itemId == R.id.nav_acercaDe) {
            navController.navigate(R.id.nav_acercaDe);
        } else if (itemId == R.id.nav_agregarAdmin) {
            navController.navigate(R.id.nav_agregarAdmin);
        } else if (itemId == R.id.nav_editarAdmin) {
            navController.navigate(R.id.nav_editarAdmin);
        } else if (itemId == R.id.nav_eliminarAdmin) {
            navController.navigate(R.id.nav_eliminarAdmin);
        } else if (itemId == R.id.nav_editarPerfil) {
            navController.navigate(R.id.nav_editarPerfil);
        } else {
            return false;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
