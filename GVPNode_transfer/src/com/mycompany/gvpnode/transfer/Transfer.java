package com.mycompany.gvpnode.transfer;

/** @copyright   2004-2013 mycompany */

import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.mycompany.ecs.cdr.CDRConstants;
import com.mycompany.gvpdriver.base.*;
import com.mycompany.gvpdriver.entity.*;
import com.mycompany.vxml.facade.*;

/** 
 * @file          Transfer.java
 * 
 * @description   This class is responsible for transferring a call. 
 * 
 * @author        Tatiana Stepourska
 * 
 * @version       1.1
 */

public class Transfer extends BaseNodeController implements ITransferConstants
{	
	private static final Logger logger 			= Logger.getLogger(Transfer.class);
	private static final long serialVersionUID 	= 2880683799526397478L;

	/**
	 * Override the abstract method specified in the base servlet to produce VXML response
	 * 
	 * @return Full VoiceXML document
	 */
	public String doResponse(String callID,							
			ICallInfo        	ci,
			VXML 				vxml,
			HttpSession         session,
			HttpServletRequest  request,
			HttpServletResponse response
	) throws ServletException,IOException, Exception 
	{	
		addMDC(callID,ci);
		String result        				= null;
		String submitNext 					= null;
		
		try {	
			submitNext = BaseGlobalConfig.submitbase + CLASS_TRANSFER_HANDLER; 
			logger.debug("submitNext: " + submitNext);
			
			// building VXML document
			result = buildDocument(vxml,
					ci,
					submitNext,
					request);		 	 		
		}
		catch(Exception e)	{
			logger.error("ERROR: " + e.getMessage());
		}
		finally {
			if(logger.isTraceEnabled()) logger.trace(result);
			removeMDC(ci);
		}
		return result;
	}

