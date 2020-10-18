# Automata & Regular Languages

## Automata -

A deterministic finite automaton, DFA, consists of:

* an alphabet Σ
* a set of states Qs
* one of these states is the start state, Q0
* some states are accepting states F
* there is a transition function δ
  - this takes a state as argument, and a character, and produces a new states; it may not be defined everywhere and is therefore a partial function.

Automaton: `A(Σ, Qs, Q0, F, δ)`

The start state _can_ be an accepting state, and it is possible that there is _no_ accepting state.

All states might also be accepting - this does not necessarily mean that all string are accepted!

```txt
  start ---> Q0 --a--> Q1 --a--> (Q4) <=a,b>
          /|   \ b     | b        /\ a
         /      \|    \/          |
        |   <b=>   Q2   ---a--> Q3
         \________________________/
```

For this automaton, δ is the function:

```txt
(Q0, a) -> Q1   (Q1, a) -> Q4   (Q4, a) -> Q4
(Q0, b) -> Q2   (Q1, b) -> Q2   (Q4, b) -> Q4 ...
```

---

**Accepting a string**

Given `A(Σ, Qs, Q0, F, δ)`, you can define

```txt
δ̂(Q, [])   == Q
δ̂(Q, c::s) == δ̂(δ(Q,c), s)
```

Where `δ̂` is the transition function for _strings_ (`δ` for chars.)

Is a string `s` accepted by `A`?

`δ̂(Q0, s) ∈ F`

i.e. is there some set of character transitions (does there exist a string transition function) from starting state `Q0` to a finishing state (`q ∈ F`) for `s`.

---

**Regular Languages**

* A _language_ is a set of strings
* A _regular expression_ specifies a (regular) language
* A language is _regular_ iff there exists a regular expression that recognises all its strings
* this is equivalent to saying: a langauge is regular iff there exists a DFA that recognises all its strings.

**Not** all languages are regular, e.g. `a{n}b{n}` is not.

---

**Non-Deterministic Finite Automata**

`N(Σ, Qs, Qs0, F, ρ)`

An NFA consists of:

* a finite set of states, Qs
* _some_ of these are the start states, Qs0
* some states are accepting states
* there is a transition _relation_, ρ:
  `(Q1, a)->Q2, (Q1, a)->Q3, ...`

e.g. NFA examples

```txt
         |b            |a,b
         \/ .---a---\| \/
start --> Q0  <--b--  Q1 --a--> (Q2)
          ._________a___________/|
```

For expression `(.*)a(.{n})bc`
(star-transitions accept any character)

```txt
        \*/
start -> 0 -a-> 1 -*-> 2 -*> ... -*-> .. -*-> n+1 -b-> n+2 -c-> (n+3)
             .________________n________________.
```

---

Thompson Construction: Rexp to εNFA:

Constructing a regular expression into a corresponding automaton.

εNFAs allow for 'silent transitions', ε, which allows for moving between states without consuming a character.

Epsilon εNFA (empty string) example:

```txt
        /a\
         Q1
         /\
         |ε
start -> Q0 <=a>
         |ε
         \/
        (Q2)
        \b/
```

εNFAs can be translated to NFAs immediately, by replacing all transitions of the form:

`q ε-> ... ε-> a-> ε-> ... ε-> q'` with
`q a-> q'`

And translating (knowing εNFA->NFA), regular expressions into equivalent NFAs.

Consider simple regular expressions **0**, **1**, c into NFAs:

```txt
 0   start->()
 1   start->(())
 c   start->()-c->(())

 ()   = intermediate state
 (()) = accepting state
```

For `r1 . r2` we obtain an εNFA by connecting the accepting states of the NFA for `r1` with ε-transitions, to the starting states of the NFA for `r2`. (And make the previous accepting states from r1 non-accepting).

We then immediately translate into an NFA.

For `r1 + r1` the case is different: Given NFAs for `r1` and `r2` we compose the transition functions (knowing that states of each NFA are distinct).
The starting states and accepting functions must also be combined appropriately.

(Function composition: apply one function to the result of the other)

e.g.

```txt
         r1                         r1+r2
start->o  ...  ()         start->o       ()
start->o       ()         start->o  ...  ()

         r2          =>
               ()                        ()
start->o ...   ()         start->o  ...  ()
               ()                        ()

```

For the `*-case` we have a NFA for `r`, and connect its accepting states to a new starting state via ε-transitions. This new starting state is also an accepting state since `r*` can recognise the empty string.

```txt
         r                              r*
                                   /-----ε------
start->o ... ()                     ε-> o      o
start->o     ()    =>   start -> () ε-> o  ... o
             ()                   \------ε----/
                                   \-----ε---- o
```

Scala code for Thompsons:
This calculates a NFA from regular expressions.

```scala
def thompson(r: Rexp) : NFAt = r match {
    case ZERO        => NFA_ZERO()
    case ONE         => NFA_ONE()
    case CHAR(c)     => NFA_CHAR(c)
    case ALT(r1, r2) => NFA_ALT(thompson(r1), thompson(r2))
    case SEQ(r1, r2) => NFA_SEQ(thompson(r1), thompson(r2))
    case STAR(r1)    => NFA_STAR(thompson(r1))
}
```

The time taken for this BFS shows why common languages matcher implementation are so slow!

---

### Subset Construction

A technique for addressing sluggish NFAs. Transforming NFAs into DFAs. (Finding automata accepting the same language)

TODO
