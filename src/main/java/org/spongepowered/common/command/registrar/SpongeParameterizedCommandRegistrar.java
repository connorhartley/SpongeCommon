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
package org.spongepowered.common.command.registrar;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.common.command.SpongeParameterizedCommand;

import java.util.Collection;

/**
 * For use with {@link org.spongepowered.api.command.Command.Parameterized}
 */
public final class SpongeParameterizedCommandRegistrar extends SpongeCommandRegistrar<Command.Parameterized> {

    private static final TypeToken<Command.Parameterized> COMMAND_TYPE = TypeToken.of(Command.Parameterized.class);
    public static final ResourceKey CATALOG_KEY = ResourceKey.sponge("managed");
    public static final SpongeParameterizedCommandRegistrar INSTANCE = new SpongeParameterizedCommandRegistrar(CATALOG_KEY);

    private SpongeParameterizedCommandRegistrar(final ResourceKey catalogKey) {
        super(catalogKey);
    }

    @Override
    Collection<LiteralCommandNode<CommandSource>> createNode(final CommandMapping mapping, final Command.Parameterized command) {
        Preconditions.checkArgument(command instanceof SpongeParameterizedCommand, "Command must be a SpongeParameterizedCommand!");
        return ((SpongeParameterizedCommand) command).buildWithAliases(mapping.getAllAliases());
    }

    @Override
    public TypeToken<Command.Parameterized> handledType() {
        return SpongeParameterizedCommandRegistrar.COMMAND_TYPE;
    }

}
