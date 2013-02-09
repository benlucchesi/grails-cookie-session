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

import org.springframework.beans.factory.InitializingBean

import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream

import javax.crypto.spec.SecretKeySpec
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SealedObject
import javax.crypto.Cipher

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.serializers.FieldSerializer
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer

import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope
import org.codehaus.groovy.grails.commons.ConfigurationHolder as ch

import java.util.UUID

import groovy.util.logging.Log4j

@Log4j
class CookieSessionRepository implements SessionRepository, InitializingBean  {
  
  def grailsApplication

  def cryptoKey

  String cookieName = "grails_session" // default cookie name
  boolean encryptCookie = true
  String cryptoAlgorithm = "Blowfish"
  def cryptoSecret = null
  long maxInactiveInterval = 120 * 1000
  int cookieCount = 5
  int maxCookieSize = 2048
  String serializer = "java"

  void afterPropertiesSet(){

    log.trace "afterPropertiesSet()"

    log.info "configuring CookieSessionRepository"
   
    if( ch.config.grails.plugin.cookiesession.containsKey('encryptcookie') ){
      encryptCookie = ch.config.grails.plugin.cookiesession.encryptcookie?true:false
      log.info "grails.plugin.cookiesession.encryptcookie set: \'${encryptCookie}\'"
    }
    else{
      encryptCookie = true
      log.info "grails.plugin.cookiesession.encryptcookie not set. defaulting to \'${encryptCookie}\'"
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('cryptoalgorithm') ){
      cryptoAlgorithm = ch.config.grails.plugin.cookiesession.cryptoalgorithm.toString()
      log.info "grails.plugin.cookiesession.cryptoalgorithm set: \'${cryptoAlgorithm}\'"
    }
    else{
      cryptoAlgorithm = "Blowfish"
      log.info "grails.plugin.cookiesession.cryptoalgorithm not set. defaulting to \'${cryptoAlgorithm}\'"
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('sessiontimeout') ){
      maxInactiveInterval = ch.config.grails.plugin.cookiesession.sessiontimeout * 1000
      if( maxInactiveInterval == -1 ){
        log.info "config.grails.plugin.cookiesession.sessiontimeout set to -1. sessions will remain active while the user's browser remain open"
      }
      else if( maxInactiveInterval > 0 ){
        log.info "grails.plugin.cookiesession.sessiontimeout set: ${maxInactiveInterval} ms."
      }
      else if( maxInactiveInterval <= 0 ){
        log.warn "config.grails.plugin.cookiesession.sessiontimeout needs to be greater than or equal to zero. defaulting to -1"
        maxInactiveInterval = -1
      }
    }
    else{
      maxInactiveInterval = -1
      log.info "grails.plugin.cookiesession.sessiontimeout not set. defaulting to ${maxInactiveInterval}"
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('cookiename') ){
      cookieName = ch.config.grails.plugin.cookiesession.cookiename
      log.info "grails.plugin.cookiesession.cookiename set: \'${cookieName}\'"
    }else{
      cookieName = "gsession" 
      log.info "grails.plugin.cookiesession.cookiename not set. defaulting to \'${cookieName}\'"
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('secret') ){
      if( ch.config.grails.plugin.cookiesession.secret instanceof byte[] )
        cryptoSecret = ch.config.grails.plugin.cookiesession.secret
      else
        cryptoSecret = ch.config.grails.plugin.cookiesession.secret.bytes

      log.info "grails.plugin.cookiesession.secret set: \'${cryptoSecret.collect{ 'x' }.join()}\'" 
      if( !encryptCookie )
        log.warn "grails.plugin.cookiesession.secret is set, but encryptcookie == false. Set encryptcookie to true if you want to encrypt sessions!"
    }
    else if( encryptCookie ) {
      log.warn "Crypto secret is not configured for session repository. A random key will be generated.  Sessions can only be decrypted for this instance of the application. To make session transportable between multiple instances of this application, set the grails.plugin.cookiesession.secret configuration explicitly."
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('cookiecount') ){
      cookieCount =  ch.config.grails.plugin.cookiesession.cookiecount
      log.info "grails.plugin.cookiesession.cookiecount set: ${cookieCount}"
    }
    else{
      cookieCount = 5
      log.info "grails.plugin.cookiesession.cookiecount not set. defaulting to ${cookieCount}"
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('serializer') ){
      if( ! ['java','kryo'].contains( ch.config.grails.plugin.cookiesession.serializer ) ){
        serializer = 'java'
        log.error "grails.plugin.cookiesession.serializer set to invalid value. defaulting to 'java'"
      }
      else{
        serializer = ch.config.grails.plugin.cookiesession.serializer
      } 
      
      log.info "grails.plugin.cookiesession.serializer set: ${serializer}"
    }
    else{
      serializer = 'java'
      log.info "grails.plugin.cookiesession.serializer not set. defaulting to ${serializer}"
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('maxcookiesize') ){

      maxCookieSize = ch.config.grails.plugin.cookiesession.maxcookiesize.toInteger()

      if( maxCookieSize < 1024 && maxCookieSize > 4096 ){
        maxCookieSize = 2048
        log.info "grails.plugin.cookiesession.maxCookieSize must be between 1024 and 4096. defaulting to 2048"
      }
      else{
        log.info "grails.plugin.cookiesession.maxCookieSize set: ${maxCookieSize}"
      }
    }
    else{
      maxCookieSize = 2048
      log.info "grails.plugin.cookiesession.maxcookiesize no set. defaulting to ${maxCookieSize}"
    }

    if( maxCookieSize * cookieCount > 6114 ){
      log.warn "the maxcookiesize and cookiecount settings will allow for a max session size of ${maxCookieSize*cookieCount} bytes. Make sure you increase the max http header size in order to support this configuration. see the help file for this plugin for instructions."
    }

    if( ch.config.grails.plugin.cookiesession.containsKey('id') )
      log.warn "the grails.plugin.cookiesession.id setting is deprecated! Use the grails.plugin.cookiesession.cookiename setting instead!"

    if( ch.config.grails.plugin.cookiesession.containsKey('timeout') )
      log.warn "the grails.plugin.cookiesession.timeout setting is deprecated! Use the grails.plugin.cookiesession.sessiontimeout setting instead!" 

    if( ch.config.grails.plugin.cookiesession.hmac.containsKey('secret') )
      log.warn "the grails.plugin.cookiesession.hmac.secret setting is deprecated! Use the grails.plugin.cookiesession.secret setting instead!"

    if( ch.config.grails.plugin.cookiesession.hmac.containsKey('id') )
      log.warn "the grails.plugin.cookiesession.hmac.id setting is deprecated!"

    if( ch.config.grails.plugin.cookiesession.hmac.containsKey('algorithm') )
      log.warn "the grails.plugin.cookiesession.hmac.algorithm is deprecated! Use the grails.plugin.cookiesession.cryptoalgorithm setting instead!"

    // initialize the crypto key
    if( cryptoSecret == null ){
      def keyGenerator = javax.crypto.KeyGenerator.getInstance( cryptoAlgorithm.split('/')[0] )
      def secureRandom = new java.security.SecureRandom()
      keyGenerator.init(secureRandom)
      cryptoKey = keyGenerator.generateKey()
    }
    else{
      cryptoKey = new SecretKeySpec(cryptoSecret, cryptoAlgorithm.split('/')[0])
    }
  }

