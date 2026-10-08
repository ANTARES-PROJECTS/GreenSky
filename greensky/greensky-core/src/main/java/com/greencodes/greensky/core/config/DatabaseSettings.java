package com.greencodes.greensky.core.config;

/**
 * Parâmetros de conexão com o PostgreSQL. A senha NÃO fica aqui nem no config.yml:
 * vem da variável de ambiente {@value #PASSWORD_ENV}.
 */
public record DatabaseSettings(String host, int port, String name, String user, int poolSize) {

    public static final String PASSWORD_ENV = "GREENSKY_DB_PASSWORD";

    public DatabaseSettings {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("database.host não pode ser vazio");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("database.port deve estar entre 1 e 65535 (atual: " + port + ")");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("database.name não pode ser vazio");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("database.user não pode ser vazio");
        }
        if (poolSize < 1 || poolSize > 50) {
            throw new IllegalArgumentException("database.pool-size deve estar entre 1 e 50 (atual: " + poolSize + ")");
        }
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + name;
    }
}
