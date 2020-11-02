// Coursework 2 - A simple lexer inspired by work of Sulzmann & Lu
//==================================================

// regular expressions including records
abstract class Rexp
case object ZERO extends Rexp
case object ONE extends Rexp
case class CHAR(c: Char) extends Rexp
case class ALT(r1: Rexp, r2: Rexp) extends Rexp
case class SEQ(r1: Rexp, r2: Rexp) extends Rexp
case class STAR(r: Rexp) extends Rexp
case class PLUS(r: Rexp) extends Rexp
case class OPTIONAL(r: Rexp) extends Rexp
case class NTIMES(r: Rexp, n: Int) extends Rexp
case class RANGE(chars: Set[Char]) extends Rexp
case class RECD(x: String, r: Rexp) extends Rexp

// values
abstract class Val
case object Empty extends Val
case class Chr(c: Char) extends Val
case class Sequ(v1: Val, v2: Val) extends Val
case class Left(v: Val) extends Val
case class Right(v: Val) extends Val
case class Stars(vs: List[Val]) extends Val
case class Rec(x: String, v: Val) extends Val
case class Plus(vs: List[Val]) extends Val
case class NTimes(vs: List[Val]) extends Val
case class Opt(v: Val) extends Val
// N.B. Range is covered by Chr

// convenience for typing in regular expressions
def charlist2rexp(s : List[Char]): Rexp = s match {
  case Nil    => ONE
  case c::Nil => CHAR(c)
  case c::s   => SEQ(CHAR(c), charlist2rexp(s))
}
implicit def string2rexp(s : String)      : Rexp = charlist2rexp(s.toList)

implicit def charset2rexp(cs : Set[Char]) : Rexp = RANGE(cs)
implicit def char2rexp(c: Char)           : Rexp = CHAR(c)

implicit def RexpOps(r: Rexp) = new {
  def | (s: Rexp) = ALT(r, s)
  def %           = STAR(r)
  def ~ (s: Rexp) = SEQ(r, s)
}

implicit def stringOps(s: String) = new {
  def | (r: Rexp) = ALT(s, r)
  def | (r: String) = ALT(s, r)
  def % = STAR(s)
  def ~ (r: Rexp) = SEQ(s, r)
  def ~ (r: String) = SEQ(s, r)
  def $ (r: Rexp) = RECD(s, r)
}

// == FUNCTIONS ==

// whether an expression can match the empty string
def nullable(r: Rexp) : Boolean = r match {
  case ZERO             => false
  case ONE              => true
  case CHAR(_)          => false
  case ALT(r1, r2)      => nullable(r1) || nullable(r2)
  case SEQ(r1, r2)      => nullable(r1) && nullable(r2)
  case STAR(_)          => true
  case NTIMES(r, n)     => if (n == 0) true else nullable(r)
  case PLUS(r)          => nullable(r)
  case OPTIONAL(r)      => true
  case RANGE(chars)     => false
  case RECD(_, r1)      => nullable(r1)
}

// the derivate of an expression w.r.t a char
// (for expression matching c::s, resulting der matches s)
def der(c: Char, r: Rexp) : Rexp = r match {
  case ZERO         => ZERO
  case ONE          => ZERO
  case CHAR(d)      => if (c == d) ONE else ZERO
  case ALT(r1, r2)  => ALT(der(c, r1), der(c, r2))
  case SEQ(r1, r2)  =>
    if (nullable(r1)) ALT(SEQ(der(c, r1), r2), der(c, r2))
    else SEQ(der(c, r1), r2)
  case STAR(r)      => SEQ(der(c, r), STAR(r))
  case NTIMES(r, n) =>
    if (n == 0) ZERO
    else SEQ(der(c, r), NTIMES(r, n - 1))
  case PLUS(r)      => SEQ(der(c, r), STAR(r))
  case OPTIONAL(r)  => der(c, r)
  case RANGE(chars) => if (chars.contains(c)) ONE else ZERO
  case RECD(_, r1)  => der(c, r1)
}

// extracts a string from a value
def flatten(v: Val) : String = v match {
  case Empty        => ""
  case Chr(c)       => c.toString
  case Left(v)      => flatten(v)
  case Right(v)     => flatten(v)
  case Sequ(v1, v2) => flatten(v1) ++ flatten(v2)
  case Stars(vs)    => vs.map(flatten).mkString
  case Plus(vs)     => vs.map(flatten).mkString
  case NTimes(vs)   => vs.map(flatten).mkString
  case Rec(_, v)    => flatten(v)
  // case Opt(v)       => flatten(v) // todo?
}

