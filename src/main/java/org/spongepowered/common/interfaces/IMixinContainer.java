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
package org.spongepowered.common.interfaces;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public interface IMixinContainer extends IMixinInventory {

    boolean capturingInventory();

    void setCaptureInventory(boolean flag);

    void detectAndSendChanges(boolean captureOnly);

    void setCanInteractWith(@Nullable Predicate<EntityPlayer> predicate);
    
    void setSpectatorChest(boolean spectatorChest);

    Slot getContainerSlot(int slot);

    void setPlugin(PluginContainer plugin);

    void setShiftCrafting(boolean flag);
    boolean isShiftCrafting();

    void setLastCraft(CraftItemEvent.Craft event);

    net.minecraft.item.ItemStack getPreviousCursor();

    void setFirePreview(boolean firePreview);

    List<SlotTransaction> getPreviewTransactions();

    @Nullable Location<World> getOpenLocation();
    void setOpenLocation(@Nullable Location<World> loc);

    void setInUse(boolean inUse);
    boolean isInUse();

    List<EntityPlayerMP> listeners();

    void setViewed(Object viewed);

}
