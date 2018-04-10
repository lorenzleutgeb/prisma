package it.unibz.stud_inf.ils.white.prisma.util;

import org.sat4j.specs.UnitPropagationListener;

@FunctionalInterface
public interface PlainUnitPropagationListener extends UnitPropagationListener {
	@Override
	default boolean enqueue(int p) {
		return enqueue(p, null);
	}

	@Override
	default void unset(int p) {
		throw new UnsupportedOperationException();
	}
}
