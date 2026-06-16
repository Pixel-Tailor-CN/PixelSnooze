package vip.mystery0.pixel.snooze.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import vip.mystery0.pixel.snooze.holiday.HolidayDataConfig
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    holidayRepository: HolidayRepository,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var holidayDataUrl by remember { mutableStateOf(preferencesRepository.holidayDataUrl()) }
    var isUsingDefaultHolidayDataUrl by remember {
        mutableStateOf(preferencesRepository.isUsingDefaultHolidayDataUrl())
    }
    var showHolidayDataUrlDialog by remember { mutableStateOf(false) }

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
                    key = "category_holiday_data",
                    title = { Text("节假日数据") }
                )
                item(key = "holiday_data_url", contentType = "Preference") {
                    Preference(
                        title = { Text("云端数据地址") },
                        summary = {
                            Text(holidayDataUrl.summaryText(isUsingDefaultHolidayDataUrl))
                        },
                        icon = {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null)
                        },
                        onClick = {
                            showHolidayDataUrlDialog = true
                        }
                    )
                }
                item(key = "custom_holiday_data_guide", contentType = "Preference") {
                    Preference(
                        title = { Text("自建节假日数据指南") },
                        summary = { Text("查看 JSON 格式、字段含义和推荐生成方案") },
                        icon = {
                            Icon(Icons.Rounded.Article, contentDescription = null)
                        },
                        onClick = {
                            context.openUrl(HolidayDataConfig.CUSTOM_DATA_GUIDE_URL)
                        }
                    )
                }
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

    if (showHolidayDataUrlDialog) {
        HolidayDataUrlDialog(
            initialUrl = holidayDataUrl,
            onDismiss = { showHolidayDataUrlDialog = false },
            onReset = {
                preferencesRepository.resetHolidayDataUrl()
                holidayRepository.clearCacheAndReload()
                holidayDataUrl = preferencesRepository.holidayDataUrl()
                isUsingDefaultHolidayDataUrl =
                    preferencesRepository.isUsingDefaultHolidayDataUrl()
                showHolidayDataUrlDialog = false
                Toast.makeText(context, "已恢复默认节假日数据地址", Toast.LENGTH_SHORT).show()
            },
            onSave = { url, onComplete ->
                if (url.isEmpty()) {
                    preferencesRepository.resetHolidayDataUrl()
                    holidayRepository.clearCacheAndReload()
                    holidayDataUrl = preferencesRepository.holidayDataUrl()
                    isUsingDefaultHolidayDataUrl =
                        preferencesRepository.isUsingDefaultHolidayDataUrl()
                    showHolidayDataUrlDialog = false
                    Toast.makeText(context, "已恢复默认节假日数据地址", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                } else {
                    holidayRepository.refreshFromRemoteUrl(url) { result ->
                        mainHandler.post {
                            if (result.success) {
                                preferencesRepository.updateHolidayDataUrl(url)
                                holidayDataUrl = preferencesRepository.holidayDataUrl()
                                isUsingDefaultHolidayDataUrl =
                                    preferencesRepository.isUsingDefaultHolidayDataUrl()
                                showHolidayDataUrlDialog = false
                                Toast.makeText(
                                    context,
                                    "节假日数据已检测并缓存",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onComplete(null)
                            } else {
                                onComplete(result.errorMessage ?: "节假日数据不可用")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun HolidayDataUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSave: (String, (String?) -> Unit) -> Unit
) {
    var input by remember(initialUrl) { mutableStateOf(initialUrl) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isChecking) onDismiss()
        },
        title = { Text("云端数据地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "保存前会先检测该地址并缓存一份可用数据。检测失败时不会保存 URL。",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorText = null
                    },
                    label = { Text("HTTPS URL") },
                    supportingText = {
                        Text(
                            errorText ?: if (isChecking) {
                                "正在检测并缓存节假日数据"
                            } else {
                                "留空或恢复默认将使用内置的默认数据源。"
                            }
                        )
                    },
                    enabled = !isChecking,
                    isError = errorText != null,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalizedUrl = input.trim()
                    if (!normalizedUrl.isSupportedHolidayDataUrl()) {
                        errorText = "请输入有效的 https:// 地址"
                    } else {
                        isChecking = true
                        errorText = null
                        onSave(normalizedUrl) { failureMessage ->
                            isChecking = false
                            errorText = failureMessage
                        }
                    }
                },
                enabled = !isChecking
            ) {
                Text(if (isChecking) "检测中" else "保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReset, enabled = !isChecking) {
                    Text("恢复默认")
                }
                TextButton(onClick = onDismiss, enabled = !isChecking) {
                    Text("取消")
                }
            }
        }
    )
}

private fun String.summaryText(isDefault: Boolean): String {
    return if (isDefault) {
        "使用默认中国大陆数据源"
    } else {
        "使用自定义数据源：$this"
    }
}

private fun String.isSupportedHolidayDataUrl(): Boolean {
    if (isBlank()) return true
    val uri = toUri()
    return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
