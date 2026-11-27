package com.autocontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private val maintenanceTypes = listOf(
    "Cambio de aceite", "Filtro de aceite", "Filtro de aire", "Filtro de habitáculo",
    "Filtro de combustible", "Distribución (correa/cadena)", "Bomba de agua",
    "Tensor y poleas", "Embrague", "Frenos delanteros", "Frenos traseros",
    "Pastillas de freno", "Discos de freno", "Líquido de frenos",
    "Neumáticos", "Alineación y balanceo", "Amortiguadores", "Tren delantero",
    "Bujías", "Bobinas de encendido", "Batería", "Refrigerante",
    "Limpieza de radiador", "Correa de accesorios", "Caja de cambios",
    "Diferencial", "Dirección hidráulica", "Aire acondicionado",
    "Escaneo electrónico", "Service general", "Otro"
)

data class Maintenance(
    val id: Long, val type: String, val doneKm: Int, val doneDate: String,
    val dueKm: Int, val dueDate: String, val notes: String, val cost: String
) {
    fun remainingAt(currentKm: Int): Int = if (dueKm > 0) dueKm - currentKm else Int.MAX_VALUE
    fun statusAt(currentKm: Int): String = when {
        dueKm > 0 && remainingAt(currentKm) <= 0 -> "VENCIDO"
        dueKm > 0 && remainingAt(currentKm) <= 1000 -> "PRÓXIMO"
        else -> "AL DÍA"
    }
}

