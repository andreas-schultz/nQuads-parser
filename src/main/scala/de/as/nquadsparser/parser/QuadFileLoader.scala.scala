/*
 * LDIF
 *
 * Copyright 2011-2012 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.as.nquadsparser.parser

import scala.collection.mutable.ArrayBuffer
import scala.Predef._
import scala.actors.Actor
import scala._
import java.io._
import de.as.nquadsparser.util.{TemporaryFileCreator, Consts}
import de.as.nquadsparser.model.Quad
import de.as.nquadsparser.io.{FileQuadWriter, FileQuadReader, QuadQueue, QuadWriter}
import java.util.logging.Logger


/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 09.06.11
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */

class QuadFileLoader(graphURI: String, discardFaultyQuads: Boolean = false) {
  val log = Logger.getLogger(getClass.getName)
  val quadParser = new QuadParser(graphURI)

  def this() {
    this(Consts.DEFAULT_GRAPH)
  }

  def parseQuadLine(line: String): Quad = {
    quadParser.parseLine(line) match {
      case quad: Quad => quad
      case _ => null
    }
  }

  def validateQuads(input: BufferedReader): Seq[Pair[Int, String]] = {
    val errorList = new ArrayBuffer[Pair[Int, String]]
    var lineNr = 1

    var line: String = input.readLine()
    while(line!=null) {
      try {
        parseQuadLine(line)
      } catch {
        case e: RuntimeException => {
          errorList += Pair(lineNr, line)
        }
      }
      line = input.readLine()
      lineNr += 1
    }

    errorList
  }

  def validateQuadsMT(input: BufferedReader): Seq[Pair[Int, String]] = {
    var finishMessage = new FinishMessage
    var loop = true
    val validateActor = new ValidationActor(finishMessage)
    validateActor.start
    val pA: Seq[Actor] = for(i <- 1 to 10) yield new ParserActor(validateActor)
    val parseActors = pA.toArray
    for(actor <- parseActors) actor.start()

    var counter = 0;
    var nextCounterStep = 0
    var lines = new ArrayBuffer[String]

    while(!finishMessage.getStatus._1) {
      while(loop) {
        val line = input.readLine()
        var quad: Quad = null

        if(line==null) {
          loop = false
          parseActors((math.random*parseActors.size).asInstanceOf[Int]) ! QuadStrings(nextCounterStep, lines)
        } else {
          counter += 1
          lines.append(line)
          if(counter % 100 == 0) {
            parseActors((math.random*parseActors.size).asInstanceOf[Int]) ! QuadStrings(nextCounterStep, lines)
            lines = new ArrayBuffer[String]
            nextCounterStep+=100
          }
        }
      }
      for(actor <- parseActors)
        actor ! Finish
    }
    return finishMessage.getStatus._2
  }

  def readQuads(input: BufferedReader, quadQueue: QuadWriter) {
    var loop = true
    var faultyRead = false

    var counter = 1;
    var nrOfErrors = 0;

    while(loop) {
      val line = input.readLine()
      var quad: Quad = null

      if(line==null)
        loop = false
      else {
        try {
          quad = parseQuadLine(line)
        } catch {
          case e: RuntimeException => {
            if(!discardFaultyQuads)
              faultyRead=true
            nrOfErrors += 1
            log.warning("Parse error found at line " + counter + ". Input line: " + line)
          }
        }
        if(quad!=null && (!faultyRead)) {
          quadQueue.write(quad) }
      }
      counter += 1
    }
    if(faultyRead)
      throw new RuntimeException("Found errors while parsing NT/NQ file. Found " + nrOfErrors + " parse errors. See log file for more details.")
  }

  def readQuadsMT(input: BufferedReader, quadWriter: QuadWriter) {
    var finishMessage = new FinishMessage
    var loop = true
    val quadWriterActor = new QuadWriterActor(quadWriter, finishMessage)
    quadWriterActor.start
    val pA: Seq[Actor] = for(i <- 1 to 10) yield new QuadParserActor(quadWriterActor, graphURI)
    val parseActors = pA.toArray
    for(actor <- parseActors) actor.start()

    var counter = 0;
    var nextCounterStep = 0
    var lines = new ArrayBuffer[String]

    while(!finishMessage.getStatus._1) {
      while(loop) {
        val line = input.readLine()
        var quad: Quad = null

        if(line==null) {
          loop = false
          parseActors((math.random*parseActors.size).asInstanceOf[Int]) ! QuadStrings(nextCounterStep, lines)
        } else {
          counter += 1
          lines.append(line)
          if(counter % 100 == 0) {
            parseActors((math.random*parseActors.size).asInstanceOf[Int]) ! QuadStrings(nextCounterStep, lines)
            lines = new ArrayBuffer[String]
            nextCounterStep+=100
          }
        }
      }
      for(actor <- parseActors)
        actor ! Finish
    }

    val errors = finishMessage.getStatus()._2
    if(errors.size > 0)
      throw new RuntimeException("Found errors while parsing NT/NQ file. Found " + errors.size + " parse errors. Please set 'validateSources' to true and rerun for error details.")
  }
}

object MTTest {
  def main(args: Array[String]) {
    println("Starting...")
    val start = System.currentTimeMillis
//    val reader = new BufferedReader(new FileReader("/home/andreas/cordis_dump.nt"))
    val reader = new BufferedReader(new FileReader("/home/andreas/aba.nt"))
    val loader = new QuadFileLoader("irrelevant")
    val quadWriter = new QuadQueue
    loader.readQuads(reader, quadWriter)
//    val results = loader.validateQuadsMT(reader)
//    if(results.size>0)
//      println(results.size + " errors found.")
//    for(result <- results)
//      println("Error: Line " + result._1 + ": " + result._2)
    println("That's it. Took " + (System.currentTimeMillis-start)/1000.0 + "s")
  }
}

