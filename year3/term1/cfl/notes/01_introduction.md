# Lecture 1 - Introduction

## Books / Reading Resources

* [The Art of Assembly](http://flint.cs.yale.edu/cs422/doc/art-of-asm/pdf/)
* [Basics of Compiler Design](http://hjemmesider.diku.dk/~torbenm/Basics/)

## Module Objective

To write a compiler :)

```
              +-----------+  +-----------+  +----------+
   input      |           |  |           |  |          |    binary
  program ---->  lexer    +-->  parser   +--> code gen +---> code
              |           |  |           |  |          |
              +-----------+  +-----------+  +----------+
```

## Module Structure

 1) [Introduction & Languages](01_introduction.md)
 2) Regular Expressions & Derivatives
 3) Automata & Regular Languages
 4) Lexing & Tokenising
 5) Grammars & Parsing
 6) While-Language
 7) Compilation & the JVM
 8) Compiling Functional Languages
 9) Optimisations
 10) LLVM (Compiler Infrastructure)

## Module Assessment

* Examination (40%) -
  * 10% Mid-Term after Reading Week
  * 30% Final Exam in Jan.
* Weekly Engagement (10%)
* Coursework (45%) - Can use **any** programming language you like
  * 5% Matcher
  * 8% Lexer
  * 10% Parser / Interpreter
  * 10% JVM Compiler
  * 12% LLVM Compiler
* Homework, Weekly (0%)
  * Uploaded to KEATS, send answers via email (individually responded)
  * **All question in exams will be in homework!**

---

## Why study compilers

* **Growing Hardware Complexity** -
Not just increasing clock speed on processors: multiple cores, vector units, crypto accelerators.
DSPs, GPUs, ARM big.LITTLE, etc.

* **Safety and Security** -
Lots of security disasters associated with critical applications and low level languages.
For new code, we want to write in safer and higher-level languages (e.g. to avoid bad memory allocation), while still compiling to safe & optimised assembly.

---

## Topic Structure & Overview

### Lectures 1-5

##### Transforming strings into structured data

**Lexing** - Recognising "Words" (based on regular expressions)
**Parsing** - Recognising "Sentences"

### Lectures 5-10

##### Code generation for small imperative & small functional languages

**Interpretes** - Directly runs a program
**Compilers** - Generate JVM code and LLVM-IR Code

---

## Definitions & Examples

#### Inductive definition of basic regular expressions

```
r  ::=  0      nothing
     |  1      empty string / "" / []
     |  c      character
     |  r₁+r₂  alternative / choice
     |  r₁.r₂  sequence
     |  r*     star (zero or more)
```

#### Familiar regular expressions

email address: /`[a-z0-9_\.-]+@[a-z0-9\.-]+.[a-z\.]{2,6}`/

```
COMMON REGEX NOTATION:
\               escape following char (e.g. \. or \\)
^               match at start of string
$               match at end of string
r*              matches 0 or more times
r+              matches 1 or more times
r?              matches 0 or 1 times
r{n}            matches exactly n times
r{n,m}          matches at least n and at most m times
[...]           matches any single character inside brackets
[^...]          matches any single character not inside brackets
a-z A-Z 0-9     character ranges (e.g. [a-z]: 'any lowercase letter in the alphabet')
\d              any digit (i.e. [0-9])
\s              any whitespace character ' ', '\t', etc.
\b              word boundary
.               matches any character except newline '\n'
(r)             capturing group - groups expressions and remembers matches text
```

---

#### Evil Regular Expressions

Seemingly innocent examples: `(a*)*b` and `[a?]{n}[a]{n}`.
Try matching these on strings with the form:
`a, aa, aaa, aaaa, aaaaa, a...a, a^n`

In common programming language implementations [Python, Ruby, Java 8], matching time grows exponentially with input size (n).

e.g. for `(a*)*b`:

```
t    ┌───────────────────────────────────────┐
i  30│                                  *    │
m    │                                  *    │
e  25│                           Python *    │
     │                                  *    │
i  20│                                 *     │
n    │                                 *     │
   15│                                 *     │
s    │                                 *     │
e  10│                                *      │
c    │                               *       │
o   5│                             **        │
n    │                          ***          │
d   0*---*-----*------*--*-*****             │ (input size)
s    └───────────────────────────────────────┘ n
     0   5    10    15    20    25    30   35
```

*Next Lecture* (`2, Regular Expressions & Derivatives`) we will show how to implement this such that a match can be completed in under 5 seconds for even values of `n` > `4x10^6`.

#### Regular Expression Denial of Service (ReDOS)

This is a problem for code editors, forums, network intrusion detection systems, etc.

Also known as **Catastrophic Backtracking**, some examples:

```
[a?]{n}[a]{n}
(a*)*b
([a‐z]+)*
(a+aa)*
(a+a?)*
```

(<https://vimeo.com/112065252>)

Real world example (Cloudflare): <https://blog.cloudflare.com/details-of-the-cloudflare-outage-on-july-2-2019/>
guilty expression:
``?:(?:\"|'|\]|\}|\\|\d|(?:nan|infinity|true|false|null|undefined|symbol|math)|\`|\-|\+)+[)]*;?((?:\s|-|~|!|{}|\|\||\+)*.*(?:.*=.*)))``

---

#### Languages & Strings

**Strings** are just lists of characters, e.g. "hello" / `[h,e,l,l,o]` / *hello*
The empty string: `[]`, or `""` acts as the additive identity (like 0 for natural/whole numbers).

**Languages** are sets of strings, e.g. `{[], hello, foobar, a, abc}`

**Concatenation of Strings and Languages**

String Concatenation: `s₁ @ s₂`:

* `foo @ bar = foobar`
* `baz @ [] = baz`

Language Concatenation: `L₁ @ L₂`:

`A @ B` ≡ `{s₁ @ s₂ | s₁ ∊ A  ^  s₁ ∊ B}`
(`≡` or `≝` signifies an identity, equal to by definition)

The concatenation of two languages `A` and `B` is the set of all strings `s₁@s₂` where `s₁∊A` and `s₁∊B`.
This means `A@B` consists of all possible strings formed by pairing one string from `A` with another from `B`.

Examples:

* `A@{[]} = A` (concat. Language of only empty string '[]' - additive identity)
* `A@∅    = ∅` (concat. Empty Language '∅')
* `A = {hello} and B = {world}, A@B = {helloworld}`
and with languages including empty string `[]`:
* `A = {[], hello} and B = {[], world}, A@B = {hello, world, helloworld}`

If the two given languages are regular, the resulting language is also regular.

---

#### Meaning & Matching

**The meaning of a regex**:

---

#### Operators

Good reference guide (Union, Concatenation, Kleene Closure): <https://courses.engr.illinois.edu/cs373/sp2013/Lectures/lec06.pdf>

---

#### Questions
