package com.shorrockin.narrator.utils

import org.apache.commons.logging.LogFactory

/**
 * simple logging trait to access the commons logging trait.
 *
 * @author Chris Shorrock
 */
trait Logging {
  @transient @volatile lazy val logger = LogFactory.getLog(this.getClass.getName)
}
