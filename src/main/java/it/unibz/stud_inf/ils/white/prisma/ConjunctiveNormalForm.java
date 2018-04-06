package it.unibz.stud_inf.ils.white.prisma;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unibz.stud_inf.ils.white.prisma.ast.Atom;
import it.unibz.stud_inf.ils.white.prisma.ast.Expression;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.MixedDataStructureDanielWL;
import org.sat4j.minisat.core.Constr;
import org.sat4j.minisat.core.ILits;
import org.sat4j.minisat.core.Propagatable;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.UnitPropagationListener;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static it.unibz.stud_inf.ils.white.prisma.Util.SET_COLLECTOR;
import static java.lang.Math.abs;
import static org.sat4j.core.LiteralsUtils.toDimacs;
import static org.sat4j.core.LiteralsUtils.toInternal;

public class ConjunctiveNormalForm {
	public static final String UNSAT = "UNSATISFIABLE";
	private static final String ATOM_PREFIX = ":";

	private final BiMap<String, Integer> map;
	private final IVec<IVecInt> clauses;
	private final IntIdGenerator generator;

	public ConjunctiveNormalForm() {
		this(HashBiMap.create(), new Vec<>(), 1);
	}

	private ConjunctiveNormalForm(BiMap<String, Integer> map, IVec<IVecInt> clauses, int initial) {
		this.map = map;
		this.clauses = clauses;
		this.generator = new IntIdGenerator(initial);
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
		VecInt clause = new VecInt(literals);
		clause.sortUnique();
		return clauses.push(clause);
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

		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);
			for (int j = 0; j < clause.size(); j++) {
				out.print(clause.get(j));
				out.print(' ');
			}
			out.println(" 0");
		}
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

	private static class Unit {
		private final int literal;
		private final Constr reason;

		private Unit(int literal, Constr reason) {
			this.literal = literal;
			this.reason = reason;
		}

		public int getLiteral() {
			return literal;
		}

		public Constr getReason() {
			return reason;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Unit unit = (Unit) o;
			return literal == unit.literal &&
				Objects.equals(reason, unit.reason);
		}

		@Override
		public int hashCode() {
			return Objects.hash(literal, reason);
		}
	}

	private static class Compressor implements UnitPropagationListener {
		private final List<Unit> queue = new ArrayList<>();

		@Override
		public boolean enqueue(int p) {
			return enqueue(p, null);
		}

		@Override
		public boolean enqueue(int p, Constr from) {
			for (Unit u : queue) {
				if (u.getLiteral() == p) {
					return true;
				}
			}

			Unit u = new Unit(p, from);
			queue.add(u);

			// We are so naive ...
			return true;
		}

		@Override
		public void unset(int p) {
			throw new UnsupportedOperationException();
		}
	}

	public ConjunctiveNormalForm compress() {
		Compressor l = new Compressor();
		MixedDataStructureDanielWL dsf = new MixedDataStructureDanielWL();
		dsf.setUnitPropagationListener(l);
		ILits lits = dsf.getVocabulary();

		try {
			for (int i = 0; i < clauses.size(); i++) {
				IVecInt clause = clauses.get(i);
				IVecInt converted = new VecInt(clause.size());
				int p;
				for (int j = 0; j < clause.size(); j++) {
					p = clause.get(j);
					int fromPool = dsf.getVocabulary().getFromPool(p);
					converted.push(fromPool);
				}
				dsf.createClause(converted);
			}
		} catch (ContradictionException e) {
			e.printStackTrace();
		}

		int pos = 0;
		while (pos < l.queue.size()) {
			for (int i = 0; i < getVariableCount(); i++) {
				int internal = toInternal(i + 1);
				String s = lits.isSatisfied(internal) ? "T" : (lits.isFalsified(internal) ? "F" : "?");
				System.out.println((i + 1) + " " + s);
			}

			Unit u = l.queue.get(pos++);

			if (u == null) {
				throw new IllegalStateException();
			}

			System.out.println("PROP " + toDimacs(u.getLiteral()) + " FOR REASON " + u.getReason());

			lits.satisfies(u.getLiteral());

			IVec<Propagatable> watches = dsf.getVocabulary().watches(u.getLiteral());

			final int size = watches.size();
			for (int i = 0; i < size; i++) {
				watches.get(i).propagate(l, u.getLiteral());
			}
		}

		IVec<IVecInt> compressed = new Vec<>(clauses.size());
		Set<Integer> occurring = new HashSet<>(map.size());

		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);
			VecInt copy = new VecInt(clause.size());

			for (int j = 0; j < clause.size(); j++) {
				int literal = clause.get(j);
				if (lits.isSatisfied(toInternal(literal))) {
					copy = null;
					break;
				} else if (!lits.isFalsified(toInternal(literal))) {
					copy.push(literal);
					occurring.add(abs(literal));
				}
			}

			if (copy != null) {
				compressed.push(copy);
			}
		}

		for (Map.Entry<String, Integer> e : map.entrySet()) {
			int literal = e.getValue();
			int internal = toInternal(literal);

			if (!isAtom(e.getKey())) {
				continue;
			}

			if (lits.isFalsified(internal)) {
				compressed.push(new VecInt(new int[]{-(literal)}));
				occurring.add(abs(literal));
			} else if (lits.isSatisfied(internal)) {
				compressed.push(new VecInt(new int[]{literal}));
				occurring.add(abs(literal));
			}
		}

		// Now pack new BiMap for compressed CNF.
		/*
		int max = occurring.size();
		BiMap<String, Integer> packed = HashBiMap.create(max);

		int id = 1;
		for (Integer p : occurring) {
			String key = map.inverse().get(p);
			packed.put(key, id++);
		}
		*/

		return new ConjunctiveNormalForm(map, compressed, (int)generator.getHighestId() + 1);
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
