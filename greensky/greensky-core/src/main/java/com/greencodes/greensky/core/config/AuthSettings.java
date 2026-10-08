package com.greencodes.greensky.core.config;

import com.greencodes.greensky.integration.auth.AuthProvider;

/**
 * Autenticação dos jogadores.
 *
 * @param devMode libera {@code provider: none} com online-mode=false (qualquer um entra com
 *     qualquer nick). Só para desenvolvimento/testes com bots.
 */
public record AuthSettings(AuthProvider provider, boolean devMode) {

    /**
     * Decide se o plugin pode subir com esta configuração.
     *
     * @param serverOnlineMode o {@code online-mode} do server.properties
     * @throws IllegalStateException se a combinação for insegura
     */
    public void validateFor(boolean serverOnlineMode) {
        if (provider == AuthProvider.NONE && !serverOnlineMode && !devMode) {
            throw new IllegalStateException("auth.provider=none com online-mode=false deixa qualquer um entrar com"
                    + " qualquer nick. Use auth.provider=nlogin, ou auth.dev-mode=true SÓ em desenvolvimento.");
        }
    }

    /** O modo inseguro de desenvolvimento está efetivamente em uso? */
    public boolean insecureDevMode(boolean serverOnlineMode) {
        return provider == AuthProvider.NONE && !serverOnlineMode && devMode;
    }
}
