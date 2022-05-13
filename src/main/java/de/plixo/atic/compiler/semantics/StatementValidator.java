package de.plixo.atic.compiler.semantics;

import de.plixo.atic.DebugHelper;
import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.buckets.Namespace;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.exceptions.NameCollisionException;
import de.plixo.atic.exceptions.NotImplementedException;
import de.plixo.atic.exceptions.UnknownTypeException;
import de.plixo.atic.exceptions.validation.IncompatibleTypeException;
import de.plixo.atic.exceptions.validation.MissingArgumentsException;
import de.plixo.atic.exceptions.validation.TooMuchArgumentsException;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static de.plixo.atic.compiler.semantics.SemanticHelper.*;
import static de.plixo.atic.compiler.semantics.type.Expression.*;

public class StatementValidator {

    static List<Namespace> namespaces;

    public static void validate(List<Namespace> namespaces) {
        StatementValidator.namespaces = namespaces;
        namespaces.forEach(namespace -> namespace.functions.forEach(functionStruct -> {
            final Map<String, SemanticType> nameSet = new HashMap<>();
            functionStruct.input.forEach(nameSet::put);
            AtomicInteger registerCounter = new AtomicInteger(functionStruct.input.size());
            validateStatement(functionStruct, nameSet, functionStruct.statement, registerCounter);
        }));
        System.out.println("Validated all statement names");
    }

    private static void validateStatement(Namespace.FunctionStruct function, Map<String, SemanticType> names,
                                          SemanticStatement statement,
                                          AtomicInteger registerCounter) {
        if (statement instanceof SemanticStatement.Block) {
            SemanticStatement.Block block = (SemanticStatement.Block) statement;
            final Map<String, SemanticType> copy = new HashMap<>(names);
            AtomicInteger counterCopy = new AtomicInteger(registerCounter.intValue());
            block.statements.forEach(subStatement -> validateStatement(function, copy, subStatement, counterCopy));
        } else if (statement instanceof SemanticStatement.Declaration) {
            SemanticStatement.Declaration declaration = (SemanticStatement.Declaration) statement;
            declaration.register = registerCounter.getAndIncrement();
            if (names.containsKey(declaration.name)) {
                throw new NameCollisionException("\"" + declaration.name + "\" was declared twice");
            }
            final SemanticType type = ExpressionValidator
                    .getType(declaration.type, function, names, namespaces, declaration.expression);
            if (declaration.type instanceof SemanticType.StructType) {
                SemanticType.StructType structType = (SemanticType.StructType) declaration.type;
                final boolean b = structType.structure == Primitives.auto;
                if (b) {
                    declaration.type = type;
                    names.put(declaration.name, declaration.type);
                    System.out.println("Auto resolved to " + type);
                    return;
                }
            }
            assertType(declaration.type, type);
            names.put(declaration.name, declaration.type);

        }
    }


    public static class ExpressionValidator {
        static List<Namespace> namespaces;
        static Namespace.FunctionStruct function;
        static Map<String, SemanticType> prevDeclaredVariables;

        public static SemanticType getType(SemanticType preferred, Namespace.FunctionStruct function,
                                           Map<String, SemanticType> prevDeclaredVariables,
                                           List<Namespace> namespaces,
                                           AutoLexer.SyntaxNode<TokenRecord<Token>> expression) {

            ExpressionValidator.namespaces = namespaces;
            ExpressionValidator.function = function;
            ExpressionValidator.prevDeclaredVariables = prevDeclaredVariables;

            if (testNode(expression, "boolArithmetic")) {
                return byExpr(expression);
            } else {
               return byFunction(expression,preferred);
            }
        }

        private static SemanticType byFunction(AutoLexer.SyntaxNode<TokenRecord<Token>> function ,
                                               SemanticType preferred) {
            val functionNode = yieldNode(function,"function");
            if(testNode(functionNode,"anonymousFunction")) {
                val anon = yieldNode(functionNode,"anonymousFunction");
                walk("ID","IdList",anon,node -> {
                   // final String name = new
                });
            } else {
                val anon = yieldNode(functionNode,"richFunction");
            }

        }

        private static SemanticType byExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            return byBoolExpr(yieldNode(expr, "boolArithmetic"));
        }

