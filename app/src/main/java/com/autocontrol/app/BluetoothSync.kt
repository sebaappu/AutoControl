package com.autocontrol.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

private val syncUuid: UUID = UUID.fromString("b9b7d7e1-1d8a-4b5b-9d8e-5b3d3b5c8a21")

@Composable
fun BluetoothSyncScreen(json: String, onImport: (String) -> Unit) {
    val context = LocalContext.current
    val adapter = remember { BluetoothAdapter.getDefaultAdapter() }
    var permissionGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 31 ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    }
    var paired by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<BluetoothDevice?>(null) }
    var menu by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Primero vinculá los dos teléfonos desde Ajustes > Bluetooth.") }
    var busy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            paired = try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (_: SecurityException) { emptyList() }
            status = if (paired.isEmpty()) "No hay teléfonos vinculados todavía. Vinculalos desde Ajustes > Bluetooth." else "Elegí el teléfono con el que querés sincronizar."
        } else status = "Hace falta permiso de Bluetooth para conectar los teléfonos."
    }

    fun refreshDevices() {
        if (Build.VERSION.SDK_INT >= 31 && !permissionGranted) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            paired = try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (_: SecurityException) { emptyList() }
            status = if (paired.isEmpty()) "No hay teléfonos vinculados. Vinculalos desde Ajustes > Bluetooth y volvé a actualizar." else "Teléfonos vinculados: ${paired.size}."
        }
    }

    fun runTransfer(receive: Boolean) {
        val target = selected
        if (adapter == null) { status = "Este teléfono no tiene Bluetooth."; return }
        if (!receive && target == null) { status = "Elegí primero el otro teléfono."; return }
        if (!permissionGranted && Build.VERSION.SDK_INT >= 31) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT); return
        }
        busy = true
        status = if (receive) "Esperando conexión del otro teléfono (hasta 60 segundos)…" else "Conectando con el otro teléfono…"
        Thread {
            var socket: BluetoothSocket? = null
            var server: BluetoothServerSocket? = null
            try {
                if (receive) {
                    server = adapter.listenUsingRfcommWithServiceRecord("Mecanica Cachi", syncUuid)
                    socket = server.accept(60000)
                    val input = DataInputStream(socket.inputStream)
                    val size = input.readInt()
                    if (size <= 0 || size > 15 * 1024 * 1024) throw IllegalStateException("El archivo recibido no tiene un tamaño válido.")
                    val bytes = ByteArray(size)
                    input.readFully(bytes)
                    val incoming = String(bytes, StandardCharsets.UTF_8)
                    if (!incoming.trim().startsWith("[")) throw IllegalStateException("Los datos recibidos no son válidos.")
                    Handler(Looper.getMainLooper()).post {
                        onImport(incoming)
                        status = "¡Datos recibidos y combinados correctamente!"
                        busy = false
                    }
                } else {
                    socket = target!!.createRfcommSocketToServiceRecord(syncUuid)
                    socket.connect()
                    val bytes = json.toByteArray(StandardCharsets.UTF_8)
                    val output = DataOutputStream(socket.outputStream)
                    output.writeInt(bytes.size)
                    output.write(bytes)
                    output.flush()
                    Handler(Looper.getMainLooper()).post {
                        status = "¡Datos enviados! En el otro teléfono debe aparecer la confirmación."
                        busy = false
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    status = "No se pudo completar la transferencia: ${e.message ?: "revisá que ambos estén vinculados y que el receptor esté esperando."}"
                    busy = false
                }
            } finally {
                try { socket?.close() } catch (_: Exception) {}
                try { server?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            paired = try { adapter?.bondedDevices?.toList() ?: emptyList() } catch (_: SecurityException) { emptyList() }
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Sincronizar teléfonos", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17212B))
        Text("Pasá clientes, vehículos y todo el historial de mantenimientos entre dos Android. La transferencia es directa por Bluetooth; no usa internet.", color = Color.Gray, fontSize = 13.sp)
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Vincular los teléfonos", fontWeight = FontWeight.Bold)
                Text("En ambos equipos abrí Ajustes > Bluetooth y vinculalos. Después volvé acá.", fontSize = 13.sp, color = Color.Gray)
                OutlinedButton(onClick = { refreshDevices() }, modifier = Modifier.fillMaxWidth()) { Text(if (permissionGranted) "Actualizar teléfonos vinculados" else "Habilitar Bluetooth") }
                if (paired.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selected?.let { try { it.name ?: it.address } catch (_: SecurityException) { "Teléfono seleccionado" } } ?: "Elegir teléfono", modifier = Modifier.weight(1f))
                            Text("⌄")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            paired.forEach { device ->
                                DropdownMenuItem(text = { Text(try { device.name ?: device.address } catch (_: SecurityException) { "Dispositivo Bluetooth" }) }, onClick = { selected = device; menu = false })
                            }
                        }
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2. Transferir información", fontWeight = FontWeight.Bold)
                Text("Para enviar: elegí el otro equipo y tocá Enviar. Para recibir: tocá Recibir primero y después Enviar desde el otro teléfono.", fontSize = 13.sp, color = Color.Gray)
                Button(onClick = { runTransfer(false) }, enabled = !busy && selected != null, modifier = Modifier.fillMaxWidth()) { Text("Enviar mis datos") }
                OutlinedButton(onClick = { runTransfer(true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Recibir y actualizar mis datos") }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        Text(status, fontSize = 13.sp, color = Color(0xFF1769AA))
        Text("La importación combina los vehículos por patente y evita duplicar los mantenimientos ya registrados. No borra la información que ya tenés.", fontSize = 12.sp, color = Color.Gray)
    }
}

internal fun mergeVehicleData(current: List<Vehicle>, incomingJson: String): List<Vehicle> {
    val incoming = loadVehicles(incomingJson)
    val merged = current.associateBy { it.plate.trim().uppercase() }.toMutableMap()
    incoming.forEach { new ->
        val key = new.plate.trim().uppercase()
        val old = merged[key]
        if (old == null) {
            merged[key] = new
        } else {
            val services = (old.maintenances + new.maintenances).distinctBy { it.id }.sortedBy { it.id }
            merged[key] = old.copy(
                client = old.client.ifBlank { new.client },
                phone = old.phone.ifBlank { new.phone },
                make = old.make.ifBlank { new.make },
                model = old.model.ifBlank { new.model },
                year = old.year.ifBlank { new.year },
                km = maxOf(old.km, new.km),
                maintenances = services
            )
        }
    }
    return merged.values.sortedBy { it.client.lowercase() }
}
