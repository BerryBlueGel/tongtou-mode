# 通透模式

Minecraft Java **26.2 + Fabric** 的纯客户端 Mod，正式版 **v1.0.0**。

哦耶耶！桃花🌸谢了有玫瑰🌹～  

![通透模式封面](TongtouMode-v1.0.0-cover.jpg)

## 下载

前往 [v1.0.0 发布页](https://github.com/BerryBlueGel/tongtou-mode/releases/tag/v1.0.0) 下载 `tongtou-mode-1.0.0+26.2.jar`，或下载包含 Fabric API 与说明的安装包。GitHub 下载附件使用英文文件名，内容与已验收的中文文件名版本完全一致；下载后可改名为 `通透模式-1.0.0+26.2.jar`。

本仓库包含完整源码、Gradle Wrapper 和客户端游戏测试。开发过程中使用了 AI 辅助编码；功能经过客户端游戏测试和实际游戏验收，具体验证范围见下文。

## 安装与使用

1. 使用 Minecraft Java 26.2 的 Fabric 实例，Java 25，Fabric Loader 0.19.3 或更高。
2. 把 `通透模式-1.0.0+26.2.jar` 放入这个实例的 `mods` 文件夹，删除旧版 `jelly-party-0.1.0+26.2.jar`、`jelly-party-0.1.1+26.2.jar` 或原英文文件名 `tongtou-mode-1.0.0+26.2.jar`，只保留一份本 Mod。
3. 需要同为 26.2 的 Fabric API。交付安装包附有 `fabric-api-0.152.2+26.2.jar`；已有适配 26.2 的 Fabric API 时保留原来的即可，不要同时放两份。
4. 进入世界，按 **J** 开始；再次按 **J** 停止。
5. 修改按键：游戏“选项 → 控制 → 按键绑定 → 通透模式”。

歌曲音量受“主音量”和“唱片机 / 音符盒”滑块控制。若歌曲无法开始，屏幕会显示音量检查提示。

### PCL2 列表显示

发布文件使用中文文件名，以便 PCL2 在无法匹配在线项目而回退到文件名时也能显示“通透模式”。

经检查的 PCL2 源码中，本地 Mod 列表使用匹配到的在线项目图片；没有在线项目时显示默认方块图标。这一页的图片和项目说明不会直接采用 JAR 内的 Fabric 图标和说明。内置图标与说明已经设置；GitHub 发布本身不保证 PCL2 能匹配到在线项目。

参考：[PCL2 列表控件源码](https://github.com/Meloong-Git/PCL/blob/0e0d12fdce6a2804916fb2be60e41144da637c18/Plain%20Craft%20Launcher%202/Pages/PageInstance/MyLocalModItem.xaml.vb)。

## 实际行为

- 内置从视频中完整提取的约 **54.32 秒**音轨，无需额外复制音频。
- 开启提示：**通透模式开启**。关闭提示（包括歌曲播放完毕）：**你也快了**。
- 开始后，玩家周围 **24 格**内的原版生物、其他玩家和自己的角色模型同步上下 Q 弹、压扁变胖、拉高变瘦。
- 音轨约 **10.24 秒**，即视频歌词“人生几十年”的“人”字开始高亮时，加入原地旋转，Q 弹继续。
- 旋转速度逐渐加快至每秒 **240°**（旧版为 180°），Q 弹节拍默认约 130 BPM。
- 动画只变换角色模型，不修改玩家朝向或相机。第一人称视角照常，F5 第三人称可以看见自己转圈；灵魂出窍等 Mod 如果沿用原版角色渲染流程，也能看见该效果，组合兼容性尚未实测。
- 按键停止或音轨播完后，模型用约 0.2 秒恢复正常。
- 生物进入或离开范围时也用约 0.2 秒过渡，避免旋转角度绕过 360°时突然翻转。
- 单人游戏暂停时歌曲与动画一起暂停；联机打开菜单但世界仍在运行时继续播放。
- 退出世界、切换维度或关闭客户端后清理本次播放。
- 盔甲架、掉落物等不参与。标准生物渲染器、玩家渲染器和末影龙渲染器均有处理。
- 音乐与变形只在本机发生。Mod 没有网络数据包、服务器入口、实体位置修改或 AI 修改；服务器和其他玩家不需要安装。

## 调整参数

第一次启动会在当前游戏实例的 `config/jellyparty.json` 生成配置，默认如下。每次开始播放时重新读取配置。

```json
{
  "configVersion": 1,
  "radius": 24.0,
  "spinStartSeconds": 10.24,
  "bpm": 130.0,
  "beatOffsetSeconds": 0.0,
  "spinDegreesPerSecond": 240.0,
  "hopHeight": 0.18,
  "transitionSeconds": 0.2,
  "volume": 0.8
}
```

从 0.1.0 升级时，旧配置的默认 180°/秒会自动迁移到 240°/秒，并保留其他参数；旧配置中设置为其他值的自定义转速保持不变。新版配置以后手动设为 180 也会保留。

`spinStartSeconds` 是相对于所交付音频文件开头的秒数。10.24 秒依据视频的歌词高亮估算；如果实际听感略有偏差，可以上下微调这个值。

`bpm` 控制每分钟 Q 弹次数，`hopHeight` 控制上下位移，`spinDegreesPerSecond` 控制旋转速度。范围外继续正常显示，只有本机已加载的生物和玩家能参与。

## 构建与验证

使用 Java 25：

```powershell
.\gradlew.bat build
.\gradlew.bat runClientGameTest
```

固定依赖：Minecraft 26.2、Fabric Loader 0.19.3、Fabric API 0.152.2+26.2、Fabric Loom 1.17.11、Gradle 9.6.0。

测试代码位于 `src/gametest`，不会进入发布 JAR。测试在独立游戏目录中创建临时存档，验证实际键盘输入、音频游标、旋转切入、距离过滤、自身模型与模拟远端玩家、第一和第三人称镜头朝向保持不变、F5 切换、配置迁移、暂停、手动停止、完整播放自然结束以及退出世界后的清理，同时保存游戏截图。模拟远端玩家使用本机 RemotePlayer 测试实体；未连接真实多人服务器。

渲染使用 Minecraft 的 PoseStack，并把不可变动画参数复制进实体 RenderState；读取真实音频游标的 OpenAL 操作在游戏声音线程执行。没有直接调用 OpenGL 绘图接口。

测试覆盖范围以交付的验证报告为准。与其他更换生物模型、渲染器或声音系统的 Mod 的组合兼容性需单独验证。

v1.0.0 在已验收的 v0.1.1 基础上更新名称、说明、提示和图标。正式版构建与文件检查通过，所有运行时类和内置音频与 v0.1.1 完全一致；此前游戏测试及用户验收作为行为验证依据。本次未重复运行游戏测试。内部 Mod ID 和配置文件名保持 `jellyparty`，可继续使用已有配置和按键。

开发参考：[Fabric 26.2](https://fabricmc.net/2026/06/15/262.html)、[官方项目模板](https://github.com/FabricMC/fabric-example-mod/tree/26.2)、[客户端声音](https://docs.fabricmc.net/develop/sounds/dynamic-sounds)、[游戏测试](https://docs.fabricmc.net/develop/automatic-testing)。
