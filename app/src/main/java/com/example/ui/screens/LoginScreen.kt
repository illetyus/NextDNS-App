package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ApiConnectionStatus
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable
fun LoginScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivityForSecureLogin() }
  val clipboardManager = LocalClipboardManager.current
  val apiStatus by viewModel.apiStatus.collectAsState()

  var apiKeyInput by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }

  val isLoading = apiStatus is ApiConnectionStatus.Connecting
  val scrollState = rememberScrollState()

  LaunchedEffect(apiStatus) {
    if (apiStatus is ApiConnectionStatus.Connected) {
      apiKeyInput = ""
      isPasswordVisible = false
    }
  }

  DisposableEffect(activity) {
    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // NextDNS Shield Logo + Brand
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "NextDNS Logo",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp)
        )
        Text(
          text = "NextDNS",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "NextDNS Yönetim Paneli",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Login Card
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "API Anahtarı ile Giriş Yap",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )

          Text(
            text = "Hesabınızı bağlamak ve profillerinizi yönetmek için NextDNS API Anahtarınızı girin.",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.5.sp,
              lineHeight = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          // API Key Input
          OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("API Anahtarı", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
            placeholder = { Text("örn. 28df1993bf40d5885cfa", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
              if (apiKeyInput.isNotBlank()) viewModel.saveApiKey(apiKeyInput.trim())
            }),
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, modifier = Modifier.size(24.dp)) {
                  Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                  onClick = {
                    clipboardManager.getText()?.let {
                      apiKeyInput = it.text.trim()
                    }
                  },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.Default.ContentPaste, contentDescription = "Yapıştır", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
              }
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          // Error message if any
          if (apiStatus is ApiConnectionStatus.Error) {
            Text(
              text = (apiStatus as ApiConnectionStatus.Error).message,
              color = MaterialTheme.colorScheme.error,
              fontSize = 11.5.sp
            )
          }

          // Giriş Butonu
          Button(
            onClick = {
              if (apiKeyInput.isNotBlank()) {
                viewModel.saveApiKey(apiKeyInput.trim())
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isLoading && apiKeyInput.isNotBlank()
          ) {
            if (isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
              Text("Giriş Yap", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
          }

          // Misafir / Demo Modu Butonu
          OutlinedButton(
            onClick = { viewModel.enterGuestMode() },
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurface
            )
          ) {
            Text("Demo / Misafir Modu ile Keşfet", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline)

          // API Anahtarı Nasıl Alınır Linki
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .bounceClick {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://my.nextdns.io/account"))
                context.startActivity(intent)
              }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = "API Anahtarınızı my.nextdns.io/account adresinden alın",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
          }
        }
      }
    }
  }
}
