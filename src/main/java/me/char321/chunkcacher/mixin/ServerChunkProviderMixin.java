package me.char321.chunkcacher.mixin;

import me.char321.chunkcacher.WorldCache;
import net.minecraft.server.world.ChunkGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ServerChunkProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkProviderMixin {
    @Shadow
    @Final
    private ServerWorld world;

    @Redirect(method = "getOrGenerateChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ChunkGenerator;generate(II)Lnet/minecraft/world/chunk/Chunk;"))
    private Chunk getCachedChunk(ChunkGenerator instance, int x, int z) {
        if (WorldCache.shouldCache()) {
            Chunk cachedChunk = WorldCache.getChunk(x, z, world, false);
            if (cachedChunk != null) {
                return cachedChunk;
            }
        }
        Chunk chunk = instance.generate(x, z);
        if (WorldCache.shouldCache()) {
            WorldCache.addChunk(world, chunk, false);
        }
        return chunk;
    }
}
