package com.arrowswap.mixin;

import com.arrowswap.ArrowSwap;
import java.io.File;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
abstract class OptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void arrowswap$registerKeys(Minecraft client, File directory, CallbackInfo ci) {
        int original = keyMappings.length;
        keyMappings = Arrays.copyOf(keyMappings, original + 2);
        keyMappings[original] = ArrowSwap.FIRST;
        keyMappings[original + 1] = ArrowSwap.SECOND;
    }
}
