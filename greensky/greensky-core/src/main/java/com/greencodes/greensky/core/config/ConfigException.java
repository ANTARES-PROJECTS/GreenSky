package com.greencodes.greensky.core.config;

/** Configuração inválida; o plugin não deve subir com ela. */
public final class ConfigException extends Exception {

    public ConfigException(String message) {
        super(message);
    }
}
