import org.scalacheck._
import org.scalacheck.Prop._

import scala.tools.nsc.Global
import scala.tools.nsc.doc
import scala.tools.nsc.doc.model.comment._

class Factory(val g: Global, val s: doc.Settings)
  extends doc.model.ModelFactory(g, s) {
  thisFactory: Factory with CommentFactory with doc.model.TreeFactory =>

  def strip(c: Comment): Option[Inline] = {
    c.body match {
      case Body(List(Paragraph(Chain(List(Summary(inner)))))) => Some(inner)
      case _ => None
    }
  }

  def parseComment(s: String): Option[Inline] =
    strip(parse(s, "", scala.tools.nsc.util.NoPosition))
}

object Test extends Properties("CommentFactory") {
  val factory = {
    val settings = new doc.Settings((str: String) => {})
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    val g = new Global(settings, reporter)
    (new Factory(g, settings) with CommentFactory with doc.model.TreeFactory)
  }

  def parse(src: String, dst: Inline) = {
    factory.parseComment(src) match {
        case Some(inline) =>
          inline == dst
        case _ =>
          false
    }
  }

  property("parse") = parse(
      "/** One two three */",
      Text("One two three")
  )
  property("parse") = parse(
    "/** One `two` three */",
    Chain(List(Text("One "), Monospace("two"), Text(" three")))
  )

  property("parse") = parse(
      """
/** One two
  * three */""",
      Text("One two\nthree")
  )
  property("parse") = parse(
      """
/** One `two`
  * three */""",
      Chain(List(Text("One "), Monospace("two"), Text("\n"), Text("three")))
  )

  property("parse") = parse(
      """
/** One `two`
 *  three */""",
      Chain(List(Text("One "), Monospace("two"), Text("\n"), Text(" three")))
  )

  property("parse") = parse(
      """
/** One
  * `two` three */""",
      Chain(List(Text("One"), Text("\n"), Monospace("two"), Text(" three")))
  )

  property("Trac #4361 - ^...^") = parse(
      """
/**
 * hello ^world^ */""",
      Chain(List(Text("hello "), Superscript(Text("world"))))
  )

  property("Trac #4361 - single ^ symbol") = parse(
      """
/**
 * <pre>
 * hello ^world
 * </pre>
 *
 */""",
      Chain(List(Text(""), Text("\n"),
                 HtmlTag("<pre>"), Text("\n"),
                 Text("hello "), Chain(List(Text("^"),
                                            Chain(List(Text("world"),
                                                       Text("\n"),
                                                       HtmlTag("</pre>")))))))
  )
}
