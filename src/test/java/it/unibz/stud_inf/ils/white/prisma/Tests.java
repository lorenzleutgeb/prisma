package it.unibz.stud_inf.ils.white.prisma;

import it.unibz.stud_inf.ils.white.prisma.ast.expressions.Formula;
import it.unibz.stud_inf.ils.white.prisma.cnf.ClauseAccumulator;
import it.unibz.stud_inf.ils.white.prisma.cnf.DIMACSCNF;
import it.unibz.stud_inf.ils.white.prisma.parser.Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;

class Tests {
	static Stream<? extends Arguments> groundInstances() {
		return Stream.of(
			of("~(q | p)",     2, 2, 1),
			of("~(q & p)",     2, 1, 3),
			of("~~(q | p)",    2, 1, 3),
			of("~~q",          1, 1, 1),
			of("~q",           1, 1, 1),
			of("p | q",        2, 1, 3),
			of("p & q",        2, 2, 1),
			of("p ^ q",        2, 2, 2),
			of("(p | q) ^ (r | s)", -1, -1, 6),
			of("(p & q) ^ (r & s)", -1, -1, 6),
			of("(p -> q) -> r",     -1, -1, 5),
			of("p & ~p",       0, 1, 0),
			of("~~~~q",        1, 1, 1),
			of("p -> q",       2, 1, 3),
			of("p <-> q",      2, 2, 2),
			of("p ? q : r",    3, 2, 4),
			of("p <- q",       2, 1, 3),
			of("~(p -> q)",    2, 2, 1),
			of("true & false", 0, 1, 0),
			of("1 > 2",        0, 1, 0),

			of("~(~(~p & ~q) & ~(~q & r))", 3 + 2, 5, 3), // Yields DNF for NNF.
			of("~(p -> s -> (q & r))", 4, 2, 9)
		);
	}

	static Stream<? extends Arguments> nonGroundInstances() {
		return Stream.of(
			of("forall @X in {a,b} ~(@X & (exists #Y in [1...3] (t(#Y) -> q(#Y))))", -1, -1, -1),
			of("forall $x in {a,b} exists $y in {$x,c} p($x,$y)", -1, -1, -1), // noswitch, fine, related, dependent
			of("exists $x in {a,b} forall $y in {$x,c} p($x,$y)", -1, -1, -1), // noswitch, related, dependent
			of("exists $x in {a,b} forall $y in {c,d} p($x,$y)", -1, -1, -1), // noswitch, dependent
			of("exists $x in {a,b} forall $y in {c,d} (p($x) & p($y))", -1, -1, -1), // noswitch, fine, related, dependent

			of("exists $y in {a,b,c} forall $x in {a,b,c} (q & (p($x) & p($y)))", -1, -1, -1), // switch
			of("forall $x in {a,b,c} exists $y in {a,b,c} (q & (p($x) & p($y)))", -1, -1, -1), // noswitch
			of("exists $y in {a,b,c} exists $z in {a,b,c} forall $x in {a,b,c} (q & (p($x) & p($y)))", -1, -1, -1),  // switch, eliminate

			of("exists $y in {a,b,c} forall $x in {a,b,c} (q | (p($x) | p($y)))", -1, -1, -1),
			of("forall $x in {a,b,c} exists $y in {a,b,c} (q | (p($x) | p($y)))", -1, -1, -1),
			of("forall #X in [1...3] p(#X)", -1, -1, -1),
			of("forall #X in [1...3] forall #Y in [1...3] p(#X,#Y)", -1, -1, -1),
			of("forall #Y in [1...3] exists #X in [1...3] p(#Y,#X,#Y+#X)", -1, -1, -1),
			of("exists #X in [1...3] p(#X)", -1, -1, -1),
			of("forall $Y in {a,b,c} exists #X in [1...3] p($Y,#X)", -1, -1, -1),
			of("forall $X in {a,b} exists $Y in {c,d} forall $Z in {e,f} (p($X) | p($Y) | p($Z))", -1, -1, -1),
			of("forall $X in {a,b} exists $Y in {c,d} p($X,$Y)", -1, -1, -1),
			of("forall @X in {a,b} (@X & (exists #Y in [1...3] t(#Y)))", -1, -1, -1),
			of("~(exists $X in {a,b} p($X))", -1, -1, -1),
			of("(forall $x in {a,b} (exists $y in {a,b} phi($y)) | ((exists $z in {a,b} psi($z)) -> rho($x)))", -1, -1, -1),
			of("forall $x in {a,b} ((exists $y in {a,b} phi($y)) | ((exists $z in {a,b} psi($z)) -> rho($x)))", -1, -1, -1),
			of("exists #x in [2...6] (#x < 4 & p(#x))", -1, -1, -1),
			of("exists $y in {a,b,c} forall $x in {a,b,c} (p($x) & p($y))", -1, -1, -1),
			of("forall $x in {a,b,c} exists $y in {a,b,c} (p($x) & p($y))", -1, -1, -1)
		);
	}

