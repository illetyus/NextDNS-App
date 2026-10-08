package com.example.ui.screens

import com.example.ui.viewmodel.NavTab

enum class NavigationLayoutMode {
  COMPACT,
  MEDIUM,
  EXPANDED
}

data class NavigationGroup(
  val title: String,
  val tabs: List<NavTab>
)

fun navigationLayoutForWidth(widthDp: Float): NavigationLayoutMode = when {
  widthDp < 600f -> NavigationLayoutMode.COMPACT
  widthDp < 840f -> NavigationLayoutMode.MEDIUM
  else -> NavigationLayoutMode.EXPANDED
}

val nextDnsNavigationGroups: List<NavigationGroup> = listOf(
  NavigationGroup(
    title = "Genel",
    tabs = listOf(NavTab.SETUP)
  ),
  NavigationGroup(
    title = "Koruma",
    tabs = listOf(NavTab.SECURITY, NavTab.PRIVACY, NavTab.PARENTAL)
  ),
  NavigationGroup(
    title = "Kurallar",
    tabs = listOf(NavTab.DENYLIST, NavTab.ALLOWLIST)
  ),
  NavigationGroup(
    title = "Aktivite",
    tabs = listOf(NavTab.ANALYTICS, NavTab.LOGS)
  ),
  NavigationGroup(
    title = "Ayarlar",
    tabs = listOf(NavTab.SETTINGS)
  ),
  NavigationGroup(
    title = "Hesap",
    tabs = listOf(NavTab.ACCOUNT)
  )
)
