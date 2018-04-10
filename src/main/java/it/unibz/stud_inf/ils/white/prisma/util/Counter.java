package it.unibz.stud_inf.ils.white.prisma.util;

import java.util.function.IntSupplier;
import java.util.stream.IntStream;

public class Counter implements IntSupplier {
	private final int step;
	private int next;

	public Counter() {
		this(0);
	}

	public Counter(int initial) {
		this(initial, 1);
	}

	public Counter(int initial, int step) {
		this.next = initial;
		this.step = step;
	}

	@Override
	public synchronized int getAsInt() {
		return next += step;
	}

	public synchronized IntStream stream() {
		return IntStream.iterate(next, (x) -> x + step);
	}
}