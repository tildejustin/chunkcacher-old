package me.char321.chunkcacher.mixin;

import me.char321.chunkcacher.WorldCache;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Shadow
    @Final
    private LevelInfo levelInfo;

    @Inject(method = "setupWorld(Ljava/lang/String;Ljava/lang/String;JLnet/minecraft/world/level/LevelGeneratorType;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;prepareWorlds()V"))
    private void blockSpawnCandidateOverwrite(CallbackInfo ci) {
        WorldCache.isGenerating = true;
        WorldCache.checkGeneratorOptions(this.levelInfo);
    }

    @Inject(method = "setupWorld(Ljava/lang/String;Ljava/lang/String;JLnet/minecraft/world/level/LevelGeneratorType;Ljava/lang/String;)V", at = @At("TAIL"))
    private void isNotGenerating(CallbackInfo ci) {
        WorldCache.isGenerating = false;
    }
}