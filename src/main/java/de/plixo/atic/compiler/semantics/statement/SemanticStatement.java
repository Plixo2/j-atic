package de.plixo.atic.compiler.semantics.statement;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.type.SemanticType;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;

import java.util.List;

public abstract class SemanticStatement {
    @RequiredArgsConstructor
    public static class Evaluation extends SemanticStatement {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
    }

    @RequiredArgsConstructor
    public static class Declaration extends SemanticStatement {
        public final String name;
        public final SemanticType type;
        public int register = -1;
        public final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
    }

    @RequiredArgsConstructor
    public static class Assignment extends SemanticStatement {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> member;
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
    }

    @RequiredArgsConstructor
    public static class Branch extends SemanticStatement {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
        final SemanticStatement statement;
        final SemanticStatement alternative;
    }

    @RequiredArgsConstructor
    public static class Block extends SemanticStatement {
        public final List<SemanticStatement> statements;
    }

}
