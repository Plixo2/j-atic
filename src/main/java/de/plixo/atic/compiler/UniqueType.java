package de.plixo.atic.compiler;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

public class UniqueType {
    final String uniqueName;
    final String[] alias;
    public UniqueType(String uniqueName , String... alias) {
        this.uniqueName = uniqueName;
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UniqueType uniqueType = (UniqueType) o;
        return compare(uniqueType.uniqueName);
    }

    public boolean compare(String name) {
        return name.equalsIgnoreCase(uniqueName) || Arrays.asList(alias).contains(name);
    }

    @Override
    public String toString() {
        return "UniqueObject{" +
                "uniqueName='" + uniqueName + '\'' +
                '}';
    }
}
