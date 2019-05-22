package types
import cats.{Group, Monoid}
import cats.implicits._
import cats.kernel.Semigroup

object FolderCounters {
  sealed trait Folder extends Serializable with Product
  case object Active extends Folder
  case object Shared extends Folder
  case object Resolved extends Folder

  type Counters = Map[Folder, Int]

  case class Alert(data: String, folders: Set[Folder])

  trait myGroup[A] {
    def combine(x: A, y: A): A
    def remove(x: A, y: A): A = combine(x, negate(y))
    def negate(x: A): A
    def empty(): A
  }

  trait CountersApi {
    def addToCounters(current: Counters, alert: Alert): Counters
    def removeFromCounters(current: Counters, alert: Alert): Counters
  }

  object CountersApiV1Instance extends CountersApi {

    /**
      * Please notice, the current code ignores the `current` parameter and always returns the counters taken from the alert.
      * This is an intentional bug left here waiting to be found by the tests.
      */
    override def addToCounters(current: Counters, alert: Alert): Counters =
      alert.folders.groupBy(identity).mapValues(_.size)

    override def removeFromCounters(current: Counters,
                                    alert: Alert): Counters = {
      val currentFoldersCount = current.toList
      val alertFoldersCount =
        alert.folders.groupBy(identity).mapValues(_.size * -1).toList
      (currentFoldersCount ++ alertFoldersCount)
        .groupBy(_._1)
        .mapValues(_.map(_._2).sum)
    }
  }

  object CountersApiV2Instance extends CountersApi {

    def fromAlert(alert: Alert): Counters =
      alert.folders.groupBy(identity).mapValues(_.size)

    override def addToCounters(current: Counters, alert: Alert): Counters = {
      val alertFoldersCount =
        alert.folders.groupBy(identity).mapValues(_.size).toList
      (current.toList ++ alertFoldersCount)
        .groupBy(_._1)
        .mapValues(_.map(_._2).sum)
    }

    override def removeFromCounters(current: Counters,
                                    alert: Alert): Counters = {
      val countersToBeRemoved = fromAlert(alert)
      countersToBeRemoved.foldLeft(current) {
        case (counters, (folder, count)) =>
          counters
            .get(folder)
            .map(_ - count)
            .fold(counters)(newFolderCount =>
              counters.updated(folder, newFolderCount))
      }
    }
  }

  object CountersApiV3Instance extends CountersApi {

    def fromAlert(alert: Alert): Counters =
      alert.folders.groupBy(identity).mapValues(_.size)

    override def addToCounters(current: Counters, alert: Alert): Counters = {
      val alertFoldersCount =
        alert.folders.groupBy(identity).mapValues(_.size).toList
      (current.toList ++ alertFoldersCount)
        .groupBy(_._1)
        .mapValues(_.map(_._2).sum)
    }

    override def removeFromCounters(current: Counters,
                                    alert: Alert): Counters = {
      val countersToBeRemoved = fromAlert(alert)
      countersToBeRemoved.foldLeft(current) {
        case (counters, (folder, count)) =>
          counters
            .get(folder)
            .map(_ - count)
            .fold(counters)(newFolderCount =>
              if (newFolderCount != 0) {
                counters.updated(folder, newFolderCount)
              } else {
                counters - folder
            })
      }
    }
  }


  object CountersApi extends Group[Counters] {
    override def combine(x: Counters, y: Counters): Counters =
      Semigroup[Counters].combine(x, y)

    override def inverse(x: Counters): Counters = x.mapValues(_ * -1)

    override def empty(): Counters = Map()
  }
}
