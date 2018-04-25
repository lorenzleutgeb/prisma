package it.unibz.stud_inf.ils.white.prisma.ast.expressions;

import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PredicateVariable extends Predicate implements Variable<Predicate> {
	private final String raw;

	public PredicateVariable(String name) {
		this.raw = name;
	}

	@Override
	public Predicate ground(Substitution substitution) {
		return substitution.eval(this);
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return Collections.singleton(this);
	}

	@Override
	public String toString() {
		return "@" + raw;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PredicateVariable that = (PredicateVariable) o;
		return Objects.equals(raw, that.raw);
	}

	@Override
	public int hashCode() {

		return Objects.hash(raw);
	}

	@Override
	public Predicate standardize(Map<Variable, Variable> map, Counter generator) {
		return (PredicateVariable) map.get(this);
	}
}
