package it.unibz.stud_inf.ils.white.prisma;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unibz.stud_inf.ils.white.prisma.ast.Atom;
import it.unibz.stud_inf.ils.white.prisma.ast.Expression;
import javafx.beans.binding.IntegerExpression;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.constraints.MixedDataStructureDanielWL;
import org.sat4j.minisat.core.Constr;
import org.sat4j.minisat.core.ILits;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.UnitPropagationListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;
import static org.sat4j.core.LiteralsUtils.toDimacs;

public class ConjunctiveNormalForm {
	private final BiMap<Expression, Integer> map;
	private final IVec<IVecInt> clauses;

	private final Identifier atoms;
	private final Identifier gates;

	public ConjunctiveNormalForm() {
		this(HashBiMap.create(), new Vec<>(), 0, 0);
	}

	private ConjunctiveNormalForm(BiMap<Expression, Integer> map, IVec<IVecInt> clauses, int atomInitial, int gateInitial) {
		this.map = map;
		this.clauses = clauses;
		this.atoms = new Identifier(atomInitial, +2);
		this.gates = new Identifier(gateInitial, -2);
	}

	private Expression get(Integer variable) {
		return map.inverse().get(variable);
	}

	public Integer get(Expression expression) {
		return map.get(expression);
	}

	public Integer put(Expression expression, Integer variable) {
		if (map.containsKey(expression)) {
			throw new IllegalArgumentException("Already known.");
		}
		return map.put(expression, variable);
	}

	public Integer put(Expression expression) {
		int variable = (expression instanceof Atom ? atoms : gates).getAsInt();
		if (map.containsKey(expression)) {
			throw new IllegalArgumentException("Already known.");
		}
		map.put(expression, variable);
		return variable;
	}

	public Integer computeIfAbsent(Expression expression) {
		Integer variable = get(expression);
		return variable != null ? variable : put(expression);
	}

	public long getVariableCount() {
		return (atoms.peek() / 2) + (gates.peek() / -2);
	}

	public int getClauseCount() {
		return clauses.size();
	}

	public IVec<IVecInt> add(int... literals) {
		return clauses.push(new VecInt(literals));
	}

	public DIMACSCNF compress() {
		LiteralQueue l = new LiteralQueue();
		MixedDataStructureDanielWL dsf = new MixedDataStructureDanielWL();

		final List<Integer> propagated = new ArrayList<>();

		dsf.setUnitPropagationListener(new UnitPropagationListener() {
			@Override
			public boolean enqueue(int p) {
				return enqueue(p, null);
			}

			@Override
			public boolean enqueue(int p, Constr from) {
				propagated.add(p);
				return true;
			}

			@Override
			public void unset(int p) {
				throw new UnsupportedOperationException();
			}
		});

		ILits lits = dsf.getVocabulary();

		int off = atoms.peek() + 1;
		int nvar = atoms.peek() / 2 + gates.peek() / -2;
		lits.init(nvar);

		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);

			IVecInt converted = new VecInt(clause.size());
			for (int j = 0; j < clause.size(); j++) {
				int literal = clause.get(j);
				if (literal < 0) {
					literal = (literal * -1) + off;
				}
				literal = lits.getFromPool(toDimacs(literal));
				converted.push(literal);
			}
			try {
				dsf.createClause(converted);
			} catch (ContradictionException e) {
				return DIMACSCNF.UNSAT;
			}
		}

		for (int j = 0; j < propagated.size(); j++) {
			var u = propagated.get(j);

			if (lits.isFalsified(u)) {
				return DIMACSCNF.UNSAT;
			}

			if (!lits.isSatisfied(u)) {
				lits.satisfies(u);
			}

			final var watches = dsf.getVocabulary().watches(u);
			final int size = watches.size();
			for (int i = 0; i < size; i++) {
				if (!watches.get(i).propagate(l, u)) {
					return DIMACSCNF.UNSAT;
				}
			}
		}

		Identifier denseId = new Identifier(0);
		Map<Integer, Integer> denseMap = new HashMap<>(map.size());
		Set<IVecInt> compressed = new HashSet<>(clauses.size());

		// Filter all clauses according using the partial assignment
		// resulting from unit propagation and DIMACSify variables
		// along the way.
		for (int i = 0; i < clauses.size(); i++) {
			IVecInt clause = clauses.get(i);

			boolean skip = false;
			for (int j = 0; j < clause.size(); j++) {
				int literal = clause.get(j);

				if (literal < 0) {
					literal = (literal * -1) + off;
				}

				if (lits.isSatisfied(literal)) {
					skip = true;
					break;
				}
			}

			if (skip) {
				continue;
			}

			VecInt copy = new VecInt(clause.size());
			for (int j = 0; j < clause.size(); j++) {
				int literal = clause.get(j);
				int internal = (literal < 0) ? literal * -1 + off : literal;

				if (!lits.isFalsified(internal)) {
					int p = denseMap.computeIfAbsent(literal & ~1, k -> denseId.getAsInt());
					copy.push(((literal & 1) == 1 ? -1 : 1) * p);
				}
			}

			compressed.add(copy);
		}

		// Now pack new BiMap for compressed CNF.
		Map<Integer, Atom> packed = new HashMap<>(denseMap.size());

		for (Map.Entry<Expression, Integer> e : map.entrySet()) {
			int literal = e.getValue();
			if (literal < 0) {
				continue;
			}

			int variable = literal & ~1;

			int p = denseMap.computeIfAbsent(variable, k -> denseId.getAsInt());

			if (!lits.isUnassigned(literal)) {
				var clause = new VecInt(new int[] {
					(lits.isFalsified(literal) ? -1 : 1) * p
				});
				compressed.add(clause);
			}

			Expression value = map.inverse().get(variable);
			if (!(value instanceof Atom)) {
				continue;
			}
			packed.put(p, (Atom) value);
		}

		return new DIMACSCNF(
			packed,
			compressed,
			denseMap.size()
		);
	}

	@Override
	public String toString() {
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
					sb.append(get(literal & ~1));
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
