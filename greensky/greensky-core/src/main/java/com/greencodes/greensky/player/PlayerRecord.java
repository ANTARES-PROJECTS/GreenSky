package com.greencodes.greensky.player;

import java.util.UUID;

/** Jogador conhecido pelo GreenSky: UUID (identidade) e o nick com a grafia registrada. */
public record PlayerRecord(UUID uuid, String name) {}
