import $file.lexer

val Lexer = lexer.Lexer
type Token = (String, String)

case class ~[+A, +B](x: A, y: B)

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

// ================================
// === HELPER FUNCTIONS ===========

// allow us to write TokParser(_some_token_) as p"__some_token__"
implicit def parser_interpolation(sc: StringContext) = new {
    def p(args: Any*) = TokParser(sc.s(args:_*))
}

// more convenient syntax for parser combinators
implicit def ParserOps[I : IsSeq, T](p: Parser[I, T]) = new {
  def ||     (q : => Parser[I, T])  =  new AltParser[I, T]   (p, q)
  def ~[S]   (q : => Parser[I, S])  =  new SeqParser[I, T, S](p, q)
  def map[S] (f: => T => S)         =  new MapParser[I, T, S](p, f)
}

// remove outer quotes from strings and unescape special characters
def unescape(s: String): String = {
    val e = s.replaceAll("\"(.+)\"", "$1")  // remove quotes
    StringContext treatEscapes e
}

// ================================
// === AST TOKENS =================

abstract class AExp
abstract class BExp
abstract class Stmt

type Block = List[Stmt]

case class Var (s: String) extends AExp
case class Num (i: Int)    extends AExp

case object True  extends BExp
case object False extends BExp

case class ApplyArithOp (op: String, lhs: AExp, rhs: AExp) extends AExp
case class ApplyBoolOp  (op: String, lhs: AExp, rhs: AExp) extends BExp

// logical operators
case class And(lhs: BExp, rhs: BExp) extends BExp
case class Or (lhs: BExp, rhs: BExp)  extends BExp

case object Skip extends Stmt
case class  If     (cond: BExp, then_block: Block, else_block: Block) extends Stmt
case class  While  (cond: BExp, do_block:  Block) extends Stmt
case class  Assign (var_name: String, bound_exp: AExp)  extends Stmt

case class  Read      (s: String)           extends Stmt
case class  WriteVar  (s: String)           extends Stmt
case class  WriteStr  (s: String)           extends Stmt

// ================================
// === AST STRUCTURE ==============

//=> arithmetic expressions
lazy val AExp: Parser[List[Token], AExp] =
  (Mult ~ p"+" ~ AExp).map[AExp]{ case x ~ _ ~ z => ApplyArithOp("+", x, z) } ||
  (Mult ~ p"-" ~ AExp).map[AExp]{ case x ~ _ ~ z => ApplyArithOp("-", x, z) } ||
   Mult

lazy val Mult: Parser[List[Token], AExp] =
  (Term ~ p"*" ~ Mult).map[AExp]{ case x ~ _ ~ z => ApplyArithOp("*", x, z) } ||
  (Term ~ p"/" ~ Mult).map[AExp]{ case x ~ _ ~ z => ApplyArithOp("/", x, z) } ||
  (Term ~ p"%" ~ Mult).map[AExp]{ case x ~ _ ~ z => ApplyArithOp("%", x, z) } ||
   Term

lazy val Term: Parser[List[Token], AExp] =
   (p"(" ~ AExp ~ p")").map{ case _ ~ y ~ _ => y } ||
    NumParser.map(Num) || IdParser.map(Var)


//=> boolean expressions
lazy val BExp: Parser[List[Token], BExp] =
   (AExp ~ p"==" ~ AExp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("==", x, z) } ||
   (AExp ~ p"!=" ~ AExp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("!=", x, z) } ||
   (AExp ~  p"<" ~ AExp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp( "<", x, z) } ||
   (AExp ~  p">" ~ AExp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp( ">", x, z) } ||
   (AExp ~ p"<=" ~ AExp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp("<=", x, z) } ||
   (AExp ~ p">=" ~ AExp).map[BExp]{ case x ~ _ ~ z => ApplyBoolOp(">=", x, z) } ||
   (p"(" ~ BExp ~ p")" ~ p"&&" ~ BExp).map[BExp]{ case _ ~ y ~ _ ~ _ ~ v => And(y, v) } ||
   (p"(" ~ BExp ~ p")" ~ p"||" ~ BExp).map[BExp]{ case _ ~ y ~ _ ~ _ ~ v =>  Or(y, v) } ||
   (p"true" .map[BExp]{ _ => True  }) ||
   (p"false".map[BExp]{ _ => False }) ||
   (p"(" ~ BExp ~ p")").map[BExp]{ case _ ~ x ~ _ => x }

