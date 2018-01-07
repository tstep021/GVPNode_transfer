package com.mycompany.gvpnode.transfer;

/** @copyright   2010-2013 mycompany  */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

//import javolution.util.FastMap;

import org.apache.log4j.Logger;
//import org.apache.log4j.MDC;

import com.mycompany.ecs.cdr.CDRConstants;
import com.mycompany.ecs.cdr.CDRUtils;
import com.mycompany.gvpdriver.base.*;
import com.mycompany.gvpdriver.entity.*;
import com.mycompany.vxml.facade.*;

/** 
 * File       TransferHandler.java
 * 
 * Description  
 * 
 * @author       Tatiana Stepourska
 * 
 * @version      1.0
 */
public class TransferHandler  extends BaseNodeController implements ITransferConstants 
{
	private static final long serialVersionUID = 5558745207905330103L;
	private static final Logger logger = Logger.getLogger(TransferHandler.class);
	
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
		logger.info("started");
			
		String result      = null;	
		String txResult    = null;
		String term        = null; 		
		int iTerm          = CDRConstants.TERM_CALLER_HANGUP;
		String submitBase  = BaseGlobalConfig.submitbase;
		String target      = submitBase + CLASS_TRANSFER_CHECKOUT;
		ExitOption opt 	   = null;
		NodeInfo node 	   = null;
		//int busyTries	   = 0;
		//int noanswerTries  = 0;
		//long retryDelay    = 0;
		//String beforeTxTime = null;
		//String filledTxTime = System.currentTimeMillis();
		//long before = 0;
		long after = 0;
		
		//get retry options, if any
		try {
			node = ci.getCurrentNode();
			
			//busyTries	   = 0;
			//noanswerTries  = 0;
			//retryDelay    = 0;
		}
		catch(Exception e){
			
		}
		
		try 	{
			after = System.currentTimeMillis();
			ci.setParam1(""+CDRUtils.calculateDuration(ci.getCallLengthBeforeXfer(),after));
		}
		catch(Exception e)	{
			logger.error("Error: " + e.getMessage());
		}
		
		try 	{
			term        = request.getParameter(CDRConstants.KEY_TERM);
			logger.info("term: " + term);
		
			//txResult = request.getParameter(TRANSFER_RESULT_KEY);
			//logger.info("txResult: " + txResult);		
		}
		catch(Exception e)	{
			logger.error("Error: " + e.getMessage());
		}
		
/*		try 	{
			beforeTxTime = request.getParameter(BEFORE_TX_TIME_KEY);
			logger.info("beforeTxTime: " + beforeTxTime);	
			
			before = Long.parseLong(beforeTxTime);
			ci.setCallLengthBeforeXfer(before);
		}
		catch(Exception e)	{
			logger.error("Error: " + e.getMessage());
		}*/
		
	/*	try 	{
			//filledTxTime = request.getParameter(FILLED_TX_TIME_KEY);
			//logger.info("filledTxTime: " + filledTxTime);
			
			after = System.currentTimeMillis();
			ci.setParam1(""+after);
			
			ci.setParam1(String.valueOf(CDRUtils.calculateDuration(after, before)));
		}
		catch(Exception e)	{
			logger.error("Error: " + e.getMessage());
		}
		*/
	
