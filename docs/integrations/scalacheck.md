---
id: scalacheck
title: ScalaCheck
---

MUnit supports writing property-based tests using
[ScalaCheck](http://www.scalacheck.org/).

## Getting started

ScalaCheck support is provided as a separate module. You can add it to your
build via:

```scala
libraryDependencies += "org.scalameta" %% "munit-scalacheck" % "@VERSION@" % Test
```

You can then extend `ScalaCheckSuite` and write ScalaCheck property tests:

```scala mdoc
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class IntegerSuite extends ScalaCheckSuite {

  property("addition is commutative") {
    forAll { (n1: Int, n2: Int) =>
      n1 + n2 == n2 + n1
    }
  }

  property("0 is the identity of addition") {
    forAll { (n: Int) =>
      n + 0 == n
    }
  }

}
```

> 👉 `property` is almost identical to `FunSuite`'s `test`, but it additionally
> ensures that the test value is a ScalaCheck `Prop`.
>
> It supports all of `test`'s features, such as tagging and marking the property
> as expected to fail:
>
> ```scala
> property("my property".tag(WindowsOnly)) {
>   forAll { (n: Int) => n * 0 == 0 }
> }
> ```
>
> ```scala
> property("issue-123".fail) {
>   forAll { (n: Int) => buggyFunction(n) }
> }
> ```

## Using assertions

ScalaCheck supports writing property checks as boolean expressions (as in the
examples above).

In MUnit you can also use [assertions](../assertions.md) to verify properties:

```scala mdoc:reset
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class IntegerSuite extends ScalaCheckSuite {

  property("integer identities") {
    forAll { (n: Int) =>
      assertEquals(n + 0, n)
      assertEquals(n * 1, n)
    }
  }

}
```

The property above is equivalent to

```scala
property("integer identities") {
  forAll { (n: Int) =>
    (n + 0 == n) && (n * 1 == n)
  }
}
```

but it provides better error reporting by including the source location of the
failed assertion, which is not possible when using boolean expressions.

## Configuring ScalaCheck

You can override the `scalaCheckTestParameters` property of `ScalaCheckSuite` to
customize how ScalaCheck checks the properties:

```scala mdoc:reset
import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class IntegerSuite extends ScalaCheckSuite {

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(200)
      .withMaxDiscardRatio(10)

  property("addition is commutative") {
    forAll { (n1: Int, n2: Int) =>
      n1 + n2 == n2 + n1
    }
  }

}
```

## Migrating from ScalaTest

ScalaTest provides two styles for writing property-based tests, which are both
front-ends to ScalaCheck.

If you are using the "ScalaCheck style" API, i.e. the `Checkers` trait,
switching to MUnit requires minor mechanical changes:

```diff
-import org.scalatest.prop.Checkers
-import org.scalatest.FunSuite
+import munit.ScalaCheckSuite
 import org.scalacheck.Prop._

-class IntegerSuite extends FunSuite with Checkers {
+class IntegerSuite extends ScalaCheckSuite {

-  test("addition is commutative") {
+  property("addition is commutative") {
-    check { (n1: Int, n2: Int) =>
+    forAll { (n1: Int, n2: Int) =>
       n1 + n2 == n2 + n1
     }
   }

}
```

If you're using the "ScalaTest style" API, i.e. the
`GeneratorDrivenPropertyChecks` trait, switching to MUnit requires again a few
changes, typically including the property body.

For example, if you are using ScalaTest matchers you will need to convert them
to MUnit assertions:

```diff
-import org.scalatest.Matchers
-import org.scalatest.prop.GeneratorDrivenPropertyChecks
-import org.scalatest.FunSuite
+import munit.ScalaCheckSuite
+import org.scalacheck.Prop._

-class IntegerSuite extends FunSuite with GeneratorDrivenPropertyChecks with Matchers {
+class IntegerSuite extends ScalaCheckSuite {

-  test("addition and multiplication are commutative") {
+  property("addition and multiplication are commutative") {
     forAll { (n1: Int, n2: Int) =>
     forAll { (n1: Int, n2: Int) =>
-      (n1 + n2) should be (n2 + n1)
-      (n1 * n2) should be (n2 * n1)
+      assertEquals(n1 + n2, n2 + n1)
+      assertEquals(n1 * n2, n2 * n1)
     }
   }

}
```
