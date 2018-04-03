package it.unibz.stud_inf.ils.white.prisma;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unibz.stud_inf.ils.white.prisma.ast.Atom;
import it.unibz.stud_inf.ils.white.prisma.ast.Expression;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static it.unibz.stud_inf.ils.white.prisma.Util.SET_COLLECTOR;
import static java.lang.Math.abs;

public class ConjunctiveNormalForm {
	public static final String UNSAT = "UNSATISFIABLE";
	private static final String ATOM_PREFIX = ":";

	private final BiMap<String, Integer> map;
	private final IVec<IVecInt> clauses;
	private final IntIdGenerator generator = new IntIdGenerator(1);

	public ConjunctiveNormalForm() {
		this.map = HashBiMap.create();
		this.clauses = new Vec<>();
	}

	private String get(Integer variable) {
		return map.inverse().get(variable);
	}

	public Integer get(Expression expression) {
		return map.get(key(expression));
	}

	public Integer put(Expression expression, Integer variable) {
		return map.put(key(expression), variable);
	}

	public Integer put(Expression expression) {
		int variable = (int) generator.getNextId();
		map.put(key(expression), variable);
		return variable;
	}

	public Integer computeIfAbsent(Expression expression) {
		Integer variable = get(expression);
		if (variable != null) {
			return variable;
		}
		variable = expression.tseitin(this);
		put(expression, variable);
		return variable;
	}

	public Integer shallowComputeIfAbsent(Expression expression) {
		Integer variable = get(expression);
		if (variable != null) {
			return variable;
		}
		variable = (int) generator.getNextId();
		put(expression, variable);
		return variable;
	}

	public long getVariableCount() {
		return generator.getHighestId() - 1;
	}

	public int getClauseCount() {
		return clauses.size();
	}

	public IVec<IVecInt> add(int... literals) {
		return clauses.push(new VecInt(literals));
	}

	private static String key(Expression e) {
		final var key = e.toString();
		if (e instanceof Atom) {
			return ATOM_PREFIX + key;
		}
		return key;
	}

	public void printDimacsTo(final PrintStream out) {
		map.inverse().entrySet().stream()
			.filter(e -> isAtom(e.getValue()))
			.forEach(e -> out.println("c " + e.getKey() + " " + e.getValue().substring(ATOM_PREFIX.length())));

		out.println("p cnf " + getVariableCount() + " " + getClauseCount());

		// Vec and VecInt implementations of toArray() are cheap since they
		// only pass a reference.

		Stream.of(clauses.toArray())
			.map(c -> Stream.of(c.toArray())
				.map(String::valueOf)
				.collect(Collectors.joining(" "))
			)
			.forEach(s -> {
				out.print(s);
				out.println(" 0");
			});
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
			out.println(UNSAT);
		}
	}

	public Stream<SortedSet<String>> computeModels() {
		return StreamSupport.stream(new ModelSpliterator<>(clauses, m ->
			IntStream.of(m)
				.mapToObj(this::get)
				.filter(ConjunctiveNormalForm::isAtom)
				.map(s -> s.substring(1))
				.collect(Collectors.toCollection(TreeSet::new))
		), false);
	}

	private static boolean isAtom(String e) {
		return e != null && e.startsWith(ATOM_PREFIX) && !e.equals(ATOM_PREFIX + "true") && !e.equals(ATOM_PREFIX + "false");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);
			sb.append("{");
			for (int j = 0; j < clause.size(); j++) {
				Integer literal = clause.get(j);

				if (literal < 0) {
					sb.append("-");
				}

				Integer variable = abs(literal);
				String expression = get(variable);

				if (expression.startsWith(ATOM_PREFIX)) {
					sb.append(expression.substring(ATOM_PREFIX.length()));
				} else {
					sb.append("*");
					sb.append(variable);
				}

				if (j != clause.size() - 1) {
					sb.append(", ");
				}
			}

			sb.append(i != clauses.size() - 1 ? "}, " : "}");
		}
		return sb.append("}").toString();
	}
}
