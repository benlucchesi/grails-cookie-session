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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.ByteArrayOutputStream;

import javax.servlet.ServletContext;
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

import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope

import java.util.UUID

import org.apache.log4j.Logger;

class CookieSessionRepository implements SessionRepository, InitializingBean, ApplicationContextAware  {

  final static Logger log = Logger.getLogger(CookieSessionRepository.class.getName());

  def grailsApplication
  ApplicationContext applicationContext
  def cryptoKey

  boolean encryptCookie = true
  String cryptoAlgorithm = "Blowfish"
  def cryptoSecret = null
  
  long maxInactiveInterval = 120 

  int cookieCount = 5
  int maxCookieSize = 2048

  String cookieName
  boolean secure
  Boolean httpOnly
  String path
  String domain
  String comment
  String serializer = "java"

  def sessionCookieConfigMethods = [:]

  SessionSerializer sessionSerializer = null

  void configureCookieSessionRepository(){

    log.info "configuring CookieSessionRepository"
     
    def servletContext 
    def checkSessionCookieConfig = false

    if( applicationContext.containsBean('servletContext') ){
      servletContext = applicationContext.getBean('servletContext')
      checkSessionCookieConfig = servletContext.majorVersion >= 3; 
    }

    assignSettingFromConfig( 'encryptcookie', false, Boolean, 'encryptCookie' )
    assignSettingFromConfig( 'cryptoalgorithm', 'Blowfish', String, 'cryptoAlgorithm' )
    assignSettingFromConfig( 'secret', null, String, 'cryptoSecret' )
    assignSettingFromConfig( 'cookiecount', 5, Integer, 'cookieCount' )

    assignSettingFromConfig('serializer','java',String,'serializer' )
    if( serializer == "java" ){
      serializer = "javaSessionSerializer"
    }
    else if( serializer == "kryo" ){
      serializer = "kryoSessionSerializer"
    }
    else if( applicationContext.containsBean(serializer) && applicationContext.getType(serializer) instanceof SessionSerializer ){

    }
    else{
      log.error "no valid serializer configured. defaulting to java"
      serializer = "javaSessionSerializer"
    }
      
    assignSettingFromConfig( 'maxcookiesize', 2048, Integer, 'maxCookieSize')
    if( maxCookieSize < 1024 && maxCookieSize > 4096 ){
        maxCookieSize = 2048
        log.info "grails.plugin.cookiesession.maxCookieSize must be between 1024 and 4096. defaulting to 2048"
    }
    else{
        log.info "grails.plugin.cookiesession.maxCookieSize set: ${maxCookieSize}"
    }

    if( maxCookieSize * cookieCount > 6114 ){
      log.warn "the maxcookiesize and cookiecount settings will allow for a max session size of ${maxCookieSize*cookieCount} bytes. Make sure you increase the max http header size in order to support this configuration. see the help file for this plugin for instructions."
    }

    if( !assignSettingFromConfig( 'cookiename', 'gsession', String, 'cookieName') && checkSessionCookieConfig ){
      assignSettingFromSessionCookieConfig('name', 'cookieName')
    }

    if( !assignSettingFromConfig( 'setsecure', false, Boolean, 'secure') && checkSessionCookieConfig ){
      assignSettingFromSessionCookieConfig('secure', 'secure')
    }

    if( !assignSettingFromConfig( 'httponly', false, Boolean, 'httpOnly') && checkSessionCookieConfig ){
      assignSettingFromSessionCookieConfig( 'httponly', 'httpOnly')
    }

    if( !assignSettingFromConfig( 'path', '/', String, 'path') && checkSessionCookieConfig ){
      assignSettingFromSessionCookieConfig( 'path', 'path' )
    }

    if( !assignSettingFromConfig( 'domain', null, String, 'domain') && checkSessionCookieConfig ){
      assignSettingFromSessionCookieConfig( 'domain', 'domain')
    }

    if( !assignSettingFromConfig( 'comment', null, String, 'comment') && checkSessionCookieConfig){
      assignSettingFromSessionCookieConfig( 'comment', 'comment')
    }

    if( !assignSettingFromConfig( 'sessiontimeout', -1, Long, 'maxInactiveInterval') && checkSessionCookieConfig ){
      assignSettingFromSessionCookieConfig( 'maxAge', 'maxInactiveInterval') 
    }

    if( grailsApplication.config.grails.plugin.cookiesession.containsKey('springsecuritycompatibility') )
      log.info "grails.plugin.cookiesession.springsecuritycompatibility set: ${grailsApplication.config.grails.plugin.cookiesession['springsecuritycompatibility']}"
    else
      log.info "grails.plugin.cookiesession.springsecuritycompatibility not set. defaulting to false"

    if( grailsApplication.config.grails.plugin.cookiesession.containsKey('id') )
      log.warn "the grails.plugin.cookiesession.id setting is deprecated! Use the grails.plugin.cookiesession.cookiename setting instead!"

    if( grailsApplication.config.grails.plugin.cookiesession.containsKey('timeout') )
      log.warn "the grails.plugin.cookiesession.timeout setting is deprecated! Use the grails.plugin.cookiesession.sessiontimeout setting instead!" 

    if( grailsApplication.config.grails.plugin.cookiesession.hmac.containsKey('secret') )
      log.warn "the grails.plugin.cookiesession.hmac.secret setting is deprecated! Use the grails.plugin.cookiesession.secret setting instead!"

    if( grailsApplication.config.grails.plugin.cookiesession.hmac.containsKey('id') )
      log.warn "the grails.plugin.cookiesession.hmac.id setting is deprecated!"

    if( grailsApplication.config.grails.plugin.cookiesession.hmac.containsKey('algorithm') )
      log.warn "the grails.plugin.cookiesession.hmac.algorithm is deprecated! Use the grails.plugin.cookiesession.cryptoalgorithm setting instead!"

    // initialize the crypto key
    if( cryptoSecret == null ){
      def keyGenerator = javax.crypto.KeyGenerator.getInstance( cryptoAlgorithm.split('/')[0] )
      def secureRandom = new java.security.SecureRandom()
      keyGenerator.init(secureRandom)
      cryptoKey = keyGenerator.generateKey()
    }
    else{
      cryptoKey = new SecretKeySpec(cryptoSecret.bytes, cryptoAlgorithm.split('/')[0])
    }
  }

