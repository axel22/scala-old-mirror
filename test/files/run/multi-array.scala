object Test extends Application {
  val a = Array(1, 2, 3)
  println(a.deepToString)

  val aaiIncomplete = new Array[Array[Array[Int]]](3)
  println(aaiIncomplete(0))

  val aaiComplete: Array[Array[Int]] = Array.ofDim[Int](3, 3) // new Array[Array[Int]](3, 3)
  println(aaiComplete.deep)
  for (i <- 0 until 3; j <- 0 until 3)
    aaiComplete(i)(j) = i + j
  println(aaiComplete.deepToString)
  assert(aaiComplete.last.last == 4)
}
