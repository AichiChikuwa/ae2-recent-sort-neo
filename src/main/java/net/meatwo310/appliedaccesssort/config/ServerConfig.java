package net.meatwo310.appliedaccesssort.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    private ServerConfig() {
    }

    private static final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue historyRows = builder
            .comment("number of rows used for last modified history pins")
            .defineInRange("historyRows", 5, 1, 12);

    public static final ModConfigSpec.BooleanValue debugChat = builder
            .comment("show debug classification logs in minecraft chat")
            .define("debugChat", false);

    public static final ModConfigSpec spec = builder.build();
}
