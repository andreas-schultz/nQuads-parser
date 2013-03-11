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

package de.as.nquadsparser.model

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 26.07.11
 * Time: 16:20
 * To change this template use File | Settings | File Templates.
 */

import de.as.nquadsparser.util.{MD5Helper, NTriplesStringConverter}
import Node.{Literal, TypedLiteral, LanguageLiteral, BlankNode, UriNode}

trait NodeTrait {
  def value : String
  def datatypeOrLanguage : String
  def nodeType : Node.NodeType
  def graph : String

  def toXML =  nodeType match {
    //TODO Literal language and datatype not supported in M1
      case Literal => <Literal>{value}</Literal>
      case TypedLiteral => <Literal>{value}</Literal>
      case LanguageLiteral =>  <Literal>{value}</Literal>
      case BlankNode => <BlankNode>{value}</BlankNode>
      case UriNode => <Uri>{value}</Uri>
  }

  def datatype = nodeType match
  {
    case TypedLiteral => datatypeOrLanguage
    case _ => null
  }

  def language = nodeType match
  {
    case LanguageLiteral => datatypeOrLanguage
    case _ => null
  }

  def compare(otherNode: NodeTrait) = {
    // case: Both are Blank Nodes
    if(nodeType==BlankNode && otherNode.nodeType==BlankNode) {
      if(value!=otherNode.value)
        value.compare(otherNode.value)
      else
        graph.compare(otherNode.graph)
    } else if(nodeType==BlankNode || otherNode.nodeType==BlankNode) { // case: only one is a Blank Node
      if(nodeType==BlankNode)
        -1
      else
        1
    } else { // case: no Blank Nodes involved
      if(nodeType!=otherNode.nodeType)
        nodeType.id.compare(otherNode.nodeType.id)
      else if(value!=otherNode.value)
        value.compare(otherNode.value)
      else if(datatypeOrLanguage!=null && otherNode.datatypeOrLanguage!=null)
        datatypeOrLanguage.compare(otherNode.datatypeOrLanguage)
      else if(datatypeOrLanguage!=null && otherNode.datatypeOrLanguage==null)
        1
      else if(datatypeOrLanguage==null && otherNode.datatypeOrLanguage!=null)
        -1
      else
        0
    }
  }

  override def hashCode: Int = value.hashCode

  override def equals(other: Any): Boolean = {
    if (this.asInstanceOf[AnyRef] eq other.asInstanceOf[AnyRef])
      true
    if (!(other.isInstanceOf[NodeTrait]))
      false
    else {
      var otherNode: NodeTrait = other.asInstanceOf[NodeTrait]
      var result = (otherNode.nodeType == nodeType) && compareDTorLang(this.datatypeOrLanguage, otherNode.datatypeOrLanguage) && (this.value.equals(otherNode.value))
      if(nodeType== BlankNode)
        result = result && (graph == otherNode.graph)
      result
    }
  }

  private def compareDTorLang(v1: String, v2: String): Boolean = {
    if (v1 == null)
      v2 == null
    else v1.equals(v2)
  }

  override def toString = toNQuadsFormat

  def toNQuadsFormat = nodeType match {
    case Literal => "\"" + NTriplesStringConverter.convertToEscapedString(value) + "\""
    case TypedLiteral => "\"" + NTriplesStringConverter.convertToEscapedString(value) + "\"^^<" + NTriplesStringConverter.convertToEscapedString(datatypeOrLanguage) + ">"
    case LanguageLiteral => "\"" + NTriplesStringConverter.convertToEscapedString(value) + "\"@" + datatypeOrLanguage
    case BlankNode => "_:"+ value
    case UriNode => "<" + NTriplesStringConverter.convertToEscapedString(value) + ">"
  }

  def toNTriplesFormat = nodeType match {
    case Literal => "\"" + NTriplesStringConverter.convertToEscapedString(value) + "\""
    case TypedLiteral => "\"" + NTriplesStringConverter.convertToEscapedString(value) + "\"^^<" + NTriplesStringConverter.convertToEscapedString(datatypeOrLanguage) + ">"
    case LanguageLiteral => "\"" + NTriplesStringConverter.convertToEscapedString(value) + "\"@" + datatypeOrLanguage
    case BlankNode => "_:g" + MD5Helper.md5(graph) + value
    case UriNode => "<" + NTriplesStringConverter.convertToEscapedString(value) + ">"
  }

  def isResource = nodeType==UriNode || nodeType==BlankNode

  def isUriNode = nodeType==UriNode

  def isBlankNode = nodeType==BlankNode

  def isLanguageLiteral = nodeType==LanguageLiteral

  def isDataTypeLiteral = nodeType==TypedLiteral

  def isLiteral = nodeType==Literal||nodeType==TypedLiteral||nodeType==LanguageLiteral
}