package de.plixo.atic.cfg;

import java.util.regex.Pattern;

public enum CFGToken {
    KEYWORD("[a-zA-Z]", "\\w+$"),
    WHITESPACE("\\s", "\\s*"),
    ASSIGN(":=", "(:|:=)"),
    LITERAL("\"", "((\\\"[^\\\"]*\\\")|(\\\"[^\\\"]*))"),
    OR("\\|", "\\|");


    final Pattern peek;
    final Pattern capture;

    CFGToken(String peek, String capture) {
        this.peek = Pattern.compile("^" + peek, Pattern.MULTILINE);
        this.capture = Pattern.compile("^" + capture + "$", Pattern.MULTILINE);
    }
}
