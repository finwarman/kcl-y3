# Lecture 2 - Regular Expressions and Derivatives

Implementing an efficient regular expression matcher (that can cope with the 'evil' catastrophic backtracking expressions).

When are two regular expressions equivalent?

```re
  r1 == r2  ===  L(r1) = L(r2)
```

i.e. two expressions are equivalent if they have the same language.

Example concrete equivalences:

```re
(a+b)+c === a+(b+c)
    a+a === a
    a+b === b+a
(a.b).c === a.(b.c)
c.(a+b) === (c.a)+(c.b)
```

Corner cases:

```re
a.0 !== a
a+1 !== a
  1 === 0*
 1* === 1
 0* !== 0
```

Simplification Rules:

```re
r+0 === r
0+r === r
r.1 === r
1.r === r
r.0 === 0
0.r === 0
r+r === r
```

Simplification example:

```re
((0.b)+0).r => ((0.b)+0).r    # 0.r -> 0
            =  (0+0) . r      # r+r -> r, 0+0 -> 0
            =  0.r            # Simplified expression!
```

---

Semantic Derivative

The **Semantic Derivative** (`Der`) of a language w.r.t. to a character _c_:
(works over languages / sets of strings)

`Der(c, A) === {s | c::s ∈ A}`
i.e. the derivative of language _A_ w.r.t. character _c_ is the set of every string `s` where `c::s` (strings beginning with 'c' and tailing with 's') is in language A.

Note: In (Scala) pattern matching, the double colon (`::`) is the _cons_ operator; in `x::xs` x represents the head of the list, and xs the tail.

Example:

For `A = {foo, bar, frak}`, the derivatives w.r.t various characters are:

```re
Der(f, A) == {oo, rak}
Der(b, A) == {ar}
Der(a, A) == {}
```

The definition can be extended to strings:
`Der(s, A) == {s' | s@s' ∈ A}`

e.g. the derivate of language _A_ w.r.t. *string* _s_ contains every tail string `s'` where `s@s'` (strings starting with `s` followed by `s'`) is in language A.

(Recall `@` is the string concatenation operator)

---

The specification for matching:

A regular expressions _r_ matches a string _s_ provided: `s ∈ L(r)`.

i.e. An expressions matches a string only if that string is in the language of the expression.

The purpose of this lecture is to decide this problem (the matching problem) as fast as possible.

---

Algorithm(s):

Brzozowski's Algorithm:
Whether a regular expressions can match the empty string:

```re
nullable(0) == false                              # L(0) is the empty language {}
nullable(1) == true                               # L(1) is the empty string language {[]}
nullable(c) == false                              # a single char is not nullable
nullable(r*) == true                              # r* (0 or more) of an expressions is nullable
nullable(r1+r2) == nullable(r1) \/ nullable(r2)   # r1+r2 is nullable if r1 or r2 are nullable
nullable(r1.r2) == nullable(r1) /\ nullable(r2)   # r1.r2 is nullable if r1 and r2 are nullable
```

---

The derivative of a regular expressions -

If r matches the string c::s, what is a regular expression that matches just s?

Der(c, r) gives the answer [Brzozowski 1964]

Rexp Derivative Definitions:

```re
char:
der c (0)     == 0
der c (1)     == 0
der c (d)     == if c=d then 1 else 0
der c (r1+r2) == der c (r1) + der c (r2)
der c (r1.r2) == if nullable(r1):
                   then: (der c r1) . r2 + der c r2
                   else: (der c r1) . r2
der c (r*)    == (der c r) . (r*)

string:
ders [] r     == r
ders (c::s) r == ders s (der c r)
```

Examples:

Given `r == ((a.b)+b)*` what is:

* der a r = (TODO: confirm it should be 'r' not 'r*')

 ```re
    der a ((a.b)+b)* => [der c (r*) = (der c r) . r*]
                        (der a ((a.b)+b)) . r
                     => [der c (r1+r2) == der c (r1) + der c (r2)]
                        ((der a (a.b)) + (der a b)) . r
                     => [der c (r1.r2) == (der c r1) . r2 (+ der c r2 if nullable(r1)]
                        [nullable(a) = false]
                        (((der a a) . b) + (der a b)) . r
                     => ((1 . b) + (der a b) . r
                     => ((1 .b) + 0) . r
 ```

* der b r =

 ```re
 ```

* der c r =

 ```re
 ```
