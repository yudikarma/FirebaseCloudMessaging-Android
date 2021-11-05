package com.example.fcm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static com.example.fcm.R.id.txt;

public class MainActivity extends AppCompatActivity {
	private static final String AUTH_KEY = "AAAA1bNVItI:APA91bGvpkoJKBY8vWyoY-9xqQSuQSYYWpXAsbRPKnBkwe6FzceN-nJb9mxxz9kZuzsZbABeDjuYZTBY4FIG0T68si6Jd2wwDQ4EfS2wTyLFg-4MhtIZl-FfI7lts_Ky3-EhoUJ1jChf";
	private TextView mTextView;
	private String token;
	private String tokenHms;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTextView = findViewById(txt);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String tmp = "";
			for (String key : bundle.keySet()) {
				Object value = bundle.get(key);
				tmp += key + ": " + value + "\n\n";
			}
			mTextView.setText(tmp);
		}

		FirebaseMessaging.getInstance().getToken()
				.addOnCompleteListener(new OnCompleteListener<String>() {
					@Override
					public void onComplete(@NonNull Task<String> task) {
						if (!task.isSuccessful()) {
							Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
							return;
						}

						// Get new FCM registration token
						token = task.getResult();

						// Log and toast
						Log.d("Token : ", token);
						Toast.makeText(MainActivity.this, "Success get token", Toast.LENGTH_SHORT).show();
					}
				});



		//get token hms
	}

	private void getTokenHms() {

		new Thread() {
			@Override
			public void run() {
				try {
					// read from agconnect-services_old.json
					String appId = "104794689";
					String token = HmsInstanceId.getInstance(MainActivity.this).getToken(appId, "HCM");
					Log.i(MainActivity.class.getSimpleName(), "get token:" + token);
					/*if(!TextUtils.isEmpty(token)) {
						sendRegTokenToServer(token);
					}*/
					tokenHms = token;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTextView.setText(tokenHms);
						}
					});

					Log.d("get token:" ,token);
				} catch (ApiException e) {
					Log.e(MainActivity.class.getSimpleName(), "get token failed, " + e);
					Log.d("get token failed, " , e.getLocalizedMessage());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTextView.setText(e.getLocalizedMessage());
						}
					});
				}
			}
		}.start();
	}

	public void showToken(View view) {
		mTextView.setText(token);
	}

	public void showTokenHms(View view) {
		getTokenHms();
	}

	public void subscribe(View view) {
		FirebaseMessaging.getInstance().subscribeToTopic("news");
		mTextView.setText(R.string.subscribed);
	}

	public void unsubscribe(View view) {
		FirebaseMessaging.getInstance().unsubscribeFromTopic("news");
		mTextView.setText(R.string.unsubscribed);
	}

	public void sendToken(View view) {
		sendWithOtherThread("token");
	}

	public void sendTokens(View view) {
		sendWithOtherThread("tokens");
	}

	public void sendTopic(View view) {
		sendWithOtherThread("topic");
	}

	private void sendWithOtherThread(final String type) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				//pushNotification(type);
			}
		}).start();
	}

	private void pushNotification(String type) {
		JSONObject jPayload = new JSONObject();
		JSONObject jNotification = new JSONObject();
		JSONObject jData = new JSONObject();
		try {
			jNotification.put("title", "Google I/O 2016");
			jNotification.put("body", "Firebase Cloud Messaging (App)");
			jNotification.put("sound", "default");
			jNotification.put("badge", "1");
			jNotification.put("click_action", "OPEN_ACTIVITY_1");
			jNotification.put("icon", "ic_notification");

			jData.put("picture", "https://miro.medium.com/max/1400/1*QyVPcBbT_jENl8TGblk52w.png");

			switch(type) {
				case "tokens":
					JSONArray ja = new JSONArray();
					//ja.put("c5pBXXsuCN0:APA91bH8nLMt084KpzMrmSWRS2SnKZudyNjtFVxLRG7VFEFk_RgOm-Q5EQr_oOcLbVcCjFH6vIXIyWhST1jdhR8WMatujccY5uy1TE0hkppW_TSnSBiUsH_tRReutEgsmIMmq8fexTmL");
					ja.put(AUTH_KEY);
					ja.put(token);
					jPayload.put("registration_ids", ja);
					break;
				case "topic":
					jPayload.put("to", "/topics/news");
					break;
				case "condition":
					jPayload.put("condition", "'sport' in topics || 'news' in topics");
					break;
				default:
					jPayload.put("to", token);
			}

			jPayload.put("priority", "high");
			jPayload.put("notification", jNotification);
			jPayload.put("data", jData);

			URL url = new URL("https://fcm.googleapis.com/fcm/send");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", AUTH_KEY);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			// Send FCM message content.
			OutputStream outputStream = conn.getOutputStream();
			outputStream.write(jPayload.toString().getBytes());

			// Read FCM response.
			InputStream inputStream = conn.getInputStream();
			final String resp = convertStreamToString(inputStream);

			Handler h = new Handler(Looper.getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {
					mTextView.setText(resp);
				}
			});
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	private String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next().replace(",", ",\n") : "";
	}
}