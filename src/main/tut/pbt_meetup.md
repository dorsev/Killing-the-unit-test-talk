autoscale: true
slidenumbers: true
footer: Killing the unit test - @dor_sever


#[Fit] Killing the Unit test
dor sever @ bigpanda

![inline 100%,right](./images/myImage.png)



---

[.hide-footer]

#[Fit]The missing
#[Fit] Unit Test problem

---

[.hide-footer]

#[Fit]Solving the missing unit test problem

> Real life example: from Unit Test to Property Based Tests.

^ Show how PBT provides real bossiness value
^ And not just in toy examples
^ By the end of this talk, you will know what PBT and hot it solves this problems.

---

[.hide-footer]

![Fit](./images/itsStoryTime.jpg)

---

[.hide-footer]
![](./images/spaceIL.jpg)


---

[.hide-footer]

![Fit](./images/emptyAlerts.jpg)

---

[.hide-footer]

![Fit](./images/alerts.jpg)

---

[.hide-footer]

![Fit](./images/alerts2.jpg)

---

[.hide-footer]
#[Fit]Implement counters for folders

---

[.hide-footer]

![Fit](./images/FoldersHierarchy.jpg)

^ Let us implement the types

---

[.hide-footer]

```tut:invisible
trait Folder
case object Active extends Folder
case object Shared extends Folder
case object Resolved extends Folder
```

```scala
sealed trait Folder
case object Active extends Folder
case object Shared extends Folder
case object Resolved extends Folder
```

---

[.hide-footer]

![Fit](./images/AnAlert.jpeg)

---

[.hide-footer]

```tut:silent
case class Alert(data: String, folders: Set[Folder])
```

---

[.hide-footer]

## The requirements

* Add Alerts to current folder counters.
* Remove Alerts from folder counters.
* Direct access to a folder's count. 

^ Let's try to write our implementation down


---

[.hide-footer]
[.code-highlight: 1]

```tut:silent
type Counters = Map[Folder, Int]

trait CountersApi {
    
    def addToCounters(current: Counters,
                      alert: Alert): Counters 
   
     def removeFromCounters(current: Counters,
                            alert: Alert): Counters 
}
```

---

[.hide-footer]
[.code-highlight: 5-6,8-9]

```tut:silent
type Counters = Map[Folder, Int]

trait CountersApi {
    
    def addToCounters(current: Counters,
                      alert: Alert): Counters 
   
     def removeFromCounters(current: Counters,
                            alert: Alert): Counters 
}
```

---

[.hide-footer]

![Fit](./images/alerts.jpg)

^ Let us implment this specification into tests

---

[.hide-footer]
## Imports


```tut:invisible
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
          (currentFoldersCount ++ alertFoldersCount).groupBy(_._1).mapValues(_.map(_._2).sum)
        }
  }
```

```tut:silent
import org.scalatest._
trait TestSpec extends WordSpecLike with Matchers
import CountersApiV1Instance.addToCounters
```


  
---

[.hide-footer]

```tut:silent
class FolderCountersSpec extends TestSpec {
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
```

^ This looks like a good test coverage. 
^ 3 - 4 tests per feature.

---

[.hide-footer]
[.code-highlight: 2, 4-8]
```tut:silent
class FolderCountersSpec extends TestSpec {
 val emptyCounters = Map[Folder,Int]()
 "addToCounters" should {
   "increment the Active count by 1 when adding an Active alert" in {
     val activeAlert = 
            Alert("High CPU in elastic 11", Set(Active))
     addToCounters(emptyCounters, activeAlert) shouldEqual Map(Active -> 1)
   }
 }
}
```

^ Talk about the good stuff in this implementation
^ Talk about the problem here - Are we done?! did we write all the tests we need?
^ No way to know! so lets run some test!

---

[.hide-footer]

![Fit](./images/incrementCountersPassingv1.jpg)

^ The problem : Are we done!? we are not sure!!!
^ we dont know any better, so we deploy to production! the manager comes to us and tells us there is a bug.

---

[.hide-footer]

![Fit](./images/home-sweet-home.jpg)

---

[.hide-footer]

![Fit](./images/theBoss.jpg)

---

[.hide-footer]

