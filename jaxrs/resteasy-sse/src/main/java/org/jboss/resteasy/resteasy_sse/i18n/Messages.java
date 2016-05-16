package org.jboss.resteasy.resteasy_sse.i18n;

import javax.ws.rs.core.MediaType;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;


/**
 * User: rsearls
 * Date: 5/5/16
 *
 * Copyright Aug 24, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages
{
   Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);
   int BASE = 20000;

   @Message(id = BASE + 00, value = "Input source is already closed")
   String inputSourceAlreadyClosed();

   @Message(id = BASE + 05, value = "MediaType can not be null.")
   String mediaTypeCannotBeNull();

   @Message(id = BASE + 10, value = "Class can not be null.")
   String classCannotBeNull();

   @Message(id = BASE + 15, value = "Data object can not be null.")
   String dataObjectCannotBeNull();

   @Message(id = BASE + 20, value = "GenericType can not be null.")
   String genericTypeCannotBeNull();

   @Message(id = BASE + 25, value = "A comment or data must be set")
   String commentOrDataMustBeSet();

   @Message(id = BASE + 30, value = "No suitable message body writer for class: {0}", format=Format.MESSAGE_FORMAT)
   String noSuitableMessageBodyWriter(String name);

   @Message(id = BASE + 35, value = "Web target is 'null'.")
   String webTargeIsNull();

   @Message(id = BASE + 40, value = "Event source already connected")
   String eventSourceAlreadyConnected();

   @Message(id = BASE + 45, value = "Event source already closed")
   String eventSourceAlreadyClosed();

   @Message(id = BASE + 50, value = "<Error reading data into a string>")
   String errorReadingDataIntoString();

   @Message(id = BASE + 55, value = "ClassCastException: attempting to cast {0} to {1}", format=Format.MESSAGE_FORMAT)
   String attemptToCastClassError(String classnameAsResource, String targetTypeURL);

   @Message(id = BASE + 60, value = "Thread priority ({0}) must be >= {1}", format= Message.Format.MESSAGE_FORMAT)
   String threadPriorityBelowMinimumError(int priority, int minPriority);

   @Message(id = BASE + 65, value = "Thread priority ({0}) must be <= {1}", format= Message.Format.MESSAGE_FORMAT)
   String threadPriorityAboveMaximumError(int priority, int maxPriority);

   @Message(id = BASE + 70, value = "UncaughtExceptionHandler can not be null")
   String uncaughtExceptionHandlerIsNull();

   @Message(id = BASE + 75, value = "ThreadFactory can not be null")
   String threadFactoryIsNull();

}
