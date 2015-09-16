import grails.plugins.metadata.GrailsPlugin
import org.grails.gsp.compiler.transform.LineNumber
import org.grails.gsp.GroovyPage
import org.grails.web.taglib.*
import org.grails.taglib.GrailsTagException
import org.springframework.web.util.*
import grails.util.GrailsUtil

class gsp_cookiesessionerror_gsp extends GroovyPage {
public String getGroovyPageFileName() { "/WEB-INF/grails-app/views/error.gsp" }
public Object run() {
Writer out = getOut()
Writer expressionOut = getExpressionOut()
registerSitemeshPreprocessMode()
printHtmlPart(0)
createTagBody(1, {->
printHtmlPart(1)
createTagBody(2, {->
createTagBody(3, {->
if((grails.util.Environment.current.name == 'development') && true) {
printHtmlPart(2)
}
else {
printHtmlPart(3)
}
})
invokeTag('captureTitle','sitemesh',4,[:],3)
})
invokeTag('wrapTitleTag','sitemesh',4,[:],2)
printHtmlPart(1)
invokeTag('captureMeta','sitemesh',5,['gsp_sm_xmlClosingForEmptyTag':(""),'name':("layout"),'content':("main")],-1)
printHtmlPart(1)
if((grails.util.Environment.current.name == 'development') && true) {
invokeTag('stylesheet','asset',6,['src':("errors.css")],-1)
}
printHtmlPart(4)
})
invokeTag('captureHead','sitemesh',7,[:],1)
printHtmlPart(4)
createTagBody(1, {->
printHtmlPart(1)
if((grails.util.Environment.current.name == 'development') && true) {
printHtmlPart(5)
if(true && (Throwable.isInstance(exception))) {
printHtmlPart(6)
invokeTag('renderException','g',11,['exception':(exception)],-1)
printHtmlPart(5)
}
else if(true && (request.getAttribute('javax.servlet.error.exception'))) {
printHtmlPart(6)
invokeTag('renderException','g',14,['exception':(request.getAttribute('javax.servlet.error.exception'))],-1)
printHtmlPart(5)
}
else {
printHtmlPart(7)
expressionOut.print(exception)
printHtmlPart(8)
expressionOut.print(message)
printHtmlPart(9)
expressionOut.print(path)
printHtmlPart(10)
}
printHtmlPart(1)
}
else {
printHtmlPart(11)
}
printHtmlPart(4)
})
invokeTag('captureBody','sitemesh',30,[:],1)
printHtmlPart(12)
}
public static final Map JSP_TAGS = new HashMap()
protected void init() {
	this.jspTags = JSP_TAGS
}
public static final String CONTENT_TYPE = 'text/html;charset=UTF-8'
public static final long LAST_MODIFIED = 1442437134000L
public static final String EXPRESSION_CODEC = 'html'
public static final String STATIC_CODEC = 'none'
public static final String OUT_CODEC = 'none'
public static final String TAGLIB_CODEC = 'none'
}
