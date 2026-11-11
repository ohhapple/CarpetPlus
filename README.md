# CarpetPlus
[![License](https://img.shields.io/github/license/ohhapple/CarpetPlus.svg?label=License&color=blue)](https://choosealicense.com/licenses/lgpl-3.0/)
[![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/ohhapple/CarpetPlus/total?color=161616&label=Github%20downloads)](https://github.com/ohhapple/CarpetPlus/releases)
[![CurseForge](https://img.shields.io/curseforge/dt/1798568?label=CurseForge%20Downloads&color=orange)](https://www.curseforge.com/minecraft/mc-mods/carpetplus)
[![Modrinth](https://img.shields.io/modrinth/dt/ef6KK3nr?label=Modrinth%20Downloads)](https://modrinth.com/mod/carpetplus-ohhapple)

## 构建

需要 Java 25：

~~~powershell
.\gradlew.bat build
~~~

# 文档主页：https://ohhapple.github.io
# 相关规则指令
~~~
/carpet SuperWindCharge [boolean default=false] 风弹螺旋丸手里剑
/carpet VillageAlwaysBreed [boolean default=false] 村民一直繁殖（包括小村民）
/carpet SuperSponge [String default="false"] 超级海绵模式(可使用/carpet SuperSpongeRadius设置半径)
/carpet SuperSpongeRadius [int default=6] 超级海绵生效半径(需要使用/carpet SuperSponge开启超级海绵模式)
/carpet concreteBurnedIntoglass [boolean default=false] 16色混凝土粉末烧成16色玻璃
/carpet StackablePotion [int default=1] 药水可堆叠
/carpet StackableEnchantedBook [int default=1] 附魔书可堆叠
/carpet StackableTotemOfUndying [int default=1] 不死图腾可堆叠
/carpet StackableMilkBucket [int default=1] 奶桶可堆叠
/carpet EquipmentUnbreak [boolean default=false] 装备不消耗耐久
/carpet ShulkerBoxNested [String default="false"] 潜影盒嵌套(可选被谁操作:仅玩家,仅漏斗发射器,或者全部)
/carpet CarpetPlusNetwork [boolean default=true] CarpetPlus网络协议处理总开关,开启后可使用相关网络功能(默认开启)
/carpet GetPlayerFps [String default="false"] 获取服务器玩家FPS
/carpet AmethystShardMusicChannel [boolean default=false] 紫水晶碎片音乐频道
~~~
## 音乐播放器

音乐功能统一受 `/carpet AmethystShardMusicChannel true` 控制。开启后可在聊天框输入 `++cp music`，或手持紫水晶碎片右键打开播放器。

## 账号与 Cookie 安全

- 音乐平台登录完全可选；未登录仍可使用公开搜索、推荐和可播放内容。
- Cookie、`MUSIC_U`、二维码登录 key、账号资料和加密登录文件都只应保留在客户端本机，禁止发送给其他玩家、上传到服务器或提交到 Issue。
- 不要公开分享包含 Cookie、二维码 key、账号昵称或服务器地址的截图和日志。发送排障日志前请先删除相关隐私信息。
- 账号登录凭据仍使用每安装随机密钥和本机 DPAPI 保护。不要将账号凭据文件或 `data-key` 分享给其他人。

多人一起听还需要开启 CarpetPlus 网络协议规则：

~~~
/carpet CarpetPlusNetwork true
/carpet AmethystShardMusicChannel true
~~~

## 免责声明

CarpetPlus 是非官方项目，与 Mojang Studios、Microsoft、音乐平台及其关联方不存在隶属、授权或合作关系，相关名称仅用于说明兼容范围和服务来源。

CarpetPlus 不绕过音乐平台的登录、会员、付费、版权或地区限制。歌曲能否播放取决于平台接口、版权状态和当前账号权益，项目不保证任何歌曲、歌单、歌词或接口持续可用。

使用本模组即表示你理解并自行承担相关风险。建议在使用前备份世界与配置，并遵守所在服务器的规则及音乐平台的服务条款。项目按许可证现状提供，不对数据丢失、账号风险、服务中断或接口变化作额外担保。
