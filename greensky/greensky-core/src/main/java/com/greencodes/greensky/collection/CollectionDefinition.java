package com.greencodes.greensky.collection;

import java.util.List;
import java.util.Set;

/** Uma coleção (ex.: Pesca) e suas entradas, na ordem do arquivo. */
public record CollectionDefinition(String id, String name, List<CollectionEntry> entries) {

    public CollectionDefinition {
        entries = List.copyOf(entries);
    }

    /** Quantas entradas desta coleção estão entre as descobertas. */
    public int discoveredCount(Set<String> discoveredKeys) {
        int count = 0;
        for (CollectionEntry entry : entries) {
            if (discoveredKeys.contains(entry.key())) {
                count++;
            }
        }
        return count;
    }
}
