package de.plixo.atic.compiler.semantics.n;

import de.plixo.atic.compiler.semantics.buckets.FunctionCompilationUnit;
import de.plixo.atic.compiler.semantics.n.exceptions.RegionException;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;

public class SemanticExpressionValidator {

    public static SemanticStatement validate(SemanticStatement expression, FunctionCompilationUnit functionUnit,
                                             SemanticStatement described) throws RegionException {

        return described;
    }
}
