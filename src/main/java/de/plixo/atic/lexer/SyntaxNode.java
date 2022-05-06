package de.plixo.atic.lexer;

import de.plixo.atic.LexToken;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class SyntaxNode {
    final LexToken type;
    final String data;
    final List<SyntaxNode> sub;

    public SyntaxNode(LexToken type, String data, SyntaxNode... sub) {
        this.type = type;
        this.data = data;
        this.sub = Arrays.asList(sub);
    }
}
