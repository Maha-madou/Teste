package com.example.maintenance

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.FRENCH }

private data class Service(
    val id: String,
    val roadsideAssistance: Boolean = false,
    val maternityTransport: Boolean = false
)

private val services = listOf(
    Service("phones"),
    Service("televisions"),
    Service("printing"),
    Service("satellite"),
    Service("air"),
    Service("fridge"),
    Service(
        "auto",
        roadsideAssistance = true
    ),
    Service(
        "maternity",
        maternityTransport = true
    )
)

@Composable
private fun tr(key: String, vararg arguments: Any): String =
    AppText.get(LocalAppLanguage.current, key, *arguments)

@Composable
private fun LanguagePicker(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("${tr("language.choose")}: ${current.label}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.label) },
                    onClick = {
                        expanded = false
                        onSelect(language)
                    }
                }
            }
        }
    }
}

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
    var showQuoteFlow by rememberSaveable { mutableStateOf(false) }
    var showContentLinks by rememberSaveable { mutableStateOf(false) }
    var languageCode by rememberSaveable { mutableStateOf(AppLanguage.FRENCH.code) }
    val language = AppLanguage.entries.firstOrNull { it.code == languageCode } ?: AppLanguage.FRENCH

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalLayoutDirection provides if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
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
                                text = when {
                                    showQuoteFlow -> tr("nav.quote")
                                    showRequestForm -> tr("nav.request")
                                    showContentLinks -> tr("content.title")
                                    selectedService != null -> tr("service.${selectedService!!.id}.name")
                                    else -> tr("home.title")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (selectedService != null || showContentLinks) {
                                OutlinedButton(
                                    onClick = {
                                        when {
                                            showQuoteFlow -> showQuoteFlow = false
                                            showRequestForm -> showRequestForm = false
                                            showContentLinks -> showContentLinks = false
                                            else -> selectedService = null
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(tr("nav.back"))
                                }
                            }
                        },
                        actions = {
                            LanguagePicker(language) { languageCode = it.code }
                        }
                    )
                }
            ) { innerPadding ->
                when {
                    showQuoteFlow -> QuoteConfirmationScreen(
                        service = requireNotNull(selectedService),
                        modifier = Modifier.padding(innerPadding),
                        onSendWhatsApp = { message ->
                            val result = openWhatsApp(context, message, language)
                            scope.launch { snackbarHostState.showSnackbar(result) }
                        }
                    )
                    showRequestForm -> RequestForm(
                        service = requireNotNull(selectedService),
                        modifier = Modifier.padding(innerPadding),
                        onSubmit = { name, phone, location, problem, vehicle, knownCause, neededParts, destination ->
                            val message = buildRequestMessage(
                                language = language,
                                service = requireNotNull(selectedService),
                                name = name,
                                phone = phone,
                                location = location,
                                problem = problem,
                                vehicle = vehicle,
                                knownCause = knownCause,
                                neededParts = neededParts,
                                destination = destination
                            )
                            val result = openWhatsApp(context, message, language)
                            scope.launch { snackbarHostState.showSnackbar(result) }
                        }
                    )
                    showContentLinks -> ContentLinksScreen(
                        modifier = Modifier.padding(innerPadding),
                        onOpenLink = { url ->
                            val result = openContentLink(context, url, language)
                            if (result != null) {
                                scope.launch { snackbarHostState.showSnackbar(result) }
                            }
                        }
                    )
                    selectedService != null -> ServiceDetails(
                        service = requireNotNull(selectedService),
                        modifier = Modifier.padding(innerPadding),
                        onRequest = { showRequestForm = true },
                        onQuote = { showQuoteFlow = true },
                        onCallContact = {
                            val result = dialContact(context, language)
                            scope.launch { snackbarHostState.showSnackbar(result) }
                        }
                    )
                    else -> ServiceList(
                        modifier = Modifier.padding(innerPadding),
                        onSelect = { selectedService = it },
                        onOpenContentLinks = { showContentLinks = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceList(
    modifier: Modifier = Modifier,
    onSelect: (Service) -> Unit,
    onOpenContentLinks: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(tr("home.headline"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(tr("home.intro"))
            Spacer(Modifier.height(12.dp))
            Text(tr("home.services"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onOpenContentLinks, modifier = Modifier.fillMaxWidth()) {
                Text(tr("content.title"))
            }
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
                        Text(tr("service.${service.id}.name"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(tr("service.${service.id}.summary"), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text(tr("home.question"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class ExternalContentLink(val titleKey: String, val url: String)

@Composable
private fun ContentLinksScreen(
    modifier: Modifier = Modifier,
    onOpenLink: (String) -> Unit
) {
    val links = listOf(
        ExternalContentLink("content.news", ContentLinks.NEWS_URL),
        ExternalContentLink("content.wazouNiger", ContentLinks.WAZOU_NIGER_URL),
        ExternalContentLink("content.wazouAbroad", ContentLinks.WAZOU_ABROAD_URL),
        ExternalContentLink("content.mecca", ContentLinks.MECCA_LIVE_URL)
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(tr("content.intro"), style = MaterialTheme.typography.bodyMedium)
        }
        items(links) { link ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(tr(link.titleKey), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (link.url.isBlank()) {
                        Text(tr("content.notConfigured"), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onOpenLink(link.url) },
                        enabled = link.url.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (link.url.isBlank()) tr("content.notConfigured") else tr("content.open"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceDetails(
    service: Service,
    modifier: Modifier = Modifier,
    onRequest: () -> Unit,
    onQuote: () -> Unit,
    onCallContact: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(tr("service.${service.id}.name"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(tr("service.${service.id}.summary"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(tr("service.${service.id}.details"), style = MaterialTheme.typography.bodyLarge)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text(
                if (service.maternityTransport) {
                    tr("service.maternity.warning")
                } else {
                    tr("service.requestInfo")
                },
                modifier = Modifier.padding(18.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (service.maternityTransport) {
            OutlinedButton(onClick = onCallContact, modifier = Modifier.fillMaxWidth()) {
                Text(tr("service.callTemporary"))
            }
        }
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text(if (service.maternityTransport) tr("service.sendMaternity") else tr("service.makeRequest"))
        }
        OutlinedButton(onClick = onQuote, modifier = Modifier.fillMaxWidth()) {
            Text(tr("service.quote"))
        }
    }
}

@Composable
private fun QuoteConfirmationScreen(
    service: Service,
    modifier: Modifier = Modifier,
    onSendWhatsApp: (String) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var paymentMethod by rememberSaveable { mutableStateOf<String?>(null) }
    val language = LocalAppLanguage.current
    val parsedAmount = amount.trim().replace(',', '.').toBigDecimalOrNull()
    val validQuote = parsedAmount != null && parsedAmount > BigDecimal.ZERO && currency.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(tr("quote.title", tr("service.${service.id}.name")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (accepted == null) {
            Text(tr("quote.instructions"))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(tr("quote.amount")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amount.isNotBlank() && parsedAmount == null
            )
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it },
                label = { Text(tr("quote.currency")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (amount.isNotBlank() && (parsedAmount == null || parsedAmount <= BigDecimal.ZERO)) {
                Text(
                    tr("quote.amountError"),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (validQuote) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(tr("quote.total"), style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${amount.trim()} ${currency.trim()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("quote.total"), style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${amount.trim()} ${currency.trim()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when (accepted) {
            null -> {
                Button(
                    onClick = { accepted = true },
                    enabled = validQuote,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("quote.accept"))
                }
                OutlinedButton(
                    onClick = {
                        accepted = false
                        onSendWhatsApp(buildQuoteResponse(language, service, amount, currency, false, null))
                    },
                    enabled = validQuote,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("quote.refuse"))
                }
            }
            true -> {
                Text(tr("quote.accepted"))
                PaymentConfig.METHOD_LABELS.forEach { method ->
                    Card(
                        onClick = { paymentMethod = method },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (paymentMethod == method) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method })
                            Text(method, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                Text(
                    tr("quote.paymentNote"),
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        onSendWhatsApp(
                            buildQuoteResponse(
                                language,
                                service,
                                amount,
                                currency,
                                accepted = true,
                                paymentMethod = requireNotNull(paymentMethod)
                            )
                        )
                    },
                    enabled = paymentMethod != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("quote.confirm"))
                }
            }
            false -> {
                Text(tr("quote.refused"))
            }
        }
    }
}

@Composable
private fun RequestForm(
    service: Service,
    modifier: Modifier = Modifier,
    onSubmit: (String, String, String, String, String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var problem by rememberSaveable { mutableStateOf("") }
    var vehicle by rememberSaveable { mutableStateOf("") }
    var knownCause by rememberSaveable { mutableStateOf("") }
    var neededParts by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var locationStatus by rememberSaveable { mutableStateOf("") }
    var transcriptionStatus by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val activeSpeechRecognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) {
        onDispose { activeSpeechRecognizer.value?.destroy() }
    }

    fun beginTranscription() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            transcriptionStatus = AppText.get(language, "request.transcriptionUnavailable")
            return
        }
        transcriptionStatus = AppText.get(language, "request.transcribing")
        startVoiceRecognition(
            context = context,
            languageTag = language.speechLocale,
            onRecognizerCreated = { activeSpeechRecognizer.value = it },
            onResult = { recognizedText ->
                activeSpeechRecognizer.value = null
                if (recognizedText.isNullOrBlank()) {
                    transcriptionStatus = AppText.get(language, "request.transcriptionEmpty")
                } else {
                    problem = listOf(problem.trim(), recognizedText.trim())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    transcriptionStatus = AppText.get(language, "request.transcribed")
                }
            },
            onError = {
                activeSpeechRecognizer.value = null
                transcriptionStatus = AppText.get(language, "request.transcriptionUnavailable")
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginTranscription()
        } else {
            transcriptionStatus = AppText.get(language, "request.audioDenied")
        }
    }
    val valid = when {
        service.maternityTransport -> phone.isNotBlank() && location.isNotBlank()
        service.roadsideAssistance ->
            name.isNotBlank() && phone.isNotBlank() && problem.isNotBlank() &&
                location.isNotBlank() && vehicle.isNotBlank()
        else -> name.isNotBlank() && phone.isNotBlank() && problem.isNotBlank()
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestCurrentLocation(context) { currentLocation ->
                if (currentLocation == null) {
                    locationStatus = AppText.get(language, "request.locationUnavailable")
                } else {
                    location = AppText.get(
                        language,
                        "request.locationFormat",
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                    locationStatus = AppText.get(language, "request.locationAdded")
                }
            }
        } else {
            locationStatus = AppText.get(language, "request.locationDenied")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(tr("request.title", tr("service.${service.id}.name")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            if (service.maternityTransport) {
                tr("request.maternityIntro")
            } else {
                tr("request.genericIntro")
            }
        )
        if (service.maternityTransport) {
            Text(
                tr("request.maternityWarning"),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (!service.maternityTransport) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("request.name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = submitted && name.isBlank()
            )
        }
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = {
                Text(if (service.maternityTransport) tr("request.callbackPhone") else tr("request.phone"))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = submitted && phone.isBlank()
        )
        if (service.roadsideAssistance) {
            OutlinedTextField(
                value = vehicle,
                onValueChange = { vehicle = it },
                label = { Text(tr("request.vehicle")) },
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
                    when {
                        service.maternityTransport -> tr("request.pickup")
                        service.roadsideAssistance -> tr("request.breakdownLocation")
                        else -> tr("request.city")
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = submitted && (service.roadsideAssistance || service.maternityTransport) && location.isBlank()
        )
        OutlinedButton(
            onClick = {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    requestCurrentLocation(context) { currentLocation ->
                        if (currentLocation == null) {
                            locationStatus = AppText.get(language, "request.locationUnavailable")
                        } else {
                            location = AppText.get(
                                language,
                                "request.locationFormat",
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                            locationStatus = AppText.get(language, "request.locationAdded")
                        }
                    }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("request.shareLocation"))
        }
        if (locationStatus.isNotBlank()) {
            Text(locationStatus, style = MaterialTheme.typography.bodySmall)
        }
        if (service.maternityTransport) {
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(tr("request.destination")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = problem,
            onValueChange = { problem = it },
            label = {
                Text(
                    when {
                        service.maternityTransport -> tr("request.maternityDetails")
                        service.roadsideAssistance -> tr("request.symptoms")
                        else -> tr("request.problem")
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = if (service.maternityTransport) 2 else 4,
            isError = submitted && !service.maternityTransport && problem.isBlank()
        )
        OutlinedButton(
            onClick = {
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    beginTranscription()
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("request.transcribe"))
        }
        Text(tr("request.transcriptionPrivacy"), style = MaterialTheme.typography.bodySmall)
        if (transcriptionStatus.isNotBlank()) {
            Text(transcriptionStatus, style = MaterialTheme.typography.bodySmall)
        }
        if (service.roadsideAssistance) {
            OutlinedTextField(
                value = knownCause,
                onValueChange = { knownCause = it },
                label = { Text(tr("request.knownCause")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = neededParts,
                onValueChange = { neededParts = it },
                label = { Text(tr("request.parts")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Text(
                tr("request.partsNote"),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (submitted && !valid) {
            Text(
                when {
                    service.maternityTransport ->
                        tr("request.invalidMaternity")
                    service.roadsideAssistance ->
                        tr("request.invalidAuto")
                    else ->
                        tr("request.invalidGeneric")
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
                        neededParts.trim(),
                        destination.trim()
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("request.sendWhatsApp"))
        }
        Text(tr("request.noSave"), style = MaterialTheme.typography.bodySmall)
    }
}

private fun buildRequestMessage(
    language: AppLanguage,
    service: Service,
    name: String,
    phone: String,
    location: String,
    problem: String,
    vehicle: String,
    knownCause: String,
    neededParts: String,
    destination: String
): String = buildString {
    appendLine(AppText.get(language, "message.requestIntro"))
    appendLine(AppText.get(language, "message.service", AppText.get(language, "service.${service.id}.name")))
    if (service.maternityTransport) {
        appendLine(AppText.get(language, "message.callbackPhone", phone))
        appendLine(AppText.get(language, "message.pickup", location))
        if (destination.isNotBlank()) appendLine(AppText.get(language, "message.destination", destination))
        if (problem.isNotBlank()) appendLine(AppText.get(language, "message.urgency", problem))
        appendLine(AppText.get(language, "message.temporary"))
        append(AppText.get(language, "message.emergency"))
    } else if (service.roadsideAssistance) {
        appendLine(AppText.get(language, "message.name", name))
        appendLine(AppText.get(language, "message.phone", phone))
        appendLine(AppText.get(language, "message.vehicle", vehicle))
        appendLine(AppText.get(language, "message.breakdownLocation", location))
        appendLine(AppText.get(language, "message.symptoms", problem))
        if (knownCause.isNotBlank()) appendLine(AppText.get(language, "message.knownCause", knownCause))
        if (neededParts.isNotBlank()) appendLine(AppText.get(language, "message.parts", neededParts))
        append(AppText.get(language, "message.partsConfirmation"))
    } else {
        appendLine(AppText.get(language, "message.name", name))
        appendLine(AppText.get(language, "message.phone", phone))
        if (location.isNotBlank()) appendLine(AppText.get(language, "message.city", location))
        append(AppText.get(language, "message.problem", problem))
    }
}

private fun buildQuoteResponse(
    language: AppLanguage,
    service: Service,
    amount: String,
    currency: String,
    accepted: Boolean,
    paymentMethod: String?
): String = buildString {
    appendLine(AppText.get(language, "message.quoteReply", AppText.get(language, "service.${service.id}.name")))
    appendLine(AppText.get(language, "message.quoteTotal", amount.trim(), currency.trim()))
    if (accepted) {
        appendLine(AppText.get(language, "message.quoteAccept"))
        appendLine(AppText.get(language, "message.paymentMethod", requireNotNull(paymentMethod)))
        append(AppText.get(language, "message.paymentNotice"))
    } else {
        append(AppText.get(language, "message.quoteRefuse"))
    }
}

private fun openContentLink(context: Context, url: String, language: AppLanguage): String? {
    val uri = Uri.parse(url)
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) {
        return AppText.get(language, "content.openError")
    }

    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        null
    } catch (_: ActivityNotFoundException) {
        AppText.get(language, "content.openError")
    }
}

private fun openWhatsApp(context: Context, message: String, language: AppLanguage): String {
    val number = ContactConfig.CONTACT_PHONE_NUMBER.filter(Char::isDigit)
    if (number.length !in 8..15) {
        return AppText.get(language, "action.whatsappMissing")
    }

    val uri = Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}")
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        AppText.get(language, "action.whatsappOpened")
    } catch (_: ActivityNotFoundException) {
        AppText.get(language, "action.whatsappError")
    }
}

private fun dialContact(context: Context, language: AppLanguage): String {
    val number = ContactConfig.CONTACT_PHONE_NUMBER.filter(Char::isDigit)
    if (number.length !in 8..15) {
        return AppText.get(language, "action.contactMissing")
    }

    return try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        AppText.get(language, "action.dialerOpened")
    } catch (_: ActivityNotFoundException) {
        AppText.get(language, "action.dialerError")
    }
}

private fun startVoiceRecognition(
    context: Context,
    languageTag: String,
    onRecognizerCreated: (SpeechRecognizer) -> Unit,
    onResult: (String?) -> Unit,
    onError: () -> Unit
) {
    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    onRecognizerCreated(recognizer)
    var completed = false
    fun finish(callback: () -> Unit) {
        if (completed) return
        completed = true
        recognizer.destroy()
        callback()
    }

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onError(error: Int) = finish(onError)
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            finish { onResult(text) }
        }
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    })

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    try {
        recognizer.startListening(intent)
    } catch (_: IllegalStateException) {
        finish(onError)
    } catch (_: SecurityException) {
        finish(onError)
    }
}

@SuppressLint("MissingPermission")
private fun requestCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .firstOrNull { locationManager.isProviderEnabled(it) }
    if (provider == null) {
        onResult(null)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            locationManager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                onResult(location)
            }
        } catch (_: SecurityException) {
            onResult(null)
        }
        return
    }

    val handler = Handler(Looper.getMainLooper())
    var completed = false
    lateinit var listener: LocationListener
    fun finish(location: Location?) {
        if (completed) return
        completed = true
        handler.removeCallbacksAndMessages(null)
        try {
            locationManager.removeUpdates(listener)
        } catch (_: SecurityException) {
            // Permission may have been revoked while the one-shot request was pending.
        }
        onResult(location)
    }

    listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = finish(location)
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = finish(null)
    }
    handler.postDelayed({ finish(null) }, 15_000)
    try {
        @Suppress("DEPRECATION")
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
    } catch (_: SecurityException) {
        finish(null)
    }
}
