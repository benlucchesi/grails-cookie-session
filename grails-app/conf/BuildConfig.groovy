grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        mavenCentral()
    }
    dependencies {
      //compile 'com.esotericsoftware.kryo:kryo:2.20' 
      //compile 'org.objenesis:objenesis:1.3'
      //NOTE: don't include the above libraries. kryo-serializers has all needed dependencies
      compile 'de.javakaffee:kryo-serializers:0.23'
    }

    plugins {
    
        //compile ':spring-security-core:1.2.7.3'

        build(":release:2.0.4") {
            export = false
        }

        compile(":webxml:1.4.1")
    }
}
