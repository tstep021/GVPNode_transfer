package com.mycompany.gvpnode.transfer;

/** @copyright   2010-2013 mycompany. */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import javolution.util.FastMap;
import org.apache.log4j.Logger;
//import org.apache.log4j.MDC;

import com.mycompany.ecs.cdr.CDRConstants;
import com.mycompany.gvpdriver.base.*;
import com.mycompany.gvpdriver.entity.*;
import com.mycompany.vxml.facade.*;
//import com.mycompany.gvpdriver.util.*;

/** 
 * @file         TransferBusy.java
 * 
 * @description  This class goes to the next node in the path which 
 * 				 could either play a message and hang up or transfer 
 * 				 to another number.
 *  
 * @author       Tatiana Stepourska
 * 
 * @version      1.0
 */

public class TransferBusy extends BaseNodeController implements ITransferConstants// extends BaseCallFlow
{	

	private static final long serialVersionUID = 635857832568507224L;
	private static final Logger logger = Logger.getLogger(TransferBusy.class);

	/**
	 * Implements abstract method specified in the base servlet to produce VXML response
	 * 
	 * 
	 * @return Full VoiceXML document
	 */
	public String doResponse(String 			callID,
			ICallInfo			ci, 
			VXML 				vxml,
			HttpSession 		session, 
			HttpServletRequest  request,
			HttpServletResponse response
	)	throws ServletException,IOException, Exception
	{
		 addMDC(callID,ci);
		logger.trace("started");
		String result 						= null;

		try {	
			result = buildDocument(vxml,
					ci,
					BaseGlobalConfig.submitbase + CLASS_TRANSFER_CHECKOUT, //+ "?" + TRANSFER_RESULT_KEY + "=" + AppValues.TX_BUSY,
					request);		

		}
		catch(Exception e){
			logger.error("Error displaying error document: " + e.getMessage());
			//logger.info("trying to create error exit document: ");
			//result = this.silentExit(vxml, callID); 
		}
		finally {
			if(logger.isTraceEnabled()) logger.trace(result);
			removeMDC(ci);
		}
		return result;
	}

	/** 
	 * Builds custom part of the VXML response document body.
	 *
	 * @param ci		-- BaseCallInfo object (for Driver it should be an instance of CallInfo)
	 * @return vxml     -- Part of VXML response
	 */
	public VXML addDocumentBody(VXML vxml, ICallInfo ci, NodeInfo nodeInfo, String submitNext) throws Exception
	{  	 	
	String[] arrLangCfg = null;
		
		String promptpath  = BaseGlobalConfig.resource_repository_url + "prompts/system/";
		logger.info("promptpath: " + promptpath);
		int len = BaseGlobalConfig.languages.length;
		String txt = null;
		boolean langSelected = false;
		String langCode = null;
		
		try {
			arrLangCfg = ci.getLangArray();			
			len = arrLangCfg.length;			
			if(len<=0)
				len = BaseGlobalConfig.languages.length;
		}
		catch(Exception e) {
			arrLangCfg = BaseGlobalConfig.languages;
			len = BaseGlobalConfig.languages.length;
			logger.error("error getting session language array, using default");
		}
		
		try {
			langCode = ci.getLangCode();
			langSelected = ci.isLangSelected();			
		}
		catch(Exception e){ langSelected=false; }
		
		if(langSelected){
			try {
				txt = BaseUtils.getStringFromFile(BaseGlobalConfig.resource_repository_basepath + promptpath + langCode + "/transferBusy.txt");
				if(txt==null)
					throw new Exception("text is null, setting to default");
			}
			catch(Exception e){
				txt = " ";
			}
			
			vxml.FormStart("mainBusy"+langCode, "dialog", "true"); //id, scope, cleardtmf
			vxml.BlockStart("playBusyMsg" + langCode);
			vxml.Assign  (BaseConstants.VAR_DOC_HISTORY    , "'main"+CDRConstants.HISTORY_TOKEN_EVENT_BODY_START + "type=transferBusy"+CDRConstants.HISTORY_TOKEN_EVENT_END+"'");				
			vxml.AudioStart(promptpath + langCode + "/transferBusy.wav");
				vxml.Text(txt);
			vxml.AudioEnd();							

			vxml.Submit(submitNext, null, CDRConstants.KEY_TERM);
			vxml.BlockEnd();
			
			//in any case goto call end, overrides the vxml header handling
			vxml.ErrorStart();
			vxml.Submit(submitNext, null, CDRConstants.KEY_TERM);
			vxml.ErrorEnd();
		
			vxml.CatchStart(VXMLEvents.EVENT_ERROR);
			vxml.Submit(submitNext, null, CDRConstants.KEY_TERM);
			vxml.CatchEnd();
			
			vxml.FormEnd();
		}
		else {
	
		for(int i=0;i<len;i++) {
			try {
				txt = BaseUtils.getStringFromFile(BaseGlobalConfig.resource_repository_basepath + promptpath + arrLangCfg[i] + "/transferBusy.txt");
				if(txt==null)
					throw new Exception("text is null, setting to default");
			}
			catch(Exception e){
				txt = " ";
			}
			
			vxml.FormStart("mainBusy"+arrLangCfg[i], "dialog", "true"); //id, scope, cleardtmf
				
			//vxml.Property(VXMLProperties.PROP_TTSENGINE, BaseGlobalConfig.langPropMap.get(BaseConstants.TTSENGINE_PREFIX_KEY + BaseGlobalConfig.languages[i]));// "speechify_jill");
				
			vxml.BlockStart("playBusyMsg" + arrLangCfg[i]);
			vxml.Assign  (BaseConstants.VAR_DOC_HISTORY    , "'main"+CDRConstants.HISTORY_TOKEN_EVENT_BODY_START + "type=busy"+CDRConstants.HISTORY_TOKEN_EVENT_END+"'");	
			
			vxml.AudioStart(promptpath + arrLangCfg[i] + "/transferBusy.wav");
				vxml.Text(txt);
				//vxml.Text(Utils.getStringFromFile(BaseGlobalConfig.resource_repository_basepath + promptpath + arrLangCfg[i] + "/transferBusy.txt"));
			vxml.AudioEnd();
			
			//vxml.Text("Sorry, the line is busy.");

			if(i>=len-1)
				vxml.Submit(submitNext, null, CDRConstants.KEY_TERM);
			else
				vxml.Goto("#mainBusy"+arrLangCfg[i+1]);
			vxml.BlockEnd();
			
			vxml.FormEnd();
		}
		//in any case goto call end, overrides the vxml header handling
		vxml.ErrorStart();
		vxml.Submit(BaseGlobalConfig.submitbase + BaseCallStates.CLASS_TECH_DIFF, null, CDRConstants.KEY_TERM);
		vxml.ErrorEnd();
	
		vxml.CatchStart(VXMLEvents.EVENT_ERROR);
		vxml.Submit(BaseGlobalConfig.submitbase + BaseCallStates.CLASS_TECH_DIFF, null, CDRConstants.KEY_TERM);
		vxml.CatchEnd();
		} //end of else not selected language
	
		// all error handling is in the vxml header/footer of BaseCallFlow
		// to override, add error/catch blocks here

		return vxml;
	}
	
/*	public final void addMDC(String callID, ICallInfo ci){
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
}  // end of TechDiff