package org.wikipedia.createaccount;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.PasswordTextInput;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Pattern;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.analytics.CreateAccountFunnel;
import org.wikipedia.captcha.CaptchaHandler;
import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.NonEmptyValidator;
import org.wikipedia.views.WikiErrorView;

import java.util.List;

import retrofit2.Call;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.FeedbackUtil.setErrorPopup;

public class CreateAccountActivity extends ThemedActionBarActivity {
    public static final int RESULT_ACCOUNT_CREATED = 1;
    public static final int RESULT_ACCOUNT_NOT_CREATED = 2;

    public static final int ACTION_CREATE_ACCOUNT = 1;

    public static final String LOGIN_REQUEST_SOURCE = "login_request_source";
    public static final String LOGIN_SESSION_TOKEN = "login_session_token";
    public static final String CREATE_ACCOUNT_RESULT_USERNAME = "username";
    public static final String CREATE_ACCOUNT_RESULT_PASSWORD = "password";

    private CreateAccountInfoClient createAccountInfoClient;
    private CreateAccountClient createAccountClient;

    @Pattern(regex = "[^#<>\\[\\]|{}\\/@]*", messageResId = R.string.create_account_username_error)
    private EditText usernameEdit;
    private PasswordTextInput passwordInput;
    @Password()
    private EditText passwordEdit;
    private PasswordTextInput passwordRepeatInput;
    @ConfirmPassword(messageResId = R.string.create_account_passwords_mismatch_error)
    private EditText passwordRepeatEdit;
    // TODO: remove and replace with @Optional annotation once it's available in the library
    // https://github.com/ragunathjawahar/android-saripaar/issues/102
    @OptionalEmail(messageResId = R.string.create_account_email_error)
    private EditText emailEdit;
    private TextView createAccountButton;
    private TextView createAccountButtonCaptcha;
    private ProgressDialog progressDialog;
    private WikiErrorView errorView;

