package com.greencodes.greensky.fishing;

import io.papermc.paper.world.MoonPhase;
import java.util.Locale;

/**
 * Quando um peixe pode aparecer (README seções 32 e 146: horário, clima, lua).
 *
 * @param time ANY, DAY ou NIGHT
 * @param weather ANY; CLEAR (sem chuva); RAIN (chuva ou tempestade); THUNDER (só tempestade)
 * @param moon ANY, FULL (lua cheia) ou NEW (lua nova)
 */
public record FishConditions(Time time, Weather weather, Moon moon) {

    public static final FishConditions ANY = new FishConditions(Time.ANY, Weather.ANY, Moon.ANY);

    public enum Time { ANY, DAY, NIGHT }

    public enum Weather { ANY, CLEAR, RAIN, THUNDER }

    public enum Moon { ANY, FULL, NEW }

    public boolean matches(FishingEnvironment env) {
        boolean timeOk = switch (time) {
            case ANY -> true;
            case DAY -> env.day();
            case NIGHT -> !env.day();
        };
        boolean weatherOk = switch (weather) {
            case ANY -> true;
            case CLEAR -> !env.raining();
            case RAIN -> env.raining();
            case THUNDER -> env.thundering();
        };
        boolean moonOk = switch (moon) {
            case ANY -> true;
            case FULL -> env.moon() == MoonPhase.FULL_MOON;
            case NEW -> env.moon() == MoonPhase.NEW_MOON;
        };
        return timeOk && weatherOk && moonOk;
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value, String where) {
        if (value == null) {
            return Enum.valueOf(type, "ANY");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(where + ": valor inválido '" + value + "' (aceitos: "
                    + java.util.Arrays.toString(type.getEnumConstants()) + ")");
        }
    }
}
