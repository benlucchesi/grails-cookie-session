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

import groovy.util.logging.Log4j

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.serializers.FieldSerializer
import de.javakaffee.kryoserializers.*

import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl

import java.util.Collections;

@Log4j
class KryoSessionSerializer implements SessionSerializer{

  GrailsApplication grailsApplication

  public byte[] serialize(SerializableSession session){
    log.trace "serializeSession()"

    Kryo kryo = getConfiguredKryoSerializer()
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    Output output = new Output(outputStream)
    kryo.writeObject(output,session)
    output.close()
    def bytes = outputStream.toByteArray()

    log.trace "serialized session. ${bytes.size()}"
    return bytes
  }

  public SerializableSession deserialize(byte[] serializedSession){
    log.trace "deserializeSession()"

    def input = new Input(new ByteArrayInputStream( serializedSession ) )
    Kryo kryo = getConfiguredKryoSerializer()
    SerializableSession session = kryo.readObject(input,SerializableSession.class)
    log.trace "deserialized session."

    return session
  }

  private def getConfiguredKryoSerializer(){

    def kryo = new Kryo()

    // register fieldserializer for GrailsFlashScope
    def flashScopeSerializer = new FieldSerializer(kryo, GrailsFlashScope.class);
    kryo.register(GrailsFlashScope.class,flashScopeSerializer)

    def localeSerializer = new LocaleSerializer()
    kryo.register(java.util.Locale.class,localeSerializer)

    def s1 = new GrantedAuthoritiesListSerializer()
    kryo.register(Collections.unmodifiableList(new ArrayList<GrantedAuthority>()).class,s1)

    def s2 = new GrantedAuthoritiesSetSerializer()
    kryo.register(Collections.unmodifiableSet(new TreeSet<GrantedAuthority>()).class,s2)

    kryo.classLoader = grailsApplication.classLoader
    kryo.instantiatorStrategy = new StdInstantiatorStrategy()

    kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer( ) );
    kryo.register( Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );
    kryo.register( Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() );
    kryo.register( Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() );
    kryo.register( Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer( ) );
    kryo.register( Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer( ) );
    kryo.register( Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer( ) );
    kryo.register( GregorianCalendar.class, new GregorianCalendarSerializer() );
    kryo.register( java.lang.reflect.InvocationHandler.class, new JdkProxySerializer( ) );
    //UnmodifiableCollectionsSerializer.registerSerializers( kryo );
    SynchronizedCollectionsSerializer.registerSerializers( kryo );

    return kryo
  }
}

public class LocaleSerializer extends Serializer<java.util.Locale> {

  public LocaleSerializer (){
  }

  @Override
  public void write (Kryo kryo, Output output, java.util.Locale locale) {
    output.writeString(locale.language?:"")
    output.writeString(locale.country?:"")
    output.writeString(locale.variant?:"")
  }

  @Override
  public Object create (Kryo kryo, Input input, Class<java.util.Locale> type) {
    return new java.util.Locale(input.readString(),input.readString(),input.readString()) 
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Locale> type) {
    return new java.util.Locale(input.readString(),input.readString(),input.readString()) 
  }
}

public class GrantedAuthoritiesListSerializer extends Serializer<Object> {

  public GrantedAuthoritiesListSerializer(){

  }

  @Override
  public void write (Kryo kryo, Output output, Object collection) {
    String list = collection.each{ it.authority }.join(",")
    kryo.writeClassAndObject( output, list )
  }

  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    def list = kryo.readClassAndObject( input ).split(',').collect{ new GrantedAuthorityImpl(it) } 
    return Collections.unmodifiableList(list)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    def list = kryo.readClassAndObject( input ).split(',').collect{ new GrantedAuthorityImpl(it) } 
    return Collections.unmodifiableList(list)
  }

}

public class GrantedAuthoritiesSetSerializer extends Serializer<Object> {

  public GrantedAuthoritiesSetSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object collection) {
    String list = collection.each{ it.authority }.join(",")
    kryo.writeClassAndObject( output, list )
  }

  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    def list = kryo.readClassAndObject( input ).split(',').collect{ new GrantedAuthorityImpl(it) } 
    return Collections.unmodifiableSet(list)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    def list = kryo.readClassAndObject( input ).split(',').collect{ new GrantedAuthorityImpl(it) }.toSet() 
    return Collections.unmodifiableSet(list)
  }

}
