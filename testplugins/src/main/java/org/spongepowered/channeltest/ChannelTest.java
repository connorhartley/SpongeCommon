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
package org.spongepowered.channeltest;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.EngineConnectionSide;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.network.channel.Channel;
import org.spongepowered.api.network.channel.NoResponseException;
import org.spongepowered.api.network.channel.packet.PacketChannel;
import org.spongepowered.api.network.channel.packet.basic.BasicPacketChannel;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

import java.util.concurrent.CompletionException;

@Plugin("channeltest")
public final class ChannelTest {

    private final Logger logger;
    private final PluginContainer plugin;

    private PacketChannel channel;
    private BasicPacketChannel basicChannel;
    private RawDataChannel rawChannel;

    @Inject
    public ChannelTest(final Logger logger, final PluginContainer plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    private static String getName(final EngineConnectionSide<?> side) {
        return side == EngineConnectionSide.CLIENT ? "client" : "server";
    }

    private void logReceived(final Channel channel, final Object packet, final EngineConnection connection) {
        this.logger.info("Received {} through {} on the {} side.", packet, channel.getKey(), getName(connection.getSide()));
    }

    @Listener
    public void onRegisterChannel(final RegisterChannelEvent event) {
        this.channel = event.register(ResourceKey.of("channeltest", "default"), PacketChannel.class);
        this.channel.register(PrintTextPacket.class, 0)
                .addHandler((packet, connection) -> {
                    this.logReceived(this.channel, packet, connection);
                    this.logger.info(packet.getText());
                });
        this.channel.registerTransactional(PingPacket.class, PongPacket.class, 1)
                .setRequestHandler((requestPacket, connection, response) -> {
                    this.logReceived(this.channel, requestPacket, connection);
                    response.success(new PongPacket(requestPacket.getId()));
                });

        this.basicChannel = event.register(ResourceKey.of("channeltest", "basic"), BasicPacketChannel.class);
        this.basicChannel.register(PrintTextPacket.class, 0)
                .addHandler((packet, connection) -> {
                    this.logReceived(this.basicChannel, packet, connection);
                    this.logger.info(packet.getText());
                });
        this.basicChannel.registerTransactional(PingPacket.class, PongPacket.class, 1)
                .setRequestHandler((requestPacket, connection, response) -> {
                    this.logReceived(this.basicChannel, requestPacket, connection);
                    response.success(new PongPacket(requestPacket.getId()));
                });

        this.rawChannel = event.register(ResourceKey.of("channeltest", "raw"), RawDataChannel.class);
        this.rawChannel.handshake()
                .setRequestHandler((request, connection, response) -> {
                    final int value = request.readVarInt();
                    this.logReceived(this.rawChannel, value, connection);
                    if (value == 0) {
                        response.fail(new NoResponseException());
                    } else {
                        response.success(buf -> buf.writeVarInt(value * 2));
                    }
                });
    }

    @Listener
    public void onConnectionHandshake(final ServerSideConnectionEvent.Handshake event) {
        this.logger.info("Starting handshake phase.");
        final PingPacket pingPacket1 = new PingPacket(123);
        final ServerSideConnection connection = event.getConnection();
        this.channel.sendTo(connection, pingPacket1)
                .thenAccept(response1 -> {
                    this.logReceived(this.channel, response1, connection);
                    final PingPacket pingPacket2 = new PingPacket(456);
                    this.channel.sendTo(connection, pingPacket2)
                            .thenAccept(response2 -> {
                                this.logReceived(this.channel, response2, connection);
                                this.channel.sendTo(connection, new PrintTextPacket("Finished handshake phase."));
                                this.logger.info("Finished handshake phase.");
                            })
                            .exceptionally(cause -> {
                                this.logger.error("Failed to get a response to {}", pingPacket2, cause);
                                return null;
                            });
                })
                .exceptionally(cause -> {
                    this.logger.error("Failed to get a response to {}", pingPacket1, cause);
                    return null;
                });

        final PingPacket basicPingPacket1 = new PingPacket(1123);
        this.basicChannel.handshake().sendTo(connection, basicPingPacket1)
                .thenAccept(response1 -> {
                    this.logReceived(this.basicChannel, response1, connection);
                    final PingPacket basicPingPacket2 = new PingPacket(1456);
                    this.basicChannel.handshake().sendTo(connection, basicPingPacket2)
                            .thenAccept(response2 -> {
                                this.logReceived(this.channel, response2, connection);
                                this.basicChannel.handshake().sendTo(connection, new PrintTextPacket("Finished handshake phase for basic channel."));
                                this.logger.info("Finished handshake phase for basic channel.");
                            })
                            .exceptionally(cause -> {
                                this.logger.error("Failed to get a response to {}", basicPingPacket2, cause);
                                return null;
                            });
                })
                .exceptionally(cause -> {
                    this.logger.error("Failed to get a response to {}", pingPacket1, cause);
                    return null;
                });

        this.rawChannel.handshake().sendTo(connection, buf -> buf.writeVarInt(200))
                .thenAccept(response -> this.logReceived(this.rawChannel, response.readVarInt(), connection))
                .exceptionally(cause -> {
                    this.logger.error("Failed to get a response to raw 200 value", cause);
                    return null;
                });

        this.rawChannel.handshake().sendTo(connection, buf -> buf.writeVarInt(0))
                .thenAccept(response -> this.logReceived(this.rawChannel, response.readVarInt(), connection))
                .exceptionally(cause -> {
                    if (cause instanceof CompletionException) {
                        cause = cause.getCause();
                    }
                    if (cause instanceof NoResponseException) {
                        this.logger.error("Successfully received no response exception");
                    } else {
                        this.logger.error("Failed to get a response to raw 0 value", cause);
                    }
                    return null;
                });
    }

    @Listener
    public void onConnectionLogin(final ServerSideConnectionEvent.Login event) {
        this.logger.info("Player \"" + event.getProfile().getName().orElse("unknown") + "\" is logging in.");
    }

    @Listener
    public void onConnectionJoin(final ServerSideConnectionEvent.Join event) {
        this.logger.info("Player \"" + event.getPlayer().getName() + "\" joined.");

        final ServerSideConnection connection = event.getConnection();
        final PingPacket pingPacket1 = new PingPacket(789);
        this.channel.sendTo(connection, pingPacket1)
                .thenAccept(response1 -> this.logReceived(this.channel, response1, connection))
                .exceptionally(cause -> {
                    this.logger.error("Failed to get a response to {}", pingPacket1, cause);
                    return null;
                });

        this.basicChannel.play().sendTo(connection, new PrintTextPacket("You successfully joined the server."))
                .exceptionally(cause -> {
                    this.logger.error(cause);
                    return null;
                });
    }
}
