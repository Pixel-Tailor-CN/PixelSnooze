package vip.mystery0.pixel.snooze.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingGuideDialog(
    listenerEnabled: Boolean,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenRestSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    val steps = remember(listenerEnabled) { onboardingGuideSteps(listenerEnabled) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.lastIndex

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("入门引导")
                Text(
                    text = "${currentStepIndex + 1}/${steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = currentStep.title,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                when (currentStep.action) {
                    OnboardingGuideAction.NotificationSettings -> {
                        OutlinedButton(
                            onClick = onOpenNotificationListenerSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("打开通知监听设置")
                        }
                    }

                    OnboardingGuideAction.RestSchedule -> {
                        OutlinedButton(
                            onClick = onOpenRestSchedule,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("设置休息日规则")
                        }
                    }

                    null -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isLastStep) {
                        onDismiss()
                    } else {
                        currentStepIndex += 1
                    }
                }
            ) {
                Text(if (isLastStep) "完成" else "下一步")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStepIndex > 0) {
                    TextButton(onClick = { currentStepIndex -= 1 }) {
                        Text("上一步")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("跳过")
                }
            }
        }
    )
}

private fun onboardingGuideSteps(listenerEnabled: Boolean): List<OnboardingGuideStep> {
    return listOf(
        OnboardingGuideStep(
            title = "开启通知监听",
            description = if (listenerEnabled) {
                "通知监听已经开启。Pixel Snooze 会在收到闹钟通知时读取通知内容，并只在今天是休息日时尝试执行跳过操作。"
            } else {
                "先授权 Pixel Snooze 读取通知。授权后应用才能看到闹钟通知，并在今天是休息日时尝试执行跳过操作。"
            },
            action = if (listenerEnabled) null else OnboardingGuideAction.NotificationSettings
        ),
        OnboardingGuideStep(
            title = "确认闹钟关键词",
            description = "默认关键词是“节假日闹钟”。如果你的闹钟通知标题或正文不是这个文案，回到主界面点击“关键词”修改。"
        ),
        OnboardingGuideStep(
            title = "确认跳过按钮文本",
            description = "应用会匹配闹钟通知按钮里的文本。不同闹钟应用文案可能不同，必要时点击“跳过按钮文本”补充。"
        ),
        OnboardingGuideStep(
            title = "设置休息日规则",
            description = "根据你的作息选择双休、单休、大小周、周期排班或完全自定义。运行时只判断今天是否是休息日。",
            action = OnboardingGuideAction.RestSchedule
        ),
        OnboardingGuideStep(
            title = "查看执行记录",
            description = "收到闹钟通知后，主界面会记录最近识别到的闹钟通知和自动跳过记录。可以用它确认配置是否命中。"
        )
    )
}

private data class OnboardingGuideStep(
    val title: String,
    val description: String,
    val action: OnboardingGuideAction? = null
)

private enum class OnboardingGuideAction {
    NotificationSettings,
    RestSchedule
}
