package it.unibz.stud_inf.ils.white.prisma.ast;

import com.google.common.collect.Sets;
import it.unibz.stud_inf.ils.white.prisma.ConjunctiveNormalForm;
import it.unibz.stud_inf.ils.white.prisma.IntIdGenerator;
import it.unibz.stud_inf.ils.white.prisma.Substitution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Atom extends Expression {
	public static final Atom TRUE = new Atom(ConstantPredicate.TRUE, Collections.emptyList());
	public static final Atom FALSE = new Atom(ConstantPredicate.FALSE, Collections.emptyList());

	private final Predicate predicate;
	protected final List<Arg> args;

	public Atom(Predicate predicate, List<Arg> args) {
		this.predicate = predicate;
		this.args = args;
	}

	public static Atom wrap(boolean constant) {
		return constant ? TRUE : FALSE;
	}

	@Override
	public Expression ground(Substitution substitution) {
		return new Atom(
			predicate.ground(substitution),
			args.stream().map(t -> t.ground(substitution)).collect(Collectors.toList())
		);
	}

	@Override
	public String toString() {
		if (args.isEmpty()) {
			return predicate.toString();
		}
		return predicate + "(" + args.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Atom atom = (Atom) o;

		if (!predicate.equals(atom.predicate)) {
			return false;
		}
		return args.equals(atom.args);
	}

	@Override
	public int hashCode() {
		int result = predicate.hashCode();
		result = 31 * result + args.hashCode();
		return result;
	}

	@Override
	public Integer tseitin(ConjunctiveNormalForm cnf) {
		return cnf.put(this);
	}

	@Override
	public Expression deMorgan() {
		return not(this);
	}

	@Override
	public Expression standardize(Map<Long, Long> map, IntIdGenerator generator) {
		List<Arg> standardized = new ArrayList<>();

		for (Arg arg : args) {
			standardized.add((Arg) arg.standardize(map, generator));
		}

		return new Atom(
			predicate.standardize(map, generator),
			standardized
		);
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		final Set<Variable> base = predicate.getOccurringVariables();
		return args
			.stream()
			.map(Arg::getOccurringVariables)
			.reduce(base, Sets::union);
	}

	@Override
	public Set<Set<Variable>> getRelatedVariables() {
		return Set.of(getOccurringVariables());
	}

	@Override
	public boolean isLiteral() {
		return true;
	}
}
