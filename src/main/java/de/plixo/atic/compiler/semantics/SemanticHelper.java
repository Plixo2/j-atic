package de.plixo.atic.compiler.semantics;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.exceptions.IncompatibleTypeException;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;

import java.util.Objects;
import java.util.function.Consumer;

public class SemanticHelper {

    public static boolean isEquals(SemanticType a, SemanticType b) {
        return a.equals(b);
    }

    public static void walk(String action, String list, AutoLexer.SyntaxNode<TokenRecord<Token>> node,
                            Consumer<AutoLexer.SyntaxNode<TokenRecord<Token>>> consumer) {

        if (testNode(node, action)) {
            consumer.accept(yieldNode(node, action));
        }
        if (testNode(node, list)) {
            walk(action, list, yieldNode(node, list), consumer);
        }
    }

    public static boolean testNode(AutoLexer.SyntaxNode<TokenRecord<Token>> in, String name) {
        for (var node : in.list) {
            if (node.name != null && node.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static String getLeafData(AutoLexer.SyntaxNode<TokenRecord<Token>> in) {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> node = in.list.get(0);
        AutoLexer<TokenRecord<Token>>.LeafNode leafNode =
                (AutoLexer<TokenRecord<Token>>.LeafNode) node;
        return leafNode.data.data;
    }

    public static AutoLexer.SyntaxNode<TokenRecord<Token>> yieldNode(AutoLexer.SyntaxNode<TokenRecord<Token>> in,
                                                                      String name) {
        for (var node : in.list) {
            if (node.name.equalsIgnoreCase(name)) {
                return node;
            }
        }
        throw new NullPointerException("Missing a " + name + " Node in " + in.name);
    }


    public static void assertType(SemanticType a , SemanticType b) {
        final boolean equals = Objects.equals(a, b);
        if(!equals) {
            throw new IncompatibleTypeException(a + " and " + b + " are not the same");
        }
    }
}