  private boolean assignSettingFromConfig(def settingName, def defaultValue, Class t, def targetPropertyName){
    def assignedSetting = false
    try{
      def configKey = grailsApplication.config.grails.plugin.cookiesession.find{ k,v -> k.equalsIgnoreCase(settingName) }
      if( configKey ){
        this.(targetPropertyName.toString()) = configKey.value.asType(t)
        log.info "grails.plugin.cookiesession.${configKey.key} set: \'${this.(targetPropertyName.toString())}\'"
        assignedSetting = true
      }
      else{
        this.(targetPropertyName.toString()) = defaultValue
        log.info "configuring ${settingName} to default value: ${defaultValue}"
        return false
      }
    }
    catch( excp ){
      log.error "error configuring settting '${settingName}'", excp
      assignedSetting = false
    }

    return assignedSetting
  }

  private boolean assignSettingFromSessionCookieConfig(def settingName, def targetPropertyName){
    def assignedSetting = false
    def servletContext = applicationContext.getBean('servletContext')
    def setSetting = servletContext.sessionCookieConfig.metaClass.methods.find{ it.name.equalsIgnoreCase("set${settingName}") }
    def getSetting = servletContext.sessionCookieConfig.metaClass.methods.find{ it.name.equalsIgnoreCase("get${settingName}") || it.name.equalsIgnoreCase("is${settingName}") }

    if( setSetting && getSetting ){
      servletContext.sessionCookieConfig.class.metaClass."${setSetting.name}" = { newSettingValue ->
      try{
        if( newSettingValue != null ){
          this.log.trace "detected sessionCookieConfig setting changed: ${newSettingValue}"
          this.(targetPropertyName.toString()) = newSettingValue
          setSetting.invoke(servletContext.sessionCookieConfig,newSettingValue)
        }
       }
       catch( excp ){
        this.log.error "error updating setting from sessionCookieConfig ", excp
       }
      }
   
      def settingValue = getSetting.invoke(servletContext.sessionCookieConfig)
      if( settingValue != null ){
        this.(targetPropertyName.toString()) = getSetting.invoke(servletContext.sessionCookieConfig)
        log.info "servletContext.sessionCookieConfig.${getSetting.name.replaceFirst('set','')} set: \'${this.(targetPropertyName.toString())}\'"
        assignedSetting = true
      }
    }
    else{
      log.warn "unable to find sessionCookieConfig method for ${settingName}"
      assignedSetting = false
    }

    return assignedSetting
  }

