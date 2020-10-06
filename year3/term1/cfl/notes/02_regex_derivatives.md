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
(nullable == can match the empty string)

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

* der a r =

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

    der b ((a.b)+b)* => (der b ((a.b)+b)) . r
                        ((der b (a.b)) + der b b) . r
                        ((der b a) . b) + (der b b) . r
                        (0 . b) + 1 . r
                        0 + 1 . r
                        1 . r
                        r
 ```

* der c r =

 ```re
    der c ((a.b)+b)* => (der c ((a.b)+b)) . r
                        (der c (a.b) + der c (b)) . r
                        (der c (a.b) + 0) . r
                        ((der c (a) . b) + 0) . r
                        ((0 . b) + 0) . r
                        (0 + 0) . r
                        0 . r
                        0
 ```

 ---

# The Brzozowski Algorithm

The complete matcher - by building successive derivatives, then testing the final expressions (which gives the result for the intial).

 `matcher r sa == nullable(ders s r)`

 an example, does r1 match abc?

  1) Build derivative of a and r1 `(r2 = der a r1)`
  2) Build derivative of b and r2 `(r3 = der b r2)`
  3) Build derivative of c and r3 `(r4 = der c r3)`
  4) The string is exhausted: `nullable(r4)`, test whether r4 can recognise the empty string

Output: The result of this test (`true` or `false`)

Why does this work?

The idea of the algorithm - If we want to recognise the string `abc` with regular expression `r1` then:

* `Der a (L(r1))`
* `Der b (Der a (L(r1)))`
* `Der c (Der b (Der a (L(r1))))`
* Finally, we test whether the empty string is in this set; same for `Ders abc(L(r1))`

i.e. find all strings that start with an a, then take the semantic derivative - 'chop off' the a for each of these strings.
Repeat this for b and c on each successive result.

If the empty string is in the resulting set, the matching word must have been in the language of r1, L(r1). (Otherwise it does not match)

The matching algorithm works similarly. just over regular expressions instead of sets.

The matcher:

matcher(r, s) = Boolean => nullable(ders(s, r))

i.e. a match is the same as whether the derivate of the string w.r.t. r is nullable (can match the empty string)

---

The idea with derivatives

Input: string `abc` and regular expression `r`

 1) der a r
 2) der b (der a r)
 3) der c ( der b (der a r))

Finally check whether the last regular expressions can match the empty string.

Same principle as above, but for expressions. If the final derivative matches the empty string, then the expression r1 mnatches the input string `abc`.
(i.e. if the result is nullable)

---

Algorithm 2 Section

Definition for n-times

(We represent n-times a{n} as a sequence regular expressions: 0: 1, 1: a, 2: a.a, 3:a.a.a, etc. - a tree)

This problem is happens also with a? being represented as a+1.

Extending our regular expressions with explicit constructors, to avoid expanding n-times

```re

 r ::= ...
   | r^{n}
   | r^?
```

What is their meaning?
What are the cases for nullable and der?

`der` for n-times:

r{n}

n = 0   ==> 1     | nullable: true
n = 1   ==> r.r   | nullable: nullable(r)
n = 1   ==> r.r.r | nullable: nullable(r)

if n=0 then true, else we have to ask whether nullable of r holds (r is nullable)

finding a pattern (building the derivative of a sequence):

```re
n = 0 : 1 | der c 1 = 0
n = 1 : r | der c r
n = 2 : r .r |
der c (r.r) == if nullable(r):
               then: (der c r) . r + der c r
               else: (der c r) . r

            === (der c r) . r

The simplification works, since if we assume nullable(r) then the expression ends up the same either way -

In order for
  der c (r.r) ==  (der c r) . r + der c r == (der c r) . r
to hold:

