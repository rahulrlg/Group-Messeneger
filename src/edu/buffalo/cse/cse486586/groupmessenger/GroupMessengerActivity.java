package edu.buffalo.cse.cse486586.groupmessenger;


import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Comparator;

import java.util.PriorityQueue;
import java.util.Queue;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
	 static final String TAG = GroupMessengerActivity.class.getSimpleName();
	 private Uri mUri;
	 private static final String KEY_FIELD = "key";
	 private static final String VALUE_FIELD = "value";
	 private String[] connPort={"11108", "11112", "11116", "11120", "11124"};
	 static final int SERVER_PORT = 10000;
	 String myPort="";
	 private static int counter=0;
	 private static int groupSequence=0;
	 private static int processSequence=0;
	 Comparator<Message> comparator=new MessageSorter();
	 Queue<Message> qHoldBack;
	 ServerSocket serverSocket=null;
	 ContentResolver mContentResolver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        qHoldBack=new PriorityQueue<Message>(10, comparator);
        mContentResolver=getContentResolver();
        mUri=buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        /*
         * Calculate the port number that this AVD listens on.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        try{
        	serverSocket = new ServerSocket(SERVER_PORT);
        	new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        	//serverSocket.close();
        }
        catch(IOException e)
        {
        	Log.e(TAG, "Can not create a server socket."+e);
        }
        
        
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String msg=editText.getText().toString()+ "\n";
				editText.setText("");
				Message mPayload=prepareMessage(msg, myPort, "msgToSequencer");
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mPayload);
			}
		});
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs in a total-causal order.
         */
    }
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	if(null!=serverSocket)
    	{
    		
    		try {
    			
				serverSocket.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG,"Can not close server socket");
			}
    		
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    
    private Message prepareMessage(String msg,String port,String msgType)
	{
		String id=String.valueOf(counter)+port;
		Message msgObject=new Message(id, msg, port, msgType);
		counter++;
		return msgObject;
	}
    
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    private class ServerTask extends AsyncTask<ServerSocket, Message, Void>
    {
    	ServerSocket serverSocket=null;
    	Socket serviceSocket=null;
    	ObjectInputStream input=null;
    	@Override
    	protected Void doInBackground(ServerSocket... sockets) {
    		 serverSocket = sockets[0];
             Message incomingMsg;
             Log.e(TAG, "accept outside while");
             while(true)
             {
             	try{
             		serviceSocket=serverSocket.accept(); 
             		//Log.e(TAG, "accept");
             		//Log.e(TAG, myPort);
             		input = new ObjectInputStream(serviceSocket.getInputStream());
             		incomingMsg=(Message)input.readObject();
             		
             		if(null!=incomingMsg)
             		{
                   		if(incomingMsg.msgType.equals("msgToSequencer") && myPort.equals("11108"))
	             		{
	             			Sequencer seq=new Sequencer(incomingMsg);
	             			//Log.e(TAG, incomingMsg.msg);
	             			seq.multicast();
	             		}
	             		else if(incomingMsg.msgType.equals("message"))
	             		{
	             			publishProgress(incomingMsg);
	             		}
             		}        		
             	}
             	catch(ClassNotFoundException e)
             	{
             		Log.e(TAG, "Error reading object.Error message:"+e.getMessage());
             	}
             	catch(IOException ex){
             		Log.e(TAG, "Error reading messages from client.Error Message:"+ex.getMessage());
             	}
             	
             }
            
    	}
    	protected void onProgressUpdate(Message...msgPayload) {
    	
    			processMessage(msgPayload[0]);
    	
        }
    	
    	private void processMessage(Message msg)
    	{
    		try{
    			
    		qHoldBack.add(msg);
    		while(qHoldBack.size()>0)
    		{
    			Message m= qHoldBack.peek();
    			if(null!=m && m.sequenceNo==processSequence)
    			{
    				m=qHoldBack.remove();
    				ContentValues cv=new ContentValues();
    				cv.put(KEY_FIELD, String.valueOf(m.sequenceNo));
    				cv.put(VALUE_FIELD, m.msg);
    				mContentResolver.insert(mUri, cv);
    				TextView tv=(TextView)findViewById(R.id.textView1);
    				tv.append("\n" + m.msg);
    				processSequence++;
    			}
    			else{
    				break;
    			}
    		}
    		}
    		catch(Exception e)
    		{
    			Log.e(TAG+".ServerTask:processMessage",e.getMessage());
    		}
    	}
    }
    private class ClientTask extends AsyncTask<Message, Void, Void>
    {
    	@Override
    	protected Void doInBackground(Message... params) {
    			
    		Message msg= params[0];
       		try{ 
    			if(msg.msgType.equals("msgToSequencer"))
    			 {
    				 sendMessage(msg, Integer.parseInt(connPort[0]));
      			 }
    			else
    			{
    				for (String port : connPort) {
						sendMessage(msg, Integer.parseInt(port));
					}
    			}
    		
    		}
    		catch(Exception e)
    		{
    			Log.e(TAG,"Error in ClientTask:doInBackground:"+e);
    		}
            return  null;
    	}
    	private void sendMessage(Message msg,int remotePort)
    	{
    		ObjectOutputStream out=null;
    		Socket socket=null;
    		try
    		{
    			socket=new Socket(InetAddress.getByAddress(new byte[]{10,0,2,2}), remotePort); 
    			out=new ObjectOutputStream(socket.getOutputStream());
    			Log.e(TAG, "check");
    			 out.writeObject(msg);
    		}
    		catch(IOException e)
    		{
    			Log.e(TAG,"Unable to create socket.Message:"+e);
    		}
    		finally
    		{
    			try
            	{
            		out.close();
       			 	socket.close();
            	}
            	catch(IOException e)
        		{
        			Log.e(TAG,"Unable to close the stream:"+e);
        		}
    		}
    	}
    	
    }
    
    private class MessageSorter implements Comparator<Message>
    {
    	@Override
    	public int compare(Message x,Message y)
    	{
    		if(x.sequenceNo>y.sequenceNo)
    		{
    			return 1;
    		}
    		else if(x.sequenceNo<y.sequenceNo){
    			return -1;
    		}
    		return 0;
    	}
    }
    private class Sequencer
    {
    	Message mPayload;
    	int pSequence;
    	Sequencer(Message msg)
    	{
    		 this.mPayload=msg;
    	}
    	public void multicast()
    	{
    		this.mPayload.sequenceNo=groupSequence;
    		this.mPayload.msgType="message";
    		groupSequence++;
    		//Log.e(TAG,String.valueOf(pSequence));
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, this.mPayload);
    	}
    }

    
}

