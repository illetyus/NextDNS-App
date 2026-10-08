package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveNavigationTest {

  @Test
  fun widthPolicy_usesCompactBelow600Dp() {
    assertEquals(
      NavigationLayoutMode.COMPACT,
      navigationLayoutForWidth(599f)
    )
  }

  @Test
  fun widthPolicy_usesMediumFrom600To839Dp() {
    assertEquals(
      NavigationLayoutMode.MEDIUM,
      navigationLayoutForWidth(600f)
    )
    assertEquals(
      NavigationLayoutMode.MEDIUM,
      navigationLayoutForWidth(839f)
    )
  }

  @Test
  fun widthPolicy_usesExpandedAt840DpAndAbove() {
    assertEquals(
      NavigationLayoutMode.EXPANDED,
      navigationLayoutForWidth(840f)
    )
    assertEquals(
      NavigationLayoutMode.EXPANDED,
      navigationLayoutForWidth(1200f)
    )
  }

  @Test
  fun navigationGroups_coverEveryTabExactlyOnce() {
    val groupedTabs = nextDnsNavigationGroups.flatMap { it.tabs }

    assertEquals(NavTab.values().toSet(), groupedTabs.toSet())
    assertEquals(NavTab.values().size, groupedTabs.size)
  }
}
