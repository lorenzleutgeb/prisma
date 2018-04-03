package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.ConjunctiveNormalForm;
import it.unibz.stud_inf.ils.white.prisma.Groundable;

import java.util.Set;

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

	public abstract Integer tseitin(ConjunctiveNormalForm cnf);

	public static ConjunctiveNormalForm tseitinFast(Expression expression) {
		// Assumption: Formula is ground and in NNF.
		if (expression instanceof ConnectiveExpression) {
			if (expression.isLiteral()) {
				ConjunctiveNormalForm cnf = new ConjunctiveNormalForm();
				Integer atom = cnf.put(((ConnectiveExpression)expression).getExpressions().get(0));
				cnf.add(-atom);
				return cnf;
			} else {
				return ((ConnectiveExpression)expression).tseitinFast();
			}
		}

		if (!(expression instanceof Atom)) {
			return null;
		}

		ConjunctiveNormalForm cnf = new ConjunctiveNormalForm();
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
}
