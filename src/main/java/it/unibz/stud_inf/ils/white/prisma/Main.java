package it.unibz.stud_inf.ils.white.prisma;

import it.unibz.stud_inf.ils.white.prisma.ast.expressions.Formula;
import it.unibz.stud_inf.ils.white.prisma.cnf.ClauseAccumulator;
import it.unibz.stud_inf.ils.white.prisma.parser.Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.Stack;

import static it.unibz.stud_inf.ils.white.prisma.Mode.REPL;
import static it.unibz.stud_inf.ils.white.prisma.util.Util.SET_COLLECTOR;

public class Main {
	private static final String BANNER =
		"             _                       \n" +
		"  _ __  _ __(_)___ _ __ ___   __ _          /\\   \n" +
		" | '_ \\| '__| / __| '_ ` _ \\ / _` |        /  \\\u001B[96m####\u001B[0m\n" +
		" | |_) | |  | \\__ \\ | | | | | (_| |   \u001B[97m####\u001B[0m/    \\\u001B[95m####\u001B[0m\n" +
		" | .__/|_|  |_|___/_| |_| |_|\\__,_|      /      \\\u001B[93m####\u001B[0m\n" +
		" |_|                                    /________\\\n" +
		"\n" +
		" Team White\n" +
		" powered by ANTLR.org and SAT4J.org\n";

	public static void main(String[] args) throws IOException {
		Options options = new Options(args);

		if (options.help) {
			options.printHelp();
			return;
		}

		if (REPL.equals(options.mode)) {
			if (options.positionals.size() > 0) {
				System.out.println("NOTICE: Input file is ignored in REPL mode.");
			}
			if (options.positionals.size() > 1) {
				System.out.println("NOTICE: Output file is ignored in REPL mode.");
			}
			if (options.models != 1) {
				System.out.println("NOTICE: '-models' option is ignored in REPL mode.");
			}
			repl();
			return;
		}

		CharStream inputStream;

		if (options.positionals.isEmpty()) {
			inputStream = CharStreams.fromStream(System.in);
		} else {
			inputStream = CharStreams.fromFileName(options.positionals.remove(0));
		}

		Formula formula = Parser.parse(inputStream);
		ClauseAccumulator cnf = formula.accumulate();

		try (PrintStream ps = new PrintStream(options.positionals.isEmpty() ? System.out : new FileOutputStream(options.positionals.get(0)))) {
			switch (options.mode) {
				case CNF:
					ps.println(cnf.compress());
					break;
				case DIMACS:
					cnf.compress().printDimacsTo(ps);
					break;
				case SOLVE:
					cnf.compress().printModelsTo(ps, options.models);
					break;
				default:
					bailOut("?", null);
			}
		}
	}

	private static void repl() {
		System.out.println(BANNER);

		Formula f = new Formula();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		Stack<Formula> undoStack = new Stack<>();
		undoStack.push(f);

		// Maybe print the grammar here.
		System.out.println(
			" \"<expression>\"  to  conjoin it with previous ones\n" +
			"             \"\"  to  compute models\n" +
			"        \"clear\"  to  start over\n" +
			"         \"exit\"  to  exit\n" +
			"    \"normalize\"  to  apply normalization\n" +
			"  \"standardize\"  to  apply standardization\n" +
			"         \"push\"  to  push down and reorder quantifiers\n" +
			"       \"ground\"  to  ground the formula\n" +
			"       \"dimacs\"  to  produce the CNF in DIMACS format\n" +
			"         \"undo\"  to  undo your last operation\n"
		);

		try {
			System.out.print("> ");
			String ln;
			boolean save;
			while ((ln = reader.readLine()) != null) {
				save = false;
				try {
					if (ln.isEmpty()) {
						Iterator<SortedSet<String>> it = f.accumulate().compress().computeModels().iterator();

						while (true) {
							if (!it.hasNext()) {
								System.out.println("UNSATISFIABLE");
								break;
							}

							System.out.println(it.next().stream().collect(SET_COLLECTOR));

							System.out.print("Search for more? [y|N] ");
							ln = reader.readLine();

							if (!ln.toLowerCase().startsWith("y")) {
								break;
							}
						}
					} else if (ln.toLowerCase().equals("clear")) {
						f = new Formula();
						save = true;
					} else if (ln.toLowerCase().equals("exit")) {
						System.exit(0);
					} else if (ln.toLowerCase().equals("normalize")) {
						f = f.normalize();
						save = true;
					} else if (ln.toLowerCase().equals("standardize")) {
						f = f.normalize().standardize();
						save = true;
					} else if (ln.toLowerCase().equals("push")) {
						f = f.normalize().standardize().pushQuantifiersDown();
						save = true;
					} else if (ln.toLowerCase().equals("ground")) {
						f = f.normalize().standardize().pushQuantifiersDown().ground();
						save = true;
					} else if (ln.toLowerCase().equals("dimacs")) {
						f.accumulate().compress().printDimacsTo(System.out);
					} else if (ln.toLowerCase().equals("undo")) {
						if (undoStack.size() == 1) {
							System.out.println("Cannot undo last operation!");
						}
						undoStack.pop();
						f = undoStack.pop();
						System.out.println(f.toSingleExpression());
						save = false;
					} else {
						f = f.add(Parser.parse(ln));
						save = true;
					}
				} catch (Exception e) {
					e.printStackTrace(System.out);
				}
				if (save) {
					System.out.println(f.toSingleExpression());
					undoStack.push(f);
				}

				System.out.print("> ");
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void bailOut(String s, Exception e) {
		System.err.println(s);
		if (e != null) {
			e.printStackTrace(System.err);
		}
		// LOGGER.error(format, arguments);
		System.exit(1);
		throw new RuntimeException("Reached fatal error.");
	}
}