		try	{
			iTerm = Integer.parseInt(term);
			ci.setTermCode(iTerm);	
			
			switch(iTerm)	{		
				case CDRConstants.TRANSFER_NOANSWER:
					logger.info("case TRANSFER_NOANSWER");	
					txResult = NOANSWER;
					//target = submitBase + CLASS_TRANSFER_NOANSWER;	
					break;
				case CDRConstants.TRANSFER_BUSY:
					logger.info("case TRANSFER_BUSY");
					txResult = BUSY;
					//target = submitBase + CLASS_TRANSFER_BUSY;
					break;
				case CDRConstants.TRANSFER_NETWORK_BUSY:
					logger.info("case ERROR_TRANSFER_NETWORK_BUSY");
					txResult = BUSY;
					//target = submitBase + CLASS_TRANSFER_BUSY;	
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_NO_ROUTE:
					logger.info("case TRANSFER_NO_ROUTE");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_NO_RESOURCE:
					logger.info("case TRANSFER_NO_RESOURCE");	
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_ERROR:
					logger.info("case TRANSFER_ERROR");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TERM_ERROR:
					logger.info("case ERROR");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_BAD_DESTINATION:
					logger.info("case ERROR_TRANSFER_BAD_DESTINATION");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;	
					break;
				case CDRConstants.TRANSFER_INVALID_PHONE:
					logger.info("case ERROR_TRANSFER_INVALID_PHONE");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_NO_AUTHORIZATION:
					logger.info("case ERROR_TRANSFER_NO_AUTHORIZATION");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_FAX:
					logger.info("case ERROR_TRANSFER_FAX");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_RESTRICTED_PHONE:
					logger.info("case ERROR_TRANSFER_RESTRICTED_PHONE");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;	
				case CDRConstants.TRANSFER_MAXTIME_DISCONNECT:
					logger.info("case TRANSFER_MAXTIME_DISCONNECT");
					//target = submitBase + CLASS_TRANSFER_CHECKOUT;
					txResult = TRANSFERERROR; // TRANSFER_SUCCESS;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_FAR_END_DISCONNECT:
					logger.info("case TRANSFER_FAR_END_DISCONNECT");
					//target = submitBase + CLASS_TRANSFER_CHECKOUT;
					break;
				case CDRConstants.TRANSFER_FAR_END_MODEM:
					logger.info("case TRANSFER_FAR_MODEM");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_NEAR_END_DISCONNECT:
					logger.info("case TRANSFER_NEAR_END_DISCONNECT");
					//target = submitBase + CLASS_TRANSFER_CHECKOUT;
					break;
				case CDRConstants.TRANSFER_NETWORK_DISCONNECT:
					logger.info("case TRANSFER_NETWORK_DISCONNECT");
					//txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_CHECKOUT;
					break;
				case CDRConstants.TRANSFER_NODIALTONE:
					logger.info("case TRANSFER_NODIALTONE");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_NOLICENSE:
					logger.info("case TRANSFER_NOLICENSE");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_NORINGBACK:
					logger.info("case TRANSFER_NORINGBACK");
					//target = submitBase + CLASS_TRANSFER_ERROR;
					txResult = TRANSFERERROR;
					break;
				case CDRConstants.TRANSFER_NOT_ALLOWED:
					logger.info("case TRANSFER_NOT_ALLOWED");
					//target = submitBase + CLASS_TRANSFER_ERROR;
					txResult = TRANSFERERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_REJECTED:
					logger.info("case TRANSFER_REJECTED");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_SIT_INEFFECTIVE_OTHER:
					logger.info("case TRANSFER_SIT_INEFFECTIVE_OTHER");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_SIT_NO_CIRCUIT:
					logger.info("case TRANSFER_SIT_NO_CIRCUIT");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					break;
				case CDRConstants.TRANSFER_SIT_OPERATOR_INTERCEPT:
					logger.info("case TRANSFER_SIT_OPERATOR_INTERCEPT");
					//target = submitBase + CLASS_TRANSFER_ERROR;
					txResult = TRANSFERERROR;
					break;
				case CDRConstants.TRANSFER_SIT_REORDER:
					logger.info("case TRANSFER_SIT_REORDER");
					//target = submitBase + CLASS_TRANSFER_ERROR;
					txResult = TRANSFERERROR;
					break;
				case CDRConstants.TRANSFER_SIT_VACANT_CIRCUIT:
					logger.info("case TRANSFER_SIT_VACANT_CIRCUIT");
					//target = submitBase + CLASS_TRANSFER_ERROR;
					txResult = TRANSFERERROR;
					break;
				case CDRConstants.TRANSFER_UNSUPPORTED_TRANSFER_BLIND:
					logger.info("case TRANSFER_UNSUPPORTED_TRANSFER_BLIND");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_UNSUPPORTED_TRANSFER_CONSULTATION:
					logger.info("case TRANSFER_UNSUPPORTED_TRANSFER_CONSULTATION");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_UNSUPPORTED_TRANSFER_BRIDGE:
					logger.info("case TRANSFER_UNSUPPORTED_TRANSFER_BRIDGE");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
				case CDRConstants.TRANSFER_UNSUPPORTED_URI:
					logger.info("case TRANSFER_UNSUPPORTED_URI");
					txResult = TRANSFERERROR;
					//target = submitBase + CLASS_TRANSFER_ERROR;
					//ci.removeFeature(node.getType());
					break;
					/*
				case CDRConstants.TRANSFER_VOICEMAIL:
					logger.info("case TRANSFER_VOICEMAIL");
					target = submitBase + CLASS_TRANSFER_ERROR;
					break;*/
				default:
					logger.info("case default");
					//success, voicemail and all disconnect events
					txResult = TRANSFER_SUCCESS;
			}			

			//no exit options found for txResult condition
			try {
				node.setExecutionResult(""+iTerm);

				if(txResult==null) {
					target = submitBase + CLASS_TRANSFER_CHECKOUT;
				}
				else {
					//look for corresponding exit option
					node = ci.getCurrentNode();				
					node.setResultKey(txResult);
					
					if(node!=null){
						FastMap<String, ExitOption> exitOptions = node.getExitOptions();
						if(exitOptions!=null)
							opt = exitOptions.get(txResult);	
						
						//if there are no options for condition
						//redirect to engine default handling 
						if(opt==null){		
							//no exit option, redirect to corresponding system message
							//to be played before hangup
							if(txResult.equalsIgnoreCase(BUSY))
								target = submitBase + CLASS_TRANSFER_BUSY;
							else if(txResult.equalsIgnoreCase(NOANSWER))
								target = submitBase + CLASS_TRANSFER_NOANSWER;
							else if(txResult.equalsIgnoreCase(TRANSFERERROR))
								target = submitBase + CLASS_TRANSFER_ERROR;
							else
								target = submitBase + CLASS_TRANSFER_CHECKOUT;
						}
					}
				}
				
			}
			catch(Exception e){
				logger.error("ERROR processign tx result: " + e.getMessage());
			}
			/*
			else {
				String id = opt.getExitValue();
				logger.info("next node ID: " + id);
				if(id==null){
					//no id for next node:
					node.setResult(null);
				}
				else{
					node.setResult(opt);
					target = submitBase + CLASS_TRANSFER_CHECKOUT;
				}
			}*/
			//}
			//catch(Exception ex){
				
			//}
		}
		catch(Exception e)	{
			logger.error("ERROR switching term code: " + e.getMessage());
			/*
			StackTraceElement[] trace = e.getStackTrace();			  
			  if(trace!=null)	  {
				  for(int i=0;i<trace.length;i++)	  {
					  logger.error(trace[i]);
				  }
			  }*/
			 // result = errorDoc(vxml, si.getSubmitOnEnd(), submitBase, null, this.getClass().getName());
		}
			
		try {
		logger.info("target: " + target);
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
}  //end of class