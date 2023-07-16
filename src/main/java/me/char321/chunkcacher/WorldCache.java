package me.char321.chunkcacher;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import me.char321.chunkcacher.mixin.accessor.ThreadedAnvilChunkStorageAccessor;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ScheduledTick;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ThreadedAnvilChunkStorage;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldCache {
    private static final Map<DimensionType, Long2ObjectLinkedOpenHashMap<NbtCompound>> populatedCache = new HashMap<>();
    private static final Map<DimensionType, Long2ObjectLinkedOpenHashMap<NbtCompound>> unpopulatedCache = new HashMap<>();
    public static boolean isGenerating = false;
    private static LevelInfo lastGeneratorOptions;

    public static void addChunk(ServerWorld world, Chunk chunk, boolean populated) {
        Long2ObjectLinkedOpenHashMap<NbtCompound> map = (populated ? populatedCache : unpopulatedCache).computeIfAbsent(world.dimension.getDimensionType(), k -> new Long2ObjectLinkedOpenHashMap<>());
        long chunkHash = ChunkPos.getIdFromCoords(chunk.chunkX, chunk.chunkZ);
        if (!map.containsKey(chunkHash)) {
            map.put(chunkHash, WorldCache.serialize(world, chunk));
        }
    }

    public static boolean shouldCache() {
        return isGenerating && Atum.isRunning;
    }

    public static Chunk getChunk(int x, int z, @NotNull ServerWorld world, boolean populated) {
        Long2ObjectLinkedOpenHashMap<NbtCompound> map = (populated ? populatedCache : unpopulatedCache).get(world.dimension.getDimensionType());
        if (map == null) return null;
        NbtCompound chunkNbt = map.get(ChunkPos.getIdFromCoords(x, z));
        return WorldCache.validateChunk(world, x, z, chunkNbt);
    }

    /**
     * Checks if the generator options have changed, if so, clear the cache
     * dude github copilot is so cool it auto generated these comments
     * <p>
     * kept as fallback just in case some Atum update messes anything up
     * not perfect but good enough for that purpose
     */
    public static void checkGeneratorOptions(LevelInfo generatorOptions) {
        if (lastGeneratorOptions == null || lastGeneratorOptions.getSeed() != generatorOptions.getSeed() || lastGeneratorOptions.hasStructures() != generatorOptions.hasStructures() || lastGeneratorOptions.getGeneratorType() != generatorOptions.getGeneratorType()) {
            WorldCache.clearCache();
            lastGeneratorOptions = generatorOptions;
        }
    }

    public static void clearCache() {
        populatedCache.clear();
    }

    public static NbtCompound serialize(ServerWorld world, Chunk chunk) {
        NbtCompound nbtCompound = new NbtCompound();
        NbtCompound nbtCompound2 = new NbtCompound();
        nbtCompound.put("Level", nbtCompound2);
        nbtCompound.putInt("DataVersion", 1343);
        nbtCompound2.putInt("xPos", chunk.chunkX);
        nbtCompound2.putInt("zPos", chunk.chunkZ);
        nbtCompound2.putLong("LastUpdate", world.getLastUpdateTime());
        nbtCompound2.putIntArray("HeightMap", chunk.getLevelHeightmap());
        nbtCompound2.putBoolean("TerrainPopulated", chunk.isTerrainPopulated());
        nbtCompound2.putBoolean("LightPopulated", chunk.isLightPopulated());
        nbtCompound2.putLong("InhabitedTime", chunk.getInhabitedTime());
        ChunkSection[] chunkSections = chunk.getBlockStorage();
        NbtList nbtList = new NbtList();
        boolean bl = world.dimension.isOverworld();

        for (ChunkSection chunkSection : chunkSections) {
            if (chunkSection != Chunk.EMPTY) {
                NbtCompound nbtCompound3 = new NbtCompound();
                nbtCompound3.putByte("Y", (byte) (chunkSection.getYOffset() >> 4 & 0xFF));
                byte[] bs = new byte[4096];
                ChunkNibbleArray chunkNibbleArray = new ChunkNibbleArray();
                ChunkNibbleArray chunkNibbleArray2 = chunkSection.method_11774().store(bs, chunkNibbleArray);
                nbtCompound3.putByteArray("Blocks", bs);
                nbtCompound3.putByteArray("Data", chunkNibbleArray.getValue());
                if (chunkNibbleArray2 != null) {
                    nbtCompound3.putByteArray("Add", chunkNibbleArray2.getValue());
                }

                nbtCompound3.putByteArray("BlockLight", chunkSection.getBlockLight().getValue());
                if (bl) {
                    nbtCompound3.putByteArray("SkyLight", chunkSection.getSkyLight().getValue());
                } else {
                    nbtCompound3.putByteArray("SkyLight", new byte[chunkSection.getBlockLight().getValue().length]);
                }

                nbtList.add(nbtCompound3);
            }
        }

        nbtCompound2.put("Sections", nbtList);
        nbtCompound2.putByteArray("Biomes", chunk.getBiomeArray());
        chunk.setHasEntities(false);
        NbtList nbtList2 = new NbtList();

        for (int i = 0; i < chunk.getEntities().length; ++i) {
            for (Entity entity : chunk.getEntities()[i]) {
                NbtCompound nbtCompound4 = new NbtCompound();
                if (entity.saveToNbt(nbtCompound4)) {
                    chunk.setHasEntities(true);
                    nbtList2.add(nbtCompound4);
                }
            }
        }

        nbtCompound2.put("Entities", nbtList2);
        NbtList nbtList3 = new NbtList();

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            NbtCompound nbtCompound5 = blockEntity.toNbt(new NbtCompound());
            nbtList3.add(nbtCompound5);
        }

        nbtCompound2.put("TileEntities", nbtList3);
        List<ScheduledTick> list = world.getScheduledTicks(chunk, false);
        if (list != null) {
            long l = world.getLastUpdateTime();
            NbtList nbtList4 = new NbtList();

            for (ScheduledTick scheduledTick : list) {
                NbtCompound nbtCompound6 = new NbtCompound();
                Identifier identifier = Block.REGISTRY.getIdentifier(scheduledTick.getBlock());
                nbtCompound6.putString("i", identifier.toString());
                nbtCompound6.putInt("x", scheduledTick.pos.getX());
                nbtCompound6.putInt("y", scheduledTick.pos.getY());
                nbtCompound6.putInt("z", scheduledTick.pos.getZ());
                nbtCompound6.putInt("t", (int) (scheduledTick.time - l));
                nbtCompound6.putInt("p", scheduledTick.priority);
                nbtList4.add(nbtCompound6);
            }

            nbtCompound2.put("TileTicks", nbtList4);
        }
        return nbtCompound;
    }

    public static Chunk validateChunk(ServerWorld world, int chunkX, int chunkZ, NbtCompound nbt) {
        if (nbt == null) return null;
        if (!nbt.contains("Level", 10)) {
            ThreadedAnvilChunkStorageAccessor.getLOGGER().error("Chunk file at {},{} is missing level data, skipping", chunkX, chunkZ);
            return null;
        } else {
            NbtCompound nbtCompound = nbt.getCompound("Level");
            if (!nbtCompound.contains("Sections", 9)) {
                ThreadedAnvilChunkStorageAccessor.getLOGGER().error("Chunk file at {},{} is missing block data, skipping", chunkX, chunkZ);
                return null;
            } else {
                Chunk chunk = WorldCache.getChunk(world, nbtCompound);
                if (!chunk.isChunkEqual(chunkX, chunkZ)) {
                    ThreadedAnvilChunkStorageAccessor.getLOGGER().error("Chunk file at {},{} is in the wrong location; relocating. (Expected {}, {}, got {}, {})", chunkX, chunkZ, chunkX, chunkZ, chunk.chunkX, chunk.chunkZ);
                    nbtCompound.putInt("xPos", chunkX);
                    nbtCompound.putInt("zPos", chunkZ);
                    chunk = WorldCache.getChunk(world, nbtCompound);
                }

                return chunk;
            }
        }
    }

    // method created by merging
    private static Chunk getChunk(ServerWorld world, NbtCompound nbt) {
        int i = nbt.getInt("xPos");
        int j = nbt.getInt("zPos");
        Chunk chunk = new Chunk(world, i, j);
        chunk.setLevelHeightmap(nbt.getIntArray("HeightMap"));
        chunk.setTerrainPopulated(nbt.getBoolean("TerrainPopulated"));
        chunk.setLightPopulated(nbt.getBoolean("LightPopulated"));
        chunk.setInhabitedTime(nbt.getLong("InhabitedTime"));
        NbtList nbtList = nbt.getList("Sections", 10);
        ChunkSection[] chunkSections = new ChunkSection[16];
        boolean bl = world.dimension.isOverworld();

        for (int l = 0; l < nbtList.size(); ++l) {
            NbtCompound nbtCompound = nbtList.getCompound(l);
            int m = nbtCompound.getByte("Y");
            ChunkSection chunkSection = new ChunkSection(m << 4, bl);
            byte[] bs = nbtCompound.getByteArray("Blocks");
            ChunkNibbleArray chunkNibbleArray = new ChunkNibbleArray(nbtCompound.getByteArray("Data"));
            ChunkNibbleArray chunkNibbleArray2 = nbtCompound.contains("Add", 7) ? new ChunkNibbleArray(nbtCompound.getByteArray("Add")) : null;
            chunkSection.method_11774().load(bs, chunkNibbleArray, chunkNibbleArray2);
            chunkSection.setBlockLight(new ChunkNibbleArray(nbtCompound.getByteArray("BlockLight")));
            if (bl) {
                chunkSection.setSkyLight(new ChunkNibbleArray(nbtCompound.getByteArray("SkyLight")));
            }

            chunkSection.calculateCounts();
            chunkSections[m] = chunkSection;
        }

        chunk.setLevelChunkSections(chunkSections);
        if (nbt.contains("Biomes", 7)) {
            chunk.setBiomeArray(nbt.getByteArray("Biomes"));
        }


        NbtList nbtList2 = nbt.getList("Entities", 10);

        for (int n = 0; n < nbtList2.size(); ++n) {
            NbtCompound nbtCompound2 = nbtList2.getCompound(n);
            ThreadedAnvilChunkStorage.method_11783(nbtCompound2, world, chunk);
            chunk.setHasEntities(true);
        }

        NbtList nbtList3 = nbt.getList("TileEntities", 10);

        for (int m = 0; m < nbtList3.size(); ++m) {
            NbtCompound nbtCompound3 = nbtList3.getCompound(m);
            BlockEntity blockEntity = BlockEntity.create(world, nbtCompound3);
            if (blockEntity != null) {
                chunk.addBlockEntity(blockEntity);
            }
        }

        if (nbt.contains("TileTicks", 9)) {
            NbtList nbtList4 = nbt.getList("TileTicks", 10);

            for (int o = 0; o < nbtList4.size(); ++o) {
                NbtCompound nbtCompound4 = nbtList4.getCompound(o);
                Block block;
                if (nbtCompound4.contains("i", 8)) {
                    block = Block.get(nbtCompound4.getString("i"));
                } else {
                    block = Block.getById(nbtCompound4.getInt("i"));
                }

                world.scheduleTick(new BlockPos(nbtCompound4.getInt("x"), nbtCompound4.getInt("y"), nbtCompound4.getInt("z")), block, nbtCompound4.getInt("t"), nbtCompound4.getInt("p"));
            }
        }

        return chunk;
    }
}