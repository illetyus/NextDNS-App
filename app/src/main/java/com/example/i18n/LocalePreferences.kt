package com.example.i18n

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** UI preferences are separate from account data and survive sign-out. */
object LocalePreferences {
  val supportedLanguages = listOf("en", "tr", "de", "fr", "es")
  const val SYSTEM = "system"
  private const val STORE = "app_language"
  private const val LANGUAGE = "language"

  fun selection(context: Context): String {
    if (Build.VERSION.SDK_INT >= 33) {
      val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
      return if (locales.isEmpty) SYSTEM else locales[0].language
    }
    return context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
      .getString(LANGUAGE, SYSTEM)?.takeIf { it in supportedLanguages } ?: SYSTEM
  }

  fun resolvedLanguage(context: Context): String {
    val selected = selection(context)
    if (selected != SYSTEM) return selected.takeIf { it in supportedLanguages } ?: "en"
    val systemLocales = if (Build.VERSION.SDK_INT >= 33) {
      context.getSystemService(LocaleManager::class.java).systemLocales
    } else Resources.getSystem().configuration.locales
    return (0 until systemLocales.size()).asSequence().map { systemLocales[it].language }
      .firstOrNull { it in supportedLanguages } ?: "en"
  }

  /** Only migrate a pre-Android-13 choice once; never overwrite a later system choice. */
  fun migrateToPlatform(context: Context) {
    if (Build.VERSION.SDK_INT < 33) return
    val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
    if (prefs.getBoolean("platform_migrated", false)) return
    val manager = context.getSystemService(LocaleManager::class.java)
    val saved = prefs.getString(LANGUAGE, SYSTEM)
    if (manager.applicationLocales.isEmpty && saved in supportedLanguages) {
      manager.applicationLocales = LocaleList.forLanguageTags(saved)
    }
    prefs.edit().putBoolean("platform_migrated", true).apply()
  }

  fun setSelection(context: Context, language: String): Boolean {
    require(language == SYSTEM || language in supportedLanguages)
    if (Build.VERSION.SDK_INT >= 33) {
      context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (language == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language)
      return true
    }
    return context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
      .putString(LANGUAGE, language).commit()
  }

  fun localizedContext(context: Context): Context {
    val configuration = Configuration(context.resources.configuration)
    val locale = Locale.forLanguageTag(resolvedLanguage(context))
    configuration.setLocales(LocaleList(locale))
    configuration.setLayoutDirection(locale)
    return context.createConfigurationContext(configuration)
  }
}
