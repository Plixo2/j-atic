package de.plixo.atic.compiler.semantics.statement;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.buckets.FunctionStruct;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;

public class SemanticExpression {
    public AutoLexer.SyntaxNode<TokenRecord<Token>> node;
    public FunctionStruct functionStruct;
}
