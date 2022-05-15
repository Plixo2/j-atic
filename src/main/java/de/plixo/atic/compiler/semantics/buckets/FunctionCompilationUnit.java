package de.plixo.atic.compiler.semantics.buckets;

import de.plixo.atic.compiler.semantics.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class FunctionCompilationUnit {
    public final CompilationUnit mainUnit;
    public final FunctionStruct function;
    public Map<String, SemanticType> declaredVariables = new HashMap<>();
    public int maxRegisters;
}
