package de.plixo.atic.lexer;

import de.plixo.atic.exceptions.UnknownRuleException;
import de.plixo.atic.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

@RequiredArgsConstructor
public class AutoLexer<T> {

    final BiPredicate<String, T> tokenTest;

    public SyntaxNode<T> reverseRule(GrammarReader.RuleSet ruleSet, String entry, List<T> tokens) {
        final GrammarReader.Rule rule = ruleSet.findRule(entry);
        if (rule == null) {
            throw new UnknownRuleException("Unknown entry rule \"" + entry + "\"");
        }
        final TokenStream<T> stream = new TokenStream<>(tokens);
        final SyntaxNode<T> node = testRule(rule, stream);
        if (stream.hasEntriesLeft()) {
            System.err.println("Entries left");
            while (stream.hasEntriesLeft()) {
                final T current = stream.current();
                stream.consume();
                System.err.println(current);
            }
            return null;
        }
        return node;
    }

    private SyntaxNode<T> testRule(GrammarReader.Rule rule, TokenStream<T> stream) {
        for (GrammarReader.Sentence sentence : rule.sentences) {
            final int index = stream.index();
            final SyntaxNode<T> node = testSentence(sentence, stream);
            if (node == null) {
                stream.setIndex(index);
            } else {
                node.name = rule.name;
                if (stream.hasEntriesLeft())
                    node.data = stream.current();
                return node;
            }
        }
        return null;
    }

    private SyntaxNode<T> testSentence(GrammarReader.Sentence sentence, TokenStream<T> stream) {
        final List<SyntaxNode<T>> nodes = new ArrayList<>();
        for (int i = 0; i < sentence.entries.size(); i++) {
            final GrammarReader.Entry entry = sentence.entries.get(i);
            if (!stream.hasEntriesLeft()) {
                return null;
            }
            if (entry.isLiteral()) {
                final T token = stream.current();
                if (!tokenTest.test(entry.literal, token)) {
                    return null;
                }
                if (!entry.isHidden) {
                    nodes.add(genLeaf(token));
                }
                stream.consume();
            } else {
                final SyntaxNode<T> child = testRule(entry.rule, stream);
                if (child == null) {
                    if (entry.isConcrete) {
                        final T token = stream.current();
                        if (token instanceof TokenRecord) {
                            throw new UnknownRuleException("failed to capture rule " + entry.rule.name + ": " + ((TokenRecord<?>) token).from);
                        }
                        throw new UnknownRuleException(" failed to capture rule " + entry.rule.name);
                        //TODO here
                    }
                    return null;
                }
                nodes.add(child);
            }
        }

        return genNode(nodes);
    }

    private SyntaxNode<T> genLeaf(T data) {
        return new LeafNode(data);
    }

    private SyntaxNode<T> genNode(List<SyntaxNode<T>> list) {
        final SyntaxNode<T> syntaxNode = new SyntaxNode<>();
        syntaxNode.list = list;
        return syntaxNode;
    }

    public static class SyntaxNode<O> {
        public String name;
        public List<SyntaxNode<O>> list = new ArrayList<>();
        public O data;
    }

    @RequiredArgsConstructor
    public class LeafNode extends SyntaxNode<T> {
        public final T data;
    }

}
