package com.ticuliro.forgeit;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ForgeItConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue REFORGE_COST;
    public static final ModConfigSpec.DoubleValue DROP_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue REQUIRE_PLAYER_KILL;
    public static final ModConfigSpec.BooleanValue PASSIVE_DROPS;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        REFORGE_COST = b.comment("Wallet value per reforge in copper units: 50=silver, 500=gold, 5000=platinum. Creative is free.")
            .defineInRange("reforgeCostCopper", 500, 1, 1000000000);
        DROP_MULTIPLIER = b.comment("Multiplier for coin drops. Zero disables drops.")
            .defineInRange("dropMultiplier", 1.0, 0.0, 10.0);
        REQUIRE_PLAYER_KILL = b.comment("Require a player kill or recent player damage (includes tamed wolves).")
            .define("requirePlayerKill", true);
        PASSIVE_DROPS = b.comment("Allow common/passive mobs to use the copper loot table.").define("passiveDrops", true);
        SPEC = b.build();
    }
    private ForgeItConfig() {}
}
