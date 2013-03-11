package de.as.nquadsparser.io

import de.as.nquadsparser.model.Quad

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

trait QuadReader
{
  def size : Int
  def read() : Quad
  def hasNext : Boolean

  def foreach(f: Quad => Unit) {
    while ( { hasNext } ) {
      f(read)
    }
  }
}

