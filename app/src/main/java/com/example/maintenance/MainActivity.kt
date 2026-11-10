package com.example.maintenance

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class Service(
    val name: String,
    val summary: String,
    val details: String,
    val roadsideAssistance: Boolean = false
)

private val services = listOf(
    Service("Téléphones", "Réparation et maintenance de smartphones", "Diagnostic, écran, batterie, charge et problèmes logiciels."),
    Service("Télévisions", "Diagnostic et réparation TV", "Panne d'image ou de son, alimentation et installation."),
    Service("Imprimerie", "Impression et services bureautiques", "Impression de documents et accompagnement pour vos travaux d'imprimerie."),
    Service("Antennes paraboliques", "Installation et réglage", "Pose, orientation et dépannage de votre antenne."),
    Service("Climatiseurs", "Installation et entretien", "Diagnostic, maintenance et installation de climatiseurs."),
    Service("Réfrigérateurs", "Réparation et entretien", "Diagnostic des problèmes de froid et maintenance de réfrigérateurs."),
    Service(
        "Dépannage automobile",
        "Assistance en cas de panne sur la route",
        "Décrivez votre véhicule, votre localisation et les symptômes. Un agent pourra évaluer la panne et, si elle est identifiée, confirmer avec vous la possibilité de livraison des pièces nécessaires.",
        roadsideAssistance = true
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ServicePlusApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicePlusApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var showRequestForm by rememberSaveable { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF176B5B),
            secondary = androidx.compose.ui.graphics.Color(0xFFE9A23B),
            background = androidx.compose.ui.graphics.Color(0xFFF6F8F7),
            surface = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (showRequestForm) "Votre demande" else selectedService?.name ?: "Service Plus",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (selectedService != null || showRequestForm) {
                            OutlinedButton(
                                onClick = {
                                    if (showRequestForm) showRequestForm = false else selectedService = null
                                },
                                modifier = Modifier.padding(start = 8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Retour")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            when {
                showRequestForm -> RequestForm(
                    service = requireNotNull(selectedService),
                    modifier = Modifier.padding(innerPadding),
                    onSubmit = { name, phone, location, problem, vehicle, knownCause, neededParts ->
                        val message = buildRequestMessage(
                            service = requireNotNull(selectedService),
                            name = name,
                            phone = phone,
                            location = location,
                            problem = problem,
                            vehicle = vehicle,
                            knownCause = knownCause,
                            neededParts = neededParts
                        )
                        val result = openWhatsApp(context, message)
                        scope.launch { snackbarHostState.showSnackbar(result) }
                    }
                )
                selectedService != null -> ServiceDetails(
                    service = requireNotNull(selectedService),
                    modifier = Modifier.padding(innerPadding),
                    onRequest = { showRequestForm = true }
                )
                else -> ServiceList(
                    modifier = Modifier.padding(innerPadding),
                    onSelect = { selectedService = it }
                )
            }
        }
    }
}

@Composable
private fun ServiceList(modifier: Modifier = Modifier, onSelect: (Service) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Un dépannage simple, en quelques étapes.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Choisissez le service dont vous avez besoin. Notre équipe vous recontactera via WhatsApp.")
            Spacer(Modifier.height(12.dp))
            Text("NOS SERVICES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        items(services) { service ->
            Card(
                onClick = { onSelect(service) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(service.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text("Une question ? Choisissez un service pour nous expliquer votre besoin.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ServiceDetails(service: Service, modifier: Modifier = Modifier, onRequest: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(service.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(service.summary, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(service.details, style = MaterialTheme.typography.bodyLarge)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(
                "Décrivez votre problème et laissez vos coordonnées. Votre demande sera préparée dans WhatsApp.",
                modifier = Modifier.padding(18.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Faire une demande")
        }
    }
}

@Composable
private fun RequestForm(
    service: Service,
    modifier: Modifier = Modifier,
    onSubmit: (String, String, String, String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var problem by rememberSaveable { mutableStateOf("") }
    var vehicle by rememberSaveable { mutableStateOf("") }
    var knownCause by rememberSaveable { mutableStateOf("") }
    var neededParts by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val valid = name.isNotBlank() &&
        phone.isNotBlank() &&
        problem.isNotBlank() &&
        (!service.roadsideAssistance || (location.isNotBlank() && vehicle.isNotBlank()))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Demande : ${service.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Indiquez comment vous joindre et décrivez le problème.")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Votre nom *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = submitted && name.isBlank()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Votre téléphone *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = submitted && phone.isBlank()
        )
        if (service.roadsideAssistance) {
            OutlinedTextField(
                value = vehicle,
                onValueChange = { vehicle = it },
                label = { Text("Véhicule (marque, modèle) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = submitted && vehicle.isBlank()
            )
        }
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = {
                Text(
                    if (service.roadsideAssistance) "Localisation de la panne *"
                    else "Votre ville ou quartier (facultatif)"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = submitted && service.roadsideAssistance && location.isBlank()
        )
        OutlinedTextField(
            value = problem,
            onValueChange = { problem = it },
            label = {
                Text(
                    if (service.roadsideAssistance) "Symptômes de la panne *"
                    else "Décrivez votre problème *"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            isError = submitted && problem.isBlank()
        )
        if (service.roadsideAssistance) {
            OutlinedTextField(
                value = knownCause,
                onValueChange = { knownCause = it },
                label = { Text("Panne identifiée (facultatif)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = neededParts,
                onValueChange = { neededParts = it },
                label = { Text("Pièces nécessaires, si connues (facultatif)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Text(
                "La livraison des pièces sera à confirmer avec l'équipe après évaluation de la panne.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (submitted && !valid) {
            Text(
                if (service.roadsideAssistance) {
                    "Veuillez renseigner votre nom, votre téléphone, le véhicule, la localisation et les symptômes."
                } else {
                    "Veuillez renseigner votre nom, votre téléphone et le problème."
                },
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {
                submitted = true
                if (valid) {
                    onSubmit(
                        name.trim(),
                        phone.trim(),
                        location.trim(),
                        problem.trim(),
                        vehicle.trim(),
                        knownCause.trim(),
                        neededParts.trim()
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuer avec WhatsApp")
        }
        Text("Aucune donnée n'est enregistrée dans l'application.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun buildRequestMessage(
    service: Service,
    name: String,
    phone: String,
    location: String,
    problem: String,
    vehicle: String,
    knownCause: String,
    neededParts: String
): String = buildString {
    appendLine("Bonjour, je souhaite faire une demande de service.")
    appendLine("Service : ${service.name}")
    appendLine("Nom : $name")
    appendLine("Téléphone : $phone")
    if (service.roadsideAssistance) {
        appendLine("Véhicule : $vehicle")
        appendLine("Localisation de la panne : $location")
        appendLine("Symptômes : $problem")
        if (knownCause.isNotBlank()) appendLine("Panne identifiée par le client : $knownCause")
        if (neededParts.isNotBlank()) appendLine("Pièces souhaitées (à confirmer) : $neededParts")
        append("Merci de m'indiquer les possibilités d'intervention et, si la panne est identifiée, de livraison des pièces.")
    } else {
        if (location.isNotBlank()) appendLine("Ville/quartier : $location")
        append("Problème : $problem")
    }
}

private fun openWhatsApp(context: Context, message: String): String {
    val number = WhatsappConfig.BUSINESS_NUMBER.filter(Char::isDigit)
    if (number.length !in 8..15) {
        return "WhatsApp n'est pas encore configuré. Ajoutez le numéro professionnel dans WhatsappConfig.kt."
    }

    val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        "Ouverture de WhatsApp..."
    } catch (_: ActivityNotFoundException) {
        "Impossible d'ouvrir WhatsApp ou un navigateur sur cet appareil."
    }
}
