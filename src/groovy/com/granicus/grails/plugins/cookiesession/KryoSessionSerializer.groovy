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

import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.beans.factory.InitializingBean

import java.util.Collections;

import org.codehaus.groovy.grails.commons.ConfigurationHolder as ch


@Log4j
class KryoSessionSerializer implements SessionSerializer, InitializingBean{

  GrailsApplication grailsApplication

  boolean springSecurityCompatibility = false

  void afterPropertiesSet(){
      log.trace "afterPropertiesSet()"
      if( ch.config.grails.plugin.cookiesession.containsKey('springsecuritycompatibility') ){
        springSecurityCompatibility = ch.config.grails.plugin.cookiesession['springsecuritycompatibility']?true:false
      }
  }

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

    if( springSecurityCompatibility ){
      def grantedAuthorityImplSerializer = new GrantedAuthorityImplSerializer()
      kryo.register(GrantedAuthorityImpl.class,grantedAuthorityImplSerializer)

      def grailsUserSerializer = new GrailsUserSerializer()
      kryo.register(GrailsUser.class,grailsUserSerializer)

      def usernamePasswordAuthenticationTokenSerializer = new UsernamePasswordAuthenticationTokenSerializer()
      kryo.register(UsernamePasswordAuthenticationToken.class,usernamePasswordAuthenticationTokenSerializer)
    }
    
    UnmodifiableCollectionsSerializer.registerSerializers( kryo );
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

public class GrantedAuthorityImplSerializer extends Serializer<Object> {

  public GrantedAuthorityImplSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object grantedAuth ) {
    kryo.writeClassAndObject( output, grantedAuth.authority )
  }

  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    def role = kryo.readClassAndObject( input )
    return new GrantedAuthorityImpl(role)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    def role = kryo.readClassAndObject( input )
    return new GrantedAuthorityImpl(role)
  }

}

@Log4j
public class GrailsUserSerializer extends Serializer<Object> {
  public GrailsUserSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object user) {
    //NOTE: note writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
    kryo.writeClassAndObject(output,user.id)
    kryo.writeClassAndObject(output,user.username)
    kryo.writeClassAndObject(output,user.accountNonExpired)
    kryo.writeClassAndObject(output,user.accountNonLocked)
    kryo.writeClassAndObject(output,user.credentialsNonExpired)
    kryo.writeClassAndObject(output,user.enabled)
  }

  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    def id = kryo.readClassAndObject( input )
    def username = kryo.readClassAndObject( input )
    def accountNonExpired = kryo.readClassAndObject( input )
    def accountNonLocked = kryo.readClassAndObject( input )
    def credentialsNonExpired = kryo.readClassAndObject( input )
    def enabled = kryo.readClassAndObject( input )

    return new GrailsUser(username,"",enabled,accountNonExpired,credentialsNonExpired,accountNonLocked,(Collection<GrantedAuthority>)[],id)
  }
}

@Log4j
public class UsernamePasswordAuthenticationTokenSerializer extends Serializer<Object> {
  public UsernamePasswordAuthenticationTokenSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object token) {
    kryo.writeClassAndObject(output,token.principal)
    kryo.writeClassAndObject(output,token.credentials)
    kryo.writeClassAndObject(output,token.authorities)
    kryo.writeClassAndObject(output,token.details)
  }

  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    def principal = kryo.readClassAndObject( input )
    def credentials = kryo.readClassAndObject( input )
    def authorities = kryo.readClassAndObject( input )
    def details = kryo.readClassAndObject( input )

    def token = new UsernamePasswordAuthenticationToken(principal,credentials,authorities)
    token.details = details
    return token
  }
}
