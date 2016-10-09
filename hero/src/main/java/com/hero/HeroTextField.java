package com.hero;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;

import com.hero.depandency.FormatUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuguoping on 15/9/23.
 */
public class HeroTextField extends EditText implements IHero {
    public final static int FORMAT_STYLE_NORMAL = 0;
    public final static int FORMAT_STYLE_BANKCARD = 1;
    public final static int FORMAT_STYLE_MONEY = 2;
    public final static int FORMAT_STYLE_FIX_NUMBER = 3;

    public final static int MAX_LENGTH_BANKCARD = 23;
    public final static int MAX_LENGTH_MONEY = 15;
    public final static int MAX_LENGTH_PHONE_NUMBER = 15;

    final HeroTextField self = this;
    private FocusChangeListener focusChangeListener = null;
    private int formatStyle;
    private boolean isFocused = false;

    public HeroTextField(Context context) {
        super(context);
        init();
        this.setTextSize(14.0f);
    }

    public HeroTextField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeroTextField(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HeroTextField(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        this.setBackgroundColor(Color.TRANSPARENT);
        formatStyle = FORMAT_STYLE_NORMAL;
        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                JSONObject json = HeroView.getJson(self);
                if (formatStyle != FORMAT_STYLE_NORMAL && s == getText() && s.length() > 0) {
                    String text = getText().toString();
                    try {
                        text = formatStyledText(text);
                        if (text != null) {
                            setText(text);
                            int index = getText().length();
                            setSelection(index);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (json != null && json.has("textFieldDidEditing")) {
                    try {
                        JSONObject end = json.getJSONObject("textFieldDidEditing");
                        end.put("value", self.getText().toString());
                        end.put("name", HeroView.getName(HeroTextField.this));
                        end.put("event", "UIControlEventEditingChanged");
                        ((IHeroContext) self.getContext()).on(end);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        this.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //                if (hasFocus) {
                //                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                //                    imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
                //                }
                if (focusChangeListener != null) {
                    focusChangeListener.focusChanged(hasFocus);
                }
                //                if (!hasFocus) {
                //                    try {
                //                        setText(formatTextOnEnd(getText().toString()));
                //                    } catch (Exception e) {
                //                        e.printStackTrace();
                //                    }
                //                }
                JSONObject json = HeroView.getJson(self);
                if (json != null) {
                    if (json.has("textFieldDidEndEditing") && !hasFocus) {
                        try {
                            JSONObject end = json.getJSONObject("textFieldDidEndEditing");
                            end.put("value", self.getText().toString());
                            end.put("name", HeroView.getName(HeroTextField.this));
                            end.put("event", "textFieldDidEndEditing");
                            ((IHeroContext) self.getContext()).on(end);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (json.has("textFieldDidBeginEditing") && hasFocus) {
                        try {
                            JSONObject begin = json.getJSONObject("textFieldDidBeginEditing");
                            begin.put("value", self.getText().toString());
                            begin.put("name", HeroView.getName(HeroTextField.this));
                            begin.put("event", "textFieldDidBeginEditing");
                            ((IHeroContext) self.getContext()).on(begin);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                isFocused = hasFocus;
            }
        });
    }
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void on(JSONObject jsonObject) throws JSONException {
        HeroView.on(this, jsonObject);
        if (jsonObject.has("size")) {
            String size = jsonObject.getString("size");
            try {
                float textSize = Float.parseFloat(size);
                this.setTextSize(textSize);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        if (jsonObject.has("secure")) {
            Object secure = jsonObject.get("secure");
            this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            if (secure instanceof Boolean) {
                boolean isSecure = jsonObject.getBoolean("secure");
                if (!isSecure) {
                    this.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    this.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                this.setSelection(this.getText().length());
            }
        }
        if (jsonObject.has("placeHolder")) {
            this.setHint(jsonObject.getString("placeHolder"));
        }
        if (jsonObject.has("placeHolderColor")) {
            this.setHintTextColor(HeroView.parseColor("#" + jsonObject.getString("placeHolderColor")));
        }
        if (jsonObject.has("textColor")) {
            this.setTextColor(HeroView.parseColor("#" + jsonObject.getString("textColor")));
        }
        if (jsonObject.has("clear")) {
            this.setText("");
            JSONObject json = HeroView.getJson(self);
            if (json != null && json.has("textFieldDidEditing")) {
                try {
                    JSONObject end = json.getJSONObject("textFieldDidEditing");
                    end.put("value", "");
                    ((IHeroContext) self.getContext()).on(end);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (jsonObject.has("type")) {
            String type = jsonObject.getString("type");
            if (type.equals("number")) {
                this.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else if (type.equals("phone")) {
                this.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else if (type.equals("pin")) {
                this.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        }
        if (jsonObject.has("formatStyle")) {
            if ("bankCard".equals(jsonObject.getString("formatStyle"))) {
                formatStyle = FORMAT_STYLE_BANKCARD;
                this.setInputType(InputType.TYPE_CLASS_NUMBER);
                jsonObject.put("maxLength", MAX_LENGTH_BANKCARD);
                this.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else if ("money".equals(jsonObject.getString("formatStyle"))) {
                formatStyle = FORMAT_STYLE_MONEY;
                jsonObject.put("maxLength", MAX_LENGTH_MONEY);
                this.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            } else if ("fixPhone".equals(jsonObject.getString("formatStyle"))) {
                formatStyle = FORMAT_STYLE_FIX_NUMBER;
                jsonObject.put("maxLength", MAX_LENGTH_PHONE_NUMBER);
                this.setInputType(InputType.TYPE_CLASS_PHONE);
            }
        }
        if (jsonObject.has("maxLength")) {
            this.setFilters(new InputFilter[] {new InputFilter.LengthFilter(jsonObject.getInt("maxLength"))});
        }
        if (jsonObject.has("minLength")) {
            Log.w("not implements", "minLength");
        }
        if (jsonObject.has("_allowString")) {
            Log.w("not implements", "_allowString");
        }
        if (jsonObject.has("text")) {

            this.setText(jsonObject.getString("text"));
        }
        if (jsonObject.has("editable")) {
            this.setEnabled(Boolean.valueOf(jsonObject.getString("editable")));
        }
        if (jsonObject.has("alignment")) {
            String alignment = jsonObject.getString("alignment");
            if (alignment != null && alignment.equalsIgnoreCase("center")) {
                this.setGravity(Gravity.CENTER);
            } else if (alignment != null && alignment.equalsIgnoreCase("left")) {
                this.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else if (alignment != null && alignment.equalsIgnoreCase("right")) {
                this.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            }
        }
    }

    protected String formatStyledText(String text) {
        String formattedText;
        switch (formatStyle) {
            case FORMAT_STYLE_BANKCARD:
                formattedText = FormatUtils.formatBankCardString(text);
                break;
            case FORMAT_STYLE_MONEY:
                formattedText = FormatUtils.formatMoneyString(text);
                break;
            case FORMAT_STYLE_FIX_NUMBER:
                formattedText = FormatUtils.formatFixNumberString(text);
                break;
            default:
                formattedText = text;
        }
        return formattedText;
    }

    protected String formatTextOnEnd(String text) {
        String formattedText;
        switch (formatStyle) {
            case FORMAT_STYLE_MONEY:
                formattedText = text;
                int length = text.length();
                if (text.charAt(length - 1) == '.') {
                    formattedText = text.substring(0, length - 1);
                }
                break;
            default:
                formattedText = text;
        }
        return formattedText;
    }

    public void setFocusChangeListener(FocusChangeListener f) {
        focusChangeListener = f;
    }

    public interface FocusChangeListener {
        void focusChanged(boolean hasFocus);
    }
}