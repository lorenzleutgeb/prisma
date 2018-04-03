package it.unibz.stud_inf.ils.white.prisma.ast;

import org.sat4j.core.VecInt;
import org.sat4j.maxsat.MinCostDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;
import java.util.List;

public class Quantifier<T> {
	private final boolean exists;
	private final Variable<T> variable;
	private final Domain<T> domain;

	public Quantifier(String name, Variable<T> variable, Domain<T> domain) {
		// TODO: Account for UTF-8.
		this.exists = "exists".equals(name.toLowerCase());
		this.variable = variable;
		this.domain = domain;
	}

	private Quantifier(boolean exists, Variable<T> variable, Domain<T> domain) {
		this.exists = exists;
		this.variable = variable;
		this.domain = domain;
	}

	public boolean isExististential() {
		return exists;
	}

	public boolean isUniversal() {
		return !exists;
	}

	public Quantifier<T> exists(Variable<T> variable, Domain<T> domain) {
		return new Quantifier<>(true, variable, domain);
	}

	public Quantifier<T> forall(Variable<T> variable, Domain<T> domain) {
		return new Quantifier<>(false, variable, domain);
	}

	public Quantifier<T> flip() {
		return new Quantifier<>(!exists, variable, domain);
	}

	private boolean dependsOn(Quantifier<T> other) {
		return other.getDomain().getOccurringVariables().contains(variable);
	}

	public Variable<T> getVariable() {
		return variable;
	}

	public Domain<T> getDomain() {
		return domain;
	}

	public BooleanConnective getConnective() {
		return exists ? BooleanConnective.OR : BooleanConnective.AND;
	}

	public Quantifier<T> switchBoth(Variable<T> variable, Domain<T> domain) {
		return new Quantifier<>(exists, variable, domain);
	}

	public static List<Quantifier> optimizeOrder(List<Quantifier> order) {
		// Go from right to left, pulling universal quantifiers as far as possible.
		MinCostDecorator solver = new MinCostDecorator(org.sat4j.pb.SolverFactory.newDefault());

		// Generate all "hard" constraints, add them as "exactly".
		//solver.addExactly()
		try {
			solver.addClause(new VecInt(new int[]{-1, 3}));
			solver.addClause(new VecInt(new int[]{-2, 3}));
			solver.addClause(new VecInt(new int[]{1, 2}));
			solver.newVar(3);
			solver.setCost(2, 2);
		} catch (ContradictionException e) {
			e.printStackTrace();
		}

		boolean isSat = false;
		try {
			while (solver.admitABetterSolution()) {
				if (!isSat) {
					isSat = true;
				}
				System.out.println(Arrays.toString(solver.model()));
				try {
					solver.discardCurrentSolution();
				} catch (ContradictionException e) {
					break;
				}
			}
			if (isSat) {
				System.out.println(Arrays.toString(solver.model()));
			}
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return exists ? "∃" : "∀";
	}
}
