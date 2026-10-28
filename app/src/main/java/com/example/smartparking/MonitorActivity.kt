
package com.example.smartparking

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MonitorActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var listener: ValueEventListener? = null

    private lateinit var txtConexionMonitor: TextView
    private lateinit var txtEstadoMonitor: TextView
    private lateinit var txtEntradaMonitor: TextView
    private lateinit var txtPlazaMonitor: TextView
    private lateinit var txtBarreraMonitor: TextView
    private lateinit var txtSemaforoMonitor: TextView
    private lateinit var txtNocheMonitor: TextView

    private lateinit var luzRojaMonitor: View
    private lateinit var luzAmarillaMonitor: View
    private lateinit var luzVerdeMonitor: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Impedir acceso sin autenticación
        if (FirebaseAuth.getInstance().currentUser == null) {
            volverAlLogin()
            return
        }

        setContentView(R.layout.activity_monitor)

        txtConexionMonitor = findViewById(R.id.txtConexionMonitor)
        txtEstadoMonitor = findViewById(R.id.txtEstadoMonitor)
        txtEntradaMonitor = findViewById(R.id.txtEntradaMonitor)
        txtPlazaMonitor = findViewById(R.id.txtPlazaMonitor)
        txtBarreraMonitor = findViewById(R.id.txtBarreraMonitor)
        txtSemaforoMonitor = findViewById(R.id.txtSemaforoMonitor)
        txtNocheMonitor = findViewById(R.id.txtNocheMonitor)

        luzRojaMonitor = findViewById(R.id.luzRojaMonitor)
        luzAmarillaMonitor = findViewById(R.id.luzAmarillaMonitor)
        luzVerdeMonitor = findViewById(R.id.luzVerdeMonitor)

        val btnCerrarSesion =
            findViewById<Button>(R.id.btnCerrarSesionMonitor)

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            volverAlLogin()
        }

        database = FirebaseDatabase.getInstance()
            .getReference("smartparking")

        escucharFirebase()
    }

    private fun escucharFirebase() {

        listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) {
                    txtConexionMonitor.text =
                        "● No hay datos del estacionamiento"
                    return
                }

                val entrada = snapshot.child("entrada")
                    .getValue(Boolean::class.java) ?: false

                val plazaOcupada = snapshot.child("plazaOcupada")
                    .getValue(Boolean::class.java) ?: false

                val modoNoche = snapshot.child("modoNoche")
                    .getValue(Boolean::class.java) ?: false

                val barreraAbierta = snapshot.child("barreraAbierta")
                    .getValue(Boolean::class.java) ?: false

                val semaforo = snapshot.child("semaforo")
                    .getValue(String::class.java) ?: "VERDE"

                // Mostrar datos recibidos
                txtConexionMonitor.text =
                    "● Datos recibidos de Firebase"

                txtConexionMonitor.setTextColor(
                    0xFF16A34A.toInt()
                )

                txtEntradaMonitor.text = if (entrada) {
                    "Sensor entrada: Vehículo detectado"
                } else {
                    "Sensor entrada: Sin vehículo"
                }

                txtPlazaMonitor.text = if (plazaOcupada) {
                    "Sensor plaza: Ocupada"
                } else {
                    "Sensor plaza: Libre"
                }

                txtBarreraMonitor.text = if (barreraAbierta) {
                    "Barrera: Abierta (90°)"
                } else {
                    "Barrera: Cerrada (0°)"
                }

                txtSemaforoMonitor.text =
                    "Semáforo: $semaforo"

                txtNocheMonitor.text = if (modoNoche) {
                    "Luminosidad: Noche | Luces encendidas"
                } else {
                    "Luminosidad: Día | Luces apagadas"
                }

                // Disponibilidad del estacionamiento
                if (plazaOcupada) {
                    txtEstadoMonitor.text =
                        "ESTACIONAMIENTO OCUPADO"

                    txtEstadoMonitor.setBackgroundColor(
                        0xFFDC2626.toInt()
                    )
                } else {
                    txtEstadoMonitor.text =
                        "ESTACIONAMIENTO DISPONIBLE"

                    txtEstadoMonitor.setBackgroundColor(
                        0xFF16A34A.toInt()
                    )
                }

                actualizarSemaforoVisual(semaforo)
            }

            override fun onCancelled(error: DatabaseError) {
                txtConexionMonitor.text =
                    "● Error de lectura: ${error.message}"

                txtConexionMonitor.setTextColor(
                    0xFFDC2626.toInt()
                )
            }
        }

        database.addValueEventListener(listener!!)
    }

    private fun actualizarSemaforoVisual(color: String) {

        val apagado = 0xFF4B5563.toInt()

        fun cambiarColor(vista: View, nuevoColor: Int) {
            val fondo = (
                    vista.background.constantState
                        ?.newDrawable()?.mutate()
                            as? GradientDrawable
                    ) ?: return

            fondo.setColor(nuevoColor)
            vista.background = fondo
        }

        cambiarColor(
            luzRojaMonitor,
            if (color == "ROJO") 0xFFEF4444.toInt()
            else apagado
        )

        cambiarColor(
            luzAmarillaMonitor,
            if (color == "AMARILLO") 0xFFFBBF24.toInt()
            else apagado
        )

        cambiarColor(
            luzVerdeMonitor,
            if (color == "VERDE") 0xFF22C55E.toInt()
            else apagado
        )
    }

    private fun volverAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        if (::database.isInitialized) {
            listener?.let {
                database.removeEventListener(it)
            }
        }

        super.onDestroy()
    }
}
