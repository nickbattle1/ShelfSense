package com.example.shelfsense.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shelfsense.R
import com.example.shelfsense.ui.theme.ShelfTheme

// every screen opens with this so the leaf and title land in the same place each time
@Composable
fun ScreenHeader(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val c = ShelfTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                // nudged left so the arrow sits on the content edge but keeps its 48dp touch target
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.ink)
                }
            }
            Image(
                painter = painterResource(R.drawable.logo1),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            if (title != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.ink,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (actionLabel != null) {
                TextButton(onClick = onAction, enabled = actionEnabled) {
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (actionEnabled) c.primary else c.muted
                    )
                }
            }
            actions()
        }
        content()
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val c = ShelfTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = c.primary)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = ShelfTheme.colors.muted,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp).semantics { heading() }
    )
}

@Composable
fun ShelfCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = ShelfTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun CardDivider() {
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = ShelfTheme.colors.line)
}

@Composable
fun DetailRow(label: String, value: String, strong: Boolean = false) {
    val c = ShelfTheme.colors
    val style = if (strong) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = style,
            color = if (strong) c.ink else c.muted,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Text(value, style = style, color = if (strong) c.primary else c.ink, textAlign = TextAlign.End)
    }
}

enum class BannerKind { SUCCESS, WARNING, ERROR, INFO }

@Composable
fun InfoBanner(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    kind: BannerKind = BannerKind.INFO,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    dismissLabel: String? = null,
    onDismiss: () -> Unit = {}
) {
    val c = ShelfTheme.colors
    val background = when (kind) {
        BannerKind.SUCCESS -> c.tint
        BannerKind.WARNING -> c.warnBg
        BannerKind.ERROR -> c.urgentBg
        BannerKind.INFO -> c.infoBg
    }
    val foreground = when (kind) {
        BannerKind.SUCCESS -> c.primary
        BannerKind.WARNING -> c.warn
        BannerKind.ERROR -> c.urgent
        BannerKind.INFO -> c.info
    }
    val icon = when (kind) {
        BannerKind.SUCCESS -> Icons.Filled.CheckCircle
        BannerKind.WARNING -> Icons.Filled.Warning
        BannerKind.ERROR -> Icons.Filled.ErrorOutline
        BannerKind.INFO -> Icons.Filled.Info
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.padding(top = 1.dp).size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = foreground)
                if (body != null) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = foreground,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        if (actionLabel != null || dismissLabel != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (dismissLabel != null) {
                    TextButton(onClick = onDismiss) { Text(dismissLabel, color = foreground) }
                }
                if (actionLabel != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel, color = foreground, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val c = ShelfTheme.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp).background(c.tint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth(0.85f))
        }
    }
}
