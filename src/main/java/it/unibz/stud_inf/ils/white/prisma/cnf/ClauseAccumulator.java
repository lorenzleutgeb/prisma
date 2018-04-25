package it.unibz.stud_inf.ils.white.prisma.cnf;

import it.unibz.stud_inf.ils.white.prisma.ast.expressions.Atom;
import it.unibz.stud_inf.ils.white.prisma.ast.expressions.Expression;
import it.unibz.stud_inf.ils.white.prisma.util.Counter;
import it.unibz.stud_inf.ils.white.prisma.util.PlainUnitPropagationListener;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.MixedDataStructureDanielWL;
import org.sat4j.minisat.constraints.cnf.Lits;
import org.sat4j.minisat.core.Propagatable;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.sat4j.core.LiteralsUtils.toDimacs;

public class ClauseAccumulator {
	private final IVec<IVecInt> clauses;
	private final MixedDataStructureDanielWL dsf = new MixedDataStructureDanielWL();

	private final Map<Expression, Integer> map;
	private final List<Integer> propagated = new ArrayList<>();

	private boolean contradiction = false;

	private final PlainUnitPropagationListener upl = (p, constr) -> {
		if (getLits().isFalsified(p)) {
			contradict();
			return false;
		}
		if (!getLits().isSatisfied(p)) {
			propagated.add(p);
			getLits().satisfies(p);
		}
		return true;
	};

	public ClauseAccumulator() {
		this.map = new HashMap<>();
		this.clauses = new Vec<>();
		dsf.setUnitPropagationListener(upl);
	}

	private void contradict() {
		contradiction = true;
		map.clear();
	}

	public Integer get(Expression expression) {
		return map.get(expression);
	}

	private synchronized int next() {
		return getLits().getFromPool(getVariableCount() + 1);
	}

	public Integer put(Expression expression) {
		final int variable = next();
		if (map.put(expression, variable) != null) {
			throw new RuntimeException("Mapping of expression changed!");
		}
		return variable;
	}

	public Integer computeIfAbsent(Expression expression) {
		return map.computeIfAbsent(expression, (k) -> next());
	}

	public int getVariableCount() {
		if (contradiction) {
			return 0;
		}

		return getLits().nVars();
	}

	public int getClauseCount() {
		if (contradiction) {
			return 1;
		}

		return clauses.size();
	}

	public void add(VecInt clause) {
		if (contradiction) {
			return;
		}

		IVecInt copy = new VecInt();
		clause.copyTo(copy);
		try {
			dsf.createClause(clause);
		} catch (ContradictionException e) {
			contradiction = true;
		}
		clauses.push(copy);
	}

	public void add(int... literals) {
		add(new VecInt(literals));
	}

	public DIMACSCNF compress() {
		if (contradiction) {
			return DIMACSCNF.UNSAT;
		}

		for (int j = 0; j < propagated.size(); j++) {
			final int literal = propagated.get(j);
			final IVec<Propagatable> watches = dsf.getVocabulary().watches(literal);
			final int size = watches.size();
			for (int i = 0; i < size; i++) {
				if (!watches.get(i).propagate(upl, literal)) {
					return DIMACSCNF.UNSAT;
				}
			}
		}

		Counter denseId = new Counter();
		Map<Integer, Integer> denseMap = new HashMap<>(map.size());
		Set<IVecInt> compressed = new HashSet<>(clauses.size());

		// Filter all clauses according using the partial assignment
		// resulting from unit propagation.
		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);
			VecInt copy = new VecInt(clause.size());
			for (int j = 0; j < clause.size(); j++) {
				final int literal = clause.get(j);
				if (getLits().isSatisfied(literal)) {
					copy = null;
					break;
				}
				if (!getLits().isFalsified(literal)) {
					int p = denseMap.computeIfAbsent(literal & ~1, k -> denseId.getAsInt());
					copy.push(((literal & 1) == 1 ? -1 : 1) * p);
				}
			}

			if (copy == null) {
				continue;
			}

			if (copy.isEmpty()) {
				return DIMACSCNF.UNSAT;
			}

			compressed.add(copy);
		}

		final HashMap<Integer, Atom> packed = new HashMap<>(denseMap.size());
		for (Map.Entry<Expression, Integer> e : map.entrySet()) {
			final int literal = e.getValue();
			if (literal < 0) {
				continue;
			}

			int p = denseMap.computeIfAbsent(literal & ~1, k -> denseId.getAsInt());

			if (!getLits().isUnassigned(literal)) {
				VecInt clause = new VecInt(new int[] {
					(getLits().isFalsified(literal) ? -1 : 1) * p
				});
				compressed.add(clause);
			}

			final Expression expression = e.getKey();
			if (expression instanceof Atom) {
				packed.put(p, (Atom) expression);
			}
		}

		return new DIMACSCNF(packed, compressed, denseMap.size());
	}

	public Lits getLits() {
		return (Lits) dsf.getVocabulary();
	}

	@Override
	public String toString() {
		if (contradiction) {
			return "{{}}";
		}

		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);
			sb.append("{");
			for (int j = 0; j < clause.size(); j++) {
				Integer internal = clause.get(j);

				int literal = internal;

				if ((literal & 1) == 1) {
					sb.append("~");
				}

				if (internal > 0) {
					sb.append(literal & ~1);
				} else {
					sb.append("*");
					sb.append(toDimacs(internal));
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
