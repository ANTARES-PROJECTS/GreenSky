package com.greencodes.greensky.island;

/** Violação de uma regra de negócio das ilhas (não é falha técnica). */
public final class IslandException extends RuntimeException {

    public enum Reason {
        ALREADY_HAS_ISLAND,
        NO_ISLAND,
        NOT_OWNER,
        ALREADY_MEMBER,
        NOT_A_MEMBER,
        CANNOT_REMOVE_OWNER
    }

    private final Reason reason;

    public IslandException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