//=> single statements
lazy val Stmt: Parser[List[Token], Stmt] = (
    (p"skip".map[Stmt]{_ => Skip }) ||
    (IdParser ~ p":=" ~ AExp).map[Stmt]{ case x ~ _ ~ z => Assign(x, z) } ||
    (p"read"  ~ IdParser).map[Stmt]{ case  _ ~ y => Read(y) }     ||
    (p"write" ~ IdParser ).map[Stmt]{ case _ ~ y => WriteVar(y) } ||
    (p"write" ~ StrParser).map[Stmt]{ case _ ~ y => WriteStr(y) } ||
    (p"write" ~ p"(" ~ IdParser  ~ p")").map[Stmt]{ case _ ~ _ ~ y ~ _ => WriteVar(y) } ||
    (p"write" ~ p"(" ~ StrParser ~ p")").map[Stmt]{ case _ ~ _ ~ y ~ _ => WriteStr(y) } ||
    (p"if" ~ BExp ~ p"then" ~ Block ~ p"else" ~ Block)
      .map[Stmt]{ case _ ~ y ~ _ ~ u ~ _ ~ w => If(y, u, w) } ||
    (p"while" ~ BExp ~ p"do" ~ Block).map[Stmt]{ case _ ~ y ~ _ ~ w => While(y, w) }
)

//=> one or more statements or blocks of statements
lazy val Stmts: Parser[List[Token], Block] =
  (Stmt ~ p";" ~ Stmts).map[Block]{ case x ~ _ ~ z => x :: z } ||
  (Stmt.map[Block]{ s => List(s) })

//=> blocks (statement(s) enclosed in curly braces)
lazy val Block: Parser[List[Token], Block] =
  ((p"{" ~ Stmts ~ p"}").map{ case _ ~ y ~ _ => y } ||
   (Stmt.map(s => List(s))))

val Program = Stmts; // top-level of a program is statements/block

//================================================
// === Interpreter ===============================

// an interpreter for the WHILE language
// N.B.: 'newlines' bool determines whether each 'write' statement is printed on a new line (true), or as a single stream

type Env = Map[String, Int]

def eval_aexp(a: AExp, env: Env) : Int = a match {
  case Num(i) => i
  case Var(s) => env(s)
  case ApplyArithOp("+", a1, a2) => eval_aexp(a1, env) + eval_aexp(a2, env)
  case ApplyArithOp("-", a1, a2) => eval_aexp(a1, env) - eval_aexp(a2, env)
  case ApplyArithOp("*", a1, a2) => eval_aexp(a1, env) * eval_aexp(a2, env)
  case ApplyArithOp("/", a1, a2) => eval_aexp(a1, env) / eval_aexp(a2, env)
  case ApplyArithOp("%", a1, a2) => eval_aexp(a1, env) % eval_aexp(a2, env)
}

def eval_bexp(b: BExp, env: Env) : Boolean = b match {
  case True => true
  case False => false
  case ApplyBoolOp("==", a1, a2)  =>  eval_aexp(a1, env)  == eval_aexp(a2, env)
  case ApplyBoolOp("!=", a1, a2)  => !(eval_aexp(a1, env) == eval_aexp(a2, env))
  case ApplyBoolOp(">",  a1, a2)  =>  eval_aexp(a1, env)   > eval_aexp(a2, env)
  case ApplyBoolOp("<",  a1, a2)  =>  eval_aexp(a1, env)   < eval_aexp(a2, env)
  case ApplyBoolOp(">=", a1, a2)  =>  eval_aexp(a1, env)  >= eval_aexp(a2, env)
  case ApplyBoolOp("<=", a1, a2)  =>  eval_aexp(a1, env)  <= eval_aexp(a2, env)
  case And(b1, b2)                =>  eval_bexp(b1, env)  && eval_bexp(b2, env)
  case Or (b1, b2)                =>  eval_bexp(b1, env)  || eval_bexp(b2, env)
}

def eval_stmt(s: Stmt, env: Env, newlines: Boolean) : Env = s match {
  case Skip            => env
  case Assign(x, a)    => env + (x -> eval_aexp(a, env))
  case If(b, bl1, bl2) => if (eval_bexp(b, env)) eval_bl(bl1, env, newlines) else eval_bl(bl2, env, newlines)
  case While(b, bl) =>
    if (eval_bexp(b, env)) eval_stmt(While(b, bl), eval_bl(bl, env, newlines), newlines)
    else env
  case Read(v)     => { val x = scala.io.StdIn.readLine("prompt> "); eval_stmt(Assign(v, Num(x.toInt)), env, newlines) }
  case WriteVar(x) => { val v = env(x); if (newlines) println(s"stdout> '$v'") else print(v); env }
  case WriteStr(s) => { if (newlines) println(s"stdout> $s") else print(unescape(s).replaceAll("\n", "\nstdout> ")); env }
}

