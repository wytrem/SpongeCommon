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
package org.spongepowered.common.mixin.core.item.inventory;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.interfaces.IMixinContainer;
import org.spongepowered.common.interfaces.IMixinInteractable;
import org.spongepowered.common.interfaces.entity.player.IMixinEntityPlayer;
import org.spongepowered.common.item.inventory.adapter.impl.MinecraftInventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.SlotCollection;
import org.spongepowered.common.item.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.fabric.MinecraftFabric;
import org.spongepowered.common.item.inventory.util.ContainerUtil;
import org.spongepowered.common.item.inventory.util.InventoryUtil;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
@NonnullByDefault
@Mixin(value = Container.class, priority = 998)
@Implements({@Interface(iface = MinecraftInventoryAdapter.class, prefix = "inventory$")})
public abstract class MixinContainer implements org.spongepowered.api.item.inventory.Container, IMixinContainer, CarriedInventory<Carrier> {

    @Shadow public List<Slot> inventorySlots;
    @Shadow public NonNullList<ItemStack> inventoryItemStacks ;
    @Shadow public int windowId;
    @Shadow protected List<IContainerListener> listeners;
    private boolean spectatorChest;
    private boolean dirty = true;
    private boolean dropCancelled = false;
    @Nullable private ItemStackSnapshot itemStackSnapshot;
    @Nullable private Slot lastSlotUsed = null;
    @Nullable private CraftItemEvent.Craft lastCraft = null;
    private boolean firePreview;
    @Nullable private Location<org.spongepowered.api.world.World> lastOpenLocation;
    private boolean inUse = false;

    @Shadow
    public abstract NonNullList<ItemStack> getInventory();

    @Shadow
    public abstract Slot getSlot(int slotId);

