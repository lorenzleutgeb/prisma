#show pos/2.

% INSTANCE

quant(1..5).

forall(1). exists(2). forall(3). exists(4). forall(5).

related(4,5).

pos(1,2) pos(2,3) pos(3,1) pos(4,4) pos(5,5)

forall(3). forall(1). exists(2). exists(4). forall(5).

% mustBe(X,Y) if Y depends on X.
mustBe(2,5).

% GUESS
{ pos(X,Y) : quant(X) } = 1 :- quant(Y).

% CHECK

% Exactly one quantifier at every position.
:- pos(X,A), pos(X,B), A != B.
:- pos(X,A), pos(Y,A), X != Y.

hasPos(X) :- quant(X), pos(X,Y), quant(Y).
:- quant(X), not hasPos(X).

% Prevent that two related quantifiers are moved over each other. 
:- related(X,Y), was(X,Y), willBe(Y,X).

% Prevent errors in domain dependency.
:- mustBe(X,Y), willBe(Y,X).

% OPTIMIZATION

bad(X,Y) :- forall(X), exists(Y), willBe(Y,X).
badness(S) :- S = #sum{ X,Y : bad(X,Y) }.
#minimize { S : badness(S) }.

% AUXILIARY

% Symmetry
related(A,B) :- related(B,A).

willBe(X,Y) :- pos(X,A), pos(Y,B), A < B.
was(X,Y) :- quant(X), quant(Y), X < Y.

-------------------------------------------
SAT encoding: Let N be the number of quantifiers.

% Every quantifier is either universal or existential.
%(forall #q in [1...N] universal(#q) ^ existential(#q))

% Every quantifier must be moved to some position.
(forall #q in [1...N] exists #p in [1...N] moveTo(#q,#p))

% At one position there can be at most one quantifier.
(forall #p in [1...N] forall #q1 in [1...N] forall #q2 in [#q1+1...N] (moveTo(#q1,#p) => ~moveTo(#q2,#p)))

% For any pair of quantifiers, the left one cannot depend on the right one.
(forall #l in [1...N] forall #r in [#l+1...N] forall #ql in [1...N] forall #qr in [1...N] ((moveTo(#ql,#l) & moveTo(#qr,#r)) => ~dependsOn(#l,#r)))

% For any pair of quantifiers, if they are related, then their order is not allowed to change.
(forall #l in [1...N] forall #r in [#l+1...N] forall #ql in [1...N] forall #qr in [1...N] ((moveTo(#ql,#l) & moveTo(#qr,#r)) => ~related(#ql,#qr))
