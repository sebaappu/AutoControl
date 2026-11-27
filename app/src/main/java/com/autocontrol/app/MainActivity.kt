package com.autocontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF17212B)
private val Blue = Color(0xFF1769AA)
private val Bg = Color(0xFFF3F6F9)

data class Vehicle(val id: Long, val client: String, val plate: String, val model: String, val km: Int, val dueKm: Int, val dueDate: String) {
    val remaining get() = dueKm - km
    val status get() = when { remaining <= 0 -> "VENCIDO"; remaining <= 1000 -> "PRÓXIMO"; else -> "AL DÍA" }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences("autocontrol", MODE_PRIVATE)
            var vehicles by remember {
                mutableStateOf(loadVehicles(prefs.getString("vehicles", null)))
            }
            fun save(list: List<Vehicle>) {
                vehicles = list
                prefs.edit().putString("vehicles", JSONArray().apply {
                    list.forEach { v -> put(JSONObject().apply {
                        put("id",v.id); put("client",v.client); put("plate",v.plate); put("model",v.model)
                        put("km",v.km); put("dueKm",v.dueKm); put("dueDate",v.dueDate)
                    }) }
                }.toString()).apply()
            }
            AutoControlApp(vehicles, ::save)
        }
    }
}

private fun loadVehicles(raw: String?): List<Vehicle> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val a = JSONArray(raw)
        (0 until a.length()).map { i -> a.getJSONObject(i).let {
            Vehicle(it.getLong("id"),it.getString("client"),it.getString("plate"),it.getString("model"),
                it.getInt("km"),it.getInt("dueKm"),it.optString("dueDate"))
        } }
    } catch (_: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoControlApp(vehicles: List<Vehicle>, save: (List<Vehicle>) -> Unit) {
    var tab by remember { mutableStateOf("Inicio") }
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Vehicle?>(null) }
    val due = vehicles.count { it.remaining <= 1000 }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Bg, surface = Color.White)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column { Text("AUTOCONTROL", fontWeight = FontWeight.Black, letterSpacing = 2.sp); Text("GESTIÓN DE TALLER", fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.5.sp) } },
                    actions = { Text("●", color = Color(0xFF21A179), modifier = Modifier.padding(end = 8.dp)); Text("LOCAL", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(end = 16.dp)) }
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf("Inicio","Vehículos").forEach { item ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item; selected = null }, icon = { Text(if (item=="Inicio") "⌂" else "▣", fontSize = 20.sp) }, label = { Text(item) })
                    }
                }
            },
            floatingActionButton = { if (tab == "Vehículos" && selected == null) FloatingActionButton(onClick = { showAdd = true }, containerColor = Blue, contentColor = Color.White) { Text("+", fontSize = 28.sp) } }
        ) { padding ->
            Column(Modifier.fillMaxSize().background(Bg).padding(padding).padding(16.dp)) {
                if (selected != null) {
                    VehicleDetail(selected!!, onBack = { selected = null }, onUpdate = { updated ->
                        save(vehicles.map { if (it.id == updated.id) updated else it }); selected = updated
                    })
                } else if (tab == "Inicio") {
                    Text("Panel general", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("El estado de tu taller, de un vistazo.", color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("VEHÍCULOS", vehicles.size.toString(), Modifier.weight(1f))
                        StatCard("A REVISAR", due.toString(), Modifier.weight(1f), if (due > 0) Color(0xFFFFF1D6) else Color.White)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Atención prioritaria", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                    val urgent = vehicles.filter { it.remaining <= 1000 }.sortedBy { it.remaining }
                    if (urgent.isEmpty()) EmptyState("Todo en orden", "Los vehículos próximos a mantenimiento aparecerán acá.")
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(urgent) { VehicleCard(it) { selected = it } } }
                } else {
                    Text("Mis vehículos", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Clientes y mantenimiento preventivo", color = Color.Gray)
                    Spacer(Modifier.height(14.dp))
                    if (vehicles.isEmpty()) EmptyState("Todavía no hay vehículos", "Tocá + para cargar el primer cliente y su auto.")
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(vehicles.sortedBy { it.client.lowercase() }) { VehicleCard(it) { selected = it } } }
                }
            }
        }
        if (showAdd) AddVehicleDialog(onDismiss = { showAdd = false }, onSave = { save(vehicles + it); showAdd = false })
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(18.dp)) { Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold); Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink) }
    }
}

