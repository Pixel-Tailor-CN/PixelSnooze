package vip.mystery0.pixel.snooze.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.preferenceCategory
import vip.mystery0.pixel.snooze.BuildConfig
import vip.mystery0.pixel.snooze.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        ProvidePreferenceTheme {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 24.dp
                )
            ) {
                preferenceCategory(
                    key = "category_about",
                    title = { Text("关于") }
                )
                item(key = "version_name", contentType = "Preference") {
                    Preference(
                        title = { Text("版本名称") },
                        summary = { Text(BuildConfig.VERSION_NAME) },
                        icon = {
                            Icon(Icons.Rounded.Info, contentDescription = null)
                        }
                    )
                }
                item(key = "version_code", contentType = "Preference") {
                    Preference(
                        title = { Text("版本号") },
                        summary = { Text(BuildConfig.VERSION_CODE.toString()) },
                        icon = {
                            Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                        }
                    )
                }
                item(key = "pixel_tailor", contentType = "Preference") {
                    Preference(
                        title = { Text("Pixel Tailor") },
                        summary = { Text("为中国 Pixel 用户精心缝补纯净 Android 体验") },
                        icon = {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painterResource(R.drawable.ic_pixel_tailor),
                                    contentDescription = null
                                )
                            }
                        },
                        onClick = {
                            context.openUrl("https://pixel.mystery0.app")
                        }
                    )
                }
                item(key = "telegram_channel", contentType = "Preference") {
                    Preference(
                        title = { Text("Telegram 频道") },
                        summary = { Text("关注 Telegram 频道获取最新动态") },
                        icon = {
                            Icon(Icons.Rounded.Forum, contentDescription = null)
                        },
                        onClick = {
                            context.openUrl("https://t.me/pixel_tailor_cn")
                        }
                    )
                }
            }
        }
    }
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
