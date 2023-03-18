package com.ibm.ace.http.error;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class CommonServerJavaCompute extends MbJavaComputeNode {

	PrintingThread pt = null;
	public void evaluate(MbMessageAssembly inAssembly) throws MbException
	{
		MbOutputTerminal out = getOutputTerminal("out");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
		      MbElement rootElem = outAssembly.getMessage().getRootElement();

		      MbElement jsonData = rootElem.createElementAsLastChild("JSON").
		        createElementAsFirstChild(MbElement.TYPE_NAME);
		      jsonData.setName("Data");
		      jsonData.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "flowName", getMessageFlow().getName());
		      if ( pt != null )
		    	  pt.messageCount++;
		      
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(), null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}

	public class PrintingThread extends Thread
	{
	 	String flowName;
	 	int messageCount = 0; // Could be made atomic if we really cared
	 	boolean keepRunning = true;
	   	public PrintingThread(String fn)
	   	{
	   		this.flowName = fn;
	   	}
	   	@Override
	   	public void run()
	   	{
			System.out.println("PrintingThread ["+flowName+"] starting");
			int previousMessageCount = 0;
	   		while ( keepRunning )
	   		{
	   			int messageCountCopy = messageCount; // Only copy this once to avoid threading issues
				int messagesInLastSecond = messageCountCopy - previousMessageCount;
				previousMessageCount = messageCountCopy;
	   			try {
	   				System.out.println("PrintingThread ["+flowName+"] messages in last second "+messagesInLastSecond);
	   				Thread.sleep(1000);
	   			} catch (Exception e) {
	   				e.printStackTrace();
	   			}
			}
			System.out.println("PrintingThread ["+flowName+"] stopping");
	   	}
	}

	/**
	 * onStart() is called as the message flow is started. The thread pool for
	 * the message flow is running when this method is invoked.
	 *
	 * @throws MbException
	 */
	@Override
	public void onStart() throws MbException {
		System.out.println("onStart() called for "+getMessageFlow().getName());

		pt = new PrintingThread(getMessageFlow().getName());
		pt.start(); 
	}

	/**
	 * onStop() is called as the message flow is stopped. 
	 *
	 * The onStop method is called twice as a message flow is stopped. Initially
	 * with a 'wait' value of false and subsequently with a 'wait' value of true.
	 * Blocking operations should be avoided during the initial call. All thread
	 * pools and external connections should be stopped by the completion of the
	 * second call.
	 *
	 * @throws MbException
	 */
	@Override
	public void onStop(boolean wait) throws MbException {
		System.out.println("onStop() called for "+getMessageFlow().getName());
		
		pt.keepRunning = false;
		
		if ( wait == false )
		{
			System.out.println("Sleeping for 10000ms to give clients a chance to get 404 status codes");
			try { Thread.sleep(10000); } 
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
}
