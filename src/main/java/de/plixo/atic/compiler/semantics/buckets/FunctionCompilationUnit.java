package de.plixo.atic.compiler.semantics.buckets;

import de.plixo.atic.compiler.semantics.type.SemanticType;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class FunctionCompilationUnit {
    final CompilationUnit mainUnit;
    final FunctionStruct function;
    final Map<String, SemanticType> declaredVariables;
}
