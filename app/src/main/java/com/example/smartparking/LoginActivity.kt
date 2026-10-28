
package com.example.smartparking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var edtCorreo: EditText
    private lateinit var edtContrasena: EditText
    private lateinit var btnIngresar: Button
    private lateinit var txtCrearCuenta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        edtCorreo = findViewById(R.id.edtCorreo)
        edtContrasena = findViewById(R.id.edtContrasena)
        btnIngresar = findViewById(R.id.btnIngresar)
        txtCrearCuenta = findViewById(R.id.txtCrearCuenta)

        btnIngresar.setOnClickListener {
            iniciarSesion()
        }

        txtCrearCuenta.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun obtenerCredenciales(): Pair<String, String>? {
        val correo = edtCorreo.text.toString().trim()
        val contrasena = edtContrasena.text.toString()

        if (correo.isEmpty()) {
            edtCorreo.error = "Ingresa tu correo electrónico"
            return null
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            edtCorreo.error = "Ingresa un correo válido"
            return null
        }

        if (contrasena.length < 6) {
            edtContrasena.error =
                "La contraseña debe tener al menos 6 caracteres"
            return null
        }

        return Pair(correo, contrasena)
    }

    private fun iniciarSesion() {
        val credenciales = obtenerCredenciales() ?: return

        btnIngresar.isEnabled = false

        auth.signInWithEmailAndPassword(
            credenciales.first,
            credenciales.second
        ).addOnCompleteListener(this) { tarea ->

            btnIngresar.isEnabled = true

            if (tarea.isSuccessful) {
                Toast.makeText(
                    this,
                    "Inicio de sesión exitoso",
                    Toast.LENGTH_SHORT
                ).show()

                abrirSmartParking()
            } else {
                Toast.makeText(
                    this,
                    "No se pudo iniciar sesión. Revisa tus credenciales.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun registrarUsuario() {
        val credenciales = obtenerCredenciales() ?: return

        btnIngresar.isEnabled = false
        txtCrearCuenta.isEnabled = false

        auth.createUserWithEmailAndPassword(
            credenciales.first,
            credenciales.second
        ).addOnCompleteListener(this) { tarea ->

            btnIngresar.isEnabled = true
            txtCrearCuenta.isEnabled = true

            if (tarea.isSuccessful) {

                // Firebase inicia sesión automáticamente al registrar.
                // La cerramos para exigir un inicio de sesión manual.
                auth.signOut()

                // Mantener el correo para facilitar el ingreso.
                edtCorreo.setText(credenciales.first)

                // Limpiar la contraseña por seguridad.
                edtContrasena.text.clear()

                Toast.makeText(
                    this,
                    "Cuenta creada exitosamente.",
                    Toast.LENGTH_LONG
                ).show()

                edtContrasena.requestFocus()

            } else {
                Toast.makeText(
                    this,
                    "No se pudo crear la cuenta: " +
                            (tarea.exception?.localizedMessage
                                ?: "Error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }



    private fun abrirSmartParking() {
        val intent = Intent(this, ModoActivity::class.java)
        startActivity(intent)
        finish()
    }
}
