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
package org.spongepowered.common.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.common.command.parameter.subcommand.SpongeSubcommandParameterBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SpongeParameterizedCommandBuilder implements Command.Parameterized.Builder {

    private final Set<String> claimedSubcommands = new HashSet<>();
    private final Map<Command.Parameterized, List<String>> subcommands = new HashMap<>();
    private final List<Parameter> parameters = new ArrayList<>();
    private final List<Flag> flags = new ArrayList<>();
    private final Set<String> flagAliases = new HashSet<>();
    @Nullable private CommandExecutor commandExecutor;
    @Nullable private Function<CommandCause, Optional<Component>> extendedDescription;
    @Nullable private Function<CommandCause, Optional<Component>> shortDescription;
    @Nullable private Predicate<CommandCause> executionRequirements;

    @Override
    public Command.@NonNull Builder child(final Command.@NonNull Parameterized child, @NonNull final Iterable<String> aliases) {
        for (final String alias : aliases) {
            if (this.claimedSubcommands.contains(alias.toLowerCase())) {
                throw new IllegalStateException("The alias " + alias + " already has an associated subcommand.");
            }
        }

        final List<String> s = new ArrayList<>();
        aliases.forEach(x -> s.add(x.toLowerCase()));
        this.claimedSubcommands.addAll(s);
        this.subcommands.put(child, s);
        return this;
    }

    @Override
    public Command.@NonNull Builder flag(@NonNull final Flag flag) {
        if (this.flags.stream().anyMatch(x -> x.getKey().equalsIgnoreCase(flag.getKey()))) {
            throw new IllegalArgumentException("The key " + flag.getKey() + " is already in use.");
        }
        for (final String alias : flag.getAliases()) {
            if (this.flagAliases.contains(alias)) {
                throw new IllegalArgumentException("The alias " + alias + " is already in use.");
            }
        }

        this.flags.add(flag);
        this.flagAliases.addAll(flag.getAliases());
        return this;
    }

    @Override
    public Command.@NonNull Builder parameter(@NonNull final Parameter parameter) {
        this.parameters.add(parameter);
        return this;
    }

    @Override
    public Command.@NonNull Builder setExecutor(@NonNull final CommandExecutor executor) {
        this.commandExecutor = executor;
        return this;
    }

    @Override
    public Command.@NonNull Builder setExtendedDescription(@NonNull final Function<CommandCause, Optional<Component>> extendedDescriptionFunction) {
        this.extendedDescription = extendedDescriptionFunction;
        return this;
    }

    @Override
    public Command.@NonNull Builder setShortDescription(@NonNull final Function<CommandCause, Optional<Component>> descriptionFunction) {
        this.shortDescription = descriptionFunction;
        return this;
    }

    @Override
    public Command.@NonNull Builder setPermission(@Nullable final String permission) {
        if (permission == null) {
            return this.setExecutionRequirements(null);
        }
        return this.setExecutionRequirements(commandCause -> commandCause.hasPermission(permission));
    }

    @Override
    public Command.@NonNull Builder setExecutionRequirements(@Nullable final Predicate<CommandCause> executionRequirements) {
        this.executionRequirements = executionRequirements;
        return this;
    }

    @Override
    public Command.@NonNull Parameterized build() {
        if (this.subcommands.isEmpty()) {
            Preconditions.checkState(this.commandExecutor != null, "Either a subcommand or an executor must exist!");
        } else {
            Preconditions.checkState(!(!this.parameters.isEmpty() && this.commandExecutor == null), "An executor must exist if you set parameters!");
        }

        final Predicate<CommandCause> requirements = this.executionRequirements == null ? cause -> true : this.executionRequirements;

        final List<Parameter.Subcommand> subcommands =
                this.subcommands.entrySet().stream()
                        .map(x -> new SpongeSubcommandParameterBuilder().aliases(x.getValue()).setSubcommand(x.getKey()).build())
                        .collect(Collectors.toList());

        // build the node.
        return new SpongeParameterizedCommand(
                subcommands,
                ImmutableList.copyOf(this.parameters),
                this.shortDescription,
                this.extendedDescription,
                requirements,
                this.commandExecutor,
                this.flags);
    }

    @Override
    public Command.@NonNull Builder reset() {
        this.subcommands.clear();
        this.claimedSubcommands.clear();
        this.commandExecutor = null;
        this.parameters.clear();
        this.flagAliases.clear();
        this.flags.clear();
        this.executionRequirements = null;
        this.extendedDescription = null;
        this.shortDescription = null;
        return this;
    }

}
