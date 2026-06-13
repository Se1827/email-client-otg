package com.se1827.emailclient

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.se1827.emailclient.ui.theme.EmailClientTheme

// ═══════════════════════════════════════════════════════════════════
// PHASE 2 — DATA MODEL
// Extracted from SmartCard.jsx: every field shown on any card type.
// ═══════════════════════════════════════════════════════════════════

/**
 * Identifies which card template to render.
 * Maps 1-to-1 with the web app's detectScenario() output values.
 */
enum class CardScenario {
    Flight, Meeting, GoodNews, Finance, Code, Task, Spam, Newsletter, Alert, Default
}

/**
 * Unified data model for a SmartCard email.
 *
 * In the web version, fields like [pnr], [meetingTime], [amount], [bankName]
 * and [links] are extracted at render time from the raw email body via regex.
 * In this Kotlin port they are pre-populated by the server-side classifier
 * (or by mock data during development) so the composables are pure-UI with
 * no string-parsing logic.
 *
 * @param id           Stable unique identifier (LazyColumn key).
 * @param sender       Raw sender string, e.g. "user@airline.com" or a display name.
 *                     If it contains '@', only the local-part is shown (.sc-card-sender).
 * @param subject      Email subject — rendered as the card title.
 * @param bodyPreview  First ~100 chars of the body for preview text.
 * @param scenario     Detected scenario — drives card variant selection.
 * @param pnr          Flight PNR code (Flight cards).
 * @param meetingTime  Human-readable time, e.g. "3:30 PM" (Meeting cards).
 * @param amount       Formatted amount, e.g. "₹1,25,000" (Finance cards).
 * @param bankName     Payment entity name (Finance cards).
 * @param links        Up to 3 URLs extracted from the email body.
 * @param isSpam       When true, link rows render as "Blocked" (Spam cards).
 */