    private CaptchaHandler captchaHandler;
    private CreateAccountResult createAccountResult;
    private Validator validator;
    private CreateAccountFunnel funnel;
    private WikiSite wiki;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        usernameEdit = (EditText) findViewById(R.id.create_account_username);
        passwordRepeatInput = ((PasswordTextInput) findViewById(R.id.create_account_password_repeat));
        passwordRepeatEdit = passwordRepeatInput.getEditText();
        emailEdit = (EditText) findViewById(R.id.create_account_email);
        createAccountButton = (TextView) findViewById(R.id.create_account_submit_button);
        createAccountButtonCaptcha = (TextView) findViewById(R.id.captcha_submit_button);
        EditText captchaText = (EditText) findViewById(R.id.captcha_text);
        View primaryContainer = findViewById(R.id.create_account_primary_container);
        passwordInput = (PasswordTextInput) findViewById(R.id.create_account_password_input);
        passwordEdit = passwordInput.getEditText();

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.dialog_create_account_checking_progress));

        errorView = (WikiErrorView) findViewById(R.id.view_create_account_error);
        errorView.setBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        errorView.setRetryClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
            }
        });

        wiki = WikipediaApp.getInstance().getWikiSite();
        createAccountInfoClient = new CreateAccountInfoClient();
        createAccountClient = new CreateAccountClient();

        captchaHandler = new CaptchaHandler(this, WikipediaApp.getInstance().getWikiSite(),
                progressDialog, primaryContainer, getString(R.string.create_account_activity_title),
                getString(R.string.create_account_button));

        // We enable the menu item as soon as the username and password fields are filled
        // Tapping does further validation
        validator = new Validator(this);
        Validator.registerAnnotation(OptionalEmail.class);
        validator.setValidationListener(new Validator.ValidationListener() {
            @Override
            public void onValidationSucceeded() {
                if (captchaHandler.isActive() && captchaHandler.token() != null) {
                    doCreateAccount(captchaHandler.token());
                } else {
                    getCreateAccountInfo();
                }
            }

            @Override
            public void onValidationFailed(List<ValidationError> errors) {
                for (ValidationError error : errors) {
                    View view = error.getView();
                    String message = error.getCollatedErrorMessage(view.getContext());
                    if (view instanceof EditText) {
                        //Request focus on the EditText before setting error, so that error is visible
                        view.requestFocus();
                        setErrorPopup((EditText) view, message);
                    } else {
                        throw new RuntimeException("This should not be happening");
                    }
                }
            }
        });

        passwordInput.setOnShowPasswordListener(new PasswordTextInput.OnShowPasswordClickListener() {
            @Override public void onShowPasswordClick(boolean visible) {
                passwordRepeatInput.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        });

        // Don't allow user to submit registration unless they've put in a username and password
        new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                createAccountButton.setEnabled(isValid);
            }
        }, usernameEdit, passwordEdit);

        // Don't allow user to continue when they're shown a captcha until they fill it in
        new NonEmptyValidator(new NonEmptyValidator.ValidationChangedCallback() {
            @Override
            public void onValidationChanged(boolean isValid) {
                createAccountButtonCaptcha.setEnabled(isValid);
            }
        }, captchaText);

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        createAccountButtonCaptcha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validator.validate();
            }
        });

        // Add listener so that when the user taps enter, it submits the captcha
        captchaText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_UP) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    validator.validate();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("result")) {
            createAccountResult = savedInstanceState.getParcelable("result");
        }

        findViewById(R.id.create_account_login_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // already coming from LoginActivity
                finish();
            }
        });

        funnel = new CreateAccountFunnel(WikipediaApp.getInstance(),
                getIntent().getStringExtra(LOGIN_REQUEST_SOURCE));

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart(getIntent().getStringExtra(LOGIN_SESSION_TOKEN));
        }
        // Set default result to failed, so we can override if it did not
        setResult(RESULT_ACCOUNT_NOT_CREATED);
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("result", createAccountResult);
    }

    public void showPrivacyPolicy(View v) {
        FeedbackUtil.showPrivacyPolicy(this);
    }

    public void handleAccountCreationError(@NonNull String message) {
        FeedbackUtil.showMessage(this, message);
        L.w("Account creation failed with result " + message);
    }

    public void getCreateAccountInfo() {
        createAccountInfoClient.request(wiki, new CreateAccountInfoClient.Callback() {
            @Override
            public void success(@NonNull Call<MwQueryResponse<CreateAccountInfo>> call,
                                @NonNull CreateAccountInfoResult result) {
                if (result.token() == null) {
                    handleAccountCreationError(getString(R.string.create_account_generic_error));
                } else if (result.hasCaptcha()) {
                    captchaHandler.handleCaptcha(result.token(), new CaptchaResult(result.captchaId()));
                } else {
                    doCreateAccount(result.token());
                }
            }

            @Override
            public void failure(@NonNull Call<MwQueryResponse<CreateAccountInfo>> call,
                                @NonNull Throwable caught) {
                showError(caught);
                L.e(caught);
            }
        });
    }

    public void doCreateAccount(@NonNull String token) {
        progressDialog.show();

        String email = null;
        if (emailEdit.getText().length() != 0) {
            email = emailEdit.getText().toString();
        }
        String password = passwordEdit.getText().toString();
        String repeat = passwordInput.isPasswordVisible() ? password : passwordRepeatEdit.getText().toString();

        createAccountClient.request(wiki, usernameEdit.getText().toString(),
                password, repeat, token, email,
                captchaHandler.isActive() ? captchaHandler.captchaId() : "null",
                captchaHandler.isActive() ? captchaHandler.captchaWord() : "null",
                new CreateAccountClient.Callback() {
                    @Override
                    public void success(@NonNull Call<CreateAccountResponse> call,
                                        @NonNull final CreateAccountSuccessResult result) {
                        if (!progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        finishWithUserResult(result);

                    }

                    @Override
                    public void failure(@NonNull Call<CreateAccountResponse> call, @NonNull Throwable caught) {
                        L.e(caught.toString());
                        if (!progressDialog.isShowing()) {
                            // no longer attached to activity!
                            return;
                        }
                        progressDialog.dismiss();
                        if (caught instanceof CreateAccountException) {
                            handleAccountCreationError(caught.getMessage());
                        } else {
                            showError(caught);
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        hideSoftKeyboard(this);
        super.onBackPressed();
    }

    @Override
    public void onStop() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onStop();
    }

    private void finishWithUserResult(@NonNull CreateAccountSuccessResult result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CREATE_ACCOUNT_RESULT_USERNAME, result.getUsername());
        resultIntent.putExtra(CREATE_ACCOUNT_RESULT_PASSWORD, passwordEdit.getText().toString());
        setResult(RESULT_ACCOUNT_CREATED, resultIntent);

        createAccountResult = result;
        progressDialog.dismiss();
        captchaHandler.cancelCaptcha();
        funnel.logSuccess();
        hideSoftKeyboard(CreateAccountActivity.this);
        finish();
    }

    private void showError(@NonNull Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }
}
