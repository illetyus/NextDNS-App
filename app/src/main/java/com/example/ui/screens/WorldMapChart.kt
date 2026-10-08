package com.example.ui.screens

import com.example.R
import com.example.i18n.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WorldMapChart(
  countryData: List<Pair<String, Double>>,
  modifier: Modifier = Modifier
) {
  if (countryData.isEmpty()) {
    Text(
      text = AppStrings.get(R.string.ui_e07464ef33),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp,
      modifier = modifier.padding(vertical = 12.dp)
    )
    return
  }

  val normalized = countryData
    .filter { (_, percentage) -> percentage.isFinite() && percentage >= 0.0 }
    .sortedByDescending { it.second }
    .take(8)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    normalized.forEach { (country, percentage) ->
      val progress = (percentage / 100.0)
        .coerceIn(0.0, 1.0)
        .toFloat()

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = country,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = "%" + "%.1f".format(percentage),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
