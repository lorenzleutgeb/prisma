package it.unibz.stud_inf.ils.white.prisma.parser;

import it.unibz.stud_inf.ils.white.prisma.antlr.FormulaBaseVisitor;
import it.unibz.stud_inf.ils.white.prisma.antlr.FormulaLexer;
import it.unibz.stud_inf.ils.white.prisma.antlr.FormulaParser;
import it.unibz.stud_inf.ils.white.prisma.ast.Arg;
import it.unibz.stud_inf.ils.white.prisma.ast.ArithmeticAtom;
import it.unibz.stud_inf.ils.white.prisma.ast.Atom;
import it.unibz.stud_inf.ils.white.prisma.ast.BooleanConnective;
import it.unibz.stud_inf.ils.white.prisma.ast.ConstantPredicate;
import it.unibz.stud_inf.ils.white.prisma.ast.ConstantTerm;
import it.unibz.stud_inf.ils.white.prisma.ast.Domain;
import it.unibz.stud_inf.ils.white.prisma.ast.Enumeration;
import it.unibz.stud_inf.ils.white.prisma.ast.EqualityAtom;
import it.unibz.stud_inf.ils.white.prisma.ast.Expression;
import it.unibz.stud_inf.ils.white.prisma.ast.Formula;
import it.unibz.stud_inf.ils.white.prisma.ast.IntBinaryConnectiveExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.IntExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.IntExpressionRange;
import it.unibz.stud_inf.ils.white.prisma.ast.IntNumberExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.IntUnaryConnectiveExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.IntVariable;
import it.unibz.stud_inf.ils.white.prisma.ast.ConnectiveExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.Predicate;
import it.unibz.stud_inf.ils.white.prisma.ast.PredicateVariable;
import it.unibz.stud_inf.ils.white.prisma.ast.QuantifiedExpression;
import it.unibz.stud_inf.ils.white.prisma.ast.Quantifier;
import it.unibz.stud_inf.ils.white.prisma.ast.Term;
import it.unibz.stud_inf.ils.white.prisma.ast.VariableTerm;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class Parser {
	public static Formula parse(String s) {
		try {
			return parse(CharStreams.fromString(s));
		} catch (IOException e) {
			// In this case we assume that something went fundamentally
			// wrong when using a String as input. The caller probably
			// assumes that I/O on a String should always be fine.
			throw new RuntimeException(e);
		}
	}

	public static Formula parse(CharStream stream) throws IOException {
		/*
		// In order to require less memory: use unbuffered streams and avoid constructing a full parse tree.
		FormulaLexer lexer = new FormulaLexer(new UnbufferedCharStream(is));
		lexer.setTokenFactory(new CommonTokenFactory(true));
		final FormulaParser parser = new FormulaParser(new UnbufferedTokenStream<>(lexer));
		parser.setBuildParseTree(false);
		*/
		CommonTokenStream tokens = new CommonTokenStream(new FormulaLexer(stream));
		final FormulaParser parser = new FormulaParser(tokens);

		// Try SLL parsing mode (faster but may terminate incorrectly).
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());

		final CustomErrorListener errorListener = new CustomErrorListener(stream.getSourceName());

		FormulaParser.FormulaContext formulaContext;
		try {
			// Parse program
			formulaContext = parser.formula();
		} catch (ParseCancellationException e) {
			// Recognition exception may be caused simply by SLL parsing failing,
			// retry with LL parser and DefaultErrorStrategy printing errors to console.
			if (e.getCause() instanceof RecognitionException) {
				tokens.seek(0);
				parser.addErrorListener(errorListener);
				parser.setErrorHandler(new DefaultErrorStrategy());
				parser.getInterpreter().setPredictionMode(PredictionMode.LL);
				// Re-run parse.
				formulaContext = parser.formula();
			} else {
				throw e;
			}
		}

		// If the our SwallowingErrorListener has handled some exception during parsing
		// just re-throw that exception.
		// At this time, error messages will be already printed out to standard error
		// because ANTLR by default adds an org.antlr.v4.runtime.ConsoleErrorListener
		// to every parser.
		// That ConsoleErrorListener will print useful messages, but not report back to
		// our code.
		// org.antlr.v4.runtime.BailErrorStrategy cannot be used here, because it would
		// abruptly stop parsing as soon as the first error is reached (i.e. no recovery
		// is attempted) and the user will only see the first error encountered.
		IOException ioe = errorListener.getInputOutputException();
		if (ioe != null) {
			throw ioe;
		}

		// Construct internal program representation.
		FormulaVisitor visitor = new FormulaVisitor();
		return visitor.visitFormula(formulaContext);
	}

	private static class FormulaVisitor extends FormulaBaseVisitor<Formula> {
		@Override
		public Formula visitFormula(FormulaParser.FormulaContext ctx) {
			final ExpressionVisitor visitor = new ExpressionVisitor();
			return new Formula(ctx.expression().stream().map(visitor::visit).collect(Collectors.toList()));
		}
	}

	private static class ExpressionVisitor extends FormulaBaseVisitor<Expression> {
		@Override
		@SuppressWarnings("unchecked")
		public Expression visitPredicateQuantification(FormulaParser.PredicateQuantificationContext ctx) {
			DomainVisitor visitor = new DomainVisitor();

			return new QuantifiedExpression<>(
				new Quantifier(
					ctx.quantifier.getText(),
					new PredicateVariable(ctx.variable.getText().substring(1)),
					(Domain<Predicate>) visitor.visit(ctx.predicateSet())
				),
				visit(ctx.scope)
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Expression visitTermQuantification(FormulaParser.TermQuantificationContext ctx) {
			DomainVisitor visitor = new DomainVisitor();

			return new QuantifiedExpression<>(
				new Quantifier(
					ctx.quantifier.getText(),
					new VariableTerm(ctx.variable.getText().substring(1)),
					(Domain<ConstantTerm>) visitor.visit(ctx.termSet())
				),
				visit(ctx.scope)
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Expression visitIntExpressionQuantification(FormulaParser.IntExpressionQuantificationContext ctx) {
			DomainVisitor visitor = new DomainVisitor();

			return new QuantifiedExpression<>(
				new Quantifier(
					ctx.quantifier.getText(),
					new IntVariable(ctx.variable.getText().substring(1)),
					(Domain<IntNumberExpression>) visitor.visit(ctx.intExpressionSet())
				),
				visit(ctx.scope)
			);
		}

		@Override
		public Expression visitParenthesizedExpression(FormulaParser.ParenthesizedExpressionContext ctx) {
			return visit(ctx.expression());
		}

		@Override
		public Expression visitAtom(FormulaParser.AtomContext ctx) {
			PredicateVisitor visitor = new PredicateVisitor();
			return new Atom(
				visitor.visit(ctx.predicate()),
				wrap(ctx.args())
			);
		}

		@Override
		public Expression visitUnary(FormulaParser.UnaryContext ctx) {
			return new ConnectiveExpression(
				BooleanConnective.NOT,
				visit(ctx.expression())
			);
		}

		@Override
		public Expression visitBinary(FormulaParser.BinaryContext ctx) {
			return new ConnectiveExpression(
				BooleanConnective.fromOperator(ctx.op.getText()),
				visit(ctx.left),
				visit(ctx.right)
			);
		}

		@Override
		public Expression visitTernary(FormulaParser.TernaryContext ctx) {
			return new ConnectiveExpression(
				BooleanConnective.ITE,
				visit(ctx.condition),
				visit(ctx.truthy),
				visit(ctx.falsy)
			);
		}

		@Override
		public Expression visitBooleanConstant(FormulaParser.BooleanConstantContext ctx) {
			if (ctx.TRUE() != null) {
				return Atom.TRUE;
			}
			if (ctx.FALSE() != null) {
				return Atom.FALSE;
			}
			return null;
		}

		@Override
		public Expression visitArithmeticAtom(FormulaParser.ArithmeticAtomContext ctx) {
			IntExpressionVisitor visitor = new IntExpressionVisitor();
			return new ArithmeticAtom(
				ArithmeticAtom.Connective.fromOperator(ctx.op.getText()),
				asList(visitor.visit(ctx.left), visitor.visit(ctx.right))
			);
		}

		@Override
		public Expression visitEqualityAtom(FormulaParser.EqualityAtomContext ctx) {
			TermVisitor visitor = new TermVisitor();
			return new EqualityAtom(
				EqualityAtom.Connective.fromOperator(ctx.op.getText()),
				asList(visitor.visit(ctx.left), visitor.visit(ctx.right))
			);
		}
	}

	private static class DomainVisitor extends FormulaBaseVisitor<Domain> {
		@Override
		public Domain visitIntExpressionRange(FormulaParser.IntExpressionRangeContext ctx) {
			IntExpressionVisitor visitor = new IntExpressionVisitor();

			return new IntExpressionRange(
				visitor.visit(ctx.minimum),
				visitor.visit(ctx.maximum)
			);
		}

		@Override
		public Domain visitTermEnumeration(FormulaParser.TermEnumerationContext ctx) {
			return new Enumeration<>(wrap(ctx.terms()));
		}

		@Override
		public Domain visitPredicateEnumeration(FormulaParser.PredicateEnumerationContext ctx) {
			return new Enumeration<>(wrap(ctx.predicates()));
		}


		@Override
		public Domain visitIntExpressions(FormulaParser.IntExpressionsContext ctx) {
			return new Enumeration<>(wrap(ctx.intExpressions()));
		}
	}

	private static class PredicateVisitor extends FormulaBaseVisitor<Predicate> {
		@Override
		public Predicate visitPredicateConstant(FormulaParser.PredicateConstantContext ctx) {
			return new ConstantPredicate(ctx.getText());
		}

		@Override
		public Predicate visitPredicateVariable(FormulaParser.PredicateVariableContext ctx) {
			return new PredicateVariable(ctx.getText().substring(1));
		}
	}

	private static class TermVisitor extends FormulaBaseVisitor<Term> {
		@Override
		public Term visitTermConstant(FormulaParser.TermConstantContext ctx) {
			return new ConstantTerm(ctx.getText());
		}

		@Override
		public Term visitTermVariable(FormulaParser.TermVariableContext ctx) {
			return new VariableTerm(ctx.getText().substring(1));
		}
	}

	private static class IntExpressionVisitor extends FormulaBaseVisitor<IntExpression> {
		@Override
		public IntExpression visitAbsIntExpression(FormulaParser.AbsIntExpressionContext ctx) {
			return new IntUnaryConnectiveExpression(
				IntUnaryConnectiveExpression.Connective.ABS,
				visit(ctx.intExpression())
			);
		}

		@Override
		public IntExpression visitNegIntExpression(FormulaParser.NegIntExpressionContext ctx) {
			return new IntUnaryConnectiveExpression(
				IntUnaryConnectiveExpression.Connective.NEG,
				visit(ctx.intExpression())
			);
		}

		@Override
		public IntExpression visitBinaryIntExpression(FormulaParser.BinaryIntExpressionContext ctx) {
			return new IntBinaryConnectiveExpression(
				visit(ctx.left),
				IntBinaryConnectiveExpression.Connective.fromOperator(ctx.op.getText()),
				visit(ctx.right)
			);
		}

		@Override
		public IntExpression visitNumIntExpression(FormulaParser.NumIntExpressionContext ctx) {
			return new IntNumberExpression(Integer.parseInt(ctx.number.getText()));
		}

		@Override
		public IntExpression visitVarIntExpression(FormulaParser.VarIntExpressionContext ctx) {
			return new IntVariable(ctx.variable.getText().substring(1));
		}

		@Override
		public IntExpression visitParenthesizedIntExpression(FormulaParser.ParenthesizedIntExpressionContext ctx) {
			return visit(ctx.intExpression());
		}
	}

	private static class ArgVisitor extends FormulaBaseVisitor<Arg> {
		@Override
		public Arg visitArgTerm(FormulaParser.ArgTermContext ctx) {
			TermVisitor visitor = new TermVisitor();
			return visitor.visit(ctx.term());
		}

		@Override
		public Arg visitArgIntExpression(FormulaParser.ArgIntExpressionContext ctx) {
			IntExpressionVisitor visitor = new IntExpressionVisitor();
			return visitor.visit(ctx.intExpression());
		}
	}

	private static List<Arg> wrap(FormulaParser.ArgsContext ctx) {
		if (ctx == null) {
			return emptyList();
		}

		ArgVisitor visitor = new ArgVisitor();

		final List<Arg> terms = new ArrayList<>();
		do {
			FormulaParser.ArgContext arg = ctx.arg();
			terms.add(visitor.visit(arg));
		} while ((ctx = ctx.args()) != null);

		return terms;
	}

	private static List<Term> wrap(FormulaParser.TermsContext ctx) {
		if (ctx == null) {
			return emptyList();
		}

		TermVisitor visitor = new TermVisitor();

		final List<Term> terms = new ArrayList<>();
		do {
			FormulaParser.TermContext term = ctx.term();
			terms.add(visitor.visit(term));
		} while ((ctx = ctx.terms()) != null);

		return terms;
	}

	private static List<Predicate> wrap(FormulaParser.PredicatesContext ctx) {
		if (ctx == null) {
			return emptyList();
		}

		PredicateVisitor visitor = new PredicateVisitor();

		final List<Predicate> preds = new ArrayList<>();
		do {
			FormulaParser.PredicateContext predicate = ctx.predicate();
			preds.add(visitor.visit(predicate));
		} while ((ctx = ctx.predicates()) != null);

		return preds;
	}

	private static List<IntExpression> wrap(FormulaParser.IntExpressionsContext ctx) {
		if (ctx == null) {
			return emptyList();
		}

		IntExpressionVisitor visitor = new IntExpressionVisitor();

		final List<IntExpression> ints = new ArrayList<>();
		do {
			FormulaParser.IntExpressionContext predicate = ctx.intExpression();
			ints.add(visitor.visit(predicate));
		} while ((ctx = ctx.intExpressions()) != null);

		return ints;
	}
}
