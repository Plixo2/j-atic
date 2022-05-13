package de.plixo.atic;

import java.util.regex.Pattern;


public enum Token {
    WHITESPACE("", "\\s", "\\s*"),
    ASSIGN_ARROW("->", "->", "(-|->)"),
    PARENTHESES_O("(", "\\(", "\\("),
    PARENTHESES_C(")", "\\)", "\\)"),
    BRACES_O("{", "\\{", "\\{"),
    BRACES_C("}", "\\}", "\\}"),
    BRACKET_O("[", "\\[", "\\["),
    BRACKET_C("]", "\\]", "\\]"),
    SEPARATOR(",", "\\,", "\\,"),
    NOT("!", "\\!", "\\!"),
    OR("||", "\\|\\|", "(\\||\\|\\|)"),
    AND("&&", "\\&\\&", "(\\&|\\&\\&)"),
    PLUS("+", "\\+", "(\\+)"),
    MINUS("-", "\\-[^0-9]", "(\\-)"),
    MUL("*", "\\*", "(\\*)"),
    DIV("/", "\\/", "(\\/)"),
    GREATER("<", "<", "(<)"),
    SMALLER(">", ">", "(>)"),
    SMALLER_EQUALS("<=", "<=", "(<=|<)"),
    GREATER_EQUALS(">=", ">=", "(>=|>)"),
    EQUALS("==", "==", "(==|=)"),
    NON_EQUALS("!=", "!=", "(!=|!)"),
    FUNCTION("fn", "fn", "(fn|f)"),
    LIST("list", "list", "(list|lis|li|l)"),
    TRUE("true", "true", "(true|tru|tr|t)"),
    FALSE("false", "false", "(false|fals|fal|fa|f)"),
    logic("logic", "logic", "(logic|logi|log|lo|l)"),
    STRUCT("struct", "struct", "(struct|struc|stru|str|st|s)"),
    NUMBER("number", "[0-9-]", "[0-9-.]+"),
    KEYWORD("keyword", "[a-zA-Z]", "\\w+"),
    DOT(".", "\\.", "\\."),
    END_OF_FILE("EOF", "$.^", "$.^"),
    ASSIGN("=", "=", "="),

    ;
    public final Pattern peek;
    public final Pattern capture;
    public final String alias;

    Token(String alias, String peek, String capture) {
        if (alias == null || alias.isEmpty()) {
            this.alias = this.name();
        } else {
            this.alias = alias;
        }

        this.peek = Pattern.compile("^" + peek, Pattern.MULTILINE);
        this.capture = Pattern.compile("^" + capture + "$", Pattern.MULTILINE);
    }
}
