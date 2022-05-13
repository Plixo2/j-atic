package de.plixo.atic.compiler.semantics.buckets;

import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.TreeMap;

@RequiredArgsConstructor
public class FunctionStruct {
    public final String name;
    public final SemanticType output;
    public final SemanticStatement statement;
    public final Map<String, SemanticType> input = new TreeMap<>();
}
