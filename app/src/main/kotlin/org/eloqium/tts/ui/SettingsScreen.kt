package org.eloqium.tts.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import org.eloqium.tts.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.eloqium.tts.engine.Eci
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.service.EloqiumForegroundService
import org.eloqium.tts.service.Settings
import org.eloqium.tts.service.SettingsDefaults

sealed class SettingsSubScreen {
    object SETTINGS : SettingsSubScreen()
    object FOREGROUND_SERVICE : SettingsSubScreen()
    object DICTIONARY_MANAGER : SettingsSubScreen()
    data class LANGUAGE_DICTIONARIES(val languageTag: String) : SettingsSubScreen()
    data class DICTIONARY_ENTRIES(val languageTag: String, val dictionaryId: String) : SettingsSubScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeSettings = settings ?: remember { Settings(context) }
    val settings = activeSettings

    // State bindings
    var currentSubScreen by remember { mutableStateOf<SettingsSubScreen>(SettingsSubScreen.SETTINGS) }
    var userDictionaryEnabled by remember { mutableStateOf(settings.userDictionaryEnabled) }

    var voiceProfile by remember { mutableIntStateOf(settings.voiceProfile) }
    var forceSpeechRate by remember { mutableStateOf(settings.forceSpeechRate) }
    var speechRate by remember { mutableIntStateOf(settings.speechRate) }
    var unlockMaxRate by remember { mutableStateOf(settings.unlockMaxRate) }

    var forcePitch by remember { mutableStateOf(settings.forcePitch) }
    var pitch by remember { mutableIntStateOf(settings.pitch) }

    var forceVolume by remember { mutableStateOf(settings.forceVolume) }
    var volume by remember { mutableIntStateOf(settings.volume) }
    var eciVoiceTagsEnabled by remember { mutableStateOf(settings.eciVoiceTagsEnabled) }

    var inflection by remember { mutableIntStateOf(settings.inflection) }
    var headSize by remember { mutableIntStateOf(settings.headSize) }
    var roughness by remember { mutableIntStateOf(settings.roughness) }
    var breathiness by remember { mutableIntStateOf(settings.breathiness) }

    var enableEmoji by remember { mutableStateOf(settings.enableEmoji) }
    var processPunctuation by remember { mutableStateOf(settings.processPunctuation) }
    var punctuationLevel by remember { mutableIntStateOf(settings.punctuationLevel) }
    var customPunctuation by remember { mutableStateOf(settings.customPunctuation) }
    var useNumberProcessing by remember { mutableStateOf(settings.useNumberProcessing) }
    var numberProcessingMode by remember { mutableIntStateOf(settings.numberProcessingMode) }
    var useAbbreviations by remember { mutableStateOf(settings.useAbbreviations) }
    var intonationPauses by remember { mutableStateOf(settings.intonationPauses) }

    var forceLanguage by remember { mutableStateOf(settings.forceLanguage) }
    var language by remember { mutableStateOf(settings.language) }
    var samplingRate by remember { mutableIntStateOf(settings.samplingRate) }
    var capitalsIndication by remember { mutableIntStateOf(settings.capitalsIndication) }

    // Dialog visibility states
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showSpeechRateDialog by remember { mutableStateOf(false) }
    var showPitchDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showInflectionDialog by remember { mutableStateOf(false) }
    var showHeadSizeDialog by remember { mutableStateOf(false) }
    var showRoughnessDialog by remember { mutableStateOf(false) }
    var showBreathinessDialog by remember { mutableStateOf(false) }
    var showPunctDialog by remember { mutableStateOf(false) }
    var showNumberDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showSamplingDialog by remember { mutableStateOf(false) }
    var showCapitalsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val punctLevels = listOf("None", "Some", "Most", "All", "Custom")
    val numberModes = listOf("Digits", "Pairs", "Triplets", "Smart")
    val capitalsModes = listOf("None", "Pitch raise", "Say capital")

