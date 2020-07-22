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
package org.spongepowered.common.command.parameter.flag;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class SpongeFlag implements Flag {

    private final String key;
    private final Set<String> aliases;
    private final Predicate<CommandCause> requirement;
    @Nullable private final Parameter associatedParameter;

    public SpongeFlag(
            final String key,
            final Set<String> aliases,
            final Predicate<CommandCause> requirement,
            @Nullable final Parameter associatedParameter) {
        this.key = key;
        this.aliases = aliases;
        this.requirement = requirement;
        this.associatedParameter = associatedParameter;
    }

    @Override
    @NonNull
    public String getKey() {
        return this.key;
    }

    @Override
    @NonNull
    public Collection<String> getAliases() {
        return ImmutableSet.copyOf(this.aliases);
    }

    @Override
    @NonNull
    public Predicate<CommandCause> getRequirement() {
        return this.requirement;
    }

    @Override
    @NonNull
    public Optional<Parameter> getAssociatedParameter() {
        return Optional.ofNullable(this.associatedParameter);
    }

}
