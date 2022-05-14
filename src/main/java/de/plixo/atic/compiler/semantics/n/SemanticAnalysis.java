package de.plixo.atic.compiler.semantics.n;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.Primitives;
import de.plixo.atic.compiler.semantics.buckets.CompilationUnit;
import de.plixo.atic.compiler.semantics.buckets.FunctionStruct;
import de.plixo.atic.compiler.semantics.buckets.Namespace;
import de.plixo.atic.compiler.semantics.buckets.Structure;
import de.plixo.atic.compiler.semantics.n.exceptions.NameCollisionException;
import de.plixo.atic.compiler.semantics.n.exceptions.RegionException;
import de.plixo.atic.compiler.semantics.n.exceptions.UnknownTypeException;
import de.plixo.atic.compiler.semantics.n.exceptions.UnresolvedAutoException;
import de.plixo.atic.compiler.semantics.statement.SemanticExpression;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

import static de.plixo.atic.compiler.semantics.n.SemanticAnalysisHelper.*;


public class SemanticAnalysis {

    final CompilationUnit unit;
    final AutoLexer.SyntaxNode<TokenRecord<Token>> topLevel;

    public SemanticAnalysis(AutoLexer.SyntaxNode<TokenRecord<Token>> topLevel) {
        unit = new CompilationUnit();
        this.topLevel = topLevel;
    }

    public void analyse() {
        addPrimitives();
        try {
            makeStructsObjects();
            makeStructMembers();
            makeNamespaceObject();
        } catch (RegionException exception) {
            exception.printStackTrace();
            System.err.println("Error at " + exception.position);
        }
    }

    private void addPrimitives() {
        unit.addStructure(Primitives.decimal);
        unit.addStructure(Primitives.integer);
        unit.addStructure(Primitives.void_);
        unit.addStructure(Primitives.auto);
    }

    private void makeStructsObjects() throws RegionException {
        walk("struct", "top", topLevel, node -> {
            final Structure structure = new Structure(getId(node));
            if (unit.containsStruct(structure.name)) {
                throw new NameCollisionException(structure.name, node.data.from);
            }
            unit.addStructure(structure);
        });
    }

    private void makeStructMembers() throws RegionException {
        walk("struct", "top", topLevel, struct -> {
            final String name = getId(struct);
            final Structure structure = unit.getStruct(name);
            walk("declaration", "declarationCompound", struct, node -> {
                val typeOfVar = getNode(node, "Type");
                final String nameOfVar = getId(node);
                final SemanticType semanticType = genSemanticType(typeOfVar, unit);
                if (isAutoDeep(semanticType)) {
                    throw new UnresolvedAutoException(nameOfVar, node.data.from);
                }
                structure.members.put(nameOfVar, semanticType);
            });
        });
    }

    private void makeNamespaceObject() throws RegionException {
        walk("logic", "top", topLevel, node -> {
            final String name = getId(node);
            final Namespace namespace = new Namespace(name);
            walk("logicSpace", "logicCompound", node, function -> {
                final String functionName = getId(function);
                final SemanticType returnType = genSemanticType(getNode(function, "type"), unit);
                if (isAutoDeep(returnType)) {
                    throw new UnresolvedAutoException(functionName, function.data.from);
                }
                final SemanticStatement statement = genStatement(getNode(function, "statement"));
                final FunctionStruct functionStruct = new FunctionStruct(functionName, returnType, statement);
                walk("inputTerm", "inputList", function, input -> {
                    final SemanticType inType = genSemanticType(getNode(input, "type"), unit);
                    if (isAutoDeep(returnType)) {
                        throw new UnknownTypeException("return", input.data.from);
                    }
                    final String inName = getId(input);
                    if (functionStruct.input.containsKey(inName)) {
                        throw new NameCollisionException(inName, input.data.from);
                    }
                    functionStruct.input.put(inName, inType);
                });
                namespace.functions.add(functionStruct);
            });
            unit.addNamespace(namespace);
        });
    }

    private SemanticStatement genStatement(AutoLexer.SyntaxNode<TokenRecord<Token>> statement) throws RegionException {
        if (testNode(statement, "blockStatement")) {
            val blockStatement = foundNode;
            final List<SemanticStatement> statements = new ArrayList<>();
            walk("statement", "statementCompound", blockStatement, node -> {
                statements.add(genStatement(node));
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
            val member = getNode(assignmentStatement, "ID");
            val expression =  getNode(assignmentStatement, "expression");
            return new SemanticStatement.Assignment(member, expression);
        } else
            throw new NullPointerException();
        //TODO make branches statement, evaluation statement
    }




}
