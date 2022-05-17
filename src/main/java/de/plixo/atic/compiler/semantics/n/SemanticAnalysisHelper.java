package de.plixo.atic.compiler.semantics.n;

import de.plio.nightlist.NightList;
import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.Primitives;
import de.plixo.atic.compiler.semantics.buckets.CompilationUnit;
import de.plixo.atic.compiler.semantics.n.exceptions.RegionException;
import de.plixo.atic.compiler.semantics.n.exceptions.UnexpectedTypeException;
import de.plixo.atic.compiler.semantics.n.exceptions.UnknownTypeException;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SemanticAnalysisHelper {

    public static NightList<AutoLexer.SyntaxNode<TokenRecord<Token>>> walk(String action, String list,
                                                                           AutoLexer.SyntaxNode<TokenRecord<Token>> node) throws RegionException {

        final NightList<AutoLexer.SyntaxNode<TokenRecord<Token>>> collection = NightList.create();
        walk(action, list, node, collection::add);
        return collection;
    }

    private static void walk(String action, String list, AutoLexer.SyntaxNode<TokenRecord<Token>> node,
                            Consumer<AutoLexer.SyntaxNode<TokenRecord<Token>>> consumer) throws RegionException {

        if (testNode(node, action)) {
            consumer.accept(foundNode);
        }
        if (testNode(node, list)) {
            walk(action, list, foundNode, consumer);
        }
    }

    public static AutoLexer.SyntaxNode<TokenRecord<Token>> foundNode;

    public static boolean testNode(AutoLexer.SyntaxNode<TokenRecord<Token>> in, String name) {
        for (var node : in.list) {
            if (node.name != null && node.name.equalsIgnoreCase(name)) {
                foundNode = node;
                return true;
            }
        }
        foundNode = null;
        return false;
    }


    public static String getId(AutoLexer.SyntaxNode<TokenRecord<Token>> id) {
        val identifier = getNode(id, "ID");
        return getLeafData(identifier);
    }

    public static String getLeafData(AutoLexer.SyntaxNode<TokenRecord<Token>> in) {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> node = in.list.get(0);
        AutoLexer<TokenRecord<Token>>.LeafNode leafNode = (AutoLexer<TokenRecord<Token>>.LeafNode) node;
        return leafNode.data.data;
    }

    public static AutoLexer.SyntaxNode<TokenRecord<Token>> getNode(AutoLexer.SyntaxNode<TokenRecord<Token>> in,
                                                                   String name) {
        for (var node : in.list) {
            if (node.name.equalsIgnoreCase(name)) {
                return node;
            }
        }
        throw new NullPointerException("Missing a " + name + " Node in " + in.name);
    }

    public static boolean isAutoDeep(SemanticType type) {
        if (type instanceof SemanticType.ArrayType) {
            return isAutoDeep(((SemanticType.ArrayType) type).arrayObject);
        } else if (type instanceof SemanticType.FunctionType) {
            SemanticType.FunctionType functionType = (SemanticType.FunctionType) type;
            return isAutoDeep(functionType.output) || functionType.input.any(SemanticAnalysisHelper::isAutoDeep);
        } else if (type instanceof SemanticType.StructType) {
            SemanticType.StructType structType = (SemanticType.StructType) type;
            return structType.structure == Primitives.auto;
        }
        throw new NullPointerException();
    }


    public static boolean isAutoShallow(SemanticType type) {
        if (type instanceof SemanticType.StructType) {
            SemanticType.StructType structType = (SemanticType.StructType) type;
            return structType.structure == Primitives.auto;
        }
        return false;
    }

    @FunctionalInterface
    public interface ThrowableConsumer<T> {
        void accept(T t) throws RegionException;
    }

    public static SemanticType genSemanticType(AutoLexer.SyntaxNode<TokenRecord<Token>> type, CompilationUnit unit) {
        if (testNode(type, "arrayType")) {
            val arrayType = foundNode;
            return new SemanticType.ArrayType(genSemanticType(getNode(arrayType, "type"), unit));
        } else if (testNode(type, "functionType")) {
            val functionType = getNode(type, "functionType");
            final SemanticType returnType = genSemanticType(getNode(functionType, "Type"), unit);
            final NightList<SemanticType> types = NightList.create();
            val functionTypeCompound = getNode(functionType,
                    "functionTypeCompound");
            final NightList<SemanticType> map =
                    walk("Type", "functionTypeCompound", functionTypeCompound).map(node -> genSemanticType(node, unit));
            types.join(map);
            return new SemanticType.FunctionType(returnType, types);
        } else if (testNode(type, "objectType")) {
            val objectType = getNode(type, "objectType");
            final String typeOfVar = getId(objectType);
            if (!unit.containsStruct(typeOfVar)) {
                throw new UnknownTypeException(typeOfVar, objectType.data.from);
            }
            return new SemanticType.StructType(unit.getStruct(typeOfVar));
        }
        return null;
    }

    public static void assertType(SemanticType a, SemanticType b, int region) {
        final boolean equals = Objects.equals(a, b);
        if (!equals) {
            throw new UnexpectedTypeException(a + " and " + b + " are not the same", region);
        }
    }

    public static void assertTypeNumber(SemanticType a, int region) throws RegionException {
        if (!a.equals(Primitives.integer_type) && !a.equals(Primitives.decimal_type)) {
            throw new UnexpectedTypeException(a + " is not a int or an decimal", region);
        }
    }

    public static SemanticStatement genStatement(AutoLexer.SyntaxNode<TokenRecord<Token>> statement,
                                              CompilationUnit unit) throws RegionException {
        if (testNode(statement, "blockStatement")) {
            val blockStatement = foundNode;
            final List<SemanticStatement> statements = new ArrayList<>();
            walk("statement", "statementCompound", blockStatement).apply(node -> {
                statements.add(genStatement(node,unit));
            });
            return new SemanticStatement.Block(statements);
        } else if (testNode(statement, "declarationStatement")) {
            val declarationStatement = foundNode;
            final SemanticType type = genSemanticType(getNode(declarationStatement, "Type"), unit);
            final String name = getId(declarationStatement);
            val expression = getNode(declarationStatement, "expression");
            return new SemanticStatement.Declaration(name, type, expression);
        } else if (testNode(statement, "assignmentStatement")) {
            val assignmentStatement = foundNode;
            val member = getNode(assignmentStatement, "member");
            val expression = getNode(assignmentStatement, "expression");
            return new SemanticStatement.Assignment(member, expression);
        } else if (testNode(statement, "returnStatement")) {
            val assignmentStatement = foundNode;
            val expression = getNode(assignmentStatement, "expression");
            return new SemanticStatement.Return(expression);
        } else if (testNode(statement, "evaluationStatement")) {
            val assignmentStatement = foundNode;
            val expression = getNode(assignmentStatement, "expression");
            return new SemanticStatement.Evaluation(expression);
        } else
            throw new NullPointerException();
        //TODO make branches statement
    }


}