object QuadFileLoader {
  def loadQuadsIntoTempFileQuadQueue(file: File, graph: String, dicardFaultyQuads: Boolean): FileQuadReader = {
    val reader = new BufferedReader(new FileReader(file))
    loadQuadsIntoTempFileQuadQueue(reader, graph, dicardFaultyQuads)
  }

  def loadQuadsIntoTempFileQuadQueue(inputStream: InputStream, graph: String = Consts.DEFAULT_GRAPH, dicardFaultyQuads: Boolean=false): FileQuadReader = {
    val reader = new BufferedReader(new InputStreamReader(inputStream))
    loadQuadsIntoTempFileQuadQueue(reader, graph, dicardFaultyQuads)
  }

  def loadQuadsIntoTempFileQuadQueue(reader: Reader, graph: String, dicardFaultyQuads: Boolean): FileQuadReader = {
    val bufReader = new BufferedReader(reader)
    loadQuadsIntoTempFileQuadQueue(bufReader, graph, dicardFaultyQuads)
  }

  def loadQuadsIntoTempFileQuadQueue(reader: BufferedReader, graph: String, dicardFaultyQuads: Boolean): FileQuadReader = {
    val output = TemporaryFileCreator.createTemporaryFile("quadqueue", ".dat")
    output.deleteOnExit()
    val writer = new FileQuadWriter(output)
    val loader = new QuadFileLoader(graph, dicardFaultyQuads)
    loader.readQuads(reader, writer)
    writer.finish()
    return new FileQuadReader(output)
  }
}

class ParserActor(validateActor: Actor) extends Actor {
  private val loader = new QuadFileLoader("")

  private def validateQuads(c: Int, lines: scala.Seq[String]): Unit = {
    var counter = c
    val errors = new ArrayBuffer[Pair[Int, String]]

    for (line <- lines) {
      try {
        counter += 1
        loader.parseQuadLine(line)
      } catch {
        case e: RuntimeException => {
          errors.append(Pair(counter, line))
        }
      }
    }
    if (errors.size > 0)
      validateActor ! Errors(errors)
  }

  def act() {
    loop {
          react {
            case QuadStrings(c, lines) => {
              validateQuads(c, lines)
            }
            case Finish => {
              validateActor ! Finish
              exit()
            }
          }
        }
  }
}

class QuadParserActor(quadWriterActor: Actor, graph: String) extends Actor {
  private val loader = new QuadFileLoader(graph)

  private def parseQuads(c: Int, lines: scala.Seq[String]): Unit = {
    var counter = c
    val errors = new ArrayBuffer[Pair[Int, String]]
    val quads = new ArrayBuffer[Quad]

    for (line <- lines) {
      try {
        counter += 1
        loader.parseQuadLine(line) match {
          case quad: Quad => quads.append(quad)
          case _ => // do nothing
        }
      } catch {
        case e: RuntimeException => {
          errors.append(Pair(counter, line))
        }
      }
    }
    if (errors.size > 0)
      quadWriterActor ! Errors(errors)
    else
      quadWriterActor ! QuadsMessage(quads)
  }

  def act() {
    loop {
          react {
            case QuadStrings(c, lines) => {
              parseQuads(c, lines)
            }
            case Finish => {
              quadWriterActor ! Finish
              exit()
            }
          }
        }
  }
}

class ValidationActor(finishMessage: FinishMessage) extends Actor {
  val s = System.currentTimeMillis
  val allErrors = new ArrayBuffer[Pair[Int, String]]
  var finishCounter = 0

  def act() {
    loop {
      react {
        case Errors(quadErrors) =>
          allErrors ++= quadErrors
        case Finish =>
          finishCounter += 1
            if(finishCounter==10) {
              finishMessage.finish(allErrors.sortWith(_._1 < _._1))
              exit()
            }
      }
    }
  }
}

class QuadWriterActor(quadWriter: QuadWriter, finishMessage: FinishMessage) extends Actor {
  val s = System.currentTimeMillis
  val allErrors = new ArrayBuffer[Pair[Int, String]]
  var finishCounter = 0
  private val log = Logger.getLogger(getClass.getName)

  def act() {
    loop {
      react {
        case QuadsMessage(quads) => for(quad <- quads) quadWriter.write(quad)
        case Errors(quadErrors) =>
          allErrors ++= quadErrors
        case Finish =>
          finishCounter += 1
            if(finishCounter==10) {
              finishMessage.finish(allErrors.sortWith(_._1 < _._1))
              exit()
            }
      }
    }
  }
}

case class QuadsMessage(val quads: Iterable[Quad])

case class QuadStrings(val counter: Int, val quads: Seq[String])

case class Errors(val quadErrors: Seq[Pair[Int, String]])

case object Finish

class FinishMessage {
  var finished = false
  var results: Seq[Pair[Int, String]] = null

  def finish(results: Seq[Pair[Int, String]]) {
    this.results = results
    finished = true
  }

  def getStatus(): Pair[Boolean, Seq[Pair[Int, String]]] = {
    if(finished)
      return Pair(finished, results)
    else
      return Pair(false, null)
  }
}
