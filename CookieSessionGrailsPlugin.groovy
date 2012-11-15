import org.springframework.web.filter.DelegatingFilterProxy
import grails.util.Environment
import com.granicus.grails.plugins.cookiesession.CookieSessionFilter
import com.granicus.grails.plugins.cookiesession.CookieSessionRepository
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean

class CookieSessionGrailsPlugin {
    def version = "2.0.1"
    def grailsVersion = "1.2.4 > *"
    def title = "Cookie Session Plugin" // Headline display name of the plugin
    def author = "Ben Lucchesi"
    def authorEmail = "benlucchesi@gmail.com"
    def description = "The Cookie Session plugin enables grails applications to store session data in http cookies between requests instead of in memory on the server. This allows application deployments to be more stateless which supports simplified scaling architectures and fault tolerance." 
    def documentation = "http://github.com/benlucchesi/grails-cookie-session-v2"
    def license = "APACHE"

    // Online location of the plugin's browseable source code.
    def scm = [url: 'http://github.com/benlucchesi/grails-cookie-session-v2']

    def getWebXmlFilterOrder() {
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