        private static SemanticType byBoolExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (BOOL_EXPR.isImplemented(expr)) {
                final SemanticType left = byCompExpr(BOOL_EXPR.next(expr));
                final SemanticType right = byBoolExpr(BOOL_EXPR.same(expr));
                assertType(right, Primitives.integer_type);
                assertType(left, Primitives.integer_type);
                return Primitives.integer_type;
            } else
                return byCompExpr(BOOL_EXPR.next(expr));
        }

        private static SemanticType byCompExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (COMP_EXPR.isImplemented(expr)) {
                final SemanticType left = byArithmetic(COMP_EXPR.next(expr));
                final SemanticType right = byCompExpr(COMP_EXPR.same(expr));
                assertType(right, Primitives.integer_type);
                assertType(left, Primitives.integer_type);
                return Primitives.integer_type;
            } else
                return byArithmetic(COMP_EXPR.next(expr));
        }


        private static SemanticType byArithmetic(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (ARITHMETIC.isImplemented(expr)) {
                final SemanticType left = byTerm(ARITHMETIC.next(expr));
                final SemanticType right = byArithmetic(ARITHMETIC.same(expr));
                assertTypeNumber(left);
                assertTypeNumber(right);
                return left;
            } else
                return byTerm(ARITHMETIC.next(expr));
        }

        private static SemanticType byTerm(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            if (TERM.isImplemented(expr)) {
                final SemanticType left = byFactor(TERM.next(expr));
                final SemanticType right = byTerm(TERM.same(expr));
                assertTypeNumber(left);
                assertTypeNumber(right);
                return left;
            } else
                return byFactor(TERM.next(expr));
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
                val memberCompound = testNode(member, "memberCompound") ? yieldNode(member, "memberCompound") : null;
                final String start = getLeafData(yieldNode(member, "ID"));

                if (prevDeclaredVariables.containsKey(start)) {
                    final SemanticType semanticType = prevDeclaredVariables.get(start);
                    if (memberCompound == null) {
                        return semanticType;
                    }
                    return getMember(semanticType, memberCompound);
                }

                if (function.input.containsKey(start)) {
                    final SemanticType semanticType = function.input.get(start);
                    if (memberCompound == null) {
                        return semanticType;
                    }
                    return getMember(semanticType, memberCompound);
                }
                throw new NotImplementedException("Namespace validation");

/*                final Optional<Namespace> any = namespaces.stream().filter(namespace -> namespace.name.equals(start))
                        .findAny();
                if (any.isPresent()) {
                    if (testNode(member, "memberCompound")) {
                        val memberCompound = yieldNode(member,
                                "memberCompound");
                        // memberCompound.

                    }
                    // any.get().functions.stream().filter(functionStruct -> functionStruct.name.equals())
                    return null;
                }*/
            }
            DebugHelper.printNode(expr);
            throw new UnknownTypeException("not yet implemented");
        }

        private static SemanticType getMember(SemanticType type, AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
            final SemanticType[] prevType = {type};
            walk("varTerminal", "memberCompound", expr, node -> {
                if (testNode(node, "memberAccess")) {
                    final String name = getLeafData(yieldNode(yieldNode(node, "memberAccess"), "id"));
                    if (prevType[0] instanceof SemanticType.ArrayType) {
                        throw new IncompatibleTypeException("Cant access members on arrays");
                    } else if (prevType[0] instanceof SemanticType.FunctionType) {
                        throw new IncompatibleTypeException("Cant access members on functions");
                    } else if (prevType[0] instanceof SemanticType.StructType) {
                        final SemanticType.StructType structType = (SemanticType.StructType) prevType[0];
                        if (!structType.structure.members.containsKey(name)) {
                            throw new IncompatibleTypeException("Unknown Member \"" + name + "\" for type \"" + prevType[0]
                                    .toString() +
                                    "\"");
                        }
                        prevType[0] = structType.structure.members.get(name);
                    } else {
                        throw new IncompatibleTypeException("Unknown Semantic type");
                    }
                } else if (testNode(node, "callAccess")) {
                    val callAccess = yieldNode(node, "callAccess");
                    if (prevType[0] instanceof SemanticType.ArrayType) {
                        throw new IncompatibleTypeException("Cant call an array");
                    } else if (prevType[0] instanceof SemanticType.StructType) {
                        throw new IncompatibleTypeException("Cant call an object");
                    } else if (prevType[0] instanceof SemanticType.FunctionType) {
                        final SemanticType.FunctionType structType = (SemanticType.FunctionType) prevType[0];
                        final List<SemanticType> input = structType.input;
                        final AtomicInteger argCount = new AtomicInteger();
                        final int size = input.size();
                        walk("expression", "argList", callAccess, (arg) -> {
                            final int count = argCount.getAndIncrement();
                            if (count >= size) {
                                throw new TooMuchArgumentsException(prevType[0].toString() + " got " + size +
                                        " arguments");
                            }
                            final SemanticType semanticType = byExpr(arg);
                            assertType(semanticType, input.get(count));
                        });
                        if (argCount.get() != size) {
                            throw new MissingArgumentsException(prevType[0].toString() + " got " + size +
                                    " arguments");
                        }

                        prevType[0] = structType.output;
                    } else {
                        throw new IncompatibleTypeException("Unknown Semantic type");
                    }
                } else if (testNode(node, "arrayAccess")) {
                    val arrayAccess = yieldNode(node, "arrayAccess");
                    final SemanticType semanticType = byExpr(arrayAccess);
                    assertType(semanticType, Primitives.integer_type);
                    if (prevType[0] instanceof SemanticType.StructType) {
                        throw new IncompatibleTypeException("Cant index an object");
                    } else if (prevType[0] instanceof SemanticType.FunctionType) {
                        throw new IncompatibleTypeException("Cant index an functions");
                    } else if (prevType[0] instanceof SemanticType.ArrayType) {
                        final SemanticType.ArrayType arrayType = (SemanticType.ArrayType) prevType[0];
                        prevType[0] = arrayType.arrayObject;
                    } else {
                        throw new IncompatibleTypeException("Unknown Semantic type");
                    }
                }
            });
            return prevType[0];
        }
    }
}
