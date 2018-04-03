package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.Substitution;

import java.util.Set;

public abstract class Arg<T extends Arg> implements Standardizable<T> {
	public abstract Arg ground(Substitution substitution);

	public abstract Set<Variable> getOccurringVariables();
}
