package de.plixo.atic.compiler.semantics.buckets;

import de.plixo.atic.Token;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;

import java.util.*;

public class CompilationUnit {
    final Map<String, Structure> structures = new HashMap<>();
    final List<Namespace> namespaces = new ArrayList<>();
    final Map<AutoLexer.SyntaxNode<TokenRecord<Token>>,FunctionStruct> preEvaluatedFunction = new HashMap<>();

    public void addStructure(Structure structure) {
        structures.put(structure.name, structure);
    }

    public boolean containsStruct(String name) {
        return structures.containsKey(name);
    }


    public Structure getStruct(String name) {
        return structures.get(name);
    }

    public void addNamespace(Namespace namespace) {
        namespaces.add(namespace);
    }


    public void addUnassociatedFunction(FunctionStruct struct,AutoLexer.SyntaxNode<TokenRecord<Token>> node) {
        assert node.name.equalsIgnoreCase("function");
        preEvaluatedFunction.put(node,struct);
    }
}
