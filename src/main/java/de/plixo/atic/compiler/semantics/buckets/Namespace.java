package de.plixo.atic.compiler.semantics.buckets;

import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class Namespace {
    public final String name;
    public final List<FunctionStruct> functions = new ArrayList<>();

}
