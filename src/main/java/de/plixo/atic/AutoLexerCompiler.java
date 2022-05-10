package de.plixo.atic;

import de.plixo.atic.compiler.Compiler;
import de.plixo.lexer.AutoLexer;
import de.plixo.lexer.tokenizer.TokenRecord;

public class AutoLexerCompiler extends Compiler<AutoLexer.SyntaxNode<TokenRecord<Token>>> {
//    public AutoLexerCompiler() {
//        super((token) -> {
////            if (token.list.size() == 0) {
////                throw new MissingNodeException("");
////            }
//            final AutoLexer.SyntaxNode<TokenRecord<Token>> node = token.list.get(0);
//            AutoLexer<TokenRecord<Token>>.LeafNode leafNode =
//                    (AutoLexer<TokenRecord<Token>>.LeafNode) node;
//            return leafNode.data.data;
//        });
//    }
}
