package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.Map;

public interface Standardizable<S> {
	S standardize(Map<Variable, Variable> map, Counter generator);
}
