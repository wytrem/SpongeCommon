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
package org.spongepowered.common.world.gen.builders;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.util.weighted.WeightedObject;
import org.spongepowered.api.util.weighted.WeightedTable;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.api.world.gen.PopulatorObject;
import org.spongepowered.api.world.gen.populator.Forest;
import org.spongepowered.api.world.gen.populator.Forest.Builder;
import org.spongepowered.common.world.gen.populators.ForestPopulator;

import java.util.function.Function;

import javax.annotation.Nullable;

public class ForestBuilder implements Forest.Builder {
    
    private VariableAmount count;
    private WeightedTable<PopulatorObject> types;
    @Nullable private Function<Location<Extent>, PopulatorObject> override;
    
    public ForestBuilder() {
        reset();
    }

    @Override
    public Builder perChunk(VariableAmount count) {
        this.count = count;
        return this;
    }

    @Override
    public Builder types(WeightedTable<PopulatorObject> types) {
        checkNotNull(types, "types");
        this.types = types;
        return this;
    }

    @Override
    public Builder type(PopulatorObject type, double weight) {
        checkNotNull(type);
        checkArgument(weight > 0);
        this.types.add(new WeightedObject<PopulatorObject>(type, weight));
        return this;
    }

    @Override
    public Builder supplier(@Nullable Function<Location<Extent>, PopulatorObject> override) {
        this.override = override;
        return this;
    }

    @Override
    public Builder from(Forest value) {
        WeightedTable<PopulatorObject> table = new WeightedTable<>();
        table.addAll(value.getTypes());
        return perChunk(value.getTreesPerChunk())
            .types(table)
            .supplier(value.getSupplierOverride().orElse(null));
    }

    @Override
    public Builder reset() {
        this.types = new WeightedTable<>();
        this.count = VariableAmount.fixed(5);
        this.override = null;
        return this;
    }

    @Override
    public Forest build() throws IllegalStateException {
        ForestPopulator pop = new ForestPopulator();
        pop.setTreesPerChunk(this.count);
        pop.getTypes().addAll(this.types);
        pop.setSupplierOverride(this.override);
        return pop;
    }

}
