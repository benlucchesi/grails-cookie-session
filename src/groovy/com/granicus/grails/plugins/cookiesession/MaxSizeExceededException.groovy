package com.granicus.grails.plugins.cookiesession

class MaxSizeExceededException extends Exception {
    MaxSizeExceededException(String message) {
        super(message)
    }
}
