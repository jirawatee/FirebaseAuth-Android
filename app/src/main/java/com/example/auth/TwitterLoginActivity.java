package com.example.auth;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.TwitterAuthProvider;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import java.io.InputStream;
import java.net.URL;

import io.fabric.sdk.android.Fabric;

public class TwitterLoginActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "TwitterLoginActivity";
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener mAuthListener;
	private ImageView mImageView;
	private TextView mTextViewProfile;
	private TwitterLoginButton mLoginButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Configure Twitter SDK
		TwitterAuthConfig authConfig = new TwitterAuthConfig(
				getString(R.string.twitter_consumer_key),
				getString(R.string.twitter_consumer_secret)
		);
		Fabric.with(this, new Twitter(authConfig));

		// Inflate layout (must be done after Twitter is configured)
		setContentView(R.layout.activity_twitter);

		mImageView = findViewById(R.id.logo);
		mTextViewProfile = findViewById(R.id.profile);
		findViewById(R.id.button_twitter_signout).setOnClickListener(this);

		mAuth = FirebaseAuth.getInstance();
		mAuthListener = new FirebaseAuth.AuthStateListener() {
			@Override
			public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
				FirebaseUser user = firebaseAuth.getCurrentUser();
				if (user != null) {
					Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
				} else {
					Log.d(TAG, "onAuthStateChanged:signed_out");
				}
				updateUI(user);
			}
		};

		// initialize_twitter_login
		mLoginButton = findViewById(R.id.button_twitter_login);
		mLoginButton.setCallback(new Callback<TwitterSession>() {
			@Override
			public void success(Result<TwitterSession> result) {
				Log.d(TAG, "twitterLogin:success" + result);
				handleTwitterSession(result.data);
			}

			@Override
			public void failure(TwitterException exception) {
				Log.w(TAG, "twitterLogin:failure", exception);
				updateUI(null);
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		mAuth.addAuthStateListener(mAuthListener);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mAuthListener != null) {
			mAuth.removeAuthStateListener(mAuthListener);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Pass the activity result to the Twitter login button.
		mLoginButton.onActivityResult(requestCode, resultCode, data);
	}

	// auth_with_twitter
	private void handleTwitterSession(TwitterSession session) {
		Log.d(TAG, "handleTwitterSession:" + session);
		showProgressDialog();

		AuthCredential credential = TwitterAuthProvider.getCredential(
				session.getAuthToken().token,
				session.getAuthToken().secret
		);

		mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
				if (!task.isSuccessful()) {
					mTextViewProfile.setTextColor(Color.RED);
					mTextViewProfile.setText(task.getException().getMessage());
				} else {
					mTextViewProfile.setTextColor(Color.DKGRAY);
				}
				hideProgressDialog();
			}
		});
	}

	private void signOut() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.logout);
		alert.setCancelable(false);
		alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				mAuth.signOut();
				Twitter.logOut();
				updateUI(null);
			}
		});
		alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		});
		alert.show();
	}

	private void updateUI(FirebaseUser user) {
		if (user != null) {
			if (user.getPhotoUrl() != null) {
				new DownloadImageTask().execute(user.getPhotoUrl().toString());
			}
			mTextViewProfile.setText("DisplayName: " + user.getDisplayName());
			mTextViewProfile.append("\n\n");
			mTextViewProfile.append("Email: " + user.getEmail());
			mTextViewProfile.append("\n\n");
			mTextViewProfile.append("Firebase ID: " + user.getUid());

			findViewById(R.id.button_twitter_login).setVisibility(View.GONE);
			findViewById(R.id.button_twitter_signout).setVisibility(View.VISIBLE);
		} else {
			mImageView.getLayoutParams().width = (getResources().getDisplayMetrics().widthPixels / 100) * 64;
			mImageView.setImageResource(R.mipmap.authentication);
			mTextViewProfile.setText(null);

			findViewById(R.id.button_twitter_login).setVisibility(View.VISIBLE);
			findViewById(R.id.button_twitter_signout).setVisibility(View.GONE);
		}
		hideProgressDialog();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_twitter_signout:
				signOut();
				break;
		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		@Override
		protected Bitmap doInBackground(String... urls) {
			Bitmap mIcon = null;
			try {
				InputStream in = new URL(urls[0]).openStream();
				mIcon = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mIcon;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				mImageView.getLayoutParams().width = (getResources().getDisplayMetrics().widthPixels / 100) * 24;
				mImageView.setImageBitmap(result);
			}
		}
	}
}