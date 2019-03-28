package com.paul

import java.time.{DayOfWeek, LocalDate}
import java.util.{Optional, OptionalInt, function}

import org.slf4j.Logger

import scala.language.implicitConversions

package object spark {

  //todo: remove these function converts when we upgrade to scala 2.12
  //converts a scala function to a java function automatically
  implicit def scalaFnToJava8Fn[A, B](fn: A => B): function.Function[A, B] = new function.Function[A, B] {
    override def apply(t: A): B = fn(t)
  }

  implicit def scalaFnToJava8Consumer[A, B](fn: A => B): function.Consumer[A] = new function.Consumer[A] {
    override def accept(t: A): Unit = fn(t)
  }

  implicit def scalaFnToJava8Supplier[A](fn: () => A): function.Supplier[A] = new function.Supplier[A] {
    override def get(): A = fn()
  }


  implicit def scalaFnToJavaRunnable[A](fn: () => A): Runnable = new Runnable {
    override def run(): Unit = fn()
  }

  //converts a scala function to a java predicate automatically
  implicit def scalaFnToJava8Predicate[A](fn: A => Boolean): function.Predicate[A] = new function.Predicate[A] {
    override def test(t: A) = fn(t)
  }

  implicit def javaOptionalIntToScala(optionalInt: OptionalInt): Option[Int] = if (optionalInt.isPresent) Some(optionalInt.getAsInt) else None

  implicit def javaOptionalToScala[A](optional: Optional[A]): Option[A] = if (optional.isPresent) Some(optional.get()) else None

  /**
    * use like
    * val myVal = timed("a message", log) {  doSomeExpensiveThing() }
    */
  def timed[A](tag: String, logger: Logger)(fn: => A): A = {
    val startTime = System.currentTimeMillis()
    val r = fn
    val runTime: Double = (System.currentTimeMillis() - startTime) / 1000.0
    logger.info(s"$tag finished in $runTime s")
    r
  }

  import scala.reflect.runtime.universe._

  def fieldNamesOfCaseClass[T: TypeTag]: Seq[String] = {
    val tpe = typeOf[T]
    val constructorSymbol = tpe.decl(termNames.CONSTRUCTOR)
    val defaultConstructor =
      if (constructorSymbol.isMethod) constructorSymbol.asMethod
      else {
        val ctors = constructorSymbol.asTerm.alternatives
        ctors.map(_.asMethod).find(_.isPrimaryConstructor).get
      }

    defaultConstructor.paramLists.reduceLeft(_ ++ _).map {
      sym => sym.name.toString
    }
  }

  def previousWeekday(localDate: LocalDate): LocalDate = previousOrSameWeekday(localDate.minusDays(1))

  def previousOrSameWeekday(localDate: LocalDate): LocalDate = {
    if (localDate.getDayOfWeek == DayOfWeek.SATURDAY) {
      localDate.minusDays(1)
    } else if (localDate.getDayOfWeek == DayOfWeek.SUNDAY) {
      localDate.minusDays(2)
    } else {
      localDate
    }
  }

}
