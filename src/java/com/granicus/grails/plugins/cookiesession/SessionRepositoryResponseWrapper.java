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

import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;

import java.util.Map;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class SessionRepositoryResponseWrapper extends HttpServletResponseWrapper {
       
    static final Logger log = Logger.getLogger(SessionRepositoryResponseWrapper.class.getName());

    private String sessionId = "simplesession";
    private SessionRepository sessionRepository;
    private SessionRepositoryRequestWrapper request;
    private boolean sessionSaved = false;
    private boolean enforceSession = false;
    private ArrayList<SessionPersistenceListener> sessionPersistenceListeners;

    public SessionRepositoryResponseWrapper( HttpServletResponse response, 
                                              SessionRepository sessionRepository,
                                              SessionRepositoryRequestWrapper request,
                                              boolean enforceSession ) {
      super(response);
      this.sessionRepository = sessionRepository;
      this.request = request;
      this.enforceSession = enforceSession;
    }

    public void setSessionPersistenceListeners(ArrayList<SessionPersistenceListener> value){
      if( log.isTraceEnabled() ){ log.trace("setSessionPersistenceListeners()"); }
      this.sessionPersistenceListeners = value;
    }

    public void saveSession(){

      if( log.isTraceEnabled() ){ log.trace("saveSession()"); }

      if( this.isCommitted() ){
        if( log.isTraceEnabled() ){ log.trace("response is already committed, not attempting to save."); }
        return; 
      }

      if( sessionSaved == true ){
        if( log.isTraceEnabled() ){ log.trace("session is already saved, not attempting to save again."); }
        return;
      }

      SerializableSession session = (SerializableSession) request.getSession(this.enforceSession);

      if( session == null ){
        if( log.isTraceEnabled() ){ log.trace("session is null, not saving."); }
        return;
      }

      // flag the session as saved.
      sessionSaved = true;

      if( log.isTraceEnabled() ){ log.trace("calling session repository to save session."); }

      // call sessionPersistenceListeners
      if( sessionPersistenceListeners != null ){
        for( SessionPersistenceListener listener : sessionPersistenceListeners ){
          try{
            listener.beforeSessionSaved(session);
          }
          catch( Exception excp ){
            log.error("Error calling SessionPersistenceListener.beforeSessionSaved(): " + excp.toString());
          }
        }
      }
 
      sessionRepository.saveSession(session,this);
    }

    @Override
    public void setStatus(int sc){
      if( log.isTraceEnabled() ){ log.trace("intercepting setStatus(" + sc + ") to save session"); }
      this.saveSession();
      super.setStatus(sc);
    }

    @Override
    public void setStatus(int sc, String sm){
      if( log.isTraceEnabled() ){ log.trace("intercepting setStatus(" + sc + ", " + sm + ") to save session"); }
      this.saveSession();
      super.setStatus(sc, sm);
    }

    @Override
    public void flushBuffer() throws java.io.IOException{
      if( log.isTraceEnabled() ){ log.trace("intercepting flushBuffer to save session"); }
      this.saveSession();
      super.flushBuffer();
    }

    @Override
    public java.io.PrintWriter getWriter() throws java.io.IOException{
      if( log.isTraceEnabled() ){ log.trace("intercepting getWriter to save session"); }
      this.saveSession();
      return ( super.getWriter() );
    }

    @Override
    public ServletOutputStream getOutputStream() throws java.io.IOException{
      if( log.isTraceEnabled() ){ log.trace("intercepting getOutputStream to save session"); }
      this.saveSession();
      return ( super.getOutputStream() );
    }

    @Override
    public void sendRedirect(String location) throws java.io.IOException{
      if( log.isTraceEnabled() ){ log.trace("intercepting sendRedirect(" + location + ") to save session"); }
      this.saveSession();
      super.sendRedirect(location);
    }

    @Override
    public void sendError(int sc) throws java.io.IOException{
      if( log.isTraceEnabled() ){ log.trace("intercepting sendError(" + sc + ") to save session"); }
      this.saveSession();
      super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws java.io.IOException{
      if( log.isTraceEnabled() ){ log.trace("intercepting sendError(" + sc + ", " + msg + ") to save session"); }
      this.saveSession();
      super.sendError(sc,msg);
    }
}
