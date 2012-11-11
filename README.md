# Cookie Session Grails Plugin

Current Version: 2.0.0

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

## Important Features Supported by Cookie Session V2 (not supported by the original cookie-session)

+ compatible with flash scope

+ compatible with webflow
  
+ supports secure sessions
 
+ supports sessions larger than 4kb

# Installation

grails install-plugin cookie-session

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
      <td>The secret key used to encrypt session data. If not set, a random key will be created at runtime. If multiple instances of the application are deployed, this parameter must be set manually so the application can decrypt data produced by any of the instances.</td>
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
    grails.plugin.cookiesession.secret = "This is my secret. There are many like it, but this one is mine.".bytes
    grails.plugin.cookiesession.cookiecount = 10
    grails.plugin.cookiesession.maxcookiesize = 2048  // 2kb
    grails.plugin.cookiesession.sessiontimeout = 3600 // one hour
    grails.plugin.cookiesession.cookiename = 'gsession'

## Understanding cookiecount and maxcookiesize
The maximum session size stored by this plugin is calculated by (cookiecount * maxcookiesize). The reason for these two parameters is that through experimentation, some browsers didn't reliably set large cookies set before the subsequent request. To solve this issue, this plugin supports configuring the max size of each cookie stored and the number of cookies to span the session over. The default values are conservative. If sessions in your application meet or exceed the max session size as configured, first increase the cookiecount and then the maxcookiesize parameters.

## Enabling large session
To enable large sessions, you'll need to increase the max http header size of the tomcat http connector. In tomcat this can be configured in the server.xml with the maxHttpHeaderSize parameter. Set the parameter to something large, such as 262144 ( 256kb ). 

When developing in grails, you can configure the embedded tomcat instance with the tomcat startup event:

1.  create the file scripts/_Events.groovy in your project directory
2.  add the following code:

        eventConfigureTomcat = {tomcat ->
          tomcat.connector.setAttribute("maxHttpHeaderSize",262144)
        }

If you're using a container other than tomcat, refer to the documentation to figure out how to configure the maximum http header size.

These configuration changes are needed because by default the max http header in tomcat is 8kb which is far to small
for storing a serialized session.

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
remove the deprecated configuration settings from your application.

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

## automated tests
automated tests for this plugin are located at:
github.com/benlucchesi/test-cookie-session-plugin
