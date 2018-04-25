package it.unibz.stud_inf.ils.white.prisma.ast.expressions;

import it.unibz.stud_inf.ils.white.prisma.ast.ConstantTerm;
import it.unibz.stud_inf.ils.white.prisma.ast.Substitution;
import it.unibz.stud_inf.ils.white.prisma.ast.Variable;
import it.unibz.stud_inf.ils.white.prisma.ast.terms.Arg;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.unibz.stud_inf.ils.white.prisma.ast.expressions.EqualityAtom.Connective.EQ;

public class EqualityAtom extends Atom {
	private final Connective connective;

	public EqualityAtom(Connective connective, List<Arg> args) {
		super(new ConstantPredicate(connective.toString()), args);
		this.connective = connective;
	}

	@Override
	public Expression ground(Substitution substitution) {
		if (args.size() != 2) {
			throw new UnsupportedOperationException();
		}

		ConstantTerm left = (ConstantTerm) args.get(0).ground(substitution);
		ConstantTerm right = (ConstantTerm) args.get(1).ground(substitution);

		return left.equals(right) == connective.equals(EQ) ? TRUE : FALSE;
	}

	@Override
	public Expression deMorgan() {
		return new EqualityAtom(
			connective.flip(),
			args
		);
	}

	public enum Connective {
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
	public Expression standardize(Map<Variable, Variable> map, Counter generator) {
		List<Arg> standardized = new ArrayList<>();

		for (Arg arg : args) {
			standardized.add((Arg) arg.standardize(map, generator));
		}

		return new EqualityAtom(
			connective,
			standardized
		);
	}
}
