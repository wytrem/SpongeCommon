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

import net.minecraft.inventory.InventoryBasic;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.item.inventory.adapter.impl.MinecraftInventoryAdapter;
import org.spongepowered.common.item.inventory.custom.CustomInventory;
import org.spongepowered.common.item.inventory.custom.CustomLens;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotLensCollection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
@Mixin(CustomInventory.class)
public abstract class MixinCustomInventory implements MinecraftInventoryAdapter, Inventory, CarriedInventory<Carrier> {

    @Shadow(remap = false) protected InventoryArchetype archetype;
    @Shadow(remap = false) private InventoryBasic inv;
    @Shadow(remap = false) private Carrier carrier;

    private SlotLensCollection slots;
    private CustomLens lens;
    private PluginContainer plugin;

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>*", at = @At("RETURN"), remap = false)
    private void onConstructed(InventoryArchetype archetype, Map<String, InventoryProperty<?, ?>> properties, Carrier carrier, Map<Class<? extends
            InteractInventoryEvent>, List<Consumer<? extends InteractInventoryEvent>>> listeners, boolean isVirtual, PluginContainer plugin,CallbackInfo ci) {
        this.slots = new SlotLensCollection.Builder().add(this.inv.getSizeInventory()).build();
        this.lens = new CustomLens(this, this.slots, archetype, properties);
        this.plugin = plugin;
    }

    @Override
    public Lens getRootLens() {
        return this.lens;
    }

    @Override
    public InventoryArchetype getArchetype() {
        return this.archetype;
    }

    @Override
    public Optional<Carrier> getCarrier() {
        return Optional.ofNullable(this.carrier);
    }

    @Override
    public PluginContainer getPlugin() {
        return this.plugin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SlotProvider getSlotProvider() {
        return this.slots;
    }
}
