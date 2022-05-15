package de.plixo.atic.compiler.semantics.n;

import de.plixo.atic.DebugHelper;
import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.Primitives;
import de.plixo.atic.compiler.semantics.buckets.FunctionCompilationUnit;
import de.plixo.atic.compiler.semantics.n.exceptions.*;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.exceptions.NotImplementedException;
import de.plixo.atic.exceptions.UnknownTypeException;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static de.plixo.atic.compiler.semantics.n.SemanticAnalysisHelper.*;
import static de.plixo.atic.compiler.semantics.type.Expression.*;

public class SemanticExpressionValidator {

    private static SemanticType described;
    private static FunctionCompilationUnit functionUnit;

    public static SemanticType validate(AutoLexer.SyntaxNode<TokenRecord<Token>> expression,
                                        FunctionCompilationUnit functionUnit,
                                        SemanticType described) throws RegionException {

        return byExpr(expression);
    }

    private static SemanticType byFunction(AutoLexer.SyntaxNode<TokenRecord<Token>> function,
                                           SemanticType preferred) throws RegionException {
        val functionNode = getNode(function, "function");

        if (preferred instanceof SemanticType.ArrayType) {
            throw new UnexpectedTypeException("cant set a function in an array", function.data.from);
        } else if (!isAutoShallow(preferred) && preferred instanceof SemanticType.StructType) {
            throw new UnexpectedTypeException("cant set a function in an object", function.data.from);
        }

        if (testNode(functionNode, "anonymousFunction")) {
            if (isAutoShallow(preferred)) {
                throw new UnknownTypeException("auto cant be resolved for anonymous functions");
            } else {
                SemanticType.FunctionType functionType = (SemanticType.FunctionType) preferred;
                val anonymousFunction = foundNode;
                final AtomicInteger count = new AtomicInteger();
                final Map<String, SemanticType> types = new TreeMap<>();
                walk("ID", "IdList", anonymousFunction, node -> {
                    final String name = getLeafData(node);
                    int counter = count.getAndIncrement();
                    if (functionUnit.declaredVariables.containsKey(name) || functionUnit.function.input
                            .containsKey(name)) {
                        throw new NameCollisionException("identifier \"" + name + "\" has multiple entries",
                                node.data.from);
                    }
                    if (counter > functionType.input.size()) {
                        throw new TooManyArgumentsException(node.data.from);
                    }
                    types.put(name, functionType.input.get(counter));
                    //TODO evaluate function
                });
                if (count.get() != functionType.input.size()) {
                    throw new MissingArgumentsException(anonymousFunction.data.from);
                }
                final ArrayList<SemanticType> typeList = new ArrayList<>();
                types.forEach((k, v) -> typeList.add(v));
                //assertType(functionType.output, functionType.output);

                for (int i = 0; i < functionType.input.size(); i++) {
                    assertType(functionType.input.get(i), typeList.get(0), anonymousFunction.data.from);
                }
                return functionType;
            }
        } else {
            val richFunction = getNode(functionNode, "richFunction");
            final SemanticType returnType = genSemanticType(getNode(richFunction, "Type"), functionUnit.mainUnit);
            final AtomicInteger count = new AtomicInteger();
            final Map<String, SemanticType> types = new TreeMap<>();
            walk("inputTerm", "inputList", richFunction, node -> {
                val type = getNode(node, "Type");
                final SemanticType semanticType = genSemanticType(type, functionUnit.mainUnit);
                final String name = getId(node);
                count.getAndIncrement();
                if (functionUnit.declaredVariables.containsKey(name) || functionUnit.function.input
                        .containsKey(name)) {
                    throw new NameCollisionException("identifier \"" + name + "\" has multiple entries",
                            node.data.from);
                }
                types.put(name, semanticType);
                //TODO evaluate function

            });
            final ArrayList<SemanticType> typeList = new ArrayList<>();
            types.forEach((k, v) -> typeList.add(v));
            if (!isAutoShallow(preferred)) {
                SemanticType.FunctionType functionType = (SemanticType.FunctionType) preferred;
                if (count.get() > functionType.input.size()) {
                    throw new TooManyArgumentsException(richFunction.data.from);
                } else if (count.get() < functionType.input.size()) {
                    throw new MissingArgumentsException(richFunction.data.from);
                } else {
                    assertType(functionType.output, returnType, richFunction.data.from);
                    for (int i = 0; i < functionType.input.size(); i++) {
                        assertType(functionType.input.get(i), typeList.get(0), richFunction.data.from);
                    }
                }
                return functionType;
            } else {
                return new SemanticType.FunctionType(returnType, typeList);
            }
        }
    }

