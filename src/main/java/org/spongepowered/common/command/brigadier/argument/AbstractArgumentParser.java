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
package org.spongepowered.common.command.brigadier.argument;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.ValueParameter;
import org.spongepowered.common.command.brigadier.SpongeStringReader;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContext;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public abstract class AbstractArgumentParser<T> implements ArgumentParser<T>, SuggestionProvider<CommandSource>, ValueParameter<T> {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    @Override
    public T parse(final Parameter.Key<? super T> key, final SpongeCommandContextBuilder contextBuilder, final SpongeStringReader reader)
            throws CommandSyntaxException {
        final ArgumentReader.Immutable state = reader.getImmutable();
        final org.spongepowered.api.command.parameter.CommandContext.Builder.Transaction transaction = contextBuilder.startTransaction();
        try {
            final Optional<? extends T> value = this.getValue(key, reader, contextBuilder);
            contextBuilder.commit(transaction);
            if (value.isPresent()) {
                return value.get();
            }
        } catch (final ArgumentParseException e) {
            // reset the state as it did not go through.
            reader.setState(state);
            contextBuilder.rollback(transaction);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                    .createWithContext(reader, e.getSuperText());
        }
        return null;
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<?> context,
            final SuggestionsBuilder builder) {
        for (final String s : this.complete((SpongeCommandContext) context)) {
            if (INTEGER_PATTERN.matcher(s).matches()) {
                try {
                    builder.suggest(Integer.parseInt(s));
                } catch (final NumberFormatException ex) {
                    builder.suggest(s);
                }
            } else if (s.toLowerCase(Locale.ROOT).startsWith(builder.getRemaining())) {
                builder.suggest(s);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return ImmutableList.of();
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) {
        return this.listSuggestions(context, builder);
    }

    @Override
    public boolean doesNotRead() {
        return false;
    }

}
