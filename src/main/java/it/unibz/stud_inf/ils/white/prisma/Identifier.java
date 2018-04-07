package it.unibz.stud_inf.ils.white.prisma;

import java.util.function.IntSupplier;

public class Identifier implements IntSupplier {
	private final int step;
	private int next;

	public Identifier() {
		this(0);
	}

	public Identifier(int initial) {
		this(initial, 1);
	}

	public Identifier(int initial, int step) {
		this.next = initial;
		this.step = step;
	}

	@Override
	public synchronized int getAsInt() {
		return next += step;
	}

	public int peek() {
		return next;
	}
}