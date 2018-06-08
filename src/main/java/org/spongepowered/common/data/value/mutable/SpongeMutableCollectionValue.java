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

import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.data.value.immutable.ImmutableCollectionValue;
import org.spongepowered.api.data.value.mutable.MutableCollectionValue;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeCollectionValue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public abstract class SpongeMutableCollectionValue<E,
    C extends Collection<E>,
    M extends MutableCollectionValue<E, C, M, I>,
    I extends ImmutableCollectionValue<E, C, I, M>>
    extends SpongeMutableValue<C, M, I> implements MutableCollectionValue<E, C, M, I> {


    public SpongeMutableCollectionValue(Key<? extends Value<C>> key, C defaultValue) {
        super(key, defaultValue);
    }

    public SpongeMutableCollectionValue(Key<? extends Value<C>> key, C defaultValue, C actualValue) {
        super(key, defaultValue, actualValue);
    }

    @Override
    public M set(C value) {
        this.actualValue = checkNotNull(value);
        return (M) this;
    }

    @Override
    public M transform(Function<C, C> function) {
        this.actualValue = checkNotNull(function).apply(this.actualValue);
        return (M) this;
    }


    @Override
    public M add(E e) {
        this.actualValue.add(checkNotNull(e));
        return (M) this;
    }

    @Override
    public M addAll(Iterable<E> elements) {
        for (E e : checkNotNull(elements)) {
            this.actualValue.add(checkNotNull(e));
        }
        return (M) this;
    }

    @Override
    public M remove(E e) {
        this.actualValue.remove(checkNotNull(e));
        return (M) this;
    }

    @Override
    public M removeAll(Iterable<E> elements) {
        for (E e : elements) {
            this.actualValue.remove(checkNotNull(e));
        }
        return (M) this;
    }

    @Override
    public M removeAll(Predicate<E> predicate) {
        for (Iterator<E> iterator = this.actualValue.iterator(); iterator.hasNext(); ) {
            if (checkNotNull(predicate).test(iterator.next())) {
                iterator.remove();
            }
        }
        return (M) this;
    }

    @Override
    public int size() {
        return this.actualValue.size();
    }

    @Override
    public boolean isEmpty() {
        return this.actualValue.isEmpty();
    }

    @Override
    public boolean contains(E e) {
        return this.actualValue.contains(checkNotNull(e));
    }

    @Override
    public boolean containsAll(Collection<E> iterable) {
        return this.actualValue.containsAll(iterable);
    }

    @Override
    public boolean exists() {
        return this.actualValue != null;
    }

    @Override
    public abstract I asImmutable();

    @Override
    public abstract M copy();

    @Override
    public Optional<C> getDirect() {
        return Optional.of(this.actualValue);
    }

    @Override
    public Iterator<E> iterator() {
        return this.actualValue.iterator();
    }
}