	public VXML addDocumentBody(VXML vxml, ICallInfo ci, NodeInfo ni, String submitTo) throws Exception 	{		 
		String ani = null;	
		String phoneNum = null;		
		//for GVP platform
		String destination = null;
		String connecttimeout 	= BaseGlobalConfig.connecttimeout; 
		String connectwhen    	= BaseGlobalConfig.connectwhen;
		String transferaudio	= BaseGlobalConfig.transferaudio;
		//String errorRetry     	= DEFAULT_ERROR_RETRY;
		String txtype		  	= VXMLValues.TRANSFER_TYPE_CONSULTATION;
		//String bridge		  	= VXMLValues.VAL_FALSE;
		//String analisys			= VXMLValues.VAL_FALSE;	
		String langMode			= null;
		//TODO - to remove or align with submitTo
		//TODo: busy, noanswer
		//String busyRetry      = DEFAULT_BUSY_RETRY;
		//String noanswerRetry  = DEFAULT_NOANSWER_RETRY;
		//FastMap<String, FastMap<String, FastMap<String, Audio>>> audioLangMap = null;
		FastMap<String, FastMap<String, FastList<Audio>>> audioLangMap = null;
		
		FastList<Audio> audios  = null;
		Timestamp tStamp = new Timestamp(System.currentTimeMillis());
		String bargein	 		= VXMLValues.VAL_FALSE;
		//String nodeID           = null;
		//String promptpath  		= null;
		Audio a 				= null;
		String langCode			= null;		
		/* Testing for busy or noanswer setting*/
		//FastMap<String, ExitOption> exitoptions	= null;				
		//String transferBusy		= null;
		//String transferNoAnswer	= null;
		long delay				= DEFAULT_TX_RETRY_DELAY;
		String platform			= CDRConstants.PLATFORM_GVP;
		
		//TODO  implement retry delay based on a node property from database
		
		try 	{
			ani 				= ci.getMessage(CLID_KEY);
			if(!BaseUtils.isValidPhone(ani))
				ani=""+ci.getCLID();
			logger.info("ani: " + ani);
			
			phoneNum 			= ni.getProperty(BaseConstants.TXPHONE_KEY);// ci.getTransferNumber();
			logger.info("phoneNum: " + phoneNum);
			
			platform            = ci.getPlatform();
			logger.info("platform: " + platform);
			
			if (platform.equalsIgnoreCase(CDRConstants.PLATFORM_GVP)){
				destination = "tel:"+ci.getTrunkGroup()+phoneNum;
				logger.info("transfer destination: " + destination);
			}					
			
			//set into CDR transfer number 
			ci.setTransferNumber(phoneNum);

				if(ni.getProperty(TRANSFER_TYPE_KEY)!=null)
					txtype = ni.getProperty(TRANSFER_TYPE_KEY);
				logger.info("txtype: " + txtype);
				
				if(ni.getProperty(VXMLProperties.ATTR_CONNECTTIMEOUT)!=null)
					connecttimeout = ni.getProperty(VXMLProperties.ATTR_CONNECTTIMEOUT);			

				if(ni.getProperty(VXMLProperties.ATTR_CONNECTWHEN)!=null)
					connectwhen    = ni.getProperty(VXMLProperties.ATTR_CONNECTWHEN);

			langCode 	= ci.getLangCode();		
			
			if(ni.getProperty(VXMLProperties.ATTR_TRANSFERAUDIO)!=null){
				transferaudio = ni.getProperty(VXMLProperties.ATTR_TRANSFERAUDIO);
			}
			
			//if(errorRetry==null)	
			//	errorRetry = DEFAULT_ERROR_RETRY;
			//if(busyRetry==null)	
			//	errorRetry = DEFAULT_BUSY_RETRY;
			//if(noanswerRetry==null)	
			//	errorRetry = DEFAULT_NOANSWER_RETRY;
			if(logger.isTraceEnabled()) {
				logger.trace("connecttimeout: " + connecttimeout);
				logger.trace("connectwhen: " + connectwhen);
			}			
		}
		catch(Exception e)	{
			logger.error("Error getting node properties: " + e.getMessage());
		}
		
		try {
			delay		= Long.parseLong(ni.getProperty(KEY_DELAY));
			logger.info("delay: "+delay);
		}
		catch(Exception e){
			
		}

		try {				
			audioLangMap = ni.getAudioLangMap();
			if(audioLangMap!=null&&audioLangMap.size()>0){		
				//audios = this.buildMainAudioList(audioLangMap,ci.getLangArray(), ci.isLangSelected(), langCode);		
				audios = this.buildAudioList(audioLangMap,ci.getLangArray(), ci.isLangSelected(), langCode, BaseConstants.PROMPT_TYPE_MAIN);						
			}
			ni.appendAudiosToHistory(audios);
		}
		catch(Exception e) {
			logger.error("error getting audios: " + e.getMessage());
		}	
		
		if(delay>0){
			try{
				Thread.sleep(delay);
			}
			catch(Exception e){
				
			}
			//set timestamp for CDR
			ci.setEndTimeBeforeXfer(System.currentTimeMillis());
		}
		try {
			//ci.addFeature(ni.getType());
			
			//capture successful result and close the history
			ni.appendHistory("'main"+CDRConstants.HISTORY_TOKEN_EVENT_BODY_START + "type"+ CDRConstants.HISTORY_TOKEN_KEYVALUE_DELIM+txtype+CDRConstants.HISTORY_TOKEN_EVENT_END+"'");
			
			ni.setExecutionResult(""+CDRConstants.TERM_TRANSFER_SUCCESS);
			
			//ni.appendHistory("result=["+CDRConstants.TERM_TRANSFER_SUCCESS+"]");
			//ci.appendCallHistory(ni.getHistory());
		}
		catch(Exception e){
			
		}


		/* transfer attributes
		    name="string"
		    expr="ECMAScript_Expression"
		    cond="ECMAScript_Expression"
		    dest="URI"
		    destexpr="ECMAScript_Expression"
		    bridge="boolean"
		    type="bridge" | "blind" | "consultation" | "local" | "network" | "unsupervised" | "supervised"
		    method="string"
		    connecttimeout="time_interval"
		    maxtime="time_interval"
		    transferaudio="URI"
		    analysis="boolean"
		    connectwhen="analysis" | "answered" | "immediate"
		    detectansweringmachine="boolean"
		    aai="string"
		    aaiexpr="ECMAScript_Expression"
		    signalvar="ECMAScript_Object"
		    consultexpr="ECMAScript_Expression"
		 */
		vxml.FormStart("main");
		vxml.VarJS("callVars", "new Object()" );
		vxml.Var  ("phoneNum", phoneNum);
		//vxml.Var  (TRANSFER_RESULT_KEY, "");
		//vxml.VarJS(BEFORE_TX_TIME_KEY , "");
		//vxml.VarJS(FILLED_TX_TIME_KEY , "");

		vxml.BlockStart("setCallVars");			
		//declare action start for the gvp logger
		//vxml.LogStart(null, VXML.LOG_LABEL_ACTION_START, null, null, null);//Log( label="com.genesyslab.var.ActionStart">action_1
		//vxml.LogEnd();
		//NOTE:: RLT transfers have to set the CLID, 
		//or "error.transfer.noroute" event will be thrown on 5.x.x VG platforms
		vxml.Assign("callVars.ani", "'" + ani + "'");
		
		//append each audio wrapped with vxml prompt tag
		if(audios!=null) {
		//append all available audios to vxml
		for (FastList.Node<Audio> n = audios.head(), end = audios.tail(); (n = n.getNext()) != end;) {
			try{
				a = n.getValue();
				if(a==null)
					continue;
			
				langMode = BaseGlobalConfig.langPropMap.get(BaseConstants.LANG_MODE__KEY + a.getLanguage());
				logger.debug("langMode: " +langMode);
				//appendPrompt(vxml, a, bargein, langMode);
				a.appendPrompt(vxml, bargein, langMode, null); //cond is null
			}
			catch(Exception e){
				logger.error("Error wrapping audio: " + e.getMessage());
			}
		}	//end of the loop		
		}	//end of audios != null	
		
		ni.appendAudiosToHistory(audios);

		vxml.Assign(CDRConstants.KEY_TERM, "'"+String.valueOf(CDRConstants.TERM_TRANSFER_SUCCESS)+"'");	
		//vxml.Assign  (BaseConstants.VAR_DOC_HISTORY    , "'main"+CDRConstants.HISTORY_TOKEN_EVENT_BODY_START + "type"+ CDRConstants.HISTORY_TOKEN_KEYVALUE_DELIM+txtype+CDRConstants.HISTORY_TOKEN_EVENT_END+"'");	
		//vxml.Assign(BEFORE_TX_TIME_KEY , "(new Date()).getTime()");
		vxml.BlockEnd();

		//transfer tag implementation is different for VG and GVP
		if (platform.equalsIgnoreCase(CDRConstants.PLATFORM_GVP)){
		vxml.TransferStart(
		    		null,						//	--aai="Your aai value"
		    		null,						//  --aaiexpr="Your ECMAScript expression"
		    		null, //bridge,						//	--bridge="true"|"false"
		    		null,						//	--cond="Your ECMAScript expression"
		    		connecttimeout,				//	--connecttimeout="Your time interval"
		    		destination,				//	--dest="Your URI" (Optional)                              
		    		null,						//	--destexpr="Your ECMAScript expression" (Optional)       
		    		null,						//	--expr="Your ECMAScript expression"
		    		null,						//	--maxtime="Your time interval"
		    		TRANS_CALL_KEY,				//	--name="Your name value" 
		    		transferaudio, //BaseGlobalConfig.transferaudio,	//	--transferaudio="Your URI"
		    		txtype,						//	--type="bridge"|"blind"|"consultation" (Optional)                              
		    		null, 						//  --gvp:analysis="true"|"false"
		    		null,						//	--gvp:authcode"Your string"
		    		connectwhen,				//	--gvp:connectwhen="answered"|"immediate"
		    		null,						//	--gvp:consultexpr="Your ECMAScript expression"
		    		null,						//	--gvp:disconnectonansweringmachine="true"|"false"
		    		null,						//	--gvp:method="HKF"|"REFER"|"BRIDGE"|"REFERJOIN"|"MEDIAREDIRECT"|"ATTCOURTESY"|"ATTCONSULT"|"ATTCONFERENCE"|"ATTOOBCOURTESY"|"ATTOOBCONSULT"|"ATTOOBCONFERENCE"
		    		null,						//	--gvp:private="true|false"
		    		"callVars", 				// 	--gvp:signalvar="Your ECMA object"
		    		null						//	--gvp:userdata="Your ECMAScript expression"
		);
		}
		else {
			connectwhen    = "immediate";
				vxml.TransferStart(
						TRANS_CALL_KEY, 
						null, //analisys,    //analysis 
						"false", //bridge, //bridge
						txtype , //type
						connecttimeout,  //connecttimeout
						connectwhen,     //connectwhen
						"'phone://'+ phoneNum", //destexpr, number to transfer
						BaseGlobalConfig.transferaudio,  //transferaudio
						"callVars"
				); 
		}


		vxml.FilledStart();//"transCall", "any");
		//vxml.Assign(FILLED_TX_TIME_KEY , "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, TRANS_CALL_KEY);
		/*vxml.LogStart();
		vxml.Text(tStamp + "::TRANSFER_RESULT::");
		vxml.Value(TRANSFER_RESULT_KEY);
		vxml.LogEnd();*/
		
		vxml.IfStart(TRANS_CALL_KEY + "=='"+VXMLValues.TX_BUSY+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_BUSY)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_BUSY);
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result busy, " + CDRConstants.TRANSFER_BUSY);
			}
			//Testing busy
			/*vxml.Assign("xferBusy", "'" + transferBusy + "'");
			vxml.IfStart("xferBusy=='" + VXMLValues.TX_BUSY + "'");
				vxml.Goto("#transferErrorBusy");
			vxml.Else();
				vxml.Goto("#transferError");
			vxml.IfEnd();*/
			vxml.Goto("#transferError");
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_NOANSWER + "'"); // || transCall=='no_answer'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NOANSWER)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NOANSWER);
			/*if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result noanswer: " + CDRConstants.TRANSFER_NOANSWER + ". ");
			}*/
			//Testing noanswer
			/*vxml.Assign("xferNOA", "'" + transferNoAnswer + "'");
			vxml.IfStart("xferNOA=='" + VXMLValues.TX_NOANSWER + "'");
				vxml.Goto("#transferErrorNoAnswer");
			vxml.Else();
				vxml.Goto("#transferError");
			vxml.IfEnd();*/
			vxml.Goto("#transferError");
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_NETWORK_BUSY + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NETWORK_BUSY)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result network busy, " + CDRConstants.TRANSFER_NETWORK_BUSY);
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NETWORK_BUSY);
			vxml.Goto("#transferError");
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_UNKNOWN + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TERM_UNKNOWN)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result unknown, " + CDRConstants.TERM_UNKNOWN);
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_UNKNOWN);	
			vxml.Goto("#transferError");	
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_INVALID_PHONE_NO + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_INVALID_PHONE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_INVALID_PHONE_NO);
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result invalid phone number, " + CDRConstants.TRANSFER_INVALID_PHONE);
			}
			vxml.Goto("#transferError");
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_FAR_END_MACHINE + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_VOICEMAIL)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result far end machine");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_FAR_END_MACHINE);
			//vxml.Goto("#transferError");
			vxml.Submit(submitTo, null, CDRConstants.KEY_TERM);
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_FAR_END_FAX + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_FAX)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result far end fax");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_FAR_END_FAX);
			vxml.Goto("#transferError");
		vxml.ElseIf(TRANS_CALL_KEY + "=='" + VXMLValues.TX_RESTRICTED_PHONE_NO + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_RESTRICTED_PHONE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::"  +VXMLValues.TX_RESTRICTED_PHONE_NO);
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result restricted phone number");
			}
			vxml.Goto("#transferError");		
		
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_nEAR_END_DISCONNECT + "'");
				vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NEAR_END_DISCONNECT)+"'");
				if(BaseGlobalConfig.debug) {
					vxml.Text("Transfer result near end disconnect with lower case");
				}
				vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_nEAR_END_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_NEAR_END_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NEAR_END_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result near end disconnect with caps");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NEAR_END_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_fAR_END_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_FAR_END_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result far end disconnect with lower case");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_fAR_END_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_FAR_END_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_FAR_END_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result far end disconnect with caps");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_FAR_END_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_nETWORK_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NETWORK_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result network disconnect with lower case");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_nETWORK_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_NETWORK_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NETWORK_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result network disconnect with caps");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NETWORK_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_mAXTIME_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_MAXTIME_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result max time disconnect with lower case");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_mAXTIME_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_MAXTIME_DISCONNECT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_MAXTIME_DISCONNECT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result max time disconnect with caps");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_MAXTIME_DISCONNECT);
				vxml.Disconnect();
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_rEJECTED + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_FAX)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result rejected with lower case");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_FAR_END_FAX);
				vxml.Goto("#transferError");
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_REJECTED + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_REJECTED)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result rejected with caps");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_rEJECTED);
				vxml.Goto("#transferError");
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_nOT_ALLOWED + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NOT_ALLOWED)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result not allowed with lower case");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_nOT_ALLOWED);
				vxml.Goto("#transferError");
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_NOT_ALLOWED + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NOT_ALLOWED)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result not allowed with caps");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NOT_ALLOWED);
				vxml.Goto("#transferError");
				
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_uNKNOWN + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TERM_UNKNOWN)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result unknown with lower case");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_uNKNOWN);
				vxml.Goto("#transferError");
		
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_MODEM + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_FAR_END_MODEM)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result modem");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_MODEM);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_FAR_END_MODEM + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_FAR_END_MODEM)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result far end modem");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_FAR_END_MODEM);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_NODIALTONE + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NODIALTONE)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result no dial tone");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NODIALTONE);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_NORINGBACK + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NORINGBACK)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result no ring back");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_NORINGBACK);
				vxml.Goto("#transferError");

			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_SIT_REORDER + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_SIT_REORDER)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result sit reorder");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_SIT_REORDER);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_SIT_VACANT_CIRCUIT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_SIT_VACANT_CIRCUIT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result sit vacant circuit");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_SIT_VACANT_CIRCUIT);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_SIT_NO_CIRCUIT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_SIT_NO_CIRCUIT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result no circuit");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_SIT_NO_CIRCUIT);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_SIT_INEFFECTIVE_OTHER + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_SIT_INEFFECTIVE_OTHER)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result ineffective other");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_SIT_INEFFECTIVE_OTHER);
				vxml.Goto("#transferError");
			
			vxml.ElseIf(TRANS_CALL_KEY + "=='"+VXMLValues.TX_SIT_OPERATOR_INTERCEPT + "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_SIT_OPERATOR_INTERCEPT)+"'");
			if(BaseGlobalConfig.debug) {
				vxml.Text("Transfer result operator intercept");
			}
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_SIT_OPERATOR_INTERCEPT);
				vxml.Goto("#transferError");

		////////////////////////////////////////////////////////////
		vxml.Else();
		if(BaseGlobalConfig.debug) {
			vxml.Text("Transfer result suppose to be success, submitting to handler");
		}
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TERM_TRANSFER_SUCCESS)+"'");
			//vxml.Assign(TRANSFER_RESULT_KEY, TRANSFER_SUCCESS);
			//vxml.Submit(submitTo, null, CDRConstants.KEY_TERM + " " + TRANSFER_RESULT_KEY + " " + BEFORE_TX_TIME_KEY + " " + FILLED_TX_TIME_KEY);	 			 
			vxml.Submit(submitTo, null, CDRConstants.KEY_TERM + " " + BaseConstants.VAR_DOC_HISTORY); // + " " + BEFORE_TX_TIME_KEY + " " + FILLED_TX_TIME_KEY);	 			 
			
		vxml.IfEnd();

		vxml.FilledEnd();


		vxml.CatchStart(VXMLValues.TX_ERR_CONNECTION_NOAUTHORIZATION);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NO_AUTHORIZATION)+"'");
			//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERR_CONNECTION_NOAUTHORIZATION+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_CONNECTION_NOAUTHORIZATION);
			if(BaseGlobalConfig.debug) {
				vxml.Text("Error connection no authorization");
			}			
			vxml.Goto("#transferError");
		vxml.CatchEnd();
		
		vxml.CatchStart(VXMLValues.TX_ERR_TELEPHONE_NOAUTHORIZATION);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERR_TELEPHONE_NOAUTHORIZATION+"'");
		vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NO_AUTHORIZATION)+"'");
		vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_TELEPHONE_NOAUTHORIZATION);
		if(BaseGlobalConfig.debug) {
		vxml.Text("Error telephone no authorization");
		}
		vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERR_TELEPHONE_BADDESTINATION);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
	//	vxml.Assign(TRANSFER_RESULT_KEY,"'"+ VXMLValues.TX_ERR_TELEPHONE_BADDESTINATION+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_BAD_DESTINATION)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_TELEPHONE_BADDESTINATION);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error telephone bad destination");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERR_CONNECTION_BADDESTINATION);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERR_CONNECTION_BADDESTINATION+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_BAD_DESTINATION)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_CONNECTION_BADDESTINATION);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection bad destination");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERR_TELEPHONE_NOROUTE);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERR_TELEPHONE_NOROUTE+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NO_ROUTE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_TELEPHONE_NOROUTE);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error telephone no route");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERR_CONNECTION_NOROUTE);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERR_CONNECTION_NOROUTE+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NO_ROUTE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_CONNECTION_NOROUTE);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection no route");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERR_TELEPHONE_NORESOURCE);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERR_TELEPHONE_NORESOURCE+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NO_RESOURCE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_TELEPHONE_NORESOURCE);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error telephone no resource");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();
		
		vxml.CatchStart(VXMLValues.TX_ERR_CONNECTION_NORESOURCE);
		//vxml.Assign(FILLED_TX_TIME_KEY , "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY,"'"+ VXMLValues.TX_ERR_CONNECTION_NORESOURCE+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NO_RESOURCE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERR_CONNECTION_NORESOURCE);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection no resource");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERROR_CONNECTION_NOLICENSE);
		//vxml.Assign(FILLED_TX_TIME_KEY , "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERROR_CONNECTION_NOLICENSE+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_NOLICENSE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERROR_CONNECTION_NOLICENSE);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection no license");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();
		
		vxml.CatchStart(VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_BLIND);
		//vxml.Assign(FILLED_TX_TIME_KEY , "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_BLIND+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_UNSUPPORTED_TRANSFER_BLIND)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_BLIND);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection unsupported blind transfer");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();
	
		vxml.CatchStart(VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_BRIDGE);
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_BRIDGE+"'");
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_UNSUPPORTED_TRANSFER_BRIDGE)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_BRIDGE);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection unsupported bridge transfer");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_CONSULTATION);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_CONSULTATION+"'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_UNSUPPORTED_TRANSFER_CONSULTATION)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERROR_UNSUPPORTED_TRANSFER_CONSULTATION);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection unsupported consultation transfer");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLValues.TX_ERROR_UNSUPPORTED_URI);
		//vxml.Assign(FILLED_TX_TIME_KEY , "(new Date()).getTime()");
		//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ VXMLValues.TX_ERROR_UNSUPPORTED_URI+ "'");
			vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_UNSUPPORTED_URI)+"'");
			vxml.Log(tStamp + "::TRANSFER_RESULT::" + VXMLValues.TX_ERROR_UNSUPPORTED_URI);
			if(BaseGlobalConfig.debug) {
			vxml.Text("Error connection unsupported u r i");
			}
			vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.CatchStart(VXMLEvents.EVENT_ERROR);
		//vxml.Assign(FILLED_TX_TIME_KEY, "(new Date()).getTime()");
				vxml.Assign(CDRConstants.KEY_TERM, "'"+ String.valueOf(CDRConstants.TRANSFER_ERROR)+"'");
				//vxml.Assign(TRANSFER_RESULT_KEY, "'"+ AppEvents.EVENT_ERROR + "'");
				vxml.Log(tStamp + "::TRANSFER_RESULT::transfer error");
				if(BaseGlobalConfig.debug) {
				vxml.Text("Error caught inside the transfer form");
				}
				vxml.Goto("#transferError");
		vxml.CatchEnd();

		vxml.TransferEnd();
		vxml.FormEnd();
		
		/*Start of Transfer error busy*/
		/*vxml.FormStart("transferErrorBusy");
		vxml.BlockStart();	
			vxml.Submit(submitBase +  CLASS_TRANSFER_BUSY, 
					null, 
					CDRConstants.KEY_TERM + " " + TRANS_CALL_KEY, 
					null, 
					null, 
					BaseGlobalConfig.fetchaudio, 
					null, 
					null, 
					null,
					"0"
			);	
		vxml.BlockEnd();
		vxml.FormEnd();*/
		/*End of Transfer error busy*/
		
		/*Start of Transfer error no answer*/
		/*
		vxml.FormStart("transferErrorNoAnswer");
		vxml.BlockStart();	
		vxml.Submit(submitBase +  CLASS_TRANSFER_NOANSWER, 
				null, 
				CDRConstants.KEY_TERM + " " + TRANS_CALL_KEY, 
				null, 
				null, 
				BaseGlobalConfig.fetchaudio, 
				null, 
				null, 
				null,
				"0"
		);		
		vxml.BlockEnd();
		vxml.FormEnd();*/
		/*End of Transfer error no answer*/

		vxml.FormStart("transferError");
		vxml.BlockStart();
			if(BaseGlobalConfig.debug) {
				//vxml.Text("Transfer result: ");// + CDRConstants.TRANSFER_BUSY);
				//vxml.Value(TRANSFER_RESULT_KEY);
				vxml.Text(". Term code: ");
				vxml.Value(CDRConstants.KEY_TERM);
			}
			//vxml.Submit(submitTo, null, CDRConstants.KEY_TERM + " " + TRANSFER_RESULT_KEY + " " + BEFORE_TX_TIME_KEY + " " + FILLED_TX_TIME_KEY);
			vxml.Submit(submitTo, null, CDRConstants.KEY_TERM + " " + BaseConstants.VAR_DOC_HISTORY); // + " " + BEFORE_TX_TIME_KEY + " " + FILLED_TX_TIME_KEY);
			
		vxml.BlockEnd();
		vxml.FormEnd();	    

		return vxml;
	}	 
/*	
	public final void addMDC(String callID, ICallInfo ci){
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
}//end of Transfer class