@Composable
fun VehicleCard(v: Vehicle, onClick: () -> Unit) {
    val color = when(v.status) { "VENCIDO" -> Color(0xFFB3261E); "PRÓXIMO" -> Color(0xFFB26A00); else -> Color(0xFF16845B) }
    Card(Modifier.fillMaxWidth().clickable(onClick), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(v.client, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink); Text(v.model, color = Color.Gray, fontSize = 13.sp) }
                Text(v.status, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.background(color.copy(alpha=.10f), RoundedCornerShape(20.dp)).padding(horizontal=10.dp, vertical=6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row { Text(v.plate.uppercase(), fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f)); Text("${v.km} km", color = Color.Gray) }
            Spacer(Modifier.height(5.dp))
            Text(if (v.remaining <= 0) "Cambio de correa vencido por ${-v.remaining} km" else "Próximo cambio en ${v.remaining} km", color = color, fontSize = 13.sp)
            if (v.dueDate.isNotBlank()) Text("Fecha objetivo: ${v.dueDate}", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String) {
    Box(Modifier.fillMaxWidth().padding(top=38.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▣", fontSize = 34.sp, color = Blue); Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Ink); Text(message, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    var client by remember { mutableStateOf("") }; var plate by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }; var km by remember { mutableStateOf("") }
    var dueKm by remember { mutableStateOf("") }; var date by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onDismiss, title={ Text("Nuevo vehículo", fontWeight=FontWeight.Bold) },
        text={ Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(client,{client=it},label={Text("Nombre del cliente")},singleLine=true)
            OutlinedTextField(plate,{plate=it},label={Text("Patente")},singleLine=true)
            OutlinedTextField(model,{model=it},label={Text("Marca y modelo")},singleLine=true)
            OutlinedTextField(km,{km=it},label={Text("Kilometraje actual")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(dueKm,{dueKm=it},label={Text("Km del próximo cambio")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(date,{date=it},label={Text("Fecha objetivo (dd/mm/aaaa)")},singleLine=true)
            if(error) Text("Completá cliente, patente y kilometrajes válidos.",color=Color.Red,fontSize=12.sp)
        } },
        confirmButton={ TextButton(onClick={
            val a=km.toIntOrNull(); val b=dueKm.toIntOrNull()
            if(client.isBlank()||plate.isBlank()||a==null||b==null||a<0||b<0) error=true
            else onSave(Vehicle(System.currentTimeMillis(),client.trim(),plate.trim().uppercase(),model.trim(),a,b,date.trim()))
        }) { Text("Guardar") } },
        dismissButton={ TextButton(onClick=onDismiss){Text("Cancelar")} })
}

@Composable
fun VehicleDetail(v: Vehicle, onBack: () -> Unit, onUpdate: (Vehicle) -> Unit) {
    var km by remember(v.id) { mutableStateOf(v.km.toString()) }
    TextButton(onClick=onBack) { Text("← Volver") }
    Text(v.plate, fontSize=28.sp, fontWeight=FontWeight.Black, color=Ink)
    Text(v.client + " · " + v.model, color=Color.Gray)
    Spacer(Modifier.height(18.dp))
    VehicleCard(v) {}
    Spacer(Modifier.height(20.dp))
    Text("Actualizar kilometraje", fontWeight=FontWeight.Bold, fontSize=18.sp, color=Ink)
    OutlinedTextField(km,{km=it},label={Text("Kilometraje actual")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
    Spacer(Modifier.height(8.dp))
    Button(onClick={ km.toIntOrNull()?.takeIf { it >= 0 }?.let { onUpdate(v.copy(km=it)) } }) { Text("Guardar kilometraje") }
}
