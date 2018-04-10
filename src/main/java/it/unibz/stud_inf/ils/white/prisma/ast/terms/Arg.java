package it.unibz.stud_inf.ils.white.prisma.ast.terms;

import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Standardizable;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;

import java.util.Set;

public abstract class Arg<T extends Arg> implements Standardizable<T> {
	public abstract Arg ground(Substitution substitution);

	public abstract Set<Variable> getOccurringVariables();
}
