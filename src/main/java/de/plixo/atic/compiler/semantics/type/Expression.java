package de.plixo.atic.compiler.semantics.type;

import de.plixo.atic.Token;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;

import static de.plixo.atic.compiler.semantics.n.SemanticAnalysisHelper.*;

@RequiredArgsConstructor
public enum Expression {
    BOOL_EXPR("boolArithmetic", "comparisonArithmetic", "boolArithmeticFunc"),
    COMP_EXPR("comparisonArithmetic", "arithmetic", "comparisonArithmeticFunc"),
    ARITHMETIC("arithmetic", "term", "arithmeticFunc"),
    TERM("term", "factor", "termFunc"),
    FACTOR("term", "", ""),
    ;
    final String name;
    final String defaultNext;
    final String function;

    public boolean isImplemented(AutoLexer.SyntaxNode<TokenRecord<Token>> record) {
        return isImplemented(record, this.function);
    }
    public AutoLexer.SyntaxNode<TokenRecord<Token>> next(AutoLexer.SyntaxNode<TokenRecord<Token>> record) {
        return getNode(record,defaultNext);
    }

    public String function(AutoLexer.SyntaxNode<TokenRecord<Token>> record) {
        return getLeafData(getNode(record,function));
    }

    public AutoLexer.SyntaxNode<TokenRecord<Token>> same(AutoLexer.SyntaxNode<TokenRecord<Token>> record) {
        return getNode(record,name);
    }

    public Expression getNext() {
        if (this == FACTOR) {
            return null;
        }
        return Expression.values()[this.ordinal() + 1];
    }

    private static boolean isImplemented(AutoLexer.SyntaxNode<TokenRecord<Token>> record, String function) {
        return testNode(record, function);
    }
}
