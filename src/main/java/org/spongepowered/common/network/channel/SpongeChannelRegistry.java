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

import com.google.common.collect.ImmutableList;
import net.minecraft.network.IPacket;
import net.minecraft.network.login.client.CCustomPayloadLoginPacket;
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.channel.Channel;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.ChannelRegistry;
import org.spongepowered.api.network.channel.NoResponseException;
import org.spongepowered.api.network.channel.packet.PacketChannel;
import org.spongepowered.api.network.channel.packet.basic.BasicPacketChannel;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.registry.DuplicateRegistrationException;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.login.client.CCustomPayloadLoginPacketAccessor;
import org.spongepowered.common.accessor.network.login.server.SCustomPayloadLoginPacketAccessor;
import org.spongepowered.common.accessor.network.play.client.CCustomPayloadPacketAccessor;
import org.spongepowered.common.accessor.network.play.server.SCustomPayloadPlayPacketAccessor;
import org.spongepowered.common.network.channel.packet.SpongeBasicPacketChannel;
import org.spongepowered.common.network.channel.packet.SpongePacketChannel;
import org.spongepowered.common.network.channel.raw.SpongeRawDataChannel;
import org.spongepowered.common.util.Constants;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@SuppressWarnings("unchecked")
public class SpongeChannelRegistry implements ChannelRegistry {

    private final Map<ResourceKey, SpongeChannel> channels = new HashMap<>();
    private final Map<Class<?>, Tuple<Integer, CreateFunction<SpongeChannel>>> channelBuilders = new HashMap<>();

    private final ChannelBufferAllocator bufferAllocator;

    public SpongeChannelRegistry(final ChannelBufferAllocator bufferAllocator) {
        this.bufferAllocator = bufferAllocator;

        this.registerChannelType(0, RawDataChannel.class, SpongeRawDataChannel::new);
        this.registerChannelType(1, PacketChannel.class, SpongePacketChannel::new);
        this.registerChannelType(2, BasicPacketChannel.class, SpongeBasicPacketChannel::new);
    }

    public ChannelBufferAllocator getBufferAllocator() {
        return this.bufferAllocator;
    }

    interface CreateFunction<C extends Channel> {

        C create(int type, ResourceKey key, SpongeChannelRegistry registry);
    }

    private <T extends Channel> void registerChannelType(final int id,
            final Class<T> channelType, final CreateFunction<? extends T> builder) {
        this.channelBuilders.put(channelType, Tuple.of(id, (CreateFunction<SpongeChannel>) builder));
    }

    public <C extends Channel> C createChannel(final ResourceKey channelKey, final Class<C> channelType) throws DuplicateRegistrationException {
        Objects.requireNonNull(channelKey, "channelKey");
        Objects.requireNonNull(channelType, "channelType");
        if (this.channels.containsKey(channelKey)) {
            throw new DuplicateRegistrationException("The channel key \"" + channelKey + "\" is already in use.");
        }
        final Tuple<Integer, CreateFunction<SpongeChannel>> tuple = this.channelBuilders.get(channelType);
        if (tuple == null) {
            throw new IllegalArgumentException("Unsupported channel type: " + channelType);
        }
        final SpongeChannel channel = tuple.getSecond().create(tuple.getFirst(), channelKey, this);
        this.channels.put(channelKey, channel);
        return (C) channel;
    }

    @Override
    public Optional<Channel> get(final ResourceKey channelKey) {
        Objects.requireNonNull(channelKey, "channelKey");
        return Optional.ofNullable(this.channels.get(channelKey));
    }

    @Override
    public <C extends Channel> C getOfType(final ResourceKey channelKey, final Class<C> channelType) {
        Objects.requireNonNull(channelKey, "channelKey");
        Objects.requireNonNull(channelType, "channelType");
        final Channel binding = this.channels.get(channelKey);
        if (binding != null) {
            if (!channelType.isInstance(binding)) {
                throw new IllegalStateException("There's already a channel registered for "
                        + channelKey + ", but it is not of the requested type " + channelType);
            }
            return (C) binding;
        }
        return this.createChannel(channelKey, channelType);
    }

    @Override
    public Collection<Channel> getChannels() {
        return ImmutableList.copyOf(this.channels.values());
    }

    private static final class ChannelRegistrySyncFuture {

        private final CompletableFuture<Void> future;

        private ChannelRegistrySyncFuture(final CompletableFuture<Void> future) {
            this.future = future;
        }
    }

