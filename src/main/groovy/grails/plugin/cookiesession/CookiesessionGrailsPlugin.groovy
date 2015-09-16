package grails.plugin.cookiesession

import grails.plugins.*
import org.springframework.boot.context.embedded.FilterRegistrationBean
import org.springframework.core.Ordered
import org.springframework.web.filter.DelegatingFilterProxy

class CookiesessionGrailsPlugin extends Plugin {

    def version = "3.0"

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.0 > *"

    def loadAfter = ['controllers']

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Cookie Session Plugin" // Headline display name of the plugin
    def author = "Ben Lucchesi"
    def authorEmail = "benlucchesi@gmail.com"
    def description = '''\
The Cookie Session plugin enables grails applications to store session data in http cookies between requests instead of in memory on the server. This allows application deployments to be more stateless which supports simplified scaling architectures and fault tolerance." 
    def documentation = "http://github.com/benlucchesi/grails-cookie-session-v2
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/cookiesession"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Accuracy Software, LTD", url: "http://www.benlucchesi.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->

        if ( !config.grails.plugin.cookiesession.enabled ) {
            return
        }

        sessionRepository(CookieSessionRepository){ bean ->
          bean.autowire = 'byName'
        }

        if( config.grails.plugin.cookiesession.containsKey("condenseexceptions") && 
            config.grails.plugin.cookiesession["condenseexceptions"] == true ) 
          exceptionCondenser(ExceptionCondenser)

        // ALWAYS CONFIGURED!
        javaSessionSerializer(JavaSessionSerializer){ bean ->
          bean.autowire = 'byName'
        }

        if( config.grails.plugin.cookiesession.containsKey("serializer") && config.grails.plugin.cookiesession["serializer"] == "kryo" )
          kryoSessionSerializer(KryoSessionSerializer){ bean ->
            bean.autowire = 'byName'
          }

        if( config.grails.plugin.cookiesession.containsKey("springsecuritycompatibility") &&  config.grails.plugin.cookiesession["springsecuritycompatibility"] == true )
          securityContextSessionPersistenceListener(SecurityContextSessionPersistenceListener){ bean ->
            bean.autowire = 'byName'
          }

        cookieSessionFilter(FilterRegistrationBean){
          filter = bean(CookieSessionFilter){
            sessionRepository = ref("sessionRepository") 
          }
          order = Ordered.HIGHEST_PRECEDENCE + 25
        }

      }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
