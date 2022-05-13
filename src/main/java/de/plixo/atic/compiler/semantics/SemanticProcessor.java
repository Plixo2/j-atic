package de.plixo.atic.compiler.semantics;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.buckets.FunctionStruct;
import de.plixo.atic.compiler.semantics.buckets.Namespace;
import de.plixo.atic.compiler.semantics.buckets.Structure;
import de.plixo.atic.compiler.semantics.statement.SemanticStatement;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.exceptions.NameCollisionException;
import de.plixo.atic.exceptions.UnknownTypeException;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.plixo.atic.compiler.semantics.SemanticHelper.*;

public class SemanticProcessor {

    final static List<Structure> structures = new ArrayList<>();
    final static Map<String, Structure> strutMap = new HashMap<>();
    final static List<Namespace> namespaces = new ArrayList<>();

    public static void convert(AutoLexer.SyntaxNode<TokenRecord<Token>> in) {
        walk("struct", "top", in, node -> structures.add(genStruct(node)));
        structures.add(Primitives.integer);
        structures.add(Primitives.decimal);
        structures.add(Primitives.auto);
        structures.add(Primitives.void_);

        structures.forEach(structure -> {
            if (strutMap.containsKey(structure.name)) {
                throw new NameCollisionException("Struct name \"" + structure.name + "\" has multiple entries");
            }
            strutMap.put(structure.name, structure);
        });
        walk("struct", "top", in, SemanticProcessor::makeMembers);
        walk("logic", "top", in, node -> namespaces.add(genNamespace(node)));
        StatementValidator.validate(namespaces,strutMap);
    }

    private static Namespace genNamespace(AutoLexer.SyntaxNode<TokenRecord<Token>> logic) {
        final String name = getLeafData(yieldNode(logic, "ID"));
        final Namespace namespace = new Namespace(name);
        walk("logicSpace", "logicCompound", logic, node -> namespace.functions.add(genFunctionStruct(node)));
        return namespace;
    }

    private static FunctionStruct genFunctionStruct(AutoLexer.SyntaxNode<TokenRecord<Token>> logicSpace) {
        final String name = getLeafData(yieldNode(logicSpace, "ID"));
        final SemanticType returnType = genSemanticType(yieldNode(logicSpace, "type"),strutMap);
        if (isAutoDeep(returnType)) {
            throw new UnknownTypeException("auto cant be resolved as a function return type \"" + name + "\"");
        }
        final SemanticStatement statement = genStatement(yieldNode(logicSpace, "statement"));
        final FunctionStruct functionStruct = new FunctionStruct(name, returnType, statement);
        walk("inputTerm", "inputList", logicSpace, node -> {
            final SemanticType inType = genSemanticType(yieldNode(node, "type"),strutMap);
            if (isAutoDeep(returnType)) {
                throw new UnknownTypeException("auto cant be resolved as a function input \"" + name + "\"");
            }
            final String inName = getLeafData(yieldNode(node, "ID"));
            if (functionStruct.input.containsKey(inName)) {
                throw new NameCollisionException("Function input \"" + inName + "\" has multiple entries in \"" + name + "\"");
            }
            functionStruct.input.put(inName, inType);
        });

        return functionStruct;
    }

    private static SemanticStatement genStatement(AutoLexer.SyntaxNode<TokenRecord<Token>> statement) {
        if (testNode(statement, "blockStatement")) {
            val blockStatement = yieldNode(statement, "blockStatement");
            final List<SemanticStatement> statements = new ArrayList<>();
            walk("statement", "statementCompound", blockStatement, node -> {
                statements.add(genStatement(node));
            });
            return new SemanticStatement.Block(statements);
        } else if (testNode(statement, "declarationStatement")) {
            val declarationStatement = yieldNode(statement, "declarationStatement");
            final SemanticType type = genSemanticType(yieldNode(declarationStatement, "Type"),strutMap);
            final String name = getLeafData(yieldNode(declarationStatement, "ID"));
            val expression = yieldNode(declarationStatement, "expression");
            return new SemanticStatement.Declaration(name, type, expression);
        } else if (testNode(statement, "assignmentStatement")) {
            val assignmentStatement = yieldNode(statement, "assignmentStatement");
            val member = yieldNode(assignmentStatement, "ID");
            val expression = yieldNode(assignmentStatement, "expression");
            return new SemanticStatement.Assignment(member, expression);
        } else
            throw new UnknownTypeException("Missing type here");
        //TODO make branches statement, evaluation statement
    }

    private static void makeMembers(AutoLexer.SyntaxNode<TokenRecord<Token>> struct) {
        val id = yieldNode(struct, "ID");
        final String name = getLeafData(id);
        final Structure structure = strutMap.get(name);
        assert structure != null;
        walk("declaration", "declarationCompound", struct, node -> {
            val typeOfVar = yieldNode(node, "Type");
            final String nameOfVar = getLeafData(yieldNode(node, "ID"));
            if (strutMap.containsKey(nameOfVar)) {
                throw new NameCollisionException("Variable name \"" + structure.name + "\" has multiple entries in " +
                        "\"" + name + "\"");
            }
            final SemanticType semanticType = genSemanticType(typeOfVar,strutMap);
            if (isAutoDeep(semanticType)) {
                throw new UnknownTypeException("auto cant be resolved on structure \"" + name + "\"");
            }
            structure.members.put(nameOfVar, semanticType);
        });
    }


    private static Structure genStruct(AutoLexer.SyntaxNode<TokenRecord<Token>> struct) {
        val id = yieldNode(struct, "ID");
        final String name = getLeafData(id);
        return new Structure(name);
    }


}
