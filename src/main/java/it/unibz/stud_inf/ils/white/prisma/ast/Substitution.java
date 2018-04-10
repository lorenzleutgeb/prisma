package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.ast.Variable;

import java.util.HashMap;
import java.util.Map;

public class Substitution {
	private final Map<Variable, Object> erased;

	public Substitution() {
		this.erased = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	public <T> T eval(Variable<T> variable) {
		Object o = erased.get(variable);
		return (T) o;
	}

	@SuppressWarnings("unchecked")
	public <T> T put(Variable<T> variable, T item) {
		return (T) erased.put(variable, item);
	}

	@SuppressWarnings("unchecked")
	public <T> T remove(Variable<T> variable) {
		return (T) erased.remove(variable);
	}
}