    fun refreshAllFromSettings() {
        voiceProfile = settings.voiceProfile
        forceSpeechRate = settings.forceSpeechRate
        speechRate = settings.speechRate
        unlockMaxRate = settings.unlockMaxRate
        forcePitch = settings.forcePitch
        pitch = settings.pitch
        forceVolume = settings.forceVolume
        volume = settings.volume
        inflection = settings.inflection
        headSize = settings.headSize
        roughness = settings.roughness
        breathiness = settings.breathiness
        enableEmoji = settings.enableEmoji
        processPunctuation = settings.processPunctuation
        punctuationLevel = settings.punctuationLevel
        customPunctuation = settings.customPunctuation
        useNumberProcessing = settings.useNumberProcessing
        numberProcessingMode = settings.numberProcessingMode
        useAbbreviations = settings.useAbbreviations
        intonationPauses = settings.intonationPauses
        forceLanguage = settings.forceLanguage
        language = settings.language
        samplingRate = settings.samplingRate
        capitalsIndication = settings.capitalsIndication
        userDictionaryEnabled = settings.userDictionaryEnabled
        eciVoiceTagsEnabled = settings.eciVoiceTagsEnabled
    }

    BackHandler(enabled = (currentSubScreen !is SettingsSubScreen.SETTINGS)) {
        when (val screen = currentSubScreen) {
            is SettingsSubScreen.DICTIONARY_ENTRIES -> currentSubScreen = SettingsSubScreen.LANGUAGE_DICTIONARIES(screen.languageTag)
            is SettingsSubScreen.LANGUAGE_DICTIONARIES -> currentSubScreen = SettingsSubScreen.DICTIONARY_MANAGER
            is SettingsSubScreen.DICTIONARY_MANAGER -> currentSubScreen = SettingsSubScreen.SETTINGS
            is SettingsSubScreen.FOREGROUND_SERVICE -> currentSubScreen = SettingsSubScreen.SETTINGS
            SettingsSubScreen.SETTINGS -> onNavigateBack()
        }
    }

