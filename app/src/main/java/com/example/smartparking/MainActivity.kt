package com.example.smartparking

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.DecelerateInterpolator
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var txtEstado: TextView
    private lateinit var txtEntrada: TextView
    private lateinit var txtEstacionamiento: TextView
    private lateinit var txtBarrera: TextView
    private lateinit var txtSemaforo: TextView
    private lateinit var txtLuz: TextView
    private lateinit var txtIluminacion: TextView

    private lateinit var switchEntrada: Switch
    private lateinit var switchEstacionamiento: Switch
    private lateinit var switchNoche: Switch

    private lateinit var luzRoja: View
    private lateinit var luzAmarilla: View
    private lateinit var luzVerde: View

    private lateinit var brazoBarrera: View
    private lateinit var txtEstadoBarreraVisual: TextView

    private var barreraVisualAbierta = false

    private val handler = Handler(Looper.getMainLooper())

    private var barreraAbierta = false
    private var esperandoEstacionamiento = false
    private var cierrePendiente = false
    private var entradaAnterior = false
    private var plazaAnterior = false

    private lateinit var database: DatabaseReference

    private val cerrarBarrera = Runnable {
        cierrePendiente = false

        if (!switchEntrada.isChecked) {
            barreraAbierta = false
            esperandoEstacionamiento =
                !switchEstacionamiento.isChecked

            actualizarSistema()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprobar si existe una sesión autenticada
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance()
            .getReference("usuarios")
            .child(uid)
            .child("rol")
            .get()
            .addOnSuccessListener { snapshot ->

                val rol = snapshot.getValue(String::class.java)

                if (rol == "admin") {
                    // Aquí debe comenzar la inicialización del controlador.
                } else {
                    Toast.makeText(
                        this,
                        "Acceso denegado: solo administradores",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo verificar el permiso",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }

        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance()
            .getReference("smartparking")

        txtEstado = findViewById(R.id.txtEstado)
        txtEntrada = findViewById(R.id.txtEntrada)
        txtEstacionamiento =
            findViewById(R.id.txtEstacionamiento)
        txtBarrera = findViewById(R.id.txtBarrera)
        txtSemaforo = findViewById(R.id.txtSemaforo)
        txtLuz = findViewById(R.id.txtLuz)
        txtIluminacion = findViewById(R.id.txtIluminacion)

        switchEntrada = findViewById(R.id.switchEntrada)
        switchEstacionamiento =
            findViewById(R.id.switchEstacionamiento)
        switchNoche = findViewById(R.id.switchNoche)

        luzRoja = findViewById(R.id.luzRoja)
        luzAmarilla = findViewById(R.id.luzAmarilla)
        luzVerde = findViewById(R.id.luzVerde)

        brazoBarrera = findViewById(R.id.brazoBarrera)

        txtEstadoBarreraVisual =
            findViewById(R.id.txtEstadoBarreraVisual)

        val btnReiniciar =
            findViewById<Button>(R.id.btnReiniciar)

        switchEntrada.setOnCheckedChangeListener { _, _ ->
            procesarSensores()
        }

        switchEstacionamiento.setOnCheckedChangeListener { _, _ ->
            procesarSensores()
        }

        switchNoche.setOnCheckedChangeListener { _, _ ->
            actualizarSistema()
        }

        btnReiniciar.setOnClickListener {
            cancelarCierre()

            barreraAbierta = false
            esperandoEstacionamiento = false

            switchEntrada.isChecked = false
            switchEstacionamiento.isChecked = false
            switchNoche.isChecked = false

            entradaAnterior = false
            plazaAnterior = false

            actualizarSistema()
        }


        val btnCerrarSesion =
            findViewById<Button>(R.id.btnCerrarSesion)

        btnCerrarSesion.setOnClickListener {

            // Cerrar sesión en Firebase
            FirebaseAuth.getInstance().signOut()

            // Regresar al inicio de sesión
            val intent = Intent(this, LoginActivity::class.java)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }


        actualizarSistema()
    }


    private fun guardarEstadoFirebase() {

        val entrada = switchEntrada.isChecked
        val plazaOcupada = switchEstacionamiento.isChecked
        val modoNoche = switchNoche.isChecked

        val semaforo = when {
            plazaOcupada -> "ROJO"
            esperandoEstacionamiento -> "AMARILLO"
            else -> "VERDE"
        }

        val estado = mapOf(
            "entrada" to entrada,
            "plazaOcupada" to plazaOcupada,
            "modoNoche" to modoNoche,
            "barreraAbierta" to barreraAbierta,
            "semaforo" to semaforo
        )

        database.setValue(estado)
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "SmartParking",
                    "Error al guardar en Firebase",
                    error
                )
            }
    }


    private fun cancelarCierre() {
        handler.removeCallbacks(cerrarBarrera)
        cierrePendiente = false
    }

    private fun procesarSensores() {
        val entrada = switchEntrada.isChecked
        val plazaOcupada = switchEstacionamiento.isChecked

        // Detectar cuando el vehículo sale de la plaza
        if (plazaAnterior && !plazaOcupada) {
            cancelarCierre()
            barreraAbierta = false
            esperandoEstacionamiento = false
        }

        // Si la plaza está ocupada, bloquear nuevos ingresos
        if (plazaOcupada) {
            cancelarCierre()
            barreraAbierta = false
            esperandoEstacionamiento = false
        } else if (entrada && !entradaAnterior) {

            // Vehículo detectado en la entrada
            cancelarCierre()
            barreraAbierta = true
            esperandoEstacionamiento = false

        } else if (!entrada && entradaAnterior &&
            barreraAbierta) {

            // El sensor dejó de detectar el vehículo
            cancelarCierre()
            cierrePendiente = true

            handler.postDelayed(cerrarBarrera, 3000)
        }

        entradaAnterior = entrada
        plazaAnterior = plazaOcupada

        actualizarSistema()
    }

    private fun actualizarSistema() {
        val entrada = switchEntrada.isChecked
        val plazaOcupada = switchEstacionamiento.isChecked
        val noche = switchNoche.isChecked

        txtEntrada.text = if (entrada) {
            "Sensor entrada: Vehículo detectado"
        } else {
            "Sensor entrada: Sin vehículo"
        }

        txtEstacionamiento.text = if (plazaOcupada) {
            "Sensor plaza: Ocupada"
        } else {
            "Sensor plaza: Libre"
        }

        if (plazaOcupada) {
            txtEstado.text = "ESTACIONAMIENTO OCUPADO"
            txtEstado.setBackgroundColor(0xFFDC2626.toInt())
        } else {
            txtEstado.text = "ESTACIONAMIENTO DISPONIBLE"
            txtEstado.setBackgroundColor(0xFF16A34A.toInt())
        }

        txtBarrera.text = when {
            barreraAbierta && cierrePendiente ->
                "Barrera: Abierta (cierre en 3 segundos)"

            barreraAbierta ->
                "Barrera: Abierta (90°)"

            else ->
                "Barrera: Cerrada (0°)"
        }
        actualizarBarreraVisual(barreraAbierta)

        val colorSemaforo = when {
            plazaOcupada -> "ROJO"
            esperandoEstacionamiento -> "AMARILLO"
            else -> "VERDE"
        }

        txtSemaforo.text = "Semáforo: $colorSemaforo"
        actualizarSemaforo(colorSemaforo)

        txtLuz.text = if (noche) {
            "Luminosidad: Noche"
        } else {
            "Luminosidad: Día"
        }

        txtIluminacion.text = if (noche) {
            "LED entrada: Encendido\n" +
                    "LED estacionamiento: Encendido"
        } else {
            "LED entrada: Apagado\n" +
                    "LED estacionamiento: Apagado"
        }

        guardarEstadoFirebase()

    }


    private fun actualizarBarreraVisual(abierta: Boolean) {

        // Evita repetir la animación si el estado no cambió
        if (barreraVisualAbierta == abierta) {
            return
        }

        barreraVisualAbierta = abierta

        // Punto desde el cual gira el brazo
        brazoBarrera.pivotX = 0f
        brazoBarrera.pivotY = brazoBarrera.height / 2f

        // Ángulo de la animación
        val angulo = if (abierta) -90f else 0f

        brazoBarrera.animate()
            .rotation(angulo)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        txtEstadoBarreraVisual.text = if (abierta) {
            "Barrera abierta (90°)"
        } else {
            "Barrera cerrada (0°)"
        }
    }


    private fun actualizarSemaforo(color: String) {
        val apagado = 0xFF4B5563.toInt()

        fun cambiarColor(vista: View, nuevoColor: Int) {
            val fondo = (vista.background.constantState
                ?.newDrawable()?.mutate() as? GradientDrawable)
                ?: return

            fondo.setColor(nuevoColor)
            vista.background = fondo
        }

        cambiarColor(
            luzRoja,
            if (color == "ROJO") 0xFFEF4444.toInt()
            else apagado
        )

        cambiarColor(
            luzAmarilla,
            if (color == "AMARILLO") 0xFFFBBF24.toInt()
            else apagado
        )

        cambiarColor(
            luzVerde,
            if (color == "VERDE") 0xFF22C55E.toInt()
            else apagado
        )
    }

    override fun onDestroy() {
        cancelarCierre()
        super.onDestroy()
    }
}
