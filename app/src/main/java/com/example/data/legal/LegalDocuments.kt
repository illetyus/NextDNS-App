package com.example.data.legal

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

data class LegalBundle(val language: String, val terms: String, val privacy: String, val draft: Boolean)

/** Offline documents; validation failure never supplies a different revision for acceptance. */
object LegalDocuments {
  fun load(context: Context, language: String = context.resources.configuration.locales[0].language): Result<LegalBundle> =
    readBundle(language) { path ->
      context.assets.open("legal/$path").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

  internal fun readBundle(language: String, read: (String) -> String): Result<LegalBundle> = runCatching {
    val selected = language.takeIf { it in listOf("tr", "en", "de", "fr", "es") } ?: "en"
    val registry = JSONObject(read("registry.json"))
    val status = registry.getString("status")
    require(status in listOf("DRAFT", "APPROVED"))
    require(registry.getString("digest_format") == "sha256-utf8-lf")
    require(registry.getString("terms_revision") == LegalAcceptanceStore.CURRENT_TERMS_REVISION)
    val documents = registry.getJSONArray("documents")
    val byPath = (0 until documents.length()).map { documents.getJSONObject(it) }.associateBy { it.getString("path") }
    val expected = listOf("tr", "en", "de", "fr", "es").map { "terms_$it.txt" }.toSet() + "privacy_en.txt"
    require(byPath.keys == expected && documents.length() == expected.size)
    fun document(path: String, revision: String): String {
      val metadata = byPath.getValue(path)
      require(metadata.getString("status") == status)
      val content = read(path).replace("\r\n", "\n")
      val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
      require(digest == metadata.getString("sha256"))
      require("Revision: $revision" in content)
      return content
    }
    LegalBundle(selected, document("terms_$selected.txt", registry.getString("terms_revision")),
      document("privacy_en.txt", registry.getString("privacy_revision")), status == "DRAFT")
  }
}
