package de.plixo.atic.compiler.semantics.buckets;

import de.plio.nightlist.NightList;
import de.plixo.atic.Token;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;

import java.util.*;

public class CompilationUnit {
    public final Map<String, Structure> structures = new HashMap<>();
    public final NightList<Namespace> namespaces = NightList.create();
    public final Map<AutoLexer.SyntaxNode<TokenRecord<Token>>,FunctionStruct> preEvaluatedFunction = new HashMap<>();

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
