package de.plixo.atic.compiler.semantic.statement.interfaces;

import de.plixo.atic.compiler.semantic.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class Namespace {
    public final String name;
    public final List<FunctionStruct> functions = new ArrayList<>();

    @RequiredArgsConstructor
    public static class FunctionStruct {
        public final String name;
        public final SemanticType output;
        public final Map<String,SemanticType> input = new HashMap<>();
    }
}
