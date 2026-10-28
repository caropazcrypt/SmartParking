package com.example.smartparking

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Vincular vistas de la interfaz
        edtCorreo = findViewById(R.id.edtCorreo)
        edtContrasena = findViewById(R.id.edtContrasena)
        btnIngresar = findViewById(R.id.btnIngresar)
        txtCrearCuenta = findViewById(R.id.txtCrearCuenta)

        // Botón Iniciar Sesión
        btnIngresar.setOnClickListener {
            iniciarSesion()
        }

        txtCrearCuenta.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun iniciarSesion() {
        val correo = edtCorreo.text.toString().trim()
        val contrasena = edtContrasena.text.toString().trim()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Formato de correo no válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Autenticación en la nube mediante canal seguro
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ModoActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Error de acceso: Credenciales inválidas o no autorizadas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun registrarUsuario() {
        val correo = edtCorreo.text.toString().trim()
        val contrasena = edtContrasena.text.toString().trim()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Ingresa un correo y contraseña para registrarte", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Ingresa un formato de correo válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (!esContrasenaRobusta(contrasena)) {
            Toast.makeText(
                this,
                "Seguridad ISO 27400: La clave requiere al menos 8 caracteres, mayúscula, minúscula, número y un símbolo especial (@#$%^&+=!._)",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Usuario registrado con éxito en Firebase", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ModoActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Error al registrar: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun esContrasenaRobusta(password: String): Boolean {
        val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._])(?=\\S+$).{8,}$"
        return password.matches(passwordRegex.toRegex())
    }
}