package de.plixo.atic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        final String fileContent = readFile(new File("files/in.txt"));
        try {
            final List<TokenRecord<Token>> apply = Tokenizer.apply(fileContent, Token.values(),
                    (token, subString) -> token.peek.asPredicate().test(subString),
                    (token, subString) -> token.capture.asPredicate().test(subString)
            );
            apply.forEach(f -> {
                System.out.println("Token: " + f.token.name() + ", " + f.data);
            });
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
