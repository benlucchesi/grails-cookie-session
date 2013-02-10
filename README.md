
# Cookie Session Grails Plugin

Current Version: 2.0.6

The Cookie Session plugin enables grails applications to store session data in http cookies between requests instead of in memory on the server. Client sessions are transmitted from the browser to the application with each request and transmitted back with each response. This allows application deployments to be more stateless. Benefits of managing sessions this way include:

* Simplified Scaling

      Because a client's session is passed with every request, the deployment architecture need not be concerned 
      with scaling strategies that account for sessions stored on the server, such as session replication or sticky sessions. 
      Simply add application instances and route requests to them. Also, because the session data is stored with client, 
      the server doesn't expend memory or disk space storing sessions that are open for long periods of time.

* Fault Tolerance
    
      When sessions are stored in memory on the server, if an application crashes or becomes inaccessible,
      clients' sessions are usually lost which can result in unexpected logouts, redirects, or loss of data. 
      When sessions are stored cookies,the applications can be much more tolerant to server-side commission failures. 
      In a single-instance deployment scenario, the server or application can be recycled 
      and clients can continue working when the application becomes available and with their session fully intact. In a 
      multi-instance deployment scenario, any instance of the applicatin can service a clients request. A benificial 
      side effect of cookie-sessions is that applications can be upgraded or restarted without logging out users.

## Features new to version 2.0

+ compatible with flash scope
+ compatible with webflow
+ supports secure sessions
+ supports sessions larger than 4kb

# Installation

grails install-plugin cookie-session

  or

edit grails/conf/Build.config and add the following line under the plugins closure

  runtime ":cookie-session:2.0.5"

# Configuration
The following parameters are supported directly by the cookie-session-v2 plugin. Note, additional configuration is needed for webflow and large session support. See additional instructions below.

## Parameters
<table>
  <thead>
    <tr>
      <th>name</th>
      <th>default</th>
      <th>description</th>
  </thead>
  <tbody>
    <tr>
      <td>grails.plugin.cookiesession.enabled</td>
      <td>true</td>
      <td>enables or disables the cookie session.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.encryptcookie</td>
      <td>true</td>
      <td>enable or disable encrypting session data stored in cookies.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.cryptoalgorithm</td>
      <td>Blowfish</td>
      <td>The cryptographic algorithm used to encrypt session data (i.e. Blowfish, DES, DESEde, AES). NOTE: the secret must be compatible with the crypto algorithm.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.secret</td>
      <td><b>generated</b></td>
      <td>The secret key used to encrypt session data. If not set, a random key will be created at runtime. Set this parameter if deploying multiple instances of the application or if sessions need to survive a server crash or restart. sessions to be recovered after a server crash or restart.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.cookiecount</td>
      <td>5</td>
      <td>The maximum number of cookies that are created to store the session in</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.maxcookiesize</td>
      <td>2048</td>
      <td>The max size for each cookie expressed in bytes.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.sessiontimeout</td>
      <td>0</td>
      <td>The length of time a session can be inactive for expressed in seconds. -1 indicates that a session will be active for as long as the browser is open.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.cookiename</td>
      <td>gsession-X</td>
      <td>X number of cookies will be written per the cookiecount parameter. Each cookie is suffixed with the integer index of the cookie.</td>
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.condenseexceptions</td>
      <td>false</td>
      <td>replaces instances of Exceptions objects in the session with the Exception.getMessage() in the session (see SessionPersistanceListener for further details)</td> 
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.serializer</td>
      <td>'java'</td>
      <td>specify serializer used to serialize session objects. valid values are: 'java', 'kryo'</td> 
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.id</td>
      <td><b>deprecated</b></td>
      <td>deprecated. use the 'grails.plugin.cookiesession.cookiename' setting.</td>
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.timeout</td>
      <td><b>deprecated</b></td>
      <td>deprecated. use the 'grails.plugin.cookiesession.sessiontimeout' setting.</td>
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.hmac.secret</td>
      <td><b>deprecated</b></td>
      <td>deprecated. use the 'grails.plugin.cookiesession.secret' setting.</td>
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.hmac.id</td>
      <td><b>deprecated</b></td>
      <td>deprecated. no equivelent setting is present in this version of the plugin.</td>
    </tr>

    <tr>
      <td>grails.plugin.cookiesession.hmac.algorithm  </td>
      <td><b>deprecated</b></td>
      <td>deprecated. use the 'grails.plugin.cookiesession.cryptoalgorithm' settings.</td>
    </tr>


  </tbody>
</table>

## Example