## The bug

---

[.hide-footer]

![inline](./images/TheBugInOurCode.jpg)

^ Why did we not find this in our tests?

---

[.hide-footer]
## The original test

---

[.hide-footer]

![inline](./images/TheOriginalTest.jpg)


---

[.hide-footer]

## The missing unit test problem

---

[.hide-footer]

![inline](./images/ThePartialValuesProblem.jpg)

^ The input partiality problem
^ hard coded inputs in tests. 
^ As it turns out Property based tests allows us to solve this problem

---

[.hide-footer]

## Property Based Tests

* Property based tests work by defining a property

---

[.hide-footer]

## Property Based Tests

* Property based tests work by defining a property
which is a high-level specification of behavior

---

[.hide-footer]

## Property Based Tests

* Property based tests work by defining a property, 
 which is a high-level specification of behavior
 that should hold for all values of the specific type.

---

[.hide-footer]

## Property Based Tests

* Property based tests work by defining a property, 
 which is a high-level specification of behavior
 that should hold for all values of the specific type.
 
 <br>
* > ∀ value  : T =>  property(value) == true

---

[.hide-footer]

## The white lie
* We can't really test all values.
* We generate *100* random inputs, and verify the property on them.

---

[.hide-footer]

## my first property

* For all lists, if we reverse them twice, we should get the original list.

$$
∀lst :List =>  lst.reverse.reverse == lst
$$

---

[.hide-footer]

```tut:silent
List("my","first","property").reverse.reverse
```

---

[.hide-footer]

```tut
List("my","first","property").reverse.reverse
```

---

[.hide-footer]

```tut:silent
List(1,1,2,3,5,8,13,21).reverse.reverse
```

---

[.hide-footer]

```tut
List(1,1,2,3,5,8,13,21).reverse.reverse
```


---

[.hide-footer]

## Property based tests - The mechanics
* Scala check
* Generators
* Properties

---

[.hide-footer]

```tut:silent
trait Generator[A] {
  def sample: A
}
```

---

[.hide-footer]

## Int generator

```tut:silent
object intGen extends Generator[Int] {
  override def sample: Int = scala.util.Random.nextInt
}
```

```tut
intGen.sample
intGen.sample
intGen.sample
```

---

[.hide-footer]

## Let's Generate Counters

```tut
type Counters = Map[Folder, Int]
```

---

[.hide-footer]

```tut:silent
 import org.scalacheck.Gen
 val folderToCounterGen =
  for {
    folder <- Gen.oneOf(Active, Resolved, Shared)
    counter <- Gen.posNum[Int]
  } yield folder -> counter

  implicit val countersGen: Gen[Map[Folder, Int]] =
    Gen.listOf(folderToCounterGen).map(_.toMap)
```

---

[.hide-footer]

```tut
countersGen.sample
countersGen.sample
```

```tut:invisible
 val alertGen = for {
    data <- Gen.alphaStr
    folders <- Gen.listOf(Gen.oneOf(Active, Resolved, Shared))
  } yield Alert(data.take(10), folders.distinct.toSet)
  
  import org.scalacheck.Arbitrary
  implicit val countersArb: Arbitrary[Counters] = Arbitrary(countersGen)
  implicit val alertsArb: Arbitrary[Alert] = Arbitrary(alertGen)
```

---

[.hide-footer]

#[Fit]Properties

---

[.hide-footer]

#[Fit] Property = Assertion ? 

---

[.hide-footer]

# What is a property ? 

* Properties are assertions with special requirements
* They must hold for all values!

---

[.hide-footer]


## Imports

```tut:invisible
import org.scalatest.prop.GeneratorDrivenPropertyChecks
trait FolderPropertiesSpec extends WordSpecLike with Matchers with GeneratorDrivenPropertyChecks
import CountersApiV1Instance.addToCounters

```

```scala
import org.scalatest.prop.GeneratorDrivenPropertyChecks
trait FolderPropertiesSpec extends WordSpecLike 
                           with Matchers 
                           with GeneratorDrivenPropertyChecks
import CountersApiV1Instance.addToCounters
```

---

[.hide-footer]

