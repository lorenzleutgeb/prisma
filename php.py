#!/usr/bin/env python3

# Team White

from sys import argv, exit

if len(argv) != 3:
    print('Need exactly two arguments!')
    exit(1)

pigeons = int(argv[1])
holes = int(argv[2])

print("forall #p in [1..." + str(pigeons) + "] exists #h in [1..." + str(holes) + "] " + "a(#p,#h)")
print("forall #p1 in [1..." + str(pigeons - 1) + "] forall #p2 in [#p1+1..." + str(pigeons) + "] forall #h in [1..." + str(holes) + "] (a(#p1,#h) -> ~a(#p2,#h))")
print("forall #h1 in [1..." + str(holes - 1) + "] forall #h2 in [#h1+1..." + str(holes) + "] forall #p in [1..." + str(pigeons) + "] (a(#p,#h1) -> ~a(#p,#h2))")
