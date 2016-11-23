package grails.plugin.cookiesession

class MaxSizeExceededException extends Exception {
    MaxSizeExceededException(String message) {
        super(message)
    }
}
