package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.ast.terms.Term;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ConstantTerm extends Term {
	private final String raw;

	public ConstantTerm(String raw) {
		this.raw = raw;
	}

	@Override
	public Term standardize(Map<Long, Long> map, Counter generator) {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConstantTerm that = (ConstantTerm) o;
		return Objects.equals(raw, that.raw);
	}

	@Override
	public int hashCode() {
		return Objects.hash(raw);
	}
}
