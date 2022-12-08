import java.io.File
import scala.io.Source
import Term.*
import Expr.Nop
import Expr.Value
import Expr.Predicate
import Expr.Assignment

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
  case class Nop() extends Expr
  case class Value(value: String) extends Expr
  case class Pointer(binding: String) extends Expr
  case class Predicate(expr: Expr, check: Expr) extends Expr
  case class Or(left: Expr, right: Expr) extends Expr
  case class And(left: Expr, right: Expr) extends Expr
  case class Eq(left: Expr, right: Expr) extends Expr
  case class Assignment(receiver: Pointer, value: Expr) extends Expr
  case class Show(value: Expr) extends Expr
  case class Hide(value: Expr) extends Expr

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

def toAST(
    terms: List[Term],
    current: Expr,
    stack: List[Term] = List.empty
): Expr =
  if terms.isEmpty then current
  else
    terms.head match
      case Show => toAST(terms.tail, current, stack :+ Term.Show)
      case Hide => toAST(terms.tail, current, stack :+ Term.Hide)
      case Set  => toAST(terms.tail, current, stack :+ Term.Set)
      case When => Expr.Predicate(current, toAST(terms.tail, Expr.Nop(), stack))
      case Or   => Expr.Or(current, toAST(terms.tail, Expr.Nop(), stack))
      case And  => Expr.And(current, toAST(terms.tail, Expr.Nop(), stack))
      case To =>
        stack match
          case Set :: Pointer(binding) :: _ =>
            toAST(terms.tail, Expr.Nop(), stack :+ To)
          case _ => Expr.Nop()
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
            toAST(terms.tail, Expr.Nop(), stack :+ Pointer(binding))
          case _ => Expr.Nop()

@main def entrypoint =
  val statements = parse(read(File("example.txt")))
  statements.foreach(println)
  val ast = statements.map(statement => toAST(statement.terms, Expr.Nop()))
  ast.foreach(println)
