package com.nexusisekai.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nexusisekai.BuildConfig;
import com.nexusisekai.R;
import com.nexusisekai.game.GameViewModel;
import com.nexusisekai.net.GameClient;
import com.nexusisekai.net.PacketWriter;

public class LoginActivity extends AppCompatActivity {

    private GameViewModel vm;

    // Login form
    private EditText etUser, etPass;
    private Button   btnLogin, btnToRegister;
    private TextView tvStatus;

    // Register form
    private View     registerGroup;
    private EditText etRegUser, etRegPass, etRegEmail;
    private Button   btnRegister, btnToLogin;

    private boolean showingRegister = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        vm = new ViewModelProvider(this).get(GameViewModel.class);

        // Bind views
        etUser       = findViewById(R.id.etUsername);
        etPass       = findViewById(R.id.etPassword);
        btnLogin     = findViewById(R.id.btnLogin);
        btnToRegister= findViewById(R.id.btnToRegister);
        tvStatus     = findViewById(R.id.tvStatus);
        registerGroup= findViewById(R.id.registerGroup);
        etRegUser    = findViewById(R.id.etRegUsername);
        etRegPass    = findViewById(R.id.etRegPassword);
        etRegEmail   = findViewById(R.id.etRegEmail);
        btnRegister  = findViewById(R.id.btnRegister);
        btnToLogin   = findViewById(R.id.btnToLogin);

        registerGroup.setVisibility(View.GONE);

        // Observe
        vm.loginError.observe(this, err -> {
            if (err != null) {
                tvStatus.setText(err);
                tvStatus.setTextColor(getColor(android.R.color.holo_red_light));
                setLoading(false);
            }
        });

        vm.charSlots.observe(this, slots -> {
            if (slots != null) {
                startActivity(new Intent(this, CharSelectActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        vm.notification.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Buttons
        btnLogin.setOnClickListener(v -> doLogin());
        btnToRegister.setOnClickListener(v -> toggleRegister(true));
        btnRegister.setOnClickListener(v -> doRegister());
        btnToLogin.setOnClickListener(v -> toggleRegister(false));

        // Connect to server
        tvStatus.setText("Đang kết nối...");
        GameClient.getInstance().connect(BuildConfig.SERVER_HOST, BuildConfig.SERVER_PORT);
        tvStatus.postDelayed(() -> tvStatus.setText("Đã kết nối. Nhập tài khoản."), 2000);
    }

    private void toggleRegister(boolean showReg) {
        showingRegister = showReg;
        registerGroup.setVisibility(showReg ? View.VISIBLE : View.GONE);
        etUser.setVisibility(showReg ? View.GONE : View.VISIBLE);
        etPass.setVisibility(showReg ? View.GONE : View.VISIBLE);
        btnLogin.setVisibility(showReg ? View.GONE : View.VISIBLE);
        btnToRegister.setVisibility(showReg ? View.GONE : View.VISIBLE);
    }

    private void doLogin() {
        String user = etUser.getText().toString().trim();
        String pass = etPass.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) {
            tvStatus.setText("Nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }
        setLoading(true);
        tvStatus.setText("Đang đăng nhập...");
        GameClient.getInstance().send(PacketWriter.login(user, pass));
    }

    private void doRegister() {
        String user  = etRegUser.getText().toString().trim();
        String pass  = etRegPass.getText().toString();
        String email = etRegEmail.getText().toString().trim();
        if (user.length() < 3 || pass.length() < 6) {
            Toast.makeText(this, "Username ≥3 ký tự, password ≥6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        GameClient.getInstance().send(PacketWriter.register(user, pass, email));
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
    }
}
