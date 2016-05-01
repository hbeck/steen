package engine

/**
  * Created by FM on 05.04.16.
  */
case class Time(timePoint: Long) {
  override def toString = {
    timePoint.toString
  }
}

object Time {
  implicit val ordering = Ordering.by((time: Time) => time.timePoint)
}

object At {
  def second(seconds: Long) = Second(seconds)

  def minute(minutes: Long) = Minute(minutes)
}

object Second {
  def apply(seconds: Long) = Time(seconds * 1000)
}

object Minute {
  def apply(minutes: Long) = Second(minutes * 60)
}