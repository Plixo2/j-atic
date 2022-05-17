package de.plixo.atic.compiler.semantics.n;

import de.plio.nightlist.NightList;
import de.plixo.atic.DebugHelper;
import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.Primitives;
import de.plixo.atic.compiler.semantics.buckets.FunctionCompilationUnit;
import de.plixo.atic.compiler.semantics.buckets.FunctionStruct;
import de.plixo.atic.compiler.semantics.n.exceptions.*;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static de.plixo.atic.compiler.semantics.n.SemanticAnalysisHelper.*;
import static de.plixo.atic.compiler.semantics.type.Expression.*;

@RequiredArgsConstructor
public class SemanticExpressionValidator {


    final FunctionCompilationUnit functionUnit;

    public SemanticType resolveMember(AutoLexer.SyntaxNode<TokenRecord<Token>> expression) {
        return byMember(expression);
    }

    public SemanticType resolve(AutoLexer.SyntaxNode<TokenRecord<Token>> expression, SemanticType described) {
        final SemanticType semanticType = byExpr(expression, described);

        if (isAutoShallow(described)) {
            //System.out.println("resolved auto to " + semanticType);
            return semanticType;
        }
        assertType(described, semanticType, expression.data.from);
        return described;
    }

    private SemanticType byExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr, SemanticType described) throws RegionException {
        if (testNode(expr, "boolArithmetic")) {
            return byBoolExpr(getNode(expr, "boolArithmetic"));
        } else if (testNode(expr, "function")) {
            if(described == null) {
                throw new UnexpectedTypeException("cant resolve a nested function", expr.data.from);
            }
            return byFunction(expr, described);
        } else
            DebugHelper.printNode(expr);
        throw new NullPointerException("tried to resolve a wrong node");
    }

    private SemanticType byFunction(AutoLexer.SyntaxNode<TokenRecord<Token>> function,
                                    SemanticType preferred) throws RegionException {
        val functionNode = getNode(function, "function");

        if (preferred instanceof SemanticType.ArrayType) {
            throw new UnexpectedTypeException("cant set a function in an array", function.data.from);
        } else if (!isAutoShallow(preferred) && preferred instanceof SemanticType.StructType) {
            throw new UnexpectedTypeException("cant set a function in an object", function.data.from);
        }

        if (testNode(functionNode, "anonymousFunction")) {
            if (isAutoShallow(preferred)) {
                throw new UnknownTypeException("auto cant be resolved for anonymous functions",function.data.from);
            } else {
                SemanticType.FunctionType functionType = (SemanticType.FunctionType) preferred;
                val anonymousFunction = foundNode;
                final Map<String, SemanticType> types = new TreeMap<>();
                final ArrayList<SemanticType> linearList = new ArrayList<>();
                val list =
                        walk("ID", "IdList", anonymousFunction).split(SemanticAnalysisHelper::getLeafData)
                                .throwIf(node ->
                                        functionUnit.declaredVariables.containsKey(node.b) || functionUnit.function
                                                .input.containsKey(node.b), node -> {
                                    throw new NameCollisionException("identifier \"" + node.b + "\" has multiple " +
                                            "entries",
                                            node.a.data.from);
                                });
                if (list.size() < functionType.input.size()) {
                    throw new MissingArgumentsException(anonymousFunction.data.from);
                } else if (list.size() > functionType.input.size()) {
                    throw new TooManyArgumentsException(anonymousFunction.data.from);
                }
                list.applyIndex((node, count) -> {
                    final SemanticType type = functionType.input.get(count);
                    types.put(node.b, type);
                    linearList.add(type);
                });
                for (int i = 0; i < functionType.input.size(); i++) {
                    assertType(functionType.input.get(i), linearList.get(0), anonymousFunction.data.from);
                }

                val subStatement = getNode(anonymousFunction, "statement");
                val statement = genStatement(subStatement,
                        functionUnit.mainUnit);
                val functionStruct = new FunctionStruct("anonymousFunction", Primitives.auto_type, statement);
                functionStruct.input.putAll(types);
                final FunctionCompilationUnit compiledFunction = new FunctionCompilationUnit(functionUnit.mainUnit,
                        functionStruct);

                //TODO implement closure
                //compiledFunction.declaredVariables.putAll(functionUnit.declaredVariables);
                compiledFunction.declaredVariables.putAll(functionUnit.function.input);
                new SemanticStatementValidator(statement, compiledFunction).validate();
                //TODO return type detection for void
                if(!isAutoShallow(compiledFunction.function.output))
                    assertType(functionType.output, compiledFunction.function.output, subStatement.data.from);
                functionUnit.mainUnit.preEvaluatedFunction.put(anonymousFunction, functionStruct);
                return functionType;
            }
        } else {
            val richFunction = getNode(functionNode, "richFunction");
            final SemanticType returnType = genSemanticType(getNode(richFunction, "Type"), functionUnit.mainUnit);
            final AtomicInteger count = new AtomicInteger();
            final Map<String, SemanticType> types = new TreeMap<>();
            walk("inputTerm", "inputList", richFunction).apply(node -> {
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
            final NightList<SemanticType> typeList = NightList.create();
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


    private SemanticType byBoolExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (BOOL_EXPR.isImplemented(expr)) {
            final SemanticType left = byCompExpr(BOOL_EXPR.next(expr));
            final SemanticType right = byBoolExpr(BOOL_EXPR.same(expr));
            assertType(right, Primitives.integer_type, expr.data.from);
            assertType(left, Primitives.integer_type, expr.data.from);
            return Primitives.integer_type;
        } else
            return byCompExpr(BOOL_EXPR.next(expr));
    }

    private SemanticType byCompExpr(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (COMP_EXPR.isImplemented(expr)) {
            final SemanticType left = byArithmetic(COMP_EXPR.next(expr));
            final SemanticType right = byCompExpr(COMP_EXPR.same(expr));
            assertType(right, Primitives.integer_type, expr.data.from);
            assertType(left, Primitives.integer_type, expr.data.from);
            return Primitives.integer_type;
        } else
            return byArithmetic(COMP_EXPR.next(expr));
    }


    private SemanticType byArithmetic(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (ARITHMETIC.isImplemented(expr)) {
            final SemanticType left = byTerm(ARITHMETIC.next(expr));
            final SemanticType right = byArithmetic(ARITHMETIC.same(expr));
            assertTypeNumber(left, expr.data.from);
            assertTypeNumber(right, expr.data.from);
            return left;
        } else
            return byTerm(ARITHMETIC.next(expr));
    }

    private SemanticType byTerm(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (TERM.isImplemented(expr)) {
            final SemanticType left = byFactor(TERM.next(expr));
            final SemanticType right = byTerm(TERM.same(expr));
            assertTypeNumber(left, expr.data.from);
            assertTypeNumber(right, expr.data.from);
            return left;
        } else
            return byFactor(TERM.next(expr));
    }


    private SemanticType byFactor(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        if (testNode(expr, "expression")) {
            return byExpr(foundNode, null);
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
            return byMember(foundNode);
        }
        throw new UnknownTypeException("not yet implemented",expr.data.from);
    }

    private SemanticType byMember(AutoLexer.SyntaxNode<TokenRecord<Token>> expr) {
        val memberCompound = testNode(expr, "memberCompound") ? foundNode : null;
        final String start = getId(expr);

        if (functionUnit.declaredVariables.containsKey(start)) {
            final SemanticType semanticType = functionUnit.declaredVariables.get(start);
            if (memberCompound == null) {
                return semanticType;
            }
            return byMemberTerminal(semanticType, memberCompound);
        }

        if (functionUnit.function.input.containsKey(start)) {
            final SemanticType semanticType = functionUnit.function.input.get(start);
            if (memberCompound == null) {
                return semanticType;
            }
            return byMemberTerminal(semanticType, memberCompound);
        }
        throw new UnknownTypeException("Cant resolve name " + start,expr.data.from);
        //throw new NotImplementedException("Namespace validation");

        /*                final Optional<Namespace> any = namespaces.stream().filter(namespace -> namespace.name
        .equals(start))
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

    private SemanticType byMemberTerminal(SemanticType type, AutoLexer.SyntaxNode<TokenRecord<Token>> expr) throws RegionException {
        final SemanticType[] prevType = {type};
        walk("varTerminal", "memberCompound", expr).apply(node -> {
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
                    final NightList<SemanticType> input = structType.input;
                    final AtomicInteger argCount = new AtomicInteger();
                    final int size = input.size();
                    walk("expression", "argList", callAccess).apply(arg -> {
                        final int count = argCount.getAndIncrement();
                        if (count >= size) {
                            throw new TooManyArgumentsException(arg.data.from);
                        }
                        final SemanticType semanticType = byExpr(arg, input.get(count));
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
                final SemanticType semanticType = byExpr(getNode(arrayAccess,"expression"), Primitives.integer_type);
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
