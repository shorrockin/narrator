package com.shorrockin.narrator

/**
 * this exception can be thrown from a an action along with an identifier
 * which is then used to capture metrics about this type of exception. By
 * using this class you will receive better error reports during the statistics
 * reporting.
 *
 * @author Chris Shorrock
 */
class NarratorStoryException(val id:String) extends Exception {
}