Config.groovy

    grails.plugin.cookiesession.enabled = true
    grails.plugin.cookiesession.encryptcookie = true
    grails.plugin.cookiesession.cryptoalgorithm = "Blowfish"
    grails.plugin.cookiesession.secret = "This is my secret."
    grails.plugin.cookiesession.cookiecount = 10
    grails.plugin.cookiesession.maxcookiesize = 2048  // 2kb
    grails.plugin.cookiesession.sessiontimeout = 3600 // one hour
    grails.plugin.cookiesession.cookiename = 'gsession'
    grails.plugin.cookiesession.condenseexceptions = false
    grails.plugin.cookiesession.serializer = 'kryo'

## Understanding cookiecount and maxcookiesize
The maximum session size stored by this plugin is calculated by (cookiecount * maxcookiesize). The reason for these two parameters is that through experimentation, some browsers didn't reliably set large cookies set before the subsequent request. To solve this issue, this plugin supports configuring the max size of each cookie stored and the number of cookies to span the session over. The default values are conservative. If sessions exceed the max session size as configured, first increase the cookiecount and then the maxcookiesize parameters.

## Enabling large session
To enable large sessions, increase the max http header size for the servlet container you are using. 

Due to the potentially large amount of data that may be stored, consider setting it to something large, such as 262144 ( 256kb ).

### Tomcat
Edit the server.xml and set the connector's maxHttpHeaderSize parameter. 

When developing in grails, configure the embedded tomcat server with the tomcat configuration event:

1.  create the file scripts/_Events.groovy in your project directory
2.  add the following code:

        eventConfigureTomcat = {tomcat ->
          tomcat.connector.setAttribute("maxHttpHeaderSize",262144)
        }

### Jetty (2.0.5+)
Edit the jetty.xml or web.xml and set the connector's requestHeaderSize and responseHeaderSize parameters.

1.  create the file scripts/_Events.groovy in your project directory
2.  add the following code:

        eventConfigureJetty = {jetty ->
          jetty.connectors[0].requestHeaderSize = 262144
          jetty.connectors[0].responseHeaderSize = 262144
        }

## Enabling Webflow Support
In order for cookie-session-v2 to work with webflows correctly, additional hibernate configuration is needed.

1.  create the hibernate.cfg.xml file: grails create-hibernate-cfg-xml
2.  edit the grails-app/conf/hibernate/hibernate.cfg.xml file and the hibernate.session_factory_name property under the session-factory element

        <hibernate-configuration>
          <session-factory>
            <property name="hibernate.session_factory_name">session_factory</property>
          </session-factory>
        </hibernate-configuration>

These configuration changes are needed to support deserializing a webflow conversation container by an
application instance other than the one that serialized the conversation container. The conversation container contains an object
that references the hibernate session factory. By default, the hibernate session factory is assigned a name at runtime. 
This causes deserialization of the conversation container to fail because a session factory with the same name 
when it was serialized isn't present. This scenario occurs when the conversation container is deserialized 
by an instance of of the application other than the one where the conversation container originated. The solution is to explicitly 
name the session factory so that an object with the same name is always available during deserialization.

## SessionPersistenceListener (versions 2.0.3+)
SessionPersistenceListener is an interface used inspect the session just after its been deserialized from persistent storage and just before being serialized and persisted. 

SessionPersistenceListener defines the following methods:
    void afterSessionRestored( SerializableSession session )
    void beforeSessionSaved( SerializableSession session )

To use, write a class that implements this interface and define the object in the application's spring application context (grails-app/conf/spring/resources.groovy). The CookieSession plugin will scan the application context and retrieve references to all classes that implement SessionPersistenceListener. The order that the SessionPersistenceListeners are called is unspecified. For an example of how to implement a SessionPersistenceListener, see the ExceptionCondenser class which is part of the cookie-session plugin.

The ExceptionCondenser uses beforeSessionSaved() to replace instances of Exceptions the exception's message. This is useful because some libraries, notably the spring-security, store exceptions in the session, which can cause the cookie-session storage to overflow. The ExceptionCondenser can be installed by either adding it in the application context or by enabling it with the convenience settings grails.plugin.cookiesession.condenseexceptions = true.

