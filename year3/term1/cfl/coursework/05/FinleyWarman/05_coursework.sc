// A Small LLVM Compiler for a Simple Functional Language
// Finley Warman

import $file.parser
import parser._

// ==================================

// Generated Program Data:

// Global Const List
val globals = collection.mutable.Map[String, String]()

// Function Definitions Return Types
val function_types = collection.mutable.Map[String, String]()

// ==================================

// Conversions from 'fun' language types to LLVM-IR types
val type_to_llvm_type = Map(
  "Void"   -> "void",
  "Int"    -> "i32",
  "Double" -> "double"
)

// ==================================

// For generating new labels
var counter = -1

def Fresh(x: String) = {
  counter += 1
  x ++ "_" ++ counter.toString()
}

// ==================================

// Internal CPS language for FUN
abstract class KExp
abstract class KVal

case class KVar(s: String, ty: String = "UNDEF") extends KVal
case class KGlob(s: String) extends KVal // loading global const

case class KNum (i: Int) extends KVal    // integer
case class KFNum(f: Float) extends KVal  // float

case class Kop (o: String, v1: KVal, v2: KVal) extends KVal // integer operation
case class Kdop(o: String, v1: KVal, v2: KVal) extends KVal // float operation

case class KCall(o: String, vrs: List[KVal]) extends KVal   // function call
case class KWrite(v: KVal) extends KVal

case class KNone() extends KVal                   // empty value (no code)
case class KPass(e1: KVal, e2: KExp) extends KExp // generate current value and continue

case class KLet(x: String, e1: KVal, e2: KExp) extends KExp { // llvm var assignment
  override def toString = s"LET $x = $e1 in \n$e2"
}
case class KIf(x1: String, e1: KExp, e2: KExp) extends KExp { // if-else expression
  def pad(e: KExp) = e.toString.replaceAll("(?m)^", "  ")

  override def toString =
     s"IF $x1\nTHEN\n${pad(e1)}\nELSE\n${pad(e2)}"
}

case class KReturn(v: KVal) extends KExp // return value from function

// ==================================

// Type Inference

// Determine type of binary expression based on known operand types
def type_of_bin_exp(lhs: KVal, rhs: KVal) = (lhs, rhs) match {
  case (KVar(_, "i32"), _) => "i32"
  case (_, KVar(_, "i32")) => "i32"

  case (KVar(_, "double"), _) => "double"
  case (_, KVar(_, "double")) => "double"

  case (_, KNum(_)) => "i32"
  case (KNum(_), _) => "i32"

  case (_, KFNum(_)) => "double"
  case (KFNum(_), _) => "double"

  case _  => throw new Exception(
    s"Type Inference Error for Binary Expression:\n-> ${lhs}\n-> ${rhs}")
}

def type_of_unary_exp(exp: KVal) = exp match {
  case KVar(_, t)   if t != "UNDEF" => t
  case KNum(_)      => "i32"
  case KFNum(_)     => "double"
  case _  =>  throw new Exception(
    s"Type Inference Error for KVal: ${exp}")
}

// Generate the correct binary op K-expression for the given type (Kop / Kdop)
def get_typed_kop_exp(typ: String, op: String, lhs: KVal, rhs: KVal) = typ match {
  case "i32"    => Kop (op, lhs, rhs)
  case "double" => Kdop(op, lhs, rhs)
  case _  =>  throw new Exception(
    s"Typing Error for '${op}' operation, Invalid Type: ${typ}")
}

// ==================================

