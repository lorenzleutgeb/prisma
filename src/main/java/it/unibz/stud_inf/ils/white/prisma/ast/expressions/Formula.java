package it.unibz.stud_inf.ils.white.prisma.ast.expressions;

import it.unibz.stud_inf.ils.white.prisma.ast.Groundable;
import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;
import it.unibz.stud_inf.ils.white.prisma.cnf.ClauseAccumulator;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.BooleanConnective.AND;
import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.Expression.and;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class Formula implements Iterable<Expression>, Groundable<Formula, Formula> {
	private final List<Expression> expressions;

	public Formula(List<Expression> expressions) {
		this.expressions = expressions;
	}

	public Formula() {
		this(new ArrayList<>());
	}

	public Formula add(Expression expression) {
		List<Expression> copy = new ArrayList<>(expressions.size() + 1);
		copy.addAll(expressions);
		copy.add(expression);
		return new Formula(copy);
	}

	public Formula add(Formula formula) {
		List<Expression> copy = new ArrayList<>(expressions.size() + formula.expressions.size());
		copy.addAll(expressions);
		copy.addAll(formula.expressions);
		return new Formula(copy);
	}

	@Override
	public String toString() {
		return expressions
			.stream()
			.map(Expression::toString)
			.collect(joining("\n"));
	}

	@Override
	public Iterator<Expression> iterator() {
		return expressions.iterator();
	}

	public Formula toSingleExpression() {
		return new Formula(singletonList(and(standardize().expressions)));
	}

	public Formula ground(Substitution substitution) {
		return new Formula(singletonList(and(
			standardize().expressions.stream()
				.map(e -> e.ground(substitution)).collect(toList())
		).compress()));
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		throw new UnsupportedOperationException();
	}

	public Formula normalize() {
		return new Formula(
			expressions
				.stream()
				.map(Expression::normalize)
				.collect(toList())
		);
	}

	public ClauseAccumulator tseitin() {
		Expression root = expressions.get(0);

		// Are we already in CNF by chance?
		ClauseAccumulator acc = Expression.tseitinFast(root);
		if (acc != null) {
			return acc;
		}

		ConnectiveExpression connectiveExpression = (ConnectiveExpression) root;

		final ClauseAccumulator facc = new ClauseAccumulator();

		final IntStream literals = connectiveExpression.getExpressions().stream().mapToInt(e -> e.tseitin(facc));

		if (connectiveExpression.getConnective().equals(AND)) {
			literals.forEach(facc::add);
		} else {
			facc.add(literals.toArray());
		}

		return facc;
	}

	public Formula pushQuantifiersDown() {
		return new Formula(
			expressions.stream()
				.map(Expression::pushQuantifiersDown)
				.collect(toList())
		);
	}

	@Override
	public Formula standardize(Map<Long, Long> map, Counter generator) {
		return new Formula(
			expressions
				.stream().map(e -> e.standardize(new HashMap<>(map), generator))
				.collect(toList())
		);
	}

	public Formula standardize() {
		return standardize(new HashMap<>(), new Counter());
	}

	public ClauseAccumulator accumulate() {
		return normalize()
			.standardize()
			.pushQuantifiersDown()
			.ground()
			.tseitin();
	}
}
