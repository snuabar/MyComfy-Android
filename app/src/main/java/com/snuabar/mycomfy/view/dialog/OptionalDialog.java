package com.snuabar.mycomfy.view.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.common.Common;

import java.util.Objects;

/**
 * Common dialog with a negative and a positive button.
 */
public class OptionalDialog extends BaseDialogFragment {

    private final static String TAG = OptionalDialog.class.getName();

    /**
     * Dialog Style
     */
    public enum Type {
        /**
         * Dialog with positive button with black text.(Default style)
         */
        Default,
        /**
         * Dialog with positive button with blue text.
         */
        Info,
        /**
         * Dialog with positive button with red text.
         */
        Alert,
    }

    private Type mType = Type.Default;

    private TextView mTvTitle = null;
    private TextView mTvMessage = null;
    private TextView mTvMessage2 = null;
    private Button mBtnNegative = null;
    private Button mBtnPositive = null;

    private @StringRes int mTitleId = 0;
    private @StringRes int mMessageId = 0;
    private @StringRes int mMessageId2 = 0;
    private String mMessageText = null;
    private String mMessageText2 = null;
    private @StringRes int mNegativeId = 0;
    private String mNegativeText = null;
    private @StringRes int mPositiveId = 0;
    private String mPositiveText = null;
    private Callbacks.Callback mNegativeCallback = null;
    private Callbacks.Callback mPositionCallback = null;
    private Callbacks.CallbackT<OptionalDialog> mPositionCallbackT = null;
    private Callbacks.CallbackT<OptionalDialog> mWillDismissCallback = null;
    private Callbacks.Callback mDismissCallback = null;
    private Callbacks.CallbackT<Integer> mDismissCallbackT = null;
    private boolean mPositiveVisible = true;
    private boolean mNegativeVisible = true;

    private View mButtonsContainer = null;
    private @StringRes int mNeutralId = 0;
    private String mTitleString = null;
    private boolean mNeutralVisible = false;
    private Button mBtnNeutral = null;
    private Callbacks.Callback mNeutralCallback = null;

    private @LayoutRes int mCustomViewId = 0;

    private ColorStateList mPositiveButtonDefaultTextColor = null;
    private Callbacks.CallbackT<OptionalDialog> mDialogCreatedCallback = null;
    private boolean mIsTitleCenter = false;

