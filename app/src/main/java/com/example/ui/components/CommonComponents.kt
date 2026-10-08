package com.example.ui.components

import com.example.i18n.UiLabels

import com.example.R
import com.example.i18n.AppStrings

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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Subtle press feedback for custom clickable surfaces.
 * Uses a non-bouncy spring and keeps motion intentionally small.
 */
@Composable
fun Modifier.bounceClick(
  scaleDown: Float = 0.98f,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null
): Modifier {
  if (onClick == null) return this

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) scaleDown else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "bounceScale"
  )

  return this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .clickable(
      interactionSource = interactionSource,
      indication = ripple(
        bounded = true,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
      ),
      enabled = enabled,
      onClick = onClick
    )
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
            .toggleable(
              value = checked,
              role = Role.Switch,
              onValueChange = onCheckedChange
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp)
        ) {
          NextDnsSwitch(
            checked = checked,
            onCheckedChange = null
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = if (checked) "$title etkin" else AppStrings.get(R.string.enable_setting, title),
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
      .toggleable(
        value = checked,
        role = Role.Switch,
        onValueChange = onCheckedChange
      )
      .heightIn(min = 48.dp)
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
      onCheckedChange = null
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
      .toggleable(
        value = checked,
        role = Role.Checkbox,
        onValueChange = onCheckedChange
      )
      .heightIn(min = 48.dp)
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = null,
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
          .heightIn(min = 48.dp)
          .bounceClick(scaleDown = 0.99f) { expanded = true },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = UiLabels.canonical(selectedValue),
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
            text = { Text(UiLabels.canonical(option), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
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
        modifier = Modifier.heightIn(min = 48.dp)
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
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier
) {
  Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
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

@Immutable
data class NextDnsButtonColors(
  val containerColor: Color,
  val contentColor: Color
)

@Immutable
data class NextDnsOutlineButtonColors(
  val borderColor: Color,
  val contentColor: Color
)

object NextDnsButtonDefaults {
  @Composable
  fun buttonColors(
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White
  ): NextDnsButtonColors = NextDnsButtonColors(
    containerColor = containerColor,
    contentColor = contentColor
  )

  @Composable
  fun outlineButtonColors(
    borderColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.primary
  ): NextDnsOutlineButtonColors = NextDnsOutlineButtonColors(
    borderColor = borderColor,
    contentColor = contentColor
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
  colors: NextDnsButtonColors = NextDnsButtonDefaults.buttonColors(),
  enabled: Boolean = true
) {
  Button(
    onClick = onClick,
    modifier = modifier.heightIn(min = 48.dp),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = colors.containerColor,
      contentColor = colors.contentColor,
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
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold
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
  colors: NextDnsOutlineButtonColors = NextDnsButtonDefaults.outlineButtonColors(),
  icon: ImageVector? = null
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = 48.dp),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      colors.borderColor.copy(alpha = 0.7f)
    ),
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = colors.contentColor
    ),
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
      )
    )
  }
}



enum class BadgeStyle {
  BETA,
  RECOMMENDED
}

/**
 * Unified Pill Badge
 */
@Composable
fun NextDnsBadge(
  text: String,
  style: BadgeStyle,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor, fontWeight, padH, padV) = when (style) {
    BadgeStyle.BETA -> BadgeConfig(
      containerColor = MaterialTheme.colorScheme.tertiary,
      contentColor = MaterialTheme.colorScheme.onTertiary,
      fontWeight = FontWeight.Black,
      horizontalPadding = 6.dp,
      verticalPadding = 2.dp
    )
    BadgeStyle.RECOMMENDED -> BadgeConfig(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.Bold,
      horizontalPadding = 7.dp,
      verticalPadding = 2.5.dp
    )
  }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = bgColor,
    modifier = modifier
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = fontWeight,
        color = textColor,
        fontSize = 9.sp,
        letterSpacing = 0.4.sp
      ),
      modifier = Modifier.padding(horizontal = padH, vertical = padV)
    )
  }
}

@Immutable
private data class BadgeConfig(
  val containerColor: Color,
  val contentColor: Color,
  val fontWeight: FontWeight,
  val horizontalPadding: Dp,
  val verticalPadding: Dp
)

/**
 * BETA Pill Badge (Wrapper for backwards compatibility)
 */
@Composable
fun NextDnsBetaBadge(modifier: Modifier = Modifier) {
  NextDnsBadge(
    text = "BETA",
    style = BadgeStyle.BETA,
    modifier = modifier
  )
}

/**
 * Recommended Pill Badge (Wrapper for backwards compatibility)
 */
@Composable
fun NextDnsRecommendedBadge(modifier: Modifier = Modifier) {
  NextDnsBadge(
    text = AppStrings.get(R.string.ui_4ce78923da),
    style = BadgeStyle.RECOMMENDED,
    modifier = modifier
  )
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
fun copyToClipboard(context: Context, text: String, label: String = AppStrings.get(R.string.ui_02af74b2f0)) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(label, text)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, AppStrings.get(R.string.copied_value, label), Toast.LENGTH_SHORT).show()
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