def eval_bl(bl: Block, env: Env, newlines: Boolean) : Env = bl match {
  case Nil => env
  case s::bl => eval_bl(bl, eval_stmt(s, env, newlines), newlines)
}

// interpreter entry point
def eval(bl: Block, newlines: Boolean = true) : Env = {
    if (!newlines) print("stdout> ");
    eval_bl(bl, Map(), newlines)
}

//================================================
// ============== MAIN ===========================

// PROGRAM DEFINITIONS:

// requires user input
val fib_while =
"""write "Fib";
read n;
minus1 := 0;
minus2 := 1;
while n > 0 do {
       temp := minus2;
       minus2 := minus1 + minus2;
       minus1 := temp;
       n := n - 1
};
write "Result";
write minus2
"""

// XXX - modify start value (default: 1000)
def while_three_while(start: Integer = 1000) =
s"""start := $start;
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

val prime_while  =
"""// prints out prime numbers from 2 to 100

end := 100;
n := 2;
while (n < end) do {
  f := 2;
  tmp := 0;
  while ((f < n / 2 + 1) && (tmp == 0)) do {
    if ((n / f) * f == n) then  { tmp := 1 } else { skip };
    f := f + 1
  };
  if (tmp == 0) then { write(n) } else { skip };
  n  := n + 1
}
"""

val collatz_while =
"""// Collatz series
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
}
"""

val parse_me = "if (a < b) then skip else a := a * b + 1"

// === TEST PROGRAMS =================
// parse_program(parse_me);
// lex_parse_run_program(fib_while,              "fib.while");
// lex_parse_run_program(prime_while,            "primes.while");
// lex_parse_run_program(while_three_while(100), "loops.while",   do_time=true);
// lex_parse_run_program(collatz_while,          "collatz.while", stdout_newline=false);
run_loop_benchmarks;

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

// Time a block of code and output time in seconds, return block value
def time[R](block: => R): R = {
    val start = System.nanoTime()
    val result = block // call-by-name
    val end = System.nanoTime()
    val dur = (end-start)/1000000000.0;
    println("\rElapsed time: " + "%.3f".format(dur) + " seconds")
    result
}

// Given a program, lex and parser it, giving the AST
def parse_program(program_text: String) = {
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
        pprint(parsed, toplevel=true);
        println();
  } else {
        println(parsed);
        println("No AST was returned, not interpreting");
  }
  println("=====================================");
  scala.io.StdIn.readLine("Press ENTER to continue...");
}

// Given a program string, lex and parse it, then run it with the interpreter
def lex_parse_run_program(
    program_text: String,            // program code (while language)
    program_name: String,            // filename of program
    do_time: Boolean = false,        // time how long the program took to run?
    stdout_newline: Boolean = true   // print each program 'write' on a newline?
  ) = {
    val title = s" $program_name ";
    val fullwidth = "=====================================".length();
    val padlength = ((fullwidth - title.length()) / 2).toInt;
    println((("="*padlength) + title + ("="*padlength)).padTo(fullwidth, "=").mkString + "\n");
    println(program_text);

    println("=====================================");
    println("============ LEXED TOKENS ===========\n");

    val tokens = Lexer.tokenize_string_program(program_text);
    Lexer.pretty_print_tokens(tokens);

    println();
    println("=====================================");
    println("============= PARSED AST ============");

    val parsed = Program.parse_all(tokens);

    if (!parsed.isEmpty) {
        pprint(parsed, toplevel=true);
        println();
        println("=====================================");
        println("=== RUNNING PROGRAM (INTERPRETED) ===");
        println(s"==> [$program_name]\n");

        println(">running...\n")

        val result = if (do_time) time{eval(parsed.head, stdout_newline)} else eval(parsed.head, stdout_newline);

        println("\n>done!\n");
        println("Resulting environment: (variable->value)")
        result foreach {case (key, value) => val k = key.padTo(10," ").mkString;println(s" - $k => $value")}

    } else {
        println(parsed);
        println("No AST was returned, not interpreting");
    }

    println("=====================================");
    scala.io.StdIn.readLine("Press ENTER to continue...");
}

// run benchmarks on loop program
def run_loop_benchmarks = {
  println("\nBenchmarking while-loop program...\n")
  val loop_sizes = (0 to 4).map(x => x * 100);
  loop_sizes.map(size => {
      val ast = Program.parse_all(Lexer.tokenize_string_program(while_three_while(size)));
      println(s"Start Value: $size");
      time {eval(ast.head)};
      println()
  })
  println("\nDone!\n")
}
