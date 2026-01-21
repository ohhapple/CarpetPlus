package com.ohhapple.carpetplus.mixin.ShulkerBoxNested;

import com.ohhapple.carpetplus.settings.CarpetPlusSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin extends RandomizableContainerBlockEntity implements WorldlyContainer {
    protected ShulkerBoxBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    //漏斗可以放入潜影盒
    @Inject(method = "canPlaceItemThroughFace", at = @At("HEAD"), cancellable = true)
    public void canPlaceItemThroughFace(int i, ItemStack itemStack, Direction direction, CallbackInfoReturnable<Boolean> cir){
        if("both".equals(CarpetPlusSettings.ShulkerBoxNested)||"redstone".equals(CarpetPlusSettings.ShulkerBoxNested)){cir.setReturnValue(true);}
    }
}
