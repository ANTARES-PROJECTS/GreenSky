package com.greencodes.greensky.database;

/** Falha de conexão, migração ou execução SQL. */
public final class DatabaseException extends RuntimeException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
