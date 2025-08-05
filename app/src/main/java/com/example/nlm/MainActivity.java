
package com.example.nlm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private TextInputEditText inputText;
    private MaterialButton analyzeBtn, historyBtn;
    private TextView characterCount, primarySentiment, confidenceText, detailedResults, supportiveMessage;
    private LinearLayout loadingLayout;
    private MaterialCardView resultCard;
    private ImageView resultIcon;
    private LinearProgressIndicator confidenceBar;
    private Chip chipHappy, chipNeutral, chipStressed;

    // API Configuration
    private static final String HUGGING_FACE_API_KEY = "hf_aC";
    private static final String API_URL = "https://router.huggingface.co/hf-inference/models/distilbert/distilbert-base-uncased-finetuned-sst-2-english";

    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json");
    private final DecimalFormat percentFormat = new DecimalFormat("##.#");

    // Sentiment response templates
    private final Map<String, String[]> sentimentResponses = new HashMap<String, String[]>() {{
        put("POSITIVE", new String[]{
                "That's wonderful to hear! Your positive energy really shines through. 🌟",
                "I'm glad you're feeling great! Keep embracing those good vibes. ✨",
                "Your optimism is inspiring! It's beautiful to see such positivity. 😊",
                "What a lovely sentiment! Your happiness is contagious. 🌈"
        });
        put("NEGATIVE", new String[]{
                "I hear you, and it's okay to feel this way sometimes. Remember, tough times don't last, but resilient people do. 💙",
                "Thank you for sharing your feelings. It takes courage to express difficult emotions. You're not alone. 🤗",
                "I understand you're going through a challenging time. Be gentle with yourself - you deserve compassion. 🌸",
                "Your feelings are valid. Consider reaching out to someone you trust or taking small steps toward self-care. 💚"
        });
        put("NEUTRAL", new String[]{
                "Thanks for sharing your thoughts. Sometimes neutral days are perfectly okay too. 🌤️",
                "I appreciate your honesty. Balanced emotions show emotional awareness and stability. ⚖️",
                "It's perfectly fine to have calm, steady days. Not every day needs to be extraordinary. 🍃",
                "Your measured perspective is refreshing. Sometimes the middle ground is exactly where we need to be. 🌿"
        });
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupEventListeners();
        setupChipSuggestions();
    }

    private void initializeViews() {
        inputText = findViewById(R.id.inputText);
        analyzeBtn = findViewById(R.id.analyzeBtn);
        historyBtn = findViewById(R.id.historyBtn);
        characterCount = findViewById(R.id.characterCount);
        loadingLayout = findViewById(R.id.loadingLayout);
        resultCard = findViewById(R.id.resultCard);
        primarySentiment = findViewById(R.id.primarySentiment);
        confidenceText = findViewById(R.id.confidenceText);
        detailedResults = findViewById(R.id.detailedResults);
        supportiveMessage = findViewById(R.id.supportiveMessage);
        resultIcon = findViewById(R.id.resultIcon);
        confidenceBar = findViewById(R.id.confidenceBar);
        chipHappy = findViewById(R.id.chipHappy);
        chipNeutral = findViewById(R.id.chipNeutral);
        chipStressed = findViewById(R.id.chipStressed);
    }

    private void setupEventListeners() {
        // Character counter
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                characterCount.setText(length + "/500");

                // Enable/disable analyze button
                analyzeBtn.setEnabled(length > 0 && length <= 500);

                // Change color based on character count
                if (length > 450) {
                    characterCount.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_dark));
                } else if (length > 400) {
                    characterCount.setTextColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_orange_dark));
                } else {
                    characterCount.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.secondary_text));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Analyze button click
        analyzeBtn.setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (!text.isEmpty()) {
                analyzeSentiment(text);
            }
        });

        // History button click
        historyBtn.setOnClickListener(v -> {
            // TODO: Implement history view
            showMessage("History feature coming soon!");
        });
    }

    private void setupChipSuggestions() {
        chipHappy.setOnClickListener(v -> {
            inputText.setText(chipHappy.getText());
            inputText.setSelection(inputText.getText().length());
        });

        chipNeutral.setOnClickListener(v -> {
            inputText.setText(chipNeutral.getText());
            inputText.setSelection(inputText.getText().length());
        });

        chipStressed.setOnClickListener(v -> {
            inputText.setText(chipStressed.getText());
            inputText.setSelection(inputText.getText().length());
        });
    }

    private void analyzeSentiment(String text) {
        showLoading(true);
        hideResults();

        try {
            JSONObject json = new JSONObject();
            json.put("inputs", text);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + HUGGING_FACE_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Connection failed. Please check your internet connection and try again.");
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> showLoading(false));

                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            if (response.code() == 503) {
                                showError("AI model is currently loading. Please wait a moment and try again.");
                            } else {
                                showError("Analysis failed. Please try again later.");
                            }
                        });
                        return;
                    }

                    final String responseBody = response.body().string();
                    runOnUiThread(() -> parseAndDisplayResult(responseBody));
                }
            });

        } catch (JSONException e) {
            showLoading(false);
            showError("Request error. Please try again.");
        }
    }

    private void parseAndDisplayResult(String responseBody) {
        try {
            JSONArray outerArray = new JSONArray(responseBody);
            JSONArray sentimentArray = outerArray.getJSONArray(0);

            if (sentimentArray.length() == 0) {
                showError("No sentiment data received. Please try again.");
                return;
            }

            // Find the highest confidence sentiment
            JSONObject topSentiment = null;
            double highestScore = 0;

            for (int i = 0; i < sentimentArray.length(); i++) {
                JSONObject sentiment = sentimentArray.getJSONObject(i);
                double score = sentiment.getDouble("score");
                if (score > highestScore) {
                    highestScore = score;
                    topSentiment = sentiment;
                }
            }

            if (topSentiment != null) {
                displaySentimentResult(topSentiment, sentimentArray);
            } else {
                showError("Unable to analyze sentiment. Please try again.");
            }

        } catch (JSONException e) {
            showError("Error processing results: " + e.getMessage());
        }
    }

    private void displaySentimentResult(JSONObject topSentiment, JSONArray allSentiments) {
        try {
            String label = topSentiment.getString("label");
            double score = topSentiment.getDouble("score");

            // Normalize label names
            String displayLabel = normalizeSentimentLabel(label);
            String sentimentColor = getSentimentColor(label);

            // Set primary sentiment
            primarySentiment.setText("Primary Sentiment: " + displayLabel);
            primarySentiment.setTextColor(ContextCompat.getColor(this, getSentimentColorResource(label)));

            // Set confidence
            confidenceText.setText("Confidence: " + percentFormat.format(score * 100) + "%");
            confidenceBar.setProgress((int) (score * 100));

            // Set appropriate icon
            resultIcon.setImageResource(getSentimentIcon(label));
            resultIcon.setColorFilter(ContextCompat.getColor(this, getSentimentColorResource(label)));

            // Build detailed results
            StringBuilder detailBuilder = new StringBuilder();
            detailBuilder.append("Detailed Analysis:\n\n");

            for (int i = 0; i < allSentiments.length(); i++) {
                JSONObject sentiment = allSentiments.getJSONObject(i);
                String sentimentLabel = normalizeSentimentLabel(sentiment.getString("label"));
                double sentimentScore = sentiment.getDouble("score");

                detailBuilder.append("• ").append(sentimentLabel).append(": ")
                        .append(percentFormat.format(sentimentScore * 100)).append("%\n");
            }

            detailedResults.setText(detailBuilder.toString());

            // Set supportive message
            String[] responses = sentimentResponses.get(label.toUpperCase());
            if (responses != null && responses.length > 0) {
                int randomIndex = (int) (Math.random() * responses.length);
                supportiveMessage.setText(responses[randomIndex]);
            } else {
                supportiveMessage.setText("Thank you for sharing your thoughts with me. 💙");
            }

            showResults();

        } catch (JSONException e) {
            showError("Error displaying results: " + e.getMessage());
        }
    }

    private String normalizeSentimentLabel(String label) {
        switch (label.toUpperCase()) {
            case "LABEL_0":
            case "NEGATIVE":
                return "Negative";
            case "LABEL_1":
            case "NEUTRAL":
                return "Neutral";
            case "LABEL_2":
            case "POSITIVE":
                return "Positive";
            default:
                return label;
        }
    }

    private String getSentimentColor(String label) {
        switch (label.toUpperCase()) {
            case "LABEL_0":
            case "NEGATIVE":
                return "negative";
            case "LABEL_1":
            case "NEUTRAL":
                return "neutral";
            case "LABEL_2":
            case "POSITIVE":
                return "positive";
            default:
                return "neutral";
        }
    }

    private int getSentimentColorResource(String label) {
        switch (label.toUpperCase()) {
            case "LABEL_0":
            case "NEGATIVE":
                return R.color.sentiment_negative;
            case "LABEL_1":
            case "NEUTRAL":
                return R.color.sentiment_neutral;
            case "LABEL_2":
            case "POSITIVE":
                return R.color.sentiment_positive;
            default:
                return R.color.primary_text;
        }
    }

    private int getSentimentIcon(String label) {
        switch (label.toUpperCase()) {
            case "LABEL_0":
            case "NEGATIVE":
                return R.drawable.ic_sentiment_sad;
            case "LABEL_1":
            case "NEUTRAL":
                return R.drawable.ic_sentiment_neutral;
            case "LABEL_2":
            case "POSITIVE":
                return R.drawable.ic_sentiment_happy;
            default:
                return R.drawable.ic_sentiment_neutral;
        }
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        analyzeBtn.setEnabled(!show);

        if (show) {
            analyzeBtn.setText("Analyzing...");
        } else {
            analyzeBtn.setText("Analyze Sentiment");
        }
    }

    private void showResults() {
        resultCard.setVisibility(View.VISIBLE);
        animateCardEntry(resultCard);
    }

    private void hideResults() {
        resultCard.setVisibility(View.GONE);
    }

    private void showError(String message) {
        hideResults();
        // For now, we'll use the supportive message area for errors
        // In a full implementation, you'd want a proper error display
        showMessage(message);
    }

    private void showMessage(String message) {
        // Simple toast-like message - in a full app, use Snackbar or custom dialog
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }

    private void animateCardEntry(View view) {
        view.setAlpha(0f);
        view.setTranslationY(50f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeIn, slideUp);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }
}