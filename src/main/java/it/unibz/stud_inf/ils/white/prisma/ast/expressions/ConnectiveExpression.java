package it.unibz.stud_inf.ils.white.prisma.ast.expressions;

import it.unibz.stud_inf.ils.white.prisma.ast.Groundable;
import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;
import it.unibz.stud_inf.ils.white.prisma.cnf.ClauseAccumulator;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;
import org.sat4j.core.VecInt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.Atom.FALSE;
import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.Atom.TRUE;
import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.BooleanConnective.AND;
import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.BooleanConnective.ITE;
import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.BooleanConnective.NOT;
import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.BooleanConnective.OR;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.sat4j.core.LiteralsUtils.neg;

public class ConnectiveExpression extends Expression {
	private final BooleanConnective connective;
	private final List<Expression> expressions;

	public ConnectiveExpression(BooleanConnective connective, List<Expression> expressions) {
		int enforcedArity = connective.getEnforcedArity();
		if (enforcedArity != 0 && expressions.size() != enforcedArity) {
			throw new IllegalArgumentException("Number of expressions does not match enforced arity of connective!");
		}
		if (enforcedArity == 0 && expressions.size() < 2) {
			throw new IllegalArgumentException("Trying to instantiate connective with just one expression where there is no enforced arity!");
		}
		if (expressions.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException();
		}

		if (is(OR) || is(AND)) {
			expressions = new ArrayList<>(new HashSet<>(expressions));
		}

		this.expressions = unmodifiableList(expressions);
		this.connective = connective;
		assertArity();
	}

	public ConnectiveExpression(BooleanConnective connective, Expression... expressions) {
		this(connective, asList(expressions));
	}

	public BooleanConnective getConnective() {
		return connective;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}

	@Override
	public Expression compress() {
		List<Expression> compressed = new ArrayList<>(expressions.size());

		for (Expression e : expressions) {
			if (e.isLiteral()) {
				compressed.add(e);
				continue;
			}
			if (!(e instanceof ConnectiveExpression)) {
				return null;
			}
			ConnectiveExpression ce = (ConnectiveExpression) e;
			if (connective.equals(ce.connective)) {
				Expression cee = ce.compress();
				if (cee instanceof ConnectiveExpression) {
					compressed.addAll(((ConnectiveExpression)cee).expressions);
				} else {
					compressed.add(cee);
				}
			} else {
				compressed.add(ce.compress());
			}
		}
		return swap(compressed);
	}

	public Expression swap(List<Expression> expressions) {
		if (expressions.size() == 1 && !is(NOT)) {
			return expressions.get(0);
		}
		return new ConnectiveExpression(connective, expressions);
	}

	private void assertArity() {
		int enforcedArity = connective.getEnforcedArity();
		if (enforcedArity == 0) {
			return;
		}
		if (enforcedArity != expressions.size()) {
			throw new IllegalStateException("Wrong arity!");
		}
	}

	public boolean is(BooleanConnective connective) {
		return connective.equals(this.connective);
	}

	public boolean isClause() {
		return is(OR) && stream().allMatch(Expression::isLiteral);
	}

	@Override
	public boolean isLiteral() {
		return is(NOT) && (expressions.get(0) instanceof Atom);
	}

	private Stream<Expression> stream() {
		return expressions.stream();
	}

	private Expression map(Function<? super Expression, ? extends Expression> f) {
		return swap(stream().map(f).collect(toList()));
	}

	@Override
	public Expression pushQuantifiersDown() {
		return new ConnectiveExpression(
			connective,
			stream()
				.map(Expression::pushQuantifiersDown)
				.collect(toList())
		);
	}

	@Override
	public Expression normalize() {
		int enforcedArity = connective.getEnforcedArity();
		if (enforcedArity != 0 && enforcedArity != expressions.size()) {
			throw new UnsupportedOperationException();
		}

		// From here on we assume enforcedArity == expressions.size()

		// Push down negation (NNF) and eliminate double negation.
		if (is(NOT)) {
			final Expression subExpression = expressions.get(0);
			if (subExpression instanceof Atom) {
				return not(subExpression.normalize());
			}
			if ((subExpression instanceof ConnectiveExpression)) {
				ConnectiveExpression subMultary = (ConnectiveExpression) subExpression;
				if (subMultary.is(NOT)) {
					return subMultary.expressions.get(0).normalize();
				}
			}
			return subExpression.normalize().deMorgan();
		}

		// Translate ITE into an equivalent conjunction.
		if (is(ITE)) {
			final Expression condition = expressions.get(0);
			final Expression truthy = expressions.get(1);
			final Expression falsy = expressions.get(2);
			return and(
				or(not(condition).normalize(), truthy.normalize()),
				or(condition.normalize(), falsy.normalize())
			);
		}

		final Expression left = expressions.get(0);
		final Expression right = expressions.get(1);

		switch (this.connective) {
			case THEN:
				return or(not(left).normalize(), right.normalize());
			case IFF:
				return and(
					or(not(left).normalize(), right.normalize()),
					or(not(right).normalize(), left.normalize())
				);
			case IF:
				return or(left.normalize(), not(right).normalize());
			case XOR:
				return and(
					or(left, right).normalize(),
					or(not(left), not(right)).normalize()
				);
			default:
				return swap(asList(left.normalize(), right.normalize()));
		}
	}

