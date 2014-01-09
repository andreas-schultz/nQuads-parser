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

package de.as.nquadsparser.util

import java.io.{FileWriter, BufferedWriter, CharConversionException}

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 09.06.11
 * Time: 16:16
 * To change this template use File | Settings | File Templates.
 */

class NTriplesStringConverter

object NTriplesStringConverter {

  def convertFromEscapedString(value: String): String = {
    if(value.indexOf('\\') == -1)
      return value

    val sb = new java.lang.StringBuilder

    // iterate over code points (http://blogs.sun.com/darcy/entry/iterating_over_codepoints)
    val inputLength = value.length
    var offset = 0

    while(offset < inputLength)
    {
      val c = value.charAt(offset)
      if(c!='\\')
        sb append c
      else {
        offset += 1
        val specialChar = value.charAt(offset)

        specialChar match {
          case '"' => sb append '"'
          case 't' => sb append '\t'
          case 'r' => sb append '\r'
          case '\\' => sb append '\\'
          case 'n' => sb append '\n'
          case 'u' => {
            offset += 1
            val codepoint = value.substring(offset, offset + 4)
            val character = Integer.parseInt(codepoint, 16).asInstanceOf[Char]
            sb append character
            offset += 3
          }
          case 'U' => {
            offset += 1
            val codepoint = value.substring(offset, offset + 8)
            val character = Integer.parseInt(codepoint, 16)
            sb appendCodePoint character
            offset += 7
          }
        }
      }
      offset += 1
    }
    sb.toString
  }

  def convertToEscapedString(input: String): String = {
    val sb = new StringBuilder

    // iterate over code points (http://blogs.sun.com/darcy/entry/iterating_over_codepoints)
    val inputLength = input.length
    var offset = 0

    while (offset < inputLength)
    {
      val c = input.codePointAt(offset)
      offset += Character.charCount(c)
      //Ported from Jena's NTripleWriter
     	if (c == '\\' || c == '"')
    	{
    		sb append '\\' append c.toChar
    	}	else if (c == '\n')	{
    		sb append "\\n"
    	}	else if (c == '\r')	{
    		sb append "\\r";
    	}	else if (c == '\t')	{
    		sb append "\\t"
    	}	else if (c >= 32 && c < 127) {
    		sb append c.toChar
    	}	else {
    		val hexStr = c.toHexString.toUpperCase
        val hexStrLen = hexStr.length

        if (c <= 0xffff)
        {
        // 16-bit code point
          sb append "\\u"
          sb append "0" * (4 - hexStrLen)  // leading zeros
        } else if (c <= 0x10ffff) { // biggest representable code point
          // 32-bit code point
          sb append "\\U"
          sb append "0" * (8 - hexStrLen)  // leading zeros
        } else {
          throw new CharConversionException("code point "+c+" outside of range (0x0000..0x10ffff)")
        }
       	sb append hexStr
    	}
    }

    sb.toString
  }

  def convertBnodeLabel(label : String) : String = {
    // see http://www.w3.org/TR/rdf-sparql-query/#rBLANK_NODE_LABEL
    val sb = new StringBuilder

    val inputLength = label.length
    var offset = 0

    while (offset < inputLength)
    {
      val c = label.codePointAt(offset)
      // if a-zA-Z
      if ((c >= 65 && c <= 90) || (c >= 97 && c <= 122))
        sb append c.toChar

      else {
        if ((offset == 0))
          sb append "B"
        // if 0-9
        if ((c >= 48 && c <= 57))
          sb append c.toChar
        else {
          val hexString = c.toHexString.toUpperCase
          sb append hexString
        }
      }

      offset += Character.charCount(c)
    }
    sb.toString

  }

  def main(args: Array[String]) {
//    val str = NTriplesStringConverter.convertFromEscapedString("update news.article_version set content='1\\U000104001' where article_version_id=7374;")
//    val writer = new BufferedWriter(new FileWriter("utf8mb_test.sql"))
//    writer.append(str)
//    writer.newLine()
//    writer.flush()
//    writer.close()
    println(NTriplesStringConverter.convertToEscapedString(
      """|
        |
        |حسن حبيب / القطيف،جزيرة العرب
        |
        |جنين اليازوري /فلسطين
        |
        |فيصل اوكي دوكي/موسيقي/لبنان
        |
        |ادون/ كاتب وناشط مدني/ بيروت
        |
        |سمر كعدي/فرنسا
        |
        |ريما عيسى - مخرجة سينمائية / فلسطين
        |
        |ولاء الغصي/ فلسطين
        |
        |سمارة سلام/مصورة/ سوريا فلسطين
        |
        |عمر الشيخ - مدون وناشط سياسي  / موريتانيا
        |
        |محمد حفظ الله الهمداني/مدرس /اليمن
        |
        |حسين خليلي/فلسطين
        |
        |علي حسن/مصور فوتوغرافي/البحرين
        |
        |هديل محمد/مدونة/السعودية،فلسطين
        |
        |ألاء البابا/فلسطين
        |
        |ريما الوابل/السعودية
        | """.stripMargin).split("\\\\u").mkString("\n"))
  }
}
