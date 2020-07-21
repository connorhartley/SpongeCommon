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
package org.spongepowered.common.network.channel;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.netty.buffer.Unpooled;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.network.channel.ChannelBuf;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public final class RegisterChannelHelper {

    private static final char SEPARATOR = '\0';
    private static final Splitter SPLITTER = Splitter.on(RegisterChannelHelper.SEPARATOR);
    private static final Joiner JOINER = Joiner.on(RegisterChannelHelper.SEPARATOR);

    public static List<ResourceKey> decodePayload(final ChannelBuf payload) {
        final byte[] content = payload.readBytes(payload.available());
        return RegisterChannelHelper.SPLITTER.splitToList(new String(content, StandardCharsets.UTF_8))
                .stream().map(ResourceKey::resolve).collect(Collectors.toList());
    }

    public static ChannelBuf encodePayload(final Iterable<ResourceKey> keys) {
        final String content = RegisterChannelHelper.JOINER.join(keys);
        return ChannelBuffers.wrap(Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
    }

    private RegisterChannelHelper() {
    }
}
