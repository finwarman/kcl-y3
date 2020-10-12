//  Run Tests:
//    amm 01_coursework.sc test{x}
//  or
//    amm re3.sc all

// TODO: look at https://www.ccs.neu.edu/home/turon/re-deriv.pdf

// ===== CLASS DEFINITIONS =====

abstract class Rexp
case object ZERO extends Rexp
case object ONE extends Rexp
case class CHAR(c: Char) extends Rexp
case class ALT(r1: Rexp, r2: Rexp) extends Rexp
case class SEQ(r1: Rexp, r2: Rexp) extends Rexp
case class STAR(r: Rexp) extends Rexp
// Extended (Q3):
// RANGE, PLUS, OPTIONAL, NTIMES, UPTO, FROM, BETWEEN, NOT
case class RANGE(chars: Set[Char]) extends Rexp
case class PLUS(r: Rexp) extends Rexp
case class OPTIONAL(r: Rexp) extends Rexp
case class NTIMES(r: Rexp, n: Int) extends Rexp
case class UPTO(r: Rexp, m: Int) extends Rexp
case class FROM(r: Rexp, n: Int) extends Rexp
case class BETWEEN(r: Rexp, n: Int, m: Int) extends Rexp
case class NOT(r: Rexp) extends Rexp
// Extended (Q4):
// CFUN
case class CFUN(f: Char => Boolean) extends Rexp

// infix operators for SEQ and ALT for more readable expressions!
implicit class RexpOps(private val r1: Rexp) extends AnyVal {
  def + (r2: Rexp): Rexp = ALT(r1, r2)
  def o (r2: Rexp): Rexp = SEQ(r1, r2)
}

// ====== FUNCTION DEFINITIONS =====

// CFUN related functions:
def _CHAR(ch: Char): Char => Boolean = { (c: Char) => {(ch == c)} }
def _RANGE(chars: Set[Char]) : Char => Boolean = { (c: Char) => {chars.contains(c)} }
def _ALL() : Char => Boolean = { (c: Char) => true}

// the nullable function: tests whether the regular
// expression can recognise the empty string
def nullable(r: Rexp): Boolean =
  r match {
    case ZERO        => false
    case ONE         => true
    case CHAR(_)     => false
    case ALT(r1, r2) => nullable(r1) || nullable(r2)
    case SEQ(r1, r2) => nullable(r1) && nullable(r2)
    case STAR(_)     => true

    // === Extended Cases ===
    case NTIMES(r, n)     => if (n == 0) true else nullable(r)
    case PLUS(r)          => nullable(r)
    case OPTIONAL(r)      => true
    case UPTO(r, m)       => true
    case FROM(r, n)       => if (n == 0) true else nullable(r)
    case BETWEEN(r, n, m) => if (n == 0) true else nullable(r)
    case RANGE(chars)     => false
    case NOT(r)           => !(nullable(r))
    // == Q4 ==
    case CFUN(f)          => false
    // ======================
  }

// the derivative of a regular expression w.r.t. a character
def der(c: Char, r: Rexp): Rexp =
  r match {
    case ZERO        => ZERO
    case ONE         => ZERO
    case CHAR(d)     => if (c == d) ONE else ZERO
    case ALT(r1, r2) => ALT(der(c, r1), der(c, r2))
    case SEQ(r1, r2) =>
      if (nullable(r1)) ALT(SEQ(der(c, r1), r2), der(c, r2))
      else SEQ(der(c, r1), r2)
    case STAR(r) => SEQ(der(c, r), STAR(r))

    // === Extended Cases ===
    case NTIMES(r, n) =>
      if (n == 0) ZERO
      else SEQ(der(c, r), NTIMES(r, n - 1))

    case PLUS(r) =>
      SEQ(der(c, r), STAR(r)) // one of r matches, followed by zero-or-more of r
    // TODO - is this allowed?

    case OPTIONAL(r) => der(c, r)

    case BETWEEN(r, n, m) =>
      if (n == 0) {
        if (m == 0) ZERO
        else SEQ(der(c, r), BETWEEN(r, n, m - 1))
      } else SEQ(der(c, r), BETWEEN(r, n - 1, m - 1))

    case UPTO(r, m) =>
      if (m == 0) ZERO
      else SEQ(der(c, r), UPTO(r, m - 1))

    case FROM(r, n) =>
      if (n == 0)
        SEQ(der(c, r), FROM(r, n)) // TODO - can also use STAR instead of FROM
      else SEQ(der(c, r), FROM(r, n - 1))

    case RANGE(chars) => if (chars.contains(c)) ONE else ZERO

    case NOT(r) => NOT(der(c, r))

    // == Q4 ==

    case CFUN(f) => if (f(c)) ONE else ZERO

    // ======================
  }

