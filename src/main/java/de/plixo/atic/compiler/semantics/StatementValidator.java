package de.plixo.atic.compiler.semantics;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.buckets.Namespace;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.exceptions.IncompatibleTypeException;
import de.plixo.atic.exceptions.NameCollisionException;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static de.plixo.atic.compiler.semantics.SemanticHelper.assertType;
import static de.plixo.atic.compiler.semantics.type.Expression.*;

public class StatementValidator {


    public static void validate(List<Namespace> namespaces) {
        namespaces.forEach(namespace -> namespace.functions.forEach(functionStruct -> {
            final Set<String> nameSet = new HashSet<>();
            functionStruct.input.forEach((k, v) -> nameSet.add(k));
            AtomicInteger registerCounter = new AtomicInteger(functionStruct.input.size());
            validateStatement(nameSet, functionStruct.statement, registerCounter);
        }));
        System.out.println("Validated all statement names");
    }

    private static void validateStatement(Set<String> names, SemanticStatement statement,
                                          AtomicInteger registerCounter) {
        if (statement instanceof SemanticStatement.Block) {
            SemanticStatement.Block block = (SemanticStatement.Block) statement;
            final Set<String> copy = new HashSet<>(names);
            AtomicInteger counterCopy = new AtomicInteger(registerCounter.intValue());
            block.statements.forEach(subStatement -> validateStatement(copy, subStatement, counterCopy));
        } else if (statement instanceof SemanticStatement.Declaration) {
            SemanticStatement.Declaration declaration = (SemanticStatement.Declaration) statement;
            declaration.register = registerCounter.getAndIncrement();
            if (names.contains(declaration.name)) {
                throw new NameCollisionException("\"" + declaration.name + "\" was declared twice");
            }
            names.add(declaration.name);
        }
    }


    public static class ExpressionValidator {
        private static SemanticType getType(SemanticType preferred, Namespace.FunctionStruct function,
                                            Map<String, Type> prevDeclaredVariables,
                                            AutoLexer.SyntaxNode<TokenRecord<Token>> expression) {


            return null;
        }


        private static SemanticType byBoolExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (BOOL_EXPR.isImplemented(expr)) {
                final SemanticType left = byCompExpr(BOOL_EXPR.next(expr));
                final SemanticType right = byBoolExpr(BOOL_EXPR.same(expr));
                assertType(right, Primitives.integer_type);
                assertType(left, Primitives.integer_type);
                return Primitives.integer_type;
            } else
                return byCompExpr(expr);
        }

        private static SemanticType byCompExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (COMP_EXPR.isImplemented(expr)) {
                final SemanticType left = byArithmetic(COMP_EXPR.next(expr));
                final SemanticType right = byCompExpr(COMP_EXPR.same(expr));
                assertType(right, Primitives.integer_type);
                assertType(left, Primitives.integer_type);
                return Primitives.integer_type;
            } else
                return byArithmetic(expr);
        }


        private static SemanticType byArithmetic(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (ARITHMETIC.isImplemented(expr)) {
                final SemanticType left = byTerm(ARITHMETIC.next(expr));
                final SemanticType right = byArithmetic(ARITHMETIC.same(expr));
                if (!Primitives.integer_type.equals(left) && !Primitives.decimal_type.equals(left)) {
                    throw new IncompatibleTypeException("left side of an arithmetic expression is neither an int, nor" +
                            " and decimal");
                }
                assertType(right, left);
                return left;
            } else
                return byTerm(expr);
        }

        private static SemanticType byTerm(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (TERM.isImplemented(expr)) {
                final SemanticType left = byFactor(TERM.next(expr));
                final SemanticType right = byTerm(TERM.same(expr));
                if (!Primitives.integer_type.equals(left) && !Primitives.decimal_type.equals(left)) {
                    throw new IncompatibleTypeException("left side of an arithmetic expression is neither an int, nor" +
                            " and decimal");
                }
                assertType(right, left);
                return left;
            } else
                return byFactor(expr);
        }


        private static SemanticType byFactor(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            return new SemanticType.StructType(Primitives.integer);
        }
    }
}
