package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.Counter;
import it.unibz.stud_inf.ils.white.prisma.Substitution;
import it.unibz.stud_inf.ils.white.prisma.Util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class VariableTerm extends Term implements Variable<ConstantTerm> {
	private final long raw;

	public VariableTerm(String name) {
		this.raw = Util.toLong(name.getBytes());
	}

	public VariableTerm(long name) {
		this.raw = name;
	}

	@Override
	public ConstantTerm ground(Substitution substitution) {
		return substitution.eval(this);
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

		VariableTerm that = (VariableTerm) o;

		return raw == that.raw;
	}

	@Override
	public int hashCode() {
		return (int) (raw ^ (raw >>> 32));
	}

	@Override
	public Term standardize(Map<Long, Long> map, Counter generator) {
		Long id = map.get(this.raw);
		if (id == null) {
			throw new RuntimeException("Free variable!");
		}
		return new VariableTerm(id);
	}

	@Override
	public long toLong() {
		return raw;
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return Collections.singleton(this);
	}
}
