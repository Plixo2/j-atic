package de.plixo.atic.compiler.semantic;

import de.plixo.atic.Token;
import de.plixo.atic.compiler.Primitives;
import de.plixo.atic.compiler.semantic.statement.interfaces.Namespace;
import de.plixo.atic.compiler.semantic.statement.interfaces.Structure;
import de.plixo.atic.compiler.semantic.type.SemanticType;
import de.plixo.lexer.AutoLexer;
import de.plixo.lexer.tokenizer.TokenRecord;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.plixo.atic.compiler.semantic.SemanticHelper.*;

public class SemanticProcessor {

    final static List<Structure> structures = new ArrayList<>();
    final static Map<String, Structure> strutMap = new HashMap<>();
    final static List<Namespace> namespaces = new ArrayList<>();

    public static void convert(AutoLexer.SyntaxNode<TokenRecord<Token>> in) {
        walk("struct", "top", in, node -> structures.add(genStruct(node)));
        structures.add(Primitives.integer);
        structures.add(Primitives.decimal);
        structures.add(Primitives._void);

        structures.forEach(structure -> {
            if (strutMap.containsKey(structure.name)) {
                throw new NameCollisionException("Struct name \"" + structure.name + "\" has multiple entries");
            }
            strutMap.put(structure.name, structure);
        });
        walk("struct", "top", in, SemanticProcessor::makeMembers);

        System.out.println("Generated:");
        structures.forEach(System.out::println);

        walk("logic", "top", in, node -> namespaces.add(genNamespace(node)));

        System.out.println("Namespaces:");

        namespaces.forEach(namespace -> {
            System.out.println(namespace.name);
            namespace.functions.forEach(func -> System.out.println(func.name + ": " + func.input.toString() + " -> " + func.output));
        });

    }

    private static Namespace genNamespace(AutoLexer.SyntaxNode<TokenRecord<Token>> logic) {
        final String name = getLeafData(yieldNode(logic, "ID"));
        final Namespace namespace = new Namespace(name);
        walk("logicSpace", "logicCompound", logic, node -> namespace.functions.add(genFunctionStruct(node)));
        return namespace;
    }

    private static Namespace.FunctionStruct genFunctionStruct(AutoLexer.SyntaxNode<TokenRecord<Token>> logicSpace) {
        final String name = getLeafData(yieldNode(logicSpace, "ID"));
        final SemanticType type = genSemanticType(yieldNode(logicSpace, "type"));
        final Namespace.FunctionStruct functionStruct = new Namespace.FunctionStruct(name, type);
        walk("inputTerm", "inputList", logicSpace, node -> {
            final SemanticType inType = genSemanticType(yieldNode(node, "type"));
            final String inName = getLeafData(yieldNode(node, "ID"));
            if(functionStruct.input.containsKey(inName)) {
                throw new NameCollisionException("Function input \"" + inName + "\" has multiple entries in \"" + name + "\"");
            }
            functionStruct.input.put(inName, inType);
        });

        return functionStruct;
    }

    private static void makeMembers(AutoLexer.SyntaxNode<TokenRecord<Token>> struct) {
        val id = yieldNode(struct, "ID");
        final String name = getLeafData(id);
        final Structure structure = strutMap.get(name);
        assert structure != null;
        walk("declaration", "declarationCompound", struct, node -> {
            val typeOfVar = yieldNode(node, "Type");
            final String nameOfVar = getLeafData(yieldNode(node, "ID"));
            if (strutMap.containsKey(nameOfVar)) {
                throw new NameCollisionException("Variable name \"" + structure.name + "\" has multiple entries in " +
                        "\"" + name + "\"");
            }
            final SemanticType semanticType = genSemanticType(typeOfVar);
            structure.members.put(nameOfVar, semanticType);
        });
    }

    private static SemanticType genSemanticType(AutoLexer.SyntaxNode<TokenRecord<Token>> type) {
        final String typeOfVar = getLeafData(yieldNode(type, "ID"));
        if (!strutMap.containsKey(typeOfVar)) {
            throw new UnknownTypeException("Unknown Type " + typeOfVar);
        }
        return new SemanticType.StructType(strutMap.get(typeOfVar));
    }

