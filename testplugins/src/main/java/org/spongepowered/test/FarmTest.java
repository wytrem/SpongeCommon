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
package org.spongepowered.test;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.BreedEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "farm")
public final class FarmTest {

    @Listener
    public void onGameConstruction(final GameConstructionEvent event) {
        Keys.IS_ADULT.registerEvent(Animal.class, valueChangeEvent -> {
            System.err.println(valueChangeEvent.toString());
        });
    }

    @Listener
    public void onSpawnEntity(SpawnEntityEvent event) {
        event.getEntities().stream().filter(e -> e instanceof Animal).map(e -> (Animal) e).forEach(e -> e.offer(Keys.CUSTOM_NAME_VISIBLE, true));
    }

    @Listener
    public void onBreedEntityReadyToMate(final BreedEntityEvent.ReadyToMate event) {
        System.err.println(event.getCause().toString());
    }

    @Listener
    public void onBreedEntityFindMate(final BreedEntityEvent.FindMate event) {
        System.err.println(event.getCause().toString());
    }

    @Listener
    public void onBreedEntityBreed(final BreedEntityEvent.Breed event) {
        System.err.println(event.getCause().toString());
    }
}