// CPS translation from Exps to KExps using a continuation k
def CPS(e: Exp, fn: String, env: Map[String, String])(k: KVal => KExp) : KExp = e match {

  case Var(s) => if (globals.contains(s)) {   // type is known from global consts
                  val z = Fresh("glob_tmp")
                  KLet(z, KGlob(s), k(KVar(z, globals(s))))
                } else if (env.contains(s)) { // type is known from local var env
                  k(KVar(s, env(s)))
                } else k(KVar(s))             // var type is (currently) unknown

  case Num(i)  => k(KNum(i))
  case FNum(i) => k(KFNum(i))

  case ApplyArithOp(o, e1, e2) => {
    val z = Fresh("tmp")
    CPS(e1, fn, env)(y1 =>
      CPS(e2, fn, env)(y2 => {
        val exp_type = type_of_bin_exp(y1, y2)
        val kvar = KVar(z, exp_type)
        val op   = get_typed_kop_exp(exp_type, o, y1, y2)
        KLet(z, op, k(kvar))
      }
    ))
  }

  case If(ApplyBoolOp(o, b1, b2), e1, e2) => {
    val z = Fresh("tmp")
    CPS(b1, fn, env)(y1 =>
      CPS(b2, fn, env)(y2 => {
        val exp_type = type_of_bin_exp(y1, y2)
        val op   = get_typed_kop_exp(exp_type, o, y1, y2)
        KLet(z, op, KIf(z, CPS(e1, fn, env)(k), CPS(e2, fn, env)(k)))
      }
    ))
  }

  case Call(name, args) => {
    def aux(args: List[Exp], vs: List[KVal]) : KExp = args match {
      case Nil => {
        if (function_types(name) == "void") {
          KPass(KCall(name, vs), k(KNone()))
        } else {
          val z = Fresh("tmp")
          val call_type = function_types(name)
          KLet(z, KCall(name, vs), k(KVar(z, call_type)))
        }
      }
      case e::es => CPS(e, fn, env)(y => aux(es, vs ::: List(y)))
    }
    aux(args, Nil)
  }

  case Sequence(e1, e2) =>
    CPS(e1, fn, env)(_ => CPS(e2, fn, env)(y2 => k(y2)))
  case Write(e) => {
    val z = Fresh("tmp")
    CPS(e, fn, env)(y => KLet(z, KWrite(y), k(KVar(z))))
  }
}

// Initial Continuation
def CPSi(e: Exp, fn: String, env: Map[String, String]) = CPS(e, fn, env)(KReturn)

// ==================================

// String interpolation helpers (for code generation)
implicit def string_inters(sc: StringContext) = new {
    def i(args: Any*): String = "   " ++ sc.s(args:_*) ++ "\n"
    def l(args: Any*): String = sc.s(args:_*) ++ ":\n"
    def m(args: Any*): String = sc.s(args:_*) ++ "\n"
}

// ==================================

// Compilation

// Mathematical and Boolean operations
// - Integer
def compile_op(op: String) = op match {
  case "+"  => "add i32 "
  case "*"  => "mul i32 "
  case "-"  => "sub i32 "
  case "/"  => "sdiv i32 "
  case "%"  => "srem i32 "
  case "==" => "icmp eq i32 "
  case "<=" => "icmp sle i32 "
  case "<"  => "icmp slt i32 "
}
// - Double (float)
def compile_dop (op: String) = op match {
  case "+"  => "fadd double "
  case "*"  => "fmul double "
  case "-"  => "fsub double "
  case "==" => "fcmp oeq double "
  case "<=" => "fcmp ole double "
  case "<"  => "fcmp olt double "
}

// Compile K Values
def compile_val(v: KVal) : String = v match {
  case KNone() => ""

  case KNum(i)  => s"$i"
  case KFNum(i) => s"$i"

  case KVar(s, t) => s"%$s"
  case KGlob(s)   => {val t = globals(s); s"load $t, $t* @$s"}

  case Kop(op, x1, x2) => s"${compile_op(op)} ${compile_val(x1)}, ${compile_val(x2)}"
  case Kdop(op, x1, x2) => s"${compile_dop(op)} ${compile_val(x1)}, ${compile_val(x2)}"

  case KCall(x1, args) => {
    val args_list = args.map(v => s"${type_of_unary_exp(v)} ${compile_val(v)}").mkString(",")
    s"call ${function_types(x1)} @$x1 ($args_list)"
  }

  case KWrite(x1) =>
    s"call i32 @printInt (i32 ${compile_val(x1)})"
}

// Compile K Expressions
def compile_exp(a: KExp) : String = a match {
  case KReturn(v) => v match {
    case KNone()    => i"ret void"
    case KNum(i)    => i"ret i32 ${i}"
    case KFNum(i)   => i"ret double ${i}"
    case KVar(s, t) => i"ret $t ${compile_val(v)}"
  }

  case KLet(x: String, v: KVal, e: KExp) =>
    i"%$x = ${compile_val(v)}" ++ compile_exp(e)

  // compile given value with no side-effects
  case KPass(e1, e2) =>
    i"${compile_val(e1)}" ++ compile_exp(e2)

  case KIf(x, e1, e2) => {
    val if_br = Fresh("if_branch")
    val else_br = Fresh("else_branch")
    i"br i1 %$x, label %$if_br, label %$else_br" ++
    l"\n$if_br" ++
    compile_exp(e1) ++
    l"\n$else_br" ++
    compile_exp(e2)
  }
}

