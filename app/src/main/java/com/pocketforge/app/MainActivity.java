package com.pocketforge.app;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private LinearLayout topBarLayout;
    private EditText codeEditor;
    private WebView miniAppWebView;
    private Button btnToggleView;
    private Button btnInstallShortcut;
    private boolean isRunning = false;

    // Starter template: An interactive full-screen bouncing ball game!
    private static final String DEFAULT_APP_CODE =
        "// Full-Screen Game Demo in PocketForge!\n" +
        "const canvas = PocketForge.createCanvas();\n" +
        "const ctx = canvas.getContext('2d');\n" +
        "\n" +
        "let x = canvas.width / 2;\n" +
        "let y = canvas.height / 2;\n" +
        "let dx = 4;\n" +
        "let dy = 4;\n" +
        "let radius = 25;\n" +
        "let color = '#2196F3';\n" +
        "\n" +
        "// Change ball color on screen tap\n" +
        "canvas.addEventListener('touchstart', (e) => {\n" +
        "    color = '#' + Math.floor(Math.random()*16777215).toString(16);\n" +
        "});\n" +
        "\n" +
        "function gameLoop() {\n" +
        "    ctx.clearRect(0, 0, canvas.width, canvas.height);\n" +
        "    \n" +
        "    // Draw ball\n" +
        "    ctx.beginPath();\n" +
        "    ctx.arc(x, y, radius, 0, Math.PI * 2);\n" +
        "    ctx.fillStyle = color;\n" +
        "    ctx.fill();\n" +
        "    ctx.closePath();\n" +
        "    \n" +
        "    // Bounce off walls\n" +
        "    if (x + dx > canvas.width - radius || x + dx < radius) dx = -dx;\n" +
        "    if (y + dy > canvas.height - radius || y + dy < radius) dy = -dy;\n" +
        "    x += dx;\n" +
        "    y += dy;\n" +
        "    \n" +
        "    requestAnimationFrame(gameLoop);\n" +
        "}\n" +
        "gameLoop();\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topBarLayout = findViewById(R.id.topBarLayout);
        codeEditor = findViewById(R.id.codeEditor);
        miniAppWebView = findViewById(R.id.miniAppWebView);
        btnToggleView = findViewById(R.id.btnToggleView);
        btnInstallShortcut = findViewById(R.id.btnInstallShortcut);

        setupSandboxWebView();
        setupBackNavigation();

        codeEditor.setText(DEFAULT_APP_CODE);

        btnToggleView.setOnClickListener(v -> {
            if (isRunning) {
                switchToEditor();
            } else {
                runMiniApp(codeEditor.getText().toString());
            }
        });

        btnInstallShortcut.setOnClickListener(v -> showInstallDialog());

        String launchedCode = getIntent().getStringExtra("EXTRA_MINI_APP_CODE");
        if (launchedCode != null && !launchedCode.isEmpty()) {
            codeEditor.setText(launchedCode);
            runMiniApp(launchedCode);
        }
    }

    private void setupBackNavigation() {
        // Pressing your phone's Back button exits the game and returns to Editor
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
        
        // Hide editor AND top buttons for total full-screen immersion
        topBarLayout.setVisibility(View.GONE);
        codeEditor.setVisibility(View.GONE);
        miniAppWebView.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Tap phone Back button to exit", Toast.LENGTH_SHORT).show();

        String encodedCode = Base64.encodeToString(jsCode.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
                "<style>" +
                "  html, body { width: 100%; height: 100%; margin: 0; padding: 0; overflow: hidden; background: #111; color: #FFF; font-family: sans-serif; }" +
                "  #root { width: 100%; height: 100%; display: flex; flex-direction: column; gap: 8px; padding: 8px; box-sizing: border-box; }" +
                "  canvas { display: block; width: 100%; height: 100%; touch-action: none; }" +
                "  button { padding: 12px; font-size: 16px; border: none; border-radius: 8px; background: #2196F3; color: white; }" +
                "  input { padding: 12px; font-size: 16px; border: 1px solid #CCC; border-radius: 8px; }" +
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
        btnToggleView.setText("Run Mini-App");
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
                Toast.makeText(this, "Shortcut requested! Check your Home Screen.", Toast.LENGTH_SHORT).show();
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
        bgPaint.setColor(Color.parseColor("#4CAF50"));
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
