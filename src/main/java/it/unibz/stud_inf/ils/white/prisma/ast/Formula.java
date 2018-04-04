package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.ConjunctiveNormalForm;
import it.unibz.stud_inf.ils.white.prisma.Groundable;
import it.unibz.stud_inf.ils.white.prisma.IntIdGenerator;
import it.unibz.stud_inf.ils.white.prisma.Substitution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.unibz.stud_inf.ils.white.prisma.ast.Atom.TRUE;
import static it.unibz.stud_inf.ils.white.prisma.ast.BooleanConnective.AND;
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

	public void add(Expression expression) {
		expressions.add(expression);
	}

	public void add(Formula formula) {
		expressions.addAll(formula.expressions);
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
		if (expressions.isEmpty()) {
			return new Formula(singletonList(TRUE));
		}

		List<Expression> expressions = standardize().expressions;

		if (expressions.size() == 1) {
			return new Formula(
				singletonList(expressions.get(0))
			);
		}

		return new Formula(singletonList(
			expressions
				.stream()
				.reduce(TRUE, Expression::and)
		));
	}

	public Formula ground(Substitution substitution) {
		if (this.expressions.isEmpty()) {
			return new Formula(singletonList(TRUE));
		}

		List<Expression> expressions = standardize().expressions;

		if (expressions.size() == 1) {
			return new Formula(
				singletonList(expressions.get(0).ground(substitution))
			);
		}

		return new Formula(singletonList(new ConnectiveExpression(
			AND,
			expressions.stream().map(e -> e.ground(substitution)).collect(toList())
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

	public ConjunctiveNormalForm tseitin() {
		Expression root = expressions.get(0);

		// Are we already in CNF by chance?
		ConjunctiveNormalForm cnf = Expression.tseitinFast(root);

		if (cnf == null) {
			ConnectiveExpression connectiveExpression = (ConnectiveExpression) root;

			final ConjunctiveNormalForm fcnf = new ConjunctiveNormalForm();

			final var literals = connectiveExpression.getExpressions().stream().mapToInt(fcnf::computeIfAbsent);

			if (connectiveExpression.getConnective().equals(AND)) {
				literals.forEach(fcnf::add);
			} else {
				fcnf.add(literals.toArray());
			}

			cnf = fcnf;
		}

		// Ensure that "true" is true in every model.
		Integer t = cnf.get(Atom.TRUE);
		if (t != null) {
			cnf.add(t);
		}

		// Ensure that "false" is false in every model.
		Integer f = cnf.get(Atom.FALSE);
		if (f != null) {
			cnf.add(-f);
		}

		return cnf;
	}

	public Formula pushQuantifiersDown() {
		return new Formula(
			expressions.stream()
				.map(Expression::pushQuantifiersDown)
				.collect(toList())
		);
	}

	@Override
	public Formula standardize(Map<Long, Long> map, IntIdGenerator generator) {
		return new Formula(
			expressions
				.stream().map(e -> e.standardize(new HashMap<>(map), generator))
				.collect(toList())
		);
	}

	public Formula standardize() {
		return standardize(new HashMap<>(), new IntIdGenerator());
	}

	public ConjunctiveNormalForm toConjunctiveNormalForm() {
		return normalize().standardize().pushQuantifiersDown().ground().tseitin();
	}
}