// ==================================

val prelude = """

; =============== Prelude ===============

declare i32 @printf(i8*, ...)

@.str_nl = private constant [2 x i8] c"\0A\00"
@.str_star = private constant [2 x i8] c"*\00"
@.str_space = private constant [2 x i8] c" \00"

define void @new_line() #0 {
  %t0 = getelementptr [2 x i8], [2 x i8]* @.str_nl, i32 0, i32 0
  %1 = call i32 (i8*, ...) @printf(i8* %t0)
  ret void
}

define void @print_star() #0 {
  %t0 = getelementptr [2 x i8], [2 x i8]* @.str_star, i32 0, i32 0
  %1 = call i32 (i8*, ...) @printf(i8* %t0)
  ret void
}

define void @print_space() #0 {
  %t0 = getelementptr [2 x i8], [2 x i8]* @.str_space, i32 0, i32 0
  %1 = call i32 (i8*, ...) @printf(i8* %t0)
  ret void
}

define void @skip() #0 {
  ret void
}

@.str = private constant [4 x i8] c"%d\0A\00"

define void @print_int(i32 %x) {
   %t0 = getelementptr [4 x i8], [4 x i8]* @.str, i32 0, i32 0
   call i32 (i8*, ...) @printf(i8* %t0, i32 %x)
   ret void
}

; =======================================
; ========= Start of User Code ==========

"""

// ==================================

function_types("printf")      = "i32"
function_types("new_line")    = "void"
function_types("print_star")  = "void"
function_types("print_space") = "void"
function_types("skip")        = "void"
function_types("print_int")   = "void"

import scala.collection.mutable.ListBuffer

// compile function for declarations and main
def compile_decl(d: Decl) : String = d match {
  case Const(name, v) => {
    globals(name) = "i32"
    m"@$name = global i32 $v \n"
  }
  case FConst(name, v) => {
    globals(name) = "double"
    m"@$name = global double $v \n"
  }
  case Def(name, args, typ, body) => {
    function_types(name) = type_to_llvm_type(typ)

    val args_list = args.map{case (n,t) => s"${type_to_llvm_type(t)} %$n"}.mkString(", ");

    // env - store list of known types for all variables (initially populate with argument types)
    val env = args.map{case (n, t) => n -> type_to_llvm_type(t)}.toMap
    println(env);

    val ret_type = function_types(name)
    m"define $ret_type @$name ($args_list) {" ++
      compile_exp(CPSi(body, name, env)) ++
    m"}\n"
  }
  case Main(body) => {
    function_types("main") = "i32"

    m"define i32 @main() {" ++
      compile_exp(CPS(body, "main", Map())(_ => KReturn(KNum(0)))) ++
    m"}\n"
  }
}

// Main Compiler Function (Generate LLVM-IR Code)
def compile(prog: List[Decl]) : String =
  prelude ++ (prog.map(compile_decl).mkString)

// ==================================

def load_program(fname: String): String = {
    val path = os.pwd / "programs" / (fname+".fun")
    val file = fname.stripSuffix("." ++ path.ext)
    os.read(path)
}

def lex_parse_write_run(code: String, program_name: String) = {
  val parsed_ast = Parser.parse_program(code, program_name + ".fun")
  val llvm_code  = compile(parsed_ast);

  println("\n============= GENERATED LLVM ============\n");
  println(llvm_code);

  // write to file
  val build_dir = "build/" + program_name + "/"
  os.makeDir.all(os.pwd / "build" / program_name)

  val fname = program_name + ".ll"
  val path = os.pwd / "build" / program_name / fname
  val file = fname.stripSuffix("." ++ path.ext)
  os.write.over(os.pwd / "build" / program_name / (file ++ ".ll"), llvm_code)

  println("=========================================")
  println("============= COMPILING LLVM: ============\n");

  // compile and run
  os.proc("llc", "-filetype=obj", build_dir + file ++ ".ll").call()
  os.proc("gcc", build_dir + file ++ ".o", "-o", build_dir + file ++ ".bin").call()
  println("Done.")

  println("\n============= RUNNING LLVM: ============\n");

  os.proc(os.pwd / "build" / program_name / (file ++ ".bin")).call(stdout = os.Inherit)

  println("\n=========================================")
  println("Done.")
}

// ==================================

// Generate code for fun program and run:
//val prog = "mand" // TODO
val prog = "sqr"

val code = load_program(prog)
lex_parse_write_run(code, prog)
