package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Android 16 Tactile Spring Bounce Modifier:
 * Provides fluid squishy spring feedback and haptic vibration on touch.
 */
@Composable
fun Modifier.bounceClick(
  scaleDown: Float = 0.94f,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null
): Modifier {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) scaleDown else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "bounceScale"
  )

  val clickableModifier = if (onClick != null) {
    Modifier.clickable(
      interactionSource = interactionSource,
      indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
      enabled = enabled,
      onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick.invoke()
      }
    )
  } else {
    Modifier.clickable(
      interactionSource = interactionSource,
      indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
      enabled = enabled,
      onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
    )
  }

  return this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .then(clickableModifier)
}

/**
 * Simple spring scale on press for buttons and components
 */
@Composable
fun Modifier.pressScale(
  isPressed: Boolean,
  scaleDown: Float = 0.95f
): Modifier {
  val scale by animateFloatAsState(
    targetValue = if (isPressed) scaleDown else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "pressScale"
  )
  return this.graphicsLayer {
    scaleX = scale
    scaleY = scale
  }
}

/**
 * Android 16 Expressive Live Status Pulsing Beacon:
 * Smooth animated concentric wave indicating active network protection.
 */
@Composable
fun StatusBeacon(
  color: Color = MaterialTheme.colorScheme.tertiary,
  size: Dp = 10.dp,
  isPulsing: Boolean = true,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "beaconPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isPulsing) 2.4f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = LinearOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseScale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = LinearOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseAlpha"
  )

  Box(
    modifier = modifier
      .size(size * 2.5f)
      .drawBehind {
        if (isPulsing) {
          drawCircle(
            color = color.copy(alpha = pulseAlpha),
            radius = (size.toPx() / 2f) * pulseScale
          )
        }
        drawCircle(
          color = color,
          radius = size.toPx() / 2f
        )
      }
  )
}

/**
 * Android 16 Expressive Card with fluid rounded curves, subtle ambient border & tonal depth
 */
@Composable
fun NextDnsCard(
  modifier: Modifier = Modifier,
  title: String? = null,
  subtitle: String? = null,
  isBeta: Boolean = false,
  isActiveHighlight: Boolean = false,
  headerAction: @Composable (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val borderColor by animateColorAsState(
    targetValue = if (isActiveHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "cardBorderColor"
  )

  val cardBg = if (isActiveHighlight) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
    shadowElevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      if (title != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (subtitle != null) 6.dp else 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = (-0.2).sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (isBeta) {
              NextDnsBetaBadge()
            }
          }

          if (headerAction != null) {
            headerAction()
          }
        }
      }

      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.5.sp,
            lineHeight = 17.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 14.dp)
        )
      }

      content()
    }
  }
}

/**
 * Android 16 Expressive Setting Row with animated state feedback, tactile toggle and haptics
 */
@Composable
fun NextDnsSettingToggle(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  isBeta: Boolean = false,
  actionButton: @Composable (() -> Unit)? = null
) {
  val haptic = LocalHapticFeedback.current
  val animatedBgColor by animateColorAsState(
    targetValue = if (checked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "settingBgColor"
  )

  val animatedBorderColor by animateColorAsState(
    targetValue = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "settingBorderColor"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, animatedBorderColor, RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    color = animatedBgColor,
    shadowElevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = if (subtitle != null) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (isBeta) {
            NextDnsBetaBadge()
          }
        }
      }

      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.5.sp,
            lineHeight = 17.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 14.dp)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onCheckedChange(!checked)
            }
            .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
          NextDnsSwitch(
            checked = checked,
            onCheckedChange = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onCheckedChange(it)
            }
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = if (checked) "$title etkin" else "$title etkinleştir",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.5.sp,
              fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (actionButton != null) {
          actionButton()
        }
      }
    }
  }
}

/**
 * Reusable Setting Toggle Row (compact within cards)
 */
@Composable
fun NextDnsSettingToggleRow(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .bounceClick { onCheckedChange(!checked) }
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.5.sp,
          lineHeight = 16.sp
        )
      }
    }
    NextDnsSwitch(
      checked = checked,
      onCheckedChange = onCheckedChange
    )
  }
}

/**
 * Reusable Checkbox Row with label and explanation
 */
@Composable
fun NextDnsCheckboxRow(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .bounceClick { onCheckedChange(!checked) }
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.primary,
        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
    Column {
      Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.5.sp
        )
      }
    }
  }
}

/**
 * Reusable Dropdown Selector Component
 */
