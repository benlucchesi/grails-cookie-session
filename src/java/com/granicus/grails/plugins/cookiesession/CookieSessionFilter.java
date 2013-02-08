package com.granicus.grails.plugins.cookiesession;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

import java.util.Map;
import java.util.ArrayList;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.BeansException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class CookieSessionFilter extends OncePerRequestFilter implements InitializingBean, ApplicationContextAware {

    final static Logger log = Logger.getLogger(CookieSessionFilter.class.getName());
    String sessionId = "gsession";

    ApplicationContext applicationContext;
    ArrayList<SessionPersistenceListener> sessionPersistenceListeners;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        if( log.isTraceEnabled() ){ log.trace("setApplicationContext()"); }
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws ServletException
    {
      super.afterPropertiesSet();

      if( log.isTraceEnabled() ){ log.trace("afterPropertiesSet()"); }

      sessionPersistenceListeners = new ArrayList<SessionPersistenceListener>();
      // scan the application context for SessionPersistenceListeners 
      Map beans = applicationContext.getBeansOfType(SessionPersistenceListener.class);
      for( Object beanName : beans.keySet().toArray() ){
        sessionPersistenceListeners.add((SessionPersistenceListener)beans.get(beanName));
        if( log.isTraceEnabled() ){ log.trace("added listener: " + beanName.toString()); }
      }
    }

    // dependency injected
    private SessionRepository sessionRepository;
    public void setSessionRepository(SessionRepository repository){
      sessionRepository = repository;
    }
    public SessionRepository getSessionRepository(){
      return ( sessionRepository ); 
    }

    @Override
    protected void initFilterBean() {
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain chain) throws IOException, ServletException {
 
      if( log.isTraceEnabled() ){ log.trace("doFilterInteral()"); }
  
      SessionRepositoryRequestWrapper requestWrapper = new SessionRepositoryRequestWrapper( request, sessionRepository );
      requestWrapper.setServletContext( this.getServletContext() );
      requestWrapper.setSessionPersistenceListeners(this.sessionPersistenceListeners);
      requestWrapper.restoreSession();

      SerializableSession session = (SerializableSession)requestWrapper.getSession();

      SessionRepositoryResponseWrapper responseWrapper = new SessionRepositoryResponseWrapper( response, sessionRepository, session );
      responseWrapper.setSessionPersistenceListeners(this.sessionPersistenceListeners);
      chain.doFilter(requestWrapper, responseWrapper);
    }
}
