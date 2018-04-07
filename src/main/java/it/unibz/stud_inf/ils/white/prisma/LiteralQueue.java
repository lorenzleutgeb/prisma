package it.unibz.stud_inf.ils.white.prisma;

import org.sat4j.minisat.core.Constr;
import org.sat4j.specs.UnitPropagationListener;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Queue;

class LiteralQueue implements UnitPropagationListener {
	private final Queue<Integer> delegate = new ArrayDeque<>();

	@Override
	public boolean enqueue(int p) {
		return enqueue(p, null);
	}

	@Override
	public boolean enqueue(int p, Constr from) {
		if (delegate.contains(p)) {
			return true;
		}

		delegate.add(p);

		// We are so naive ...
		return true;
	}

	@Override
	public void unset(int p) {
		throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public @Nonnull Integer remove() {
		return delegate.remove();
	}
}