    private static Structure genStruct(AutoLexer.SyntaxNode<TokenRecord<Token>> struct) {
        val id = yieldNode(struct, "ID");
        final String name = getLeafData(id);
        return new Structure(name);
    }

    private static class NameCollisionException extends RuntimeException {
        public NameCollisionException(String message) {
            super(message);
        }
    }

    private static class UnknownTypeException extends RuntimeException {
        public UnknownTypeException(String message) {
            super(message);
        }
    }

    /*
    private static CompilationUnit unit = null;


    public static void setUnit(CompilationUnit unit) {
        SemanticProcessor.unit = unit;
    }

    public static void walk(String action, String list, AutoLexer.SyntaxNode<TokenRecord<Token>> node,
                            Consumer<AutoLexer.SyntaxNode<TokenRecord<Token>>> consumer) {
        if (testNode(node, action)) {
            consumer.accept(yieldNode(node, action));
        }
        if (testNode(node, list)) {
            walk(action, list, yieldNode(node, list), consumer);
        }
    }

    public static void main(AutoLexer.SyntaxNode<TokenRecord<Token>> in) {

        unit.structures.add(new BuildInInt());
        unit.structures.add(new BuildInDouble());
        final Map<AutoLexer.SyntaxNode<TokenRecord<Token>>, Structure> foundStructs = new HashMap<>();
        walk("struct", "top", in, struct -> {
            final Structure structure = buildStructure(struct);
            unit.structures.add(structure);
            foundStructs.put(struct,structure);
        });
        checkNameCollisions();
        processTypes(foundStructs);
    }

    private static void processTypes(Map<AutoLexer.SyntaxNode<TokenRecord<Token>>,Structure> structs) {
        structs.forEach((node,struct) -> {
            
        });
    }

    private static void checkNameCollisions() {
        final Set<String> set = new HashSet<>();
        unit.structures.forEach(structure -> {
            if (set.contains(structure.name)) {
                System.err.println("Duplicated Name \"" + structure.name + "\"");
            }
            set.add(structure.name);
        });
    }


//    private static void buildStaticFunctions(Map<String,List<AutoLexer.SyntaxNode<TokenRecord<Token>>>> namespaces) {
//        final List<Namespace> namespaceList = new ArrayList<>();
//        namespaces.forEach((k,v) -> {
//            Namespace namespace = new Namespace(k);
//
//        });
//    }
//
//    public static void buildFunction() {
//
//    }

    private static Structure buildStructure(AutoLexer.SyntaxNode<TokenRecord<Token>> structs) {

        //process into Structure
        //check existence
        //check case
        val id = yieldNode(structs, "ID");
        return new Structure(leafData(id));

    }

    private static boolean testNode(AutoLexer.SyntaxNode<TokenRecord<Token>> in, String name) {
        for (var node : in.list) {
            if (node.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }


    private static String leafData(AutoLexer.SyntaxNode<TokenRecord<Token>> in) {
        final AutoLexer.SyntaxNode<TokenRecord<Token>> node = in.list.get(0);
        AutoLexer<TokenRecord<Token>>.LeafNode leafNode =
                (AutoLexer<TokenRecord<Token>>.LeafNode) node;
        return leafNode.data.data;
    }

    private static AutoLexer.SyntaxNode<TokenRecord<Token>> yieldNode(AutoLexer.SyntaxNode<TokenRecord<Token>> in,
                                                                      String name) {
        for (var node : in.list) {
            if (node.name.equalsIgnoreCase(name)) {
                return node;
            }
        }
        throw new NullPointerException("Missing a " + name + " Node in " + in.name);
        //return null;
        // throw new MissingNodeException("Missing a " + name + " Node in " + in.name);
    }

    @RequiredArgsConstructor
    public static class CompilationUnit {
        final List<Structure> structures = new ArrayList<>();
        final List<Namespace> namespaces = new ArrayList<>();
    }

    @RequiredArgsConstructor
    public static class StatementBucket {
        final List<SemanticStatement> statements = new ArrayList<>();
    }
    */
}