    private static SemanticType byExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (testNode(expr, "boolArithmetic")) {
            return byBoolExpr(expr);
        } else {
            return byFunction(expr, described);
        }
    }

    private static SemanticType byBoolExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (BOOL_EXPR.isImplemented(expr)) {
            final SemanticType left = byCompExpr(BOOL_EXPR.next(expr));
            final SemanticType right = byBoolExpr(BOOL_EXPR.same(expr));
            assertType(right, Primitives.integer_type, expr.data.from);
            assertType(left, Primitives.integer_type, expr.data.from);
            return Primitives.integer_type;
        } else
            return byCompExpr(BOOL_EXPR.next(expr));
    }

    private static SemanticType byCompExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (COMP_EXPR.isImplemented(expr)) {
            final SemanticType left = byArithmetic(COMP_EXPR.next(expr));
            final SemanticType right = byCompExpr(COMP_EXPR.same(expr));
            assertType(right, Primitives.integer_type, expr.data.from);
            assertType(left, Primitives.integer_type, expr.data.from);
            return Primitives.integer_type;
        } else
            return byArithmetic(COMP_EXPR.next(expr));
    }


    private static SemanticType byArithmetic(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (ARITHMETIC.isImplemented(expr)) {
            final SemanticType left = byTerm(ARITHMETIC.next(expr));
            final SemanticType right = byArithmetic(ARITHMETIC.same(expr));
            assertTypeNumber(left, expr.data.from);
            assertTypeNumber(right, expr.data.from);
            return left;
        } else
            return byTerm(ARITHMETIC.next(expr));
    }

    private static SemanticType byTerm(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (TERM.isImplemented(expr)) {
            final SemanticType left = byFactor(TERM.next(expr));
            final SemanticType right = byTerm(TERM.same(expr));
            assertTypeNumber(left, expr.data.from);
            assertTypeNumber(right, expr.data.from);
            return left;
        } else
            return byFactor(TERM.next(expr));
    }


    private static SemanticType byFactor(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (testNode(expr, "expression")) {
            return byExpr(foundNode);
        } else if (testNode(expr, "unary")) {
            val unary = foundNode;
            AutoLexer.SyntaxNode<TokenRecord<Token>> neg;
            if (testNode(expr, "neg_unary")) {
                neg = foundNode;
            } else {
                neg = getNode(unary, "pos_unary");
            }
            return byFactor(getNode(neg, "factor"));
        } else if (testNode(expr, "not")) {
            val not = foundNode;
            final SemanticType factor = byFactor(getNode(not, "factor"));
            assertType(factor, Primitives.integer_type, not.data.from);
            return factor;
        } else if (testNode(expr, "number")) {
            return Primitives.integer_type;
        } else if (testNode(expr, "boolLiteral")) {
            return Primitives.integer_type;
        } else if (testNode(expr, "member")) {
            val member = foundNode;
            val memberCompound = testNode(member, "memberCompound") ? foundNode : null;
            final String start = getId(member);

            if (functionUnit.declaredVariables.containsKey(start)) {
                final SemanticType semanticType = functionUnit.declaredVariables.get(start);
                if (memberCompound == null) {
                    return semanticType;
                }
                return getMember(semanticType, memberCompound);
            }

            if (functionUnit.function.input.containsKey(start)) {
                final SemanticType semanticType = functionUnit.function.input.get(start);
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

    private static SemanticType getMember(SemanticType type, AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        final SemanticType[] prevType = {type};
        walk("varTerminal", "memberCompound", expr, node -> {
            if (testNode(node, "memberAccess")) {
                val memberNode = SemanticAnalysisHelper.foundNode;
                final String name = getId(memberNode);
                if (prevType[0] instanceof SemanticType.ArrayType) {
                    throw new UnexpectedTypeException("Cant access members on arrays", memberNode.data.from);
                } else if (prevType[0] instanceof SemanticType.FunctionType) {
                    throw new UnexpectedTypeException("Cant access members on functions", memberNode.data.from);
                } else if (prevType[0] instanceof SemanticType.StructType) {
                    final SemanticType.StructType structType = (SemanticType.StructType) prevType[0];
                    if (!structType.structure.members.containsKey(name)) {
                        throw new UnexpectedTypeException("Unknown Member \"" + name + "\" for type \"" + prevType[0]
                                .toString() + "\"", memberNode.data.from);
                    }
                    prevType[0] = structType.structure.members.get(name);
                } else {
                    throw new UnexpectedTypeException("Unknown Semantic type", expr.data.from);
                }
            } else if (testNode(node, "callAccess")) {
                val callAccess = foundNode;
                if (prevType[0] instanceof SemanticType.ArrayType) {
                    throw new UnexpectedTypeException("Cant call an array", callAccess.data.from);
                } else if (prevType[0] instanceof SemanticType.StructType) {
                    throw new UnexpectedTypeException("Cant call an object", callAccess.data.from);
                } else if (prevType[0] instanceof SemanticType.FunctionType) {
                    final SemanticType.FunctionType structType = (SemanticType.FunctionType) prevType[0];
                    final List<SemanticType> input = structType.input;
                    final AtomicInteger argCount = new AtomicInteger();
                    final int size = input.size();
                    walk("expression", "argList", callAccess, (arg) -> {
                        final int count = argCount.getAndIncrement();
                        if (count >= size) {
                            throw new TooManyArgumentsException(arg.data.from);
                        }
                        final SemanticType semanticType = byExpr(arg);
                        assertType(semanticType, input.get(count), arg.data.from);
                    });
                    if (argCount.get() != size) {
                        throw new MissingArgumentsException(expr.data.from);
                    }

                    prevType[0] = structType.output;
                } else {
                    throw new UnexpectedTypeException("Unknown Semantic type", expr.data.from);
                }
            } else if (testNode(node, "arrayAccess")) {
                val arrayAccess = foundNode;
                final SemanticType semanticType = byExpr(arrayAccess);
                assertType(semanticType, Primitives.integer_type, arrayAccess.data.from);
                if (prevType[0] instanceof SemanticType.StructType) {
                    throw new UnexpectedTypeException("Cant index an object", arrayAccess.data.from);
                } else if (prevType[0] instanceof SemanticType.FunctionType) {
                    throw new UnexpectedTypeException("Cant index an functions", arrayAccess.data.from);
                } else if (prevType[0] instanceof SemanticType.ArrayType) {
                    final SemanticType.ArrayType arrayType = (SemanticType.ArrayType) prevType[0];
                    prevType[0] = arrayType.arrayObject;
                } else {
                    throw new UnexpectedTypeException("Unknown Semantic type", arrayAccess.data.from);
                }
            }
        });
        return prevType[0];
    }

}
