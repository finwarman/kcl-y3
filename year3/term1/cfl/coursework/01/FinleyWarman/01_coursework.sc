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
// Extended:
// RANGE, PLUS, OPTIONAL, NTIMES, UPTO, FROM, BETWEEN
case class RANGE(r: Rexp, chars: Set[Char]) extends Rexp
case class PLUS(r: Rexp) extends Rexp
case class OPTIONAL(r: Rexp) extends Rexp
case class NTIMES(r: Rexp, n: Int) extends Rexp
case class UPTO(r: Rexp, m: Int) extends Rexp
case class FROM(r: Rexp, n: Int) extends Rexp
case class BETWEEN(r: Rexp, n: Int, m: Int) extends Rexp

// TODO - negation

// ==============================

// ====== FUNCTION DEFINITIONS =====

// Extended features:

// range
// def RANGE(chars : Set[Char]) : Char => Boolean = {   (ch) => chars.contains(ch)  }

// plus r - 1 or more

// optional r - one or zero
// def OPTIONAL(r: Rexp) = ALT(r, ONE)

// exactly n times r

// zero or more r, up to m times

// n or more times r

// at least n times r but no more than m times

// a set of characters for character ranges

// not-regular-expression of r

// ------

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
    // TODO case RANGE    =>
    // TODO case NOT(r)  =>
    //     if (nullable(r) == ZERO) ONE
    //     else ZERO // nullable(r) == ONE

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
    case STAR(r1) => SEQ(der(c, r1), STAR(r1))

    // === Extended Cases ===
    case NTIMES(r, n) =>
      if (n == 0) ZERO
      else SEQ(der(c, r), NTIMES(r, n - 1))

    case PLUS(r) =>
      SEQ(der(c, r), STAR(r)) // one of r matches, followed by zero-or-more of r

    case OPTIONAL(r) => der(c, r)

    case BETWEEN(r, n, m) =>
      if (n == 0) ZERO
      else if (n == m) der(c, NTIMES(r, n))
      else SEQ(der(c, r), BETWEEN(r, n + 1, m))
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
  print(("Testing '" + testName + "'...").padTo(50, ' '));
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
  println("TODO");
  true;
}

@doc("UPTO")
@main
def testUpTo() = {
  println("\nTesting UPTO:");
  println("TODO");
  true;
}

@doc("FROM")
@main
def testFrom() = {
  println("\nTesting FROM:");
  println("TODO");
  true;
}

@doc("BETWEEN")
@main
def testBetween() = {
  println("\nTesting BETWEEN:");
  println("TODO");
  true;
}

@doc("PLUS")
@main
def testPlus() = {
  println("\nTesting PLUS:");

  // a+
  val R1 = PLUS(CHAR('a'));
  assertTest(matcher(R1, "a"), true, "a+ matches a");
  assertTest(matcher(R1, "aaaa"), true, "a+ matches aaaa");
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

// RUN ALL:

@doc("All tests.")
@main
def all() = {
  testRange();
  testFrom();
  testBetween();
  testUpTo();
  testPlus();
  testOptional();
  testNTimes();
  println("Done :)");
}

// ==============================
