package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

/** Offline notices are available before sign-in and after acceptance. */
@Composable
fun SourceLicenseLink() {
  var open by remember { mutableStateOf(false) }
  TextButton(onClick = { open = true }, modifier = Modifier.testTag("source_licenses_link")) {
    Text(stringResource(R.string.source_licenses_label))
  }
  if (open) {
    val context = LocalContext.current
    val text = remember(context) {
      runCatching { context.assets.open("licenses/THIRD_PARTY_NOTICES.txt").bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrNull()
    }
    Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
      Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(stringResource(R.string.source_licenses_label), style = MaterialTheme.typography.headlineSmall)
          val content = text ?: stringResource(R.string.legal_document_error)
          val sections = remember(content) { content.split("\n=== ").mapIndexed { index, section -> if (index == 0) section else "=== $section" } }
          SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize().testTag("source_licenses_content"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              items(sections) { section -> Text(section) }
            }
          }
          Button(onClick = { open = false }, modifier = Modifier.fillMaxWidth().testTag("source_licenses_close")) {
            Text(stringResource(R.string.legal_close_label))
          }
        }
      }
    }
  }
}
