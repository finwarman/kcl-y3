import $file.lexer

val Lexer = lexer.Lexer
type Token = (String, String)

// === AST TOKENS =================

// new ast tokens for fun lang

abstract class Exp
abstract class BExp
abstract class Decl

case class Sequence(lhs: Exp, rhs: Exp) extends Exp // multiple consecutive expressions

case class Var  (s: String) extends Exp
case class Num  (i: Int)    extends Exp // integer numbers
case class FNum (f: Float)  extends Exp // floating-point numbers

case class Def    (name: String, args: List[(String, String)], typ: String, body: Exp) extends Decl
case class Main   (e: Exp) extends Decl
case class Const  (name: String, v: Int)   extends Decl
case class FConst (name: String, x: Float) extends Decl

case class Call (name: String, args: List[Exp]) extends Exp
case class If   (cond: BExp, then_block: Exp, else_block: Exp) extends Exp

case class Write(e: Exp) extends Exp

case class ApplyArithOp (op: String, lhs: Exp, rhs: Exp) extends Exp
case class ApplyBoolOp  (op: String, lhs: Exp, rhs: Exp) extends BExp

// =================================

object Parser {

case class ~[+A, +B](x: A, y: B)

// more convenient syntax for parser combinators
implicit def ParserOps[I : IsSeq, T](p: Parser[I, T]) = new {
  def ||     (q : => Parser[I, T])  =  new AltParser[I, T]   (p, q)
  def ~[S]   (q : => Parser[I, S])  =  new SeqParser[I, T, S](p, q)
  def map[S] (f: => T => S)         =  new MapParser[I, T, S](p, f)
}


// === PARSER ======================

type IsSeq[A] = A => Seq[_]
// sequence constraint on parser input type

abstract class Parser[I : IsSeq, T]{
  def parse(in: I): Set[(T, I)]

