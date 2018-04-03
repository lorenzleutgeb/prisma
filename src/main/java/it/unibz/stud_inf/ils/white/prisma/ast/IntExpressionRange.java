package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.IntIdGenerator;
import it.unibz.stud_inf.ils.white.prisma.Substitution;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.collect.Sets.union;

public class IntExpressionRange extends Domain<IntNumberExpression> {
	private final IntExpression min;
	private final IntExpression max;

	public IntExpressionRange(IntExpression min, IntExpression max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Stream<IntNumberExpression> stream(Substitution substitution) {
		return IntStream.rangeClosed(
			min.ground(substitution).toInteger(),
			max.ground(substitution).toInteger()
		).mapToObj(IntNumberExpression::new);
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return union(min.getOccurringVariables(), max.getOccurringVariables());
	}

	@Override
	public String toString() {
		return "[" + min + "…" + max + "]";
	}

	@Override
	public Domain<IntNumberExpression> standardize(Map<Long, Long> map, IntIdGenerator generator) {
		return new IntExpressionRange(
			min.standardize(map, generator),
			max.standardize(map, generator)
		);
	}
}