  void afterPropertiesSet(){
    log.trace "afterPropertiesSet()"
    configureCookieSessionRepository()
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

      def maxInactiveIntervalMillis = maxInactiveInterval * 1000;
      def currentTime = System.currentTimeMillis()
      def lastAccessedTime = session?.lastAccessedTime?:0
      long inactiveInterval = currentTime - lastAccessedTime 

      if( session ){
        if( session.isValid == false ){
          log.info "retrieved invalidated session from cookie. lastAccessedTime: ${new Date(lastAccessedTime)}.";
          session = null
        }
        else if( maxInactiveInterval == -1 || inactiveInterval <= maxInactiveIntervalMillis ){
          log.info "retrieved valid session from cookie. lastAccessedTime: ${new Date(lastAccessedTime)}"
          session.isNewSession = false
          session.lastAccessedTime = System.currentTimeMillis()
          session.servletContext = request.servletContext
        }
        else if( inactiveInterval > maxInactiveIntervalMillis ){
          log.info "retrieved expired session from cookie. lastAccessedTime: ${new Date(lastAccessedTime)}. expired by ${inactiveInterval} ms.";
          session = null
        }
      }
      else{
        log.info "no session retrieved from cookie."
      }

    }
    catch( excp ){
      log.error "An error occurred while restoring session from cookies. A null session will be returned and a new SerializableSession will be created.", excp
      session = null
    }

