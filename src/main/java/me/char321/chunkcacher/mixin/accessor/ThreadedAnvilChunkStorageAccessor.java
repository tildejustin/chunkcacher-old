package me.char321.chunkcacher.mixin.accessor;

import net.minecraft.world.chunk.ThreadedAnvilChunkStorage;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ThreadedAnvilChunkStorageAccessor {
    @Accessor
    static Logger getLOGGER() {
        throw new UnsupportedOperationException();
    }
}
