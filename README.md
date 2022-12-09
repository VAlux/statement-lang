# Statement lang 
----

## Given the DSL example source:
```
Show *Q2 when Q1.A1 == true
Hide *Q3 when Q1.A3 == false
Set *Q5.A1 to 3
Set *Q5.A2 to Q3.A2
Set *Q5.A3 to 10 when Q5.A2 == Q5.A1
Set *Q5.A3 to 10 when Q5.A2 == Q5.A1 and Q5.A3 == Q5.A4 or Q5.A6 == Q5.A8
```

## The following statements will be parsed:

```scala
Statement(List(Show, Pointer(Q2), When, Val(Q1.A1), Eq, Val(true)))
Statement(List(Hide, Pointer(Q3), When, Val(Q1.A3), Eq, Val(false)))
Statement(List(Set, Pointer(Q5.A1), To, Val(3)))
Statement(List(Set, Pointer(Q5.A2), To, Val(Q3.A2)))
Statement(List(Set, Pointer(Q5.A3), To, Val(10), When, Val(Q5.A2), Eq, Val(Q5.A1)))
Statement(List(Set, Pointer(Q5.A3), To, Val(10), When, Val(Q5.A2), Eq, Val(Q5.A1), And, Val(Q5.A3), Eq, Val(Q5.A4), Or, Val(Q5.A6), Eq, Val(Q5.A8)))
```

## Which can be translated in the following AST:
```scala
Predicate(Show(Pointer(Q2)),Eq(Value(Q1.A1),Value(true)))
Predicate(Hide(Pointer(Q3)),Eq(Value(Q1.A3),Value(false)))
Assignment(Pointer(Q5.A1),Value(3))
Assignment(Pointer(Q5.A2),Value(Q3.A2))
Predicate(Assignment(Pointer(Q5.A3),Value(10)),Eq(Value(Q5.A2),Value(Q5.A1)))
Predicate(Assignment(Pointer(Q5.A3),Value(10)),And(Eq(Value(Q5.A2),Value(Q5.A1)),Or(Eq(Value(Q5.A3),Value(Q5.A4)),Eq(Value(Q5.A6),Value(Q5.A8)))))
```

## And, for example, can be interpreted as series of text statements
```
if (Q1.A1 == true) then Show *Q2
if (Q1.A3 == false) then Hide *Q3
*Q5.A1 = 3
*Q5.A2 = Q3.A2
if (Q5.A2 == Q5.A1) then *Q5.A3 = 10
if (Q5.A2 == Q5.A1 and Q5.A3 == Q5.A4 or Q5.A6 == Q5.A8) then *Q5.A3 = 10
```

## Example of interpretation into GraphVis:
```graphviz
PREDICATE0 [label="PREDICATE"]
ROOT -- PREDICATE0
EQ1 [label="EQ"]
PREDICATE0 -- EQ1
EQ1 -- "Q1.A1"
EQ1 -- "true"
PREDICATE0 -- "SHOW2 (Pointer(Q2))"

PREDICATE0 [label="PREDICATE"]
ROOT -- PREDICATE0
EQ1 [label="EQ"]
PREDICATE0 -- EQ1
EQ1 -- "Q1.A3"
EQ1 -- "false"
PREDICATE0 -- "HIDE2 (Pointer(Q3))"

ASSIGNMENT0 [label="ASSIGNMENT"]
ROOT -- ASSIGNMENT0
ASSIGNMENT0 -- "*(Q5.A1)"
ASSIGNMENT0 -- "3"

ASSIGNMENT0 [label="ASSIGNMENT"]
ROOT -- ASSIGNMENT0
ASSIGNMENT0 -- "*(Q5.A2)"
ASSIGNMENT0 -- "Q3.A2"

PREDICATE0 [label="PREDICATE"]
ROOT -- PREDICATE0
EQ1 [label="EQ"]
PREDICATE0 -- EQ1
EQ1 -- "Q5.A2"
EQ1 -- "Q5.A1"
ASSIGNMENT2 [label="ASSIGNMENT"]
PREDICATE0 -- ASSIGNMENT2
ASSIGNMENT2 -- "*(Q5.A3)"
ASSIGNMENT2 -- "10"

PREDICATE0 [label="PREDICATE"]
ROOT -- PREDICATE0
AND1 [label="AND"]
PREDICATE0 -- AND1
EQ2 [label="EQ"]
AND1 -- EQ2
EQ2 -- "Q5.A2"
EQ2 -- "Q5.A1"
OR3 [label="OR"]
AND1 -- OR3
EQ4 [label="EQ"]
OR3 -- EQ4
EQ4 -- "Q5.A3"
EQ4 -- "Q5.A4"
EQ5 [label="EQ"]
OR3 -- EQ5
EQ5 -- "Q5.A6"
EQ5 -- "Q5.A8"
ASSIGNMENT2 [label="ASSIGNMENT"]
PREDICATE0 -- ASSIGNMENT2
ASSIGNMENT2 -- "*(Q5.A3)"
ASSIGNMENT2 -- "10"
```
# Run:

1. Install SBT from https://www.scala-sbt.org
2. Go to the project root folder
3. Run `sbt run` command from terminal