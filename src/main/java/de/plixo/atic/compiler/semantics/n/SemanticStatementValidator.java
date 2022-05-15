package de.plixo.atic.compiler.semantics.n;

import de.plixo.atic.compiler.semantics.buckets.FunctionCompilationUnit;
import de.plixo.atic.compiler.semantics.n.exceptions.NameCollisionException;
import de.plixo.atic.compiler.semantics.n.exceptions.RegionException;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static de.plixo.atic.compiler.semantics.n.SemanticAnalysisHelper.assertType;
import static de.plixo.atic.compiler.semantics.n.SemanticAnalysisHelper.isAutoShallow;

public class SemanticStatementValidator {


    private static FunctionCompilationUnit functionUnit;
    private static int maxRegisters = 0;

    public static void validate(SemanticStatement statement, FunctionCompilationUnit functionUnit) throws RegionException {
        SemanticStatementValidator.functionUnit = functionUnit;
        SemanticStatementValidator.maxRegisters = 0;
        validateStatement(statement, functionUnit.function.output, new AtomicInteger());
        functionUnit.maxRegisters = maxRegisters;
    }

    private static void validateStatement(SemanticStatement statement, SemanticType returnType,
                                          AtomicInteger registerCount) throws RegionException {
        maxRegisters = Math.max(maxRegisters, registerCount.get());
        if (statement instanceof SemanticStatement.Block) {
            SemanticStatement.Block block = (SemanticStatement.Block) statement;
            final Map<String, SemanticType> copy = new HashMap<>(functionUnit.declaredVariables);
            AtomicInteger counterCopy = new AtomicInteger(registerCount.intValue());
            for (SemanticStatement subStatement : block.statements) {
                validateStatement(subStatement, returnType, counterCopy);
            }
            functionUnit.declaredVariables = copy;
        } else if (statement instanceof SemanticStatement.Declaration) {
            SemanticStatement.Declaration declaration = (SemanticStatement.Declaration) statement;
            declaration.register = registerCount.getAndIncrement();
            if (functionUnit.declaredVariables.containsKey(declaration.name)) {
                throw new NameCollisionException(declaration.name, declaration.expression.data.from);
            }
            final SemanticType type = SemanticExpressionValidator
                    .validate(declaration.expression, functionUnit, declaration.type);

            if (isAutoShallow(declaration.type)) {
                declaration.type = type;

                functionUnit.declaredVariables.put(declaration.name, declaration.type);
                System.out.println("Auto resolved to " + type);
                return;
            }
            assertType(declaration.type, type, declaration.expression.data.from);
            functionUnit.declaredVariables.put(declaration.name, declaration.type);
        } else if (statement instanceof SemanticStatement.Return) {
            SemanticStatement.Return aReturn = (SemanticStatement.Return) statement;
            final SemanticType type = SemanticExpressionValidator
                    .validate(aReturn.expression, functionUnit, aReturn.type);
            //TODO make grammer and validate left and right
        }
    }


}
