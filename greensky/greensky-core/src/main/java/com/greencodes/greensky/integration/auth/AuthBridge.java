package com.greencodes.greensky.integration.auth;

import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Ponte com o plugin de login. O GreenSky não conhece o plugin de login diretamente: só
 * recebe "este jogador autenticou" por aqui.
 */
public interface AuthBridge {

    String name();

    /**
     * Começa a ouvir autenticações. {@code onAuthenticated} é chamado na thread do servidor,
     * uma vez por login do jogador.
     */
    void start(Plugin plugin, Consumer<Player> onAuthenticated);

    /** O jogador já está autenticado agora? (usado para quem já estava online num /reload). */
    boolean isAuthenticated(Player player);
}
