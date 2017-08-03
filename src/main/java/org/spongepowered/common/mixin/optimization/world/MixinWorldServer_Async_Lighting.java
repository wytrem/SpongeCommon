/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.optimization.world;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.util.math.IMixinBlockPos;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkProviderServer;
import org.spongepowered.common.mixin.core.world.MixinWorld;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(value = WorldServer.class)
public abstract class MixinWorldServer_Async_Lighting extends MixinWorld implements IMixinWorldServer {

    private ExecutorService lightExecutorService =
            Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Sponge - Async Light Thread").build());

    @Override
    public boolean checkLightFor(EnumSkyBlock lightType, BlockPos pos) {
        return this.updateLightAsync(lightType, pos);
    }

    @Override
    public boolean checkLightAsync(EnumSkyBlock lightType, BlockPos pos, net.minecraft.world.chunk.Chunk currentChunk, List<Chunk> neighbors) {
        // Sponge - This check is not needed as neighbors are checked in updateLightAsync
        if (false && !this.isAreaLoaded(pos, 17, false)) {
            return false;
        } else {
            final IMixinChunk spongeChunk = (IMixinChunk) currentChunk;
            int i = 0;
            int j = 0;
            //this.theProfiler.startSection("getBrightness"); // Sponge - don't use profiler off of main thread
            int k = this.getLightForAsync(lightType, pos, currentChunk, neighbors); // Sponge - use thread safe method
            int l = this.getRawBlockLightAsync(lightType, pos, currentChunk, neighbors); // Sponge - use thread safe method
            int i1 = pos.getX();
            int j1 = pos.getY();
            int k1 = pos.getZ();

            if (l > k) {
                this.lightUpdateBlockList[j++] = 133152;
            } else if (l < k) {
                this.lightUpdateBlockList[j++] = 133152 | k << 18;

                while (i < j) {
                    int l1 = this.lightUpdateBlockList[i++];
                    int i2 = (l1 & 63) - 32 + i1;
                    int j2 = (l1 >> 6 & 63) - 32 + j1;
                    int k2 = (l1 >> 12 & 63) - 32 + k1;
                    int l2 = l1 >> 18 & 15;
                    BlockPos blockpos = new BlockPos(i2, j2, k2);
                    int i3 = this.getLightForAsync(lightType, blockpos, currentChunk, neighbors); // Sponge - use thread safe method

                    if (i3 == l2) {
                        this.setLightForAsync(lightType, blockpos, 0, currentChunk, neighbors); // Sponge - use thread safe method

                        if (l2 > 0) {
                            int j3 = MathHelper.abs_int(i2 - i1);
                            int k3 = MathHelper.abs_int(j2 - j1);
                            int l3 = MathHelper.abs_int(k2 - k1);

                            if (j3 + k3 + l3 < 17) {
                                BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos.retain();

                                for (EnumFacing enumfacing : EnumFacing.values()) {
                                    int i4 = i2 + enumfacing.getFrontOffsetX();
                                    int j4 = j2 + enumfacing.getFrontOffsetY();
                                    int k4 = k2 + enumfacing.getFrontOffsetZ();
                                    blockpos$pooledmutableblockpos.setPos(i4, j4, k4);
                                    // Sponge start - get chunk safely
                                    final Chunk pooledChunk = this.getLightChunk(blockpos$pooledmutableblockpos, currentChunk, neighbors);
                                    if (pooledChunk == null) {
                                        continue;
                                    }
                                    int l4 = Math.max(1, pooledChunk.getBlockState(blockpos$pooledmutableblockpos).getLightOpacity());
                                    i3 = this.getLightForAsync(lightType, blockpos$pooledmutableblockpos, currentChunk, neighbors);
                                    // Sponge end

                                    if (i3 == l2 - l4 && j < this.lightUpdateBlockList.length) {
                                        this.lightUpdateBlockList[j++] = i4 - i1 + 32 | j4 - j1 + 32 << 6 | k4 - k1 + 32 << 12 | l2 - l4 << 18;
                                    }
                                }

                                blockpos$pooledmutableblockpos.release();
                            }
                        }
                    }
                }

                i = 0;
            }

            //this.theProfiler.endSection(); // Sponge - don't use profiler off of main thread
            //this.theProfiler.startSection("checkedPosition < toCheckCount"); // Sponge - don't use profiler off of main thread

            while (i < j) {
                int i5 = this.lightUpdateBlockList[i++];
                int j5 = (i5 & 63) - 32 + i1;
                int k5 = (i5 >> 6 & 63) - 32 + j1;
                int l5 = (i5 >> 12 & 63) - 32 + k1;
                BlockPos blockpos1 = new BlockPos(j5, k5, l5);
                int i6 = this.getLightForAsync(lightType, blockpos1, currentChunk, neighbors); // Sponge - use thread safe method
                int j6 = this.getRawBlockLightAsync(lightType, blockpos1, currentChunk, neighbors); // Sponge - use thread safe method

                if (j6 != i6) {
                    this.setLightForAsync(lightType, blockpos1, j6, currentChunk, neighbors); // Sponge - use thread safe method

                    if (j6 > i6) {
                        int k6 = Math.abs(j5 - i1);
                        int l6 = Math.abs(k5 - j1);
                        int i7 = Math.abs(l5 - k1);
                        boolean flag = j < this.lightUpdateBlockList.length - 6;

                        if (k6 + l6 + i7 < 17 && flag) {
                            // Sponge start - use thread safe method getLightForAsync
                            if (this.getLightForAsync(lightType, blockpos1.west(), currentChunk, neighbors) < j6) {
                                this.lightUpdateBlockList[j++] = j5 - 1 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightForAsync(lightType, blockpos1.east(), currentChunk, neighbors) < j6) {
                                this.lightUpdateBlockList[j++] = j5 + 1 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightForAsync(lightType, blockpos1.down(), currentChunk, neighbors) < j6) {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 - 1 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightForAsync(lightType, blockpos1.up(), currentChunk, neighbors) < j6) {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 + 1 - j1 + 32 << 6) + (l5 - k1 + 32 << 12);
                            }

                            if (this.getLightForAsync(lightType, blockpos1.north(), currentChunk, neighbors) < j6) {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 - 1 - k1 + 32 << 12);
                            }

                            if (this.getLightForAsync(lightType, blockpos1.south(), currentChunk, neighbors) < j6) {
                                this.lightUpdateBlockList[j++] = j5 - i1 + 32 + (k5 - j1 + 32 << 6) + (l5 + 1 - k1 + 32 << 12);
                            }
                            // Sponge end
                        }
                    }
                }
            }

