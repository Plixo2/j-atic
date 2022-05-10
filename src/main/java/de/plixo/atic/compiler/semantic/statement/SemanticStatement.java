package de.plixo.atic.compiler.semantic.statement;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantic.SemanticProcessor;
import de.plixo.atic.compiler.semantic.type.SemanticType;
import de.plixo.lexer.AutoLexer;
import de.plixo.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;

import java.util.List;

public abstract class SemanticStatement {
    @RequiredArgsConstructor
    public static class Evaluation extends SemanticStatement {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
    }

    @RequiredArgsConstructor
    public static class Declaration extends SemanticStatement {
        final String name;
        final SemanticType type;
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
    }

    @RequiredArgsConstructor
    public static class Assignment extends SemanticStatement {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> member;
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
    }

    @RequiredArgsConstructor
    public static class BranchDeclaration extends SemanticStatement {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> expression;
        final SemanticStatement statement;
        final SemanticStatement alternative;
    }

    @RequiredArgsConstructor
    public static class Block extends SemanticStatement {
        final List<SemanticStatement> statement;
    }

}
