package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = SageOnPrimaryContainerDark,
    secondary = MossSecondaryDark,
    onSecondary = MossOnSecondaryDark,
    secondaryContainer = MossSecondaryContainerDark,
    onSecondaryContainer = MossOnSecondaryContainerDark,
    tertiary = EarthTertiary,
    background = NatureBackgroundDark,
    onBackground = NatureOnBackgroundDark,
    surface = NatureSurfaceDark,
    onSurface = NatureOnSurfaceDark,
    surfaceVariant = NatureSurfaceVariantDark,
    onSurfaceVariant = NatureOnSurfaceVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = MossSecondary,
    onSecondary = MossOnSecondary,
    secondaryContainer = MossSecondaryContainer,
    onSecondaryContainer = MossOnSecondaryContainer,
    tertiary = EarthTertiary,
    onTertiary = EarthOnTertiary,
    tertiaryContainer = EarthTertiaryContainer,
    onTertiaryContainer = EarthOnTertiaryContainer,
    background = NatureBackground,
    onBackground = NatureOnBackground,
    surface = NatureSurface,
    onSurface = NatureOnSurface,
    surfaceVariant = NatureSurfaceVariant,
    onSurfaceVariant = NatureOnSurfaceVariant,
    outline = NatureOutline,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
