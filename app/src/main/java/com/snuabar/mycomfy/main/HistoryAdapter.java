package com.snuabar.mycomfy.main;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
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
import com.snuabar.mycomfy.main.data.DataIO;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.VideoUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<AbstractMessageModel> mValues;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Callbacks.Callback2T<Integer, Boolean> onItemClickCallback;
    private final Set<Integer> selections;
    private boolean isEditMode = false;
    private final Context context;

    public HistoryAdapter(Context context, List<AbstractMessageModel> items, Callbacks.Callback2T<Integer, Boolean> onItemClickCallback) {
        this.context = context.getApplicationContext();
        mValues = items;
        this.onItemClickCallback = onItemClickCallback;
        this.selections = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutHistoryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.binding.checkBox.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.binding.checkBox.setChecked(selections.contains(position));

        AbstractMessageModel model = mValues.get(position);
        if (ImageUtils.getThumbnail(model)) {
            holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(model.getThumbnailFile().getAbsolutePath()));
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
            int[] size;
            if (model.isVideo()) {
                VideoUtils.VideoSize videoSize = VideoUtils.INSTANCE.getVideoSize(model.getImageFile());
                size = new int[]{videoSize.getWidth(), videoSize.getHeight()};
            } else {
                size = ImageUtils.getImageSize(model.getImageFile());
            }
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
        int index = mValues.indexOf(content);
        if (index != -1) {
            mHandler.post(() -> notifyItemChanged(index));
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        selections.clear();
        notifyDataSetChanged();
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

    public List<AbstractMessageModel> deleteSelection() {
        List<Integer> indices = getSelectedIndices();
        indices.sort((o1, o2) -> o2 - o1);

        List<AbstractMessageModel> deletedModels = new ArrayList<>();
        for (int i : indices) {
            deletedModels.add(mValues.remove(i));
            notifyItemRemoved(i);
        }
        return deletedModels;
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