package de.plixo.atic.compiler.semantics.buckets;

import de.plixo.atic.compiler.semantics.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@RequiredArgsConstructor
public class Structure {
    public final String name;
    public final Map<String, SemanticType> members = new TreeMap<>();

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        return "Structure{" +
                "name='" + name + '\'' +
                ", members=" + members +
                '}';
    }
}