    public void postRegistryEvent() {
        final Cause cause = Cause.of(EventContext.empty(), this);
        final RegisterChannelEvent event = new RegisterChannelEvent() {

            @Override
            public <C extends Channel> C register(ResourceKey channelKey, Class<C> channelType) throws DuplicateRegistrationException {
                return createChannel(channelKey, channelType);
            }

            @Override
            public Game getGame() {
                return SpongeCommon.getGame();
            }

            @Override
            public Cause getCause() {
                return cause;
            }
        };
        Sponge.getEventManager().post(event);
    }

    /**
     * Sends the login channel registrations. The client will respond back with
     * the registrations and the returned future will be completed.
     *
     * @param connection The connection to send the registrations to
     */
    public CompletableFuture<Void> sendLoginChannelRegistry(final EngineConnection connection) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        final TransactionStore store = ConnectionUtil.getTransactionStore(connection);
        final int transactionId = store.nextId();

        store.put(transactionId, null, new ChannelRegistrySyncFuture(future));

        final ChannelBuf payload = this.encodeChannelRegistry();
        final IPacket<?> mcPacket = PacketUtil.createLoginPayloadRequest(Constants.Channels.SPONGE_CHANNEL_REGISTRY, payload, transactionId);
        PacketSender.sendTo(connection, mcPacket, sendFuture -> {
            if (!sendFuture.isSuccess()) {
                future.completeExceptionally(sendFuture.cause());
            }
        });

