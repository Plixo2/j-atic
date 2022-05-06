package de.plixo.atic.lexer;

import de.plixo.atic.LexToken;
import de.plixo.atic.Token;
import de.plixo.atic.TokenRecord;

import java.util.List;
import java.util.Stack;

import static de.plixo.atic.LexToken.IDENTIFIER;
import static de.plixo.atic.LexToken.TOPLEVEL;
import static de.plixo.atic.Token.KEYWORD;


public class Lexer implements LexBranches {

    private final Stack<LexToken> tokenStack = new Stack<>();

    final TokenStream<TokenRecord<Token>> stream;

    public Lexer(List<TokenRecord<Token>> records) {
        stream = new TokenStream<>(records);
    }

    @Override
    public SyntaxNode topLevel() {
        begin(TOPLEVEL);
        if (test(KEYWORD)) {

        }
        return end();
    }

    @Override
    public SyntaxNode identifier() {
        begin(IDENTIFIER);
        final String data = stream.current().data;
        assertToken(KEYWORD);
        return finishData(data);
    }

    public boolean test(Token token) {
        return stream.current().token == token;
    }

    public void assertToken(Token token) {
        if (stream.current().token != token) {
            throw new UnexpectedTokenException("Expected " + token.name() + ", but got " + stream.current().token);
        }
    }

    public void begin(LexToken token) {
        tokenStack.push(token);
    }

    public SyntaxNode end() {
        tokenStack.pop();
        return null;
    }


    public SyntaxNode finish(SyntaxNode... sub) {
        return new SyntaxNode(tokenStack.pop(), "", sub);
    }

    public SyntaxNode finish(List<SyntaxNode> list) {
        return new SyntaxNode(tokenStack.pop(), "", list);
    }

    public SyntaxNode finishData(String data) {
        return new SyntaxNode(tokenStack.pop(), data);
    }

    public static class UnexpectedTokenException extends RuntimeException {
        public UnexpectedTokenException(String message) {
            super(message);
        }
    }
    public static class MissingTokenException extends RuntimeException {
        public MissingTokenException(String message) {
            super(message);
        }
    }
}
