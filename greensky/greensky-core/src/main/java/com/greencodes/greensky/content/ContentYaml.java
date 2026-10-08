package com.greencodes.greensky.content;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Leitura dos arquivos de conteúdo. No YAML do Bukkit o ponto é separador de caminho, então
 * uma chave {@code fish.cod} viraria {@code fish -> cod}. Aqui o separador é {@code /}, e as
 * chaves com ponto ficam inteiras. Erro de sintaxe vira exceção (o Bukkit normalmente só loga
 * e devolve um arquivo vazio).
 */
public final class ContentYaml {

    public static final char SEPARATOR = '/';

    private ContentYaml() {}

    /**
     * @throws IllegalArgumentException se o arquivo não puder ser lido ou tiver erro de sintaxe
     */
    public static YamlConfiguration load(File file) {
        YamlConfiguration yaml = create();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalArgumentException(file.getName() + ": não foi possível ler (" + e.getMessage() + ")", e);
        }
        return yaml;
    }

    /** @throws IllegalArgumentException se tiver erro de sintaxe */
    public static YamlConfiguration load(Reader reader, String name) {
        YamlConfiguration yaml = create();
        try {
            yaml.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalArgumentException(name + ": não foi possível ler (" + e.getMessage() + ")", e);
        }
        return yaml;
    }

    /** @throws IllegalArgumentException se tiver erro de sintaxe */
    public static YamlConfiguration parse(String text) {
        YamlConfiguration yaml = create();
        try {
            yaml.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException("YAML inválido (" + e.getMessage() + ")", e);
        }
        return yaml;
    }

    private static YamlConfiguration create() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator(SEPARATOR);
        return yaml;
    }
}
