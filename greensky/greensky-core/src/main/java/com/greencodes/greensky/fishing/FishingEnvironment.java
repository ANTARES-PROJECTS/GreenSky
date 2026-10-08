package com.greencodes.greensky.fishing;

import io.papermc.paper.world.MoonPhase;
import org.bukkit.World;

/** Condições do mundo no momento da fisgada (horário, clima, lua). Sem Bukkit nos testes: é só um valor. */
public record FishingEnvironment(boolean day, boolean raining, boolean thundering, MoonPhase moon) {

    private static final long TICKS_PER_DAY = 24000L;

    /** Lê o mundo (thread do servidor). A lua vem do número do dia: {@code MoonPhase.getPhase(dia)}. */
    public static FishingEnvironment of(World world) {
        return new FishingEnvironment(
                world.isDayTime(),
                world.hasStorm(),
                world.isThundering(),
                MoonPhase.getPhase(world.getFullTime() / TICKS_PER_DAY));
    }
}