data class SmartCardEmail(
    val id: String,
    val sender: String,
    val subject: String,
    val bodyPreview: String,
    val scenario: CardScenario,
    val pnr: String? = null,
    val meetingTime: String? = null,
    val amount: String? = null,
    val bankName: String? = null,
    val links: List<String> = emptyList(),
    val isSpam: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════
// PHASE 3 — DESIGN TOKENS
// Translated 1-to-1 from SmartCard.css into Compose Color/dp/sp.
// Intentionally hard-coded: this feature uses an explicit dark
// glassmorphism palette that has no equivalent in the app's current
// MaterialTheme / Material-You dynamic colour system.
// ═══════════════════════════════════════════════════════════════════

private val ScreenBg          = Color(0xFF0A0B14)   // page bg
private val CardBorderColor   = Color(0x12FFFFFF)   // rgba(255,255,255,0.07)

// Text hierarchy  (.sc-card-title / .sc-card-sender / .sc-card-label)
private val TextPrimary       = Color(0xFFFFFFFF)
private val TextSecondary     = Color(0x99FFFFFF)   // rgba(255,255,255,0.60)
private val TextMuted         = Color(0x66FFFFFF)   // rgba(255,255,255,0.40)

// Neutral chip  (.sc-chip--neutral)
private val ChipNeutralBg     = Color(0x0DFFFFFF)   // rgba(255,255,255,0.05)
private val ChipNeutralBorder = Color(0x14FFFFFF)   // rgba(255,255,255,0.08)

/**
 * Encapsulates the three colour values that define a button variant.
 *
 * CSS hover transform (translateY / box-shadow) has no direct mobile
 * equivalent; Compose ripple indication provides tactile pressed feedback.
 */
private data class BtnStyle(val bg: Color, val text: Color, val border: Color)

// .sc-action-btn--primary (indigo)
private val BtnPrimary = BtnStyle(Color(0x266366F1), Color(0xFFA5B4FC), Color(0x406366F1))
// .sc-action-btn--success (green)
private val BtnSuccess = BtnStyle(Color(0x1F10B981), Color(0xFF34D399), Color(0x3810B981))
// .sc-action-btn--ghost
private val BtnGhost   = BtnStyle(Color(0x0AFFFFFF), Color(0x8CFFFFFF), Color(0x14FFFFFF))

// Link warning section  (.sc-link-section)
private val LinkSectionBg     = Color(0x0FF59E0B)   // rgba(245,158,11,0.06)
private val LinkSectionBorder = Color(0x26F59E0B)   // rgba(245,158,11,0.15)
private val AmberLabel        = Color(0xFFFBBF24)
private val AmberIcon         = Color(0xFFF59E0B)
private val LinkUrlColor      = Color(0x59FFFFFF)   // rgba(255,255,255,0.35)
private val SpamRed           = Color(0xFFEF4444)

// ── Per-scenario theme (web: getBrandTheme()) ─────────────────────────────

private data class ScenarioTheme(
    val accent: Color,   // left-border + chip accent
    val iconBg: Color,   // icon badge bg (~10 % opacity of accent)
    val cardBg: Color    // card container bg (tinted dark)
)

private fun scenarioTheme(scenario: CardScenario): ScenarioTheme = when (scenario) {
    CardScenario.Flight     -> ScenarioTheme(Color(0xFF60A5FA), Color(0x1A60A5FA), Color(0xFF0D1424))
    CardScenario.Meeting    -> ScenarioTheme(Color(0xFFA78BFA), Color(0x1AA78BFA), Color(0xFF120F28))
    CardScenario.GoodNews   -> ScenarioTheme(Color(0xFF34D399), Color(0x1A34D399), Color(0xFF0C1E17))
    CardScenario.Finance    -> ScenarioTheme(Color(0xFFFBBF24), Color(0x1AFBBF24), Color(0xFF1C160A))
    CardScenario.Code       -> ScenarioTheme(Color(0xFF818CF8), Color(0x1A818CF8), Color(0xFF0F1023))
    CardScenario.Task       -> ScenarioTheme(Color(0xFFF472B6), Color(0x1AF472B6), Color(0xFF1C0D1A))
    CardScenario.Spam       -> ScenarioTheme(Color(0xFFEF4444), Color(0x1AEF4444), Color(0xFF1C0A0A))
    CardScenario.Newsletter -> ScenarioTheme(Color(0xFF38BDF8), Color(0x1A38BDF8), Color(0xFF0B1A26))
    CardScenario.Alert      -> ScenarioTheme(Color(0xFFF59E0B), Color(0x1AF59E0B), Color(0xFF1C1408))
    CardScenario.Default    -> ScenarioTheme(Color(0xFF94A3B8), Color(0x1A94A3B8), Color(0xFF111827))
}

/** Maps CardScenario to the nearest Material Icon equivalent of the Lucide icons in the web. */
private fun scenarioIcon(scenario: CardScenario): ImageVector = when (scenario) {
    CardScenario.Flight     -> Icons.Filled.Flight          // web: Plane
    CardScenario.Meeting    -> Icons.Filled.CalendarMonth   // web: CalendarDays
    CardScenario.GoodNews   -> Icons.Filled.CheckCircle     // web: CheckCircle2
    CardScenario.Alert      -> Icons.Filled.Warning         // web: AlertTriangle
    CardScenario.Finance    -> Icons.Filled.CreditCard      // web: CreditCard
    CardScenario.Code       -> Icons.Filled.Code            // web: GitPullRequest
    CardScenario.Task       -> Icons.Filled.Assignment      // web: ClipboardList
    CardScenario.Spam       -> Icons.Filled.ReportProblem   // web: ShieldAlert
    CardScenario.Newsletter -> Icons.Filled.Article         // web: Newspaper
    CardScenario.Default    -> Icons.Filled.Email           // web: Mail
}

private fun scenarioLabel(scenario: CardScenario): String = when (scenario) {
    CardScenario.Flight     -> "FLIGHT / TRAVEL"
    CardScenario.Meeting    -> "MEETING / EVENT"
    CardScenario.GoodNews   -> "GOOD NEWS"
    CardScenario.Alert      -> "OFFICIAL NOTICE"
    CardScenario.Finance    -> "FINANCE / INVOICE"
    CardScenario.Code       -> "DEVELOPMENT / CODE"
    CardScenario.Task       -> "TASK / TO-DO"
    CardScenario.Spam       -> "SYSTEM WARNING: SPAM"
    CardScenario.Newsletter -> "NEWSLETTER / DIGEST"
    CardScenario.Default    -> "EMAIL"
}

// ═══════════════════════════════════════════════════════════════════
// SUB-COMPONENTS
// Direct Kotlin/Compose equivalents of the CSS chip, button, and
// SafeLink sub-components from SmartCard.jsx.
// ═══════════════════════════════════════════════════════════════════

/** Accent-tinted chip (.sc-chip with inline accent styles from JSX). */
@Composable
private fun ScenarioChip(
    text: String,
    accent: Color,
    iconBg: Color,
    leadingIcon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(iconBg)
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, tint = accent, modifier = Modifier.size(10.dp))
        }
        Text(text, color = accent, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Neutral chip (.sc-chip--neutral). */
@Composable
private fun NeutralChip(text: String, leadingIcon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ChipNeutralBg)
            .border(1.dp, ChipNeutralBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(10.dp))
        }
        Text(text, color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Styled action button (.sc-action-btn from CSS).
 *
 * @param style One of [BtnPrimary], [BtnSuccess], [BtnGhost], or a custom [BtnStyle].
 */
@Composable
private fun ActionButton(
    text: String,
    style: BtnStyle,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(style.bg)
            .border(1.dp, style.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, tint = style.text, modifier = Modifier.size(11.dp))
        }
        Text(text, color = style.text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        trailingIcon?.let {
            Icon(it, contentDescription = null, tint = style.text, modifier = Modifier.size(11.dp))
        }
    }
}

