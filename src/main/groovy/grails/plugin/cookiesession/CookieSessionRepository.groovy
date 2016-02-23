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
 *  benlucchesi@gmail.com
 */

package grails.plugin.cookiesession;

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

import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SealedObject
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

//import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope
import org.grails.web.servlet.GrailsFlashScope

import java.util.UUID

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CookieSessionRepository implements SessionRepository, InitializingBean, ApplicationContextAware  {

  final static Logger log = LoggerFactory.getLogger(CookieSessionRepository.class.getName());

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
  Boolean secure
  Boolean httpOnly
  String path
  String domain
  String comment
  String serializer = "java"
  Boolean useSessionCookieConfig
  Boolean useInitializationVector
  Boolean useGCMmode
  def servletContext 

  def sessionCookieConfigMethods = [:]

  SessionSerializer sessionSerializer = null

  void configureCookieSessionRepository(){

    log.info "configuring CookieSessionRepository"

    if( applicationContext.containsBean('servletContext') )
      servletContext = applicationContext.getBean('servletContext')

    assignSettingFromConfig( 'useSessionCookieConfig', false, Boolean, 'useSessionCookieConfig' )
    if( useSessionCookieConfig ){

      if( servletContext?.majorVersion < 3 )
          useSessionCookieConfig = false

      if( useSessionCookieConfig == false )
        log.warn "useSessionCookieConfig was enabled in the config file, but has been disabled because the servlet does not support SessionCookieConfig."  
    }

    // if useSessionCookieConfig, then attach to invokeMethod and setProperty so that the values can be intercepted and assigned to local variables
    if( useSessionCookieConfig ){

      servletContext.sessionCookieConfig.class.metaClass.invokeMethod = { String method, args ->
        switch( method ){
          case 'setName':
            this.cookieName = args[0]
            break
          case 'setHttpOnly':
            this.httpOnly = args[0]
            break
          case 'setSecure':
            this.secure = args[0]
            break
          case 'setPath':
            this.path = args[0]
            break
          case 'setDomain':
            this.domain = args[0]
            break
          case 'setComment':
            this.comment = args[0]
            break
          case 'setMaxAge':
            this.maxInactiveInterval = args[0]
            break
        }

        servletContext.sessionCookieConfig.metaClass.methods.find{ it.name == method }?.invoke( servletContext.sessionCookieConfig, args )
        log.trace "detected sessionCookieConfig.${method} -> ${args}"
      }

      servletContext.sessionCookieConfig.class.metaClass.setProperty = { String property, value ->
        switch( property ){
          case 'name':
            this.cookieName = value
            break
          case 'httpOnly':
            this.httpOnly = value
            break
          case 'secure':
            this.secure = value
            break
          case 'path':
            this.path = value
            break
          case 'domain':
            this.domain = value
            break
          case 'comment':
            this.comment = value
            break
          case 'maxAge':
            this.maxInactiveInterval = value
            break
        }

        servletContext.sessionCookieConfig.metaClass.properties.find{ it.name == property }.setProperty( servletContext.sessionCookieConfig, value )
        log.trace "detected sessionCookieConfig.${property} -> ${value}"
      }
    }

    assignSettingFromConfig( 'encryptcookie', false, Boolean, 'encryptCookie' )
    assignSettingFromConfig( 'cryptoalgorithm', 'Blowfish', String, 'cryptoAlgorithm' )
    
    def cryptoSecretConfig = grailsApplication.config.grails.plugin.cookiesession.find{ k,v -> k.equalsIgnoreCase('secret') }
    if( cryptoSecretConfig ){
      if( cryptoSecretConfig.value instanceof byte[] ){
        cryptoSecret = cryptoSecretConfig.value 
        log.trace "grails.plugin.cookiesession.secret set with byte[]"
      }
      else if( cryptoSecretConfig.value instanceof String ){
        cryptoSecret = cryptoSecretConfig.value.bytes
        log.trace "grails.plugin.cookiesession.secret set with String.bytes"
      }
      else if( cryptoSecretConfig.value instanceof ArrayList ){
        cryptoSecret = cryptoSecretConfig.value as byte[]
        log.trace "grails.plugin.cookiesession.secret set with ArrayList as byte[]"
      }
    }
    
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
  
    assignSettingFromConfig( 'cookiename', 'gsession', String, 'cookieName')
    assignSettingFromConfig( 'setsecure', false, Boolean, 'secure')
    assignSettingFromConfig( 'httponly', false, Boolean, 'httpOnly')
    assignSettingFromConfig( 'path', '/', String, 'path')
    assignSettingFromConfig( 'domain', null, String, 'domain')
    assignSettingFromConfig( 'comment', null, String, 'comment')
    assignSettingFromConfig( 'sessiontimeout', -1, Long, 'maxInactiveInterval')

    if( useSessionCookieConfig ){
      this.cookieName = servletContext.sessionCookieConfig.name ?: cookieName
      this.httpOnly =  servletContext.sessionCookieConfig.httpOnly ?: secure
      this.secure = servletContext.sessionCookieConfig.secure ?: secure
      this.path = servletContext.sessionCookieConfig.path ?: path
      this.domain = servletContext.sessionCookieConfig.domain ?: domain
      this.comment = servletContext.sessionCookieConfig.comment ?: comment
      this.maxInactiveInterval = servletContext.sessionCookieConfig.maxAge ?: maxInactiveInterval
      log.trace "processed sessionCookieConfig. cookie settings are: [name: ${cookieName}, httpOnly: ${httpOnly}, secure: ${secure}, path: ${path}, domain: ${domain}, comment: ${comment}, maxAge: ${maxInactiveInterval}]" 
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
      cryptoKey = new SecretKeySpec(cryptoSecret, cryptoAlgorithm.split('/')[0])
    }
  
    // determine if an initialization vector is needed
    useInitializationVector = cryptoAlgorithm.indexOf('/') < 0 ? false : cryptoAlgorithm.split('/')[1].toUpperCase() != 'ECB' 
    useGCMmode =  cryptoAlgorithm.indexOf('/') < 0 ? false : cryptoAlgorithm.split('/')[1].toUpperCase() == 'GCM'


    // check to see if spring security's sessionfixationprevention is turned on
    if( grailsApplication.config.grails.plugin.springsecurity.useSessionFixationPrevention == true ){
      log.error "grails.plugin.springsecurity.useSessionFixationPrevention == true. Spring Security Session Fixation Prevention is incompatible with cookie session plugin. Your application will experience unexpected behavior."
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
        session = deserializeSession(serializedSession, request)
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
   
    if( session.isValid )
      putDataInCookie(response, serializedSession )
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

      if( useInitializationVector ){
        def iv = cipher.IV
        def output = [iv.length]
        output.addAll(iv)
        output.addAll(bytes)
        bytes = output as byte[]
      }
    }

    log.trace "base64 encoding serialized session from ${bytes.length} bytes"
    def serializedSession = bytes.encodeBase64().toString()

    log.info "serialized session: ${serializedSession.size()} bytes"
    return serializedSession
  }

  SerializableSession deserializeSession( String serializedSession, HttpServletRequest request ){
    log.trace "deserializeSession()"

    def session = null
    
    try
    {
      log.trace "decodeBase64 serialized session from ${serializedSession.size()} bytes."
      def input = serializedSession.decodeBase64()
 
      if( encryptCookie ){
        log.trace "decrypting serialized session from ${input.length} bytes."
        Cipher cipher = Cipher.getInstance(cryptoAlgorithm)

        if( useInitializationVector ){
          int ivLen = input[0]
          if ( useGCMmode ) {
            GCMParameterSpec ivSpec = new GCMParameterSpec(128, input, 1, ivLen)
            cipher.init( Cipher.DECRYPT_MODE, cryptoKey, ivSpec )
          } else {
            IvParameterSpec ivSpec = new IvParameterSpec(input, 1, ivLen)
            cipher.init( Cipher.DECRYPT_MODE, cryptoKey, ivSpec )
          }
          input = cipher.doFinal( input, 1 + ivLen, input.length - 1 - ivLen )
        } else {
          cipher.init( Cipher.DECRYPT_MODE, cryptoKey )
          input = cipher.doFinal(input)
        }
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
       if( log.isDebugEnabled() )
         log.debug "Serialized-session: '$serializedSession'\n" +
                   "request-uri: ${request.requestURI}" +
                   request.headerNames.toList().inject('') { str, name ->
                     request.getHeaders(name).inject(str) { str2, val -> "$str2\n$name: $val" }
                   }
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

  void putDataInCookie(HttpServletResponse response, String value ){
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
      else
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

    if( servletContext?.majorVersion >= 3 )
      c.httpOnly = httpOnly

    if( domain )
      c.domain = domain
    if( comment )
      c.comment = comment

    if( servletContext?.majorVersion >= 3 )
      log.trace "created cookie name=${c.name}, maxAge=${c.maxAge}, secure=${c.secure}, path=${c.path}, httpOnly=${c.httpOnly}, domain=${c.domain}, comment=${c.comment}"
    else
      log.trace "created cookie name=${c.name}, maxAge=${c.maxAge}, secure=${c.secure}, path=${c.path}, domain=${c.domain}, comment=${c.comment}"

    return c
  }

  boolean isSessionIdValid(String sessionId){
    log.trace "isSessionIdValid() : ${sessionId}"
    return true;
  }
}
