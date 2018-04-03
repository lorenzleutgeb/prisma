package it.unibz.stud_inf.ils.white.prisma;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util {
	public static final Collector<CharSequence, ?, String> SET_COLLECTOR = Collectors.joining(", ", "{", "}");

	public static long toLong(byte[] bytes) {
		if (bytes.length > 8) {
			throw new IllegalArgumentException();
		}

		long ret = 0;
		for (int i = 0; i < 8 && i < bytes.length; i++) {
			ret <<= 8;
			ret |= (long)bytes[i] & 0xFF;
		}

		return ret;
	}
}