## The first property
[.code-highlight: 2]
```tut:silent
class FolderCountersPropertySpec extends FolderPropertiesSpec {
  "increment the alert folders" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(counters, alert) shouldEqual ???
  }
}
```

---

[.hide-footer]

[.code-highlight: 3]
```tut:silent
class FolderCountersPropertySpec extends FolderPropertiesSpec {
  "increment the alert folders" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(counters, alert) shouldEqual ???
  }
}
```
---

[.hide-footer]

[.code-highlight: 4]
```tut:silent
class FolderCountersPropertySpec extends FolderPropertiesSpec {
  "increment the alert folders" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(counters, alert) shouldEqual ???
  }
}
```

^ This is our first property! we are using the forAll syntax
^ We are using the 2 generators we defined before
^ How can we assert? we really want to combine the alert folders here with the counters, but that will be reimplementing our logic again!!!
^ It's the chicken and egg problem


---

[.hide-footer]
## How do we define properties? 

---

[.hide-footer]
![Fit](./images/ThePartialValuesProblem.jpg)

^ Look at our code from a function's point of view 
^ Find the relationship between the inputs and the outputs
^ Keep an 👀 on special inputs (for example : empty values)  
^ If stuck, try reduce the space complexity of inputs
^ Test the property
^ Refine the property
^ Repeat

---

[.hide-footer]
##[Fit]Empty alert Property

---

[.hide-footer]

![original](./images/TheEmptyAlertRule.jpg)

---

[.hide-footer]

```tut:invisible
import CountersApiV1Instance.addToCounters
```
```tut:silent
class FolderCountersPropertySpec extends FolderPropertiesSpec {
  "adding the empty alert should not change the counters" in forAll {
    counters: Map[Folder, Int] =>
      addToCounters(counters, Alert("",Set())) shouldEqual counters
   }
}
``` 

---

[.hide-footer]

![Fit](./images/foundTheBug.jpg)

---

[.hide-footer]

#[Fit]Victory ? 

---

[.hide-footer]

## Remove From Counters
* Repeat the process

---

[.hide-footer]

#[Fit] The missing properties problem
How to find properties?

```tut:silent
  trait CountersApi {
    def addToCounters(current: Counters, alert: Alert): Counters
    def removeFromCounters(current: Counters, alert: Alert): Counters
  }
```

---

![Fit](./images/running_in_tlv.jpg)

---

[.hide-footer]
# Remove and Add are related!

- Removing and then adding an alert should keep the original counters
```tut:reset:invisible
```
```tut:invisible
    
    trait Folder
    case object Active extends Folder
    case object Shared extends Folder
    case object Resolved extends Folder
        
        
    case class Alert(data: String, folders: Set[Folder])
    import org.scalatest._
    import org.scalatest.FunSuiteLike
    import org.scalatest.prop.GeneratorDrivenPropertyChecks

    trait FolderPropertiesSpec extends WordSpecLike with Matchers with GeneratorDrivenPropertyChecks

    type Counters = Map[Folder,Int]

    import org.scalacheck.Gen
     
    val folderToCounterGen =
      for {
        folder <- Gen.oneOf(Active, Resolved, Shared)
        counter <- Gen.posNum[Int]
      } yield folder -> counter

    implicit val countersGen: Gen[Map[Folder, Int]] =
        Gen.listOf(folderToCounterGen).map(_.toMap)
     
    val alertGen = for {
        data <- Gen.alphaStr
        folders <- Gen.listOf(Gen.oneOf(Active, Resolved, Shared))
      } yield Alert(data.take(10), folders.distinct.toSet)
      
     import org.scalacheck.Arbitrary
     implicit val countersArb: Arbitrary[Counters] = Arbitrary(countersGen)
     implicit val alertsArb: Arbitrary[Alert] = Arbitrary(alertGen)
    
    trait CountersApi {
      def addToCounters(current: Counters, alert: Alert): Counters
      def removeFromCounters(current: Counters, alert: Alert): Counters
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
       val removed = fromAlert(alert)
       removed.foldLeft(current)(
         (agg, elem) =>
           agg
             .get(elem._1)
             .map(x => x - elem._2)
             .fold(agg)(newInt => agg.updated(elem._1, newInt)))
     }
   }
   val api = CountersApiV2Instance
   import CountersApiV2Instance._   
```