	@Override
	public Expression ground(Substitution substitution) {
		Stream<Expression> groundExpressions = stream().map(e -> e.ground(substitution));

		if (is(AND) || is(OR)) {
			groundExpressions = groundExpressions.filter(e -> {
				return !(e.equals(getIdentity()));
			});
		}

		return swap(groundExpressions.collect(toList()));
	}

	public Expression getIdentity() {
		if (!is(AND) && !is(OR)) {
			throw new UnsupportedOperationException("Identity is only defined for AND and OR.");
		}
		return is(AND) ? TRUE : FALSE;
	}

	@Override
	public Integer tseitin(ClauseAccumulator cnf) {
		//if (!is(NOT) && !is(AND) && !is(OR)) {
		//	throw new UnsupportedOperationException("Tseitin translation is only defined for AND, OR and NOT.");
		//}

		if (is(NOT)) {
			// NOT is easy since it delegates to the subExpression.
			final Expression subExpression = expressions.get(0);

			//if (!(subExpression instanceof Atom)) {
			//	throw new IllegalStateException("Formula must be in negation normal form.");
			//}

			return neg(subExpression.tseitin(cnf));
		}

		Integer self = cnf.get(this);

		if (self != null) {
			return self;
		}

		self = cnf.put(this);

		List<Integer> literals = stream().map(e -> e.tseitin(cnf)).collect(toList());

		// We'll generate an implication downward.
		final int notSelf = neg(self);

		// These three lines depend on the implementation of ArrayList.
		if (is(OR)) {
			final VecInt clause = new VecInt(literals.size() + 1);
			clause.push(notSelf);
			for (Integer l : literals) {
				clause.unsafePush(l);
			}
			cnf.add(clause);
		}

		if (is(AND)) {
			for (Integer l : literals) {
				cnf.add(notSelf, l);
			}
		}

		return self;
	}

	@Override
	public Expression deMorgan() {
		if (is(NOT)) {
			return expressions.get(0);
		}

		return new ConnectiveExpression(
			is(AND) ? OR : AND,
			stream()
				.map(Expression::deMorgan)
				.collect(toList())
		);
	}

	public ClauseAccumulator tseitinFast() {
		// If this is a disjunction, continue under
		// the assumption that this is a clause.
		if (is(OR)) {
			final ClauseAccumulator cnf = new ClauseAccumulator();
			int[] cnfClause = new int[expressions.size()];
			for (int i = 0; i < expressions.size(); i++) {
				final Expression it = expressions.get(i);

				if (!it.isLiteral()) {
					return null;
				}
				if (it instanceof Atom) {
					int variable = cnf.computeIfAbsent(it);
					cnfClause[i] = variable;
				} else {
					int variable = cnf.computeIfAbsent(((ConnectiveExpression) it).expressions.get(0));
					cnfClause[i] = variable ^ 1;
				}
			}
			cnf.add(cnfClause);
			return cnf;
		}

		if (!is(AND)) {
			return null;
		}

		// Continue under the assumption that this is a conjunction of clauses.
		ClauseAccumulator cnf = new ClauseAccumulator();
		for (Expression e : expressions) {
			if (e.isLiteral()) {
				if (e instanceof Atom) {
					cnf.add(cnf.computeIfAbsent(e));
				} else {
					cnf.add(cnf.computeIfAbsent(((ConnectiveExpression) e).expressions.get(0)) ^ 1);
				}
				continue;
			}

			if ((e instanceof ConnectiveExpression)) {
				ConnectiveExpression clause = (ConnectiveExpression) e;
				int[] cnfClause = new int[clause.expressions.size()];
				for (int i = 0; i < clause.expressions.size(); i++) {
					Expression it = clause.expressions.get(i);

					if (!it.isLiteral()) {
						return null;
					}

					if (it instanceof Atom) {
						int variable = cnf.computeIfAbsent(it);
						cnfClause[i] = variable;
					} else {
						int variable = cnf.computeIfAbsent(((ConnectiveExpression) it).expressions.get(0));
						cnfClause[i] = variable ^ 1;
					}
				}
				cnf.add(cnfClause);
			} else {
				return null;
			}
		}
		return cnf;
	}

	@Override
	public Expression standardize(Map<Variable, Variable> map, Counter generator) {
		return map(t -> t.standardize(map, generator));
	}

	@Override
	public String toString() {
		int enforcedArity = connective.getEnforcedArity();

		if (enforcedArity == 0) {
			return stream()
				.map(Object::toString)
				.collect(joining(" " + connective + " ", "(", ")"));
		}

		if (is(ITE)) {
			return "(" + expressions.get(0) + " ? " + expressions.get(1) + " : " + expressions.get(2) + ")";
		}

		if (is(NOT)) {
			return "¬" + expressions.get(0);
		}

		if (enforcedArity == 2) {
			return "(" + expressions.get(0) + " " + connective + " " + expressions.get(1) + ")";
		}

		throw new RuntimeException("How did I get here?");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ConnectiveExpression that = (ConnectiveExpression) o;
		return connective == that.connective && Objects.equals(expressions, that.expressions);
	}

	@Override
	public int hashCode() {
		return hash(connective, expressions);
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		final Set<Variable> base = new HashSet<>();
		expressions
			.stream()
			.map(Groundable::getOccurringVariables)
			.forEach(base::addAll);
		return base;
	}

	@Override
	public Set<Set<Variable>> getRelatedVariables() {
		final Set<Set<Variable>> base = new HashSet<>();
		expressions
			.stream()
			.map(Expression::getRelatedVariables)
			.forEach(base::addAll);
		return base;
	}
}
