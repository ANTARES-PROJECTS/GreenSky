package com.greencodes.greensky.integration.auth;

/** Quem autentica os jogadores (config {@code auth.provider}). */
public enum AuthProvider {
    /** Plugin nLogin (necessário com online-mode=false em produção). */
    NLOGIN,
    /** Ninguém: o jogador é considerado autenticado ao entrar. Só com online-mode=true ou em desenvolvimento. */
    NONE
}
