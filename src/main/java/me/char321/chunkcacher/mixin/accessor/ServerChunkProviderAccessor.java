package me.char321.chunkcacher.mixin.accessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ServerChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkProvider.class)
public interface ServerChunkProviderAccessor {
    @Accessor
    Long2ObjectMap<Chunk> getLoadedChunksMap();
}