    when (val screen = currentSubScreen) {
        is SettingsSubScreen.FOREGROUND_SERVICE -> {
            ForegroundServiceSettingsScreen(
                settings = settings,
                onNavigateBack = { currentSubScreen = SettingsSubScreen.SETTINGS }
            )
            return
        }
        is SettingsSubScreen.DICTIONARY_MANAGER -> {
            DictionaryManagerScreen(
                settings = settings,
                onOpenLanguage = { currentSubScreen = SettingsSubScreen.LANGUAGE_DICTIONARIES(it) },
                onNavigateBack = { currentSubScreen = SettingsSubScreen.SETTINGS }
            )
            return
        }
        is SettingsSubScreen.LANGUAGE_DICTIONARIES -> {
            LanguageDictionariesScreen(
                settings = settings,
                languageTag = screen.languageTag,
                onOpenDictionary = { dictId -> currentSubScreen = SettingsSubScreen.DICTIONARY_ENTRIES(screen.languageTag, dictId) },
                onNavigateBack = { currentSubScreen = SettingsSubScreen.DICTIONARY_MANAGER }
            )
            return
        }
        is SettingsSubScreen.DICTIONARY_ENTRIES -> {
            DictionaryEntriesScreen(
                settings = settings,
                languageTag = screen.languageTag,
                dictionaryId = screen.dictionaryId,
                onNavigateBack = { currentSubScreen = SettingsSubScreen.LANGUAGE_DICTIONARIES(screen.languageTag) }
            )
            return
        }
        SettingsSubScreen.SETTINGS -> { /* Fall through to main settings scaffold */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate up" }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // =================================================================
            // 1. VOICE
            // =================================================================
            SettingsCategoryHeader(title = "Voice")

            // Voice profile
            SettingsClickableItem(
                title = "Voice profile",
                subtitle = Eci.PRESET_NAMES.getOrElse(voiceProfile) { "Reed" },
                onClick = { showVoiceDialog = true }
            )

            // Force rate
            SettingsSwitchItem(
                title = "Force rate",
                subtitle = "Keep Eloqium speech rate even when applications request another rate",
                checked = forceSpeechRate,
                onCheckedChange = {
                    forceSpeechRate = it
                    settings.forceSpeechRate = it
                }
            )

            // Speech rate 21-value selector
            SettingsClickableItem(
                title = "Speech rate",
                subtitle = formatRelativeLabel(speechRate),
                enabled = forceSpeechRate,
                onClick = { showSpeechRateDialog = true }
            )

            // Unlock maximum rate
            SettingsSwitchItem(
                title = "Unlock maximum rate",
                subtitle = "Allow speech rate to exceed standard maximum speed",
                checked = unlockMaxRate,
                onCheckedChange = {
                    unlockMaxRate = it
                    settings.unlockMaxRate = it
                }
            )

            // Force pitch
            SettingsSwitchItem(
                title = "Force pitch",
                subtitle = "Keep Eloqium pitch setting even when applications request another pitch",
                checked = forcePitch,
                onCheckedChange = {
                    forcePitch = it
                    settings.forcePitch = it
                }
            )

            // Pitch 21-value selector
            SettingsClickableItem(
                title = "Pitch",
                subtitle = formatRelativeLabel(pitch),
                enabled = forcePitch,
                onClick = { showPitchDialog = true }
            )

            // Force volume
            SettingsSwitchItem(
                title = "Force volume",
                subtitle = "Keep Eloqium volume level even when system multimedia volume changes",
                checked = forceVolume,
                onCheckedChange = {
                    forceVolume = it
                    settings.forceVolume = it
                }
            )

            // Volume 21-value selector
            SettingsClickableItem(
                title = "Volume",
                subtitle = formatRelativeLabel(volume),
                enabled = forceVolume,
                onClick = { showVolumeDialog = true }
            )

            // ECI Voice Tags
            SettingsSwitchItem(
                title = "ECI Voice Tags",
                subtitle = "Allow supported backquote ECI voice/control annotations in incoming text",
                checked = eciVoiceTagsEnabled,
                onCheckedChange = {
                    eciVoiceTagsEnabled = it
                    settings.eciVoiceTagsEnabled = it
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // =================================================================
            // 2. VOICE CHARACTERISTICS (-10..10)
            // =================================================================
            SettingsCategoryHeader(title = "Voice Characteristics")

            SettingsClickableItem(
                title = "Inflection",
                subtitle = formatRelativeLabel(inflection),
                onClick = { showInflectionDialog = true }
            )

            SettingsClickableItem(
                title = "Head size",
                subtitle = formatRelativeLabel(headSize),
                onClick = { showHeadSizeDialog = true }
            )

            SettingsClickableItem(
                title = "Roughness",
                subtitle = formatRelativeLabel(roughness),
                onClick = { showRoughnessDialog = true }
            )

            SettingsClickableItem(
                title = "Breathiness",
                subtitle = formatRelativeLabel(breathiness),
                onClick = { showBreathinessDialog = true }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // =================================================================
            // 3. TEXT PROCESSING
            // =================================================================
            SettingsCategoryHeader(title = "Text Processing")

            SettingsSwitchItem(
                title = "Emoji / Emoticon",
                subtitle = "Read emoji symbols and conservative ASCII emoticons aloud",
                checked = enableEmoji,
                onCheckedChange = {
                    enableEmoji = it
                    settings.enableEmoji = it
                }
            )

            SettingsSwitchItem(
                title = "Process punctuation",
                subtitle = "Verbalize punctuation marks during speech",
                checked = processPunctuation,
                onCheckedChange = {
                    processPunctuation = it
                    settings.processPunctuation = it
                }
            )

            SettingsClickableItem(
                title = "Punctuation level",
                subtitle = punctLevels.getOrElse(punctuationLevel) { "None" },
                enabled = processPunctuation,
                onClick = { showPunctDialog = true }
            )

            SettingsSwitchItem(
                title = "Use number processing",
                subtitle = "Format number sequences into digits, pairs, triplets, or smart grouping",
                checked = useNumberProcessing,
                onCheckedChange = {
                    useNumberProcessing = it
                    settings.useNumberProcessing = it
                }
            )

            SettingsClickableItem(
                title = "Number processing mode",
                subtitle = numberModes.getOrElse(numberProcessingMode) { "Digits" },
                enabled = useNumberProcessing,
                onClick = { showNumberDialog = true }
            )

            SettingsSwitchItem(
                title = "Use abbreviations",
                subtitle = "Expand common abbreviations using built-in dictionary",
                checked = useAbbreviations,
                onCheckedChange = {
                    useAbbreviations = it
                    settings.useAbbreviations = it
                }
            )

            SettingsSwitchItem(
                title = "Intonation pauses",
                subtitle = "Preserve natural pauses and prosodic intonation at sentence breaks",
                checked = intonationPauses,
                onCheckedChange = {
                    intonationPauses = it
                    settings.intonationPauses = it
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // =================================================================
            // 4. USER DICTIONARY
            // =================================================================
            SettingsCategoryHeader(title = "User Dictionary")

            SettingsSwitchItem(
                title = "User Dictionary",
                subtitle = if (userDictionaryEnabled) "On" else "Off",
                checked = userDictionaryEnabled,
                onCheckedChange = {
                    userDictionaryEnabled = it
                    settings.userDictionaryEnabled = it
                }
            )

            SettingsClickableItem(
                title = "Dictionaries",
                subtitle = "Word replacements and pronunciations",
                onClick = { currentSubScreen = SettingsSubScreen.DICTIONARY_MANAGER }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // =================================================================
            // 5. LANGUAGE & AUDIO
            // =================================================================
            SettingsCategoryHeader(title = "Language & Audio")

            SettingsSwitchItem(
                title = "Force language",
                subtitle = "Keep selected language even when applications request another language",
                checked = forceLanguage,
                onCheckedChange = {
                    forceLanguage = it
                    settings.forceLanguage = it
                }
            )

            val displayLang = LocaleMatcher.ENTRIES.firstOrNull { it.bcp47Tag == language }?.displayName ?: language
            SettingsClickableItem(
                title = "Language",
                subtitle = displayLang,
                enabled = forceLanguage,
                onClick = { showLangDialog = true }
            )

            SettingsClickableItem(
                title = "Sampling rate",
                subtitle = "$samplingRate Hz",
                onClick = { showSamplingDialog = true }
            )

            SettingsClickableItem(
                title = "Capitals indication",
                subtitle = capitalsModes.getOrElse(capitalsIndication) { "None" },
                onClick = { showCapitalsDialog = true }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // =================================================================
            // 6. ADVANCED
            // =================================================================
            SettingsCategoryHeader(title = "Advanced")

            SettingsClickableItem(
                title = "Eloqium foreground service",
                subtitle = "Improve reliability when Eloqium runs in the background",
                onClick = { currentSubScreen = SettingsSubScreen.FOREGROUND_SERVICE }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerDefaults.color.copy(alpha = 0.5f)
            )

            // =================================================================
            // 7. RESET
            // =================================================================
            SettingsCategoryHeader(title = "Reset")

            SettingsClickableItem(
                title = "Reset all settings to the default",
                subtitle = "Restore all configuration values to initial defaults",
                onClick = { showResetDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    // 21-Value Relative Dialogs (-10..10)
    if (showSpeechRateDialog) {
        RelativeValueDialog(
            title = "Speech rate",
            currentValue = speechRate,
            onValueSelected = {
                speechRate = it
                settings.speechRate = it
                showSpeechRateDialog = false
            },
            onDismiss = { showSpeechRateDialog = false }
        )
    }

    if (showPitchDialog) {
        RelativeValueDialog(
            title = "Pitch",
            currentValue = pitch,
            onValueSelected = {
                pitch = it
                settings.pitch = it
                showPitchDialog = false
            },
            onDismiss = { showPitchDialog = false }
        )
    }

    if (showVolumeDialog) {
        RelativeValueDialog(
            title = "Volume",
            currentValue = volume,
            onValueSelected = {
                volume = it
                settings.volume = it
                showVolumeDialog = false
            },
            onDismiss = { showVolumeDialog = false }
        )
    }

    if (showInflectionDialog) {
        RelativeValueDialog(
            title = "Inflection",
            currentValue = inflection,
            onValueSelected = {
                inflection = it
                settings.inflection = it
                showInflectionDialog = false
            },
            onDismiss = { showInflectionDialog = false }
        )
    }

    if (showHeadSizeDialog) {
        RelativeValueDialog(
            title = "Head size",
            currentValue = headSize,
            onValueSelected = {
                headSize = it
                settings.headSize = it
                showHeadSizeDialog = false
            },
            onDismiss = { showHeadSizeDialog = false }
        )
    }

    if (showRoughnessDialog) {
        RelativeValueDialog(
            title = "Roughness",
            currentValue = roughness,
            onValueSelected = {
                roughness = it
                settings.roughness = it
                showRoughnessDialog = false
            },
            onDismiss = { showRoughnessDialog = false }
        )
    }

    if (showBreathinessDialog) {
        RelativeValueDialog(
            title = "Breathiness",
            currentValue = breathiness,
            onValueSelected = {
                breathiness = it
                settings.breathiness = it
                showBreathinessDialog = false
            },
            onDismiss = { showBreathinessDialog = false }
        )
    }

    VoiceProfileDialog(
        visible = showVoiceDialog,
        currentProfile = voiceProfile,
        onProfileSelected = {
            voiceProfile = it
            settings.voiceProfile = it
            showVoiceDialog = false
        },
        onDismiss = { showVoiceDialog = false }
    )

    PunctuationLevelDialog(
        visible = showPunctDialog,
        currentLevel = punctuationLevel,
        customPunctuation = customPunctuation,
        levels = punctLevels,
        onConfirm = { level, customStr ->
            punctuationLevel = level
            customPunctuation = customStr
            settings.punctuationLevel = level
            settings.customPunctuation = customStr
            showPunctDialog = false
        },
        onDismiss = { showPunctDialog = false }
    )

    NumberProcessingDialog(
        visible = showNumberDialog,
        currentMode = numberProcessingMode,
        modes = numberModes,
        onModeSelected = {
            numberProcessingMode = it
            settings.numberProcessingMode = it
            showNumberDialog = false
        },
        onDismiss = { showNumberDialog = false }
    )

    LanguageDialog(
        visible = showLangDialog,
        currentLanguageTag = language,
        onLanguageSelected = {
            language = it
            settings.language = it
            showLangDialog = false
        },
        onDismiss = { showLangDialog = false }
    )

    SamplingRateDialog(
        visible = showSamplingDialog,
        currentRate = samplingRate,
        onRateSelected = {
            samplingRate = it
            settings.samplingRate = it
            showSamplingDialog = false
        },
        onDismiss = { showSamplingDialog = false }
    )

    CapitalsIndicationDialog(
        visible = showCapitalsDialog,
        currentMode = capitalsIndication,
        modes = capitalsModes,
        onModeSelected = {
            capitalsIndication = it
            settings.capitalsIndication = it
            showCapitalsDialog = false
        },
        onDismiss = { showCapitalsDialog = false }
    )

    ResetConfirmationDialog(
        visible = showResetDialog,
        onConfirmReset = {
            settings.resetAll()
            EloqiumForegroundService.syncService(context, settings)
            refreshAllFromSettings()
            showResetDialog = false
        },
        onDismiss = { showResetDialog = false }
    )
}
