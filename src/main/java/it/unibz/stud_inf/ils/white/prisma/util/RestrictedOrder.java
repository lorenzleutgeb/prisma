package it.unibz.stud_inf.ils.white.prisma.util;

import org.sat4j.minisat.core.ILits;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.IPhaseSelectionStrategy;

import java.io.PrintWriter;
import java.util.Set;

import static java.lang.Math.abs;
import static org.sat4j.core.LiteralsUtils.toDimacs;
import static org.sat4j.core.LiteralsUtils.toInternal;

public class RestrictedOrder implements IOrder {
	private final IOrder delegate;
	private final Set<Integer> guessable;

	public RestrictedOrder(IOrder delegate, Set<Integer>guessable) {
		this.delegate = delegate;
		this.guessable = guessable;
	}

	@Override
	public void setLits(ILits lits) {
		delegate.setLits(lits);
	}

	@Override
	public int select() {
		while (true) {
			int selection = delegate.select();
			if (guessable.contains(abs(toDimacs(selection)))) {
				return selection;
			}
			if (selection == ILits.UNDEFINED) {
				return ILits.UNDEFINED;
			}
		}
	}

	@Override
	public void undo(int x) {
		delegate.undo(x);
	}

	@Override
	public void updateVar(int p) {
		if (guessable.contains(abs(toDimacs(p)))) {
			delegate.updateVar(p);
		}
	}

	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void printStat(PrintWriter out, String prefix) {
		delegate.printStat(out, prefix);
	}

	@Override
	public void setVarDecay(double d) {
		delegate.setVarDecay(d);
	}

	@Override
	public void varDecayActivity() {
		delegate.varDecayActivity();
	}

	@Override
	public double varActivity(int p) {
		return delegate.varActivity(p);
	}

	@Override
	public void assignLiteral(int p) {
		delegate.assignLiteral(p);
	}

	@Override
	public void setPhaseSelectionStrategy(IPhaseSelectionStrategy strategy) {
		delegate.setPhaseSelectionStrategy(strategy);
	}

	@Override
	public IPhaseSelectionStrategy getPhaseSelectionStrategy() {
		return delegate.getPhaseSelectionStrategy();
	}

	@Override
	public void updateVarAtDecisionLevel(int q) {
		delegate.updateVarAtDecisionLevel(q);
	}

	@Override
	public double[] getVariableHeuristics() {
		return delegate.getVariableHeuristics();
	}
}
