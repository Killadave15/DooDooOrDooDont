package doodoo.doodooordoodont;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by David on 4/5/2018.
 */

public class LoginActivity extends Activity {
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_login);

        //Initializes the Facebook Sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        //Initializes variables for google sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //Adds an OnEditorActionListener to listen for the Done button and login the user
        EditText password = findViewById(R.id.password);
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;
                switch(result) {
                    case EditorInfo.IME_ACTION_DONE:
                        if (validateForm()){
                            signIn(((TextView)findViewById(R.id.emailAddress)).getText().toString(),
                                    ((TextView)findViewById(R.id.password)).getText().toString());
                        }
                        break;
                }
                return true;
            }
        });

        //Changes the text of the Google sign in button
        SignInButton googleButton = findViewById(R.id.google_login);
        TextView textView = (TextView) googleButton.getChildAt(0);
        textView.setText("Sign In with Google");
        googleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleSignIn();
            }
        });

        //Not used code for registering a callback of the facebook button
        loginButton = findViewById(R.id.facebook_login);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override public void onSuccess(LoginResult loginResult) {            }
            @Override public void onCancel() {            }
            @Override public void onError(FacebookException e) {            }
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //Adds the Createaccount dialog to the buttons onclicklistener
        final Button createAccount = findViewById(R.id.create_button);
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Creates the alertdialog and the layout to add the fields to
                AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
                alert.setMessage("Please enter your information");
                alert.setTitle("Account Creation");
                final LinearLayout layout = new LinearLayout(LoginActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);

                //Username field
                final EditText username = new EditText(LoginActivity.this);
                username.setHint("Username");
                username.setWidth(layout.getWidth());

                //Password field
                final EditText pass = new EditText(LoginActivity.this);
                pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                pass.setHint("Password");
                pass.setWidth(layout.getWidth());

                //email field
                final EditText email = new EditText(LoginActivity.this);
                email.setHint("Email");
                email.setWidth(layout.getWidth());

                //gender field
                final EditText gender = new EditText(LoginActivity.this);
                gender.setHint("Gender");
                gender.setWidth(layout.getWidth());

                layout.addView(username);
                layout.addView(pass);       //Adds field to layout
                layout.addView(email);
                layout.addView(gender);
                alert.setView(layout);

                //Sets the positive button to call create account with the fields
                alert.setPositiveButton("Create Account", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        createAccount(username.getText().toString(),pass.getText().toString(),email.getText().toString(),gender.getText().toString());
                    }
                });
                alert.show();

            }
        });

        //Adds onClick to login button to validate the entries and attempt to sign in user
        final Button login = findViewById(R.id.login_button);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateForm()){
                    signIn(((TextView)findViewById(R.id.emailAddress)).getText().toString(),
                                        ((TextView)findViewById(R.id.password)).getText().toString());
                }
            }



        });
    }

    /**
     * onStart
     *
     * This method is run everytime the activity is started, even if it has already been created.
     * If this activity starts through an intent, this method is still called.
     */
    @Override
    public void onStart(){
        super.onStart();
    }

    /**
     * createAccount:
     *
     * This method creates an account for the database using the specified username, password, and
     * email.
     *
     * @param username Username for the account
     * @param password Password for the account
     * @param email email for the account
     */
    private void createAccount(final String username, String password, final String email, final String gender) {
        Log.d(TAG, "createAccount:" + email);
        mAuth.signOut(); //Signs out the anonymous user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            saveUserData(email, username, gender, user.getUid());
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserData(String email, String username, String gender, String uid) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("name",username);
        map.put("numReviews",0);
        map.put("gender", gender);
        map.put("userId", mAuth.getUid());
        db.collection("users").document(mAuth.getUid()).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User profile stored.");
                }
                else {
                    Log.d(TAG, "User profile not stored.");
                }
            }
        });
    }

    /**
     * signIn
     *
     * This method checks to make sure that the fields have been filled out, and if so it attempts
     * to log the user in using the specified credentials
     *
     * @param email The user entered email
     * @param password The user entered password
     */
    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);

        //Checks to make sure the fields are filled out
        if (!validateForm()) {
            return;
        }

        //Attampts to log the user in using the email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this,"Successfully logged in.",
                                    Toast.LENGTH_SHORT).show();

                            //Send the user back to the home screen
                            Intent toHome = new Intent(LoginActivity.this,MainActivity.class);
                            startActivity(toHome);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
        else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this,"Successfully logged in.",
                                    Toast.LENGTH_SHORT).show();

                            //Send the user back to the home screen
                            Intent toHome = new Intent(LoginActivity.this,MainActivity.class);
                            startActivity(toHome);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this,"Authentication Failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }


    /**
     * validateForm:
     *
     * This method checks the emailAddress and password field to make sure that the user has filled
     * them out.
     *
     * @return Returns a boolean depending on whether it was filled out or not
     */
    private boolean validateForm() {
        boolean valid = true;
        TextView mEmailField = findViewById(R.id.emailAddress);
        TextView mPasswordField = findViewById(R.id.password);

        //Checks the email field, and sets and error if it is empty
        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        //Checks the password field, and sets an error if it is empty
        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    public void onBackPressed() {
        Intent toHome = new Intent(this, MainActivity.class);
        startActivity(toHome);
    }
}
