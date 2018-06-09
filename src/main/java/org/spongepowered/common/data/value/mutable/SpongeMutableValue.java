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
package org.spongepowered.common.data.value.mutable;

import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.common.data.value.AbstractValue;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;

import java.util.function.Function;

@SuppressWarnings("unchecked")
public class SpongeMutableValue<E, M extends Value.Mutable<E, M, I>, I extends Value.Immutable<E, I, M>> extends AbstractValue<E> implements
    Value.Mutable<E, M, I> {

    public SpongeMutableValue(Key<? extends Value<E>> key, E defaultValue) {
        this(key, defaultValue, defaultValue);
    }

    public SpongeMutableValue(Key<? extends Value<E>> key, E defaultValue, E actualValue) {
        super(key, defaultValue, actualValue);
    }

    @Override
    public M set(E value) {
        this.actualValue = value;
        return (M) this;
    }

    @Override
    public M transform(Function<E, E> function) {
        this.actualValue = function.apply(this.actualValue);
        return (M) this;
    }

    @Override
    public M asMutable() {
        return (M) this;
    }

    @Override
    public I asImmutable() {
        return (I) new ImmutableSpongeValue<>(this.getKey(), this.actualValue, this.getDefault());
    }

    @Override
    public M copy() {
        return (M) new SpongeMutableValue<>(this.getKey(), this.getDefault(), this.actualValue);
    }

    public static final class Single<E> extends SpongeMutableValue<E, Mutable.Single<E>, Immutable.Single<E>> implements Mutable.Single<E> {

        public Single(Key<? extends Value<E>> key, E defaultValue) {
            super(key, defaultValue);
        }

        public Single(Key<? extends Value<E>> key, E defaultValue, E actualValue) {
            super(key, defaultValue, actualValue);
        }


    }
}
