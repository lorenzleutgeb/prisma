package it.unibz.stud_inf.ils.white.prisma;

import java.util.ArrayList;
import java.util.List;

import static it.unibz.stud_inf.ils.white.prisma.Mode.REPL;

public class Options {
	public List<String> positionals = new ArrayList<>();

	public Long models = 1L;

	public Mode mode = REPL;

	public boolean help;

	public Options(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if ("-mode".equals(args[i]) && args.length > i + 1) {
				mode = Mode.valueOf(args[i + 1].toUpperCase());
				i++;
			} else if ("-help".equals(args[i])) {
				help = true;
			} else if ("-models".equals(args[i]) && args.length > i + 1) {
				models = Long.valueOf(args[i + 1]);
				i++;
			} else {
				positionals.add(args[i]);
			}
		}
	}

	public void printHelp() {
		System.out.println("Usage: prisma [options] [<input> [<output>]]\n" +
			"  Options:\n" +
			"    -help\n" +
			"      Show help message.\n" +
			"      Default: false\n" +
			"    -mode\n" +
			"      Mode to use.\n" +
			"      Default: REPL\n" +
			"      Possible Values: [CNF, DIMACS, SOLVE, REPL]\n" +
			"    -models\n" +
			"      Number of models to compute (at most).\n" +
			"      Default: 1\n");
	}
}
