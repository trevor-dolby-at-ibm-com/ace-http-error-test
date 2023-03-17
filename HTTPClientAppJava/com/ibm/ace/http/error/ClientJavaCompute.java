package com.ibm.ace.http.error;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class ClientJavaCompute extends MbJavaComputeNode {

	PrintingThread pt = null;
	
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below

		      MbElement inputRoot = inAssembly.getMessage().getRootElement();
		      try
		      {
		    	  // Should probably use int
		    	  String statusCode = (String)(inputRoot.getFirstElementByPath("HTTPResponseHeader/X-Original-HTTP-Status-Code").getValueAsString());
		    	  //System.out.println("statusCode "+statusCode);
		    	  if ( statusCode.equals("200") )
			    	  pt.successCount++;
		    	  else if ( statusCode.equals("404") )
			    	  pt.notFoundCount++;
		    	  else
			    	  pt.errorCount++;
		      }
		      catch ( java.lang.Throwable jlt )
		      {
		    	  // Assume not an HTTP response, so must be a failure
		    	  pt.failureCount++;
		      }
		      
		      if ( ( System.getProperty("broker.stopping") != null ) && 
		    	   ( System.getProperty("broker.stopping").equals("true")) )
		      {
		    	  throw new MbUserException(this, "evaluate()", "", "", "", new String[] {"broker stopping"});
		      }
		    	  
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
	 	
	 	// Could be made atomic if we really cared
	 	int successCount = 0;
	 	int notFoundCount = 0;
	 	int errorCount = 0;
	 	int failureCount = 0;
	 
	 	boolean keepRunning = true;
	   	public PrintingThread(String fn)
	   	{
	   		this.flowName = fn;
	   	}
	   	@Override
	   	public void run()
	   	{
			System.out.println("PrintingThread ["+flowName+"] starting");
		 	int previousSuccessCount = 0;
		 	int previousNotFoundCount = 0;
		 	int previousErrorCount = 0;
		 	int previousFailureCount = 0;
		 	
	   		while ( keepRunning )
	   		{
	   		 	int successCountCopy = successCount;
	   		 	int notFoundCountCopy = notFoundCount;
	   		 	int errorCountCopy = errorCount;
	   		 	int failureCountCopy = failureCount;

				int successInLastSecond = successCountCopy - previousSuccessCount;
				previousSuccessCount = successCountCopy;
				int notFoundInLastSecond = notFoundCountCopy - previousNotFoundCount;
				previousNotFoundCount = notFoundCountCopy;
				int errorInLastSecond = errorCountCopy - previousErrorCount;
				previousErrorCount = errorCountCopy;
				int failureInLastSecond = failureCountCopy - previousFailureCount;
				previousFailureCount = failureCountCopy;
	   			try {
	   				System.out.println("PrintingThread ["+flowName+"] 200 "+successInLastSecond+" 404 "+notFoundInLastSecond+" error "+errorInLastSecond+" failure "+failureInLastSecond);
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
	}
}
