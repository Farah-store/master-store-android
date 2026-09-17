package com.masterstore.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();

    private LinearLayout productsContainer;
    private ProgressBar loadingBar;

    private static final String PRODUCTS_URL =
            "https://master4store.com/wp-json/wc/store/v1/products?per_page=20";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        productsContainer = findViewById(R.id.productsContainer);
        loadingBar = findViewById(R.id.loadingBar);

        loadProducts();
    }

    private void loadProducts() {

        loadingBar.setVisibility(View.VISIBLE);

        Request request = new Request.Builder()
                .url(PRODUCTS_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() -> {

                    loadingBar.setVisibility(View.GONE);

                    Toast.makeText(
                            MainActivity.this,
                            "تعذر الاتصال بالمتجر",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                if (!response.isSuccessful()) {

                    runOnUiThread(() -> {

                        loadingBar.setVisibility(View.GONE);

                        Toast.makeText(
                                MainActivity.this,
                                "خطأ من WooCommerce: " + response.code(),
                                Toast.LENGTH_LONG
                        ).show();
                    });

                    return;
                }

                String json = response.body() != null
                        ? response.body().string()
                        : "[]";

                try {

                    JSONArray products = new JSONArray(json);

                    runOnUiThread(() -> {

                        loadingBar.setVisibility(View.GONE);

                        displayProducts(products);
                    });

                } catch (Exception e) {

                    runOnUiThread(() -> {

                        loadingBar.setVisibility(View.GONE);

                        Toast.makeText(
                                MainActivity.this,
                                "حدث خطأ في قراءة المنتجات",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }
        });
    }

    private void displayProducts(JSONArray products) {

        productsContainer.removeAllViews();

        if (products.length() == 0) {

            TextView empty = new TextView(this);

            empty.setText("لا توجد منتجات حالياً");
            empty.setTextSize(17);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(20, 40, 20, 40);

            productsContainer.addView(empty);

            return;
        }

        for (int i = 0; i < products.length(); i++) {

            try {

                JSONObject product = products.getJSONObject(i);

                String name = product.optString(
                        "name",
                        "منتج"
                );

                JSONObject prices =
                        product.optJSONObject("prices");

                String price = "";

                if (prices != null) {

                    String rawPrice =
                            prices.optString("price", "");

                    String symbol =
                            prices.optString(
                                    "currency_symbol",
                                    ""
                            );

                    price = formatPrice(rawPrice)
                            + " "
                            + symbol;
                }

                boolean inStock =
                        product.optBoolean(
                                "is_in_stock",
                                true
                        );

                addProductCard(
                        name,
                        price,
                        inStock
                );

            } catch (Exception ignored) {
            }
        }
    }

    private void addProductCard(
            String name,
            String price,
            boolean inStock
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                20,
                20,
                20,
                20
        );

        card.setBackgroundColor(
                0xFFFFFFFF
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                18
        );

        card.setLayoutParams(cardParams);

        TextView nameText =
                new TextView(this);

        nameText.setText(name);
        nameText.setTextSize(18);
        nameText.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        nameText.setTextColor(
                0xFF172017
        );

        card.addView(nameText);

        TextView priceText =
                new TextView(this);

        priceText.setText(price);
        priceText.setTextSize(17);
        priceText.setTextColor(
                0xFF16A34A
        );

        LinearLayout.LayoutParams priceParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        priceParams.setMargins(
                0,
                10,
                0,
                10
        );

        priceText.setLayoutParams(priceParams);

        card.addView(priceText);

        TextView stockText =
                new TextView(this);

        if (inStock) {

            stockText.setText(
                    "متوفر"
            );

            stockText.setTextColor(
                    0xFF16A34A
            );

        } else {

            stockText.setText(
                    "غير متوفر"
            );

            stockText.setTextColor(
                    0xFFCC0000
            );
        }

        card.addView(stockText);

        Button cartButton =
                new Button(this);

        cartButton.setText(
                "إضافة إلى السلة"
        );

        cartButton.setEnabled(
                inStock
        );

        cartButton.setOnClickListener(
                v -> Toast.makeText(
                        MainActivity.this,
                        "سيتم إضافة المنتج للسلة",
                        Toast.LENGTH_SHORT
                ).show()
        );

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonParams.setMargins(
                0,
                12,
                0,
                0
        );

        cartButton.setLayoutParams(
                buttonParams
        );

        card.addView(cartButton);

        productsContainer.addView(card);
    }

    private String formatPrice(String value) {

        try {

            double number =
                    Double.parseDouble(value);

            if (number == Math.floor(number)) {

                return String.valueOf(
                        (long) number
                );
            }

            return String.format(
                    "%.2f",
                    number
            );

        } catch (Exception e) {

            return value;
        }
    }
}