(der c r) . r + der c r == (der c r) . r + (der c r) . 1
                        == (der c r) . (r+1)    # factorising
                        == (der c r) . r        # since r is nullable and can already match empty string
                                                 (otherwise we couldn't simplify)
```

---

```re
   r{n}     der
=========|========
n = 0: 1   0
n = 1: r   (der c r)
n = 2: r   (der c r) . r
n = 3: r   (der c r) . r .r
```

`nullable(r{n}) == if n = 0 then true else nullable(r)`

`der c (r{n})   == if n = 0 then 0    else (der c r) . r{n - 1}` (recursively defined)

showing 3 case, and every following case as a result

```re

der c (r.r.r) == if nullable(r)
                 then (der c r) . r . r + der c (r.r)
                 else (der c r) . r . r

        === else (der c r) . r . r

Simplifying:
    (r IS nullable in this branch)
    ...  then (der c r) . r . r + der c (r.r)
      =  then (der c r) . (r . r + r)          # factorising:
      =  then (der c r) . (r . (r + 1))        # we don't need + 1 since r can already match 1:
      =  then (der c r) . (r . r)              # rearrange:
      =  then (der c r) . r . r
```

---

Algorithm 3

Examples:

```re
 r == ((a.b)+b)*
 der a r  = ((1.b)+0).r        (derivate gives junk: this can be simplified to b.r)
 der b r  = ((0.b)+1).r        (... r)
 der c r  = ((0.b)+0).r        (... 0)

 These extraneous 0s and 1s will accumulate over time growing the expressions' trees massively.
```

Simplification rules: (For eliminating junk!)

```re

r+0  => r
0+r  => r
r.1  => r
1.r  => r
r.0  => 0
0.r  => 0
r+r  => r

```

Introduce some simplification function, `simp`.

e.g.

```scala
def ders(s: List[Char], r: Rexp) : Rexp = s match{
    case Nil  => r
    case c::s => ders(s, simp(der(c, r)))
}
```

We must simplify from the inside out - match the expressions, test whether it is alternative or sequence, THEN simplify and match.

```scala

def simp(r: Rexp) : Rexp = r match{
    case ALT(r1, r2)=>{
        (simp(r1), simp(r2)) match {
            case(ZERO, r2s)=>r2s
            case(r1s, ZERO)=>r1s
            case(r1s, r2s)=>
                if(r1s == r2s) r1s
                else ALT(r1s, r2s)
        }
    }

    case SEQ(r1, r2)=>{
        (simp(r1), simp(r2)) match {
            case(ZERO, _)=>ZERO
            case(_, ZERO)=>ZERO
            case(ONE, r2s)=>r2s
            case(r1s, ONE)=>r1s
            case(r1s, r2s)=>SEQ(r1s, r2s)
        }
    }

    case r=>r // no simplification in necessary
}

```

---

What is good about this algorithm?

* It extends to most regular expressions, for example `~r`.
* It is easy to implement in a functional language
* The algorithm is already quite old (there is new work to be done using it as a tokenizer)
* We can prove its correctness!

---

Negation of regular expressions

* `~r` (everything that r *cannot* recognise)
* `L(~r) == UNIV - L(r)` (UNIV is all the strings, L(r) is all the strings r can match)
* `nullable(~r) == not(nullable(r))`
* `der c (~r) == ~(der c r)`

Used often for recognising comments:

```re
 /.*.(~([a-z]*.*./.[a-z]*)).*./
```

---

The specification for matching:
`For all r s, matcher(s, r) iff s ∈ L(r)`

Nullable and der:

`nullable(r) iff [] ∈ L(r)` for all r.
`L(der c r) = Der c (L(r))` for all r, c.

---

Proofs about Rexps
If we want to prove something, say a property P(r), for all regular expressions `r`, then...

* P holds for 0, 1, and c
* P holds for r1+r2, assuming P holds for r1 and r2
* P holds for r1.r2, assuming P holds for r1 and r2
* P holds for r*, assuming P holds for r

Assume `P(r)` is the property:
`nullable(r) iff [] ∈ L(r)`

---

CW
