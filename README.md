# CarpetPlus
[![License](https://img.shields.io/github/license/ohhapple/CarpetPlus.svg?label=License&color=blue)](https://choosealicense.com/licenses/lgpl-3.0/)
[![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/ohhapple/CarpetPlus/total?color=161616&label=Github%20downloads)](https://github.com/ohhapple/CarpetPlus/releases)

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
### 想边玩游戏边听音乐？本mod已实现音乐频道功能 ↓↓↓
### 注意: 由于部分歌曲为付费网络歌曲或无版权，所以本播放器无法播放相关资源,资源获取速度与网速有关，大约3-5秒
### 注意: 加入服务器多人音乐频道的多人一起听功能需开启CarpetPlus网络协议处理总开关(默认开启)(/carpet CarpetPlusNetwork true)后才能生效
# 音乐指令
~~~
++cp music 聊天框输入以打开音乐菜单GUI

(注意：加入服务器多人音乐频道的多人一起听功能需开启CarpetPlus网络协议处理总开关(默认开启)后才能生效)
/carpet CarpetPlusNetwork [boolean default=false]

(可选：开启规则后手持紫水晶碎片右键也可打开GUI)
/carpet AmethystShardMusicChannel [boolean default=false] 紫水晶碎片音乐频道
~~~