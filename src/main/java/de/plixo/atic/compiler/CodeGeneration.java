package de.plixo.atic.compiler;

import java.util.ArrayList;
import java.util.List;

import static de.plixo.atic.compiler.Operation.*;

public class CodeGeneration {


    private static Bucket bucket = null;

    public static void setBucket(Bucket bucket) {
        CodeGeneration.bucket = bucket;
    }

    public static void genAdd(int dest, int a, int b) {
        combine(ADD, a, b, dest);
    }

    public static void genSub(int dest, int a, int b) {
        combine(SUBTRACT, a, b, dest);
    }

    public static void genMul(int dest, int a, int b) {
        combine(MUL, a, b, dest);
    }

    public static void genDiv(int dest, int a, int b) {
        combine(DIV, a, b, dest);
    }

    public static void genAnd(int dest, int a, int b) {
        combine(AND, a, b, dest);
    }

    public static void genOR(int dest, int a, int b) {
        combine(OR, a, b, dest);
    }

    public static void genLOAD_TRUE(int dest) {
        combine(LOAD_TRUE, dest);
    }

    public static void genLOAD_FALSE(int dest) {
        combine(LOAD_FALSE, dest);
    }

    public static void genGREATER(int dest, int a, int b) {
        combine(GREATER, a, b, dest);
    }

    public static void genSMALLER(int dest, int a, int b) {
        combine(SMALLER, a, b, dest);
    }

    public static void genSMALLER_EQUALS(int dest, int a, int b) {
        combine(SMALLER_EQUALS, a, b, dest);
    }

    public static void genGREATER_EQUALS(int dest, int a, int b) {
        combine(GREATER_EQUALS, a, b, dest);
    }

    public static void genEQUALS(int dest, int a, int b) {
        combine(EQUALS, a, b, dest);
    }

    public static void genNON_EQUALS(int dest, int a, int b) {
        combine(NON_EQUALS, a, b, dest);
    }

    public static void genConst(int a, int constant) {
        combine(LOAD_CONST, a);
        fill(constant);
    }

    private static void combine(Operation code, int a, int b) {
        combine(code, a, b, 0);
    }

    private static void combine(Operation code, int a) {
        combine(code, a, 0, 0);
    }

    private static void combine(Operation code) {
        combine(code, 0, 0, 0);
    }

    private static void combine(Operation code, int a, int b, int c) {
        final int _code = code.ordinal() << 24;
        final int _a = a << 16;
        final int _b = b << 8;
        fill(_code | _a | _b | c);
    }

    private static void fill(int code) {
        if (bucket == null) {
            throw new NullPointerException("Bucket is not set");
        }
        bucket.codes.add(code);
    }

    public static class Bucket {
        public List<Integer> codes = new ArrayList<>();
    }

    public static class LimitedBucket extends Bucket {
        int maxPersistentRegisters = 0;
    }
}
