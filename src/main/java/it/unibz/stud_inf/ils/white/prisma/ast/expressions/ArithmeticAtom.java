package it.unibz.stud_inf.ils.white.prisma.ast.expressions;

import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.terms.Arg;
import it.unibz.stud_inf.ils.white.prisma.ast.terms.IntNumberExpression;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArithmeticAtom extends Atom {
	private final Connective connective;

	public ArithmeticAtom(Connective connective, List<Arg> args) {
		super(new ConstantPredicate(connective.toString()), args);
		this.connective = connective;
	}

	@Override
	public Expression ground(Substitution substitution) {
		if (args.size() != 2) {
			throw new UnsupportedOperationException();
		}

		int left = ((IntNumberExpression) args.get(0).ground(substitution)).toInteger();
		int right = ((IntNumberExpression) args.get(1).ground(substitution)).toInteger();

		switch (connective) {
			case LT:
				return wrap(left < right);
			case GT:
				return wrap(left > right);
			case LE:
				return wrap(left <= right);
			case GE:
				return wrap(left >= right);
			case EQ:
				return wrap(left == right);
			case NE:
				return wrap(left != right);
			default:
				throw new RuntimeException("How did I get here?");
		}

	}

	@Override
	public Expression deMorgan() {
		return new ArithmeticAtom(
			connective.flip(),
			args
		);
	}

	public enum Connective {
		LT("<"),
		GT(">"),
		LE("<="),
		GE(">="),
		EQ("="),
		NE("!=");

		private final String asString;

		Connective(String asString) {
			this.asString = asString;
		}

		public static Connective fromOperator(String op) {
			for (Connective c : Connective.values()) {
				if (c.toString().equals(op)) {
					return c;
				}
			}
			return null;
		}

		public Connective flip() {
			switch (this) {
				case LT:
					return GE;
				case GT:
					return LE;
				case LE:
					return GT;
				case GE:
					return LT;
				case EQ:
					return NE;
				case NE:
					return EQ;
				default:
					throw new RuntimeException("How did I get here?");
			}
		}

		@Override
		public String toString() {
			return asString;
		}
	}

	@Override
	public Expression standardize(Map<Long, Long> map, Counter generator) {
		List<Arg> standardized = new ArrayList<>();

		for (Arg arg : args) {
			standardized.add((Arg) arg.standardize(map, generator));
		}

		return new ArithmeticAtom(
			connective,
			standardized
		);
	}
}
