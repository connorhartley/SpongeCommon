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
package org.spongepowered.common.mixin.core.tileentity;

import co.aikar.timings.Timing;
import com.google.common.base.MoreObjects;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.bridge.TimingBridge;
import org.spongepowered.common.bridge.TrackableBridge;
import org.spongepowered.common.bridge.data.DataCompoundHolder;
import org.spongepowered.common.bridge.tileentity.TileEntityBridge;
import org.spongepowered.common.data.SpongeDataManager;
import org.spongepowered.common.relocate.co.aikar.timings.SpongeTimings;

import javax.annotation.Nullable;

@Mixin(net.minecraft.tileentity.TileEntity.class)
public abstract class TileEntityMixin implements TileEntityBridge, DataCompoundHolder, TimingBridge {

    //@formatter:off
    @Shadow @Final private TileEntityType<?> type;
    @Shadow @Nullable private BlockState cachedBlockState;
    @Shadow protected net.minecraft.world.World world;
    @Shadow protected BlockPos pos;

    @Shadow public abstract BlockPos shadow$getPos();
    @Shadow public abstract BlockState shadow$getBlockState();
    @Shadow public abstract void shadow$markDirty();
    //@formatter:on
    @Nullable private Timing impl$timing;

    /**
     * Read extra data (SpongeData) from the tile entity's NBT tag.
     *
     * @param compound The SpongeData compound to read from
     */
    protected void bridge$readFromSpongeCompound(final CompoundNBT compound) {
        throw new UnsupportedOperationException("Implement me");
    }

    /**
     * Write extra data (SpongeData) to the tile entity's NBT tag.
     *
     * @param compound The SpongeData compound to write to
     */
    protected void bridge$writeToSpongeCompound(final CompoundNBT compound) {
        SpongeDataManager.getInstance().serializeCustomData(compound, this);
    }

    @Override
    public Timing bridge$getTimingsHandler() {
        if (this.impl$timing == null) {
            this.impl$timing = SpongeTimings.getTileEntityTiming((BlockEntity) this);
        }
        return this.impl$timing;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                // Double check some mods are registering their tile entities and doing some "interesting"
                // things with doing a to string on a tile entity not actually functionally valid in the game "yet".
            .add("tileType", ((BlockEntityType) this.type).getKey())
            .add("world", this.world)
            .add("pos", this.pos)
            .add("blockMetadata", this.cachedBlockState)
            .toString();
    }

    protected MoreObjects.ToStringHelper getPrettyPrinterStringHelper() {
        return MoreObjects.toStringHelper(this)
            .add("type", ((BlockEntityType) this.type).getKey())
            .add("world", this.world.getWorldInfo().getWorldName())
            .add("pos", this.pos);
    }

    @Override
    public String bridge$getPrettyPrinterString() {
        return this.getPrettyPrinterStringHelper().toString();
    }
}
