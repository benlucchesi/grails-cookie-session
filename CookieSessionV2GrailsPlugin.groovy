import org.springframework.web.filter.DelegatingFilterProxy
import grails.util.Environment
import com.granicus.grails.plugins.cookiesession.CookieSessionFilter
import com.granicus.grails.plugins.cookiesession.CookieSessionRepository
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean

class CookieSessionV2GrailsPlugin {
    def version = "2.0.0"
    def grailsVersion = "2.1.0 > *"
    def title = "Cookie Session V2 Plugin" // Headline display name of the plugin
    def author = "Ben Lucchesi"
    def authorEmail = "benlucchesi@gmail.com"
    def description = "The Cookie Session V2 plugin stores the session data in cookies to support the development of stateless web applications. This implementation works with flash scope and webflow."
    def documentation = "http://github.com/benlucchesi/grails-cookie-session-v2"
    def license = "APACHE"

    // Online location of the plugin's browseable source code.
    def scm = [url: 'https://github.com/benlucchesi/grails-cookie-session-v2']

    def getWebXmlFilterOrder() {
        // make sure the filter is first
        [cookieSessionFilter: -100]
    }

    def doWithWebDescriptor = { xml ->

        if ( !application.config.grails.plugin.cookiesession.enabled ) {
            return
        }

        // add the filter after the last context-param
        def contextParam = xml.'context-param'

        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('cookieSessionFilter')
                'filter-class'(DelegatingFilterProxy.name)
            }
        }

        def filter = xml.'filter'
        filter[filter.size() - 1] + {
            'filter-mapping' {
                'filter-name'('cookieSessionFilter')
                'url-pattern'('/*')
            }
        }
    }

    def doWithSpring = {

        if ( !application.config.grails.plugin.cookiesession.enabled ) {
            return
        }

        sessionRepository(CookieSessionRepository){
          grailsApplication = ref("grailsApplication")
        }

        cookieSessionFilter(CookieSessionFilter) {
            sessionRepository = ref("sessionRepository")
        }

    }

}
