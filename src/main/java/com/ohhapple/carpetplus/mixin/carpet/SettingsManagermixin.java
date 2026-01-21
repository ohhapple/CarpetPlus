package com.ohhapple.carpetplus.mixin.carpet;

import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import com.ohhapple.carpetplus.CarpetPlus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SettingsManager.class)
public abstract class SettingsManagermixin {



    @Inject(
            method = "listAllSettings",
            at = @At(
                    value = "INVOKE",
                    target = "Lcarpet/api/settings/SettingsManager;getCategories()Ljava/lang/Iterable;",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void displayModInfo(CommandSourceStack source, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == CarpetServer.settingsManager) {
            MutableComponent message = Component.empty();
            message.append(Component.literal("CarpetPlus ").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal("版本: ").withStyle(ChatFormatting.GRAY));
            message.append(Component.literal(CarpetPlus.getVersion()).withStyle(ChatFormatting.GRAY));

            source.sendSuccess(() -> message, false);
        }
    }
}