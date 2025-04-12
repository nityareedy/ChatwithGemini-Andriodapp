package com.example.chatwithgemini;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class MainActivity extends AppCompatActivity {

    private EditText userInput;
    private ScrollView scrollView;
    private LinearLayout messageContainer;

    private static final String API_KEY = "";
    private static final String API_URL = "" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userInput = findViewById(R.id.user_input);
        scrollView = findViewById(R.id.scrollView);
        messageContainer = findViewById(R.id.message_container);
        ImageButton sendBtn = findViewById(R.id.send_button);

        sendBtn.setOnClickListener(v -> {
            String prompt = userInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                addMessage(prompt, true);
                sendPromptToGemini(prompt);
                userInput.setText("");
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        View messageView = getLayoutInflater().inflate(R.layout.message_item, null);
        TextView messageText = messageView.findViewById(R.id.message_text);
        ImageView icon = messageView.findViewById(R.id.icon);

        messageText.setText(text);
        icon.setImageResource(isUser ? android.R.drawable.sym_def_app_icon : android.R.drawable.ic_dialog_info);

        messageContainer.addView(messageView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendPromptToGemini(String prompt) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();

        try {
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            json.put("contents", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder().url(API_URL).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addMessage("❌ Error: " + e.getMessage(), false));
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String res = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(res);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        JSONObject first = candidates.getJSONObject(0);
                        JSONObject content = first.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String reply = parts.getJSONObject(0).getString("text");

                        runOnUiThread(() -> addMessage(reply, false));
                    } catch (Exception e) {
                        runOnUiThread(() -> addMessage("❗ Parsing error: " + e.getMessage(), false));
                    }
                } else {
                    runOnUiThread(() -> addMessage("⚠️ API Error: " + response.message(), false));
                }
            }
        });
    }
}