// simplify the given expression 'inside out', using common simplifications - reduces bloat
def simp(r: Rexp): Rexp =
  r match {
    case ALT(r1, r2) =>
      (simp(r1), simp(r2)) match {
        case (ZERO, r2s) => r2s
        case (r1s, ZERO) => r1s
        case (r1s, r2s)  => if (r1s == r2s) r1s else ALT(r1s, r2s)
      }
    case SEQ(r1, r2) =>
      (simp(r1), simp(r2)) match {
        case (ZERO, _)  => ZERO
        case (_, ZERO)  => ZERO
        case (ONE, r2s) => r2s
        case (r1s, ONE) => r1s
        case (r1s, r2s) => SEQ(r1s, r2s)
      }
    case r => r
  }

// the derivative w.r.t. a string (iterates der)
def ders(s: List[Char], r: Rexp): Rexp =
  s match {
    case Nil    => r
    case c :: s => ders(s, simp(der(c, r)))
  }

// the main matcher function
def matcher(r: Rexp, s: String): Boolean =
  nullable(ders(s.toList, r))

// ==============================

// ===== TESTS =====

def assertTest(result: Any, expected: Any, testName: String) = {
  print(("Testing '" + testName + "'...").padTo(55, ' '));
  try {
    assert(result == expected);
    println(Console.GREEN + "PASS");
  } catch {
    case ae: AssertionError => {
      println(Console.RED + "FAIL");
      println(f"\tExpected: $expected, Actual: $result");
    }
    case e: Exception => throw e
  }
  print(Console.WHITE);
}

@doc("RANGE")
@main
def testRange() = {
  println("\nTesting RANGE:");

  // [a,b,c,d]
  val R1 = RANGE(Set('a', 'b', 'c', 'd'));
  assertTest(matcher(R1, "a"), true, "[a,b,c,d] matches a");
  assertTest(matcher(R1, "b"), true, "[a,b,c,d] matches b");
  assertTest(matcher(R1, "ab"), false, "[a,b,c,d] doesn't match ab");
  assertTest(matcher(R1, ""), false, "[a,b,c,d] doesn't match empty string");

  // [a,b]+
  val R2 = PLUS(RANGE(Set('a', 'b')));
  assertTest(matcher(R2, "ab"), true, "[a,b,c,d]+ matches ab");
  assertTest(matcher(R2, "aaaa"), true, "[a,b,c,d]+ matches aaaa");
  assertTest(matcher(R2, ""), false, "[a,b,c,d]+ doesn't match empty string");

  println();
}

@doc("UPTO")
@main
def testUpTo() = {
  println("\nTesting UPTO:");

  // a{..2}
  val R1 = UPTO(CHAR('a'), 2);
  assertTest(matcher(R1, ""), true, "a{..2} matches empty string");
  assertTest(matcher(R1, "a"), true, "a{..2} matches a");
  assertTest(matcher(R1, "aa"), true, "a{..2} matches aa");
  assertTest(matcher(R1, "aaa"), false, "a{..2} doesn't match aaa");

  // ab{..3}c
  val R2 = SEQ(CHAR('a'), SEQ(UPTO(CHAR('b'), 3), CHAR('c')));
  assertTest(matcher(R2, "ac"), true, "ab{..3}c matches ac");
  assertTest(matcher(R2, "abbc"), true, "ab{..3}c matches abbc");
  assertTest(matcher(R2, "abbbbc"), false, "ab{..3}c doesn't match abbbbc");

  println();
}

@doc("FROM")
@main
def testFrom() = {
  println("\nTesting FROM:");

  // a{2..}
  val R1 = FROM(CHAR('a'), 2);
  assertTest(matcher(R1, "aa"), true, "a{2..} matches aa");
  assertTest(matcher(R1, "aaa"), true, "a{2..} matches aaa");
  assertTest(matcher(R1, "a"), false, "a{2..} doesn't match a");
  assertTest(matcher(R1, ""), false, "a{2..} doesn't match empty string");

  // ab{3..}c
  val R2 = SEQ(CHAR('a'), SEQ(FROM(CHAR('b'), 3), CHAR('c')));
  assertTest(matcher(R2, "abbbc"), true, "ab{3..}c matches abbbc");
  assertTest(matcher(R2, "abbbbc"), true, "ab{..3}c matches abbbbc");
  assertTest(matcher(R2, "abc"), false, "ab{3..}c doesn't match abc");
  assertTest(matcher(R2, "ac"), false, "ab{3..}c doesn't match ac");

  println();
}

