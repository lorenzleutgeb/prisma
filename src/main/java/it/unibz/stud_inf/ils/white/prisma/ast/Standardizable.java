package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.Counter;

import java.util.Map;

public interface Standardizable<S> {
	S standardize(Map<Long, Long> map, Counter generator);
}
