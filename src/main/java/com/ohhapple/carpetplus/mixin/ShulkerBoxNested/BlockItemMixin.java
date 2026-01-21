package com.ohhapple.carpetplus.mixin.ShulkerBoxNested;

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin extends Item {
    public BlockItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(
            method = "canFitInsideContainerItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCanFitInsideContainerItems(CallbackInfoReturnable<Boolean> cir) {
        BlockItem self = (BlockItem)(Object)this;
        Block block = self.getBlock();

        // 检查是否是潜影盒方块
        if (block instanceof ShulkerBoxBlock) {
            // 根据规则决定是否允许放入容器
            if ("both".equals(CarpetPlusSettings.ShulkerBoxNested)||"player".equals(CarpetPlusSettings.ShulkerBoxNested)) {cir.setReturnValue(true);}
        }
    }
}