@doc("BETWEEN")
@main
def testBetween() = {
  println("\nTesting BETWEEN:");

  // a{3, 6}
  val R1 = BETWEEN(CHAR('a'), 3, 6);
  assertTest(matcher(R1, "aaa"), true, "a{3, 6} matches aaa");
  assertTest(matcher(R1, "aaaaaa"), true, "a{3, 6} matches aaaaaa");
  assertTest(matcher(R1, "aaaa"), true, "a{3, 6} matches aaaa");
  assertTest(matcher(R1, "a"), false, "a{3, 6} doesn't match a");
  assertTest(matcher(R1, "aaaaaaa"), false, "a{3, 6} doesn't match aaaaaaa");
  assertTest(matcher(R1, "aaaab"), false, "a{3, 6} doesn't match aaaab");

  println();
}

@doc("PLUS")
@main
def testPlus() = {
  println("\nTesting PLUS:");

  // a+
  val R1 = PLUS(CHAR('a'));
  assertTest(matcher(R1, "a"), true, "a+ matches a");
  assertTest(matcher(R1, "aaaa"), true, "a+ matches aaaa");
  assertTest(matcher(R1, ""), false, "a+ doesn't match empty string");
  assertTest(matcher(R1, "ba"), false, "a+ doesn't match ba");

  // a+b+
  val R2 = SEQ(PLUS(CHAR('a')), PLUS(CHAR('b')));
  assertTest(matcher(R2, "ab"), true, "a+b+ matches ab");
  assertTest(matcher(R2, "aaabb"), true, "a+b+ matches aaabb");
  assertTest(matcher(R2, "aaa"), false, "a+b+ doesn't match aaa");

  println();
}

@doc("OPTIONAL")
@main
def testOptional() = {
  println("\nTesting OPTIONAL:");

  // a?
  val R1 = OPTIONAL(CHAR('a'));
  assertTest(matcher(R1, "a"), true, "a? matches a");
  assertTest(matcher(R1, ""), true, "a? matches empty string");
  assertTest(matcher(R1, "ab"), false, "a? doesn't match ab");

  // a?cb?
  val R2 = SEQ(OPTIONAL(CHAR('a')), SEQ(CHAR('c'), OPTIONAL(CHAR('b'))));
  assertTest(matcher(R2, "acb"), true, "a?cb? matches acb");
  assertTest(matcher(R2, "c"), true, "a?cb? matches c");
  assertTest(matcher(R2, "aacb"), false, "a?cb? doesn't match aacb");
  assertTest(matcher(R2, "ab"), false, "a?cb? doesn't match ab");

  println();
}

@doc("NTIMES")
@main
def testNTimes() = {
  println("\nTesting NTIMES:");

  // a{5}
  val R1 = NTIMES(CHAR('a'), 5);
  assertTest(matcher(R1, "aaaaa"), true, "a{5} matches aaaaa");
  assertTest(matcher(R1, "aaaa"), false, "a{5} doesn't match aaaa");

  // ab{2}c{3}
  val R2 = SEQ(CHAR('a'), SEQ(NTIMES(CHAR('b'), 2), NTIMES(CHAR('c'), 3)));
  assertTest(matcher(R2, "abbccc"), true, "ab{2}c{3} matches abbccc");
  assertTest(matcher(R2, "aabbcc"), false, "ab{2}c{3} doesn't match aabbcc");

  println();
}

@doc("NOT")
@main
def testNot() = {
  println("\nTesting NOT:");

  // ~a
  val R1 = NOT(CHAR('a'));
  assertTest(matcher(R1, "b"), true, "~a matches b");
  assertTest(matcher(R1, "ab"), true, "~a matches ab");
  assertTest(matcher(R1, "aaa"), true, "~a matches aaa");
  assertTest(matcher(R1, "a"), false, "~a doesn't match a");


  println();
}

