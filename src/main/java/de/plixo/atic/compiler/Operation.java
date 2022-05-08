package de.plixo.atic.compiler;

import java.util.HashMap;
import java.util.Map;

public enum Operation {
    ADD,
    SUBTRACT,
    MUL,
    DIV,
    OR,
    AND,
    GREATER,
    SMALLER,
    SMALLER_EQUALS,
    GREATER_EQUALS,
    EQUALS,
    NON_EQUALS,
    LOAD_TRUE,
    LOAD_FALSE,
    LOAD_CONST;

    public static Map<Integer, Operation> indexedMap = new HashMap<>();

    static {
        for (Operation value : Operation.values()) {
            indexedMap.put(value.ordinal(), value);
        }
    }
}
