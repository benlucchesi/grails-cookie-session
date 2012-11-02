package com.granicus.grails.plugins.cookiesession;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class CookieSessionFilter extends OncePerRequestFilter {

    final static Logger log = Logger.getLogger(CookieSessionFilter.class.getName());
    String sessionId = "gsession";

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
      requestWrapper.restoreSession();

      SerializableSession session = (SerializableSession)requestWrapper.getSession();

      SessionRepositoryResponseWrapper responseWrapper = new SessionRepositoryResponseWrapper( response, sessionRepository, session );
      chain.doFilter(requestWrapper, responseWrapper);
    }
}
