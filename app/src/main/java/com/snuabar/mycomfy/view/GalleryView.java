package com.snuabar.mycomfy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.snuabar.mycomfy.R;


public class GalleryView extends RecyclerView {

    private final static int NumberOfColumnsAutoFit = -1;

    private final static int HorizontalSpacingStrategyGapSensitive = -1;//Default
    private final static int HorizontalSpacingStrategyFixed = -2;

    private final static float DefaultColumnWidth = 120;//DP

    private float _horizontalSpacing = 0;
    private float _verticalSpacing = 0;
    private float _columnWidth = 0;
    private int _numberOfColumns = 0;
    private int _horizontalSpacingStrategy = 0;

    protected float mHorizontalSpacing = 0;
    protected float mVerticalSpacing = 0;
    protected float mColumnWidth = 0;
    protected int mNumberOfColumns = 0;

    private static final int VIEW_TYPE_HEADER = 1;

    public GalleryView(@NonNull Context context) {
        super(context);
        init(null, 0);
    }

    public GalleryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public GalleryView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GalleryView, defStyle, 0);

        _horizontalSpacing = a.getDimension(R.styleable.GalleryView_horizontalSpacing, 0);
        _verticalSpacing = a.getDimension(R.styleable.GalleryView_verticalSpacing, 0);
        _columnWidth = a.getDimension(R.styleable.GalleryView_columnWidth, 0);
        _numberOfColumns = a.getInt(R.styleable.GalleryView_numColumns, NumberOfColumnsAutoFit);
        _horizontalSpacingStrategy = a.getInt(R.styleable.GalleryView_horizontalSpacingStrategy, HorizontalSpacingStrategyGapSensitive);

        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        addItemDecoration(mItemDecoration);
        configureLayout();
    }

    /**
     * Configures variables for item decoration.
     */
    protected void configure() {
        if (_columnWidth <= 0) {
            mColumnWidth = DefaultColumnWidth;
        } else {
            mColumnWidth = _columnWidth;
        }

        Context context = getContext();

        float screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (_numberOfColumns == NumberOfColumnsAutoFit) {
            int maxNumberOfColumns = (int) Math.floor(screenWidth / mColumnWidth);
            if (_horizontalSpacing != 0) {
                mNumberOfColumns = (int) Math.floor((screenWidth - _horizontalSpacing * maxNumberOfColumns) / mColumnWidth);
            } else {
                mNumberOfColumns = maxNumberOfColumns;
            }
        } else {
            mNumberOfColumns = _numberOfColumns;
        }

        if (HorizontalSpacingStrategyFixed == _horizontalSpacingStrategy) {
            mHorizontalSpacing = _horizontalSpacing;
        } else {
            float unusedSpace = screenWidth - mColumnWidth * mNumberOfColumns - _horizontalSpacing * mNumberOfColumns;
            if (unusedSpace < 0) {
                unusedSpace = 0;
            }

            mHorizontalSpacing = (int) (_horizontalSpacing + (unusedSpace / mNumberOfColumns));
        }
        mVerticalSpacing = _verticalSpacing;

//        Context context = getContext();
//        float screenWidth = context.getResources().getDisplayMetrics().widthPixels;
//        int minNumberOfColumns = 3;
//        screenWidth / minNumberOfColumns
    }

    protected void updateLayoutManager() {
        GridLayoutManager layoutManager;
        if (getLayoutManager() instanceof GridLayoutManager) {
            layoutManager = (GridLayoutManager) getLayoutManager();
            layoutManager.setSpanCount(mNumberOfColumns);
        } else {
            layoutManager = new GridLayoutManager(getContext(), mNumberOfColumns,
                    GridLayoutManager.VERTICAL, false);
            setLayoutManager(layoutManager);
        }

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (getAdapter().getItemViewType(position) == VIEW_TYPE_HEADER) {
                    return layoutManager.getSpanCount();
                }
                else {
                    return 1;
                }
            }
        });
    }

    public void configureLayout() {
        configure();
        updateLayoutManager();
    }

    /**
     * Sets the margin of this view.
     *
     * @param l Left.
     * @param t Top.
     * @param r Right.
     * @param b Bottom.
     */
    public void setMargin(int l, int t, int r, int b) {
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp.leftMargin != l || lp.topMargin != t || lp.rightMargin != r || lp.bottomMargin != b) {
            lp.leftMargin = l;
            lp.topMargin = t;
            lp.rightMargin = r;
            lp.bottomMargin = b;
            lp.setMarginStart(l);
            lp.setMarginEnd(r);
            setLayoutParams(lp);
        }
    }

    private final ItemDecoration mItemDecoration = new ItemDecoration() {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull State state) {
            if (parent.getAdapter() == null) {
                return;
            }

            final int pos = parent.getChildAdapterPosition(view);
            final int columns = mNumberOfColumns;
            final int rows = (int)Math.ceil(parent.getAdapter().getItemCount() / (float)columns);
            final int row = pos / columns;
            final int column = pos % columns;

            outRect.left = (int) (mHorizontalSpacing / 2);
            outRect.top = (int) mVerticalSpacing;
            if (row == rows - 1) {//Last row
                outRect.bottom = (int) mVerticalSpacing;
            }
        }
    };
}
