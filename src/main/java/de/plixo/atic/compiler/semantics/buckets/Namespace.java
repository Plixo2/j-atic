package de.plixo.atic.compiler.semantics.buckets;

import de.plio.nightlist.NightList;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class Namespace {
    public final String name;
    public final NightList<FunctionStruct> functions = NightList.create();

}