  SerializableSession restoreSession( HttpServletRequest request ){
    log.trace "restoreSession()"
    
    SerializableSession session = null

    try{
      // - get the data from the cookie 
      // - deserialize the session (handles compression and encryption)
      // - check to see if the session is expired
      // - return the session

      def serializedSession = getDataFromCookie(request)

      if( serializedSession ){
        session = deserializeSession(serializedSession)
      }

      def currentTime = System.currentTimeMillis()
      def lastAccessedTime = session?.lastAccessedTime?:0
      long inactiveInterval = currentTime - lastAccessedTime 

      if( session ){
        if( session.isValid == false ){
          log.info "retrieved invalidated session from cookie. lastAccessedTime: ${new Date(lastAccessedTime)}.";
          session = null
        }
        else if( maxInactiveInterval == -1 || inactiveInterval <= maxInactiveInterval ){
          log.info "retrieved valid session from cookie. lastAccessedTime: ${new Date(lastAccessedTime)}"
          session.isNewSession = false
          session.lastAccessedTime = System.currentTimeMillis()
          session.servletContext = request.servletContext
        }
        else if( inactiveInterval > maxInactiveInterval ){
          log.info "retrieved expired session from cookie. lastAccessedTime: ${new Date(lastAccessedTime)}. expired by ${inactiveInterval} ms.";
          session = null
        }
      }
      else{
        log.info "no session retrieved from cookie."
      }

    }
    catch( excp ){
      log.error "An error occurred while restoring session from cookies. A null session will be returned and a new SerializableSession will be created.. Exception follows:\n ${excp}"
      session = null
    }

    return session
  }

  void saveSession( SerializableSession session, HttpServletResponse response ){
    log.trace "saveSession()"


    String serializedSession = serializeSession(session) 

    if( session.isValid )
      putDataInCookie(response, serializedSession )
    else
      deleteCookie(response)
  }

  String serializeSession( SerializableSession session ){
    log.trace "serializeSession()"

    log.trace "serializing and compressing session"
    ByteArrayOutputStream stream = new ByteArrayOutputStream()

    switch( serializer ){
      case 'kryo':
        Kryo kryo = getConfiguredKryoSerializer()
        Output output = new Output(new GZIPOutputStream(stream))
        kryo.writeObject(output,session)
        output.close()
      break
      
      case 'java':
        def output = new ObjectOutputStream(new GZIPOutputStream(stream))
        output.writeObject(session)
        output.close()
      break
    } 

    byte[] output = null
    if( encryptCookie ){
      log.trace "encrypting serialized session"
      Cipher cipher = Cipher.getInstance(cryptoAlgorithm)
      cipher.init( Cipher.ENCRYPT_MODE, cryptoKey ) 
      output = cipher.doFinal(stream.toByteArray())
    }
    else{
      output = stream.toByteArray()
    }

    log.trace "base64 encoding serialized session"
    def serializedSession = output.encodeBase64().toString()

    log.info "serialized session: ${serializedSession.size()} bytes"
    return serializedSession
  }