// extracts an environment from a value;
// used for tokenising a string
def env(v: Val) : List[(String, String)] = v match {
  case Empty        => Nil
  case Chr(c)       => Nil
  case Left(v)      => env(v)
  case Right(v)     => env(v)
  case Sequ(v1, v2) => env(v1) ::: env(v2)
  case Stars(vs)    => vs.flatMap(env)
  case Rec(x, v)    => (x, flatten(v))::env(v)
  case Plus(vs)     => vs.flatMap(env)
  case NTimes(vs)   => vs.flatMap(env)
  case Opt(v)       => env(v)
}


// The injection and mkeps part of the lexer
//===========================================

// calculate posix value for how regex has matched empty string
// (only cases where nullable(r))
def mkeps(r: Rexp) : Val = r match {
  case ONE           => Empty
  case ALT(r1, r2)   => if (nullable(r1))
                          Left(mkeps(r1))
                        else
                          Right(mkeps(r2))
  case SEQ(r1, r2)   => Sequ(mkeps(r1), mkeps(r2))
  case STAR(r)       => Stars(Nil)
  case RECD(x, r)    => Rec(x, mkeps(r))
  //   case NTIMES(r, n)  => if (n==0) Empty else mkeps(r)
  case NTIMES(r, n)  => if (n==0) NTimes(Nil) else NTimes(List(mkeps(r)))
  case OPTIONAL(r)   => Empty
  case PLUS(r)       => Plus(List(mkeps(r)))
  // no case for char, range, 0 since they're not nullable
}

// how the derivate expression has matched a string

// r matched some c::s
// r_der = (der c r) matches s
// v (or v_der) tells us how the above derivate matched its string
// we want to put c back in to v (putting c back on the front c::s)

def inj(r: Rexp, c: Char, v: Val) : Val = (r, v) match {
  case (SEQ(r1, r2), Sequ(v1, v2))           => Sequ(inj(r1, c, v1), v2)
  case (SEQ(r1, r2), Left(Sequ(v1, v2)))     => Sequ(inj(r1, c, v1), v2)
  case (SEQ(r1, r2), Right(v2))              => Sequ(mkeps(r1), inj(r2, c, v2))
  case (ALT(r1, r2), Left(v1))               => Left(inj(r1, c, v1))
  case (ALT(r1, r2), Right(v2))              => Right(inj(r2, c, v2))
  case (CHAR(d),   Empty)                    => Chr(c)
  case (RANGE(cs), Empty)                    => Chr(c)
  case (RECD(x, r1), _)                      => Rec(x, inj(r1, c, v))
  case (STAR(r), Sequ(v1, Stars(vs)))        => Stars(inj(r, c, v1)::vs)
  case (PLUS(r), Sequ(v1, Stars(vs)))        => Plus(inj(r, c, v1)::vs)
  case (OPTIONAL(r), _)                      => Opt(inj(r, c, v))
  case (NTIMES(r, 0), _)                     => Empty
  case (NTIMES(r, n), Sequ(v1, NTimes(vs)))  => NTimes(inj(r, c, v1)::vs)
}

// some "rectification" functions for simplification
def F_ID(v: Val): Val      = v
def F_RIGHT(f: Val => Val) = (v:Val) => Right(f(v))
def F_LEFT(f: Val => Val)  = (v:Val) => Left(f(v))
def F_ALT(f1: Val => Val, f2: Val => Val) = (v:Val) => v match {
  case Right(v) => Right(f2(v))
  case Left(v) => Left(f1(v))
}
def F_SEQ(f1: Val => Val, f2: Val => Val) = (v:Val) => v match {
  case Sequ(v1, v2) => Sequ(f1(v1), f2(v2))
}
def F_SEQ_Empty1(f1: Val => Val, f2: Val => Val) =
  (v:Val) => Sequ(f1(Empty), f2(v))
def F_SEQ_Empty2(f1: Val => Val, f2: Val => Val) =
  (v:Val) => Sequ(f1(v), f2(Empty))
def F_RECD(f: Val => Val) = (v:Val) => v match {
  case Rec(x, v) => Rec(x, f(v))
}
def F_ERROR(v: Val): Val = throw new Exception("error")

// simplification
def simp(r: Rexp): (Rexp, Val => Val) = r match {
  case ALT(r1, r2) => {
    val (r1s, f1s) = simp(r1)
    val (r2s, f2s) = simp(r2)
    (r1s, r2s) match {
      case (ZERO, _) => (r2s, F_RIGHT(f2s))
      case (_, ZERO) => (r1s, F_LEFT(f1s))
      case _ => if (r1s == r2s) (r1s, F_LEFT(f1s))
                else (ALT (r1s, r2s), F_ALT(f1s, f2s))
    }
  }
  case SEQ(r1, r2) => {
    val (r1s, f1s) = simp(r1)
    val (r2s, f2s) = simp(r2)
    (r1s, r2s) match {
      case (ZERO, _) => (ZERO, F_ERROR)
      case (_, ZERO) => (ZERO, F_ERROR)
      case (ONE, _) => (r2s, F_SEQ_Empty1(f1s, f2s))
      case (_, ONE) => (r1s, F_SEQ_Empty2(f1s, f2s))
      case _ => (SEQ(r1s,r2s), F_SEQ(f1s, f2s))
    }
  }
  case r => (r, F_ID)
}

