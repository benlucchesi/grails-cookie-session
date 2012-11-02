# Cookie Session V2 Grails Plugin

The Cookie Session V2 plugin allows grails applications to store session data in cookies. By storing session data
in cookies, a complete record of a client's state is transmitted with each request from client to server.
This allows the application to be completely stateless and thus support a drastically simplified scaling architectures and
robust fault tolerance.

## Simplified Scaling
After installing the cookie session v2 plugin, application scaled by starting new application
instances and routing requests to the application. No clustering configuration or session replication needed.

## Fault Tolerance
In the event an application instance or its server becomes unavailable, other application instances
are able to service the requests. If only a single instance of an application is deployed, it can be brought
down an up again without loosing clients sessions because sessions are restored on each request. A side affect of this
architecture is that applications can be upgraded without dropping clients sessions. 

## Relationship to grails-cookie-session plugin
This project started as a fix to the grails-cookie-session plugin. However after
sorting through architectural issues and attempting to fix the code so that it supported my use-cases, the
project because a complete rewrite. With that said, this project would not have been possible (or at least would have
taken much longer!) had it not been for the original work. Many thanks to the author of the grails-cookie-session plugin
for giving me a place to start.

## Supported Use-cases
the driving motivation for this plugin, was to use cookie-based session with full support for the following
  <ul>
    <li>flash scope</li>
    <li>webflow</li>
    <li>secure session</li>
    <li>sessions larger than 8kb</li>
  </ul>

## Secure Sessions
this plugin can be configured to encrypt the serialized session stored in the cookie. This feature prevents
clients from accessing or tampering with potentially sensitive data being stored in the session.

# Installation

grails install-plugin cookie-session-v2

# Issues

# Configuration

## Parameters

## Example
