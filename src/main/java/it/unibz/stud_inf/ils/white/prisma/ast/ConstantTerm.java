package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.IntIdGenerator;
import it.unibz.stud_inf.ils.white.prisma.Substitution;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ConstantTerm extends Term {
	private final String raw;

	public ConstantTerm(String raw) {
		this.raw = raw;
	}

	@Override
	public Term standardize(Map<Long, Long> map, IntIdGenerator generator) {
		return this;
	}

	@Override
	public ConstantTerm ground(Substitution substitution) {
		return this;
	}

	@Override
	public String toString() {
		return raw;
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return Collections.emptySet();
	}
}
