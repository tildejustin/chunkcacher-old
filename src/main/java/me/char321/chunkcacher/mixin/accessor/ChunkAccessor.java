package me.char321.chunkcacher.mixin.accessor;

import net.minecraft.server.world.ChunkGenerator;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Chunk.class)
public interface ChunkAccessor {
    @Invoker
    void callPopulate(ChunkGenerator chunkGenerator);
}
