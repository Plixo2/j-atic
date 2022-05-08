package de.plixo.lexer;

import de.plixo.lexer.exceptions.UnknownRuleException;
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
                if (!entry.hidden) {
                    nodes.add(genLeaf(token));
                }
                stream.consume();
            } else {
                final SyntaxNode<T> child = testRule(entry.rule, stream);
                if (child == null) {
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
    }

    @RequiredArgsConstructor
    public class LeafNode extends SyntaxNode<T> {
        public final T data;
    }

}
