package com.snuabar.mycomfy.main;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.databinding.LayoutHistoryItemBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.ThumbnailCacheManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<AbstractMessageModel> mValues;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Callbacks.Callback2T<Integer, Boolean> onItemClickCallback;
    private final Set<Integer> selections;
    private final Map<String, Integer> idToIndexMap;
    private boolean isEditMode = false;
    private final Context context;
    private final Set<String> matchedIDs;

    public HistoryAdapter(Context context, Callbacks.Callback2T<Integer, Boolean> onItemClickCallback) {
        this.context = context.getApplicationContext();
        mValues = new ArrayList<>();
        this.idToIndexMap = new HashMap<>();
        updateIdToIndexMap();
        this.onItemClickCallback = onItemClickCallback;
        this.selections = new HashSet<>();
        this.matchedIDs = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutHistoryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (!matchedIDs.isEmpty() && !matchedIDs.contains(mValues.get(position).getId())) {
            holder.itemView.getLayoutParams().width = 0;
            holder.itemView.getLayoutParams().height = 0;
            return;
        }
        holder.itemView.getLayoutParams().width = (int) holder.itemView.getResources().getDimension(R.dimen.gallery_item_size);
        holder.itemView.getLayoutParams().height = (int) holder.itemView.getResources().getDimension(R.dimen.gallery_item_size);

        holder.binding.checkBox.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.binding.checkBox.setChecked(selections.contains(position));

        AbstractMessageModel model = mValues.get(position);
        if (ImageUtils.getThumbnail(model)) {
            Bitmap thumb = ThumbnailCacheManager.Companion.getInstance().getThumbnail(model.getThumbnailFile().getAbsolutePath());
            holder.binding.imageView.setImageBitmap(thumb);
            if (thumb == null) {
                ThumbnailCacheManager.Companion.getInstance().getThumbnailAsync(
                        model.getThumbnailFile().getAbsolutePath(),
                        model.getId(), this::onThumbnailLoad);
            }
        } else {
            float width = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_width);
            float height = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_height);
            ImageUtils.makeThumbnailAsync(model, width, height, this::onThumbnailMake);
        }
        if (model.getParameters() != null) {
            boolean upscaled = model.getParameters().getUpscale_factor() > 1.0;
            if (upscaled) {
                holder.binding.tvScaleFactor.setVisibility(View.VISIBLE);
                holder.binding.tvScaleFactor.setText(String.format(Locale.getDefault(),
                        "%.01fx",
                        model.getParameters().getUpscale_factor()));
            } else {
                holder.binding.tvScaleFactor.setVisibility(View.GONE);
            }
            holder.binding.tvTitle.setText(Common.formatTimestamp(model.getUTCTimestampCompletion()));
            int[] size = model.getImageSize();
            holder.binding.tvInfo.setText(String.format(Locale.getDefault(),
                    "%d x %d %s",
                    size[0], size[1],
                    Common.formatFileSize(model.getImageFile().length()
                    )));
        } else {
            holder.binding.tvScaleFactor.setVisibility(View.GONE);
        }
        holder.binding.ivPlay.setVisibility(model.isVideo() ? View.VISIBLE : View.GONE);
    }

    private void onThumbnailMake(AbstractMessageModel content) {
        Integer index = idToIndexMap.get(content.getId());
        if (index != null && index >= 0 && index < mValues.size()) {
            mHandler.post(() -> notifyItemChanged(index));
        }
    }

    private void onThumbnailLoad(Bitmap bmp, Object passBack) {
        if (passBack instanceof String) {
            Integer index = idToIndexMap.get(passBack);
            if (index != null && index >= 0 && index < mValues.size()) {
                notifyItemChanged(index);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selections.clear();
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setValues(List<AbstractMessageModel> values) {
        mValues = Objects.requireNonNullElseGet(values, ArrayList::new);
        updateIdToIndexMap();
        notifyDataSetChanged();
    }

    private void updateIdToIndexMap() {
        idToIndexMap.clear();
        for (int i = 0; i < mValues.size(); i++) {
            idToIndexMap.put(mValues.get(i).getId(), i);
        }
    }

    public void toggleSelection(int position) {
        if (selections.contains(position)) {
            selections.remove(position);
        } else {
            selections.add(position);
        }
        notifyItemChanged(position);
    }

    public List<Integer> getSelectedIndices() {
        return new ArrayList<>(selections);
    }

    /**
     * 给搜索用
     *
     * @param ids 不在这个列表中的项会被隐藏。
     */
    public void setMatchedIDs(Set<String> ids) {
        if (matchedIDs.isEmpty() && (ids == null || ids.isEmpty())) {
            return;
        }
        matchedIDs.clear();
        if (ids != null) {
            matchedIDs.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final LayoutHistoryItemBinding binding;

        public ViewHolder(LayoutHistoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.itemView.setLongClickable(true);
            this.itemView.setOnLongClickListener(v -> {
                if (onItemClickCallback != null) {
                    onItemClickCallback.apply(getAbsoluteAdapterPosition(), true);
                }
                return true;
            });
            this.itemView.setOnClickListener(v -> {
                if (onItemClickCallback != null) {
                    onItemClickCallback.apply(getAbsoluteAdapterPosition(), false);
                }
            });
        }
    }
}