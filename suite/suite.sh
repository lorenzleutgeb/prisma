#!/bin/bash

# List all teams whose implementations should be compared here.
TEAMS=(blue white)

# For every team write a function that executes their implementation.
# It should be named like the team and take exactly four parameters:
# - The name of the input file which contains a formula in high level
#   language.
# - The name of the output file where the CNF should be written to in
#   DIMACS fromat.
# - The name of the output file to redirect standard output to (if
#   applicable).
# - The name of the output file to redirect standard error to (if
#   applicable).

function blue() {
	yunis cnf < $1 > $2 2> $4
}

function white() {
	prisma -mode DIMACS $1 $2 > $3 2> $4
}

# This script generates a CSV-like output with the following columns:

echo '# instance, vars, clauses, time, result'

# Note that the columns vars, clauses, result are per-team.

TIMEFORMAT='%3R'

for f in $1
do
	b=$(basename $f .bool)
	echo -ne "$b\t"
	for team in ${TEAMS[*]}
	do
		t=$((time $team $f $team/$b.cnf $team/$b.out $team/$b.err) 2>&1 | tr ',' '.')
		size=$(grep 'p cnf ' $team/$b.cnf | cut -b7- | tr ' ' '\t')
		echo -ne "$size\t$t\t"
		minisat -cpu-lim=5 $team/$b.cnf $team/$b-minisat.out > /dev/null 2> $team/$b-minisat.err
		result=$(head -n 1 $team/$b-minisat.out)
		echo -ne "$result\t"
	done
	echo
done
