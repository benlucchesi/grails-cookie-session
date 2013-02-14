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
import com.granicus.grails.plugins.cookiesession.SessionPersistenceListener;
import com.granicus.grails.plugins.cookiesession.SerializableSession;
import org.springframework.security.web.context.SecurityContextRepository

import org.codehaus.groovy.grails.commons.ConfigurationHolder as ch

import groovy.util.logging.Log4j

@Log4j
public class SecurityContextSessionPersistenceListener implements SessionPersistenceListener, InitializingBean {

    String cookieName = "gsession"

    void afterPropertiesSet(){
      log.trace "afterPropertiesSet()"
      if( ch.config.grails.plugin.cookiesession.containsKey('cookiename') ){
        cookieName = ch.config.grails.plugin.cookiesession.cookiename
      }
    }

    public void afterSessionRestored( SerializableSession session ){
    }

    public void beforeSessionSaved( SerializableSession session ){
      log.trace "beforeSessionSaved()"

      if( session.SPRING_SECURITY_SAVED_REQUEST_KEY ){
        def sessionCookies = session.SPRING_SECURITY_SAVED_REQUEST_KEY.@cookies.findAll{ it.name =~ cookieName }
        sessionCookies.each{
          session.SPRING_SECURITY_SAVED_REQUEST_KEY.@cookies.remove(it)
        }
      }

      if( session.SPRING_SECURITY_CONTEXT != org.springframework.security.core.context.SecurityContextHolder.getContext() ){
        log.info "persisting security context to session"
        session.SPRING_SECURITY_CONTEXT = org.springframework.security.core.context.SecurityContextHolder.getContext()
      }
      else{
        log.trace "not persisting security context"
      }
    }
}
