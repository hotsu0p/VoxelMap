package com.mamiyaotaru.voxelmap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class VoxelConstants {
    private static final Logger LOGGER = LogManager.getLogger("VoxelMap");

    private VoxelConstants() {}

    @NotNull
    public static MinecraftClient getMinecraft() { return MinecraftClient.getInstance(); }

    public static boolean isSystemMacOS() { return MinecraftClient.IS_SYSTEM_MAC; }

    public static boolean isFabulousGraphicsOrBetter() { return MinecraftClient.isFabulousGraphicsOrBetter(); }

    @NotNull
    public static Logger getLogger() { return LOGGER; }

    @NotNull
    public static Optional<IntegratedServer> getIntegratedServer() { return Optional.ofNullable(getMinecraft().getServer()); }

    @NotNull
    public static Optional<World> getWorldByKey(RegistryKey<World> key) { return getIntegratedServer().map(integratedServer -> integratedServer.getWorld(key)); }

    @NotNull
    public static ClientPlayerEntity getPlayer() {
        ClientPlayerEntity player = getMinecraft().player;

        if (player == null) {
            String error = "Attempted to fetch player entity while not in-game!";

            getLogger().fatal(error);
            throw new IllegalStateException(error);
        }

        return player;
    }
}