    @Shadow
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        throw new IllegalStateException("Shadowed.");
    }

    @Shadow protected abstract void resetDrag();

    private boolean captureInventory = false;
    private boolean shiftCraft = false;
    //private boolean postPreCraftEvent = true; // used to prevent multiple craft events to fire when setting multiple slots simultaneously
    private List<SlotTransaction> capturedSlotTransactions = new ArrayList<>();
    private List<SlotTransaction> capturedCraftShiftTransactions = new ArrayList<>();
    private List<SlotTransaction> capturedCraftPreviewTransactions = new ArrayList<>();
    private Fabric fabric;
    private SlotProvider slots;
    private Lens lens;
    private boolean initialized;
    private Map<Integer, SlotAdapter> adapters = new HashMap<>();
    private InventoryArchetype archetype;
    protected Optional<Carrier> carrier = Optional.empty();
    protected Optional<Predicate<EntityPlayer>> canInteractWithPredicate = Optional.empty();
    @Nullable private PluginContainer plugin = null;

    private LinkedHashMap<IInventory, Set<Slot>> allInventories = new LinkedHashMap<>();

    /*
    Named specifically for sponge to avoid potential illegal access errors when a mod container
    implements an interface that adds a defaulted method. Due to the JVM and compiled bytecode,
    this could be called in the event the interface with the defaulted method doesn't get
    overridden in the subclass, and therefor, will call the superclass (this class) method, and
    then bam... error.
    More specifically fixes: https://github.com/BuildCraft/BuildCraft/issues/4005
     */
    private void spongeInit() {
        if (this.initialized && !this.dirty) {
            return;
        }

        this.dirty = false;
        this.initialized = true;
        this.adapters.clear();
        this.fabric = MinecraftFabric.of(this);
        this.slots = ContainerUtil.countSlots((Container) (Object) this, this.fabric);
        this.lens = null;
        this.lens = this.spectatorChest ? null : ContainerUtil.getLens(this.fabric, (Container) (Object) this, this.slots); // TODO handle spectator
        this.archetype = ContainerUtil.getArchetype((Container) (Object) this);
        this.carrier = Optional.ofNullable(ContainerUtil.getCarrier(this));

        // If we know the lens, we can cache the adapters now
        if (this.lens != null) {
            for (org.spongepowered.api.item.inventory.Slot slot : new SlotCollection(this, this.fabric, this.lens, this.slots).slots()) {
                this.adapters.put(((SlotAdapter) slot).getOrdinal(), (SlotAdapter) slot);
            }
        }

        this.allInventories.clear();
        this.inventorySlots.forEach(slot -> this.allInventories.computeIfAbsent(slot.inventory, (i) -> new HashSet<>()).add(slot));

    }

    @Override
    public InventoryArchetype getArchetype() {
        this.spongeInit();
        return this.archetype;
    }

    /**
     * @author bloodmc
     * @reason If listener already exists, avoid firing an exception
     * and simply send the inventory changes to client.
     */
    @Overwrite
    public void addListener(IContainerListener listener) {
        Container container = (Container) (Object) this;
        if (this.listeners.contains(listener)) {
            // Sponge start
            // throw new IllegalArgumentException("Listener already listening");
            listener.sendAllContents(container, this.getInventory());
            container.detectAndSendChanges();
            // Sponge end
        } else {
            this.listeners.add(listener);
            listener.sendAllContents(container, this.getInventory());
            container.detectAndSendChanges();
        }
    }

    /**
     * @author bloodmc
     * @reason All player fabric changes that need to be synced to
     * client flow through this method. Overwrite is used as no mod
     * should be touching this method.
     *
     */
    @Overwrite
    public void detectAndSendChanges() {
        this.detectAndSendChanges(false);
    }

    @Override
    public void detectAndSendChanges(boolean captureOnly) {
        this.spongeInit();

        for (int i = 0; i < this.inventorySlots.size(); ++i) {
            final Slot slot = this.inventorySlots.get(i);
            final ItemStack itemstack = slot.getStack();
            ItemStack itemstack1 = this.inventoryItemStacks.get(i);

            if (!ItemStack.areItemStacksEqual(itemstack1, itemstack)) {

                // Sponge start
                if (this.captureInventory) {
                    final ItemStackSnapshot originalItem = itemstack1.isEmpty() ? ItemStackSnapshot.NONE
                            : ((org.spongepowered.api.item.inventory.ItemStack) itemstack1).createSnapshot();
                    final ItemStackSnapshot newItem = itemstack.isEmpty() ? ItemStackSnapshot.NONE
                            : ((org.spongepowered.api.item.inventory.ItemStack) itemstack).createSnapshot();

                    org.spongepowered.api.item.inventory.Slot adapter = null;
                    try {
                        adapter = this.getContainerSlot(i);
                        SlotTransaction newTransaction = new SlotTransaction(adapter, originalItem, newItem);
                        if (this.shiftCraft) {
                            this.capturedCraftShiftTransactions.add(newTransaction);
                        } else {
                            if (!this.capturedCraftPreviewTransactions.isEmpty()) { // Check if Preview transaction is this transaction
                                SlotTransaction previewTransaction = this.capturedCraftPreviewTransactions.get(0);
                                if (previewTransaction.equals(newTransaction)) {
                                    newTransaction = null;
                                }
                            }
                            if (newTransaction != null) {
                                this.capturedSlotTransactions.add(newTransaction);
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        SpongeImpl.getLogger().error("SlotIndex out of LensBounds! Did the Container change after creation?", e);
                    }

                    // This flag is set only when the client sends an invalid CPacketWindowClickItem packet.
                    // We simply capture in order to send the proper changes back to client.
                    if (captureOnly) {
                        continue;
                    }
                }
                // Sponge end

                itemstack1 = itemstack.copy();
                this.inventoryItemStacks.set(i, itemstack1);

                for (IContainerListener listener : this.listeners) {
                    listener.sendSlotContents((Container) (Object) this, i, itemstack1);
                }
            }
        }
    }

    @Inject(method = "addSlotToContainer", at = @At(value = "HEAD"))
    public void onAddSlotToContainer(Slot slotIn, CallbackInfoReturnable<Slot> cir) {
        this.dirty = true;
    }

    @Inject(method = "putStackInSlot", at = @At(value = "HEAD") )
    public void onPutStackInSlot(int slotId, ItemStack itemstack, CallbackInfo ci) {
        if (this.captureInventory) {
            this.spongeInit();

            final Slot slot = getSlot(slotId);
            if (slot != null) {
                ItemStackSnapshot originalItem = slot.getStack().isEmpty() ? ItemStackSnapshot.NONE
                        : ((org.spongepowered.api.item.inventory.ItemStack) slot.getStack()).createSnapshot();
                ItemStackSnapshot newItem =
                        itemstack.isEmpty() ? ItemStackSnapshot.NONE : ((org.spongepowered.api.item.inventory.ItemStack) itemstack).createSnapshot();

                org.spongepowered.api.item.inventory.Slot adapter = this.getContainerSlot(slotId);
                this.capturedSlotTransactions.add(new SlotTransaction(adapter, originalItem, newItem));
            }
        }
    }

    @Redirect(method = "slotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/EntityItem;", ordinal = 0))
    public EntityItem onDragDrop(EntityPlayer player, ItemStack itemStackIn, boolean unused) {
        final ItemStackSnapshot original = ItemStackUtil.snapshotOf(itemStackIn);
        final EntityItem entityItem = player.dropItem(itemStackIn, unused);
        if (!((IMixinEntityPlayer) player).shouldRestoreInventory()) {
            return entityItem;
        }
        if (entityItem  == null) {
            this.dropCancelled = true;
            PacketPhaseUtil.handleCustomCursor((EntityPlayerMP) player, original);
        }
        return entityItem;
    }

    @Redirect(method = "slotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/EntityItem;", ordinal = 1))
    public EntityItem onDragDropSplit(EntityPlayer player, ItemStack itemStackIn, boolean unused) {
        final EntityItem entityItem = player.dropItem(itemStackIn, unused);
        if (!((IMixinEntityPlayer) player).shouldRestoreInventory()) {
            return entityItem;
        }
        if (entityItem  == null) {
            ItemStack original = null;
            if (player.inventory.getItemStack().isEmpty()) {
                original = itemStackIn;
            } else {
                player.inventory.getItemStack().grow(1);
                original = player.inventory.getItemStack();
            }
            player.inventory.setItemStack(original);
            ((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-1, -1, original));
        }
        ((IMixinEntityPlayer) player).shouldRestoreInventory(false);
        return entityItem;
    }

    @Redirect(method = "slotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;setItemStack(Lnet/minecraft/item/ItemStack;)V", ordinal = 1))
    public void onDragCursorClear(InventoryPlayer inventoryPlayer, ItemStack itemStackIn) {
        if (!this.dropCancelled || !((IMixinEntityPlayer) inventoryPlayer.player).shouldRestoreInventory()) {
            inventoryPlayer.setItemStack(itemStackIn);
        }
        ((IMixinEntityPlayer) inventoryPlayer.player).shouldRestoreInventory(false);
        this.dropCancelled = false;
    }

    @Redirect(method = "slotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Slot;canTakeStack(Lnet/minecraft/entity/player/EntityPlayer;)Z", ordinal = 4))
    public boolean onCanTakeStack(Slot slot, EntityPlayer playerIn) {
        final boolean result = slot.canTakeStack(playerIn);
        if (result) {
            this.itemStackSnapshot = ItemStackUtil.snapshotOf(slot.getStack());
            this.lastSlotUsed = slot;
        } else {
            this.itemStackSnapshot = null;
            this.lastSlotUsed = null;
        }
        return result;
    }

    @Redirect(method = "slotClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/EntityItem;", ordinal = 3))
    public EntityItem onThrowClick(EntityPlayer player, ItemStack itemStackIn, boolean unused) {
        final EntityItem entityItem = player.dropItem(itemStackIn, true);
        if (entityItem == null && ((IMixinEntityPlayer) player).shouldRestoreInventory()) {
            final ItemStack original = ItemStackUtil.toNative(this.itemStackSnapshot.createStack());
            this.lastSlotUsed.putStack(original);
            player.openContainer.detectAndSendChanges();
            ((EntityPlayerMP) player).isChangingQuantityOnly = false;
            ((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(player.openContainer.windowId, this.lastSlotUsed.slotNumber, original));
        }
        this.itemStackSnapshot = null;
        this.lastSlotUsed = null;
        ((IMixinEntityPlayer) player).shouldRestoreInventory(false);
        return entityItem;
    }

    @Redirect(method = "slotChangedCraftingGrid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/InventoryCraftResult;setInventorySlotContents(ILnet/minecraft/item/ItemStack;)V"))
    private void beforeSlotChangedCraftingGrid(InventoryCraftResult output, int index, ItemStack itemstack)
    {
        if (!this.captureInventory) {
            // Capture Inventory is true when caused by a vanilla inventory packet
            // This is to prevent infinite loops when a client mod re-requests the recipe result after we modified/cancelled it
            output.setInventorySlotContents(index, itemstack);
            return;
        }
        this.spongeInit();
        this.capturedCraftPreviewTransactions.clear();

        ItemStackSnapshot orig = ItemStackUtil.snapshotOf(output.getStackInSlot(index));
        output.setInventorySlotContents(index, itemstack);
        ItemStackSnapshot repl = ItemStackUtil.snapshotOf(output.getStackInSlot(index));

        SlotAdapter slot = this.adapters.get(index);
        this.capturedCraftPreviewTransactions.add(new SlotTransaction(slot, orig, repl));
    }

    @Inject(method = "slotChangedCraftingGrid", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetHandlerPlayServer;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void afterSlotChangedCraftingGrid(World world, EntityPlayer player, InventoryCrafting craftingInventory, InventoryCraftResult output, CallbackInfo ci)
    {
        if (this.firePreview && !this.capturedCraftPreviewTransactions.isEmpty()) {
            Inventory inv = this.query(QueryOperationTypes.INVENTORY_TYPE.of(CraftingInventory.class));
            if (!(inv instanceof CraftingInventory)) {
                SpongeImpl.getLogger().warn("Detected crafting but Sponge could not get a CraftingInventory for " + this.getClass().getName());
                return;
            }
            SlotTransaction previewTransaction = this.capturedCraftPreviewTransactions.get(this.capturedCraftPreviewTransactions.size() - 1);

            IRecipe recipe = CraftingManager.findMatchingRecipe(craftingInventory, world);
            SpongeCommonEventFactory.callCraftEventPre(player, ((CraftingInventory) inv), previewTransaction, ((CraftingRecipe) recipe),
                    ((Container)(Object) this), this.capturedCraftPreviewTransactions);
            this.capturedCraftPreviewTransactions.clear();
        }
    }

    private ItemStack previousCursor;

    @Override
    public ItemStack getPreviousCursor() {
        return this.previousCursor;
    }

    @Inject(method = "slotClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;grow(I)V", ordinal = 1))
    private void beforeOnTakeClickWithItem(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, CallbackInfoReturnable<Integer> cir) {
       this.previousCursor = player.inventory.getItemStack().copy(); // capture previous cursor for CraftItemEvent.Craft
    }

    @Inject(method = "slotClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;setItemStack(Lnet/minecraft/item/ItemStack;)V", ordinal = 3))
    private void beforeOnTakeClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, CallbackInfoReturnable<Integer> cir) {
        this.previousCursor = player.inventory.getItemStack().copy(); // capture previous cursor for CraftItemEvent.Craft
    }

    @Redirect(method = "slotClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Slot;onTake(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", ordinal = 5))
    private ItemStack redirectOnTakeThrow(Slot slot, EntityPlayer player, ItemStack stackOnCursor) {
        this.lastCraft = null;
        ItemStack result = slot.onTake(player, stackOnCursor);
        if (this.lastCraft != null) {
            if (slot instanceof SlotCrafting) {
                if (this.lastCraft.isCancelled()) {
                    stackOnCursor.setCount(0); // do not drop crafted item when cancelled
                }
            }
        }
        return result;
    }

    @Inject(method = "slotClick", at = @At("RETURN"))
    private void onReturn(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        // Reset variables needed for CraftItemEvent.Craft
        this.lastCraft = null;
        this.previousCursor = null;
    }


    @Redirect(method = "slotClick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/Container;transferStackInSlot(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirectTransferStackInSlot(Container thisContainer, EntityPlayer player, int slotId) {
        Slot slot = thisContainer.getSlot(slotId);
        if (!(slot instanceof SlotCrafting)) {
            return thisContainer.transferStackInSlot(player, slotId);
        }
        this.lastCraft = null;
        this.shiftCraft = true;
        ItemStack result = thisContainer.transferStackInSlot(player, slotId);
        if (this.lastCraft != null) {
            if (this.lastCraft.isCancelled()) {
                result = ItemStack.EMPTY; // Return empty to stop shift-crafting
            }
        }
        this.shiftCraft = false;

        return result;
    }

    @Override
    public boolean capturingInventory() {
        return this.captureInventory;
    }

    @Override
    public void setCaptureInventory(boolean flag) {
        this.captureInventory = flag;
    }

    @Override
    public void setSpectatorChest(boolean spectatorChest) {
        this.spectatorChest = spectatorChest;
    }

    @Override
    public List<SlotTransaction> getCapturedTransactions() {
        return this.capturedSlotTransactions;
    }

    @Override
    public List<SlotTransaction> getPreviewTransactions() {
        return this.capturedCraftPreviewTransactions;
    }

    @Override
    public void setLastCraft(CraftItemEvent.Craft event) {
        this.lastCraft = event;
    }

    @Override
    public void setFirePreview(boolean firePreview) {
        this.firePreview = firePreview;
    }

    @Override
    public void setShiftCrafting(boolean flag) {
        this.shiftCraft = flag;
    }

    @Override
    public boolean isShiftCrafting() {
        return this.shiftCraft;
    }

    public SlotProvider inventory$getSlotProvider() {
        this.spongeInit();
        return this.slots;
    }

    public Lens inventory$getRootLens() {
        this.spongeInit();
        return this.lens;
    }

    public Fabric inventory$getFabric() {
        this.spongeInit();
        return this.fabric;
    }

    @Override
    public void setCanInteractWith(@Nullable Predicate<EntityPlayer> predicate) {
        this.canInteractWithPredicate = Optional.ofNullable(predicate); // TODO mixin into all classes extending container
    }

    @Override
    public Optional<Carrier> getCarrier() {
        return this.carrier;
    }

    @Override
    public org.spongepowered.api.item.inventory.Slot getContainerSlot(int slot) {
        org.spongepowered.api.item.inventory.Slot adapter = this.adapters.get(slot);
        if (adapter == null) // Slot is not in Lens
        {
            Slot mcSlot = this.inventorySlots.get(slot); // Try falling back to vanilla slot
            if (mcSlot == null)
            {
                SpongeImpl.getLogger().warn("Could not find slot #%s in Container %s", slot, getClass().getName());
                return null;
            }
            return ((org.spongepowered.api.item.inventory.Slot) mcSlot);
        }
        return adapter;
    }

    @Override
    public void setPlugin(PluginContainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public Location<org.spongepowered.api.world.World> getOpenLocation() {
        return this.lastOpenLocation;
    }

    @Override
    public void setOpenLocation(Location<org.spongepowered.api.world.World> loc) {
        this.lastOpenLocation = loc;
    }

    @Override
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    @Override
    public boolean isInUse() {
        return this.inUse;
    }

    @Override
    public boolean isViewedSlot(org.spongepowered.api.item.inventory.Slot slot) {
        this.spongeInit();
        if (slot instanceof Slot) {
            Set<Slot> set = allInventories.get(((Slot) slot).inventory);
            if (set != null) {
                if (set.contains(slot)) {
                    if (allInventories.size() == 1) {
                        return true;
                    }
                    // TODO better detection of viewer inventory - needs tracking of who views a container
                    // For now assume that a player inventory is always the viewers inventory
                    if (((Slot) slot).inventory.getClass() != InventoryPlayer.class) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Inventory> getViewed() {
        List<Inventory> list = new ArrayList<>();
        for (IInventory inv : this.allInventories.keySet()) {
            Inventory inventory = InventoryUtil.toInventory(inv, null);
            list.add(inventory);
        }
        return list;
    }

    @Override
    public void setCursor(org.spongepowered.api.item.inventory.ItemStack item) {
        ItemStack nativeStack = ItemStackUtil.toNative(item);
        this.listeners().stream().findFirst()
                .ifPresent(p -> p.inventory.setItemStack(nativeStack));
    }

    @Override
    public Optional<org.spongepowered.api.item.inventory.ItemStack> getCursor() {
        return this.listeners().stream().findFirst()
                .map(p -> p.inventory.getItemStack())
                .map(ItemStackUtil::fromNative);
    }


    @Override
    public Optional<Player> getViewer() {
        return this.listeners().stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst();
    }

    @Override
    public List<EntityPlayerMP> listeners() {
        return this.listeners.stream()
                .filter(EntityPlayerMP.class::isInstance)
                .map(EntityPlayerMP.class::cast)
                .collect(Collectors.toList());
    }

    @Nullable private Object viewed;

    @Override
    public void setViewed(Object viewed) {
        this.viewed = viewed;
    }

    @Inject(method = "onContainerClosed", at = @At(value = "HEAD"))
    private void onOnContainerClosed(EntityPlayer player, CallbackInfo ci) {
        this.unTrackInteractable(this.viewed);
        this.viewed = null;
    }

    private void unTrackInteractable(@Nullable Object inventory) {
        if (inventory instanceof Carrier) {
            inventory = ((Carrier) inventory).getInventory();
        }
        if (inventory instanceof Inventory) {
            ((Inventory) inventory).asViewable().ifPresent(i -> ((IMixinInteractable) i).removeContainer(((Container)(Object) this)));
        }
        // TODO else unknown inventory - try to provide wrapper Interactable
    }

}