// lexing functions including simplification
def lex_simp(r: Rexp, s: List[Char]) : Val = s match {
  case Nil => if (nullable(r)) mkeps(r) else
    { throw new Exception("lexing error") }
  case c::cs => {
    val (r_simp, f_simp) = simp(der(c, r))
    inj(r, c, f_simp(lex_simp(r_simp, cs)))
  }
}
def lexing_simp(r: Rexp, s: String) = env(lex_simp(r, s.toList))

// unsimplified forms for testing
def lex(r: Rexp, s: List[Char]) : Val = s match {
  case Nil => if (nullable(r)) mkeps(r) else { throw new Exception("lexing error") }
  case c::cs => {
    val rr = der(c, r)
    inj(r, c, lex(rr, cs))
  }
}
def lexing(r: Rexp, s: String) = env(lex(r, s.toList));


// The Lexing Rules for the WHILE Language

val letters    = (('A' to 'Z') ++ ('a' to 'z')).toSet
val predigits  = ('1' to '9').toSet
val digits     = ('0' to '9').toSet

// chars
val LETTER     : Rexp = RANGE(letters)
val SPACE      : Rexp = " "
val EOL        : Rexp = "\n"
val TAB        : Rexp = "\t"
val WHITESPACE = PLUS(SPACE | EOL | TAB)
val UNDERSCORE : Rexp = "_"
val SEMI       : Rexp = ";"
val DSLASH     : Rexp = "//"

// numeric
val PREDIG     : Rexp = RANGE(predigits) // 1..9
val DIGIT      : Rexp = RANGE(digits)    // 0..9
val NUMBER     = DIGIT | (PREDIG ~ DIGIT.%)  // ([0-9])|([1-9][0-9]*)

// symbols
val OP         : Rexp = "+" | "-" | "*" | "%" | "/" | "==" | "!=" | ">" | "<" | "<=" | ">=" | ":=" | "&&" | "||"
val SYM        : Rexp = RANGE(letters ++ "._><=;,:\\".toSet)
val RPAREN     : Rexp = ")"
val LPAREN     : Rexp = "("
val LCURL      : Rexp = "{"
val RCURL      : Rexp = "}"
val PARENTH    : Rexp = "({})".toSet

// words
val KEYWORD    : Rexp = "while" | "if" | "then" | "else" | "do" | "for" | "to" | "true" | "false" | "read" | "write" | "skip"
val STRING     : Rexp = "\"" ~ (SYM | WHITESPACE | DIGIT ).% ~ "\""
val ID         = LETTER ~ (UNDERSCORE | LETTER | DIGIT).%
val COMMENT    = DSLASH ~ (SYM | SPACE | DIGIT).% ~ EOL

// expression to match (and tokenize) only valid while-language programs
val WHILE_LANG_REG = (
  ("kwd"  $ KEYWORD) |
  ("op"   $ OP)      |
  ("id"   $ ID)      |
  ("semi" $ SEMI)    |
  ("num"  $ NUMBER)  |
  ("comm" $ COMMENT) |
  ("str"  $ STRING)  |
  ("brkt" $ (LPAREN | RPAREN)) |
  ("crly" $ (LCURL  | RCURL))  |
  ("wspc" $ WHITESPACE)
).%

// escapes strings and prints them out as "", "\n" and so on
def esc(raw: String): String = {
  import scala.reflect.runtime.universe._
  Literal(Constant(raw)).toString
}
def escape(tks: List[(String, String)]) = tks.map{ case (s1, s2) => s"$s1:\t " + esc(s2) }

