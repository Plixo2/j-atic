package de.plixo.atic;

import de.plixo.atic.lexer.AutoLexer;
import de.plixo.atic.lexer.GrammarReader;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import de.plixo.atic.lexer.tokenizer.Tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class CFGTest {
    public static void main(String[] args) {
        final String fileContent = readFile(new File("files/cfg.txt"));
        String fileInput = readFile(new File("files/testlayout.txt"));
        fileInput = fileInput.replace(System.lineSeparator(), " ");


        long t1 = System.currentTimeMillis();


        final GrammarReader.RuleSet ruleSet = GrammarReader
                .loadFromString(fileContent.split(System.lineSeparator()));
        System.out.println("-> Grammar rules took " + (System.currentTimeMillis() - t1) + "ms ");

        t1 = System.currentTimeMillis();
        final List<TokenRecord<Token>> apply = Tokenizer
                .apply(fileInput, Token.values(),
                        (token, subString) -> token.peek.asPredicate().test(subString),
                        (token, subString) -> token.capture.asPredicate().test(subString)
                );
        apply.removeIf(f -> f.token == Token.WHITESPACE);
        System.out.println("-> Tokenizer took " + (System.currentTimeMillis() - t1) + "ms ");
        t1 = System.currentTimeMillis();
        apply.add(new TokenRecord<>(Token.END_OF_FILE,"END OF FILE",fileInput.length(),fileInput.length()));
        final AutoLexer<TokenRecord<Token>> autoLexer =
                new AutoLexer<>((string, token) -> token.token.alias.equalsIgnoreCase(string));
        final AutoLexer.SyntaxNode<TokenRecord<Token>> in = autoLexer
                .reverseRule(ruleSet, "In", apply);
        final long time = System.currentTimeMillis() - t1;
        DebugHelper.printNode(in);
        System.out.println("-> Lexer took " + time + "ms ");
        t1 = System.currentTimeMillis();

        if (in == null) {
            apply.forEach(token -> System.out.println(token.data));
            System.err.println("Could not apply ruleset");
            return;
        }

        SemanticProcessor.convert(in);
      //  AutoLexerCompiler compiler = new AutoLexerCompiler();
      //  compiler.entry(in);
      //  System.out.println("-> Compiler took " + (System.currentTimeMillis() - t1) + "ms ");

    }


    private static String readFile(File file) {
        if (!file.exists()) {
            System.out.println("File does not exist");
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            int content;
            while ((content = fileInputStream.read()) != -1) {
                builder.append((char) content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
