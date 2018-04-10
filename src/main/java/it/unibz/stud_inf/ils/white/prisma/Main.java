package it.unibz.stud_inf.ils.white.prisma;

import com.beust.jcommander.JCommander;
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
		Options options = new Options();

		JCommander jc = JCommander.newBuilder().programName("prisma").addObject(options).build();
		jc.parse(args);

		if (options.help) {
			jc.usage();
			return;
		}

		if (REPL.equals(options.mode)) {
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
					ps.println(cnf);
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

		// Maybe print the grammar here.
		System.out.println(" \"<expression>\"  to  conjoin it with previous ones\n             \"\"  to  search computeModels\n        \"clear\"  to  start over\n         \"exit\"  to  exit");

		try {
			System.out.print("> ");
			String ln;
			while ((ln = reader.readLine()) != null) {
				if (ln.isEmpty()) {
					var it = f.accumulate().compress().computeModels().iterator();

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
				} else {
					f.add(Parser.parse(ln));
					System.out.println(f.toSingleExpression());
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