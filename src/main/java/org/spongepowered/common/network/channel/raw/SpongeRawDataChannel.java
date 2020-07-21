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
package org.spongepowered.common.network.channel.raw;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.handshake.RawHandshakeDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import org.spongepowered.common.network.channel.SpongeChannel;
import org.spongepowered.common.network.channel.SpongeChannelRegistry;
import org.spongepowered.common.network.channel.TransactionResult;

import java.util.function.Consumer;

public class SpongeRawDataChannel extends SpongeChannel implements RawDataChannel {

    private final SpongeRawPlayDataChannel play = new SpongeRawPlayDataChannel(this);
    private final SpongeRawLoginDataChannel login = new SpongeRawLoginDataChannel(this);

    public SpongeRawDataChannel(final ResourceKey key, final SpongeChannelRegistry registry) {
        super(key, registry);
    }

    @Override
    public RawPlayDataChannel play() {
        return this.play;
    }

    @Override
    public RawHandshakeDataChannel handshake() {
        return this.login;
    }

    @Override
    protected void handlePlayPayload(final EngineConnection connection, final ChannelBuf payload) {
        this.play.handlePayload(connection, payload);
    }

    @Override
    protected void handleLoginRequestPayload(EngineConnection connection, int transactionId, ChannelBuf payload) {
        this.login.handleRequestPayload(connection, payload, transactionId);
    }

    @Override
    protected void handleTransactionResponse(EngineConnection connection, Object stored, TransactionResult result) {
        this.login.handleTransactionResponse(connection, stored, result);
    }

    ChannelBuf encodePayload(final Consumer<ChannelBuf> payload) {
        final ChannelBuf buf = this.getRegistry().getBufferAllocator().buffer();
        payload.accept(buf);
        return buf;
    }
}
