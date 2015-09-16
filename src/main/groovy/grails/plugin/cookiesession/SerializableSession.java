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
 *  benlucchesi@gmail.com
 */

package grails.plugin.cookiesession;

import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializableSession implements HttpSession, Serializable {

      final static Logger log = LoggerFactory.getLogger(SerializableSession.class.getName());

      // borrowed from other cookie session plugin to fake a session context
      @SuppressWarnings("deprecation") /* ServletAPI */
      transient private final javax.servlet.http.HttpSessionContext SESSION_CONTEXT = new javax.servlet.http.HttpSessionContext() {
        public HttpSession getSession(String sessionId) {
          return null;
        }
        public Enumeration<String> getIds() {
          return SESSION_CONTEXT_ID_ENUM;
        }
      };

      transient private final Enumeration<String> SESSION_CONTEXT_ID_ENUM = new Enumeration<String>() {
        public String nextElement() {
          return null;
        }
        public boolean hasMoreElements() {
            return false;
        }
      };

      public final long serialVersionUID = 42L;
      private long creationTime = 0;
      private long lastAccessedTime = 0;
      private HashMap<String,Serializable> attributes;

      private Boolean isValid = true; 
      transient private ServletContext servletContext;
      transient private boolean newSession;
      transient private int maxInactiveInterval;

      public SerializableSession(){
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.attributes = new HashMap<String,Serializable>();
      }

      public long getCreationTime(){
        return ( creationTime );
      }

      public String getId(){
        return ( "simplesession" );
      }

      public void setLastAccessedTime(long lastAccessedTime){
        this.lastAccessedTime = lastAccessedTime;
      }

      public long getLastAccessedTime(){
        return ( lastAccessedTime );
      }

      public ServletContext getServletContext(){
        return ( servletContext );
      }

      public void setServletContext(ServletContext servletContext){
        this.servletContext = servletContext;
      }

      public void setMaxInactiveInterval(int interval){
        maxInactiveInterval = interval;
      }

      public int getMaxInactiveInterval(){
        return ( maxInactiveInterval );
      }

      public HttpSessionContext getSessionContext(){
        return SESSION_CONTEXT; 
      }

      public Object getAttribute(String name){
        return ( attributes.get(name) );
      }

      public Object getValue(String name){
        return getAttribute(name);
      }

      public Enumeration getAttributeNames(){
        final Iterator<String> keys = attributes.keySet().iterator();
        final Enumeration names = new Enumeration(){
          public boolean hasMoreElements(){ return keys.hasNext();  }
          public Object nextElement(){ return keys.next(); }
        };

        return ( names );
      }

      public String[] getValueNames(){
        return ( attributes.keySet().toArray( new String[0] ) );
      }

      public void setAttribute(String name, Object value){

        attributes.put(name,(Serializable)value);
        if( value != null && value instanceof HttpSessionBindingListener ){
          try{
            ((HttpSessionBindingListener)value).valueBound(new HttpSessionBindingEvent(this,name));
          }
          catch( Exception excp ){
            log.error("failed to set attribute: " + name, excp);
          }
        }
      }

      public void putValue(String name, Object value){
        this.setAttribute(name,value);
      }

      public void removeAttribute(String name){
        Object value = attributes.remove(name);
        if( value != null && value instanceof HttpSessionBindingListener ){
          try{
            ((HttpSessionBindingListener)value).valueUnbound( new HttpSessionBindingEvent(this,name));
          }
          catch( Exception excp ){
            log.error("failed to remove attribute: " + name, excp);
          }
        }
      }

      public void removeValue(String name){
        this.removeAttribute(name);
      }

      public void invalidate(){
        isValid = false;
      }

      protected void setIsNewSession( boolean isNewSession ){
        this.newSession = isNewSession;
      }
      public boolean isNew(){
        return ( this.newSession );
      }
}
