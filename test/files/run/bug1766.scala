object Test extends Application {
  
  class C(s: String) {
  
    def this(i: Int) = this("bar")
    
    def f = {
      val v: { def n: Int } = new { val n = 3 }
      v.n
    }
    
  }
  
  new C("foo").f
  
}
