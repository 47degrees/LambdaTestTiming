# LambdaTestTiming

[![Maven Central](https://img.shields.io/maven-central/v/com.fortysevendeg/lambda-test-timing_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.fortysevendeg/lambda-test-timing_2.12)

LambdaTestTiming is an extension to 
[LambdaTest](https://github.com/47deg/LambdaTest). 
LambdaTest is a functional testing library for Scala.
LambdaTestTiming extends LambdaTest with support for timing execution times.

One of the goals of LambdaTest was to provide a small clean system that can be easily 
extended to suport new features and to customize it for specific projects. 
The core API is `LambdaAct` which is a pure functional transform from one `LambdaState` to the
next `LambdaState`. A composition of simple and compound `LambdaAct`'s is central to
each user written test.
LambdaTestTiming is a good example of this extensibility where without any changes 
to the base system, it adds timing support by defining the new timing `LambdaAct`s.

You should review the features of the base LambdaTest system before 
reading the documentation below.

## Quick Start

Include LambdaTestTiming jars

    "com.fortysevendeg" % "lambda-test-timing_2.12" % "1.3.0" % "test"
    
In your tests include

    import com.fortysevendeg.lambdatest._
    import com.fortysevendeg.lambdatestatiming._

## Timing

There are several actions that can be used to time code.
Some are assertions that fail if the code runs too long.

See the [Timing](https://github.com/47deg/LambdaTestTiming/blob/master/src/test/scala/demo/Timing.scala) demo.
