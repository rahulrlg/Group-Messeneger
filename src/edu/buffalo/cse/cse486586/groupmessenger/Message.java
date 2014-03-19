/**
 * 
 */
package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.Serializable;

/**
 * @author rahul
 *
 */
public class Message implements Serializable {
	String msgId;
	String msg;
	int sequenceNo;
	String port;
	String msgType;
	Message(String id,String msg,String port,String msgType)
	{
		this.msgId=id;
		this.msg=msg;
		this.port=port;
		this.msgType=msgType;
	}
	public void setSequenceNo(int sequenceNo)
	{
		this.sequenceNo=sequenceNo;
	}

}
