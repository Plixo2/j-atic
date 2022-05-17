package de.plixo.atic.compiler.semantics.n;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.Primitives;
import de.plixo.atic.compiler.semantics.buckets.*;
import de.plixo.atic.compiler.semantics.n.exceptions.NameCollisionException;
import de.plixo.atic.compiler.semantics.n.exceptions.RegionException;
import de.plixo.atic.compiler.semantics.n.exceptions.UnknownTypeException;
import de.plixo.atic.compiler.semantics.n.exceptions.UnresolvedAutoException;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

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
            unit.namespaces.apply(node -> {
                node.functions.apply(functionStruct -> {
                    new SemanticStatementValidator(functionStruct.statement,
                            new FunctionCompilationUnit(unit, functionStruct)).validate();
                });
            });
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
        walk("struct", "top", topLevel).split(node -> new Structure(getId(node)))
                .throwIf(ref -> unit.containsStruct(ref.b.name), ref -> {
                    throw new NameCollisionException(ref.b.name, ref.a.data.from);
                }).apply(ref -> unit.addStructure(ref.b));
    }

    private void makeStructMembers() throws RegionException {
        walk("struct", "top", topLevel).apply(struct -> {
            final String name = getId(struct);
            final Structure structure = unit.getStruct(name);
            walk("declaration", "declarationCompound", struct).apply(node -> {
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
        walk("logic", "top", topLevel).apply(node -> {
            final String name = getId(node);
            final Namespace namespace = new Namespace(name);
            walk("logicSpace", "logicCompound", node).apply(function -> {
                final String functionName = getId(function);
                val sub = getNode(function, "richFunction");
                final SemanticType returnType = genSemanticType(getNode(sub, "type"), unit);
                if (isAutoDeep(returnType)) {
                    throw new UnresolvedAutoException(functionName, sub.data.from);
                }
                final SemanticStatement statement = genStatement(getNode(sub,
                        "statement"), unit);
                final FunctionStruct functionStruct = new FunctionStruct(functionName, returnType, statement);
                walk("inputTerm", "inputList", sub).apply(input -> {
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


}