data class Vehicle(
    val id: Long, val client: String, val phone: String, val plate: String,
    val make: String, val model: String, val year: String, val km: Int,
    val maintenances: List<Maintenance> = emptyList()
) {
    val displayModel: String get() = listOf(make, model, year).filter { it.isNotBlank() }.joinToString(" ")
    val urgentCount: Int get() = maintenances.count { it.statusAt(km) != "AL DÍA" }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences("autocontrol", MODE_PRIVATE)
            var vehicles by remember { mutableStateOf(loadVehicles(prefs.getString("vehicles", null))) }
            fun save(list: List<Vehicle>) {
                vehicles = list
                prefs.edit().putString("vehicles", JSONArray().apply {
                    list.forEach { v -> put(JSONObject().apply {
                        put("id", v.id); put("client", v.client); put("phone", v.phone)
                        put("plate", v.plate); put("make", v.make); put("model", v.model)
                        put("year", v.year); put("km", v.km)
                        put("maintenances", JSONArray().apply {
                            v.maintenances.forEach { m -> put(JSONObject().apply {
                                put("id",m.id); put("type",m.type); put("doneKm",m.doneKm)
                                put("doneDate",m.doneDate); put("dueKm",m.dueKm)
                                put("dueDate",m.dueDate); put("notes",m.notes); put("cost",m.cost)
                            }) }
                        })
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
        (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
            val services = o.optJSONArray("maintenances") ?: JSONArray()
            val list = (0 until services.length()).map { j -> services.getJSONObject(j).let { m ->
                Maintenance(m.optLong("id"),m.optString("type","Otro"),m.optInt("doneKm"),
                    m.optString("doneDate"),m.optInt("dueKm"),m.optString("dueDate"),
                    m.optString("notes"),m.optString("cost"))
            } }
            Vehicle(o.getLong("id"),o.getString("client"),o.optString("phone"),
                o.getString("plate"),o.optString("make"),o.optString("model"),
                o.optString("year"),o.getInt("km"),list)
        } }
    } catch (_: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoControlApp(vehicles: List<Vehicle>, save: (List<Vehicle>) -> Unit) {
    var tab by remember { mutableStateOf("Inicio") }
    var showAdd by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var addMaintenanceFor by remember { mutableStateOf<Long?>(null) }
    val selected = vehicles.firstOrNull { it.id == selectedId }
    val allServices = vehicles.flatMap { v -> v.maintenances.map { v to it } }
    val urgent = allServices.count { it.second.statusAt(it.first.km) != "AL DÍA" }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Bg, surface = Color.White)) {
        Scaffold(
            topBar = { TopAppBar(title = { Column {
                Text("AUTOCONTROL", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("GESTIÓN PROFESIONAL DE TALLER", fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.sp)
            } }, actions = { Text("● LOCAL", fontSize = 11.sp, color = Color(0xFF16845B), modifier = Modifier.padding(end=16.dp)) }) },
            bottomBar = { NavigationBar {
                listOf("Inicio","Vehículos","Servicios").forEach { item ->
                    NavigationBarItem(selected=tab==item && selectedId==null, onClick={tab=item;selectedId=null},
                        icon={ Text(when(item){"Inicio"->"⌂";"Vehículos"->"▣";else->"⚙"},fontSize=20.sp) },
                        label={Text(item)})
                }
            } },
            floatingActionButton = {
                if (selectedId == null && tab == "Vehículos") FloatingActionButton(onClick={showAdd=true},containerColor=Blue,contentColor=Color.White){Text("+",fontSize=28.sp)}
                if (selected != null) FloatingActionButton(onClick={addMaintenanceFor=selected.id},containerColor=Blue,contentColor=Color.White){Text("+",fontSize=28.sp)}
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().background(Bg).padding(padding).padding(16.dp)) {
                when {
                    selected != null -> VehicleDetail(selected,
                        onBack={selectedId=null},
                        onUpdateKm={km -> save(vehicles.map { if(it.id==selected.id) it.copy(km=km) else it })},
                        onAddService={m -> save(vehicles.map { if(it.id==selected.id) it.copy(maintenances=it.maintenances+m) else it })}
                    )
                    tab=="Inicio" -> {
                        Text("Panel general",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Ink)
                        Text("Controlá cada vehículo y cada mantenimiento.",color=Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            StatCard("VEHÍCULOS",vehicles.size.toString(),Modifier.weight(1f))
                            StatCard("A REVISAR",urgent.toString(),Modifier.weight(1f),if(urgent>0) Color(0xFFFFF1D6) else Color.White)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("Mantenimientos que requieren atención",fontWeight=FontWeight.Bold,fontSize=17.sp,color=Ink)
                        val dueItems=allServices.filter{it.second.statusAt(it.first.km)!="AL DÍA"}.sortedBy{it.second.remainingAt(it.first.km)}
                        if(dueItems.isEmpty()) EmptyState("Todo en orden","Los servicios próximos o vencidos aparecerán acá.")
                        else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(dueItems){(v,m)->MaintenanceCard(v,m){selectedId=v.id}}}
                    }
                    tab=="Vehículos" -> {
                        Text("Clientes y vehículos",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Ink)
                        Text("Datos de contacto e historial por unidad.",color=Color.Gray)
                        Spacer(Modifier.height(14.dp))
                        if(vehicles.isEmpty()) EmptyState("Todavía no hay vehículos","Tocá + para cargar el primer cliente.")
                        else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(vehicles.sortedBy{it.client.lowercase()}){VehicleCard(it){selectedId=it.id}}}
                    }
                    else -> {
                        Text("Historial de servicios",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Ink)
                        Text("Todos los trabajos registrados en el taller.",color=Color.Gray)
                        Spacer(Modifier.height(14.dp))
                        if(allServices.isEmpty()) EmptyState("Sin servicios registrados","Entrá a un vehículo y tocá + para cargar un mantenimiento.")
                        else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(allServices.reversed()){(v,m)->MaintenanceCard(v,m){selectedId=v.id}}}
                    }
                }
            }
        }
        if(showAdd) AddVehicleDialog(onDismiss={showAdd=false},onSave={save(vehicles+it);showAdd=false})
        val target = vehicles.firstOrNull{it.id==addMaintenanceFor}
        if(target!=null) AddMaintenanceDialog(target.km,onDismiss={addMaintenanceFor=null},onSave={m->
            save(vehicles.map{if(it.id==target.id)it.copy(maintenances=it.maintenances+m)else it});addMaintenanceFor=null
        })
    }
}

@Composable
fun StatCard(label:String,value:String,modifier:Modifier=Modifier,color:Color=Color.White){
    Card(modifier,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=color)){
        Column(Modifier.padding(18.dp)){Text(label,fontSize=11.sp,color=Color.Gray,fontWeight=FontWeight.Bold);Text(value,fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)}
    }
}

@Composable
fun VehicleCard(v:Vehicle,onClick:()->Unit){
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(16.dp)){
        Column(Modifier.padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text(v.client,fontWeight=FontWeight.Bold,fontSize=17.sp,color=Ink);Text(v.displayModel.ifBlank{"Marca/modelo sin cargar"},color=Color.Gray,fontSize=13.sp)}
                Text("${v.maintenances.size} servicios",fontSize=11.sp,color=Blue)
            }
            Spacer(Modifier.height(8.dp))
            Row{Text(v.plate,fontWeight=FontWeight.Bold,color=Ink);Spacer(Modifier.weight(1f));Text("${v.km} km",color=Color.Gray)}
            if(v.phone.isNotBlank())Text("Tel. ${v.phone}",fontSize=12.sp,color=Color.Gray)
            if(v.urgentCount>0)Text("${v.urgentCount} mantenimiento(s) para revisar",color=Color(0xFFB26A00),fontSize=12.sp)
        }
    }
}

