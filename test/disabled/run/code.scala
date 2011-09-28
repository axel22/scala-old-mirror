import scala.tools.partest.utils.CodeTest

case class Element(name: String)

object Test extends App {
  case class InnerElement(name: String)
  def foo[T](ys: List[T]) = {
    val fun: reflect.Code[Int => Int] = x => x + ys.length
    fun
  }
  CodeTest(foo(List(2)), args)
  CodeTest({() => val e = Element("someName"); e}, args)
//  CodeTest({() => val e = InnerElement("someName"); e}, args) // (does not work yet)
  def titi() = {
    var truc = 0
    CodeTest(() => {
      truc = 6
    }, args)
  }
  def tata(): Unit = {
    var truc = 0
    CodeTest(() => {
      truc = truc + 6
    }, args)
  }
  titi()
  tata()
  new baz.A(args)
}

package baz {

  case class BazElement(name: String) { }

  class A(args: Array[String]) {
    CodeTest(() => new baz.BazElement("someName"), args)
  }

}



 

