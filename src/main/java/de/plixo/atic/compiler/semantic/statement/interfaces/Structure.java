package de.plixo.atic.compiler.semantic.statement.interfaces;

import de.plixo.atic.compiler.semantic.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class Structure {
    public final String name;
    public final Map<String, SemanticType> members = new HashMap<>();

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
