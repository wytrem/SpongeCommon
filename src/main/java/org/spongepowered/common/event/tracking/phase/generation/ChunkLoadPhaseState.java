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
package org.spongepowered.common.event.tracking.phase.generation;

import org.spongepowered.api.event.CauseStackManager;

import java.util.function.BiConsumer;

public final class ChunkLoadPhaseState extends GeneralGenerationPhaseState<ChunkLoadContext> {

    private final BiConsumer<CauseStackManager.StackFrame, ChunkLoadContext> CHUNK_LOAD_MODIFIER =
        super.getFrameModifier().andThen((frame, context) -> {
            frame.pushCause(context.getChunk());
        });

    public ChunkLoadPhaseState() {
        super("CHUNK_LOAD");
    }

    @Override
    public ChunkLoadContext createPhaseContext() {
        return new ChunkLoadContext(this);
    }

    @Override
    public BiConsumer<CauseStackManager.StackFrame, ChunkLoadContext> getFrameModifier() {
        return this.CHUNK_LOAD_MODIFIER;
    }
}
