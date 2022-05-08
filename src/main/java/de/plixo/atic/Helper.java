package de.plixo.atic;

import de.plixo.atic.compiler.CodeGeneration;
import de.plixo.atic.compiler.Operation;
import de.plixo.lexer.AutoLexer;
import de.plixo.lexer.tokenizer.TokenRecord;

import java.util.Iterator;
import java.util.function.Function;

public class Helper {

    public static void printBucket(CodeGeneration.Bucket bucket) {
        System.out.println("Bucket [" + bucket.codes.size() + "]");
        for (int i = 0; i < bucket.codes.size(); i++) {
            final int code = bucket.codes.get(i);
            final int opCode = code >> 24 & 0xFF;
            final int a = (code >> 16 & 0xFF);
            final int b = (code >> 8 & 0xFF);
            final int c = (code & 0xFF);
            final Operation operation = Operation.indexedMap.get(opCode);
            final String name = operation.name();
            if (operation == Operation.LOAD_CONST) {
                System.out.println("LOAD_CONST: "  + bucket.codes.get(++i) + " -> " + a);
            } else {
                System.out.println(name + ": " + a + " " + b  + " -> " + c);
            }

        }
    }

    public static <T> void printNode(AutoLexer.SyntaxNode<TokenRecord<T>> node) {
        StringBuilder buffer = new StringBuilder();
        print(node, buffer, "", "", (token) -> {
            AutoLexer<TokenRecord<T>>.LeafNode leafNode =
                    (AutoLexer<TokenRecord<T>>.LeafNode) token;
            return "\"" + leafNode.data.data + "\"";
        });
        System.out.println(buffer.toString());
    }

    private static <T> void print(AutoLexer.SyntaxNode<T> obj, StringBuilder buffer, String prefix,
                                  String childrenPrefix, Function<AutoLexer.SyntaxNode<T>, String> leafInfo) {
        buffer.append(prefix);
        if (obj instanceof AutoLexer.LeafNode) {
            buffer.append(obj.name).append(": ").append(leafInfo.apply(obj));
        } else {
            buffer.append(obj.name);
        }

        buffer.append('\n');
        for (Iterator<AutoLexer.SyntaxNode<T>> it = obj.list.iterator(); it.hasNext(); ) {
            AutoLexer.SyntaxNode<T> next = it.next();
            if (it.hasNext()) {
                print(next, buffer, childrenPrefix + "├── ", childrenPrefix + "│   ", leafInfo);
            } else {
                print(next, buffer, childrenPrefix + "└── ", childrenPrefix + "    ", leafInfo);
            }
        }
    }


}
