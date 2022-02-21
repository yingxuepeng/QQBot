package org.example.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.info
import kotlin.random.Random

/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.pyx.qqbot",
        name = "qqbot",
        version = "1.0.0"
    ) {
        author("群宠")
        info(
            "Bot管理员".trimIndent()
        )
        // author 和 info 可以删除.
    }
) {
    private var msgList = ArrayList<RepeatData>()
    private var msgMap = HashMap<String, RepeatData>()
    private const val MAX_REPEAT_COUNT = 10;
    override fun onEnable() {
        logger.info { "QQBot Plugin loaded" }

        //配置文件目录 "${dataFolder.absolutePath}/"
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {
            try {

                if (msgList.size == 0) {
                    group.sendMessage("~小浣熊悄悄起床了~")
                }

                if (group.id != 1022441533L) {
                    return@subscribeAlways
                }

                val msgStr = message.serializeToMiraiCode()

                if (msgStr.equals("全知全能的小浣熊，请帮助我避开雷区！")) {
                    var msg = ""
                    var i = 0;
                    for (msgData in msgList) {
                        i++
                        msg += "======\n[" + i + "] " + msgData.cnt + ":" + msgData.msg + "\n"
                    }
                    group.sendMessage(msg)
                }

                var repData = msgMap[msgStr];
                var isRepeat = false;

                if (repData != null) {
                    isRepeat = true
                    repData.cnt += 1
                } else {
                    repData = RepeatData()
                    repData.msg = msgStr;
                    repData.cnt = 0
                }
                synchronized(PluginMain.javaClass) {
                    //
                    if (isRepeat) {
                        // is repeat
                        msgList.remove(repData);
                        msgList.add(0, repData)
                    } else {
                        msgList.add(0, repData)
                        msgMap.set(msgStr, repData)
                        if (msgList.size > MAX_REPEAT_COUNT) {
                            val lastKey = msgList.removeLast()
                            msgMap.remove(lastKey.msg)
                        }
                    }
                }

                if (!isRepeat) {
                    return@subscribeAlways
                }

                val repeatLevel = getRepeatLevel(repData.cnt, time)
                if (repeatLevel == RepeatLevel.Miss) {
                    return@subscribeAlways
                }

                val label = getRepeatLevelLabel(repeatLevel)
                val muteTime = getRepeatMuteTime(repeatLevel)

                var nick = sender.nameCard
                if (nick == null || nick.equals("")) {
                    nick = sender.nick
                }
                group.sendMessage(Image("{1FC3D44A-6F98-6E13-2025-756013B51688}.jpg"))
                group.sendMessage("恭喜\"" + nick + "\"的第" + repData.cnt + "次复读抽中了" + label + "复读卡，喜提" + muteTime + "秒禁言~")
                sender.mute(muteTime)
            } catch (e: Exception) {
                group.sendMessage("卡bug啦~" + e.toString())
                return@subscribeAlways
            }

            return@subscribeAlways
        }

//        eventChannel.subscribeAlways<FriendMessageEvent> {
//            //好友信息
//        }
    }


    private fun printMsgList(group: Group) {
    }

    private fun getRepeatLevel(repeatCnt: Int, time: Int): RepeatLevel {
        val delta: Double = repeatCnt * 0.0001
        val rand: Double = Random(time).nextDouble() + delta

        if (rand > 0.999) {
            return RepeatLevel.SSR
        } else if (rand > 0.95) {
            return RepeatLevel.SR
        } else if (rand > 0.85) {
            return RepeatLevel.R
        } else if (rand > 0.5) {
            return RepeatLevel.Normal
        }
        return RepeatLevel.Miss
    }

    private fun getRepeatMuteTime(level: RepeatLevel): Int {
        if (level == RepeatLevel.SSR) {
            return 720
        } else if (level == RepeatLevel.SR) {
            return 360
        } else if (level == RepeatLevel.R) {
            return 180
        } else if (level == RepeatLevel.Normal) {
            return 60
        }
        return -1;
    }

    private fun getRepeatLevelLabel(level: RepeatLevel): String {
        if (level == RepeatLevel.SSR) {
            return "SSR"
        } else if (level == RepeatLevel.SR) {
            return "SR"
        } else if (level == RepeatLevel.R) {
            return "R"
        } else if (level == RepeatLevel.Normal) {
            return "N"
        }
        return "";
    }

    class RepeatData {
        public var msg: String = "";
        public var cnt: Int = 0;
    }

    enum class RepeatLevel {
        Miss,
        Normal,
        R,
        SR,
        SSR
    }

//            if (message.contentToString() == "hi") {
//                //群内发送
//                group.sendMessage("hi")
//                //向发送者私聊发送消息
//                sender.sendMessage("hi")
//                //不继续处理
//                return@subscribeAlways
//            }
//            //分类示例
//            message.forEach {
//                //循环每个元素在消息里
//                if (it is Image) {
//                    //如果消息这一部分是图片
//                    val url = it.queryUrl()
//                    group.sendMessage("图片，下载地址$url")
//                }
//                if (it is PlainText) {
//                    //如果消息这一部分是纯文本
//                    group.sendMessage("纯文本，内容:${it.content}")
//                }
//            }
//        }
//        eventChannel.subscribeAlways<NewFriendRequestEvent>{
//            //自动同意好友申请
//            accept()
//        }
//        eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent>{
//            //自动同意加群申请
//            accept()
//        }
//    }
}