@doc("Custom Infix Operators")
@main
def testCustomInfix() = {
  println("\nTesting Custom Infix Operators:");

  // abc ("'a' o 'b' o 'c'")
  val R1 = CHAR('a') o CHAR('b') o CHAR('c');
  assertTest(matcher(R1, "abc"), true, "[SEQ = o]: a.b.c matches abc");
  assertTest(matcher(R1, "abcd"), false, "[SEQ = o]: a.b.c doesn't match abcd");

  // a+b+c ("'a' + 'b' + 'c'")
  val R2 = CHAR('a') + CHAR('b') + CHAR('c');
  assertTest(matcher(R2, "a"), true, "[ALT = +]: a+b+c matches a");
  assertTest(matcher(R2, "b"), true, "[ALT = +]: a+b+c matches b");
  assertTest(matcher(R2, "ab"), false, "[ALT = +]: a+b+c doesn't match ab");
  assertTest(matcher(R2, "d"), false, "[ALT = +]: a+b+c doesn't match d");

  println();
}

@doc("Question 3 - Table Test")
@main
def question3() = {
  println("Question 3 - Table Test:\n");

  var testCases = collection.mutable.LinkedHashMap(
    "a?"         -> OPTIONAL(CHAR('a')),
    "~a"         -> NOT(CHAR('a')),
    "a{3}"       -> NTIMES(CHAR('a'), 3),
    "(a?){3}"    -> NTIMES(OPTIONAL(CHAR('a')), 3),
    "a{..3}"     -> UPTO(CHAR('a'), 3),
    "(a?){..3}"  -> UPTO(OPTIONAL(CHAR('a')), 3),
    "a{3..5}"    -> BETWEEN(CHAR('a'), 3, 5),
    "(a?){3..5}" -> BETWEEN(OPTIONAL(CHAR('a')), 3, 5),
    "a{0}"       -> NTIMES(CHAR('a'), 0)
  );

  var strings = (0 to 6).map( n => "a" * n )

  // Table Header
  println("        | " + (testCases.keys mkString "  | ") + "  |");

  print("--------+");
  testCases.keys.foreach( name => {
      print(("-" * (name.length()+3)) + "+");
  });
  print("\n")

  strings.foreach( str => {
    print((if (str.length() > 0) str else "[]").padTo(7, ' ') + " | ");

    testCases.keys.foreach( name => {
        val R1 = testCases.getOrElse(name, throw new RuntimeException("Not Found"));
        val result = matcher(R1, str);

        print((if (result) ("YES") else "-").padTo(name.length() + 1, ' ') + " | ");
    });

    println();
  })
  println();
}

@doc("Question 4 - CFUN")
@main
def question4() = {
    println("Question 4 - CFUN Tests:");

    // char
    println("\nTesting _CHAR (c):");
    val R1 = CFUN(_CHAR('a'));
    assertTest(matcher(R1, "a"), true, "CFUN: a matches a");
    assertTest(matcher(R1, "b"), false, "CFUN: a doesn't match b");
    assertTest(matcher(R1, "aa"), false, "CFUN: a doesn't match aa");
    assertTest(matcher(R1, ""), false, "CFUN: a doesn't match empty string");

    // range
    println("\nTesting _RANGE ([abc]):");
    val R2 = CFUN(_RANGE(Set('a','b','c', 'd')));
    assertTest(matcher(R2, "a"), true, "CFUN: [a,b,c,d] matches a");
    assertTest(matcher(R2, "b"), true, "CFUN: [a,b,c,d] matches b");
    assertTest(matcher(R2, "ab"), false, "CFUN: [a,b,c,d] doesn't match ab");
    assertTest(matcher(R2, ""), false, "CFUN: [a,b,c,d] doesn't match empty string");

    // all
    println("\nTesting _ALL (.):");
    val R3 = CFUN(_ALL);
    assertTest(matcher(R3, "a"), true, "CFUN: . matches a");
    assertTest(matcher(R3, "b"), true, "CFUN: . matches a");
    assertTest(matcher(R3, "c"), true, "CFUN: . matches a");
    assertTest(matcher(R3, "ab"), false, "CFUN: . doesn't match ab");

    // with other operators
    println("\nTesting CFUN with various operators:");
    assertTest(matcher(PLUS(CFUN(_ALL())), "abcdefg"), true, ".+ matches abcdefg");
    assertTest(matcher(PLUS(CFUN(_ALL())), ""), false, ".+ doesn't match empty string");
    assertTest(matcher(NTIMES(CFUN(_RANGE(Set('a','b','c'))), 3), "abc"), true, "[abc]{3} matches abc");
    assertTest(matcher(NTIMES(CFUN(_RANGE(Set('a','b','c'))), 3), "aaa"), true, "[abc]{3} matches aaa");
    assertTest(matcher(NTIMES(CFUN(_RANGE(Set('a','b','c'))), 3), "a"), false, "[abc]{3} doesn't match a");

    println("\nDone!")
    println();
}

