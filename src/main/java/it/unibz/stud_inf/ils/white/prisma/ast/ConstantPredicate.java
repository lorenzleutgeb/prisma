package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.Identifier;
import it.unibz.stud_inf.ils.white.prisma.Substitution;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;

public class ConstantPredicate extends Predicate {
	public static final ConstantPredicate TRUE = new ConstantPredicate("true");
	public static final ConstantPredicate FALSE = new ConstantPredicate("false");

	private final String raw;

	public ConstantPredicate(String raw) {
		this.raw = raw;
	}

	@Override
	public String toString() {
		return raw;
	}

	@Override
	public Predicate ground(Substitution substitution) {
		return this;
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return emptySet();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ConstantPredicate that = (ConstantPredicate) o;

		return raw.equals(that.raw);
	}

	@Override
	public int hashCode() {
		return raw.hashCode();
	}

	@Override
	public Predicate standardize(Map<Long, Long> map, Identifier generator) {
		return this;
	}
}
