object Test extends Application {
  trait C[T] {
    def t: T
  }
  
  def b: Option[C[_]] = null
  
  def c = b match {
    case Some(b) => b.t
  } 
}
