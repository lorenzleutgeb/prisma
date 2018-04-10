package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.ClauseAccumulator;
import it.unibz.stud_inf.ils.white.prisma.Groundable;

import java.util.List;
import java.util.Set;

import static it.unibz.stud_inf.ils.white.prisma.ast.Atom.FALSE;
import static it.unibz.stud_inf.ils.white.prisma.ast.Atom.TRUE;
import static it.unibz.stud_inf.ils.white.prisma.ast.BooleanConnective.AND;
import static it.unibz.stud_inf.ils.white.prisma.ast.BooleanConnective.NOT;
import static it.unibz.stud_inf.ils.white.prisma.ast.BooleanConnective.OR;

public abstract class Expression implements Groundable<Expression, Expression> {
	public Expression normalize() {
		return this;
	}

	public Expression deMorgan() {
		return this;
	}

	public Expression pushQuantifiersDown() {
		return this;
	}

	public abstract Integer tseitin(ClauseAccumulator cnf);

	public static ClauseAccumulator tseitinFast(Expression expression) {
		// Assumption: Formula is ground and in NNF.
		if (expression instanceof ConnectiveExpression) {
			if (expression.isLiteral()) {
				ClauseAccumulator cnf = new ClauseAccumulator();
				Integer atom = cnf.put(((ConnectiveExpression)expression).getExpressions().get(0));
				cnf.add(atom ^ 1);
				return cnf;
			} else {
				return ((ConnectiveExpression)expression).tseitinFast();
			}
		}

		if (!(expression instanceof Atom)) {
			return null;
		}

		ClauseAccumulator cnf = new ClauseAccumulator();

		if (FALSE.equals(expression)) {
			cnf.add();
			return cnf;
		}

		Integer atom = cnf.put(expression);
		cnf.add(atom);
		return cnf;
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		throw new UnsupportedOperationException();
	}

	public Set<Set<Variable>> getRelatedVariables()  {
		throw new UnsupportedOperationException();
	}

	public boolean isLiteral() {
		return false;
	}

	public Expression compress() {
		return this;
	}

	public static Expression and(Expression... expressions) {
		return create(AND, expressions);
	}

	public static Expression and(List<Expression> expressions) {
		return create(AND, expressions);
	}

	public static Expression or(Expression... expressions) {
		return create(OR, expressions);
	}

	public static Expression not(Expression expression) {
		return create(NOT, expression);
	}

	public static Expression create(BooleanConnective connective, Expression... expressions) {
		int enforcedArity = connective.getEnforcedArity();
		if (enforcedArity == 0 && expressions.length == 1) {
			return expressions[0];
		}
		return new ConnectiveExpression(connective, expressions);
	}

	public static Expression create(BooleanConnective connective, List<Expression> expressions) {
		if (expressions.isEmpty()) {
			if (AND.equals(connective)) {
				return TRUE;
			} else if (OR.equals(connective)){
				return FALSE;
			}
			throw new IllegalArgumentException();
		}

		int enforcedArity = connective.getEnforcedArity();
		if (enforcedArity == 0 && expressions.size() == 1) {
			return expressions.get(0);
		}
		return new ConnectiveExpression(connective, expressions);
	}
}
