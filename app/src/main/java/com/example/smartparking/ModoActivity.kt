
package com.example.smartparking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ModoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var btnControlador: Button
    private lateinit var btnMonitor: Button
    private lateinit var btnCerrarSesion: Button

    private var esAdministrador = false
    private var rolVerificado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            volverAlLogin()
            return
        }

        setContentView(R.layout.activity_modo)

        btnControlador = findViewById(R.id.btnControlador)
        btnMonitor = findViewById(R.id.btnMonitor)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesionModo)

        // Bloquear el controlador hasta verificar el rol
        btnControlador.isEnabled = false

        consultarRolUsuario()

        btnControlador.setOnClickListener {
            if (rolVerificado && esAdministrador) {
                val intent = Intent(
                    this,
                    MainActivity::class.java
                )
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "Acceso exclusivo para administradores",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnMonitor.setOnClickListener {
            val intent = Intent(
                this,
                MonitorActivity::class.java
            )
            startActivity(intent)
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            volverAlLogin()
        }
    }

    private fun consultarRolUsuario() {

        val uid = auth.currentUser?.uid ?: return

        val referencia = FirebaseDatabase
            .getInstance()
            .getReference("usuarios")
            .child(uid)
            .child("rol")

        referencia.get()
            .addOnSuccessListener { snapshot ->

                val rol = snapshot.getValue(String::class.java)

                esAdministrador = rol == "admin"
                rolVerificado = true

                btnControlador.isEnabled = esAdministrador

                if (esAdministrador) {
                    Toast.makeText(
                        this,
                        "Bienvenido, administrador",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Acceso de solo monitoreo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {

                esAdministrador = false
                rolVerificado = false
                btnControlador.isEnabled = false

                Toast.makeText(
                    this,
                    "No se pudo verificar el rol. Controlador bloqueado.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun volverAlLogin() {
        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