  SerializableSession deserializeSession( String serializedSession ){
    log.trace "deserializeSession()"

    def session = null

    try
    {
      log.trace "decodeBase64 serialized session"
      def input = serializedSession.decodeBase64()

      if( encryptCookie ){
        log.trace "decrypting cookie"
        Cipher cipher = Cipher.getInstance(cryptoAlgorithm)
        cipher.init( Cipher.DECRYPT_MODE, cryptoKey ) 
        input = cipher.doFinal(input)
      }

      log.trace "decompressing and deserializing session"

      switch( serializer ){
        case 'kryo':
          def inputStream = new Input(new GZIPInputStream( new ByteArrayInputStream( input ) ))
          Kryo kryo = getConfiguredKryoSerializer()
          session = kryo.readObject(inputStream,SerializableSession.class)
        break;
        
        case 'java':
          def inputStream = new ObjectInputStream(new GZIPInputStream( new ByteArrayInputStream( input ) )){
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

          session = (SerializableSession)inputStream.readObject();
        break;
      }
    }
    catch( excp ){
      log.error "An error occurred while deserializing a session. ${excp}"
      log.error "cause: ${excp.cause}"
      excp.printStackTrace()
      session = null
    }

    log.debug "deserialized session: ${session != null}"

    return session 
  }

  private def getConfiguredKryoSerializer(){
    def kryo = new Kryo()
    def flashScopeSerializer = new FieldSerializer(kryo, GrailsFlashScope.class);
    kryo.register(GrailsFlashScope.class,flashScopeSerializer)
    kryo.register(SerializableSession.class)
    kryo.classLoader = grailsApplication.classLoader
    kryo.instantiatorStrategy = new StdInstantiatorStrategy()
    UnmodifiableCollectionsSerializer.registerSerializers( kryo );
    return kryo
  }

  private String[] splitString(String input){
    log.trace "splitString()"

    String[] list = new String[cookieCount];

    if( !input ){
      log.trace "input empty or null."
      return list
    }

    int inputLength = input.size()

    def partitions = Math.ceil(inputLength / maxCookieSize)
    log.trace "splitting input of size ${input.size()} string into ${partitions} paritions"

    for (int i = 0; i < partitions; i++){ 
      int start = i * maxCookieSize
      int end = Math.min(start + maxCookieSize - 1, inputLength - 1)
      list[i] = input[start..end]
    }

    return list
  }

  private String combineStrings(def input){
    log.trace "combineStrings()"
    def output = input.join()
    log.trace "combined ${input.size()} strings into output of length ${output.size()}."
    return output
  }

  String getDataFromCookie(HttpServletRequest request){
    log.trace "getDataFromCookie()"

    def values = request.cookies.findAll{ 
        it.name.startsWith(cookieName) }?.sort{ 
          it.name.split('-')[1].toInteger() }.collect{ it.value }
   
    String data = combineStrings(values)
    log.debug "retrieved ${data.size()} bytes of data from ${values.size()} session cookies."
    
    return data 
  }

  void putDataInCookie(HttpServletResponse response, String value){
    log.trace "putDataInCookie() - ${value.size()}"

    // the cookie's maxAge will either be -1 or the number of seconds it should live for
    def maxAge = maxInactiveInterval == -1 ? maxInactiveInterval : (Integer)(maxInactiveInterval / 1000)

    if( value.length() > maxCookieSize * cookieCount )
    {
      log.error "Serialized session exceeds maximum session size that can be stored in cookies. Max size: ${maxCookieSize*cookieCount}, Requested Session Size: ${value.length()}."
      throw new Exception("Serialized session exceeded max size.") 
    }  

    def partitions = splitString(value)
    partitions.eachWithIndex{ it, i ->
      Cookie c = new Cookie( "${cookieName}-${i}".toString(), it?:'')
      c.setSecure(false)
      c.setPath("/")
      c.maxAge = maxAge
      response.addCookie(c)
      log.trace "added ${cookieName}-${i} to response"
   }

   log.debug "added ${partitions.size()} session cookies to response."
  }

  void deleteCookie(HttpServletResponse response){
    log.trace "deleteCookie()"
    (0..cookieCount).eachWithIndex{ it, i ->
      Cookie c = new Cookie( "${cookieName}-${i}".toString(), '')
      c.setSecure(false)
      c.setPath("/")
      c.maxAge = 0
      response.addCookie(c)
      log.trace "added ${cookieName}-${i} to response with maxAge == 0"
   }
  }

  boolean isSessionIdValid(String sessionId){
    log.trace "isSessionIdValid() : ${sessionId}"
    return true;
  }
}
