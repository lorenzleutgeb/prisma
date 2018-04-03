package it.unibz.stud_inf.ils.white.prisma.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomErrorListener extends BaseErrorListener {
	private List<RecognitionException> recognitionExceptions = new ArrayList<>();
	private List<String> messages = new ArrayList<>();

	private final String fileName;

	public CustomErrorListener(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
		super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);

		this.messages.add(fileName + ":" + line + ":" + charPositionInLine + ": " + msg);
		this.recognitionExceptions.add(e);
	}

	public IOException getInputOutputException() {
		if (recognitionExceptions.isEmpty()) {
			return null;
		}
		return new IOException(
			"Could not parse formula! \n" + String.join("\n", messages) + "\n",
			recognitionExceptions.get(0)
		);
	}
}
