package de.plixo.atic.compiler.semantics.buckets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilationUnit {
    final Map<String, Structure> structures = new HashMap<>();
    final List<Namespace> namespaces = new ArrayList<>();


    public void addStructure(Structure structure) {
        structures.put(structure.name, structure);
    }

    public boolean containsStruct(String name) {
        return structures.containsKey(name);
    }


    public Structure getStruct(String name) {
        return structures.get(name);
    }

    public void addNamespace(Namespace namespace) {
        namespaces.add(namespace);
    }
}
