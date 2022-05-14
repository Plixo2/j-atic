package de.plixo.atic.compiler.semantics.statement;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantics.buckets.FunctionStruct;
import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SemanticExpression {
    @Nullable
    public final AutoLexer.SyntaxNode<TokenRecord<Token>> node;
    @Nullable
    public final FunctionStruct functionStruct;


    public boolean isFunction() {
        return functionStruct != null;
    }

    public static SemanticExpression genExpression(AutoLexer.SyntaxNode<TokenRecord<Token>> node) {
        return new SemanticExpression(node, null);
    }

    public static SemanticExpression genFunction(FunctionStruct functionStruct) {
        return new SemanticExpression(null, functionStruct);
    }
}