/**
 * Two-step safe link row (web: <SafeLink> component in SmartCard.jsx).
 *
 * Step 1 — Renders URL + "Verify & Open →" button.
 * Step 2 — After tap, button becomes "Open ↗" which fires the system browser.
 *
 * [isSpam]=true renders the URL struck-through with a red "Blocked" badge instead
 * of offering any open action.
 */
@Composable
private fun SafeLinkRow(url: String, isSpam: Boolean = false) {
    var confirmed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shortUrl = url.removePrefix("https://").removePrefix("http://")
        .removePrefix("www.").take(40)
    val ellipsis = if (url.length > 43) "…" else ""

    // ── Spam variant: blocked ────────────────────────────────────────────────
    if (isSpam) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.ReportProblem, null,
                tint = SpamRed, modifier = Modifier.size(12.dp)
            )
            Text(
                text = "$shortUrl$ellipsis",
                color = SpamRed.copy(alpha = 0.75f),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                textDecoration = TextDecoration.LineThrough,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x26EF4444))
                    .border(1.dp, Color(0x4DEF4444), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Blocked", color = SpamRed, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // ── Normal variant: two-step verify-then-open ────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Shield, null, tint = AmberIcon, modifier = Modifier.size(12.dp))
        Text(
            text = "$shortUrl$ellipsis",
            color = LinkUrlColor,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!confirmed) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x1AF59E0B))
                    .border(1.dp, Color(0x40F59E0B), RoundedCornerShape(6.dp))
                    .clickable {
                        confirmed = true
                        Log.d("SmartCards", "URL verified by user: $url")
                        // TODO: Show a bottom-sheet confirmation before opening in production
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Verify & Open →", color = AmberLabel, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x1A10B981))
                    .border(1.dp, Color(0x4010B981), RoundedCornerShape(6.dp))
                    .clickable {
                        Log.d("SmartCards", "Opening verified URL: $url")
                        // TODO: Route through an in-app WebView for sandboxed browsing
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Open ↗", color = Color(0xFF34D399), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Amber-bordered link warning section (.sc-link-section from CSS).
 * Renders up to 2 [SafeLinkRow] entries beneath a labelled warning header.
 */
@Composable
private fun LinkSection(label: String, links: List<String>, isSpam: Boolean = false) {
    if (links.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSpam) Color(0x1AEF4444) else LinkSectionBg)
            .border(
                1.dp,
                if (isSpam) Color(0x4DEF4444) else LinkSectionBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                if (isSpam) Icons.Filled.ReportProblem else Icons.Filled.Warning,
                null,
                tint = if (isSpam) SpamRed else AmberLabel,
                modifier = Modifier.size(10.dp)
            )
            Text(
                label,
                color = if (isSpam) SpamRed else AmberLabel,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04.sp
            )
        }
        links.take(2).forEach { SafeLinkRow(url = it, isSpam = isSpam) }
    }
}

