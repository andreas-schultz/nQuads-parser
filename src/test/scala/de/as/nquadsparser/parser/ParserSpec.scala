package de.as.nquadsparser.parser

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec

/**
 * Created with IntelliJ IDEA.
 * User: andreas
 * Date: 3/6/13
 * Time: 11:13 AM
 * To change this template use File | Settings | File Templates.
 */
class ParserSpec extends FlatSpec with ShouldMatchers {
  val parser = new QuadParser()

  it should "parse a simple N-Quads line" in {
    val quad = parser.parseLine("<http://blah> <http://p> <http://o> <http://g> .")
    quad.obj should be a ('UriNode)
    quad.graph should equal ("http://g")
  }

  it should "parse language literals" in {
    val quad = parser.parseLine("<http://blah> <http://p> \"this is english\"@en <http://g> .")
    quad.obj should be a ('LanguageLiteral)
    quad.obj.language should equal ("en")
  }

  it should "parse blank nodes" in {
    val quad = parser.parseLine("_:bn <http://p> <http://o> <http://g> .")
    quad.subject should be a ('BlankNode)
  }

  it should "parse datatype literals" in {
    val quad = parser.parseLine("<http://blah> <http://p> \"214214\"^^<http://int> <http://g> .")
    quad.obj should be a ('DataTypeLiteral)
    quad.obj.datatype should equal ("http://int")
  }

  it should "throw parser exception for wrong language tags" in {
    val thrown = evaluating {
      parser.parseLine("<http://blah> <http://p> \"this is english\"@en_en <http://g> .")
    }   should produce [ParseException]
    thrown.getMessage should equal ("line 1:46 mismatched character 'e' expecting ':'")
  }
}
