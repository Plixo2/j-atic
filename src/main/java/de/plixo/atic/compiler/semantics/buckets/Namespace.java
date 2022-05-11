package de.plixo.atic.compiler.semantics.buckets;

import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class Namespace {
    public final String name;
    public final List<FunctionStruct> functions = new ArrayList<>();

    @RequiredArgsConstructor
    public static class FunctionStruct {
        public final String name;
        public final SemanticType output;
        public final SemanticStatement statement;
        public final Map<String,SemanticType> input = new TreeMap<>();
    }
}
