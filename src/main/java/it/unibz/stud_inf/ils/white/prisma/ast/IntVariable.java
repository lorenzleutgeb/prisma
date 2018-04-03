package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.IntIdGenerator;
import it.unibz.stud_inf.ils.white.prisma.Substitution;
import it.unibz.stud_inf.ils.white.prisma.Util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class IntVariable extends IntExpression implements Variable<IntNumberExpression> {
	private final long raw;

	public IntVariable(String name) {
		this.raw = Util.toLong(name.getBytes());
	}

	public IntVariable(long name) {
		this.raw = name;
	}

	@Override
	public IntNumberExpression ground(Substitution substitution) {
		return substitution.eval(this);
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return Collections.singleton(this);
	}

	@Override
	public String toString() {
		return "#" + raw;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IntVariable that = (IntVariable) o;

		return raw == that.raw;
	}

	@Override
	public int hashCode() {
		return (int) (raw ^ (raw >>> 32));
	}

	@Override
	public IntVariable standardize(Map<Long, Long> map, IntIdGenerator generator) {
		Long id = map.get(this.raw);
		if (id == null) {
			throw new RuntimeException("Free variable!");
		}
		return new IntVariable(id);
	}

	@Override
	public long toLong() {
		return raw;
	}
}
