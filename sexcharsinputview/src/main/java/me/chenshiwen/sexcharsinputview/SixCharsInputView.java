package me.chenshiwen.sexcharsinputview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class SixCharsInputView extends EditText implements TextWatcher {

    private static final int DEFAULT_SELECTED_RESOURCE = R.drawable.simple_auth_code_selected;
    private static final int DEFAULT_UNSELECTED_RESOURCE = R.drawable.simple_auth_code_unselected;
    private static final int DEFAULT_PWD_SELECTED_RESOURCE = R.drawable.simple_passwork_selected;
    private static final int DEFAULT_PWD_UNSELECTED_RESOURCE = R.drawable.simple_passwork_unselected;
    private static final int DEFAULT_PASSWORD_MASK = R.drawable.simple_passwork_mask;
    private static final int DEFAULT_SPACING = 10; // 单位dp
    private static final int DEFAULT_INPUT_FRAME_SIZE = 40; // 单位dp
    private int textColor;
    private Drawable selected;
    private Drawable unSelected;
    private int spacing; // 横向的间距
    private int sizeOfInputFrame; // 每个输入方格的边长

    private Drawable passwordMask; // 密码drawable类型的掩码
    private String passwordMaskText; // 密码文本类型的掩码
    private int passwordMaskType = PasswordMaskTypeDrawable;
    private static final int PasswordMaskTypeDrawable = 1;
    private static final int PasswordMaskTypeText = 2;

    private boolean isPassword;

    private int currentInputIndex = -1; // 0-5

    public SixCharsInputView(Context context) {
        super(context);
        init(context, null);
    }

    public SixCharsInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SixCharsInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(final Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SixCharsInputView);
        isPassword = a.getBoolean(R.styleable.SixCharsInputView_is_password, false);
        selected = a.getDrawable(R.styleable.SixCharsInputView_bg_selected);
        if (selected == null) {
            selected = context.getResources().getDrawable(isPassword? DEFAULT_PWD_SELECTED_RESOURCE : DEFAULT_SELECTED_RESOURCE);
        }
        unSelected = a.getDrawable(R.styleable.SixCharsInputView_bg_unselected);
        if (unSelected == null) {
            unSelected = context.getResources().getDrawable(isPassword? DEFAULT_PWD_UNSELECTED_RESOURCE : DEFAULT_UNSELECTED_RESOURCE);
        }
        // 密码的掩码可以设置成drawable和text两种形式，如果两种都设置，那么只有drawable有效；
        // 如果两种都不设置，那么密码的掩码为默认的drawable形式（圆黑点）。
        passwordMask = a.getDrawable(R.styleable.SixCharsInputView_password_mask);
        if (passwordMask == null) {
            passwordMaskText = a.getString(R.styleable.SixCharsInputView_password_mask_text);
            if (!TextUtils.isEmpty(passwordMaskText)) {
                passwordMaskType = PasswordMaskTypeText;
            } else {
                passwordMask = context.getResources().getDrawable(DEFAULT_PASSWORD_MASK);
            }
        } else {
            passwordMaskType = PasswordMaskTypeDrawable;
        }
        spacing = a.getDimensionPixelSize(R.styleable.SixCharsInputView_horizontal_spacing, dp2px(DEFAULT_SPACING));
        sizeOfInputFrame = a.getDimensionPixelSize(R.styleable.SixCharsInputView_input_frame_size, dp2px(DEFAULT_INPUT_FRAME_SIZE));
        a.recycle();

        setBackgroundColor(Color.TRANSPARENT);
        textColor = getPaint().getColor();
        setTextColor(Color.TRANSPARENT);
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        setCursorVisible(false);

        addTextChangedListener(this);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // 强制弹出键盘
                    SixCharsInputView.this.requestFocus();
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(SixCharsInputView.this, InputMethodManager.SHOW_FORCED);
                }
                return true;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            int widthSize = sizeOfInputFrame*6 + spacing*5;
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        } else {
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            spacing = Math.max((widthSize - sizeOfInputFrame*6)/5, 0);
        }

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            int heightSize = sizeOfInputFrame;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        TextPaint paint = getPaint();
        paint.setColor(textColor);
        String text = getText().toString();
        int textHeight = getTextHeight(paint);
        Drawable selectedDrawable = selected;
        Drawable unSelectedDrawable = unSelected;
        int left = 0;
        int right = sizeOfInputFrame;
        for (int i = 0; i <= currentInputIndex; ++i) {
            // 绘制方格(选中状态)
            canvas.clipRect(left, 0, right, sizeOfInputFrame, Region.Op.REPLACE);
            selectedDrawable.setBounds(left, 0, right, sizeOfInputFrame);
            selectedDrawable.draw(canvas);
            if (isPassword) {
                // 绘制密码掩码
                if (passwordMaskType == PasswordMaskTypeDrawable) {
                    passwordMask.setBounds(left, 0, right, sizeOfInputFrame);
                    passwordMask.draw(canvas);
                } else {
                    int textWidth = getTextWidth(paint, passwordMaskText);
                    canvas.drawText(passwordMaskText, left + sizeOfInputFrame/2 - textWidth/2, sizeOfInputFrame/2 + textHeight/2, paint);
                }
            } else {
                // 绘制数字
                String textToDraw = text.substring(i, i+1);
                int textWidth = getTextWidth(paint, textToDraw);
                canvas.drawText(textToDraw, left + sizeOfInputFrame/2 - textWidth/2, sizeOfInputFrame/2 + textHeight/2, paint);
            }
            //
            left = right + spacing;
            right = left + sizeOfInputFrame;
        }

        for (int i = currentInputIndex+1; i < 6; ++i) {
            // 绘制方格(未选中状态)
            canvas.clipRect(left, 0, right, sizeOfInputFrame, Region.Op.REPLACE);
            unSelectedDrawable.setBounds(left, 0, right, sizeOfInputFrame);
            unSelectedDrawable.draw(canvas);
            left = right + spacing;
            right = left + sizeOfInputFrame;
        }
        paint.setColor(Color.TRANSPARENT);
    }

    /**
     * 计算文本高度
     * @param paint
     * @return
     */
    private int getTextHeight(Paint paint){
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int)(Math.abs(fm.ascent));
    }

    /**
     * 计算文本宽度
     * @param paint
     * @param str
     * @return
     */
    private int getTextWidth(TextPaint paint ,String str){
        return (int)paint.measureText(str);
    }

    private int dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        currentInputIndex = s.length() - 1;
        invalidate();
    }
}