[.code-highlight: 2]
```tut:silent
class FolderCountersPropertySpecWorking extends FolderPropertiesSpec {
  "Removing and then adding an alert should keep the original counters" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(removeFromCounters(counters, alert), alert) shouldEqual counters
  }
}
```

---

[.hide-footer]

[.code-highlight: 3]
```tut:silent
class FolderCountersPropertySpecWorking extends FolderPropertiesSpec {
  "Removing and then adding an alert should keep the original counters" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(removeFromCounters(counters, alert), alert) shouldEqual counters
  }
}
```

---

[.hide-footer]

[.code-highlight: 4]
```tut:silent
class FolderCountersPropertySpecWorking extends FolderPropertiesSpec {
  "Removing and then adding an alert should keep the original counters" in forAll {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(removeFromCounters(counters, alert), alert) shouldEqual counters
  }
}
```

---

[.hide-footer]

![Fit](./images/removePropertyWorks.jpg)

---

[.hide-footer]

![Fit](./images/removePropertyFails.jpg)

^ The property fails because the generated values are un-deterministic.

---

[.hide-footer]
# Making it fail consistently
[.code-highlight: 2]
```tut:silent
class FolderCountersPropertySpecWorking extends FolderPropertiesSpec {
  "Removing and adding an alert should keep the original counters" in forAll(minSuccessful(1000)) {
    (counters: Map[Folder, Int], alert: Alert) =>
      addToCounters(removeFromCounters(counters, alert), alert) shouldEqual counters
  }
}
```

---

[.hide-footer]
## Let's define 

```tut:silent
val sharedAlert = Alert("",Set(Shared))
val emptyCounters = Map[Folder,Int]()
```


---

[.hide-footer]

## The BUG in our property
```tut
val removedCounters = removeFromCounters(emptyCounters,sharedAlert)
```

---

[.hide-footer]

## The BUG in our property
```tut
val removedCounters = removeFromCounters(emptyCounters,sharedAlert)
val result = addToCounters(removedCounters,sharedAlert) 
```

---

[.hide-footer]

## The BUG in our property
```tut
val removedCounters = removeFromCounters(emptyCounters,sharedAlert)
val result = addToCounters(removedCounters,sharedAlert) 
result == emptyCounters
```

---

[.hide-footer]

## The BUG in our property
```tut
val removedCounters = removeFromCounters(emptyCounters,sharedAlert)
val result = addToCounters(removedCounters,sharedAlert) 
Map(Shared->1) == Map()
```


---

[.hide-footer]
# The Good property? 

```tut:silent
class FolderCountersPropertySpecWorking extends FolderPropertiesSpec {
  "Adding and then removing an alert should keep the original counters" in forAll{
    (counters: Map[Folder, Int], alert: Alert) =>
      removeFromCounters(addToCounters(counters,alert),alert) shouldEqual counters
  }
}
```

---

[.hide-footer]

## A different bug
```tut
val addedAlerts = addToCounters(emptyCounters,sharedAlert)
```

---

[.hide-footer]

## A different bug

```tut
val addedAlerts = addToCounters(emptyCounters,sharedAlert)
val result = removeFromCounters(addedAlerts,sharedAlert) 
```

---

[.hide-footer]

## A different bug
```tut
val addedAlerts = addToCounters(emptyCounters,sharedAlert)
val result = removeFromCounters(addedAlerts,sharedAlert) 
result == emptyCounters
```

---

[.hide-footer]

## A different bug
```tut
val addedAlerts = addToCounters(emptyCounters,sharedAlert)
val result = removeFromCounters(addedAlerts,sharedAlert) 
Map(Shared->0) == Map()
```

---

[.hide-footer]

# Let's fix it? 
```tut:invisible
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

```
```tut:invisible
val api = CountersApiV3Instance
import CountersApiV3Instance._
val sharedAlert = Alert("",Set(Shared))
val initialCounters = Map[Folder,Int]()
val addedAlerts = api.addToCounters(initialCounters,sharedAlert)
val result = api.removeFromCounters(addedAlerts,sharedAlert) 
result == initialCounters
```


