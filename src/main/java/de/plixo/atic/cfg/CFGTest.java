package de.plixo.atic.cfg;

import de.plixo.atic.Tokenizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CFGTest {
    public static void main(String[] args) {
        final String fileContent = readFile(new File("files/cfg.txt"));
        try {
            final long t1 = System.currentTimeMillis();
            int chars = 0;
            for (int i = 0; i < 10000; i++) {

                final GrammarGen.RuleSet ruleSet = GrammarGen.loadFromString(fileContent.split(System.lineSeparator()));
                final String test = GrammarGen.genRandomSentence(ruleSet, "In");
                chars += test.length();

                final String[] lines = test.split(" ");
                final boolean reverseRule = GrammarGen.reverseRule(ruleSet, "In", lines);
                assert reverseRule;
            }
            System.out.println("Took " + (System.currentTimeMillis() - t1) + "ms for " + chars + " chars");
        } catch (Tokenizer.FailedTokenCaptureException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(File file) {
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
