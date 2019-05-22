import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FunSuiteLike, Matchers, WordSpecLike}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.typelevel.discipline.scalatest.Discipline
import types.FolderCounters._
class FolderCountersSpecStub extends WordSpecLike with Matchers {
  val api: CountersApi = CountersApiV1Instance
  "addToCounters" should {
    "increment the Active count by 1 when adding an Active alert" in {}
    "increment the Shared count by 1 when adding a Shared alert" in {}
    "increment the Resolved count by 1 when adding a Resolved alert" in {}
    "increment all folders counts by 1 when adding alert that sits on all folders" in {}
  }
  "removeFromCounters" should {
    "decrement the Active count by 1 when removing an Active alert" in {}
    "decrement the Shared count by 1 when removing a Shared alert" in {}
    "decrement all counts by 1 when removing an alert that sits on all folders" in {}
  }
}

class FolderCountersSpec extends WordSpecLike with Matchers {
  val api: CountersApi = CountersApiV1Instance
  import CountersApiV1Instance.{addToCounters,removeFromCounters}

  "addToCounters" should {
    "increment the Active count by 1 when adding an Active alert" in {
      val alert = Alert("High CPU in elastic 11", Set(Active))
      addToCounters(Map(), alert) shouldEqual Map(Active -> 1)
    }
    "increment all folders counts by 1 when adding alert that sits on all folders" in {
      val alert =
        Alert("High CPU in elastic 11", Set(Resolved, Shared, Active))
      addToCounters(Map(), alert) shouldEqual Map(Resolved -> 1,
                                                      Active -> 1,
                                                      Shared -> 1)
    }
    "increment the Shared count by 1 when adding a Shared alert" in {
      val sharedAlert = Alert("High CPU in elastic 11", Set(Shared))
      addToCounters(Map(), sharedAlert) shouldEqual Map(Shared -> 1)
    }
    "increment the Resolved count by 1 when adding a Resolved alert" in {
      val resolveAlert = Alert("High CPU in elastic 11", Set(Resolved))
      addToCounters(Map(), resolveAlert) shouldEqual Map(Resolved -> 1)
    }
  }
  "removeFromCounters" should {
    "decrement the Active count by 1 when removing an Active alert" in {
      val activeAlert = Alert("High CPU in elastic 11", Set(Active))
      removeFromCounters(Map(Active -> 3), activeAlert) shouldEqual Map(
        Active -> 2)
    }
    "decrement the Shared count by 1 when removing a Shared alert" in {
      val sharedAlert = Alert("High CPU in elastic 11", Set(Shared))
      removeFromCounters(Map(Shared -> 3), sharedAlert) shouldEqual Map(
        Shared -> 2)
    }
    "decrement all counts by 1 when removing an alert that sits on all folders" in {
      val sitsInAllAlert =
        Alert("High CPU in elastic 11", Set(Shared, Active, Resolved))
      removeFromCounters(Map(Shared -> 3, Resolved -> 3, Active -> 3),
                             sitsInAllAlert) shouldEqual Map(Shared -> 2,
                                                             Active -> 2,
                                                             Resolved -> 2)
    }
  }
}

import org.scalatest.prop.GeneratorDrivenPropertyChecks
trait FolderPropertiesSpec
    extends WordSpecLike
    with Matchers
    with GeneratorDrivenPropertyChecks

class FolderCountersPropertySpec extends FolderPropertiesSpec {
  import FolderCountersSpec.{countersArb, alertsGenArb}
  val api: CountersApi = types.FolderCounters.CountersApiV1Instance
  import CountersApiV1Instance._

  "increment the alert folders" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(counters, alert) shouldEqual ???
  }
  "hello world program" in forAll { lst: List[String] =>
    lst.reverse.reverse == lst
  }
  "adding the empty alert should not change the counters" in forAll {
    counters: Map[Folder, Int] =>
      addToCounters(counters, Alert("", Set())) shouldEqual counters
  }
}

class FolderCountersPropertySpecWorking extends FolderPropertiesSpec {
  import FolderCountersSpec.{countersArb, alertsGenArb}
  import types.FolderCounters.CountersApiV2Instance._

  "adding the empty alert should not change the counters" in forAll {
    counters: Map[Folder, Int] =>
      addToCounters(counters, Alert("", Set())) shouldEqual counters
  }
  "Removing and adding an alert should keep the original counters" in forAll(
    minSuccessful(1000)) { (counters: Map[Folder, Int], alert: Alert) =>
    addToCounters(removeFromCounters(counters, alert), alert) shouldEqual counters
  }
  "Adding and then removing an alert should keep the original counters" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      removeFromCounters(addToCounters(counters, alert), alert) shouldEqual counters
  }

  "Adding a non empty alert to counters should change the counters" in forAll(minSuccessful(1000)) {
    (alert: Alert, counters: Counters) =>
      whenever(alert.folders.nonEmpty) {
        val updatedCounters = addToCounters(counters, alert)
        alert.folders.foreach { folder =>
          updatedCounters(folder) > counters.getOrElse(folder, 0) shouldEqual true
        }
      }
  }
}

object FolderCountersSpec {
  import types.FolderCounters._
  val folderToCounterGen: Gen[(Folder, Int)] = for {
    folder <- Gen.oneOf(Active, Resolved, Shared)
    counter <- Gen.posNum[Int]
  } yield folder -> counter

  implicit val countersGen: Gen[Map[Folder, Int]] =
    Gen.listOf(folderToCounterGen).map(_.toMap)

  val alertGen = for {
    data <- Gen.numStr
    folders <- Gen.listOf(Gen.oneOf(Active, Resolved, Shared))
  } yield Alert(data.take(15), folders.toSet)

  implicit val alertsGenArb: Arbitrary[Alert] = Arbitrary(alertGen)

  implicit val countersArb: Arbitrary[Counters] = Arbitrary(countersGen)

}

import cats.kernel.Semigroup
import cats.kernel.Group
object CountersApiGroup extends Group[Counters] {
  import cats.implicits._

  override def combine(x: Counters, y: Counters): Counters =
    Semigroup[Counters].combine(x, y).filter(_._2 != 0)

  override def inverse(x: Counters): Counters = x.mapValues(_ * -1)

  override def empty(): Counters = Map()
}

import cats.laws.discipline._
import cats.kernel.laws.discipline.GroupTests
import org.scalatest.FunSuiteLike
trait GroupSpec extends Discipline with FunSuiteLike

class GroupsTests extends GroupSpec {

  implicit val countersApi = CountersApiGroup
  import cats.kernel.Eq
  implicit val countersEq: Eq[Counters] = Eq.fromUniversalEquals
  checkAll(
    "CountersGroupTests",
    GroupTests[Counters].group(FolderCountersSpec.countersArb, countersEq))
}