	@ParameterizedTest
	@MethodSource("groundInstances")
	void solveGround(String formula, int vars, int clauses, int models) throws IOException {
		solveAndAssert(formula, vars, clauses, models);
	}

	@ParameterizedTest
	@MethodSource("nonGroundInstances")
	void solveNonGround(String formula, int vars, int clauses, int models) throws IOException {
		solveAndAssert(formula, vars, clauses, models);
	}

	@Test
	void my() throws IOException {
		solveAndAssert("exists $x in {a, b} (~exists $y in {c, d} (p($x) & p($y)))", -1, -1, -1);
	}

	void solveAndAssert(String formula, int vars, int clauses, int models) throws IOException {
		solveAndAssert(CharStreams.fromString(formula), vars, clauses, models);
	}

	void solveAndAssert(CharStream formula, int vars, int clauses, int models) throws IOException {
		Formula f = Parser.parse(formula);
		System.out.println("Input:                 " + f);
		System.out.println("Normalized (&/|/~):    " + (f = f.normalize()));
		System.out.println("Standardized apart:    " + (f = f.standardize()));
		System.out.println("Quantifiers minimized: " + (f = f.pushQuantifiersDown()));
		System.out.println("Ground:                " + (f = f.ground()));

		final ClauseAccumulator cnf = f.tseitin();
		DIMACSCNF comp = cnf.compress();
		System.out.println("CNF size:             " + comp.getVariableCount() + " " + comp.getClauseCount());
		System.out.println("CNF follows:");
		System.out.println(comp);
		System.out.println("Models follow:");
		comp.printModelsTo(System.out, Long.MAX_VALUE);

		if (vars >= 0) {
			assertEquals(vars, comp.getVariableCount(), "Number of Variables");
		}
		if (clauses >= 0) {
			assertEquals(clauses, comp.getClauseCount(), "Number of Clauses");
		}
		if (models >= 0) {
			assertEquals(models, comp.computeModels().count(), "Number of Models");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"(forall @X in a, b { }"})
	void parse(final String formula) {
		assertThrows(RuntimeException.class, () -> Parser.parse(formula));
	}

	@Test
	void parseExplosion() {
		for (int n = 3; n < 11; n++) {
			final String in = String.join(" ^ ", Collections.nCopies(n, "p"));
			final String out = Parser.parse(in).accumulate().toString();
			final int ratio = out.length() / in.length();
			assertTrue(ratio < 60, "CNF does not explode in size.");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"/sudoku.bool", "/sudoku-empty.bool", "/quants-3.bool", "/quants-5.bool"})
	void instance(String fileName) throws IOException {
		Formula f = Parser.parse(CharStreams.fromStream(getClass().getResourceAsStream(fileName)));
		ClauseAccumulator cnf = f.accumulate();
		System.out.println(cnf.getVariableCount() + " " + cnf.getClauseCount());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/zebra.bool"})
	void instanceDetail(String fileName) throws IOException {
		solveAndAssert(CharStreams.fromStream(getClass().getResourceAsStream(fileName)), -1, -1, -1);
	}


	@ParameterizedTest
	@CsvSource({
		"2, 2,  2", // 2!
		"3, 2,  0",
		"2, 3,  6", // 2! * 3
		"3, 3,  6", // 3!
		"4, 3,  0",
		"3, 4, 24", // 3! * 4
		"4, 4, 24", // 4!
		"3, 5, 60", // 3! * 5 * 2
		"2, 5, 20"  // ?
	})
	void php(int pigeons, int holes, int models) throws IOException {
		solveAndAssert(
			"forall #p in [1..." + pigeons + "] exists #h in [1..." + holes + "] " + "a(#p,#h) \n" +
			"forall #p1 in [1..." + (pigeons - 1) + "] forall #p2 in [#p1+1..." + pigeons + "] forall #h in [1..." + holes + "] (a(#p1,#h) -> ~a(#p2,#h)) \n" +
			"forall #h1 in [1..." + (holes - 1) + "] forall #h2 in [#h1+1..." + holes + "] forall #p in [1..." + pigeons + "] (a(#p,#h1) -> ~a(#p,#h2))",
			pigeons * holes,
			-1,
			models
		);
	}
}