@Composable
fun MaintenanceCard(v:Vehicle,m:Maintenance,onClick:()->Unit){
    val status=m.statusAt(v.km); val remaining=m.remainingAt(v.km)
    val color=when(status){"VENCIDO"->Color(0xFFB3261E);"PRÓXIMO"->Color(0xFFB26A00);else->Color(0xFF16845B)}
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(14.dp)){
        Column(Modifier.padding(14.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Text(m.type,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f));Text(status,color=color,fontSize=10.sp,fontWeight=FontWeight.ExtraBold)}
            Text("${v.plate} · ${v.displayModel}",fontSize=12.sp,color=Color.Gray)
            Text(if(m.dueKm>0) if(remaining<=0) "Vencido por ${-remaining} km" else "Próximo control en ${remaining} km" else "Trabajo registrado",fontSize=13.sp,color=color)
            if(m.dueDate.isNotBlank())Text("Próxima fecha: ${m.dueDate}",fontSize=12.sp,color=Color.Gray)
        }
    }
}

@Composable
fun EmptyState(title:String,message:String){
    Box(Modifier.fillMaxWidth().padding(top=38.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){
        Text("▣",fontSize=34.sp,color=Blue);Spacer(Modifier.height(10.dp));Text(title,fontWeight=FontWeight.Bold,color=Ink);Text(message,color=Color.Gray,fontSize=13.sp)
    }}
}

