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
package org.spongepowered.common.item.inventory.lens.impl.minecraft.container;

import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.comp.CraftingInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.minecraft.PlayerInventoryLens;

import java.util.Arrays;
import java.util.List;

public class ContainerPlayerInventoryLens extends ContainerLens {

    private static final int CRAFTING_OUTPUT = 1;
    private static final int CRAFTING_GRID = 2;

    public ContainerPlayerInventoryLens(InventoryAdapter adapter, SlotProvider slots) {
        super(adapter, slots, lenses(adapter, slots));
        this.init(slots);
    }

    private static List<Lens> lenses(InventoryAdapter adapter, SlotProvider slots) {

        int base = CRAFTING_OUTPUT; // 1
        final CraftingInventoryLensImpl crafting = new CraftingInventoryLensImpl(0, base, CRAFTING_GRID, CRAFTING_GRID, slots);
        base += CRAFTING_GRID * CRAFTING_GRID; // 4
        final PlayerInventoryLens player = new PlayerInventoryLens(base, adapter.getFabric().getSize() - base, slots);

        // TODO equipment properties?

        return Arrays.asList(crafting, player);
    }

}
