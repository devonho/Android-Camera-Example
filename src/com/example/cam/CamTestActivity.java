package com.example.cam;

/**
 * @author Jose Davis Nidhin
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class CamTestActivity extends Activity {
	private static final String TAG = "CamTestActivity";
	Preview preview;
	Button buttonClick;
	Camera camera;
	Activity act;
	Context ctx;
	JSONObject faceAttributes;
	
	public enum DetectMode {AGE_GENDER, OCR};
	DetectMode currentMode;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		currentMode = DetectMode.OCR;
		
		setContentView(R.layout.main);

		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((FrameLayout) findViewById(R.id.layout)).addView(preview);
		preview.setKeepScreenOn(true);

		preview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
				camera.autoFocus(afCallback);

			}
		});

		Toast.makeText(ctx, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();

		//		buttonClick = (Button) findViewById(R.id.btnCapture);
		//		
		//		buttonClick.setOnClickListener(new OnClickListener() {
		//			public void onClick(View v) {
		////				preview.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		//				camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		//			}
		//		});
		//		
		//		buttonClick.setOnLongClickListener(new OnLongClickListener(){
		//			@Override
		//			public boolean onLongClick(View arg0) {
		//				camera.autoFocus(new AutoFocusCallback(){
		//					@Override
		//					public void onAutoFocus(boolean arg0, Camera arg1) {
		//						//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		//					}
		//				});
		//				return true;
		//			}
		//		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		int numCams = Camera.getNumberOfCameras();
		if(numCams > 0){
			try{
				camera = Camera.open(0);
				camera.startPreview();
				preview.setCamera(camera);
			} catch (RuntimeException ex){
				Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.ocr_mode:
	        	currentMode = DetectMode.OCR;
	            return true;
	        case R.id.age_gender_mode:
	        	currentMode = DetectMode.AGE_GENDER;
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}	
	
	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	private void handleHttpResponse(String rsp) {
		if(currentMode == DetectMode.AGE_GENDER) {	
			try {
		        JSONArray jarr;
				jarr = (JSONArray) new JSONTokener(rsp).nextValue();
		        JSONObject json = (JSONObject) jarr.get(0);
		        faceAttributes = (JSONObject)json.get("attributes");
		        makeToast("Age: " + faceAttributes.getString("age") + " Gender: " + faceAttributes.getString("gender"));
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else if(currentMode == DetectMode.OCR) {
			try {
				String ocrtext = "";
				JSONObject jrsp;
				jrsp = (JSONObject) new JSONTokener(rsp).nextValue();
				JSONArray jregions = (JSONArray)jrsp.get("regions");
				
				for(int i=0; i < jregions.length(); i++) {
					JSONObject jregion = jregions.getJSONObject(i);
					
					JSONArray jlines = (JSONArray) jregion.get("lines");
					for(int j=0;j<jlines.length();j++) {
						JSONObject jline = jlines.getJSONObject(j);
						JSONArray jwords = jline.getJSONArray("words");
						
						for(int k=0;k<jwords.length();k++) {
							JSONObject jword = jwords.getJSONObject(k);
							ocrtext = ocrtext + jword.get("text").toString() + " ";
						}
						
					}
				}
				
		        makeToast(ocrtext);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}
	
	private void makeToast(final String msg) {
        act.runOnUiThread(new Runnable() {
      	  public void run() {
      	    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
      	  }
      	});	            		
	}
	
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			//			 Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			//			 Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new SaveImageTask().execute(data);
			resetCam();
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};

	AutoFocusCallback afCallback = new AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera) {
			if(success)
				camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		}
	};
	
	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			/*
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File (sdCard.getAbsolutePath() + "/camtest");
				dir.mkdirs();				

				String fileName = String.format("%d.jpg", System.currentTimeMillis());
				File outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

				refreshGallery(outFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			*/
			makePostRequest(data[0], currentMode);
			
			return null;
		}

	    private void makePostRequest(byte[] img, CamTestActivity.DetectMode mode) {
	    	 
	    	String apikey="", url="";	    	
	    	if(mode == DetectMode.OCR) {
	    		url = getString(R.string.ocr_api_url);
	    		apikey = getString(R.string.ComputerVisionAPIs);
	    	}
	    	else if(mode == DetectMode.AGE_GENDER){
	    		url = getString(R.string.face_api_url);
	    		apikey = getString(R.string.FaceAPIs);	    		
	    	}
	        
	        HttpClient httpClient = new DefaultHttpClient();
	        HttpPost httpPost = new HttpPost(url);
	 
	        httpPost.setHeader("Ocp-Apim-Subscription-Key", apikey);
	        httpPost.setHeader("Content-Type","application/octet-stream");	        
      
	        try {
	        	httpPost.setEntity(new ByteArrayEntity(img));
	        } catch(Exception e) {
	            // log exception
	            e.printStackTrace();	        	
	        }
	 
	        //making POST request.
	        try {
	            HttpResponse response = httpClient.execute(httpPost);
	            // write response to log            
	            String rspentity =  EntityUtils.toString(response.getEntity());
	            Log.d(TAG, "Http Post Response:" + rspentity);
	            handleHttpResponse(rspentity);
	            
	        } catch (ClientProtocolException e) {
	            // Log exception
	            e.printStackTrace();
	        } catch (IOException e) {
	            // Log exception
	            e.printStackTrace();
	        }
	 
	    }    
		
	}
}