@Composable
fun VehicleDetail(v:Vehicle,onBack:()->Unit,onUpdateKm:(Int)->Unit,onAddService:(Maintenance)->Unit){
    var km by remember(v.id){mutableStateOf(v.km.toString())}
    TextButton(onClick=onBack){Text("← Volver")}
    Text(v.plate,fontSize=27.sp,fontWeight=FontWeight.Black,color=Ink)
    Text(v.displayModel,color=Color.Gray)
    Text("Cliente: ${v.client}",color=Ink)
    if(v.phone.isNotBlank())Text("Teléfono: ${v.phone}",color=Ink)
    Text("Kilometraje: ${v.km} km",color=Ink)
    Spacer(Modifier.height(12.dp))
    Text("Actualizar kilometraje",fontWeight=FontWeight.Bold,color=Ink)
    OutlinedTextField(km,{km=it},label={Text("Kilómetros actuales")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
    Button(onClick={km.toIntOrNull()?.takeIf{it>=0}?.let(onUpdateKm)}){Text("Guardar kilometraje")}
    Spacer(Modifier.height(16.dp))
    Text("Mantenimientos e historial",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink)
    if(v.maintenances.isEmpty())Text("Todavía no hay trabajos cargados.",color=Color.Gray)
    else v.maintenances.sortedByDescending{it.id}.forEach{MaintenanceCard(v,it){};Spacer(Modifier.height(8.dp))}
    Text("Usá el botón + para registrar un nuevo trabajo.",fontSize=12.sp,color=Color.Gray)
}

@Composable
fun AddVehicleDialog(onDismiss:()->Unit,onSave:(Vehicle)->Unit){
    var client by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")}
    var plate by remember{mutableStateOf("")};var make by remember{mutableStateOf("")}
    var model by remember{mutableStateOf("")};var year by remember{mutableStateOf("")};var km by remember{mutableStateOf("")}
    var error by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Nuevo cliente y vehículo",fontWeight=FontWeight.Bold)},
        text={Column(Modifier.heightIn(max=470.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
            OutlinedTextField(client,{client=it},label={Text("Nombre del cliente *")},singleLine=true)
            OutlinedTextField(phone,{phone=it},label={Text("Teléfono / WhatsApp")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),singleLine=true)
            OutlinedTextField(plate,{plate=it},label={Text("Patente *")},singleLine=true)
            OutlinedTextField(make,{make=it},label={Text("Marca (ej. Fiat)")},singleLine=true)
            OutlinedTextField(model,{model=it},label={Text("Modelo / versión")},singleLine=true)
            OutlinedTextField(year,{year=it},label={Text("Año")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(km,{km=it},label={Text("Kilometraje actual *")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            if(error)Text("Completá cliente, patente y un kilometraje válido.",color=Color.Red,fontSize=12.sp)
        }},
        confirmButton={TextButton(onClick={val n=km.toIntOrNull();if(client.isBlank()||plate.isBlank()||n==null||n<0)error=true else onSave(Vehicle(System.currentTimeMillis(),client.trim(),phone.trim(),plate.trim().uppercase(),make.trim(),model.trim(),year.trim(),n))}){Text("Guardar")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}

@Composable
fun AddMaintenanceDialog(currentKm:Int,onDismiss:()->Unit,onSave:(Maintenance)->Unit){
    var type by remember{mutableStateOf(maintenanceTypes.first())};var expanded by remember{mutableStateOf(false)}
    var doneKm by remember{mutableStateOf(currentKm.toString())};var doneDate by remember{mutableStateOf("")}
    var dueKm by remember{mutableStateOf("")};var dueDate by remember{mutableStateOf("")}
    var cost by remember{mutableStateOf("")};var notes by remember{mutableStateOf("")};var error by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Registrar mantenimiento",fontWeight=FontWeight.Bold)},
        text={Column(Modifier.heightIn(max=490.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Text("Tipo de trabajo",fontSize=12.sp,color=Color.Gray)
            Box{
                OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(type,modifier=Modifier.weight(1f));Text("▾")}
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=300.dp)){
                    maintenanceTypes.forEach{item->DropdownMenuItem(text={Text(item)},onClick={type=item;expanded=false})}
                }
            }
            OutlinedTextField(doneKm,{doneKm=it},label={Text("Km al realizar el trabajo")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(doneDate,{doneDate=it},label={Text("Fecha del trabajo (dd/mm/aaaa)")},singleLine=true)
            OutlinedTextField(dueKm,{dueKm=it},label={Text("Próximo mantenimiento a los km (opcional)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true)
            OutlinedTextField(dueDate,{dueDate=it},label={Text("Próxima fecha (opcional)")},singleLine=true)
            OutlinedTextField(cost,{cost=it},label={Text("Costo / importe (opcional)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
            OutlinedTextField(notes,{notes=it},label={Text("Repuestos, detalles y observaciones")},minLines=2)
            if(error)Text("Ingresá un kilometraje válido.",color=Color.Red,fontSize=12.sp)
        }},
        confirmButton={TextButton(onClick={val d=doneKm.toIntOrNull();val due=dueKm.toIntOrNull()?:0;if(d==null||d<0||due<0)error=true else onSave(Maintenance(System.currentTimeMillis(),type,d,doneDate.trim(),due,dueDate.trim(),notes.trim(),cost.trim()))}){Text("Guardar servicio")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
