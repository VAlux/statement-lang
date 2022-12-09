import java.io.File
import scala.io.Source

enum Token:
  case Value extends Token
  case Operator extends Token
  case Keyword extends Token

sealed trait Term(token: Token)

object Term:
  import Token.*
  case object Show extends Term(Keyword)
  case object Hide extends Term(Keyword)
  case object Set extends Term(Keyword)
  case object When extends Term(Keyword)
  case object Or extends Term(Keyword)
  case object And extends Term(Keyword)
  case object To extends Term(Keyword)
  case object Eq extends Term(Operator)
  case class Val(content: String) extends Term(Token.Value)
  case class Pointer(binding: String) extends Term(Token.Value)

case class Statement(terms: List[Term])

sealed trait Expr
object Expr:
  case object Nop extends Expr
  case class Value(value: String) extends Expr
  case class Pointer(binding: String) extends Expr
  case class Predicate(expr: Expr, check: Expr) extends Expr
  case class Or(left: Expr, right: Expr) extends Expr
  case class And(left: Expr, right: Expr) extends Expr
  case class Eq(left: Expr, right: Expr) extends Expr
  case class Assignment(receiver: Pointer, value: Expr) extends Expr
  case class Show(value: Expr) extends Expr
  case class Hide(value: Expr) extends Expr

object Parser:
  import Term.*
  def parseTerms(source: String): List[Term] = source.split(" ").toList.map {
    case "Show"                         => Show
    case "Hide"                         => Hide
    case "Set"                          => Set
    case "when"                         => When
    case "or"                           => Or
    case "and"                          => And
    case "to"                           => To
    case "=="                           => Eq
    case token if token.startsWith("*") => Pointer(token.drop(1))
    case token                          => Val(token)
  }

  def parse(statements: List[String]): List[Statement] =
    statements.map(terms => Statement(parseTerms(terms)))

def read(file: File): List[String] =
  Source.fromFile(file, "UTF-8").getLines().toList

object AST:
  import Term.*
  def toAST(
      terms: List[Term],
      current: Expr = Expr.Nop,
      stack: List[Term] = List.empty
  ): Expr =
    if terms.isEmpty then current
    else
      terms.head match
        case Show => toAST(terms.tail, current, stack :+ Term.Show)
        case Hide => toAST(terms.tail, current, stack :+ Term.Hide)
        case Set  => toAST(terms.tail, current, stack :+ Term.Set)
        case When => Expr.Predicate(current, toAST(terms.tail, Expr.Nop, stack))
        case Or   => Expr.Or(current, toAST(terms.tail, stack = stack))
        case And  => Expr.And(current, toAST(terms.tail, stack = stack))
        case To =>
          stack match
            case Set :: Pointer(binding) :: _ =>
              toAST(terms.tail, stack = stack :+ To)
            case _ => Expr.Nop
        case Eq => toAST(terms.tail, current, stack :+ Eq)
        case Val(content) =>
          stack match
            case Set :: Pointer(binding) :: To :: _ =>
              toAST(
                terms.tail,
                Expr.Assignment(Expr.Pointer(binding), Expr.Value(content)),
                stack.drop(3)
              )
            case Eq :: _ =>
              toAST(
                terms.tail,
                Expr.Eq(current, Expr.Value(content)),
                stack.drop(1)
              )
            case _ => toAST(terms.tail, Expr.Value(content), stack)
        case Pointer(binding) =>
          stack.head match
            case Show =>
              toAST(terms.tail, Expr.Show(Expr.Pointer(binding)), stack.init)
            case Hide =>
              toAST(terms.tail, Expr.Hide(Expr.Pointer(binding)), stack.init)
            case Set =>
              toAST(terms.tail, stack = stack :+ Pointer(binding))
            case _ => Expr.Nop

object Interpreter:
  import Expr.*
  def asString(root: Expr): String =
    root match
      case Nop              => "NOP"
      case Value(value)     => s"$value"
      case Pointer(binding) => s"*$binding"
      case Predicate(expr, check) =>
        s"if ${asString(check)} then ${asString(expr)}"
      case Or(left, right)  => s"(${asString(left)} or ${asString(right)})"
      case And(left, right) => s"(${asString(left)} and ${asString(right)})"
      case Eq(left, right)  => s"${asString(left)} == ${asString(right)}"
      case Assignment(receiver, value) =>
        s"${asString(receiver)} = ${asString(value)}"
      case Show(value) => s"Show ${asString(value)}"
      case Hide(value) => s"Hide ${asString(value)}"

  def asGraphVis(root: Expr, level: Int = 0, parent: String = "ROOT"): String =
    root match
      case Nop              => s"$parent -- NOP\n"
      case Value(value)     => s"$parent -- \"$value\"\n"
      case Pointer(binding) => s"$parent -- \"*($binding)\"\n"
      case Predicate(expr, check) =>
        s"PREDICATE$level [label=\"PREDICATE\"]\n" +
        s"$parent -- PREDICATE$level\n" +
        asGraphVis(check, level + 1, s"PREDICATE$level") +
        asGraphVis(expr, level + 2, s"PREDICATE$level")
      case Or(left, right) =>
        s"OR$level [label=\"OR\"]\n" +
        s"$parent -- OR$level\n" +
        asGraphVis(left, level + 1, s"OR$level") +
        asGraphVis(right, level + 2, s"OR$level")
      case And(left, right) =>
        s"AND$level [label=\"AND\"]\n" +
        s"$parent -- AND$level\n" +
        asGraphVis(left, level + 1, s"AND$level") +
        asGraphVis(right, level + 2, s"AND$level")
      case Eq(left, right) =>
        s"EQ$level [label=\"EQ\"]\n" +
        s"$parent -- EQ$level\n" +
        asGraphVis(left, level + 1, s"EQ$level") +
        asGraphVis(right, level + 2, s"EQ$level")
      case Assignment(receiver, value) =>
        s"ASSIGNMENT$level [label=\"ASSIGNMENT\"]\n" +
        s"$parent -- ASSIGNMENT$level\n" +
        asGraphVis(receiver, level + 1, s"ASSIGNMENT$level") +
        asGraphVis(value, level + 2, s"ASSIGNMENT$level")
      case Show(value) => s"$parent -- \"SHOW$level ($value)\"\n"
      case Hide(value) => s"$parent -- \"HIDE$level ($value)\"\n"

@main def entrypoint =
  val statements = Parser.parse(read(File("example.txt")))
  val ast = statements.map(statement => AST.toAST(statement.terms))

  statements.foreach(println)
  ast.foreach(println)
  ast.foreach(root => println(Interpreter.asString(root)))
  ast.foreach(root => println(Interpreter.asGraphVis(root)))
