package de.plixo.atic.compiler.semantic.type;

import de.plixo.atic.compiler.semantic.statement.interfaces.Structure;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

public abstract class SemanticType {


    @RequiredArgsConstructor
    public static class ArrayType extends SemanticType {
        final SemanticType arrayObject;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ArrayType arrayType = (ArrayType) o;
            return Objects.equals(arrayObject, arrayType.arrayObject);
        }

        @Override
        public String toString() {
            return "Array of " + arrayObject.toString();
        }

        public static ArrayType gen(SemanticType type) {
            return new ArrayType(type);
        }
    }

    @RequiredArgsConstructor
    public static class FunctionType extends SemanticType {
        final SemanticType output;
        final List<SemanticType> input;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            FunctionType that = (FunctionType) o;
            return Objects.equals(output, that.output) && Objects.equals(input, that.input);
        }

        @Override
        public String toString() {
            return "Function<" + input.toString() +"> -> " + output.toString();
        }
    }

    @RequiredArgsConstructor
    public static class StructType extends SemanticType {
        final Structure structure;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            StructType that = (StructType) o;
            return Objects.equals(structure, that.structure);
        }

        @Override
        public String toString() {
            return "Struct " + structure.name;
        }

        public static StructType gen(Structure struct) {
            return new StructType(struct);
        }
    }
}