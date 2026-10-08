-- Com online-mode=false o UUID vem do nick EXATO: "Gustavo" e "gustavo" seriam duas contas.
-- Um nick (sem diferenciar maiúsculas) pertence a uma única conta.
-- Em bancos com duplicatas esta migration falha de propósito: resolver as duplicatas antes.
DROP INDEX IF EXISTS players_name_idx;
CREATE UNIQUE INDEX players_name_lower_uidx ON players (lower(name));
