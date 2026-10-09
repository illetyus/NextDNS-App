package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.i18n.AppStrings
import com.example.i18n.LocalePreferences

private fun Context.activity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.activity()
  else -> null
}

@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var expanded by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf(false) }
  val choices = linkedMapOf(
    LocalePreferences.SYSTEM to AppStrings.get(R.string.system_language),
    "tr" to "Türkçe", "en" to "English", "de" to "Deutsch", "fr" to "Français", "es" to "Español"
  )
  val selected = LocalePreferences.selection(context)
  Column(modifier) {
    Text(AppStrings.get(R.string.language), style = MaterialTheme.typography.titleSmall)
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
      Text(choices[selected] ?: AppStrings.get(R.string.system_language))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      choices.forEach { (language, label) ->
        DropdownMenuItem(text = { Text(label) }, onClick = {
          expanded = false
          if (language != selected) {
            error = !LocalePreferences.setSelection(context, language)
            if (!error && Build.VERSION.SDK_INT < 33) context.activity()?.recreate()
          }
        })
      }
    }
    if (error) Text(AppStrings.get(R.string.language_save_error), color = MaterialTheme.colorScheme.error)
  }
}
