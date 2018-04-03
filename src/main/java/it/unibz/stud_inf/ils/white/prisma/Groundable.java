package it.unibz.stud_inf.ils.white.prisma;

import it.unibz.stud_inf.ils.white.prisma.ast.Standardizable;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;

import java.util.Set;

public interface Groundable<T,S> extends Standardizable<S> {
	T ground(Substitution substitution);

	default T ground() {
		return ground(new Substitution());
	}

	Set<Variable> getOccurringVariables();
}
