package it.unibz.stud_inf.ils.white.prisma.ast.expressions;

import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;
import it.unibz.stud_inf.ils.white.prisma.ast.terms.IntExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.terms.IntNumberExpression;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;

public class IntUnaryConnectiveExpression extends IntExpression {
	private final IntUnaryConnectiveExpression.Connective connective;
	private final IntExpression subExpression;

	public IntUnaryConnectiveExpression(Connective connective, IntExpression subExpression) {
		this.connective = connective;
		this.subExpression = subExpression;
	}

	@Override
	public IntExpression standardize(Map<Long, Long> map, Counter generator) {
		return new IntUnaryConnectiveExpression(connective, subExpression.standardize(map, generator));
	}

	public enum Connective {
		ABS("|"),
		NEG("-");

		private final String asString;

		Connective(String asString) {
			this.asString = asString;
		}

		@Override
		public String toString() {
			return asString;
		}
	}

	@Override
	public IntNumberExpression ground(Substitution substitution) {
		int x = subExpression.ground(substitution).toInteger();

		switch (connective) {
			case ABS:
				return new IntNumberExpression(abs(x));
			case NEG:
				return new IntNumberExpression(-x);
			default:
				throw new UnsupportedOperationException();
		}
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		return subExpression.getOccurringVariables();
	}
}
