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

import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class SessionRepositoryRequestWrapper extends HttpServletRequestWrapper {

      final static Logger log = Logger.getLogger(SessionRepositoryRequestWrapper.class.getName());

      private SerializableSession session;
      private SessionRepository sessionRepository;

      public SessionRepositoryRequestWrapper( HttpServletRequest request, SessionRepository sessionRepository ){
        super( request );
        this.sessionRepository = sessionRepository;
      }

      public void restoreSession() {
        if( log.isTraceEnabled() ){ log.trace("restoreSession()"); }

        // use the sessionRepository to attempt to retrieve session object
        // if a session was restored
        // - set isNew == false
        // - assign the servlet context
        
        session = sessionRepository.restoreSession( this );
      }
      
      @Override
      public HttpSession getSession(boolean create){

        // if there isn't an existing session object and create == true, then
        // - create a new session object
        // - set isNew == true
        // - assign the servlet context
        // else
        // - return the session field regardless if what it contains

        if( session == null && create == true ){
          session = new SerializableSession();
          session.setIsNewSession( true );
          session.setServletContext( this.getServletContext() );
        }
       
        return ( session );
      }

      @Override
      public HttpSession getSession(){
        if( log.isTraceEnabled() ){ log.trace("getSession()"); }

        return( this.getSession(true) ); 
      }

      @Override
      public boolean isRequestedSessionIdValid(){
        if( log.isTraceEnabled() ){ log.trace("isRequestedSessionIdValid()"); }
        
        // session repository is responsible for determining if the requested session id is valid.
        return sessionRepository.isSessionIdValid( this.getRequestedSessionId() );
      }
    }