    public final static int DISMISS_BUTTON_NONE = 0;
    public final static int DISMISS_BUTTON_POSITIVE = 1;
    public final static int DISMISS_BUTTON_NEGATIVE = 2;
    public final static int DISMISS_BUTTON_NEUTRAL = 3;
    private int mDismissButton = DISMISS_BUTTON_NONE;
    private boolean mAllowOverlap = false;
    private boolean mDismissOnStop = false;
    private boolean mRemeasureSpec = false;

    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.layout_optional_dialog;
    }

    @DrawableRes
    protected int getWindowBackground() {
        return R.drawable.optional_dialog_bg;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Set window feature: no title.
        Objects.requireNonNull(getDialog()).requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Set window background.
        Objects.requireNonNull(getDialog().getWindow()).setBackgroundDrawableResource(getWindowBackground());
        //Inflate view.
        View v = inflater.inflate(getLayoutResource(), container, false);
        mTvTitle = v.findViewById(R.id.tv_dlg_title);
        mTvMessage = v.findViewById(R.id.tv_dlg_msg);
        mTvMessage2 = v.findViewById(R.id.tv_dlg_msg2);
        mBtnNegative = v.findViewById(R.id.btn_dlg_negative);
        mBtnPositive = v.findViewById(R.id.btn_dlg_positive);
        mBtnNeutral = v.findViewById(R.id.btn_dlg_neutral);
        mBtnNeutral.setOnClickListener(this::onClick);
        mBtnNegative.setOnClickListener(this::onClick);
        mBtnPositive.setOnClickListener(this::onClick);
        mButtonsContainer = v.findViewById(R.id.cl_buttons);

        ViewGroup customViewContainer = v.findViewById(R.id.view_custom);
        if (customViewContainer != null) {
            if (mCustomViewId != 0) {
                try {
                    inflater.inflate(mCustomViewId, customViewContainer, true);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set a custom view, error: " + e.getMessage(), e);
                    customViewContainer.setVisibility(View.GONE);
                }
            } else {
                customViewContainer.setVisibility(View.GONE);
            }
        }
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (mTitleId != 0) {
            mTvTitle.setText(mTitleId);
        } else if (mTitleString != null) {
            mTvTitle.setText(mTitleString);
        } else {
            mTvTitle.setVisibility(View.GONE);
        }
        if (mIsTitleCenter) {
            mTvTitle.setGravity(Gravity.CENTER);
        }
        if (mMessageId != 0) {
            mTvMessage.setText(mMessageId);
        } else {
            if (!TextUtils.isEmpty(mMessageText)) {
                mTvMessage.setText(mMessageText);
            } else {
                mTvMessage.setVisibility(View.GONE);
            }
        }
        if (mMessageId2 != 0) {
            mTvMessage2.setVisibility(View.VISIBLE);
            mTvMessage2.setText(mMessageId2);
        } else {
            if (!TextUtils.isEmpty(mMessageText2)) {
                mTvMessage2.setVisibility(View.VISIBLE);
                mTvMessage2.setText(mMessageText2);
            } else {
                mTvMessage2.setVisibility(View.GONE);
            }
        }
        if (!TextUtils.isEmpty(mNegativeText)) {
            mBtnNegative.setText(mNegativeText);
        } else if (mNegativeId != 0) {
            mBtnNegative.setText(mNegativeId);
        }

        if (!TextUtils.isEmpty(mPositiveText)) {
            mBtnPositive.setText(mPositiveText);
        } else if (mPositiveId != 0) {
            mBtnPositive.setText(mPositiveId);
        }

        if (mNeutralId != 0) {
            mBtnNeutral.setText(mNeutralId);
        }
        if (!mNeutralVisible && !mNegativeVisible && !mPositiveVisible) {
            mButtonsContainer.setVisibility(View.GONE);
        } else {
            mBtnNeutral.setVisibility(mNeutralVisible ? View.VISIBLE : View.GONE);
            mBtnNegative.setVisibility(mNegativeVisible ? View.VISIBLE : View.GONE);
            mBtnPositive.setVisibility(mPositiveVisible ? View.VISIBLE : View.GONE);
        }
        configButtonsWidth();
        if (mPositiveButtonDefaultTextColor == null) {
            mPositiveButtonDefaultTextColor = mBtnPositive.getTextColors();
        }
        if (mType != Type.Default) {
            final ColorStateList stateList = getResources().getColorStateList(
                    mType == Type.Info ? android.R.color.holo_blue_dark : android.R.color.holo_red_dark,
                    requireContext().getTheme());
            mBtnPositive.setTextColor(stateList);
        } else {
            mBtnPositive.setTextColor(mPositiveButtonDefaultTextColor);
        }
        setCancelable(false);
        super.onViewCreated(view, savedInstanceState);

        if (mDialogCreatedCallback != null) {
            mDialogCreatedCallback.apply(this);
        }
    }

    public void show(@NonNull FragmentManager manager) {
        String tag = TAG;
        if (manager.findFragmentByTag(tag) != null) {
            if (!mAllowOverlap) {
                return;
            }
            tag += "@" + hashCode();
        }
        try {
            show(manager, tag);
            mDismissButton = DISMISS_BUTTON_NONE;
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("after onSaveInstanceState")) {
                Log.e(TAG, "Use showAllowingStateLoss() instead.");
                showAllowingStateLoss(manager);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDismissOnStop) {
            dismissAllowingStateLoss();
        }
    }

    public void showAllowingStateLoss(@NonNull FragmentManager manager) {
        String tag = TAG;
        if (manager.findFragmentByTag(tag) != null) {
            if (!mAllowOverlap) {
                return;
            }
            tag += "@" + hashCode();
        }
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
        mDismissButton = DISMISS_BUTTON_NONE;
    }

//    @Override
//    public void onConfigurationChanged(@NonNull Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        configureWindowLayout();
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        configureWindowLayout();
//    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mWillDismissCallback != null) {
            mWillDismissCallback.apply(this);
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDismissCallback != null) {
            mDismissCallback.apply();
        }
        if (mDismissCallbackT != null) {
            mDismissCallbackT.apply(mDismissButton);
        }

        mDismissButton = DISMISS_BUTTON_NONE;
    }