// ═══════════════════════════════════════════════════════════════════
// CARD SHELL
// Shared structural layout for all card variants (.sc-card in CSS).
// Slot parameters allow each variant to inject its body + actions.
//
// Left accent strip technique:
//   Row(IntrinsicSize.Min) forces the Row to derive its height from
//   the tallest child (the content Column). The 3dp accent Box then
//   uses fillMaxHeight() to span the full card height — giving a
//   full-height left border that is clipped to the card's corner radius.
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun SmartCardShell(
    theme: ScenarioTheme,
    scenario: CardScenario,
    sender: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val label         = scenarioLabel(scenario)
    val icon          = scenarioIcon(scenario)
    val senderDisplay = if ('@' in sender) sender.substringBefore('@') else sender

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.cardBg)
            .border(1.dp, CardBorderColor, RoundedCornerShape(18.dp))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // ── Left accent strip (3 dp, full card height) ──────────────
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(theme.accent)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Header (.sc-card-header) ────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Icon badge (.sc-card-icon)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(theme.iconBg)
                            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = theme.accent, modifier = Modifier.size(15.dp))
                    }

                    // Meta (.sc-card-meta)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // .sc-card-label — Spam uses red per web inline style override
                        Text(
                            text = label,
                            color = if (scenario == CardScenario.Spam) SpamRed else TextMuted,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp
                        )
                        // .sc-card-sender
                        Text(
                            text = senderDisplay,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Dismiss (.sc-dismiss)
                    // CSS: opacity:0 on card, reveals on hover → no hover on mobile;
                    // substituted with always-visible muted icon. Tap triggers dismiss.
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                Log.d("SmartCards", "Card dismissed — scenario=${scenario.name} sender=$sender")
                                // TODO: Persist dismissal per email.id via DataStore so card
                                //       does not reappear after inbox refresh
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Close, "Dismiss", tint = TextMuted, modifier = Modifier.size(13.dp))
                    }
                }

                // ── Body slot ──────────────────────────────────────────
                content()

                // ── Actions slot (.sc-card-actions) ───────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        }
    }
}

// ── Typography helpers ────────────────────────────────────────────────────

/** .sc-card-title — 13 sp, bold, 2-line clamp */
@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 18.2.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/** .sc-card-body-text — 11.5 sp, secondary colour, 2-line clamp */
@Composable
private fun CardBodyText(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

// ═══════════════════════════════════════════════════════════════════
// CARD VARIANTS
// Each function is a 1-to-1 translation of a card template from JSX.
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun FlightCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Flight)
    SmartCardShell(
        theme = t, scenario = CardScenario.Flight,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                email.pnr?.let { ScenarioChip("PNR: $it", t.accent, t.iconBg) }
                NeutralChip("Booking", Icons.Filled.Flight)
            }
            if (email.links.isNotEmpty()) LinkSection("External link — verify before opening", email.links)
        },
        actions = {
            ActionButton("View Booking", BtnPrimary, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "View Booking clicked — id=${email.id}")
                // TODO: navController.navigate("email_detail/${email.id}")
                onNavigate()
            }
        }
    )
}

@Composable
private fun MeetingCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Meeting)
    val accentBtn = BtnStyle(t.iconBg, t.accent, t.accent.copy(alpha = 0.30f))
    SmartCardShell(
        theme = t, scenario = CardScenario.Meeting,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                email.meetingTime?.let { ScenarioChip(it, t.accent, t.iconBg, Icons.Filled.Schedule) }
                NeutralChip("Invite", Icons.Filled.Group)
            }
            if (email.links.isNotEmpty()) LinkSection("Join Link", email.links)
        },
        actions = {
            ActionButton("Add to Calendar", accentBtn, leadingIcon = Icons.Filled.CalendarMonth) {
                Log.d("SmartCards", "Add to Calendar clicked — id=${email.id}")
                // TODO: Insert event via CalendarContract.Events or deep-link to Google Calendar
            }
            ActionButton("View Event", BtnGhost, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "View Event clicked — id=${email.id}")
                onNavigate()
            }
        }
    )
}