@doc("fin tests")
@main
def test() = {
  // simple lexer examples
  val R1 = NTIMES(CHAR('a'), 3)
  lexing_simp(R1, "aaa")

  val R2 = NTIMES((CHAR('a') | ONE), 3);
  lexing_simp(R2, "aa")

  // simple lexer examples with tokens
  println("Testing lexing with tokens: ");

  // def TONE            : Rexp = ("1" $ ONE)
  def TPLUS(r: Rexp)  : Rexp = (s"($r+)" $ PLUS(r))
  // def TCHAR(c: Char)  : Rexp = ("chr" $ CHAR(c))
  def TNTIMES(r: Rexp, n: Int): Rexp = (s"($r){$n}" $ NTIMES(r, n))

  val RT1 = TNTIMES(CHAR('a'), 3)
  println(lexing_simp(RT1, "aaa").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))

  val RT2 = TNTIMES((CHAR('a') | ONE), 3);
  println(lexing_simp(RT2, "aa").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))

  val RT3 = TNTIMES((SEQ(CHAR('a'), CHAR('b')) | CHAR('c')), 3);
  println(lexing_simp(RT3, "abcab").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))

  println("Practice lex:")
  println(lex(
    SEQ(ALT("ab", 'a'), (ALT(ONE, 'b'))),
    "ab".toList
    )
  )

  println("Practice lex:")
  println(lex(
    STAR(ALT("aa", 'a')),
    "aaa".toList
    )
  )

  // val RT4 = TNTIMES((CHAR('a') | ONE), 0);
  // println(lexing_simp(RT4, ).map({case (s1, s2) => s"$s1: $s2"}).mkString(""))

  val RTP1 = TPLUS(CHAR('a') | CHAR('b'))
  println(lexing_simp(RTP1, "abbaa").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))

  val RTP2 = TPLUS((SEQ(CHAR('a'), CHAR('b')) | CHAR('c')))
  println(lexing_simp(RTP2, "abc").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))
  // println(lexing_simp(RTP2, "a").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))
  // println(lexing_simp(RTP2, "").map({case (s1, s2) => s"$s1: $s2"}).mkString(""))

  println();
  //

  // todo: plus, opt, etc.

  val Ropt = OPTIONAL(CHAR('a'))
  lexing_simp(Ropt, "a")
  // lexing_simp(Ropt, "b")
  lexing_simp(Ropt, "")


  lexing_simp(PLUS('a'), "a")
  lexing_simp(PLUS('a'), "aaaaa")
  // lexing_simp(PLUS('a'), "b")

  lexing_simp(PLUS(ALT('a', 'b')), "abba")
  lexing_simp(PLUS(ALT('a', OPTIONAL('b'))), "")

  ///

  println("mkeps demos:")
  println(mkeps(
    ALT(
      (ZERO ~ (SEQ('b', 'c'))),
      ((ZERO ~ 'c') | ONE)
    )
  ));
  println(mkeps(
    SEQ(
      ALT('a', ONE),
      ALT(ONE, ONE)
    )
  ));
  println(mkeps(
    STAR('a')
  ));
  println("");

  val read_n = """read n;"""
  println(s"test lexing prog [$read_n]:\n")
  println(escape(lexing_simp(WHILE_LANG_REG, read_n)).mkString("\n"))
  println();

  val read_a = """ifif if24;"""
  println(s"test lexing prog [$read_n]:\n")
  println(escape(lexing_simp(WHILE_LANG_REG, read_a)).mkString("\n"))
  println();
}

@doc("while program tests")
@main
def while_program_tests() = {
   println("\n========== collatz.while ==========\n")

  val collatz_while =
"""write "Input a number ";
read n;
while n > 1 do {
  if n % 2 == 0
  then n := n/2
  else n := 3*n+1;
};
write "Yes";
"""

  println(escape(lexing_simp(WHILE_LANG_REG, collatz_while)).mkString("\n"))

  println("\n===================================\n")
  println("\n========== factors.while ==========\n")

  val factors_while =
"""// Find all factors of a given input number
// by J.R. Cordy August 2005

write "Input n please";
read n;
write "The factors of n are";
f := 2;
while n != 1 do {
    while (n / f) * f == n do {
        write f;
        n := n / f
    };
    f := f + 1
}"""

  println(escape(lexing_simp(WHILE_LANG_REG, factors_while)).mkString("\n"))

  println("\n===================================\n")

  println("\n==========  loops.while  ==========\n")

val while_three_while = """start := 1000;
x := start;
y := start;
z := start;
while 0 < x do {
 while 0 < y do {
  while 0 < z do { z := z - 1 };
  z := start;
  y := y - 1
 };
 y := start;
 x := x - 1
}

"""

  println(escape(lexing_simp(WHILE_LANG_REG, while_three_while)).mkString("\n"))

  println("\n===================================\n")

  println("\n========= collatz2.while ==========\n")

val collatz2_while = """// Collatz series
//
// needs writing of strings and numbers; comments

bnd := 1;
while bnd < 101 do {
  write bnd;
  write ": ";
  n := bnd;
  cnt := 0;

  while n > 1 do {
    write n;
    write ",";

    if n % 2 == 0
    then n := n / 2
    else n := 3 * n+1;

    cnt := cnt + 1
  };

  write " => ";
  write cnt;
  write "\n";
  bnd := bnd + 1
}"""

  println(escape(lexing_simp(WHILE_LANG_REG, collatz2_while)).mkString("\n"))

  println("\n===================================\n")

}


@doc("All tests.")
@main
def all() = { while_program_tests(); test(); }
