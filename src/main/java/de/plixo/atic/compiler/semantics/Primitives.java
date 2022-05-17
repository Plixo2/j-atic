package de.plixo.atic.compiler.semantics;

import de.plixo.atic.compiler.semantics.buckets.Structure;
import de.plixo.atic.compiler.semantics.type.SemanticType;

public class Primitives {
    public static Structure integer = new Structure("int");
    public static Structure decimal = new Structure("double");
    public static Structure void_ = new Structure("void");
    public static Structure auto = new Structure("auto");


    public static SemanticType integer_type = new SemanticType.StructType(integer);
    public static SemanticType decimal_type = new SemanticType.StructType(integer);
    public static SemanticType void_type = new SemanticType.StructType(integer);
    public static SemanticType auto_type = new SemanticType.StructType(auto);



}