        return future;
    }

    public void sendChannelRegistrations(final EngineConnection connection) {
        final ChannelBuf payload = RegisterChannelUtil.encodePayload(this.channels.keySet());
        final IPacket<?> mcPacket = PacketUtil.createPlayPayload(Constants.Channels.REGISTER_KEY, payload, connection.getSide());
        PacketSender.sendTo(connection, mcPacket);
    }

    /**
     * Encodes the sponge channel registry. Sending the channel registry will
     * override all known channel entries for the connection. Sending
     * "minecraft:register" packets afterwards is still possible to add channels.
     *
     * @return The encoded payload
     */
    private ChannelBuf encodeChannelRegistry() {
        final List<SpongeChannel> channels = ImmutableList.copyOf(this.channels.values());

        final ChannelBuf buf = this.bufferAllocator.buffer();
        buf.writeVarInt(channels.size());
        for (final SpongeChannel channel : channels) {
            buf.writeString(channel.getKey().getFormatted());
            // The type is included to provide extra information for e.g. proxies
            // who want to improve sponge support
            // Not used by sponge itself
            buf.writeByte((byte) channel.getType());
        }

        return buf;
    }

    private void handleChannelRegistry(final EngineConnection connection, final ChannelBuf payload) {
        final Set<ResourceKey> registered = ConnectionUtil.getRegisteredChannels(connection);
        registered.clear();

        final int count = payload.readVarInt();
        for (int i = 0; i < count; i++) {
            final ResourceKey key = ResourceKey.resolve(payload.readString());
            payload.readByte(); // type
            registered.add(key);
        }
    }

    public boolean handlePlayPayload(final EngineConnection connection, final CCustomPayloadPacket packet) {
        final CCustomPayloadPacketAccessor accessor = (CCustomPayloadPacketAccessor) packet;

        final ResourceKey channel = (ResourceKey) (Object) accessor.accessor$getChannel();
        final ChannelBuf payload = (ChannelBuf) accessor.accessor$getPayload();

        return this.handlePlayPayload(connection, channel, payload);
    }

    public boolean handlePlayPayload(final EngineConnection connection, final SCustomPayloadPlayPacket packet) {
        final SCustomPayloadPlayPacketAccessor accessor = (SCustomPayloadPlayPacketAccessor) packet;

        final ResourceKey channel = (ResourceKey) (Object) accessor.accessor$getChannel();
        final ChannelBuf payload = (ChannelBuf) accessor.accessor$getPayload();

        return this.handlePlayPayload(connection, channel, payload);
    }

    private void handleRegisterChannel(final EngineConnection connection, final ChannelBuf payload,
            final BiConsumer<Set<ResourceKey>, List<ResourceKey>> consumer) {
        final Set<ResourceKey> registered = ConnectionUtil.getRegisteredChannels(connection);
        final int readerIndex = payload.readerIndex();
        try {
            final List<ResourceKey> modified = RegisterChannelUtil.decodePayload(payload);
            consumer.accept(registered, modified);
        } finally {
            payload.readerIndex(readerIndex);
        }
    }

    private boolean handlePlayPayload(final EngineConnection connection, final ResourceKey channelKey, final ChannelBuf payload) {
        if (channelKey.equals(Constants.Channels.SPONGE_CHANNEL_REGISTRY)) {
            this.handleChannelRegistry(connection, payload);
            return true;
        } else if (channelKey.equals(Constants.Channels.REGISTER_KEY)) {
            this.handleRegisterChannel(connection, payload, Set::addAll);
            return true;
        } else if (channelKey.equals(Constants.Channels.UNREGISTER_KEY)) {
            this.handleRegisterChannel(connection, payload, Set::removeAll);
            return true;
        }
        final SpongeChannel channel = this.channels.get(channelKey);
        if (channel != null) {
            try {
                channel.handlePlayPayload(connection, payload);
            } finally {
                ChannelBuffers.release(payload);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean handleLoginRequestPayload(final EngineConnection connection, final SCustomPayloadLoginPacket packet) {
        // Server -> Client request

        final SCustomPayloadLoginPacketAccessor accessor = (SCustomPayloadLoginPacketAccessor) packet;
        final ResourceKey channel = (ResourceKey) (Object) accessor.accessor$getChannel();
        final int transactionId = accessor.accessor$getTransactionId();
        final ChannelBuf payload = (ChannelBuf) accessor.accessor$getPayload();

        try {
            return this.handleLoginRequestPayload(connection, channel, transactionId, payload);
        } finally {
            ChannelBuffers.release(payload);
        }
    }

    private boolean handleLoginRequestPayload(final EngineConnection connection, final ResourceKey channelKey,
            final int transactionId, final ChannelBuf payload) {
        if (channelKey.equals(Constants.Channels.SPONGE_CHANNEL_REGISTRY)) {
            this.handleChannelRegistry(connection, payload);
            // Respond with registered channels
            final ChannelBuf responsePayload = this.encodeChannelRegistry();
            final IPacket<?> mcPacket = PacketUtil.createLoginPayloadResponse(responsePayload, transactionId);
            PacketSender.sendTo(connection, mcPacket);
            return true;
        }
        ResourceKey actualChannelKey = channelKey;
        ChannelBuf actualPayload = payload;
        if (channelKey.equals(Constants.Channels.FML_LOGIN_WRAPPER_CHANNEL)) {
            actualChannelKey = ResourceKey.resolve(payload.readString());
            final int length = payload.readVarInt();
            actualPayload = payload.readSlice(length);
        }
        final SpongeChannel channel = this.channels.get(actualChannelKey);
        if (channel != null) {
            channel.handleLoginRequestPayload(connection, transactionId, actualPayload);
            return true;
        }
        return false;
    }

    public void handleLoginResponsePayload(final EngineConnection connection, final CCustomPayloadLoginPacket packet) {
        // Client -> Server response

        final CCustomPayloadLoginPacketAccessor accessor = (CCustomPayloadLoginPacketAccessor) packet;
        final int transactionId = accessor.accessor$getTransactionId();
        final ChannelBuf payload = (ChannelBuf) accessor.accessor$getPayload();

        try {
            this.handleLoginResponsePayload(connection, transactionId, payload);
        } finally {
            if (payload != null) {
                ChannelBuffers.release(payload);
            }
        }
    }

    private void handleLoginResponsePayload(final EngineConnection connection, final int transactionId, final @Nullable ChannelBuf payload) {
        // Sponge magic... Allows normal packets to be send during the login phase from the client to server
        if (transactionId == Constants.Channels.LOGIN_PAYLOAD_IGNORED_TRANSACTION_ID) {
            return;
        }
        if (transactionId == Constants.Channels.LOGIN_PAYLOAD_TRANSACTION_ID) {
            if (payload != null) {
                final ResourceKey channelKey = ResourceKey.resolve(payload.readString());
                this.handlePlayPayload(connection, channelKey, payload);
            }
            return;
        }
        // Normal handling
        final TransactionStore transactionStore = ConnectionUtil.getTransactionStore(connection);
        final TransactionStore.Entry entry = transactionStore.remove(transactionId);
        if (entry == null) {
            return;
        }
        if (entry.getData() instanceof ChannelRegistrySyncFuture) {
            if (payload != null) {
                this.handleChannelRegistry(connection, payload);
            }
            ((ChannelRegistrySyncFuture) entry.getData()).future.complete(null);
            return;
        }
        final TransactionResult result = payload == null ? TransactionResult.failure(new NoResponseException())
                : TransactionResult.success(payload);
        entry.getChannel().handleTransactionResponse(connection, entry.getData(), result);
    }
}
