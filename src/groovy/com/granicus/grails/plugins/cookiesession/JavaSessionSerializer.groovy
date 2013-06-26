/*
 * Copyright 2012 the original author or authors.
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
 *
 *  Ben Lucchesi
 *  ben@granicus.com or benlucchesi@gmail.com
 */

package com.granicus.grails.plugins.cookiesession;

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.apache.log4j.Logger;
import java.io.ByteArrayOutputStream;

class JavaSessionSerializer implements SessionSerializer{

  final static Logger log = Logger.getLogger(JavaSessionSerializer.class.getName());

  GrailsApplication grailsApplication

  public byte[] serialize(SerializableSession session){
    log.trace "serializeSession()"
    ByteArrayOutputStream stream = new ByteArrayOutputStream()
    def output = new ObjectOutputStream(stream)
    output.writeObject(session)
    output.close()
    byte[] bytes = stream.toByteArray() 
    log.trace "serialized session. ${bytes.size()} bytes."
    return bytes
  }

  public SerializableSession deserialize(byte[] serializedSession){

    log.trace "deserializeSession()"

    def inputStream = new ObjectInputStream(new ByteArrayInputStream( serializedSession )){
      @Override
      public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        //noinspection GroovyUnusedCatchParameter
        try {
          return grailsApplication.classLoader.loadClass(desc.getName())
        } catch (ClassNotFoundException ex) {
          return Class.forName(desc.getName())
        }
      }
    }

    SerializableSession session = (SerializableSession)inputStream.readObject();
    
    log.trace "deserialized session."
    return session
  }
}
