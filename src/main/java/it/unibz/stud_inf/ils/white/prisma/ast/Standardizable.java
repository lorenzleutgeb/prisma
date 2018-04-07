package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.Identifier;

import java.util.Map;

@Deprecated
public interface Standardizable<S> {
	@Deprecated
	S standardize(Map<Long, Long> map, Identifier generator);
}