@Composable
private fun GoodNewsCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.GoodNews)
    SmartCardShell(
        theme = t, scenario = CardScenario.GoodNews,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
        },
        actions = {
            ActionButton("Got it", BtnSuccess, leadingIcon = Icons.Filled.CheckCircle) {
                Log.d("SmartCards", "Got it clicked — id=${email.id}")
                // TODO: Mark email acknowledged in backend; remove from smart card feed
                onDismiss()
            }
            ActionButton("View Email", BtnGhost, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "View Email (GoodNews) clicked — id=${email.id}")
                onNavigate()
            }
        }
    )
}

@Composable
private fun AlertCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Alert)
    SmartCardShell(
        theme = t, scenario = CardScenario.Alert,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
            if (email.links.isNotEmpty()) LinkSection("Verification Link", email.links)
        },
        actions = {
            ActionButton("View Alert", BtnPrimary, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "View Alert clicked — id=${email.id}")
                // TODO: Navigate to EmailDetailScreen with alert context highlighted
                onNavigate()
            }
        }
    )
}

@Composable
private fun FinanceCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Finance)
    val accentBtn = BtnStyle(t.iconBg, t.accent, t.accent.copy(alpha = 0.30f))
    SmartCardShell(
        theme = t, scenario = CardScenario.Finance,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                email.amount?.let { ScenarioChip("Amount: $it", t.accent, t.iconBg) }
                email.bankName?.let { NeutralChip(it, Icons.Filled.CreditCard) }
            }
            if (email.links.isNotEmpty()) LinkSection("Invoice Link", email.links)
        },
        actions = {
            ActionButton("Pay Invoice", accentBtn, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "Pay Invoice clicked — id=${email.id}, amount=${email.amount}")
                // TODO: Deep-link to payment gateway; show payment confirmation dialog
                onNavigate()
            }
        }
    )
}

@Composable
private fun CodeCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Code)
    SmartCardShell(
        theme = t, scenario = CardScenario.Code,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
            if (email.links.isNotEmpty()) LinkSection("Repo / PR Link", email.links)
        },
        actions = {
            ActionButton("Review PR", BtnPrimary, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "Review PR clicked — id=${email.id}")
                // TODO: Open PR URL via SafeLinkRow flow, or navigate to in-app code review screen
                onNavigate()
            }
        }
    )
}

@Composable
private fun TaskCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Task)
    SmartCardShell(
        theme = t, scenario = CardScenario.Task,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
        },
        actions = {
            ActionButton("Complete Task", BtnPrimary, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "Complete Task clicked — id=${email.id}")
                // TODO: Mark task complete via task-management API; navigate to task detail
                onNavigate()
            }
        }
    )
}

@Composable
private fun SpamCard(
    email: SmartCardEmail,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigate: () -> Unit
) {
    val t = scenarioTheme(CardScenario.Spam)
    val spamBtn = BtnStyle(Color(0x26EF4444), SpamRed, Color(0x4DEF4444))
    SmartCardShell(
        theme = t, scenario = CardScenario.Spam,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
            if (email.links.isNotEmpty()) {
                LinkSection("Potentially dangerous link — blocked", email.links, isSpam = true)
            }
        },
        actions = {
            ActionButton("Report & Delete", spamBtn, leadingIcon = Icons.Filled.ReportProblem) {
                Log.d("SmartCards", "Report & Delete clicked — id=${email.id}")
                // TODO: POST to spam-report API; move to Spam folder via IMAP; remove from list
                onDismiss()
            }
        }
    )
}

@Composable
private fun NewsletterCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Newsletter)
    SmartCardShell(
        theme = t, scenario = CardScenario.Newsletter,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
        },
        actions = {
            ActionButton("Read Article", BtnPrimary, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "Read Article clicked — id=${email.id}")
                // TODO: Navigate to EmailDetailScreen or open article URL in in-app browser
                onNavigate()
            }
        }
    )
}

