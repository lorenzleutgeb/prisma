package it.unibz.stud_inf.ils.white.prisma;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.EnumConverter;

import java.util.ArrayList;
import java.util.List;

import static it.unibz.stud_inf.ils.white.prisma.Mode.REPL;

public class Options {
	@Parameter(description = "[<input> [<output>]]")
	public List<String> positionals = new ArrayList<>();

	@Parameter(names = "-models", description = "Number of models to compute (at most).")
	public Integer models = 1;

	@Parameter(names = "-mode", description = "Mode to use.", converter = ModeEnumConverter.class)
	public Mode mode = REPL;

	@Parameter(names = "-help", description = "Show help message.")
	public boolean help;

	private static class ModeEnumConverter extends EnumConverter<Mode> {
		public ModeEnumConverter(String optionName, Class<Mode> clazz) {
			super(optionName, clazz);
		}

		@Override
		public Mode convert(String value) {
			return Mode.valueOf(value.toUpperCase());
		}
	}
}
