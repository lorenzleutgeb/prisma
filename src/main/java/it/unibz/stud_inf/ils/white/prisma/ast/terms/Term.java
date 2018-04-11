package it.unibz.stud_inf.ils.white.prisma.ast.terms;

import it.unibz.stud_inf.ils.white.prisma.ast.ConstantTerm;
import it.unibz.stud_inf.ils.white.prisma.ast.Groundable;

public abstract class Term extends Arg<Term> implements Groundable<ConstantTerm, Term> {
}
