package de.plixo.atic.compiler;

import de.plixo.atic.Helper;
import de.plixo.atic.Token;
import de.plixo.atic.compiler.semantic.type.SemanticType;
import de.plixo.lexer.AutoLexer;
import de.plixo.lexer.tokenizer.TokenRecord;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class Compiler<T extends AutoLexer.SyntaxNode<TokenRecord<Token>>> {

    /*
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
        final CompiledObject compiled = compileBinaryExpr("boolArithmeticFunc", "comparisonArithmetic",
                "boolArithmetic", expr,
                this::translateComparisonExpr, this::translateBoolExpr, (op, left, right, reg) -> {
                    testObjectType(left.object, SemanticType.SingleType.fromType(Primitives.Integer));
                    testObjectType(right.object, SemanticType.SingleType.fromType(Primitives.Integer));
                    switch (op) {
                        case "&&" -> CodeGeneration.genAnd(reg, left.finalRegister, right.finalRegister);
                        case "||" -> CodeGeneration.genOR(reg, left.finalRegister, right.finalRegister);
                        default -> throw new UnknownOperandException("Unknown code \"" + op + "\"");
                    }
                    return SemanticType.SingleType.fromType(Primitives.Integer);
                });
        if (compiled != null) {
            return compiled;
        }
        final T comparisonExpr = yieldNode(expr, "comparisonArithmetic");
        return translateComparisonExpr(comparisonExpr);
    }


    private CompiledObject translateComparisonExpr(T expr) {
        final CompiledObject compiled = compileBinaryExpr("comparisonArithmeticFunc", "arithmetic",
                "comparisonArithmetic", expr,
                this::translateArithmeticExpr, this::translateComparisonExpr, (op, left, right, reg) -> {
                    testObjectType(left.object, SemanticType.SingleType.fromType(Primitives.Integer));
                    testObjectType(right.object, SemanticType.SingleType.fromType(Primitives.Integer));
                    switch (op) {
                        case "<" -> CodeGeneration.genSMALLER(reg, left.finalRegister, right.finalRegister);
                        case ">" -> CodeGeneration.genGREATER(reg, left.finalRegister, right.finalRegister);
                        case "<=" -> CodeGeneration.genSMALLER_EQUALS(reg, left.finalRegister, right.finalRegister);
                        case ">=" -> CodeGeneration.genGREATER_EQUALS(reg, left.finalRegister, right.finalRegister);
                        case "==" -> CodeGeneration.genEQUALS(reg, left.finalRegister, right.finalRegister);
                        case "!=" -> CodeGeneration.genNON_EQUALS(reg, left.finalRegister, right.finalRegister);
                        default -> throw new UnknownOperandException("Unknown code \"" + op + "\"");
                    }
                    return SemanticType.SingleType.fromType(Primitives.Integer);
                });
        if (compiled != null) {
            return compiled;
        }
        final T comparisonExpr = yieldNode(expr, "arithmetic");
        return translateArithmeticExpr(comparisonExpr);

    }


    private CompiledObject translateArithmeticExpr(T expr) {
        final CompiledObject compiled = compileBinaryExpr("arithmeticFunc", "term",
                "arithmetic", expr,
                this::translateTermExpr, this::translateArithmeticExpr, (op, left, right, reg) -> {
                    testObjectType(left.object, right.object);
                    testObjectAsNumber(left.object);
                    switch (op) {
                        case "+" -> CodeGeneration.genAdd(reg, left.finalRegister, right.finalRegister);
                        case "-" -> CodeGeneration.genSub(reg, left.finalRegister, right.finalRegister);
                        default -> throw new UnknownOperandException("Unknown code \"" + op + "\"");
                    }
                    return SemanticType.SingleType.fromType(Primitives.Integer);
                });
        if (compiled != null) {
            return compiled;
        }
        final T comparisonExpr = yieldNode(expr, "term");
        return translateTermExpr(comparisonExpr);
    }

    private CompiledObject translateTermExpr(T expr) {
        final CompiledObject compiled = compileBinaryExpr("termFunc", "factor",
                "term", expr,
                this::translateFactorExpr, this::translateTermExpr, (op, left, right, reg) -> {
                    testObjectType(left.object, right.object);
                    testObjectAsNumber(left.object);
                    switch (op) {
                        case "*" -> CodeGeneration.genMul(reg, left.finalRegister, right.finalRegister);
                        case "/" -> CodeGeneration.genDiv(reg, left.finalRegister, right.finalRegister);
                        default -> throw new UnknownOperandException("Unknown code \"" + op + "\"");
                    }
                    return SemanticType.SingleType.fromType(Primitives.Integer);
                });
        if (compiled != null) {
            return compiled;
        }
        final T comparisonExpr = yieldNode(expr, "factor");
        return translateFactorExpr(comparisonExpr);
    }

    private CompiledObject translateFactorExpr(T expr) {
        if (testNode(expr, "number")) {
            final int finalRegister = usedRegisters++;
            final String number = leaf(yieldNode(expr, "number"));
            CodeGeneration.genConst(finalRegister, Integer.parseInt(number));
            return genObject(SemanticType.SingleType.fromType(Primitives.Integer), finalRegister);
        } else if (testNode(expr, "expression")) {
            return translateExpr(yieldNode(expr, "expression"));
        } else if (testNode(expr, "unary")) {
            final T unary = yieldNode(expr, "unary");
            if (testNode(unary, "neg_unary")) {
                final CompiledObject neg_unary = translateFactorExpr(yieldNode(yieldNode(unary, "neg_unary"), "factor"
                ));
                testObjectAsNumber(neg_unary.object);
                CodeGeneration.genUNARY(neg_unary.finalRegister, neg_unary.finalRegister);
                return genObject(neg_unary.object, neg_unary.finalRegister);
            } else {
                return translateFactorExpr(yieldNode(yieldNode(unary, "pos_unary"), "factor"));
            }
        } else if (testNode(expr, "not")) {
            final T not = yieldNode(expr, "not");
            final CompiledObject negated = translateFactorExpr(yieldNode(not, "factor"));
            testObjectType(negated.object, SemanticType.SingleType.fromType(Primitives.Integer));
            CodeGeneration.genNOT(negated.finalRegister, negated.finalRegister);
            return genObject(negated.object, negated.finalRegister);
        } else if (testNode(expr, "boolLiteral")) {
            final String boolLiteral = leaf(yieldNode(expr, "boolLiteral"));
            final int finalRegister = usedRegisters++;
            switch (boolLiteral) {
                case "true" -> CodeGeneration.genLOAD_TRUE(finalRegister);
                case "false" -> CodeGeneration.genLOAD_FALSE(finalRegister);
                default -> throw new UnknownOperandException("Unknown bool terminal \"" + boolLiteral + "\"");
            }
            return genObject(SemanticType.SingleType.fromType(Primitives.Integer), finalRegister);
        }
        throw new MissingNodeException("Unknown Factor");
    }


    private void testObjectType(SemanticType.SingleType first, SemanticType.SingleType second) {
        if (!first.equals(second)) {
            throw new IncompatibleTypeException("Conflict between " + first.type + " and " + second.type);
        }
    }

    private void testObjectAsNumber(SemanticType.SingleType first) {
        if (!first.equals(SemanticType.SingleType.fromType(Primitives.Integer)) && !first
                .equals(SemanticType.SingleType.fromType(Primitives.Decimal))) {
            throw new IncompatibleTypeException("Conflict between " + first.type + " and decimal/integer");
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
            if (node.name.equalsIgnoreCase(name)) {
                return (T) node;
            }
        }
        throw new MissingNodeException("Missing a " + name + " Node in " + in.name);
    }


    private CompiledObject compileBinaryExpr(String func, String left, String right, T expr,
                                             Function<T, CompiledObject> leftFunction,
                                             Function<T, CompiledObject> rightFunction,
                                             TConsumer<String, CompiledObject, CompiledObject, Integer, SemanticType.SingleType> consumer) {
        if (testNode(expr, func)) {
            final T leftExpr = yieldNode(expr, left);
            final String op = leaf(yieldNode(expr, func));
            final T rightExpr = yieldNode(expr, right);
            int finalRegister = usedRegisters;
            final CompiledObject leftObj = leftFunction.apply(leftExpr);
            final CompiledObject rightObj = rightFunction.apply(rightExpr);
            final SemanticType.SingleType type = consumer.accept(op, leftObj, rightObj, finalRegister);
            usedRegisters = finalRegister;
            usedRegisters++;
            return genObject(type, finalRegister);
        } else {
            return null;
        }
    }

    private String leaf(T leafNode) {
        return leafData.apply(leafNode);
    }


    private static CompiledObject genObject(SemanticType.SingleType t, int finalRegister) {
        return new CompiledObject(t, finalRegister);
    }

    @RequiredArgsConstructor
    public static class CompiledObject {
        final SemanticType.SingleType object;
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


    private interfaces TConsumer<A, B, C, D, T> {
        T accept(A a, B b, C c, D d);
    }
    */
}
