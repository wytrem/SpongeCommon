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
package org.spongepowered.common.event.tracking.phase.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlaceRecipe;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingInventory;
import org.spongepowered.api.item.inventory.crafting.CraftingOutput;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.recipe.Recipe;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.interfaces.IMixinContainer;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.List;

public class PlaceRecipePacketState extends BasicInventoryPacketState {

    @Override
    public void populateContext(EntityPlayerMP playerMP, Packet<?> packet, InventoryPacketContext context) {
        ((IMixinContainer) playerMP.openContainer).setCaptureInventory(true);
        ((IMixinContainer) playerMP.openContainer).setFirePreview(false);
    }

    @Override
    public void unwind(InventoryPacketContext context) {
        CPacketPlaceRecipe packet = context.getPacket();
        boolean shift = packet.func_194319_c();
        IRecipe recipe = packet.func_194317_b();

        final EntityPlayerMP player = context.getPacketPlayer();
        ((IMixinContainer)player.openContainer).detectAndSendChanges(true);
        ((IMixinContainer) player.openContainer).setCaptureInventory(false);
        ((IMixinContainer) player.openContainer).setFirePreview(true);

        Inventory craftInv = ((Inventory) player.openContainer).query(QueryOperationTypes.INVENTORY_TYPE.of(CraftingInventory.class));
        if (!(craftInv instanceof CraftingInventory)) {
            SpongeImpl.getLogger().warn("Detected crafting without a InventoryCrafting!? Crafting Event will not fire.");
            return;
        }

        List<SlotTransaction> previewTransactions = ((IMixinContainer) player.openContainer).getPreviewTransactions();
        if (previewTransactions.isEmpty()) {
            CraftingOutput slot = ((CraftingInventory) craftInv).getResult();
            SlotTransaction st = new SlotTransaction(slot, ItemStackSnapshot.NONE, ItemStackUtil.snapshotOf(slot.peek()));
            previewTransactions.add(st);
        }
        SpongeCommonEventFactory.callCraftEventPre(player, ((CraftingInventory) craftInv), previewTransactions.get(0),
                ((CraftingRecipe) recipe), player.openContainer, previewTransactions);
        previewTransactions.clear();

        final Entity spongePlayer = EntityUtil.fromNative(player);
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(spongePlayer);
            frame.pushCause(player.openContainer);

            List<SlotTransaction> transactions = ((IMixinContainer) player.openContainer).getCapturedTransactions();
            ItemStackSnapshot cursor = ItemStackUtil.snapshotOf(player.inventory.getItemStack());
            Transaction<ItemStackSnapshot> cursorTransaction = new Transaction<>(cursor, cursor);
            ClickInventoryEvent event;
            if (shift) {
                event = SpongeEventFactory.createClickInventoryEventRecipeAll(frame.getCurrentCause(),
                        cursorTransaction, (Recipe) recipe, ((Container) player.openContainer), transactions);
            } else {
                event = SpongeEventFactory.createClickInventoryEventRecipeSingle(frame.getCurrentCause(),
                        cursorTransaction, (Recipe) recipe, ((Container) player.openContainer), transactions);
            }
            SpongeImpl.postEvent(event);
            if (event.isCancelled() || !event.getCursorTransaction().isValid()) {
                PacketPhaseUtil.handleCustomCursor(player, event.getCursorTransaction().getOriginal());
            } else {
                PacketPhaseUtil.handleCustomCursor(player, event.getCursorTransaction().getFinal());
            }
            PacketPhaseUtil.handleSlotRestore(player, player.openContainer, event.getTransactions(), event.isCancelled());
            event.getTransactions().clear();
        }
    }
}
