package scala.swing

abstract class SwingApplication extends Reactor {
  def main(args: Array[String]) = Swing.onEDT { startup(args) }
  
  def startup(args: Array[String])
  def quit() { shutdown(); sys.exit(0) }
  def shutdown() {}
}
