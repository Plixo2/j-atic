package de.plixo.atic.compiler;

import de.plixo.atic.Helper;
import de.plixo.atic.Token;
import de.plixo.lexer.AutoLexer;
import de.plixo.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.function.Function;

@RequiredArgsConstructor
public class Compiler<T extends AutoLexer.SyntaxNode<TokenRecord<Token>>> {

    final Function<T, String> leafData;
    private int usedRegisters = 0;


    public void entry(T expr) {
        usedRegisters = 0;
        final T logic = yieldNode(expr, "logic");
        final T logicCompound = yieldNode(logic, "logicCompound");
        final T logicSpace = yieldNode(logicCompound, "logicSpace");
        final T functionLogic = yieldNode(logicSpace, "functionLogic");
        final T statement = yieldNode(functionLogic, "statement");
        final T expression = yieldNode(statement, "expression");
        final CodeGeneration.LimitedBucket bucket = new CodeGeneration.LimitedBucket();
        CodeGeneration.setBucket(bucket);
        bucket.maxPersistentRegisters = 0;
        final CompiledObject compiledObject = translateExpr(expression);
        System.out.println("Final Object at " + compiledObject.finalRegister);
        CodeGeneration.setBucket(null);
        Helper.printBucket(bucket);
    }

    private CompiledObject translateExpr(T expr) {
        return translateBoolExpr(yieldNode(expr, "boolArithmetic"));
    }

    private CompiledObject translateBoolExpr(T expr) {
        if (testNode(expr, "boolArithmeticFunc")) {
            final T leftExpr = yieldNode(expr, "comparisonArithmetic");
            final String op = leaf(yieldNode(expr, "boolArithmeticFunc"));
            final T rightExpr = yieldNode(expr, "boolArithmetic");
             int finalRegister = usedRegisters;
            final CompiledObject left = translateComparisonExpr(leftExpr);
            final CompiledObject right = translateBoolExpr(rightExpr);
            testObjectType(left.object, VariableType.fromType(Primitives.Integer));
            testObjectType(right.object, VariableType.fromType(Primitives.Integer));
            usedRegisters = finalRegister;
            usedRegisters++;
            switch (op) {
                case "&&" -> CodeGeneration.genAnd(finalRegister, left.finalRegister, right.finalRegister);
                case "||" -> CodeGeneration.genOR(finalRegister, left.finalRegister, right.finalRegister);
                default -> throw new UnknownOperandException("Unknown code \"" + op + "\"");
            }
            return genObject(VariableType.fromType(Primitives.Integer), finalRegister);
        } else {
            final T comparisonExpr = yieldNode(expr, "comparisonArithmetic");
            return translateComparisonExpr(comparisonExpr);
        }
    }

    private CompiledObject translateComparisonExpr(T expr) {
        if (testNode(expr, "comparisonArithmeticFunc")) {
            final T leftExpr = yieldNode(expr, "arithmetic");
            final String op = leaf(yieldNode(expr, "comparisonArithmeticFunc"));
            final T rightExpr = yieldNode(expr, "comparisonArithmetic");
            int finalRegister = usedRegisters;
            final CompiledObject left = translateArithmeticExpr(leftExpr);
            final CompiledObject right = translateComparisonExpr(rightExpr);
            testObjectType(left.object, VariableType.fromType(Primitives.Integer));
            testObjectType(right.object, VariableType.fromType(Primitives.Integer));
            usedRegisters = finalRegister;
            usedRegisters++;
            switch (op) {
                case "<" -> CodeGeneration.genSMALLER(finalRegister, left.finalRegister, right.finalRegister);
                case ">" -> CodeGeneration.genGREATER(finalRegister, left.finalRegister, right.finalRegister);
                case "<=" -> CodeGeneration.genSMALLER_EQUALS(finalRegister, left.finalRegister, right.finalRegister);
                case ">=" -> CodeGeneration.genGREATER_EQUALS(finalRegister, left.finalRegister, right.finalRegister);
                case "==" -> CodeGeneration.genEQUALS(finalRegister, left.finalRegister, right.finalRegister);
                case "!=" -> CodeGeneration.genNON_EQUALS(finalRegister, left.finalRegister, right.finalRegister);
                default -> throw new UnknownOperandException("Unknown code \"" + op + "\"");
            }
            return genObject(VariableType.fromType(Primitives.Integer), finalRegister);
        } else {
            final T comparisonExpr = yieldNode(expr, "arithmetic");
            return translateArithmeticExpr(comparisonExpr);
        }
    }

    private CompiledObject translateArithmeticExpr(T expr) {
        if (!testNode(expr, "arithmeticFunc")) {
            final T comparisonExpr = yieldNode(expr, "term");
            return translateTermExpr(comparisonExpr);
        }
        return null;
    }

    private CompiledObject translateTermExpr(T expr) {
        if (!testNode(expr, "termFunc")) {
            final T comparisonExpr = yieldNode(expr, "factor");
            return translateFactorExpr(comparisonExpr);
        }
        return null;
    }

    private CompiledObject translateFactorExpr(T expr) {
        if (testNode(expr, "number")) {
            final int finalRegister = usedRegisters++;
            final String number = leaf(yieldNode(expr, "number"));
            CodeGeneration.genConst(finalRegister, Integer.parseInt(number));
            return genObject(VariableType.fromType(Primitives.Integer), finalRegister);
        } else if (testNode(expr, "expression")) {
            return translateExpr(yieldNode(expr, "expression"));
        }
        return null;
    }


    private void testObjectType(VariableType first, VariableType second) {
        if (!first.equals(second)) {
            throw new IncompatibleTypeException("Conflict between " + first.type + " and " + second.type);
        }
    }

    private boolean testNode(T in, String name) {
        for (var node : in.list) {
            if (node.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private T yieldNode(T in, String name) {
        for (var node : in.list) {
            if (node.name.equals(name)) {
                return (T) node;
            }
        }
        throw new MissingNodeException("Missing a " + name + " Node in " + in.name);
    }

    private String leaf(T leafNode) {
        return leafData.apply(leafNode);
    }


    private static CompiledObject genObject(VariableType t, int finalRegister) {
        return new CompiledObject(t, finalRegister);
    }

    @RequiredArgsConstructor
    public static class CompiledObject {
        final VariableType object;
        final int finalRegister;
    }

    public static class MissingNodeException extends RuntimeException {
        public MissingNodeException(String message) {
            super(message);
        }
    }


    public static class UnknownOperandException extends RuntimeException {
        public UnknownOperandException(String message) {
            super(message);
        }
    }

    public static class IncompatibleTypeException extends RuntimeException {
        public IncompatibleTypeException(String message) {
            super(message);
        }
    }

    @RequiredArgsConstructor
    public static class VariableType {
        final UniqueType type;
        final boolean isArray;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            VariableType that = (VariableType) o;
            return isArray == that.isArray && Objects.equals(type, that.type);
        }

        public static VariableType fromType(UniqueType type) {
            return new VariableType(type, false);
        }

        @Override
        public String toString() {
            return "VariableType{" +
                    "type=" + type +
                    ", isArray=" + isArray +
                    '}';
        }
    }
}
