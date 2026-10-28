package com.pocketforge.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private LinearLayout topBarLayout;
    private TextView tvAppTitle;
    private EditText codeEditor;
    private WebView miniAppWebView;
    private Button btnToggleView;
    private Button btnTheme;
    private Button btnInstallShortcut;

    private boolean isRunning = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("PocketForgePrefs", MODE_PRIVATE);

        rootLayout = findViewById(R.id.rootLayout);
        topBarLayout = findViewById(R.id.topBarLayout);
        tvAppTitle = findViewById(R.id.tvAppTitle);
        codeEditor = findViewById(R.id.codeEditor);
        miniAppWebView = findViewById(R.id.miniAppWebView);
        btnToggleView = findViewById(R.id.btnToggleView);
        btnTheme = findViewById(R.id.btnTheme);
        btnInstallShortcut = findViewById(R.id.btnInstallShortcut);

        setupSandboxWebView();
        setupBackNavigation();

        // Load saved code (blank by default, no example code)
        String savedCode = prefs.getString("saved_code", "");
        codeEditor.setText(savedCode);

        // Auto-save whatever you type
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                prefs.edit().putString("saved_code", s.toString()).apply();
            }
        });

        // Apply saved theme
        int savedTheme = prefs.getInt("app_theme", 0);
        applyTheme(savedTheme);

        btnTheme.setOnClickListener(v -> showThemeDialog());

        btnToggleView.setOnClickListener(v -> {
            if (isRunning) {
                switchToEditor();
            } else {
                runMiniApp(codeEditor.getText().toString());
            }
        });

        btnInstallShortcut.setOnClickListener(v -> showInstallDialog());

        // Launch directly if opened from home screen icon
        String launchedCode = getIntent().getStringExtra("EXTRA_MINI_APP_CODE");
        if (launchedCode != null && !launchedCode.isEmpty()) {
            codeEditor.setText(launchedCode);
            runMiniApp(launchedCode);
        }
    }

    private void showThemeDialog() {
        String[] themes = {"Midnight Slate (Default)", "Cyberpunk Neon", "Clean Light"};
        int currentTheme = prefs.getInt("app_theme", 0);

        new AlertDialog.Builder(this)
                .setTitle("Select App Look")
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    prefs.edit().putInt("app_theme", which).apply();
                    applyTheme(which);
                    dialog.dismiss();
                })
                .show();
    }

    private void applyTheme(int themeIndex) {
        if (themeIndex == 1) {
            // Cyberpunk Neon
            rootLayout.setBackgroundColor(Color.parseColor("#050811"));
            topBarLayout.setBackgroundColor(Color.parseColor("#050811"));
            tvAppTitle.setTextColor(Color.parseColor("#00E5FF"));
            codeEditor.setBackgroundColor(Color.parseColor("#0A0F1D"));
            codeEditor.setTextColor(Color.parseColor("#00FF66"));
            codeEditor.setHintTextColor(Color.parseColor("#37474F"));
            btnTheme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#152238")));
            btnInstallShortcut.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#152238")));
            btnToggleView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00E5FF")));
            btnToggleView.setTextColor(Color.parseColor("#000000"));
        } else if (themeIndex == 2) {
            // Clean Light
            rootLayout.setBackgroundColor(Color.parseColor("#F3F4F6"));
            topBarLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
            tvAppTitle.setTextColor(Color.parseColor("#111827"));
            codeEditor.setBackgroundColor(Color.parseColor("#FFFFFF"));
            codeEditor.setTextColor(Color.parseColor("#1F2937"));
            codeEditor.setHintTextColor(Color.parseColor("#9CA3AF"));
            btnTheme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
            btnTheme.setTextColor(Color.parseColor("#374151"));
            btnInstallShortcut.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E7EB")));
            btnInstallShortcut.setTextColor(Color.parseColor("#374151"));
            btnToggleView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
            btnToggleView.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            // Midnight Slate (Default)
            rootLayout.setBackgroundColor(Color.parseColor("#121318"));
            topBarLayout.setBackgroundColor(Color.parseColor("#121318"));
            tvAppTitle.setTextColor(Color.parseColor("#FFFFFF"));
            codeEditor.setBackgroundColor(Color.parseColor("#1A1C23"));
            codeEditor.setTextColor(Color.parseColor("#F3F4F6"));
            codeEditor.setHintTextColor(Color.parseColor("#6B7280"));
            btnTheme.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#242630")));
            btnTheme.setTextColor(Color.parseColor("#D1D5DB"));
            btnInstallShortcut.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#242630")));
            btnInstallShortcut.setTextColor(Color.parseColor("#D1D5DB"));
            btnToggleView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
            btnToggleView.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isRunning) {
                    switchToEditor();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupSandboxWebView() {
        WebSettings settings = miniAppWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDomStorageEnabled(true);

        miniAppWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });
    }

    private void runMiniApp(String jsCode) {
        isRunning = true;
        topBarLayout.setVisibility(View.GONE);
        codeEditor.setVisibility(View.GONE);
        miniAppWebView.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Swipe or tap Back to return", Toast.LENGTH_SHORT).show();

        String encodedCode = Base64.encodeToString(jsCode.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
                "<style>" +
                "  html, body { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; background: #0E0E10; color: #FFF; font-family: sans-serif; }" +
                "  #root { width: 100%; height: 100%; display: flex; flex-direction: column; gap: 8px; padding: 12px; box-sizing: border-box; }" +
                "  canvas { display: block; width: 100%; height: 100%; touch-action: none; }" +
                "  button { padding: 12px; font-size: 16px; border: none; border-radius: 8px; background: #6366F1; color: white; }" +
                "  input { padding: 12px; font-size: 16px; border: 1px solid #333; border-radius: 8px; background: #1C1C22; color: #FFF; }" +
                "  p { font-size: 16px; margin: 0; }" +
                "</style></head><body>" +
                "<div id='root'></div>" +
                "<script>" +
                "  const PocketForge = {" +
                "    createCanvas: function() {" +
                "      document.getElementById('root').style.padding = '0';" +
                "      const c = document.createElement('canvas');" +
                "      c.width = window.innerWidth;" +
                "      c.height = window.innerHeight;" +
                "      document.getElementById('root').appendChild(c);" +
                "      return c;" +
                "    }," +
                "    addText: function(msg) {" +
                "      const p = document.createElement('p');" +
                "      p.textContent = msg;" +
                "      document.getElementById('root').appendChild(p);" +
                "      return p;" +
                "    }," +
                "    addInput: function(placeholder) {" +
                "      const inp = document.createElement('input');" +
                "      inp.placeholder = placeholder || '';" +
                "      document.getElementById('root').appendChild(inp);" +
                "      return inp;" +
                "    }," +
                "    addButton: function(label, callback) {" +
                "      const btn = document.createElement('button');" +
                "      btn.textContent = label;" +
                "      btn.onclick = callback;" +
                "      document.getElementById('root').appendChild(btn);" +
                "      return btn;" +
                "    }" +
                "  };" +
                "  try {" +
                "    const userScript = decodeURIComponent(escape(window.atob('" + encodedCode + "')));" +
                "    new Function(userScript)();" +
                "  } catch(e) {" +
                "    PocketForge.addText('Error: ' + e.message);" +
                "  }" +
                "</script></body></html>";

        miniAppWebView.loadDataWithBaseURL("https://pocketforge.local", html, "text/html", "UTF-8", null);
    }

    private void switchToEditor() {
        isRunning = false;
        miniAppWebView.setVisibility(View.GONE);
        topBarLayout.setVisibility(View.VISIBLE);
        codeEditor.setVisibility(View.VISIBLE);
    }

    private void showInstallDialog() {
        EditText inputName = new EditText(this);
        inputName.setHint("Mini-App Name");

        new AlertDialog.Builder(this)
                .setTitle("Install to Home Screen")
                .setMessage("Enter a name for this mini-app:")
                .setView(inputName)
                .setPositiveButton("Install", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    if (name.isEmpty()) name = "Mini-App";
                    createHomeScreenShortcut(name, codeEditor.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createHomeScreenShortcut(String appName, String code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent shortcutIntent = new Intent(this, MainActivity.class);
                shortcutIntent.setAction(Intent.ACTION_VIEW);
                shortcutIntent.putExtra("EXTRA_MINI_APP_CODE", code);

                Bitmap iconBitmap = generateCustomIcon(appName);

                ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, "mini_app_" + System.currentTimeMillis())
                        .setShortLabel(appName)
                        .setIcon(Icon.createWithBitmap(iconBitmap))
                        .setIntent(shortcutIntent)
                        .build();

                shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                Toast.makeText(this, "Shortcut added to Home Screen!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Launcher doesn't support pin shortcuts", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap generateCustomIcon(String title) {
        int size = 192;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#6366F1")); // Indigo Accent
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(96f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        String letter = title.isEmpty() ? "P" : title.substring(0, 1).toUpperCase();
        float yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(letter, size / 2f, yPos, textPaint);

        return bitmap;
    }
}
