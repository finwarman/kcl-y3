//  Run Tests:
//    amm 01_coursework.sc test{x}
//  or
//    amm re3.sc all

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
    // Extended cases:
    case NTIMES(r, i) => if (i == 0) true else nullable(r)
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
    // Extended cases:
    case NTIMES(r, i) =>
      if (i == 0) ZERO else SEQ(der(c, r), NTIMES(r, i - 1))
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
  print("Testing '" + testName + "'... \t");
  try {
    assert(result == expected);
    println(Console.GREEN + "PASS");
  } catch {
    case ae: AssertionError => {
      println(Console.RED + "FAIL");
      println(f"\t - (Expected: $expected | Actual: $result)");
    }
    case e: Exception => throw e
  }
  print(Console.WHITE);
}

@doc("RANGE")
@main
def testCharSet() = {
  println("\nTesting RANGE:");
  println("TODO");
  true;
}

@doc("PLUS")
@main
def testPlus() = {
  println("\nTesting PLUS:");
  println("TODO");
  true;
}

@doc("OPTIONAL")
@main
def testOptional() = {
  println("\nTesting OPTIONAL:");
  println("TODO");
  true;
}

@doc("NTIMES")
@main
def testNTimes() = {
  println("\nTesting OPTIONAL:");

  // a{5}
  val R1 = NTIMES(CHAR('a'), 5);

  assertTest(matcher(R1, "aaaaa"), true, "a{5} matches aaaaa");
  assertTest(matcher(R1, "aaaa"), true, "a{5} doesn't match aaaa");
}

// RUN ALL:

@doc("All tests.")
@main
def all() = {
  testCharSet();
  testPlus();
  testOptional();
  testNTimes();
  println("\nDone :)");
}

// ==============================
