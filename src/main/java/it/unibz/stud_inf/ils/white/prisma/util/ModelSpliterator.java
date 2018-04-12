package it.unibz.stud_inf.ils.white.prisma.util;

import org.sat4j.minisat.constraints.MixedDataStructureDanielWL;
import org.sat4j.minisat.core.DataStructureFactory;
import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.Solver;
import org.sat4j.minisat.learning.MiniSATLearning;
import org.sat4j.minisat.orders.RSATPhaseSelectionStrategy;
import org.sat4j.minisat.orders.SubsetVarOrder;
import org.sat4j.minisat.orders.VarOrderHeap;
import org.sat4j.minisat.restarts.Glucose21Restarts;
import org.sat4j.minisat.restarts.MiniSATRestarts;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;

public class ModelSpliterator<T extends Comparable<T>, U extends SortedSet<T>> extends Spliterators.AbstractSpliterator<SortedSet<T>> {
	private final ISolver iterator;
	private final boolean contradiction;
	private final Function<int[], U> translation;
	private final HashSet<U> memory;

	public ModelSpliterator(Set<IVecInt> clauses, Set<Integer> guessable, Function<int[], U> translation) {
		super(Long.MAX_VALUE, 0);

		final int[] guessableArray = new int[guessable.size()];
		int i = 0;
		for (Integer l : guessable) {
			guessableArray[i++] = l;
		}

		IOrder order = new SubsetVarOrder(guessableArray);
		//IOrder order = new RestrictedOrder(new VarOrderHeap(new RSATPhaseSelectionStrategy()), guessable);

		// Expansion of SolverFactory.newDefault()
		// in order to allow for configuration.
		DataStructureFactory dsf = new MixedDataStructureDanielWL();
		MiniSATLearning<DataStructureFactory> learning = new MiniSATLearning<>();
		Solver<DataStructureFactory> solver = new Solver<>(learning, dsf, order, new Glucose21Restarts());
		solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
		solver.setLearnedConstraintsDeletionStrategy(solver.glucose);
		solver.setDBSimplificationAllowed(true);

		boolean contradiction = false;
		try {
			for (IVecInt clause : clauses) {
				solver.addClause(clause);
			}
		} catch (ContradictionException e) {
			contradiction = true;
		}
		this.contradiction = contradiction;
		this.translation = translation;
		this.iterator = new ModelIterator(solver);
		this.memory = new HashSet<>();
	}

	@Override
	public boolean tryAdvance(Consumer<? super SortedSet<T>> action) {
		if (contradiction) {
			return false;
		}

		try {
			if (iterator.isSatisfiable()) {
				U translation = this.translation.apply(iterator.model());
				if (!memory.contains(translation)) {
					memory.add(translation);
					action.accept(translation);
				} else {
					System.out.println("FILTERING");
				}
				return true;
			}
			return false;
		} catch (TimeoutException e) {
			e.printStackTrace();
			return false;
		}
	}
}
