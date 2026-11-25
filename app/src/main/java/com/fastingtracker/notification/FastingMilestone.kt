package com.fastingtracker.notification

enum class FastingMilestone(val hours: Int) {
    FAT_BURNING(12),
    KETOSIS(16),
    DEEP_KETOSIS(18),
    AUTOPHAGY(24),
    EXTENDED_48H(48),
    EXTENDED_72H(72);

    fun label(isChinese: Boolean): String = when (this) {
        FAT_BURNING  -> if (isChinese) "脂肪燃烧" else "Fat Burning"
        KETOSIS      -> if (isChinese) "酮症" else "Ketosis"
        DEEP_KETOSIS -> if (isChinese) "深度酮症" else "Deep Ketosis"
        AUTOPHAGY    -> if (isChinese) "细胞自噬" else "Autophagy"
        EXTENDED_48H -> if (isChinese) "延长断食" else "Extended Fast"
        EXTENDED_72H -> if (isChinese) "终极断食" else "Ultimate Fast"
    }

    fun description(isChinese: Boolean): String = when (this) {
        FAT_BURNING  -> if (isChinese) "身体已进入脂肪燃烧模式" else "Your body has entered fat burning mode"
        KETOSIS      -> if (isChinese) "恭喜，你已进入酮症状态！" else "You've reached ketosis!"
        DEEP_KETOSIS -> if (isChinese) "深度酮症，生长激素开始上升" else "Deep ketosis and growth hormone increase"
        AUTOPHAGY    -> if (isChinese) "细胞自噬已启动" else "Cellular autophagy has begun"
        EXTENDED_48H -> if (isChinese) "48 小时——了不起的成就！" else "48 hours - remarkable achievement!"
        EXTENDED_72H -> if (isChinese) "72 小时——精英级断食！" else "72 hours - elite fasting!"
    }

    companion object {
        fun fromHours(hours: Int): FastingMilestone? = values().find { it.hours == hours }
        fun getNextMilestone(currentHours: Int): FastingMilestone? = values().firstOrNull { it.hours > currentHours }
    }
}
