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
package org.spongepowered.common.mixin.api.mcp.network.login;

import static java.util.Objects.requireNonNull;

import net.kyori.adventure.text.Component;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.network.NetworkManagerBridge;

import java.net.InetSocketAddress;

@Mixin(ServerLoginNetHandler.class)
public abstract class ServerLoginNetHandlerMixin_API implements ServerSideConnection {

    @Shadow @Final public NetworkManager networkManager;
    @Shadow private com.mojang.authlib.GameProfile loginGameProfile;
    @Shadow public abstract void shadow$disconnect(ITextComponent reason);

    @Override
    public GameProfile getProfile() {
        return (GameProfile) this.loginGameProfile;
    }

    @Override
    public void close() {
        this.shadow$disconnect(new TranslationTextComponent("disconnect.disconnected"));
    }

    @Override
    public void close(final Component reason) {
        requireNonNull(reason, "reason");
        this.shadow$disconnect(SpongeAdventure.asVanilla(reason));
    }

    @Override
    public InetSocketAddress getAddress() {
        return ((NetworkManagerBridge) this.networkManager).bridge$getAddress();
    }

    @Override
    public InetSocketAddress getVirtualHost() {
        return ((NetworkManagerBridge) this.networkManager).bridge$getVirtualHost();
    }
}
