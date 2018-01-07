package com.mycompany.gvpnode.transfer;

/** @copyright   2009-2013 mycompany */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
//import org.apache.log4j.MDC;

//import com.mycompany.ecs.cdr.CDRConstants;
import com.mycompany.gvpdriver.base.*;
import com.mycompany.gvpdriver.entity.*;
import com.mycompany.vxml.facade.*;

/** 
 * @file         CheckIn.java
 * 
 * @description  
 * 
 * @author       Tatiana Stepourska
 * 
 * @version      1.1
 */
public class CheckIn extends BaseNodeController implements ITransferConstants
{
	private static final Logger logger = Logger.getLogger(CheckIn.class);
	//private static final Logger historyLogger = Logger.getLogger("CallHistory");
	private static final long serialVersionUID = 8780760485907455281L;

	/**
	 * Generates our servlet's response.  Should be overridden by each servlet.
	 *
	 * @return String      The VXML response
	 */
	 public String doResponse(String callID,							
							ICallInfo         	ci,
							VXML 				vxml,
							HttpSession         session,
							HttpServletRequest  request,
							HttpServletResponse response
							) throws ServletException,IOException, Exception 
	{		
		 addMDC(callID,ci);
		//logger.info("started");
		String result = null; 

		try {
			//nodeID = ni.getId();
			ci.getCurrentNode().startHistory(ci); 
			//ci.addFeature(ni.getType());
		}
		catch(Exception e){
			logger.error("Error getting node ID: " + e.getMessage());
		}

		try	{			
			//logger.info("started node: " + ci.getCurrentNode());
			//historyLogger.info("started node: " + ci.getCurrentNode());
			result = buildDocument(vxml,
					ci,
					BaseGlobalConfig.submitbase + CLASS_TRANSFER,
					request);
			if(logger.isTraceEnabled()) logger.trace(result);
		}
		catch(Exception e)	{
			logger.error("ERROR : " + e.getMessage());
		}
		finally {
			if(logger.isTraceEnabled()) logger.trace(result);
			removeMDC(ci);
		}
		return result;
	}
	
	public VXML addDocumentBody(VXML vxml, ICallInfo ci, NodeInfo ni, String submitNext) throws Exception
	{

		vxml.FormStart("main");			
		vxml.BlockStart("pass");

		if(BaseGlobalConfig.loadtest){			
			try {
				String[] arr = BaseUtils.getWavesForCharacters("789");//nodeID);
				if(arr!=null){
				vxml.PromptStart(VXMLValues.VAL_FALSE);
				vxml.AudioStart(BaseGlobalConfig.resource_repository_url+"prompts/system/silence1s.wav");
				vxml.Text("	");
				vxml.AudioEnd();
				for(int i=0;i<arr.length;i++){			
					vxml.AudioStart(BaseGlobalConfig.resource_repository_url+"prompts/system/dtmf_"+arr[i]+".wav");
					vxml.Text(arr[i]);
					vxml.AudioEnd();
				}
				vxml.PromptEnd();
				}
			}
			catch(Exception e) {
				
			}		
		}
			vxml.Submit(submitNext, //next,
						null, 		//expr
						null);  	//namelist 

		vxml.BlockEnd();	
		
		//all error handling is in the vxml header/footer of BaseCallFlow
		//to override, add error/catch blocks here
		
	vxml.FormEnd();	
	
	return vxml;
	}
}