package com.greencodes.greensky.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Trabalho SQL executado numa conexão do pool. */
@FunctionalInterface
public interface SqlFunction<T> {

    T apply(Connection connection) throws SQLException;
}
