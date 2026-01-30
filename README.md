# CarpetPlus
[![License](https://img.shields.io/github/license/ohhapple/CarpetPlus.svg?label=License&color=blue)](https://choosealicense.com/licenses/lgpl-3.0/)
[![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/ohhapple/CarpetPlus/total?color=161616&label=Github%20downloads)](https://github.com/ohhapple/CarpetPlus/releases)
# 相关规则指令
~~~
/carpet SuperWindCharge [boolean default=false] 风弹螺旋丸手里剑
/carpetVillageAlwaysBreed [boolean default=false] 村民一直繁殖（包括小村民）
/carpet SuperSponge [String default="false"] 超级海绵模式(可使用/carpet SuperSpongeRadius设置半径)
/carpet SuperSpongeRadius [int default=6] 超级海绵生效半径(需要使用/carpet SuperSponge开启超级海绵模式)
/carpet playerSpecificChunks [boolean default=false] 启用玩家独立视距
/carpet concreteBurnedIntoglass [boolean default=false] 16色混凝土粉末烧成16色玻璃
/carpet StackablePotion [int default=1] 药水可堆叠
/carpet StackableEnchantedBook [int default=1] 附魔书可堆叠
/carpet StackableTotemOfUndying [int default=1] 不死图腾可堆叠
/carpet EquipmentUnbreak [boolean default=false] 装备不消耗耐久
/carpet ShulkerBoxNested [String default="false"] 潜影盒嵌套(可选被谁操作:仅玩家,仅漏斗发射器,或者全部)
关于玩家独立视距范围的指令：
    启用功能
    /carpet playerSpecificChunks true
    开启功能后玩家默认视距被设置为10（不会超过服务器视距）
    设置单个玩家
    /playerchunk set 玩家A 10
    设置多个玩家
    /playerchunk set @a 10          # 所有玩家
    /playerchunk set @r 8           # 随机玩家
    /playerchunk set @p 12          # 最近的玩家
    查看设置
    /playerchunk get 玩家A
    /playerchunk get @s             # 自己
    列出所有设置
    /playerchunk list
    查看状态
    /playerchunk status
    统计信息
    /chunkstats
~~~
想边玩游戏边听音乐？本mod已实现音乐频道功能
# 音乐指令
~~~
/open 打开音乐菜单GUI 搜索框输入音乐关键词，点击搜索
~~~