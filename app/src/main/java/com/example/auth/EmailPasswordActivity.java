package com.example.auth;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.InputStream;
import java.net.URL;

public class EmailPasswordActivity extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "EmailPasswordActivity";
	private EditText mEmailField, mPasswordField;
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener mAuthListener;
	private ImageView mImageView;
	private TextView mTextViewProfile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emailpassword);

		mTextViewProfile = (TextView) findViewById(R.id.profile);
		mEmailField = (EditText) findViewById(R.id.field_email);
		mPasswordField = (EditText) findViewById(R.id.field_password);
		mImageView = (ImageView) findViewById(R.id.logo);

		findViewById(R.id.email_sign_in_button).setOnClickListener(this);
		findViewById(R.id.email_create_account_button).setOnClickListener(this);
		findViewById(R.id.sign_out_button).setOnClickListener(this);

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

	private void createAccount(String email, String password) {
		if (!validateForm()) {
			return;
		}
		showProgressDialog();
		mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
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

	private void signIn(String email, String password) {
		if (!validateForm()) {
			return;
		}
		showProgressDialog();
		mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(@NonNull Task<AuthResult> task) {
				Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
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

	private boolean validateForm() {
		if (TextUtils.isEmpty(mEmailField.getText().toString())) {
			mEmailField.setError("Required.");
			return false;
		} else if (TextUtils.isEmpty(mPasswordField.getText().toString())) {
			mPasswordField.setError("Required.");
			return false;
		} else {
			mEmailField.setError(null);
			return true;
		}
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

			findViewById(R.id.email_password_buttons).setVisibility(View.GONE);
			findViewById(R.id.email_password_fields).setVisibility(View.GONE);
			findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
		} else {
			mTextViewProfile.setText(null);

			findViewById(R.id.email_password_buttons).setVisibility(View.VISIBLE);
			findViewById(R.id.email_password_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.sign_out_button).setVisibility(View.GONE);
		}
		hideProgressDialog();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.email_create_account_button:
				createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
				break;
			case R.id.email_sign_in_button:
				signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
				break;
			case R.id.sign_out_button:
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