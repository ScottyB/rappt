package io.rappt.model;

@FunctionalInterface
public interface PIMVisitor {
    void visit(PIM pim);
}
