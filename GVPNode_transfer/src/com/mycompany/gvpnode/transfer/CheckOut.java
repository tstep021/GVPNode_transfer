package com.mycompany.gvpnode.transfer;

/** @copyright   2010-2013 mycompany  */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import javolution.util.FastMap;

import org.apache.log4j.Logger;
//import org.apache.log4j.MDC;

import com.mycompany.gvpdriver.base.*;
import com.mycompany.gvpdriver.entity.*;
import com.mycompany.vxml.facade.*;

/** 
 * File         CheckOut.java
 * 
 * Description  
 * 
 * @author       Tatiana Stepourska
 * 
 * @version      1.0
 */
public class CheckOut  extends BaseNodeController implements ITransferConstants 
{
	private static final long serialVersionUID = 5558745207905330103L;
	private static final Logger logger = Logger.getLogger(CheckOut.class);
	
	/**
	 * Implements method in superclass
	 * 
	 * @param cdr		--ApplicationCDRr object
	 * @param si       --SessionInfo object
	 * @param vxml       --VXML object
	 * @param session	--HttpSession object
	 * @param request    --HTTPSession object
	 * @param request    --HTTPServletRequest object
	 * 
	 * @return           --The well-formatted VXML document
	 */
	 public String doResponse(String callID,							
				ICallInfo         ci,
				VXML 				vxml,
				HttpSession         session,
				HttpServletRequest  request,
				HttpServletResponse response
				) throws ServletException,IOException, Exception 
	{
		 addMDC(callID,ci);
		logger.trace("started");
		
		/**
		 * Result Exit option, if any, must be set before call comes here
		 * 
		 * Changed - 20130610: 
		 * node execution result and result key are set by TransferHandler
		 * exit option search and selection left to the driver
		 */
			
		String result      = null;		
		String target      = BaseCallStates.checkin;
	
		try{
			//get document history
			String docHistory = request.getParameter(BaseConstants.VAR_DOC_HISTORY);
			logger.debug("docHistory: " + docHistory);
			if(docHistory!=null){
				ci.getCurrentNode().appendHistory(docHistory);
				//ci.appendCallHistory("~");
			}
			
			String event = request.getParameter(BaseConstants.EVENT_KEY);
			logger.info("event: " + event);
			
			if(event!=null&&event.trim().length()>0){
				//set event
				ci.setEvent(event);
				ci.getCurrentNode().setExecutionResult(event);
				ci.getCurrentNode().setResultKey(BaseConstants.EXIT_OPTION_TOKEN + event + BaseConstants.EXIT_OPTION_TOKEN);
			}
			else {
				ci.setEvent(null);
			}
			
			ci.getCurrentNode().endHistory(ci);
		}
		catch(Exception e){
			logger.error("Error ending history: " + e.getMessage());
		}
		
		try {
		result = buildDocument(vxml,
				ci,
				target,
				request);
		}
		catch(Exception e){
			
		}		
			finally {
				if(logger.isTraceEnabled()) logger.trace(result);
				removeMDC(ci);
			}
		return result;
	}	

	 public VXML addDocumentBody(VXML vxml, ICallInfo ci, NodeInfo fi, String submitNext) throws Exception
	{
			vxml.FormStart("main", "dialog", "true"); //id, scope, cleardtmf
				vxml.BlockStart("justGo");
					vxml.Submit(submitNext, null, null);
				vxml.BlockEnd();
				
				//all error handling is in the vxml header/footer of BaseCallFlow
				//to override, add error/catch blocks here
				
			vxml.FormEnd();
						
		return vxml;
	}
	 
/*		public final void addMDC(String callID, ICallInfo ci){
			try {
				MDC.put(CDRConstants.CALL_ID_KEY, callID);
			}
			catch(Exception e){}
			
			try {
				String[] keys = ci.getMDCKeys();
				String[] values = ci.getMDCValues();
				for(int i=0;i<keys.length;i++){
					MDC.put(keys[i],values[i]);
				}
			} catch (Exception e2) {
			}
		}
		
		public final void removeMDC(ICallInfo ci){
			try {
				MDC.remove(CDRConstants.CALL_ID_KEY);
			}
			catch(Exception e){}
			
			try {
				String[] keys = ci.getMDCKeys();
				for(int i=0;i<keys.length;i++){
					MDC.remove(keys[i]);
				}
			} catch (Exception e2) {}
		}*/
}  //end of class