package de.plixo.atic.lexer;

import de.plixo.atic.exceptions.FailedTokenCaptureException;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import de.plixo.atic.lexer.tokenizer.Tokenizer;
import de.plixo.atic.exceptions.MissingTokenException;
import de.plixo.atic.exceptions.UnknownRuleException;
import de.plixo.atic.exceptions.UnexpectedTokenException;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static de.plixo.atic.lexer.GrammarReader.GrammarToken.*;

public class GrammarReader {

    public static RuleSet loadFromString(String[] txt) throws FailedTokenCaptureException {
        final List<Runnable> onFinalResolve = new ArrayList<>();
        final List<Rule> rules = new ArrayList<>();
        for (String line : txt) {
            final List<TokenRecord<GrammarToken>> apply = Tokenizer.apply(line, GrammarToken.values(),
                    (token, subString) -> token.peek.asPredicate().test(subString),
                    (token, subString) -> token.capture.asPredicate().test(subString)
            );
            apply.removeIf(f -> f.token == GrammarToken.WHITESPACE);
            final TokenStream<TokenRecord<GrammarToken>> stream = new TokenStream<>(apply);
            if (stream.size() == 0) {
                continue;
            }
            if(testToken(stream,COMMENT)) {
                continue;
            }

            final Rule rule = genRule(stream, (name, ref) -> onFinalResolve.add(() -> {
                final ArrayList<Rule> rules1 = new ArrayList<>(rules);
                rules1.removeIf(ruleRef -> !ruleRef.name.equalsIgnoreCase(name));
                if (rules1.size() > 1) {
                    throw new UnknownRuleException("Found more than one definition of rule \"" + name +
                            "\"");
                } else if (rules1.size() == 0) {
                    throw new UnknownRuleException("Unknown rule \"" + name + "\"");
                }
                ref.rule = rules1.get(0);
            }));
            rules.add(rule);
        }
        onFinalResolve.forEach(Runnable::run);
        return new RuleSet(rules);
    }


    private static Rule genRule(TokenStream<TokenRecord<GrammarToken>> stream, BiConsumer<String, Entry> delayedResolve) {
        final List<Sentence> sentences = new ArrayList<>();

        final String name = assertToken(stream, KEYWORD);
        consume(stream);
        assertToken(stream, ASSIGN);
        consume(stream);

        while (stream.hasEntriesLeft()) {
            final List<Entry> entries = new ArrayList<>();
            while (stream.hasEntriesLeft() && !testToken(stream, OR)) {
                final String data = stream.current().data;
                if (testToken(stream, KEYWORD)) {
                    final Entry ruleEntry = genPlaceholderEntry();
                    entries.add(ruleEntry);
                    delayedResolve.accept(data, ruleEntry);
                    stream.consume();
                } else if (testToken(stream, LITERAL)) {
                    final Entry e = genEntry(data.substring(1, data.length() - 1));
                    entries.add(e);
                    stream.consume();
                    if(stream.hasEntriesLeft() && testToken(stream, HIDDEN)) {
                        stream.consume();
                        e.hidden = true;
                    }
                } else {
                    throw new UnexpectedTokenException("Expected keyword or literal, but got " + stream
                            .current());
                }

            }
            stream.consume();
            sentences.add(new Sentence(entries));
        }


        return new Rule(name, sentences);
    }

    private static boolean testToken(TokenStream<TokenRecord<GrammarToken>> stream, GrammarToken token) {
        if (!stream.hasEntriesLeft()) {
            throw new MissingTokenException("Expected " + token.name() + ", but ran out of tokens");
        }
        return stream.current().token == token;
    }

    private static String assertToken(TokenStream<TokenRecord<GrammarToken>> stream, GrammarToken token) {
        if (!stream.hasEntriesLeft()) {
            throw new MissingTokenException("Expected " + token.name() + ", but ran out of tokens");
        }
        if (stream.current().token != token) {
            throw new UnexpectedTokenException("Expected " + token.name() + ", but got " + stream.current());
        }
        return stream.current().data;
    }

    private static void consume(TokenStream<?> stream) {
        stream.consume();
    }

    @RequiredArgsConstructor
    public static class RuleSet {
        final List<Rule> rules;

        public Rule findRule(String name) {
            for (Rule rule : rules) {
                if (rule.name.equalsIgnoreCase(name)) {
                    return rule;
                }
            }
            return null;
        }
    }


    @RequiredArgsConstructor
    static class Rule {
        final String name;
        final List<Sentence> sentences;
    }

    @RequiredArgsConstructor
    static class Sentence {
        final List<Entry> entries;
    }

    static class Entry {
        String literal = null;
        Rule rule = null;
        boolean hidden = false;

        boolean isLiteral() {
            return literal != null;
        }
    }

    private static Entry genEntry(String literal) {
        final Entry entry = new Entry();
        entry.literal = literal;
        return entry;
    }

    private static Entry genPlaceholderEntry() {
        return new Entry();
    }

    public enum GrammarToken {
        KEYWORD("[a-zA-Z]", "\\w+$"),
        WHITESPACE("\\s", "\\s*"),
        ASSIGN(":=", "(:|:=)"),
        LITERAL("\"", "((\\\"[^\\\"]*\\\")|(\\\"[^\\\"]*))"),
        OR("\\|", "\\|"),
        HIDDEN("\\?", "\\?"),
        COMMENT("//", "//.*"),


        ;
        public final Pattern peek;
        public final Pattern capture;

        GrammarToken(String peek, String capture) {
            this.peek = Pattern.compile("^" + peek, Pattern.MULTILINE);
            this.capture = Pattern.compile("^" + capture + "$", Pattern.MULTILINE);
        }
    }
}