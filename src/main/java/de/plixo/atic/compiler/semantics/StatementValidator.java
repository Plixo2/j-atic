package de.plixo.atic.compiler.semantics;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.buckets.Namespace;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.exceptions.IncompatibleTypeException;
import de.plixo.atic.exceptions.NameCollisionException;
import de.plixo.atic.exceptions.UnknownTypeException;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static de.plixo.atic.compiler.semantics.SemanticHelper.*;
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
        static List<Namespace> namespaces;
        static Namespace.FunctionStruct function;
        static Map<String, SemanticType> prevDeclaredVariables;

        private static SemanticType getType(SemanticType preferred, Namespace.FunctionStruct function,
                                            Map<String, SemanticType> prevDeclaredVariables,
                                            List<Namespace> namespaces,
                                            AutoLexer.SyntaxNode<TokenRecord<Token>> expression) {

            ExpressionValidator.namespaces = namespaces;
            ExpressionValidator.function = function;
            ExpressionValidator.prevDeclaredVariables = prevDeclaredVariables;

            return byExpr(expression);
        }

        private static SemanticType byExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            return byBoolExpr(expr);
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
                            " an decimal");
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
                            " an decimal");
                }
                assertType(right, left);
                return left;
            } else
                return byFactor(expr);
        }


        private static SemanticType byFactor(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (testNode(expr, "expression")) {
                return byExpr(yieldNode(expr, "expression"));
            } else if (testNode(expr, "unary")) {
                val unary = yieldNode(expr, "unary");
                AutoLexer.SyntaxNode<TokenRecord<Token>> neg;
                if (testNode(expr, "neg_unary")) {
                    neg = yieldNode(unary, "neg_unary");
                } else {
                    neg = yieldNode(unary, "pos_unary");
                }
                return byFactor(yieldNode(neg, "factor"));
            } else if (testNode(expr, "not")) {
                val not = yieldNode(expr, "not");
                final SemanticType factor = byFactor(yieldNode(not, "factor"));
                assertType(factor, Primitives.integer_type);
                return factor;
            } else if (testNode(expr, "number")) {
                return Primitives.integer_type;
            } else if (testNode(expr, "boolLiteral")) {
                return Primitives.integer_type;
            } else if (testNode(expr, "member")) {
                val member = yieldNode(expr, "member");
                final String start = getLeafData(yieldNode(member, "ID"));

                if (prevDeclaredVariables.containsKey(start)) {
                    final SemanticType semanticType = prevDeclaredVariables.get(start);
                }

                if (function.input.containsKey(start)) {
                    final SemanticType semanticType = function.input.get(start);

                    return null;
                }

                final Optional<Namespace> any = namespaces.stream().filter(namespace -> namespace.name.equals(start))
                        .findAny();
                if (any.isPresent()) {
                    if (testNode(member, "memberCompound")) {
                        val memberCompound = yieldNode(member,
                                "memberCompound");
                        // memberCompound.

                    }
                    // any.get().functions.stream().filter(functionStruct -> functionStruct.name.equals())
                    return null;
                }

                throw new UnknownTypeException("Unknown reference " + start);
            }
            throw new UnknownTypeException("not yet implemented");
        }

        private static SemanticType getCalled() {

        }
    }
}
