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
package org.spongepowered.common.mixin.core.entity.ai;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.passive.EntityAnimal;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.Ageable;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.TristateResult;
import org.spongepowered.api.event.entity.BreedEntityEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.ShouldFire;

import java.util.Optional;

@Mixin(EntityAIMate.class)
public abstract class MixinEntityAIMate {

    @Shadow @Final private EntityAnimal animal;
    @Shadow private EntityAnimal targetMate;
    @Shadow private EntityAnimal getNearbyMate() {
        // Shadow implements
        return null;
    }

    @Redirect(method = "shouldExecute", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/EntityAIMate;getNearbyMate()Lnet/minecraft/entity/passive/EntityAnimal;"))
    private EntityAnimal callFindMateEvent(final EntityAIMate entityAIMate) {
        EntityAnimal nearbyMate = this.getNearbyMate();
        if (nearbyMate == null) {
            return null;
        }

        if (ShouldFire.BREED_ENTITY_EVENT_FIND_MATE) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(this.animal);
                final BreedEntityEvent.FindMate event =
                    SpongeEventFactory.createBreedEntityEventFindMate(Sponge.getCauseStackManager().getCurrentCause(), TristateResult.Result.DEFAULT,
                        TristateResult.Result.DEFAULT, Optional.empty(), (Animal) nearbyMate, (Ageable) this.animal, true);
                final boolean cancelled = SpongeImpl.postEvent(event);
                if (cancelled || event.getResult() == TristateResult.Result.DENY) {
                    nearbyMate = null;
                }
            }
        }

        return nearbyMate;
    }

    @Inject(method = "spawnBaby()V", at = @At(value = "INVOKE_ASSIGN", target = "net/minecraft/entity/passive/EntityAnimal.createChild"
        + "(Lnet/minecraft/entity/EntityAgeable;)Lnet/minecraft/entity/EntityAgeable;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private void callBreedEvent(CallbackInfo ci, EntityAgeable entityageable) {
        if (ShouldFire.BREED_ENTITY_EVENT_BREED) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                // TODO API 8 is removing this TargetXXXX nonsense so that is why I put the parents into the Cause
                frame.pushCause(this.targetMate);
                frame.pushCause(this.animal);
                final BreedEntityEvent.Breed event = SpongeEventFactory.createBreedEntityEventBreed(Sponge.getCauseStackManager().getCurrentCause(),
                    Optional.empty(), (Ageable) entityageable, (Ageable) this.targetMate);
                SpongeImpl.postEvent(event);
                if (event.isCancelled()) {
                    ci.cancel();
                }
            }
        }
    }

}
