package me.char321.chunkcacher.mixin;

import me.char321.chunkcacher.WorldCache;
import me.char321.chunkcacher.mixin.accessor.ChunkAccessor;
import me.char321.chunkcacher.mixin.accessor.ServerChunkProviderAccessor;
import net.minecraft.server.world.ChunkGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ServerChunkProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class ChunkMixin {
    @Shadow
    @Final
    private World world;
    @Unique
    private ServerChunkProvider chunkProvider;

    @Inject(
            method = "populate(Lnet/minecraft/server/world/ChunkGenerator;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;setModified()V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void addPopulatedChunkToCache(ChunkGenerator chunkGenerator, CallbackInfo ci) {
        if (WorldCache.shouldCache() && this.world instanceof ServerWorld) {
            WorldCache.addChunk((ServerWorld) world, (Chunk) (Object) this, true);
        }
    }

    @Inject(
            method = "populateIfMissing",
            at = @At(
                    value = "HEAD"
            )
    )
    private void getPopulateIfMissingArgs(ChunkProvider chunkProvider, ChunkGenerator generator, CallbackInfo ci) {
        this.chunkProvider = (ServerChunkProvider) chunkProvider;
    }

    // replaces spawn candidate chunks, which are counted as populated when generated, but need to be replaced with populated versions after spawnpoint finding
    @Redirect(
            method = "populateIfMissing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;populate(Lnet/minecraft/server/world/ChunkGenerator;)V"
            )
    )
    private void replaceWithCachedPopulatedChunks(Chunk instance, ChunkGenerator generator) {
        if (WorldCache.shouldCache()) {
            Chunk chunk = WorldCache.getChunk(instance.chunkX, instance.chunkZ, (ServerWorld) world, true);
            if (chunk != null) {
                ((ServerChunkProviderAccessor) this.chunkProvider).getLoadedChunksMap().put(ChunkPos.getIdFromCoords(instance.chunkX, instance.chunkZ), chunk);
                chunk.field_12912 = false;
                return;
            }
        }
        ((ChunkAccessor) instance).callPopulate(generator);
    }
}
