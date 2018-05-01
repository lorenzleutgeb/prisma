package it.unibz.stud_inf.ils.white.prisma.ast;

import it.unibz.stud_inf.ils.white.prisma.util.Counter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

public class Enumeration<U extends Groundable<T,U>, T> extends Domain<T> {
	private final List<U> elements;

	public Enumeration(List<U> elements) {
		this.elements = elements;
	}

	@Override
	public Stream<T> stream(Substitution substitution) {
		return elements.stream().map(p -> p.ground(substitution));
	}

	@Override
	public Set<Variable> getOccurringVariables() {
		final Set<Variable> base = new HashSet<>();
		elements
			.stream()
			.map(Groundable::getOccurringVariables)
			.forEach(base::addAll);
		return base;

	}

	@Override
	public String toString() {
		return elements.stream().map(Object::toString).collect(Collectors.joining(", ", "{", "}"));
	}

	@Override
	public Domain<T> standardize(Map<Variable, Variable> map, Counter generator) {
		return new Enumeration<U,T>(
			elements.stream().map(e -> e.standardize(map, generator)).collect(Collectors.toList())
		);
	}
}
