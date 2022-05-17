package de.plixo.atic.compiler.semantics.n;

import de.plixo.atic.compiler.semantics.Primitives;
import de.plixo.atic.compiler.semantics.buckets.FunctionCompilationUnit;
import de.plixo.atic.compiler.semantics.n.exceptions.NameCollisionException;
import de.plixo.atic.compiler.semantics.n.exceptions.RegionException;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class SemanticStatementValidator {


    final SemanticStatement statement;
    final FunctionCompilationUnit functionUnit;
    int maxRegisters;

    public void validate() {
        maxRegisters = 0;
        validateStatement(statement, functionUnit.function.output, new AtomicInteger());
        functionUnit.maxRegisters = maxRegisters;
    }

    private void validateStatement(SemanticStatement statement, SemanticType returnType,
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
            declaration.type = new SemanticExpressionValidator(functionUnit).resolve(declaration.expression,
                    declaration.type);
            functionUnit.declaredVariables.put(declaration.name, declaration.type);
        } else if (statement instanceof SemanticStatement.Return) {
            SemanticStatement.Return aReturn = (SemanticStatement.Return) statement;
            functionUnit.function.output = new SemanticExpressionValidator(functionUnit).resolve(aReturn.expression,
                    functionUnit.function.output);
        } else if (statement instanceof SemanticStatement.Evaluation) {
            SemanticStatement.Evaluation evaluation = (SemanticStatement.Evaluation) statement;
            new SemanticExpressionValidator(functionUnit).resolve(evaluation.expression, Primitives.auto_type);
        } else if (statement instanceof SemanticStatement.Assignment) {
            SemanticStatement.Assignment assignment = (SemanticStatement.Assignment) statement;
            val semanticExpressionValidator =
                    new SemanticExpressionValidator(functionUnit);
            final SemanticType member = semanticExpressionValidator.resolveMember(assignment.member);
            final SemanticType validate = semanticExpressionValidator.resolve(assignment.expression, member);
        }
        //TODO validate assignments
    }


}
