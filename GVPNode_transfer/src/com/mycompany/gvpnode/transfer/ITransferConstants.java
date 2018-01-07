package com.mycompany.gvpnode.transfer;

/** @copyright   2004-2012 mycompany */

/** 
 * @file         ITransferConstants.java
 * 
 * @description  
 * 
 * @author       Tatiana Stepourska
 * 
 * @version      1.1
 */

public interface ITransferConstants
{
	/* ====== STATIC KEYS ===================== */
	public static final String CLASS_TRANSFER_CHECKOUT     = "com.mycompany.gvpnode.transfer.CheckOut";
	public static final String CLASS_TRANSFER    			= "com.mycompany.gvpnode.transfer.Transfer";
	public static final String CLASS_TRANSFER_HANDLER		= "com.mycompany.gvpnode.transfer.TransferHandler";
	public static final String CLASS_TRANSFER_BUSY	 		= "com.mycompany.gvpnode.transfer.TransferBusy";
	public static final String CLASS_TRANSFER_NOANSWER	 	= "com.mycompany.gvpnode.transfer.TransferNoAnswer";
	public static final String CLASS_TRANSFER_ERROR	 		= "com.mycompany.gvpnode.transfer.TransferError";	
	
		
	public static final String CLID_KEY            			= "clid";
	public static final String TRANS_CALL_KEY				= "transCall";
	public static final String TRANSFER_TYPE_KEY            = "txtype";
	
	public static final String TRANSFER_SUCCESS				= "success";
	public static final String BEFORE_TX_TIME_KEY			= "beforeTxTime";
	public static final String FILLED_TX_TIME_KEY			= "filledTime";

	public static final String NOANSWER						= "noanswer";
	public static final String BUSY							= "busy";
	public static final String TRANSFERERROR				= "transfererror";
	public static final String KEY_DELAY					= "delay";
	
	/** Default number of retries if busy */
	public static final String DEFAULT_BUSY_RETRY              	= "0";
	/** Default clid value if not available */
	public static final String DEFAULT_CLID                    	= "0000000000";
	/** Default number of retries if error */
	public static final String DEFAULT_ERROR_RETRY             	= "0";	
	/** Default number of retries if no answer */
	public static final String DEFAULT_NOANSWER_RETRY          	= "0";
	/** Default connection timeout */
	public static final String DEFAULT_CONNECTTIMEOUT          	= "15s";//"30s";
	/** Default connect when */
	public static final String DEFAULT_CONNECTWHEN             	= "immediate";
	
	public static final int DEFAULT_TX_RETRY_DELAY				= 0;
	
}