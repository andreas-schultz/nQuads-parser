package de.as.nquadsparser.model

import java.util.Comparator
import de.as.nquadsparser.util.{Consts, NTriplesStringConverter}

/**
 * An RDF quad.
 */
case class Quad(subject : NodeTrait, predicate : String, obj : NodeTrait, graph : String) {
  def toNQuadFormat = {
    val sb = new StringBuilder
    sb ++= subject.toNQuadsFormat
    sb ++= " <"
    sb ++= NTriplesStringConverter.convertToEscapedString(predicate)
    sb ++= "> "
    sb ++= obj.toNQuadsFormat
    sb ++= " <"
    sb ++= NTriplesStringConverter.convertToEscapedString(graph)
    sb ++= ">"
    sb.toString
  }

  def toNTripleFormat = {
    val sb = new StringBuilder
    sb ++= subject.toNTriplesFormat
    sb ++= " <"
    sb ++= NTriplesStringConverter.convertToEscapedString(predicate)
    sb ++= "> "
    sb ++= obj.toNTriplesFormat
    sb.toString
  }

  override def equals(other: Any): Boolean = {
    if (this.asInstanceOf[AnyRef] eq other.asInstanceOf[AnyRef])
      true
    if (!(other.isInstanceOf[Quad]))
      false
    else {
      val otherQuad = other.asInstanceOf[Quad]
      subject==otherQuad.subject && predicate==otherQuad.predicate && obj==otherQuad.obj && graph==otherQuad.graph
    }
  }

  def toLine = toNQuadFormat + " .\n"
}

class ForwardComparator extends Comparator[Quad] {
  def compare(left: Quad, right: Quad) = {
    if(left.subject!=right.subject)
      left.subject.compare(right.subject)
    else if(left.predicate!=right.predicate)
      left.predicate.compareTo(right.predicate)
    else if(left.obj!=right.obj)
      left.obj.compare(right.obj)
    else
      left.graph.compareTo(right.graph)
  }
}

class BackwardComparator extends Comparator[Quad] {
  def compare(left: Quad, right: Quad) = {
    if(left.obj!=right.obj)
      left.obj.compare(right.obj)
    else if(left.predicate!=right.predicate)
      left.predicate.compareTo(right.predicate)
    else if(left.subject!=right.subject)
      left.subject.compare(right.subject)
    else
      left.graph.compareTo(right.graph)
  }
}

/**
 * Convenience class, should only be used internally!
 */
case class Triple(override val subject : NodeTrait, override val predicate : String, override val obj: NodeTrait) extends Quad(subject, predicate, obj, Consts.DEFAULT_GRAPH)