## Configuring Serialization (version 2.0.4+)
The grails.plugin.cookiesession.serializer config setting is used to pick which serializer the cookie-session plugin will use to serialize sessions. Currently, only two options are supported: 'java' and 'kryo'. 'java' is used to pick the java.io API serializer. This serializer has proven to be reliable and works 'out of the box'. 'kryo' is used to pick the Kryo serializer (http://code.google.com/p/kryo/). The Kryo serializer has many benifits over the Java serializer, primarily serialized results are significantly smaller which reduces the size of the session cookies. However, the Kryo serializer requires configuration to work correctly with some grails and spring objects, namely Authentication objects from the Spring Security plugin and the GrailsFlashScope object. By default, the cookie-session plugin configures the kryo serializer to handle these two special cases, however more special cases requiring additional configuration may exist. If your application uses the 'kryo' option, configure info level logging for 'com.granicus.grails.plugins.cookiesession.CookieSessionRepository' for test and development environments and verify that kryo is successfully serializing and deserializing all objects that will eventually be stored in the session. If objects fail to serialize, please report an issue to this github project.

NOTE: the next release of the cookie-session plugin will include a means by which to configure the kryo serializer directly

## Logging

The following log4j keys are configurable:

  *   com.granicus.grails.plugins.cookiesession.CookieSessionFilter
  *   com.granicus.grails.plugins.cookiesession.SessionRepositoryRequestWrapper
  *   com.granicus.grails.plugins.cookiesession.SessionRepositoryResponseWrapper
  *   com.granicus.grails.plugins.cookiesession.CookieSessionRepository

## Configured Beans

  *   cookieSessionFilter - the plugin filter
  *   sessionRepository - an implementation of SessionRepository

## Relationship to grails-cookie-session plugin version 0.1.2 
This project started as a fix to the grails-cookie-session plugin (https://github.com/literalice/grails-cookie-session). 
However, in the end the project became a complete reimplementation. With that said, I would like to give
special recognition to Masatoshi Hayashi. This project would not have been possible (or at least would have 
taken much longer!) had it not been for his original work. Many thanks to Masatoshi Hayashi for giving me a place to start! 
After reviewing this implementation, Masatoshi has agreed to let this version supersede his version and thus, this 
project has taken over the 'cookie-session' name in grails the plugin repository.

### Why a major version number increment from 0.1.2 to 2.0.0? 
This is a major functionality upgrade. It was also originally called cookie-session-v2 
and was intended to stand on its own. But when the decision was made to allow it to supersede the origin implementation, 
the version was set to 2.0.0 to signify that its the second version of the plugin and upgrades the original version.

### Upgrading from cookie-session version 0.1.2
This plugin is a drop-in replacement for cookie-session 0.1.2. and will work without as well as the cookie-session 0.1.2.
However, in order to take advantage of the new features in version 2.0.0, the new configuration settings will need to be used.
Also, note that some configuration settings have been deprecated and are listed in the configuration settings table below. Please
remove the deprecated configuration settings from affected applications.

## How this plugin works

  This plugin consists of the following components:
  
  *   CookieSessionFilter - a servlet filter installed in the first position of the filter chain which is responsible for wrapping the request and response object with the SessionRepositoryRequestWrapper and SessionRepositoryResponseWrapper objects.
  *   SessionRepositoryRequestWrapper - an implementation of HttpServletRequestWrapper which delegates responsibility for retrieving session data from persistence storage to an instance of a SessionRepository object and for managing an instance of SerializableSession.
  *   SessionRespositoryResponseWrapper - an implementation of HttpServletResponseWrapper which delegates saving sessions to persistance storage to an instance of SessionRepository.
  *   SerializableSession - an implementation of HttpSession that can be serialized 
  *   SessionRepository - an interface that describes a class that can save and restore a session from a persistent location
  *   CookieSessionRepository - an implementation of SerializableSession that is responsible for the mechanics of storing session data in cookies and retrieve session data from cookies.

### Execution sequence outline

When a request is received by the server, the CookieSessionFilter is called in the filter chain and performs the following:

  1.    retrieves an instance of a SessionRepository bean from the application context 
  2.    creates an instance of the SessionRepositoryRequestWrapper, assigning it the SessionRepository instance and the current request
  3.    uses SessionRepositoryRequestWrapper instance to restore the session
  4.    uses the SessionRepositoryInstance to get the current session
  5.    creates an instance of the SessionRepositoryResponseWrapper, assigning it the current session, the SessionRespository instance and the current response object.
  6.    calls the next filter in the chain

Throughout the remainder of the request, the SessionRepositoryRequestWrapper is only responsible for returning the stored instances of the SerializableSession.

As the request processing comes to a conclussion the SessionRepositoryResponseWrapper is used to intercept calls that would cause the response to be committed (i.e. written back to the client). When it intercepts these calls, it uses a SessionRepository object to persist the Session object.

The CookieSession object is a spring bean that implements the SessionRepository interface. This object is injected injected into the application so that it can be replaced with alternative implementations that can store the session to different locations such as database, shared in-memory store, shared filesystem, etc.

## How to contribute
If you want to contribute a bug fix, please work from the 'develop' branch. Additionally, before submitting a pull request please confirm that all of the tests in test suite pass. The test suite is located at github.com/benlucchesi/test-cookie-session-plugin

