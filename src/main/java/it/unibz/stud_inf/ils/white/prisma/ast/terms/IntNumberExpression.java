package it.unibz.stud_inf.ils.white.prisma.ast.terms;

import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class IntNumberExpression extends IntExpression {
	private final int number;

	public IntNumberExpression(int number) {
		this.number = number;
	}

	@Override
	public IntNumberExpression ground(Substitution substitution) {
		return this;
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return Collections.emptySet();
	}

	public int toInteger() {
		return number;
	}

	@Override
	public String toString() {
		return String.valueOf(number);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IntNumberExpression that = (IntNumberExpression) o;

		return number == that.number;
	}

	@Override
	public int hashCode() {
		return number;
	}

	@Override
	public IntExpression standardize(Map<Variable, Variable> map, Counter generator) {
		return this;
	}
}
