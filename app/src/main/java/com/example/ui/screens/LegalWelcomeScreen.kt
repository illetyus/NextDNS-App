package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class LegalLabels(
  val title: String,
  val explanation: String,
  val terms: String,
  val privacy: String,
  val accept: String,
  val continueLabel: String,
  val error: String
)

private fun legalLabels(language: String): LegalLabels = when (language) {
  "tr" -> LegalLabels("Kullanım Koşulları", "Devam etmeden önce koşulları inceleyiniz.",
    "Kullanım Koşulları", "Gizlilik Politikası (İngilizce)",
    "Kullanım Koşullarını okudum ve kabul ediyorum.", "Kabul Et ve Devam Et",
    "Kabul kaydedilemedi. Lütfen yeniden deneyiniz.")
  "de" -> LegalLabels("Nutzungsbedingungen", "Bitte lesen Sie die Bedingungen vor der Nutzung.",
    "Nutzungsbedingungen", "Datenschutzerklärung (Englisch)",
    "Ich habe die Nutzungsbedingungen gelesen und akzeptiere sie.", "Zustimmen und fortfahren",
    "Die Zustimmung konnte nicht gespeichert werden.")
  "fr" -> LegalLabels("Conditions d’utilisation", "Veuillez consulter les conditions avant de poursuivre.",
    "Conditions d’utilisation", "Politique de confidentialité (anglais)",
    "J’ai lu et j’accepte les conditions d’utilisation.", "Accepter et continuer",
    "L’acceptation n’a pas pu être enregistrée.")
  "es" -> LegalLabels("Condiciones de uso", "Consulte las condiciones antes de continuar.",
    "Condiciones de uso", "Política de privacidad (inglés)",
    "He leído y acepto las condiciones de uso.", "Aceptar y continuar",
    "No se ha podido guardar la aceptación.")
  else -> LegalLabels("Terms of Use", "Please review these terms before continuing.",
    "Terms of Use", "Privacy Policy (English)",
    "I have read and agree to the Terms of Use.", "Accept and Continue",
    "The acceptance could not be saved. Please try again.")
}

/**
 * Offline terms/privacy reader and explicit terms-only acceptance gate.
 * The bundled legal texts are DRAFT material and MUST be reviewed/replaced
 * before any public release. Privacy Policy access does not imply consent.
 */
@Composable
fun LegalWelcomeScreen(
  onAccept: () -> Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val language = remember {
    Locale.getDefault().language.let {
      if (it in listOf("tr", "en", "de", "fr", "es")) it else "en"
    }
  }
  val labels = remember(language) { legalLabels(language) }

  val terms = remember(language) {
    runCatching {
      context.assets.open("legal/terms_${language}.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()
  }
  val privacy = remember {
    runCatching {
      context.assets.open("legal/privacy_en.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()
  }

  var selectedTerms by remember { mutableStateOf(true) }
  var checked by remember { mutableStateOf(false) }
  var storageError by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(labels.title, style = MaterialTheme.typography.headlineSmall)
    Text(labels.explanation, style = MaterialTheme.typography.bodyMedium)

    Text(
      "DRAFT — LEGAL REVIEW REQUIRED — NOT FOR RELEASE",
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.labelMedium
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = { selectedTerms = true }) {
        Text(labels.terms)
      }
      OutlinedButton(onClick = { selectedTerms = false }) {
        Text(labels.privacy)
      }
    }

    Surface(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      key(selectedTerms, language) {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
          SelectionContainer {
            Text(
              if (selectedTerms) terms ?: labels.error else privacy ?: labels.error,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(
        checked = checked,
        onCheckedChange = { checked = it; storageError = false },
        enabled = terms != null && privacy != null
      )
      Text(labels.accept, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
    if (storageError) {
      Text(labels.error, color = MaterialTheme.colorScheme.error)
    }
    Button(
      onClick = { if (!onAccept()) storageError = true },
      enabled = checked && terms != null && privacy != null,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(labels.continueLabel)
    }
  }
}
