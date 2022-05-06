package de.plixo.atic.cfg;

import de.plixo.atic.TokenRecord;
import de.plixo.atic.Tokenizer;
import de.plixo.atic.lexer.Lexer;
import de.plixo.atic.lexer.TokenStream;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.BiConsumer;

import static de.plixo.atic.cfg.CFGToken.*;

public class GrammarGen {


    public static boolean reverseRule(RuleSet ruleSet, String entry, String[] tokens) {
        final Rule rule = ruleSet.findRule(entry);
        if (rule == null) {
            throw new UnknownRuleException("Unknown entry rule \"" + entry + "\"");
        }

        final StackableTokenStream<String> stream = new StackableTokenStream<>(Arrays.asList(tokens));

        final boolean test = testRule(rule, stream);
        if (stream.hasEntriesLeft()) {
            System.out.println("Could not finish!!!");
        }
        return test;
    }

    private static boolean testRule(Rule rule, StackableTokenStream<String> stream) {
        for (Sentence sentence : rule.sentences) {
            stream.push();
            final boolean test = testSentence(sentence, stream);
            if (!test) {
                stream.pop();
            } else {
                stream.drop();
                return true;
            }
        }
        return false;
    }

    private static boolean testSentence(Sentence sentence, StackableTokenStream<String> stream) {
        for (int i = 0; i < sentence.entries.size(); i++) {
            final Entry entry = sentence.entries.get(i);
            if (!stream.hasEntriesLeft()) {
                return false;
            }
            if (entry.isLiteral()) {
                final String txt = stream.current();
                if (entry.literal.equalsIgnoreCase(txt)) {
                    stream.consume();
                    continue;
                }
                return false;
            } else {
                if (testRule(entry.rule, stream)) {
                    continue;
                }
                return false;
            }

        }
        return true;
    }

    public static String genRandomSentence(RuleSet ruleSet, String entry) {
        final Rule rule = ruleSet.findRule(entry);
        if (rule == null)
            throw new UnknownRuleException("Unknown entry rule \"" + entry + "\"");
        return genSubSentence(rule);
    }

    final static Random random = new Random();

    private static String genSubSentence(Rule rule) {
        final StringBuilder builder = new StringBuilder();

        final int size = rule.sentences.size();
        if (size == 0) {
            return "";
        }
        final Sentence sentence = rule.sentences.get(random.nextInt(size));
        sentence.entries.forEach(entry -> {
            if (entry.isLiteral()) {
                builder.append(entry.literal);
                builder.append(" ");
            } else {
                builder.append(genSubSentence(entry.rule));
            }
        });
        return builder.toString();
    }

    public static RuleSet loadFromString(String[] txt) throws Tokenizer.FailedTokenCaptureException {
        final List<Runnable> onFinalResolve = new ArrayList<>();
        final List<Rule> rules = new ArrayList<>();
        for (String line : txt) {
            final List<TokenRecord<CFGToken>> apply = Tokenizer.apply(line, CFGToken.values(),
                    (token, subString) -> token.peek.asPredicate().test(subString),
                    (token, subString) -> token.capture.asPredicate().test(subString)
            );
            apply.removeIf(f -> f.token == CFGToken.WHITESPACE);
            final TokenStream<TokenRecord<CFGToken>> stream = new TokenStream<>(apply);
            if (stream.size() == 0) {
                continue;
            }
            final Rule rule = genRule(stream, (name, ref) -> {
                onFinalResolve.add(() -> {
                    final ArrayList<Rule> rules1 = new ArrayList<>(rules);
                    rules1.removeIf(ruleRef -> !ruleRef.name.equalsIgnoreCase(name));
                    if (rules1.size() > 1) {
                        throw new UnknownRuleException("Found more than one definition of rule \"" + name +
                                "\"");
                    } else if (rules1.size() == 0) {
                        throw new UnknownRuleException("Unknown rule \"" + name + "\"");
                    }
                    ref.rule = rules1.get(0);
                });
            });
            rules.add(rule);
        }
        onFinalResolve.forEach(Runnable::run);
        return new RuleSet(rules);
    }


    private static Rule genRule(TokenStream<TokenRecord<CFGToken>> stream, BiConsumer<String, Entry> delayedResolve) {
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
                } else if (testToken(stream, LITERAL)) {
                    entries.add(genEntry(data.substring(1, data.length() - 1)));
                } else {
                    throw new Lexer.UnexpectedTokenException("Expected keyword or literal, but got " + stream
                            .current());
                }
                stream.consume();
            }
            stream.consume();
            sentences.add(new Sentence(entries));
        }


        return new Rule(name, sentences);
    }

    private static boolean testToken(TokenStream<TokenRecord<CFGToken>> stream, CFGToken token) {
        if (!stream.hasEntriesLeft()) {
            throw new Lexer.MissingTokenException("Expected " + token.name() + ", but ran out of tokens");
        }
        return stream.current().token == token;
    }

    private static String assertToken(TokenStream<TokenRecord<CFGToken>> stream, CFGToken token) {
        if (!stream.hasEntriesLeft()) {
            throw new Lexer.MissingTokenException("Expected " + token.name() + ", but ran out of tokens");
        }
        if (stream.current().token != token) {
            throw new Lexer.UnexpectedTokenException("Expected " + token.name() + ", but got " + stream.current());
        }
        return stream.current().data;
    }

    private static void consume(TokenStream<?> stream) {
        stream.consume();
    }


    @RequiredArgsConstructor
    static class RuleSet {
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

    public static class UnknownRuleException extends RuntimeException {
        public UnknownRuleException(String message) {
            super(message);
        }
    }

    static class StackableTokenStream<T> extends TokenStream<T> {

        final Stack<Integer> stack = new Stack<>();

        public StackableTokenStream(List<T> list) {
            super(list);
        }

        public void push() {
            stack.push(index);
        }

        public void pop() {
            index = stack.pop();
        }

        public void drop() {
            stack.pop();
        }
    }

}