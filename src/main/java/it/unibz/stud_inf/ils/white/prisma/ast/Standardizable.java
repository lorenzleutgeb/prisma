package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.IntIdGenerator;

import java.util.Map;

@Deprecated
public interface Standardizable<S> {
	@Deprecated
	S standardize(Map<Long, Long> map, IntIdGenerator generator);
}
