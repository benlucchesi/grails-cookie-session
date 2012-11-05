# Cookie Session V2 Grails Plugin

The Cookie Session V2 plugin allows grails applications to store session data in cookies. By storing session data
in cookies, a complete record of a client's state is transmitted with each request from client to server and reconstituted
on the server. There are many benifits to this approach to managing clients' sessions.


* Simplified Scaling

      Because a client's session is passed with every request, you no longer need be concerned with complicated inefficient
      scaling strategies that compensate for the physical location of where the session is stored. Simply add application 
      instances and route requests to them. No more session replication or sticky sessions needed. In addition, because
      the session data is stored by the client, the server doesn't waste memory or disk space for sessions that are open
      for long periods of time.

* Fault Tolerance
    
      By default, grails sessions are stored in server-side memory. If you application crashes or becomes inaccessible,
      clients' session are typically lost or becomes out of sync between application instances. This can result in unexpected
      logouts, redirects, and loss of data, i.e. poor user experience. By using cookie sessions the application is much 
      less brittle. In a single instance deployment scenario, the server or application can be recycled 
      and clients can continue working with their sessions fully intact. In a multiple instance deployment scenario,
      any instance of the applicatin can service the next request.


## Relationship to grails-cookie-session plugin
This project started as a fix to the grails-cookie-session plugin. However after
sorting through architectural issues and attempting to fix the code so that it supported my use-cases, the
project became a complete rewrite. With that said, this project would not have been possible (or at least would have
taken much longer!) had it not been for the original work. Many thanks to Masatoshi Hayashi
for giving me a place to start.

## Why Cookie Session V2?
The original cookie-session plugin is functional, but does not support the full breadth of use-cases found in modern grails applications. The following use-cases are not possible with the cookie-session plugin, but are fully supported by cookie-session-v2:

+ Flash scope
  
  The Cookie-session plugin disregarded the flash scope between requests. Cookie-session-v2 preserves the flash scope between requests
  so that flash scoped variables are available on subsequent requests.

+ webflow
  
  The cookie-session plugin is not compatible with webflow. States were lost between requests. Cookie-session-v2 fully supports
  webflow state transitions and database access between requests to multiple application instances.

+ Secure session

  The cookie-session plugin compresses and signs sessions which prevents tampering, but the data can still be inspected 
  and ready by clients. Cookie-session-v2 encrypts the session data which prevents both tampering and inspection. 
 
+ Sessions larger than 4kb
  
  The cookie-session plugin not support session larger than 4kb, compressed, i.e. the max size of a cookie. Cookie-session-v2
  can store compressed sessions that approach the limit of the max-header size supported by the application server.


# Installation

grails install-plugin cookie-session-v2

# Issues

  none reported

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
      <td>The cryptographic algorithm used to encrypt session data (i.e. Blowfish, DES, DESEde, AES). NOTE: your secret must be compatible with the crypto algorithm.</td>
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
      <td>The length of time a session can be inactive for expressed in seconds. Zero indicates that a session will be active for as long as the browser is open.</td>
    </tr>
    <tr>
      <td>grails.plugin.cookiesession.cookiename</td>
      <td>gsession-X</td>
      <td>X number of cookies will be written per the cookiecount parameter. Each cookie is suffixed with the integer index of the cookie.</td>
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

## Enabling large session
To enable large sessions, you'll need to increase the max http header size of the tomcat connector. In tomcat, this can be configured in the server.xml with the maxHttpHeaderSize. Set this value to something large such as 262144 (i.e. 256kb). 

When developing in grails, you can configure the embedded tomcat instance with the tomcat startup event.

1.  create the file scripts/_Events.groovy in your project directory
2.  add the following code:

        eventConfigureTomcat = {tomcat ->
          tomcat.connector.setAttribute("maxHttpHeaderSize",262144)
        }

If you're using a container other than tomcat, refer product documentation to figure out what the maximum http header size is and how to increase it.

## Enabling webflow hibernate session
In order for webflows attached to hibernate sessions to be correctly deserialized, some additional configuration is needed. The following instructions show
how to explicitly name the hibernate session factory.  

1.  create the hibernate.cfg.xml file: grails create-hibernate-cfg-xml
2.  edit the grails-app/conf/hibernate/hibernate.cfg.xml file and the hibernate.session_factory_name property under the session-factory element

        <hibernate-configuration>
          <session-factory>
            <property name="hibernate.session_factory_name">session_factory</property>
          </session-factory>
        </hibernate-configuration>

## Logging

The following log4j keys are configurable:

  *   com.granicus.grails.plugins.cookiesession.CookieSessionFilter
  *   com.granicus.grails.plugins.cookiesession.SessionRepositoryRequestWrapper
  *   com.granicus.grails.plugins.cookiesession.SessionRepositoryResponseWrapper
  *   com.granicus.grails.plugins.cookiesession.CookieSessionRepository

## Configured Beans

  *   cookieSessionFilter - the plugin filter
  *   sessionRepository - an implementation of SessionRepository

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