@Composable
fun NextDnsDropdownSelector(
  label: String,
  selectedValue: String,
  options: List<String>,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = label,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
    Box {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
          .bounceClick { expanded = true },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = selectedValue,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
          )
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
      ) {
        options.forEach { option ->
          DropdownMenuItem(
            text = { Text(option, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
            onClick = {
              onOptionSelected(option)
              expanded = false
            }
          )
        }
      }
    }
  }
}

/**
 * Reusable Action/Alert Card with styled button and description (e.g. Danger or Copy)
 */
@Composable
fun NextDnsActionCard(
  buttonText: String,
  description: String,
  onButtonClick: () -> Unit,
  modifier: Modifier = Modifier,
  isDanger: Boolean = false
) {
  val btnColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
  val onBtnColor = if (isDanger) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
  val borderColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onButtonClick,
        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
      ) {
        Text(
          text = buttonText,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = onBtnColor
        )
      }
      Text(
        text = description,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
      )
    }
  }
}

/**
 * Android 16 Expressive Custom Switch with fluid spring response
 */
@Composable
fun NextDnsSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current

  Switch(
    checked = checked,
    onCheckedChange = {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onCheckedChange(it)
    },
    modifier = modifier,
    thumbContent = if (checked) {
      {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(12.dp),
          tint = MaterialTheme.colorScheme.primary
        )
      }
    } else null,
    colors = SwitchDefaults.colors(
      checkedThumbColor = Color.White,
      checkedTrackColor = MaterialTheme.colorScheme.primary,
      checkedBorderColor = MaterialTheme.colorScheme.primary,
      uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
      uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
      uncheckedBorderColor = MaterialTheme.colorScheme.outline
    )
  )
}

/**
 * Android 16 Expressive Primary Solid Button with Spring Bounce & Ripple
 */
@Composable
fun NextDnsButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  containerColor: Color = MaterialTheme.colorScheme.primary,
  contentColor: Color = Color.White,
  enabled: Boolean = true
) {
  val haptic = LocalHapticFeedback.current

  Button(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    modifier = modifier
      .height(40.dp)
      .bounceClick(scaleDown = 0.95f, enabled = enabled, onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = MaterialTheme.colorScheme.outline,
      disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    enabled = enabled
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        letterSpacing = 0.2.sp
      )
    )
  }
}

/**
 * Android 16 Expressive Outline Button with Spring Bounce
 */
@Composable
fun NextDnsOutlineButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  borderColor: Color = MaterialTheme.colorScheme.primary,
  contentColor: Color = MaterialTheme.colorScheme.primary,
  icon: ImageVector? = null
) {
  val haptic = LocalHapticFeedback.current

  OutlinedButton(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    modifier = modifier
      .height(38.dp)
      .bounceClick(scaleDown = 0.95f, onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = contentColor
    ),
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
      )
    )
  }
}

/**
 * BETA Pill Badge
 */
@Composable
fun NextDnsBetaBadge(modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.tertiary,
    modifier = modifier
  ) {
    Text(
      text = "BETA",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onTertiary,
        fontSize = 9.sp,
        letterSpacing = 0.4.sp
      ),
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

/**
 * Recommended Pill Badge
 */
@Composable
fun NextDnsRecommendedBadge(modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier
  ) {
    Text(
      text = "ÖNERİLEN",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 9.sp,
        letterSpacing = 0.4.sp
      ),
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
    )
  }
}

/**
 * Android 16 Expressive Info Banner
 */
@Composable
fun NextDnsInfoBanner(
  text: String,
  modifier: Modifier = Modifier,
  isSuccess: Boolean = false,
  isWarning: Boolean = false
) {
  val borderColor = when {
    isSuccess -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    isWarning -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
    else -> MaterialTheme.colorScheme.outline
  }
  val iconColor = when {
    isSuccess -> MaterialTheme.colorScheme.tertiary
    isWarning -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.5.sp,
          lineHeight = 17.sp
        ),
        color = if (isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

/**
 * Copy to Clipboard Helper with Toast and Sound
 */
fun copyToClipboard(context: Context, text: String, label: String = "Kopyalandı") {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(label, text)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, "$label kopyalandı", Toast.LENGTH_SHORT).show()
}

// Backward compatibility alias for CyberSectionCard
@Composable
fun CyberSectionCard(
  modifier: Modifier = Modifier,
  title: String? = null,
  subtitle: String? = null,
  badgeText: String? = null,
  badgeColor: Color = MaterialTheme.colorScheme.primary,
  icon: ImageVector? = null,
  headerAction: @Composable (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  NextDnsCard(
    modifier = modifier,
    title = title,
    subtitle = subtitle,
    isBeta = badgeText == "BETA",
    headerAction = headerAction,
    content = content
  )
}
