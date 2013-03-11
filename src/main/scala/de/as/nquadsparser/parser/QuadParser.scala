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

import org.antlr.runtime.{CommonTokenStream, ANTLRStringStream}
import de.as.nquadsparser.util.Consts
import de.as.nquadsparser.model.Quad

/**
 * N-Quad/N-Triple Parser
 */

class QuadParser(graphURI: String) {
  def this() {
    this(Consts.DEFAULT_GRAPH)
  }

  /**
   * Returns a Quad object if line can be parsed as Quad, or null otherwise (eg. comment lines)
   * @throws: ParseException
   */
  def parseLine(line: String, silent : Boolean = false): Quad = {
    val stream = new ANTLRStringStream(line)
    val lexer = new NQuadLexer(stream)
    val tokenStream = new CommonTokenStream(lexer)
    val parser = new NQuadParser(tokenStream)
    parser.setGraph(graphURI)
    try {
      parser.line()
    }
    catch {
      case e:ParseException =>
      {
        if (silent)
          null
        else throw e
      }
    }
  }
}
