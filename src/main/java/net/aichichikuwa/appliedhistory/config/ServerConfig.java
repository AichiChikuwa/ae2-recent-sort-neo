package net.aichichikuwa.appliedhistory.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    private ServerConfig() {
    }

    private static final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue historyRows = builder
            .comment("number of rows used for last modified history pins")
            .defineInRange("historyRows", 5, 1, 12);

    public static final ModConfigSpec.DoubleValue meLoggerEnergyPerTick = builder
            .comment("ae consumed per tick by the me logger to function (1 ae = 2 fe)")
            .defineInRange("meLoggerEnergyPerTick", 10.0, 0.0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue debugChat = builder
            .comment("show debug classification logs in minecraft chat")
            .define("debugChat", false);

    public static final ModConfigSpec spec = builder.build();
}
