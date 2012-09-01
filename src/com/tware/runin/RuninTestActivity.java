package com.tware.runin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class RuninTestActivity extends Activity implements SurfaceHolder.Callback{
	private SurfaceView sv;
	private MediaPlayer mPlayer;
	private TextView bTime, cTime, pTime, rStatus;
	private String TAG = "RuninTest";
	
	private Handler uHandler;
	private TimerTask task;
	private Timer timer = new Timer();;
	private SimpleDateFormat nowdate = new SimpleDateFormat("HH:mm:ss");
	private SurfaceHolder sHolder;
	private int runTime = 0;
	private int hour = 0;
	private int RuninTime = 2; // Define runin time
	private boolean isStop = false;
	private boolean isPause = false;
	private boolean isPass = false;
	
	private String runinVideo = null;
	private List<String> playList = new ArrayList<String>();

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        sv = (SurfaceView)findViewById(R.id.sView);
        bTime = (TextView)findViewById(R.id.view_begin);
        cTime = (TextView)findViewById(R.id.view_current);
        pTime = (TextView)findViewById(R.id.view_passed);
        rStatus = (TextView)findViewById(R.id.view_status);
        rStatus.setVisibility(View.INVISIBLE);
        
        mPlayer = new MediaPlayer();

        File f = new File("/mnt/sdcard/runin.cfg");
        if (!f.exists())
        {
        	f = new File("/mnt/sdcard/external_sdcard/runin.cfg");
        }
        
        if (f.exists() && f.isFile())
        {
        	Log.i(TAG, f.getAbsoluteFile()+ " found");
        	try {
        		BufferedReader fr = new BufferedReader(new FileReader(f));
				String str = fr.readLine();
				do{
					if (!str.startsWith("#") && str.trim().length()>= 11 )
					{
						break;
					}											
				}while((str = fr.readLine())!= null);
					
				if (!str.startsWith("#"))
				{
					String [] strSplit = new String[20];
					strSplit = str.split("=");
					if (strSplit != null && strSplit[0].equalsIgnoreCase("RuninTime"))
					{
						RuninTime = Integer.parseInt(strSplit[1].trim());
					}
				}
				
				fr.close();
			
        	} catch (FileNotFoundException e) {
				e.printStackTrace();
			}catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        
        this.getWindow().setFormat(PixelFormat.UNKNOWN);
        
        sHolder = sv.getHolder();
        sHolder.addCallback(this);
        sHolder.setKeepScreenOn(true);
        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        uHandler = new Handler(){
        	@Override
        	public void handleMessage(Message msg)
        	{
        		super.handleMessage(msg);
        		switch (msg.what)
        		{
        			case 0:
        				// show Begin time when press Play menu
        				bTime.setText("Begin time: " + nowdate.format(new Date()));
        		        break;
        			case 1:
        				// show current time while runin is on going
        				cTime.setText("Current time: " + nowdate.format(new Date()));
        				if (runTime >= 3600)
        				{
        					hour ++;
        					runTime = 0;
        				}
        				pTime.setText("Run time: " + hour +":" + runTime/60 + "  ");
        				break;
        			case 2:
        				// no video found message
        				Toast.makeText(getApplicationContext(), "Video not found!", Toast.LENGTH_LONG).show();
        				break;
        			case 3:
        				// video found message
        				Toast.makeText(getApplicationContext(), "Runin video was found, get ready!", Toast.LENGTH_LONG).show();        				
        				break;
        			case 4:
        				// Fail conditions
        				Log.e(TAG, "Got Message for Fail");
        				setRuninResults(1);
        				break;
        			case 5:
        				// Fass conditions
        				setRuninResults(0);	
        				break;
        			
        		}
        	}
        };
        
        task = new TimerTask(){
        	@Override
        	public void run()
        	{
        		runTime ++;
        		if (hour >= RuninTime)
//        		if (runTime > 600)
        		{
        			Message msg = new Message();
            		msg.what = 5;
            		uHandler.sendMessage(msg);
        		}else
        		{
        			Message msg = new Message();
            		msg.what = 1;
            		uHandler.sendMessage(msg);
        		}
        	}
        };

        new Thread(){
        	@Override
        	public void run(){
        		if (!findRuninVideo("/mnt"))
        		{
        			Message msg = new Message();
        			msg.what = 2;
        			uHandler.sendMessage(msg);
        		}
        		else
        		{
        			Message msg = new Message();
        			msg.what = 3;
        			uHandler.sendMessage(msg);
        			runinVideo = playList.get(0);
        		}
        		Log.d(TAG, "Thread exit success!");
        	}
        }.start();

        mPlayer.setOnCompletionListener(new OnCompletionListener(){
        	@Override
        	public void onCompletion(MediaPlayer mp)
        	{
        		Log.e(TAG, "onCompletion()");
        		try {
        			if(mPlayer!=null)
        			{
                		Log.e(TAG, "onCompletion() -> rePlay");
        				mPlayer.reset();
        				mPlayer.setDataSource(runinVideo);
        				mPlayer.prepare();
        				mPlayer.start();
        			}
				} catch (IllegalArgumentException e) {
        			Message msg = new Message();
					msg.what = 4;
					uHandler.sendMessage(msg);
					e.printStackTrace();
				} catch (IllegalStateException e) {
        			Message msg = new Message();
					msg.what = 4;
					uHandler.sendMessage(msg);
					e.printStackTrace();
				} catch (IOException e) {
        			Message msg = new Message();
					msg.what = 4;
					uHandler.sendMessage(msg);
					e.printStackTrace();
				}
        	}
        });
     
        mPlayer.setOnPreparedListener(new OnPreparedListener(){
        	@Override
        	public void onPrepared(MediaPlayer mp)
        	{
        		Log.e(TAG, "onPrepared");
        		if (mPlayer!=null && !mp.isPlaying())
        		{
        			mPlayer.start();
        		}
        	}
        });
        
    }
    
	/* 
	 * Function: settings for Runin Test results
	 * Parameters: type   
	 * 				0:	Pass
	 * 				1:  Fail
	 * return: no return value
	 * */
	public void setRuninResults(int type)
	{
		switch (type)
		{
			case 0:
				rStatus.setText("Pass");
				rStatus.setBackgroundColor(Color.GREEN);
				isPass = true;
				break;
			case 1:
				rStatus.setText("Fail");
				rStatus.setBackgroundColor(Color.RED);
				isPass = false;
				break;
			default:
				return ;
		}
		
		sv.setVisibility(View.INVISIBLE);
		rStatus.setVisibility(View.VISIBLE);
		rStatus.setTextColor(Color.WHITE);
		rStatus.setTextSize(200);
		/* Here release MediaPlayer. */
		if (mPlayer != null)
		{
			mPlayer.pause();
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
		}

		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}
    
    public boolean findRuninVideo(String sdPath)
    {
    	File f = new File(sdPath);
    	if (!f.exists()) return false;

    	try{
    		File[] fl = f.listFiles();
    	
    		for (int i = 0; i< fl.length; i++)
    		{
    			Log.e(TAG, fl[i].toString());
    			if (fl[i].isDirectory())  
    			{
    				findRuninVideo(fl[i].getAbsolutePath());
    			}else if (	fl[i].getName().toString().endsWith(".mp4") )
    			{    			
    				Log.i(TAG, "Found " + fl[i].toString());
    				playList.add(fl[i].toString());
    			}
    		}
    		
    		if (playList.size() <= 0 )
    		{
    			return false;
    		}
    		else
    		{
    			return true;
    		}
    	}
    	catch (Exception e)
    	{
    		return false;
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "▶ Play");
        menu.add(0, 1, 1, "■ Stop");      
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId())
        {
    		case 0:
			try {
				if (isPass)
				{
					Toast.makeText(getApplicationContext(), "Runin is Pass! If you want to retest, please reopen this app for Runin!", Toast.LENGTH_LONG).show();
					return false;
				}
				if (isStop)
				{
					Toast.makeText(getApplicationContext(), "You've stoped, can not play again!", Toast.LENGTH_LONG).show();
					return false;
				}
				
				if (mPlayer!=null && mPlayer.isPlaying())
				{
					Toast.makeText(getApplicationContext(), "Already runin!", Toast.LENGTH_LONG).show();
					return false;					
				}
				
				if (mPlayer!=null && runinVideo != null)
				{
    		        timer.schedule(task, 0, 1000);  /* Now start to Counter */
					mPlayer.setDataSource(runinVideo);
					sHolder.setFixedSize(	mPlayer.getVideoWidth(),
											mPlayer.getVideoHeight());
					mPlayer.prepare();
					mPlayer.start();
					
					Toast.makeText(getApplicationContext(), 
									"Runin Time is " + RuninTime + "H",
									Toast.LENGTH_LONG).show();
					
        			Message msg = new Message();
	    			msg.what = 0;
	    			uHandler.sendMessage(msg);
				}
				else
				{
        			Message msg = new Message();
					msg.what = 2;
					uHandler.sendMessage(msg);
				}
			} catch (IllegalArgumentException e) {
    			Message msg = new Message();
				msg.what = 4;
				uHandler.sendMessage(msg);
				e.printStackTrace();
			} catch (IllegalStateException e) {
    			Message msg = new Message();
				msg.what = 4;
				uHandler.sendMessage(msg);
				e.printStackTrace();
			} catch (IOException e) {
    			Message msg = new Message();
				msg.what = 4;
				uHandler.sendMessage(msg);
				e.printStackTrace();
			}
    			break;
    		case 1:
    			if(mPlayer != null)
    			{
    				isStop =true;
    				mPlayer.pause();
    				mPlayer.release();
    				mPlayer = null;
    				Toast.makeText(getApplicationContext(), "You've  stoped it succeessfully!", Toast.LENGTH_LONG).show();
    			}
    			else
    			{
    				Toast.makeText(getApplicationContext(), "You've stoped it yet!", Toast.LENGTH_LONG).show();
    			}
    			if (timer != null)
    			{
    				timer.cancel();
    				timer = null;
    			}
    			break;
        }
        return false;
    }

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.e(TAG, "surfaceChanged");		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.e(TAG, "surfaceCreated");
		if (mPlayer!=null)
		{
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setDisplay(sHolder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.e(TAG, "surfaceDestroyed");
	}
    
	@Override
	public void onResume()
	{
		super.onResume();
		if (mPlayer!= null && !mPlayer.isPlaying() && isPause)
		{
			Log.e(TAG, "onResume[Now start Media playing...]");
			mPlayer.start();
		}
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		if (mPlayer!= null && mPlayer.isPlaying())
		{
			Log.e(TAG, "onPause");
			isPause = true;
			mPlayer.pause();
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mPlayer != null)
		{
			mPlayer.release();
			mPlayer = null;
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPlayer != null && mPlayer.isPlaying())
			{
				new AlertDialog.Builder(this)
				.setIcon(R.drawable.icon)
				.setTitle("Warning")
				.setMessage("Running, you can't cancel it!")
				.setCancelable(false)
				.setPositiveButton("Ok", null).show();
				return false;
			}
			
			new AlertDialog.Builder(this).setTitle("Exit?")
				.setIcon(R.drawable.icon)
				.setMessage("Are you sure to exit Runin?")
				.setCancelable(false)
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Sure", new OnClickListener(){
					public void onClick(DialogInterface dialog, int whichButton)
					{
						System.exit(0);
					}
				}).show();
			return true;
		}
		return false;
	}
}