            // Sponge start - Asynchronous light updates
            if (SpongeImpl.getGlobalConfig().getConfig().getOptimizations().useAsyncLighting()) {
                spongeChunk.getPendingLightUpdates().decrementAndGet();
                for (net.minecraft.world.chunk.Chunk neighborChunk : neighbors) {
                    final IMixinChunk neighbor = (IMixinChunk) neighborChunk;
                    neighbor.getPendingLightUpdates().decrementAndGet();
                }
            }
            // Sponge end
            //this.theProfiler.endSection(); // Sponge - don't use profiler off of main thread
            return true;
        }
    }

    @Override
    public boolean updateLightAsync(EnumSkyBlock lightType, BlockPos pos) {
        if (this.getMinecraftServer().isServerStopped() || this.lightExecutorService.isShutdown()) {
            return false;
        }

        final net.minecraft.world.chunk.Chunk chunk =
                ((IMixinChunkProviderServer) this.getChunkProvider()).getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);
        IMixinChunk spongeChunk = (IMixinChunk) chunk;
        if (chunk == null || chunk.unloaded || !spongeChunk.areNeighborsLoaded()) {
            return false;
        }

        spongeChunk.getPendingLightUpdates().incrementAndGet();
        spongeChunk.setLightUpdateTime(chunk.getWorld().getTotalWorldTime());

        List<Chunk> neighbors = spongeChunk.getNeighbors();
        // add diagonal chunks
        Chunk southEastChunk = ((IMixinChunk) spongeChunk.getNeighborChunk(0)).getNeighborChunk(2);
        Chunk southWestChunk = ((IMixinChunk) spongeChunk.getNeighborChunk(0)).getNeighborChunk(3);
        Chunk northEastChunk = ((IMixinChunk) spongeChunk.getNeighborChunk(1)).getNeighborChunk(2);
        Chunk northWestChunk = ((IMixinChunk) spongeChunk.getNeighborChunk(1)).getNeighborChunk(3);
        if (southEastChunk != null) {
            neighbors.add(southEastChunk);
        }
        if (southWestChunk != null) {
            neighbors.add(southWestChunk);
        }
        if (northEastChunk != null) {
            neighbors.add(northEastChunk);
        }
        if (northWestChunk != null) {
            neighbors.add(northWestChunk);
        }

        for (net.minecraft.world.chunk.Chunk neighborChunk : neighbors) {
            final IMixinChunk neighbor = (IMixinChunk) neighborChunk;
            neighbor.getPendingLightUpdates().incrementAndGet();
            neighbor.setLightUpdateTime(chunk.getWorld().getTotalWorldTime());
        }

        this.lightExecutorService.execute(() -> {
            this.checkLightAsync(lightType, pos, chunk, neighbors);
        });

        return true;
    }

    @Override
    public ExecutorService getLightingExecutor() {
        return this.lightExecutorService;
    }

    // Thread safe methods to retrieve a chunk during async light updates
    // Each method avoids calling getLoadedChunk and instead accesses the passed neighbor chunk list to avoid concurrency issues
    public Chunk getLightChunk(BlockPos pos, Chunk currentChunk, List<Chunk> neighbors) {
        if (currentChunk.isAtLocation(pos.getX() >> 4, pos.getZ() >> 4)) {
            if (currentChunk.unloaded) {
                return null;
            }
            return currentChunk;
        }
        for (net.minecraft.world.chunk.Chunk neighbor : neighbors) {
            if (neighbor.isAtLocation(pos.getX() >> 4, pos.getZ() >> 4)) {
                if (neighbor.unloaded) {
                    return null;
                }
                return neighbor;
            }
        }

        return null;
    }

    private int getLightForAsync(EnumSkyBlock lightType, BlockPos pos, Chunk currentChunk, List<Chunk> neighbors) {
        if (pos.getY() < 0) {
            pos = new BlockPos(pos.getX(), 0, pos.getZ());
        }
        if (!((IMixinBlockPos) pos).isValidPosition()) {
            return lightType.defaultLightValue;
        }

        final Chunk chunk = this.getLightChunk(pos, currentChunk, neighbors);
        if (chunk == null || chunk.unloaded) {
            return lightType.defaultLightValue;
        }

        return chunk.getLightFor(lightType, pos);
    }

    private int getRawBlockLightAsync(EnumSkyBlock lightType, BlockPos pos, Chunk currentChunk, List<Chunk> neighbors) {
        final Chunk chunk = getLightChunk(pos, currentChunk, neighbors);
        if (chunk == null || chunk.unloaded) {
            return lightType.defaultLightValue;
        }
        if (lightType == EnumSkyBlock.SKY && chunk.canSeeSky(pos)) {
            return 15;
        } else {
            IBlockState blockState = chunk.getBlockState(pos);
            int blockLight = SpongeImplHooks.getChunkPosLight(blockState, (net.minecraft.world.World) (Object) this, pos);
            int i = lightType == EnumSkyBlock.SKY ? 0 : blockLight;
            int j = SpongeImplHooks.getBlockLightOpacity(blockState, (net.minecraft.world.World) (Object) this, pos);

            if (j >= 15 && blockLight > 0) {
                j = 1;
            }

            if (j < 1) {
                j = 1;
            }

            if (j >= 15) {
                return 0;
            } else if (i >= 14) {
                return i;
            } else {
                for (EnumFacing enumfacing : EnumFacing.values()) {
                    BlockPos blockpos = pos.offset(enumfacing);
                    int k = this.getLightForAsync(lightType, blockpos, currentChunk, neighbors) - j;

                    if (k > i) {
                        i = k;
                    }

                    if (i >= 14) {
                        return i;
                    }
                }

                return i;
            }
        }
    }

    public void setLightForAsync(EnumSkyBlock type, BlockPos pos, int lightValue, Chunk currentChunk, List<Chunk> neighbors) {
        if (((IMixinBlockPos) pos).isValidPosition()) {
            final Chunk chunk = this.getLightChunk(pos, currentChunk, neighbors);
            if (chunk != null && !chunk.unloaded) {
                chunk.setLightFor(type, pos, lightValue);
                this.notifyLightSet(pos);
            }
        }
    }
}