  def parse_all(in: I) : Set[T] =
    for ((hd, tl) <- parse(in); if tl.isEmpty) yield hd
}
// ================================
// === PARSER COMBINATORS =========

//=> sequence parser
class SeqParser[I : IsSeq, T, S]
    (p: => Parser[I, T],
     q: => Parser[I, S])
extends Parser[I, ~[T, S]]
{
  def parse(in: I) ={
    for ((hd1, tl1) <- p.parse(in); (hd2, tl2) <- q.parse(tl1))
    yield (new ~(hd1, hd2), tl2)
  }
}

//=> alternative parser
class AltParser[I : IsSeq, T]
    (p: => Parser[I, T], q: => Parser[I, T])
extends Parser[I, T]
{
  def parse(in: I) = { p.parse(in) ++ q.parse(in)}
}

//=> map parser (apply functions, semantic actions)
class MapParser[I : IsSeq, T, S]
    (p: => Parser[I, T],
     f: T => S)
extends Parser[I, S]
{
  def parse(in: I) = { for ((hd, tl) <- p.parse(in)) yield (f(hd), tl) }
}


//=> list parser
def ListParser[I, T, S](p: => Parser[I, T],
                        q: => Parser[I, S])(implicit ev: I => Seq[_]): Parser[I, List[T]] = {
  (p ~ q ~ ListParser(p, q)).map { case x ~ _ ~ z => x :: z : List[T] } ||
  (p.map ((s) => List(s)))
}

// ================================
// === ATOMIC PARSERS =============

case class TokParser(v: String) extends Parser[List[Token], String] {
    def parse(toks: List[Token]) = toks match {
        case (_, `v`)::tail => Set((v, tail))
        case _              => Set()
    }
}

// atomic parser for strings
case object StrParser extends Parser[List[Token], String] {
  def parse(toks: List[Token]) = toks match {
      case ("str", id)::tail => Set((id, tail))
      case _ => Set();
  }
}

// atomic parser for identifiers (variable names)
case object IdParser extends Parser[List[Token], String] {
  def parse(toks: List[Token]) = toks match {
      case ("id", id)::tail => Set((id, tail))
      case _ => Set();
  }
}

// atomic parser for numbers (transformed into ints)
case object NumParser extends Parser[List[Token], Int] {
  def parse(toks: List[Token]) = toks match {
      case ("num", num)::tail => Set((num.toInt, tail))
      case _ => Set();
  }
}
case object FloatParser extends Parser[List[Token], Float] {
  def parse(toks: List[Token]) = toks match {
      case ("fnum", num)::tail => Set((num.toFloat, tail))
      case _ => Set();
  }
}

case object TypeParser extends Parser[List[Token], String] {
  def parse(toks: List[Token]) = toks match {
      case ("type", typ)::tail => Set((typ, tail))
      case _ => Set();
  }
}

case object ArgDefParser extends Parser[List[Token], (String, String)] {
  def parse(toks: List[Token]) = toks match {
      case ("id", id)::("op", ":")::("type", typ)::tail => Set(((id, typ), tail))
      case _ => Set();
  }
}

// ================================
// === HELPER FUNCTIONS ===========

// allow us to write TokParser(_some_token_) as p"__some_token__"
implicit def parser_interpolation(sc: StringContext) = new {
    def p(args: Any*) = TokParser(sc.s(args:_*))
}

// remove outer quotes from strings and unescape special characters
def unescape(s: String): String = {
    val e = s.replaceAll("\"(.+)\"", "$1")  // remove quotes
    StringContext treatEscapes e
}

// ================================
// === AST STRUCTURE ==============


// arithmetic expressions
lazy val Exp:  Parser[List[Token], Exp] =
  (p"if" ~ BExp ~ p"then" ~ Exp ~ p"else" ~ Exp).map[Exp]{ case _ ~ x ~ _ ~ y ~ _ ~ z => If(x, y, z): Exp } ||
  (M ~ p";" ~ Exp).map[Exp]{ case x ~ _ ~ y => Sequence(x, y): Exp } || M

lazy val M: Parser[List[Token], Exp] =
  (IdParser ~ p"(" ~ ListParser(Exp, p",") ~ p")").map[Exp]{ case y ~ _ ~ args ~ _ => Call(y, args): Exp } ||
  (IdParser ~ p"(" ~ p")").map[Exp]{ case y ~ _ ~ _ => Call(y, Nil): Exp } || L

lazy val L: Parser[List[Token], Exp] =
  (T ~ p"+" ~ Exp).map[Exp]{ case x ~ _ ~ z => ApplyArithOp("+", x, z): Exp } ||
  (T ~ p"-" ~ Exp).map[Exp]{ case x ~ _ ~ z => ApplyArithOp("-", x, z): Exp } || T

lazy val T: Parser[List[Token], Exp] =
  (F ~ p"*" ~ T).map[Exp]{ case x ~ _ ~ z => ApplyArithOp("*", x, z): Exp } ||
  (F ~ p"/" ~ T).map[Exp]{ case x ~ _ ~ z => ApplyArithOp("/", x, z): Exp } ||
  (F ~ p"%" ~ T).map[Exp]{ case x ~ _ ~ z => ApplyArithOp("%", x, z): Exp } || F

lazy val F: Parser[List[Token], Exp] =
  (p"(" ~ Exp ~ p")").map[Exp]{ case _ ~ y ~ _ => y: Exp } ||
  (p"{" ~ Exp ~ p"}").map[Exp]{ case _ ~ y ~ _ => y: Exp } ||
  IdParser.map[Exp] { case x => Var(x): Exp } ||
  NumParser.map[Exp]{ case x => Num(x): Exp } ||
  FloatParser.map[Exp]{ case x => FNum(x): Exp}


// boolean expressions
lazy val BExp: Parser[List[Token], BExp] =
  (Exp ~ p"==" ~ Exp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("==", x, z): BExp } ||
  (Exp ~ p"!=" ~ Exp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("!=", x, z): BExp } ||
  (Exp ~ p"<"  ~ Exp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("<",  x, z): BExp } ||
  (Exp ~ p">"  ~ Exp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("<",  z, x): BExp } ||
  (Exp ~ p"<=" ~ Exp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("<=", x, z): BExp } ||
  (Exp ~ p"=>" ~ Exp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("<=", z, x): BExp } ||
  (p"(" ~ BExp ~ p")").map[BExp]{ case _ ~ x ~ _ => x : BExp}


// function definitons

lazy val Defn: Parser[List[Token], Decl] =
  (p"def" ~ IdParser ~ p"(" ~ ListParser(ArgDefParser, p",") ~ p")" ~ p":" ~ TypeParser ~ p"="~ Exp).map[Decl] { case _ ~ y ~ _ ~ args ~ _ ~ _ ~ t ~ _ ~ e => Def(y, args, t, e): Decl } ||
  // (p"def" ~ IdParser ~ p"(" ~ p")" ~ p":" ~ TypeParser ~ p"=" ~ Exp).map[Decl] { case _ ~ y ~ _ ~ _ ~ _ ~ t ~ _ ~ e  => Def(y, Nil, t, e): Decl } || // empty arg list :)
  (p"val" ~ IdParser ~ p":" ~ p"Int" ~ p"=" ~ NumParser).map[Decl]{ case _ ~ x ~ _ ~ y ~ _ ~ z => Const(x, z): Decl } ||
  (p"val" ~ IdParser ~ p":" ~ p"Double" ~ p"=" ~ FloatParser).map[Decl]{ case _ ~ x ~ _ ~ y ~ _ ~ z => FConst(x, z): Decl }

// programs

lazy val Program: Parser[List[Token], List[Decl]] = // top-level of program
  (Defn ~ p";" ~ Program).map{ case x ~ _ ~ z => x :: z : List[Decl] } ||
  (Exp.map((s) => List(Main(s)) : List[Decl]))

// === TEST HELPERS =================

// AST Pretty Printer
def pprint(obj: Any, depth: Int = 0, paramName: Option[String] = None, toplevel: Boolean = false): Unit = {
    val indent = "   " * depth
    val pname = paramName.fold("")(x => s"$x: ")
    val ptype = obj match {
        case _: Iterable[Any] => ""                 // strip out "::" from iterable types
        case obj: Product     => obj.productPrefix  // any form of tuple, i.e. any case class taking arguments
        case _                => obj.toString       // for simple objects, just take their name
    }

    if (toplevel) {
        println("Program");
    } else {
        println(s"$indent └→ $pname$ptype");
    }

    obj match {
        case seq: Iterable[Any] => seq.foreach(pprint(_, depth + 1)) // recursively print all children
        case obj: Product =>                                         // recursively print all arguments of current case class, and their children
            (obj.productIterator zip obj.productElementNames).foreach {
              case (subObj, paramName) => pprint(subObj, depth + 1, Some(paramName))
            } // create tuple for each argument object, of the form (object, objectName) (see https://stackoverflow.com/a/55032051/5864825)
        case _ =>                                                    // we are done in this branch! do nothing
    }
}

// Given a program, lex and parser it, giving the AST
def parse_program(program_text: String, name: String): List[Decl] = {
  println("=============== Program: ==============\n");
  println(program_text);
  println("\n=====================================");
  println("============ LEXED TOKENS ===========\n");
  val tokens = Lexer.tokenize_string_program(program_text);
  Lexer.pretty_print_tokens(tokens);
  println();
  println("=====================================");
  println("============= PARSED AST ============\n");

  val parsed = Program.parse_all(tokens);

  if (!parsed.isEmpty) {
        pprint(parsed.head, toplevel=true);
        println();
        println("=====================================");
        parsed.head
  } else {
        println(parsed);
        println("No AST was returned, was there a syntax error?");
        println("=====================================");
        throw new Exception("Parser Exception");
  }
}

}
