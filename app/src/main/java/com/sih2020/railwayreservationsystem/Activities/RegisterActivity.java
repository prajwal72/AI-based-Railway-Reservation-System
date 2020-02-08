package com.sih2020.railwayreservationsystem.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sih2020.railwayreservationsystem.R;
import com.sih2020.railwayreservationsystem.Utils.AppConstants;
import com.sih2020.railwayreservationsystem.Utils.GenerateBackground;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private TextView registerButton, registerBack, captchaRegister;
    private EditText email, username, password, confirmPassword,
            firstName, middleName, lastName, dob, phoneNoEditText, enterCaptchaRegister;
    private ImageView calenderIcon, refreshRegister;
    private Spinner nationality, occupation, maritalStatus;
    private RadioGroup radioGroup;
    private RadioButton male, female, transGender;
    private ScrollView rootLayout;

    private String phoneno, codeSent, mGender = "";
    private DatePickerDialog.OnDateSetListener mDate;
    private Calendar mCalendar;

    private DatabaseReference dref, pref;
    private FirebaseAuth mauth;

    private AlertDialog alertDialogPhoneNo, alertDialogOtp;
    private ProgressDialog progressDialog;

    private ArrayAdapter<String> nationalityAdapter, occupationAdapter, maritalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dref = FirebaseDatabase.getInstance().getReference("Users");
        pref = FirebaseDatabase.getInstance().getReference("Phone");
        init();
        setUpSpinners();
        receiveClicks();
        //takePhoneNoInput();


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            finish();
        }
        setRandomCaptcha();
    }

    private void setRandomCaptcha() {
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 48; i <= 57; i++)
            values.add(i);
        for (int i = 65; i <= 90; i++)
            values.add(i);
        for (int i = 97; i <= 122; i++)
            values.add(i);
        String lpassword = "";
        while (lpassword.length() < 6) {
            int x = values.get((int) (Math.random() * values.size()));
            char ch = (char) x;
            lpassword = lpassword + Character.toString(ch);
        }
        captchaRegister.setText(lpassword);
    }

    private void setUpSpinners() {
        nationalityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AppConstants.mNationality);
        nationality.setAdapter(nationalityAdapter);

        occupationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AppConstants.mOccupation);
        occupation.setAdapter(occupationAdapter);

        maritalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AppConstants.mMaritalStatus);
        maritalStatus.setAdapter(maritalAdapter);
    }

    private void receiveClicks() {
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();

                pref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean flag2 = false;
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.getKey().equals("+91" + phoneNoEditText.getText().toString())) {
                                Log.e("onDataChange: ", "levelofquality");
                                flag2 = true;
                                break;
                            }
                        }
                        if (!flag2) {
                            if (validate()) {
                                sendVerificationCode();
                            }
                            else {
                                progressDialog.dismiss();
                            }
                        } else {
                            phoneNoEditText.setError("A user exists with this phone no");
                            progressDialog.dismiss();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        registerBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId == R.id.male) {
                    mGender = "Male";
                } else if (selectedId == R.id.female) {
                    mGender = "Female";
                } else {
                    mGender = "Transgender";
                }
            }
        });

        refreshRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setRandomCaptcha();
                    }
                }, 1000);
            }
        });

        calenderIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpDateAlert();
            }
        });

        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpDateAlert();
            }
        });
    }

    private void registerUser() {
        HashMap<String, String> map = new HashMap();
        map.put("uid", mauth.getCurrentUser().getUid());
        map.put("mobile", mauth.getCurrentUser().getPhoneNumber());
        map.put("email", email.getText().toString().trim());
        map.put("username", username.getText().toString().trim());
        //map.put("password",password.getText().toString().trim());
        map.put("name", firstName.getText().toString().trim() + middleName.getText().toString().trim() +
                lastName.getText().toString().trim());
        map.put("dob", dob.getText().toString().trim());
        map.put("gender", mGender);
        map.put("nationality", AppConstants.mNationality[nationality.getSelectedItemPosition()]);
        map.put("occupation", AppConstants.mOccupation[occupation.getSelectedItemPosition()]);
        map.put("maritalStatus", AppConstants.mMaritalStatus[maritalStatus.getSelectedItemPosition()]);

        dref.child(mauth.getCurrentUser().getUid()).setValue(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Map<String, Object> temMap = new HashMap<>();
                            temMap.put(mauth.getCurrentUser().getPhoneNumber(), mauth.getCurrentUser().getUid());

                            pref.updateChildren(temMap)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Toast.makeText(RegisterActivity.this, "Registered Succesfully", Toast.LENGTH_SHORT).show();
                                            if (progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }
                                            finish();
                                        }
                                    });

                        } else {
                            Toast.makeText(RegisterActivity.this, "Registeration failed", Toast.LENGTH_SHORT).show();
                            Log.e("onComplete: ", task.getException().getMessage());
                            mauth.signOut();
                        }
                    }
                });
    }

    private boolean validate() {
        if (phoneNoEditText.getText().toString().trim().length() != 10) {
            phoneNoEditText.setError("Enter a 10 digit phone no");
            return false;
        } else if (email.getText().toString().trim().length() == 0) {
            email.setError("Enter valid email address");
            return false;
        } else if (email.getText().toString().trim().matches("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")) {
            email.setError("Enter a valid email address");
            return false;
        } else if (username.getText().toString().trim().length() == 0) {
            username.setError("Enter a user name");
            return false;
        }
//        else if(password.getText().toString().trim().length()>=6){
//            password.setError("Password length must be atleast 6 characters");
//            return false;
//        }
//        else if(confirmPassword.getText().toString().trim().equals(password.getText().toString().trim())) {
//            confirmPassword.setError("Confirm password must be same as password entered");
//            return false;
//        }
        else if (firstName.getText().toString().trim().length() == 0) {
            firstName.setError("Enter your first name");
            return false;
        } else if (dob.getText().toString().trim().length() == 0) {
            dob.setError("Enter date of birth");
            return false;
        } else if (mGender.equals("")) {
            Toast.makeText(this, "Select gender", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!enterCaptchaRegister.getText().toString().equals(captchaRegister.getText().toString())) {
            enterCaptchaRegister.setError("Enter correct captcha");
            return false;
        }
        return true;
    }

    private void takePhoneNoInput() {
        Rect displayRectangle = new Rect();
        Window window = RegisterActivity.this.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.mobile_dialog, viewGroup, false);

        dialogView.findViewById(R.id.upper_text).setBackground(GenerateBackground.generateBackground());
        TextView sendOtp = dialogView.findViewById(R.id.send_otp);
        final EditText enterPhone = dialogView.findViewById(R.id.enter_phone_no_et);

        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        alertDialogPhoneNo = builder.create();
        alertDialogPhoneNo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialogPhoneNo.setCancelable(false);
        alertDialogPhoneNo.show();

        sendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneno = enterPhone.getText().toString().trim();
                if (phoneno.equals("")) {
                    enterPhone.setError("Enter Phone Number");
                    enterPhone.requestFocus();
                    return;
                }
                if (phoneno.length() != 10) {
                    enterPhone.setError("Enter a valid 10 digit Phone Number");
                    enterPhone.requestFocus();
                    return;
                }

                sendVerificationCode();
                alertDialogPhoneNo.hide();
            }
        });
    }

    private void sendVerificationCode() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + phoneNoEditText.getText().toString().trim(),        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.e("onVerificationFailed: ", e.getMessage());

                }

                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    super.onCodeSent(s, forceResendingToken);
                    codeSent = s;


                    progressDialog.dismiss();
                    openEnterOtpDialog();
                }
            };

    private void openEnterOtpDialog() {
        Rect displayRectangle = new Rect();
        Window window = RegisterActivity.this.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.mobile_dialog, viewGroup, false);

        TextView upper_text = dialogView.findViewById(R.id.upper_text);
        upper_text.setBackground(GenerateBackground.generateBackground());
        upper_text.setText("User Registration : Step 2");

        TextView sendOtp = dialogView.findViewById(R.id.send_otp);

        final EditText enterOtp = dialogView.findViewById(R.id.enter_phone_no_et);
        enterOtp.setHint("Enter OTP");

        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        alertDialogOtp = builder.create();
        alertDialogOtp.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        alertDialogOtp.setCancelable(false);
        alertDialogOtp.show();

        sendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                String enteredOtp = enterOtp.getText().toString().trim();

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeSent, enteredOtp);
                signInWithPhoneAuthCredential(credential);
                alertDialogOtp.hide();
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mauth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.e("level", "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();

                            registerUser();

                            //mauth.signOut();
                        } else {
                            finish();
                            Log.e("level", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    private void init() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        mauth = FirebaseAuth.getInstance();

        captchaRegister = findViewById(R.id.captcha_register);
        refreshRegister = findViewById(R.id.refresh_captcha_register);
        enterCaptchaRegister = findViewById(R.id.enter_captcha_register);

        rootLayout = findViewById(R.id.register_root_layout);
        rootLayout.setBackground(GenerateBackground.generateBackground());

        registerButton = findViewById(R.id.register_button);
        registerButton.setBackground(GenerateBackground.generateBackground());

        registerBack = findViewById(R.id.register_back);
        phoneNoEditText = findViewById(R.id.phone_no_et_ra);
        email = findViewById(R.id.email_register);
        username = findViewById(R.id.user_name_register);
        password = findViewById(R.id.password_register);
        confirmPassword = findViewById(R.id.confirm_password_register);
        firstName = findViewById(R.id.first_name);
        middleName = findViewById(R.id.middle_name);
        lastName = findViewById(R.id.last_name);
        dob = findViewById(R.id.date_text_register);

        calenderIcon = findViewById(R.id.calender_image_register);
        nationality = findViewById(R.id.nationality);
        occupation = findViewById(R.id.occupation);
        maritalStatus = findViewById(R.id.marital_status);

        radioGroup = findViewById(R.id.radio_group);
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);
        transGender = findViewById(R.id.transgender);

        mCalendar = Calendar.getInstance();
        mDate = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateJourneyDate();
            }
        };
    }

    private void setUpDateAlert() {
        mCalendar.setTime(AppConstants.mDate);
        DatePickerDialog dialog = new DatePickerDialog(this, mDate,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateJourneyDate() {
        String myFormat = "MM/dd/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        Date date = null;
        try {
            date = sdf.parse(sdf.format(mCalendar.getTime()));
        } catch (Exception e) {
            Log.e("Level", e.getLocalizedMessage());
        }
        setDate(date);
    }

    private void setDate(Date date) {
        String dateToShow = (String) DateFormat.format("dd", AppConstants.mDate) +
                "/" + (String) DateFormat.format("MM", AppConstants.mDate) + "/" +
                (String) DateFormat.format("yyyy", AppConstants.mDate);
        dob.setText(dateToShow);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        if (alertDialogPhoneNo.isShowing()) {
//            alertDialogPhoneNo.dismiss();
//        } else
        if (alertDialogOtp.isShowing()) {
            alertDialogOtp.dismiss();
        }
        onBackPressed();
    }
}
