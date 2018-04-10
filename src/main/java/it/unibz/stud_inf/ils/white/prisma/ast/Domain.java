package it.unibz.stud_inf.ils.white.prisma.ast;

import java.util.Set;
import java.util.stream.Stream;

public abstract class Domain<T> implements Standardizable<Domain<T>> {
	public abstract Stream<T> stream(Substitution substitution);
	public abstract Set<Variable> getOccurringVariables();
}
