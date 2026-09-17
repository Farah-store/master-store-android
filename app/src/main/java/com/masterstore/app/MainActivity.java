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

import com.bumptech.glide.Glide;

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

    private String cartToken = null;

    private LinearLayout productsContainer;
    private ProgressBar loadingBar;

    private static final String PRODUCTS_URL =
            "https://master4store.com/wp-json/wc/store/v1/products?per_page=20";

    private static final String CART_URL =
            "https://master4store.com/wp-json/wc/store/v1/cart";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        productsContainer = findViewById(R.id.productsContainer);
        loadingBar = findViewById(R.id.loadingBar);

        loadProducts();
        getCartToken();
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

    private void getCartToken() {

        Request request = new Request.Builder()
                .url(CART_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "تعذر إنشاء جلسة السلة",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                String token = response.header("Cart-Token");

                if (token != null && !token.isEmpty()) {

                    cartToken = token;

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "السلة جاهزة ✓",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );

                } else {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "لم يتم الحصول على Cart-Token",
                                    Toast.LENGTH_LONG
                            ).show()
                    );
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

                JSONObject product =
                        products.getJSONObject(i);

                int productId =
                        product.optInt("id", 0);

                String name =
                        product.optString(
                                "name",
                                "منتج"
                        );

                JSONObject prices =
                        product.optJSONObject("prices");

                String price = "";

                if (prices != null) {

                    String rawPrice =
                            prices.optString(
                                    "price",
                                    ""
                            );

                    String symbol =
                            prices.optString(
                                    "currency_symbol",
                                    ""
                            );

                    price =
                            formatPrice(rawPrice)
                                    + " "
                                    + symbol;
                }

                boolean inStock =
                        product.optBoolean(
                                "is_in_stock",
                                true
                        );

                String imageUrl = "";

                JSONArray images =
                        product.optJSONArray("images");

                if (images != null
                        && images.length() > 0) {

                    JSONObject image =
                            images.getJSONObject(0);

                    imageUrl =
                            image.optString(
                                    "src",
                                    ""
                            );
                }

                addProductCard(
                        productId,
                        name,
                        price,
                        imageUrl,
                        inStock
                );

            } catch (Exception ignored) {
            }
        }
    }

    private void addProductCard(
            int productId,
            String name,
            String price,
            String imageUrl,
            boolean inStock
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                16,
                16,
                16,
                16
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

        ImageView productImage =
                new ImageView(this);

        productImage.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        220
                )
        );

        productImage.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        if (!imageUrl.isEmpty()) {

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(
                            android.R.drawable.ic_menu_gallery
                    )
                    .error(
                            android.R.drawable.ic_menu_gallery
                    )
                    .into(productImage);
        }

        card.addView(productImage);

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

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        nameParams.setMargins(
                0,
                14,
                0,
                0
        );

        nameText.setLayoutParams(nameParams);

        card.addView(nameText);

        TextView priceText =
                new TextView(this);

        priceText.setText(price);
        priceText.setTextSize(17);
        priceText.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
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
                8,
                0,
                8
        );

        priceText.setLayoutParams(priceParams);

        card.addView(priceText);

        TextView stockText =
                new TextView(this);

        if (inStock) {

            stockText.setText("● متوفر");
            stockText.setTextColor(0xFF16A34A);

        } else {

            stockText.setText("● غير متوفر");
            stockText.setTextColor(0xFFCC0000);
        }

        card.addView(stockText);

        Button cartButton =
                new Button(this);

        cartButton.setText(
                "🛒 إضافة إلى السلة"
        );

        cartButton.setEnabled(
                inStock
        );

        cartButton.setOnClickListener(
                v -> Toast.makeText(
                        MainActivity.this,
                        "سيتم ربط السلة في المرحلة القادمة",
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
