package com.example.cpcontesttracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cpcontesttracker.R

private const val DEVELOPER_NAME = "Shourya Vaidhya Jain"
private const val DEVELOPER_EMAIL = "parvx18@gmail.com"

/** A profile link shown as a brand icon; the glyph is drawn in [tint] on top of [background]. */
private data class SocialLink(
    val name: String,
    val url: String,
    @DrawableRes val icon: Int,
    val background: Brush,
    val tint: Color?, // null keeps the drawable's own colors
    val iconPadding: Int = 14
)

private val SOCIAL_LINKS = listOf(
    SocialLink(
        "LinkedIn", "https://www.linkedin.com/in/shourya-vaidhya-jain/",
        R.drawable.ic_linkedin, SolidColor(Color(0xFF0A66C2)), Color.White
    ),
    SocialLink(
        "LeetCode", "https://leetcode.com/u/The_Forever_Knight/",
        R.drawable.ic_leetcode, SolidColor(Color.White), null, iconPadding = 10
    ),
    SocialLink(
        "GitHub", "https://github.com/Ben10isBack",
        R.drawable.ic_github, SolidColor(Color.White), Color(0xFF181717)
    ),
    SocialLink(
        "Instagram", "https://www.instagram.com/shourya_vaidhya_jain/?hl=en",
        R.drawable.ic_instagram,
        Brush.linearGradient(listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5))),
        Color.White
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("About", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // App identity
            Image(
                painter = painterResource(R.drawable.ic_cpct_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Image(
                painter = painterResource(R.drawable.ic_cpct_wordmark),
                contentDescription = "CPCT",
                modifier = Modifier.height(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Competitive Programming Contest Tracker" + (appVersion(context)?.let { " · v$it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(28.dp))

            // Developer card
            Surface(
                color = CardBackgroundDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Developed by", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        DEVELOPER_NAME,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Thanks for using CPCT! Feel free to connect with me, share feedback or ideas, " +
                            "and report any bugs you find. I'd love to hear from you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SOCIAL_LINKS.forEach { link ->
                            SocialIcon(link, onClick = { openLink(context, link.url) })
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Email card
            Surface(
                onClick = { sendEmail(context) },
                color = CardBackgroundDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_gmail), contentDescription = null, tint = Color(0xFFEA4335))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Email · bug reports & feedback", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(DEVELOPER_EMAIL, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialIcon(link: SocialLink, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(link.background)
                    .padding(link.iconPadding.dp)
            ) {
                if (link.tint != null) {
                    Icon(painterResource(link.icon), contentDescription = link.name, tint = link.tint)
                } else {
                    Image(painterResource(link.icon), contentDescription = link.name)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(link.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "CPCT feedback")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found. Write to $DEVELOPER_EMAIL", Toast.LENGTH_LONG).show()
    }
}

private fun appVersion(context: Context): String? = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: Exception) {
    null
}
