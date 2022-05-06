package de.plixo.atic;

import java.util.regex.Pattern;


public enum Token {
    KEYWORD("[a-zA-Z]", "\\w+$"),
    WHITESPACE("\\s", "[^\\S\\r\\n]+");


    final Pattern peek;
    final Pattern capture;

    Token(String peek, String capture) {
        this.peek = Pattern.compile("^" + peek, Pattern.MULTILINE);
        this.capture = Pattern.compile("^" + capture + "$", Pattern.MULTILINE);
    }
}
