package types

trait Generator[A] {
  def generate: A
}

object intGen extends Generator[Int] {
  override def generate: Int = scala.util.Random.nextInt
}
object stringGen extends Generator[String] {
  override def generate: String = scala.util.Random.nextString(intGen.generate)
}