//    private void configureWindowLayout() {
//        //configure window width
//        if (getDialog() != null) {
//            Window window = getDialog().getWindow();
//            if (window != null) {
//                int width = CommonUtils.getDialogWidthFromScreen(window);
//                window.setLayout((int) (width * 0.8f), ViewGroup.LayoutParams.WRAP_CONTENT);
//            }
//        }
//    }

    private void onClick(final View v) {
        final int id = v.getId();
        if (R.id.btn_dlg_negative == id) {
            mDismissButton = DISMISS_BUTTON_NEGATIVE;
            if (mNegativeCallback != null) {
                mNegativeCallback.apply();
            }
        } else if (R.id.btn_dlg_positive == id) {
            mDismissButton = DISMISS_BUTTON_POSITIVE;
            if (mPositionCallback != null) {
                mPositionCallback.apply();
            }
            if (mPositionCallbackT != null) {
                mPositionCallbackT.apply(this);
            }
        } else if (R.id.btn_dlg_neutral == id) {
            mDismissButton = DISMISS_BUTTON_NEUTRAL;
            if (mNeutralCallback != null) {
                mNeutralCallback.apply();
            }
        }

        dismiss();
    }

    /**
     * Sets the string id of dialog title.
     *
     * @param textId Title string id.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setTitle(@StringRes int textId) {
        mTitleId = textId;
        return this;
    }

    /**
     * Sets the string id of dialog message text.
     *
     * @param textId Message string id.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setMessage(@StringRes int textId) {
        mMessageId = textId;
        return this;
    }

    /**
     * Sets the string id of the second dialog message text.
     *
     * @param textId Message string id.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setSecondMessage(@StringRes int textId) {
        mMessageId2 = textId;
        return this;
    }

    /**
     * Sets a message string to this dialog.
     *
     * @param text Message string.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setMessage(String text) {
        mMessageText = text;
        return this;
    }

    /**
     * Sets the second message string to this dialog.
     *
     * @param text Message string.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setSecondMessage(String text) {
        mMessageText2 = text;
        return this;
    }

    /**
     * Set the type of this dialog.
     * @see Type
     *
     * @param type Dialog type.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setType(final Type type) {
        mType = type;
        return this;
    }

    /**
     * Sets the positive Callback.
     *
     * @param callback Callback of the positive button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setPositive(final Callbacks.Callback callback) {
        mPositionCallback = callback;
        return this;
    }

    /**
     * Sets the positive callback that is aim to get some items of this dialog such as searching a custom view.
     *
     * @param callback Callback of the positive button. The parameter of this callback is a {@link OptionalDialog} instance.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setPositive(final Callbacks.CallbackT<OptionalDialog> callback) {
        mPositionCallbackT = callback;
        return this;
    }

    /**
     * Sets the positive callback with a specified string id of the button text.
     *
     * @param textId String id of the positive button.
     * @param callback Callback of the positive button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setPositive(@StringRes int textId, final Callbacks.Callback callback) {
        mPositionCallback = callback;
        mPositiveId = textId;
        return this;
    }

    /**
     * Sets the positive callback with a specified string of the button text.
     *
     * @param text   String of the positive button.
     * @param callback Callback of the positive button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setPositive(String text, final Callbacks.Callback callback) {
        mPositionCallback = callback;
        mPositiveText = text;
        return this;
    }

    /**
     * Sets the negative callback.
     *
     * @param callback Callback of the negative button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setNegative(final Callbacks.Callback callback) {
        mNegativeCallback = callback;
        return this;
    }

    /**
     * Sets the negative callback with a specified string id of the button text.
     *
     * @param textId String id of the negative button.
     * @param callback Callback of the negative button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setNegative(@StringRes int textId, final Callbacks.Callback callback) {
        mNegativeCallback = callback;
        mNegativeId = textId;
        return this;
    }

    /**
     * Sets the negative callback with a specified string of the button text.
     *
     * @param text   String of the negative button.
     * @param callback Callback of the negative button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setNegative(String text, final Callbacks.Callback callback) {
        mNegativeCallback = callback;
        mNegativeText = text;
        return this;
    }

    /**
     * Callback when dialog dismisses.
     *
     * @param callback Callback
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setDismissCallback(final Callbacks.Callback callback) {
        mDismissCallback = callback;
        return this;
    }

    public OptionalDialog setDismissCallback(final Callbacks.CallbackT<Integer> callback) {
        mDismissCallbackT = callback;
        return this;
    }

    /**
     * Action that will be invoked when this dialog is about to be dismissed.
     *
     * @param callback callback
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setWillDismissCallback(final Callbacks.CallbackT<OptionalDialog> callback) {
        mWillDismissCallback = callback;
        return this;
    }

    public OptionalDialog setPositiveVisible(final boolean visible) {
        mPositiveVisible = visible;
        return this;
    }

    public OptionalDialog setNegativeVisible(final boolean visible) {
        mNegativeVisible = visible;
        return this;
    }


    /**
     * Sets the string id of dialog title.
     *
     * @param text Title string.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setTitle(String text) {
        mTitleString = text;
        return this;
    }

    /**
     * Sets the neutral callback with a specified string id of the button text.
     *
     * @param textId String id of the positive button.
     * @param callback Callback of the positive button.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setNeutral(@StringRes int textId, final Callbacks.Callback callback) {
        mNeutralCallback = callback;
        mNeutralId = textId;
        // ボタンを指定した場合はVisibleにする
        mNeutralVisible = true;
        return this;
    }

    /**
     * Sets a custom view. This view will be placed between buttons and the message text view.
     *
     * @param layoutId Layout id of the custom view.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setCustomView(@LayoutRes int layoutId) {
        mCustomViewId = layoutId;
        return this;
    }

    /**
     * Sets an Callback. This callback will be called when this dialog is created.
     *
     * @param dialogCreatedCallback Callback
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setDialogCreatedCallback(Callbacks.CallbackT<OptionalDialog> dialogCreatedCallback) {
        mDialogCreatedCallback = dialogCreatedCallback;
        return this;
    }

    public OptionalDialog setIsTitleCenter(boolean isCenterTitle) {
        mIsTitleCenter = isCenterTitle;
        return this;
    }

    /**
     * Functions similarly as {@link View#findViewById(int)} but only for searching sub views within the custom view container.
     * If this dialog has not been initialized, null will be returned.
     *
     * @param viewId View id.
     * @return A view with given id if found, or null otherwise.
     */
    public <T extends View> T findCustomViewById(@IdRes int viewId) {
        if (getView() == null) {
            Log.e(TAG, "View of this dialog is null due to a possible cause that this dialog is not initialized or has been destroyed.");
            return null;
        }

        ViewGroup customViewContainer = getView().findViewById(R.id.view_custom);
        return customViewContainer.findViewById(viewId);
    }

    /**
     * Configure the width of buttons by setting the percent of constraint.
     */
    private void configButtonsWidth() {
        if (getView() == null) {
            return;
        }

        ConstraintLayout constraintLayout = getView().findViewById(R.id.cl_buttons);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        int buttonCount = 0;
        for (int i = 0; i < constraintLayout.getChildCount(); i++) {
            View view = constraintLayout.getChildAt(i);
            if (view instanceof Button && view.getVisibility() != View.GONE) {
                buttonCount++;
            }
        }

        final float percent = (float) Math.floor(1.f / buttonCount * 10) / 10;
        final float longest = (float) (percent + Math.ceil((1.f % percent) * 10) / 10);

        if (mBtnNegative.getVisibility() != View.GONE) {
            constraintSet.constrainPercentWidth(R.id.btn_dlg_negative, longest);
        }
        if (mBtnNeutral.getVisibility() != View.GONE) {
            constraintSet.constrainPercentWidth(R.id.btn_dlg_neutral, percent);
        }
        if (mBtnPositive.getVisibility() != View.GONE) {
            constraintSet.constrainPercentWidth(R.id.btn_dlg_positive, percent);
        }

        constraintSet.applyTo(constraintLayout);
    }

    /**
     * Allows this dialog to display and overlap on another dialog.
     *
     * @param allowOverlap Allow overlap or does not.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setAllowOverlap(boolean allowOverlap) {
        mAllowOverlap = allowOverlap;
        return this;
    }

    public OptionalDialog setDismissOnStop(boolean b) {
        mDismissOnStop = b;
        return this;
    }

    /**
     * Set a flag that indicates whether this dialog`s layout should be remeasured on every update。<br>
     * This might solve layout problems.
     *
     * @param remeasureSpec Flag. Default is {@code false}.
     * @return {@link OptionalDialog} instance.
     */
    public OptionalDialog setRemeasureSpec(boolean remeasureSpec) {
        mRemeasureSpec = remeasureSpec;
        return this;
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        if (!mRemeasureSpec || getView() == null || getDialog() == null || getDialog().getWindow() == null) {
            return;
        }

        View view = getView();
        Dialog dialog = getDialog();
        Window window = dialog.getWindow();
        final int width = (int) (Common.getDialogWidthFromDisplay(window) * 0.8f);
        // 手动测量和布局
        view.measure(
                View.MeasureSpec.makeMeasureSpec(
                        width,
                        View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        view.layout(
                0, 0,
                view.getMeasuredWidth(),
                view.getMeasuredHeight()
        );
        window.setLayout(width, view.getHeight());
    }
}
