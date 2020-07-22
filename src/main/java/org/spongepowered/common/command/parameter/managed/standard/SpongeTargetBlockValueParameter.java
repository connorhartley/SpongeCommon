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
package org.spongepowered.common.command.parameter.managed.standard;

import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.common.command.brigadier.argument.CatalogedZeroAdvanceValueParameter;

import java.util.Optional;

// TODO: When BlockRay returns
public final class SpongeTargetBlockValueParameter extends CatalogedZeroAdvanceValueParameter<ServerLocation> {

    private static final ResourceKey RESOURCE_KEY = ResourceKey.sponge("target_block");

    @Override
    @NonNull
    public ResourceKey getKey() {
        return SpongeTargetBlockValueParameter.RESOURCE_KEY;
    }

    @Override
    @NonNull
    public Optional<? extends ServerLocation> getValue(final CommandContext.@NonNull Builder context, final ArgumentReader.@NonNull Mutable reader)
            throws ArgumentParseException {
        final Object root = context.getCause().root();
        if (root instanceof Living) {
            // TODO: BlockRay, where art thou?
            throw reader.createException(TextComponent.of("The cause root is not looking at a block!"));
        }

        throw reader.createException(TextComponent.of("The cause root must be a Living!"));
    }

}
