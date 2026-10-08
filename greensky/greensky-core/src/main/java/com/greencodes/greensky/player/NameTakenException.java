package com.greencodes.greensky.player;

/** O nick (sem diferenciar maiúsculas) já pertence a outra conta. */
public final class NameTakenException extends RuntimeException {

    public NameTakenException(String attemptedName) {
        super("O nick '" + attemptedName + "' já pertence a outra conta");
    }
}