    return session
  }

  void saveSession( SerializableSession session, HttpServletResponse response ){
    log.trace "saveSession()"

    String serializedSession = serializeSession(session) 
   
    def requestCookieCount = cookieCount
    if( response instanceof SessionRepositoryResponseWrapper )
      requestCookieCount = response.request?.cookies?.count{ it.name.startsWith(cookieName) } 

    if( session.isValid )
      putDataInCookie(response, serializedSession, requestCookieCount )
    else
      deleteCookie(response)
  }

  String serializeSession( SerializableSession session ){
    log.trace "serializeSession()"

    log.trace "getting sessionSerializer: ${serializer}"
    def sessionSerializer = applicationContext.getBean(serializer)

    log.trace "serializing session"
    byte[] bytes = sessionSerializer.serialize(session)
   
    log.trace "compressing serialiezd session from ${bytes.length} bytes"
    ByteArrayOutputStream stream = new ByteArrayOutputStream()
    def gzipOut = new GZIPOutputStream(stream)
    gzipOut.write(bytes,0,bytes.length)
    gzipOut.close()
 
    bytes = stream.toByteArray()
    log.trace "compressed serialized session to ${bytes.length}"

    if( encryptCookie ){
      log.trace "encrypting serialized session"
      Cipher cipher = Cipher.getInstance(cryptoAlgorithm)
      cipher.init( Cipher.ENCRYPT_MODE, cryptoKey ) 
      bytes = cipher.doFinal(bytes)
    }

    log.trace "base64 encoding serialized session from ${bytes.length} bytes"
    def serializedSession = bytes.encodeBase64().toString()

    log.info "serialized session: ${serializedSession.size()} bytes"
    return serializedSession
  }

  SerializableSession deserializeSession( String serializedSession ){
    log.trace "deserializeSession()"

    def session = null
    
    try
    {
      log.trace "decodeBase64 serialized session from ${serializedSession.size()} bytes."
      def input = serializedSession.decodeBase64()
 
      if( encryptCookie ){
        log.trace "decrypting serialized session from ${input.length} bytes."
        Cipher cipher = Cipher.getInstance(cryptoAlgorithm)
        cipher.init( Cipher.DECRYPT_MODE, cryptoKey ) 
        input = cipher.doFinal(input)
      }

      log.trace "decompressing serialized session from ${input.length} bytes"
      def inputStream = new GZIPInputStream( new ByteArrayInputStream( input ) )
      def outputStream = new ByteArrayOutputStream()
      
      byte[] buffer = new byte[1024]
      int bytesRead = 0
      while( (bytesRead = inputStream.read(buffer)) != -1 ){
        outputStream.write(buffer,0,bytesRead)
      }
      outputStream.flush()
     
      byte[] bytes = outputStream.toByteArray()
      log.trace "decompressed serialized session to ${bytes.length} bytes"

      def sessionSerializer = applicationContext.getBean(serializer)
      session = sessionSerializer.deserialize(bytes) 
    }
    catch( excp ){
      log.error "An error occurred while deserializing a session.", excp
      session = null
    }

    log.debug "deserialized session: ${session != null}"

    return session 
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

  void putDataInCookie(HttpServletResponse response, String value, def requestCookieCount ){
    log.trace "putDataInCookie() - ${value.size()}"

    // the cookie's maxAge will either be -1 or the number of seconds it should live for
    // - sessiontimeout config value overrides maxage

    if( value.length() > maxCookieSize * cookieCount )
    {
      log.error "Serialized session exceeds maximum session size that can be stored in cookies. Max size: ${maxCookieSize*cookieCount}, Requested Session Size: ${value.length()}."
      throw new Exception("Serialized session exceeded max size.") 
    }

    def partitions = splitString(value)
    partitions.eachWithIndex{ it, i ->
      if( it )
      {
        Cookie c = createCookie(i, it, maxInactiveInterval ) // if the value of the cookie will be empty, then delete it.. 
        response.addCookie(c)
        log.trace "added session ${cookieName}-${i} to response"
      }
      else if( i < requestCookieCount )
      {
        // create a delete cookie
        Cookie c = createCookie(i, '', 0) 
        response.addCookie(c)
        log.trace "added delete ${cookieName}-${i} to response"
      }
   }

   log.debug "added ${partitions.size()} session cookies to response."
  }

  void deleteCookie(HttpServletResponse response){
    log.trace "deleteCookie()"
    (0..cookieCount).eachWithIndex{ it, i ->
      Cookie c = createCookie(i, '', 0)
      response.addCookie(c)
      log.trace "added ${cookieName}-${i} to response with maxAge == 0"
    }
  }

  private Cookie createCookie(int i, String value, long m ) {

    Cookie c = new Cookie( "${cookieName}-${i}".toString(), value)

    c.maxAge = m // maxage overrides class scope variable
    c.secure = secure
    c.path = path
    c.httpOnly = httpOnly

    if( domain )
      c.domain = domain
    if( comment )
      c.comment = comment

    log.trace "created cookie name=${c.name}, maxAge=${c.maxAge}, secure=${c.secure}, path=${c.path}, httpOnly=${c.httpOnly}, domain=${c.domain}, comment=${c.comment}"

    return c
  }

  boolean isSessionIdValid(String sessionId){
    log.trace "isSessionIdValid() : ${sessionId}"
    return true;
  }
}