@doc("Question 5 - Email")
@main
def question5() = {
    println("Question 5 - Email:\n");

    val lower = ('a' to 'z').toSet; // [a-z]
    val digits = ('0' to '9').toSet; // [0-9]

    val R_NAME = PLUS(CFUN(_RANGE(lower ++ digits + '_' + '.' + '-'))); // [a-z0-9_.-]+
    val R_DOMAIN = PLUS(CFUN(_RANGE(lower ++ digits + '.' + '-'))); // [a-z0-9.-]+
    val R_TLD = BETWEEN(CFUN(_RANGE(lower + '.')), 2, 6); // [a-z.]{2,6}

    // ([a-z0-9_.-]+)@([a-z0-9.-]+).([a-z.]{2,6})
    val R_EMAIL = SEQ(R_NAME, SEQ(CFUN(_CHAR('@')), SEQ(R_DOMAIN, SEQ(CFUN(_CHAR('.')),R_TLD))));

    val matches_email = matcher(R_EMAIL, "finley.warman@kcl.ac.uk");
    val der_wrt_email = ders("finley.warman@kcl.ac.uk".toList, R_EMAIL);

    assertTest(matches_email, true, "My email matches the regular expression");

    println("TODO!");
    println(der_wrt_email); // TODO - fix the use of 'star' above in plus

    println("\nDone!")
    println();
}

@doc("Question 6")
@main
def question6() = {
    println("Question 6:\n");

    // \/\*(~(.*\*\/.*))\*\/
    val R1 = CFUN(_CHAR('/')) o CFUN(_CHAR('*')) o (NOT(STAR(CFUN(_ALL)) o CFUN(_CHAR('*')) o CFUN(_CHAR('/')) o STAR(CFUN(_ALL) )) o CFUN(_CHAR('*')) o CFUN(_CHAR('/')));

    println("Testing \\/\\*(~(.*\\*\\/.*))\\*\\/ matches:");
    assertTest(matcher(R1, "/**/"), true, "/**/"); // yes
    assertTest(matcher(R1, "/*foobar*/"), true, "/*foobar*/"); // yes
    assertTest(matcher(R1, "/*test*/test*/"), false, "/*test*/test*/"); // no
    assertTest(matcher(R1, "/*test/*test*/"), true, "/*test/*test*/"); // yes

    println("\nDone!")
    println();
}

@doc("Question 7")
@main
def question7() = {
    println("Question 7:\n");

    val R1 = CFUN(_CHAR('a')) o CFUN(_CHAR('a')) o CFUN(_CHAR('a'));;
    val R2 = (BETWEEN(CFUN(_CHAR('a')), 19, 19)) o (OPTIONAL(CFUN(_CHAR('a'))))

    val five = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    val six = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    val seven = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    val R1PlusPlus = PLUS(PLUS(R1));
    val R2PlusPlus = PLUS(PLUS(R2));

    assertTest(matcher(R1PlusPlus, five), true,   "(R1+)+ match 5 = MATCH"); // yes
    assertTest(matcher(R1PlusPlus, six), false,   "(R1+)+ match 6 = FAIL");  // no
    assertTest(matcher(R1PlusPlus, seven), false, "(R1+)+ match 7 = FAIL");  // no

    assertTest(matcher(R2PlusPlus, five), true,  "(R2+)+ match 5 = MATCH"); // yes
    assertTest(matcher(R2PlusPlus, six), false,  "(R2+)+ match 6 = FAIL");  // no
    assertTest(matcher(R2PlusPlus, seven), true, "(R2+)+ match 7 = MATCH"); // yes

    println("\nDone!")
    println();
}

// ==== RUN ALL: ======

@doc("All tests.")
@main
def all() = {
  println("Unit Tests:");
  testRange();
  testFrom();
  testBetween();
  testUpTo();
  testPlus();
  testOptional();
  testNTimes();
  testNot();
  testCustomInfix(); // custom infix operators (RexpOps class)
  println("Done :)");
  println();
  println("Coursework Questions:\n");
  question3();
  question4();
  question5();
  question6();
  question7();
}

// ==============================
