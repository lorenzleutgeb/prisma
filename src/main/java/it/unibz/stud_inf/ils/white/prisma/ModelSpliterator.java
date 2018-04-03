package it.unibz.stud_inf.ils.white.prisma;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.SortedSet;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;

class ModelSpliterator<T extends Comparable<T>, U extends SortedSet<T>> extends Spliterators.AbstractSpliterator<SortedSet<T>> {
	private final ISolver iterator;
	private final boolean contradiction;
	private final Function<int[], U> translation;

	ModelSpliterator(IVec<IVecInt> clauses, Function<int[], U> translation) {
		super(Long.MAX_VALUE, 0);
		ISolver solver = SolverFactory.newDefault();
		boolean contradiction = false;
		try {
			solver.addAllClauses(clauses);
		} catch (ContradictionException e) {
			contradiction = true;
		}
		this.contradiction = contradiction;
		this.translation = translation;
		this.iterator = new ModelIterator(solver);
	}

	@Override
	public boolean tryAdvance(Consumer<? super SortedSet<T>> action) {
		if (contradiction) {
			return false;
		}

		try {
			if (iterator.isSatisfiable()) {
				action.accept(translation.apply(iterator.model()));
				return true;
			}
			return false;
		} catch (TimeoutException e) {
			e.printStackTrace();
			return false;
		}
	}
}
