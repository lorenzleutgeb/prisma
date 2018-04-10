package it.unibz.stud_inf.ils.white.prisma.cnf;

import it.unibz.stud_inf.ils.white.prisma.util.ModelSpliterator;
import it.unibz.stud_inf.ils.white.prisma.ast.expressions.Atom;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IVecInt;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static it.unibz.stud_inf.ils.white.prisma.util.Util.SET_COLLECTOR;
import static java.lang.Math.abs;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;

public class DIMACSCNF {
	public static final DIMACSCNF UNSAT = new DIMACSCNF(emptyMap(), singleton(VecInt.EMPTY), 0);

	private final Map<Integer, Atom> map;
	private final Set<IVecInt> clauses;
	private final int nvar;

	public DIMACSCNF(Map<Integer, Atom> map, Set<IVecInt> clauses, int nvar) {
		this.map = unmodifiableMap(map);
		this.clauses = unmodifiableSet(clauses);
		this.nvar = nvar;
	}

	public Stream<SortedSet<String>> computeModels() {
		return StreamSupport.stream(new ModelSpliterator<>(clauses, m ->
			IntStream.of(m)
				// Only positive literals.
				.filter(p -> p > 0)
				// Only literals that correspond to atoms.
				.filter(map::containsKey)
				.mapToObj(map::get)
				.map(Object::toString)
				.collect(toCollection(TreeSet::new))
		), false);
	}

	public void printModelsTo(PrintStream out, long n) {
		Stream<SortedSet<String>> models = computeModels().limit(n);

		boolean found = false;
		for (Iterator<SortedSet<String>> it = models.iterator(); it.hasNext(); ) {
			SortedSet<String> model = it.next();
			found = true;
			out.println(model.stream().collect(SET_COLLECTOR));
		}

		if (!found) {
			out.println("UNSATISFIABLE");
		}
	}

	public void printDimacsTo(final PrintStream out) {
		map.forEach((key, value) -> out.println("c " + key + " " + value));

		out.println("p cnf " + getVariableCount() + " " + getClauseCount());

		for (IVecInt clause : clauses) {
			for (int j = 0; j < clause.size(); j++) {
				out.print(clause.get(j));
				out.print(' ');
			}
			out.println(" 0");
		}
	}

	public long getVariableCount() {
		return nvar;
	}

	public int getClauseCount() {
		return clauses.size();
	}

	@Override
	public String toString() {
		return clauses.stream()
			.map(clause -> {
				StringBuilder sb = new StringBuilder("{");
				for (int j = 0; j < clause.size(); j++) {
					Integer literal = clause.get(j);

					if (literal < 0) {
						sb.append("-");
					}

					var expression = map.get(abs(literal));
					if (expression != null) {
						sb.append(expression);
					} else {
						sb.append("*");
						sb.append(abs(literal));
					}

					if (j != clause.size() - 1) {
						sb.append(", ");
					}
				}
				return sb.append("}");
			})
			.collect(SET_COLLECTOR);
	}
}