---

[.hide-footer]
#[Fit] Victory?

^ The missing properties problem

---

[.hide-footer]
##[Fit]Group to the rescue
- Disclaimer:
- this is going to require a leap of faith
- this will not work at all times
- This is going to be fast and fun

---

[.hide-footer]
##[Fit] The Plan
^ Math structures have defining properties/axioms.
^ This properties/axioms are complete.
^ Meaning : if all axioms pass => it's a valid structure.

---

[.hide-footer]
![Fit](./images/groupFromWikipedia.jpg)

---

[.hide-footer]

```tut:silent
trait myGroup[A] {
    def add(x: A, y: A): A
    def remove(x: A, y: A): A
    def inverse(x: A): A
    def identity: A
  }
```

---

[.hide-footer]

```tut:silent
  trait CountersApi {
    def addToCounters(current: Counters, alert: Alert): Counters
    def removeFromCounters(current: Counters, alert: Alert): Counters
  }
```

---

[.hide-footer]

##[Fit] Getting rid of the alert data type

---

[.hide-footer]

##[Fit] Getting rid of the alert data type

```scala
def toCounters(alert:Alert):Counters = ???
```

---

[.hide-footer]
```tut:silent
  trait CountersApi {
    def addToCounters(current: Counters, added: Counters): Counters
    def removeFromCounters(current: Counters, removed: Counters): Counters
  }
```

---

[.hide-footer]
##[Fit]Identity Element

---

[.hide-footer]

```tut:silent
  trait CountersApi {
    def addToCounters(current: Counters, added: Counters): Counters
    def removeFromCounters(current: Counters, removed: Counters): Counters
    def identity : Counters
  }
```

---

[.hide-footer]

##[Fit] Making the API polymorphic

--

[.hide-footer]

```tut:silent
trait CountersApi[A] {
    def addToCounters(current: A, added: A): A
    def removeFromCounters(current: A, removed: A): A
    def identity : A
}
```

---
[.hide-footer]


##[Fit] The inverse function

---
[.hide-footer]


```tut:silent
trait CountersApi[A] {
    def addToCounters(current: A, added: A): A
    def removeFromCounters(current: A, removed: A): A
    def inverse(x: A) : A
    def identity : A
}
```

---

[.hide-footer]

```tut:silent
trait CountersApi[A] {
    def combine(current: A, added: A): A
    def remove(current: A, removed: A): A = 
            combine(current,inverse(removed))
    def inverse(x:A): A
    def identity : A
}

```

---

[.hide-footer]

# Implementation
```tut:invisible
import cats.kernel.Semigroup
import cats.kernel.Group
object CountersApiGroup extends Group[Counters] {
    import cats.implicits._
    
    override def combine(x: Counters, y: Counters): Counters =
      Semigroup[Counters].combine(x, y)

    override def inverse(x: Counters): Counters = x.mapValues(_ * -1)

    override def empty(): Counters = Map()  
}

implicit val countersApi = CountersApiGroup
import cats.kernel.Eq
implicit val countersEq: Eq[Counters] = Eq.fromUniversalEquals

```

---

[.hide-footer]
## The tests!

```tut:silent
import cats.laws.discipline._
import cats.kernel.laws.discipline.GroupTests
import org.scalatest.FunSuiteLike
import org.typelevel.discipline.scalatest.Discipline

class GroupsTests extends Discipline with FunSuiteLike {
  checkAll("CountersGroupTests", GroupTests[Counters].group)
}
```

----

[.hide-footer]
![Fit](./images/groupRunningTests.jpg)



---

[.hide-footer]
![original](./images/tastes-like-victory.gif)

---

[.hide-footer]
![Fit](./images/dracarys.gif)

---

# Summary

- The missing unit test problem
- Property based tests
- Generators
- Properties, the missing properties problem
- Math to the rescue!

---

# Takeaways

^ Writing PBT is hard  
^ Huge payoff
^ Super fun

- Try this at home !
- Huge payoff, and it's super fun

---

# Thank you

* https://github.com/dorsev/Killing-the-unit-test-talk
* https://medium.com/free-code-camp/an-introduction-to-law-testing-in-scala-4243d72272f9