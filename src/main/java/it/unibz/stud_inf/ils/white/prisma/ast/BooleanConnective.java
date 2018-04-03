package it.unibz.stud_inf.ils.white.prisma.ast;

public enum BooleanConnective {
	THEN("→", 2),
	IFF("↔", 2),
	IF("←", 2),
	AND("∧", 0),
	OR("∨", 0),
	XOR("⊕", 2),
	ITE("?:", 3),
	NOT("¬", 1);

	private final String asString;
	private final int enforcedArity;

	BooleanConnective(String asString, int enforcedArity) {
		this.asString = asString;
		this.enforcedArity = enforcedArity;
	}

	public static BooleanConnective fromOperator(String op) {
		if (op.equals("|") || op.equals("∨")) {
			return OR;
		}
		if (op.equals("~") || op.equals("¬")) {
			return NOT;
		}
		if (op.equals("&") || op.equals("∧")) {
			return AND;
		}
		if (op.equals("->") || op.equals("→") || op.equals("⊃")) {
			return THEN;
		}
		if (op.equals("<-") || op.equals("←") || op.equals("⊂")) {
			return IF;
		}
		if (op.equals("<->") || op.equals("↔")) {
			return IFF;
		}
		if (op.equals("^") || op.equals("⊕")) {
			return XOR;
		}
		if (op.equals("?:")) {
			return ITE;
		}
		throw new RuntimeException("How did I get here?");
	}

	@Override
	public String toString() {
		return asString;
	}

	public int getEnforcedArity() {
		return enforcedArity;
	}
}
