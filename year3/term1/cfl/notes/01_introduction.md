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
