package org.jboss.resteasy.resteasy_sse.i18n;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;


/**
 * User: rsearls
 * Date: 5/5/16
 *
 * Copyright Aug 24, 2015
 */
@MessageLogger(projectCode = "RESTEASY")
public interface LogMessages extends BasicLogger
{
   LogMessages LOGGER = Logger.getMessageLogger(LogMessages.class, LogMessages.class.getPackage().getName());
   int BASE = 19500;

   //////////////////////// ERROR /////////////////////////////////////
   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 0, value = "Unable to connect - closing the event source to {0}.", format= Message.Format.MESSAGE_FORMAT)
   void unableToConnectClosingSourceError(String uri, @Cause Throwable cause);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 5, value = "Event notification in a listener of {0} class failed.", format= Message.Format.MESSAGE_FORMAT)
   void eventNotificationInListenerFailedError(String name, @Cause Throwable cause);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 10, value = "Waiting for opening the event source connection has been interrupted.", format= Message.Format.MESSAGE_FORMAT)
   void eventSourceOpenConnectionInterruptedError(@Cause Throwable cause);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 15, value = "Error closing input's underlying response input stream.", format= Message.Format.MESSAGE_FORMAT)
   void closingResponseInputStreamError(@Cause Throwable cause);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 20, value = "Attempt to shutdown the event source executor for [{0}] has timed out.", format= Message.Format.MESSAGE_FORMAT)
   void eventSourceTimeoutError(String targetUri);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 25, value = "Waiting for the event source executor for [{0}] to shutdown has been interrupted.", format= Message.Format.MESSAGE_FORMAT)
   void eventShutdownInterruptedError(String targetUri);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 30, value = "Thread priority ({0}) must be >= {1}", format= Message.Format.MESSAGE_FORMAT)
   void threadPriorityBelowMinimumError(int priority, int minPriority);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 35, value = "Thread priority ({0}) must be <= {1}", format= Message.Format.MESSAGE_FORMAT)
   void threadPriorityAboveMaximumError(int priority, int maxPriority);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 40, value = "Closing eventInput issue.", format= Message.Format.MESSAGE_FORMAT)
   void closingEventInputError(@Cause Throwable cause);

   @LogMessage(level = Logger.Level.ERROR)
   @Message(id = BASE + 45, value = "inEvent parse error: {0}.", format= Message.Format.MESSAGE_FORMAT)
   void inEventRetryParseError(String s, @Cause Throwable cause);

   //////////////////////// WARN /////////////////////////////////////
   @LogMessage(level = Logger.Level.WARN)
   @Message(id = BASE + 100, value = "InEvent, {0} , not recognized: {1}", format= Message.Format.MESSAGE_FORMAT)
   void inEventFieldNotRecognizedWarning(String name, String v);


   //////////////////////// INFO /////////////////////////////////////
   /**
   @LogMessage(level = Logger.Level.INFO)
   @Message(id = BASE + 200, value = "text goes here")
   void textGoesHere();
   **/
   //////////////////////// DEBUG /////////////////////////////////////
   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 300, value = "Shutting down event processing.")
   void shuttingDownEventProcessingDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 305, value = "Listener task started.")
   void listenerTaskStartedDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 310, value = "Connecting...")
   void connectingDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 315, value = "Connected!")
   void connectedDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 320, value = "Connection lost - scheduling reconnect in {0} ms", format= Message.Format.MESSAGE_FORMAT)
   void connectionLostSchedulingReconnectDebug(long reconnectDelay);

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 325, value = "Received HTTP 503")
   void retreivedHTTP503Debug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 330, value = "Recovering from HTTP 503 using HTTP Retry-After header value as a reconnect delay")
   void recoveringFromHTTP503Debug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 335, value = "Recovering from HTTP 503 - scheduling to reconnect in {0} ms", format= Message.Format.MESSAGE_FORMAT)
   void recoveringFromHTTP503SchedulingReconnecDebug(long delay);

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 340, value = "Listener task finished.")
   void listenerTaksFinishedDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 345, value = "New event received.")
   void newEventReceivedDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 350, value = "Aborting reconnect of event source in {0} state", format= Message.Format.MESSAGE_FORMAT)
   void abortingReconnectDebug(String state);

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 355, value = "Awaiting first contact signal.")
   void waitingFirstContactSignalDebug();

   @LogMessage(level = Logger.Level.DEBUG)
   @Message(id = BASE + 360, value = "First contact signal released.")
   void firstContactSignalReleasedDebug();

   //////////////////////// TRACE /////////////////////////////////////
   /**
   @LogMessage(level = Logger.Level.TRACE)
   @Message(id = BASE + 400, value = "text goes here")
   void textGoesHere();
   **/
}