@Composable
private fun DefaultCard(email: SmartCardEmail, onDismiss: () -> Unit, onNavigate: () -> Unit) {
    val t = scenarioTheme(CardScenario.Default)
    SmartCardShell(
        theme = t, scenario = CardScenario.Default,
        sender = email.sender, onDismiss = onDismiss,
        content = {
            CardTitle(email.subject)
            CardBodyText(email.bodyPreview)
        },
        actions = {
            ActionButton("View Email", BtnPrimary, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward) {
                Log.d("SmartCards", "View Email (Default) clicked — id=${email.id}")
                // TODO: navController.navigate("email_detail/${email.id}")
                onNavigate()
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════
// SMART CARD DISPATCHER
// Equivalent to the main <SmartCard> export in SmartCard.jsx.
// Manages its own dismissed state, keyed by email.id.
// ═══════════════════════════════════════════════════════════════════

/**
 * Main SmartCard entry point — selects the correct card variant by
 * [email.scenario] and manages local dismissed state.
 *
 * When dismissed the composable emits nothing (same semantics as
 * `return null` in the React version).
 *
 * @param email      Contextual email to render.
 * @param onNavigate Called with the email ID when the user taps the
 *                   primary navigate action. Wire to your NavController.
 */
@Composable
fun SmartCard(email: SmartCardEmail, onNavigate: (emailId: String) -> Unit) {
    var dismissed by remember(email.id) { mutableStateOf(false) }
    if (dismissed) return

    val dismiss  = { dismissed = true }
    val navigate = { onNavigate(email.id) }

    when (email.scenario) {
        CardScenario.Flight     -> FlightCard(email, dismiss, navigate)
        CardScenario.Meeting    -> MeetingCard(email, dismiss, navigate)
        CardScenario.GoodNews   -> GoodNewsCard(email, dismiss, navigate)
        CardScenario.Alert      -> AlertCard(email, dismiss, navigate)
        CardScenario.Finance    -> FinanceCard(email, dismiss, navigate)
        CardScenario.Code       -> CodeCard(email, dismiss, navigate)
        CardScenario.Task       -> TaskCard(email, dismiss, navigate)
        CardScenario.Spam       -> SpamCard(email, dismiss, navigate)
        CardScenario.Newsletter -> NewsletterCard(email, dismiss, navigate)
        CardScenario.Default    -> DefaultCard(email, dismiss, navigate)
    }
}

// ═══════════════════════════════════════════════════════════════════
// SCREEN
// Full-screen scrollable feed of SmartCards on the dark background.
// Wired as a new "Cards" tab in EmailAgentApp (see MainActivity.kt).
// ═══════════════════════════════════════════════════════════════════

/**
 * SmartCards screen — dark-themed feed of contextual email cards.
 * Starts with [mockSmartCards]. Cards can be dismissed individually.
 */
@Composable
fun SmartCardsScreen(smartCards: List<SmartCardEmail>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Smart Cards",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "AI-parsed email context — tap to act",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(smartCards, key = { it.id }) { email ->
                SmartCard(
                    email = email,
                    onNavigate = { emailId ->
                        Log.d("SmartCards", "Navigate to email detail — id=$emailId")
                        // TODO: navController.navigate("email_detail/$emailId")
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// PHASE 4 — MOCK DATA
// 5 realistic cards spanning distinct scenarios. Field names match
// SmartCardEmail exactly. Plausible real-world content, not placeholders.
// ═══════════════════════════════════════════════════════════════════

val mockSmartCards: List<SmartCardEmail> = listOf(

    SmartCardEmail(
        id          = "sc-1",
        sender      = "bookings@airindia.com",
        subject     = "Your Flight AI-302 to Mumbai is Confirmed",
        bodyPreview = "Your booking is confirmed. Depart BLR 06:25 — Arrive BOM 08:10. Check-in opens 48 hrs before departure.",
        scenario    = CardScenario.Flight,
        pnr         = "XJ7R2K",
        links       = listOf("https://www.airindia.in/manage-booking?pnr=XJ7R2K")
    ),

    SmartCardEmail(
        id          = "sc-2",
        sender      = "nisha.rao@company.io",
        subject     = "Architecture Review — Rescheduled to 3:30 PM Today",
        bodyPreview = "Hi team, the API design review has been moved. Join via the Meet link below — agenda includes auth refresh and notification retries.",
        scenario    = CardScenario.Meeting,
        meetingTime = "3:30 PM",
        links       = listOf("https://meet.google.com/abc-defg-hij")
    ),

    SmartCardEmail(
        id          = "sc-3",
        sender      = "finance@vendor-corp.com",
        subject     = "Invoice #INV-2024-0892 Due — ₹1,25,000",
        bodyPreview = "Kindly process payment for services rendered in May 2024. GST invoice and bank details are enclosed in the attachment.",
        scenario    = CardScenario.Finance,
        amount      = "₹1,25,000",
        bankName    = "HDFC Bank",
        links       = listOf("https://vendor-corp.com/invoice/INV-2024-0892")
    ),

    SmartCardEmail(
        id          = "sc-4",
        sender      = "notifications@github.com",
        subject     = "[Review Requested] feat/auth-token-refresh — PR #247",
        bodyPreview = "Aarav Mehta requested your review: replace hardcoded secrets with env vars and add token expiry handling.",
        scenario    = CardScenario.Code,
        links       = listOf("https://github.com/org/email-agent/pull/247")
    ),

    SmartCardEmail(
        id          = "sc-5",
        sender      = "no-reply@promos-win.biz",
        subject     = "You've Been SELECTED! Claim Your ₹50,000 Reward NOW",
        bodyPreview = "Congratulations! Your account was randomly selected for a special reward. Click the link below immediately — offer expires in 24 hours!",
        scenario    = CardScenario.Spam,
        isSpam      = true,
        links       = listOf("http://totally-legit-prize.biz/claim?ref=victim99")
    )
)

// ═══════════════════════════════════════════════════════════════════
// PHASE 6 — PREVIEWS
// All previews force darkTheme=true + dynamicColor=false to render
// the dark glassmorphism palette correctly in Android Studio.
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "SmartCards — Full Screen", showBackground = true,
    backgroundColor = 0xFF0A0B14, widthDp = 390)
@Composable
private fun SmartCardsScreenPreview() {
    EmailClientTheme(darkTheme = true, dynamicColor = false) { SmartCardsScreen(mockSmartCards) }
}

@Preview(name = "Card — Flight", showBackground = true, backgroundColor = 0xFF0A0B14)
@Composable
private fun FlightCardPreview() {
    EmailClientTheme(darkTheme = true, dynamicColor = false) {
        Box(Modifier.padding(16.dp)) { SmartCard(mockSmartCards[0]) {} }
    }
}

@Preview(name = "Card — Meeting", showBackground = true, backgroundColor = 0xFF0A0B14)
@Composable
private fun MeetingCardPreview() {
    EmailClientTheme(darkTheme = true, dynamicColor = false) {
        Box(Modifier.padding(16.dp)) { SmartCard(mockSmartCards[1]) {} }
    }
}

@Preview(name = "Card — Finance", showBackground = true, backgroundColor = 0xFF0A0B14)
@Composable
private fun FinanceCardPreview() {
    EmailClientTheme(darkTheme = true, dynamicColor = false) {
        Box(Modifier.padding(16.dp)) { SmartCard(mockSmartCards[2]) {} }
    }
}

@Preview(name = "Card — Code / PR", showBackground = true, backgroundColor = 0xFF0A0B14)
@Composable
private fun CodeCardPreview() {
    EmailClientTheme(darkTheme = true, dynamicColor = false) {
        Box(Modifier.padding(16.dp)) { SmartCard(mockSmartCards[3]) {} }
    }
}

@Preview(name = "Card — Spam", showBackground = true, backgroundColor = 0xFF0A0B14)
@Composable
private fun SpamCardPreview() {
    EmailClientTheme(darkTheme = true, dynamicColor = false) {
        Box(Modifier.padding(16.dp)) { SmartCard(mockSmartCards[4]) {} }
    }
}
