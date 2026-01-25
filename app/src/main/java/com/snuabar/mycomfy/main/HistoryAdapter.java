package com.snuabar.mycomfy.main;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.databinding.FragmentHistoryItemBinding;
import com.snuabar.mycomfy.utils.ImageUtils;

import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<ImageUtils.ImageContent> mValues;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Callbacks.CallbackT<Integer> onItemClickCallback;

    public HistoryAdapter(List<ImageUtils.ImageContent> items, Callbacks.CallbackT<Integer> onItemClickCallback) {
        mValues = items;
        this.onItemClickCallback = onItemClickCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentHistoryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        ImageUtils.ImageContent content = mValues.get(position);
        if (ImageUtils.getThumbnail(content)) {
            holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(content.getThumbnailFile().getAbsolutePath()));
        } else {
            float width = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_width);
            float height = holder.itemView.getContext().getResources().getDimension(R.dimen.thumbnail_height);
            ImageUtils.makeThumbnailAsync(content, width, height, this::onThumbnailMake);
        }
        if (content.getParams() != null) {
            holder.binding.tvPrompt.setText(content.getParams().getPrompt());
            holder.binding.tvInfo.setText(
                    String.format(Locale.getDefault(), "%d x %d, 种子：%d",
                            content.getParams().getImg_width(),
                            content.getParams().getImg_height(),
                            content.getParams().getSeed()));
        }
    }

    private void onThumbnailMake(ImageUtils.ImageContent content) {
        int index = mValues.indexOf(content);
        if (index != -1) {
            mHandler.post(() -> notifyItemChanged(index));
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final FragmentHistoryItemBinding binding;

        public ViewHolder(FragmentHistoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.itemView.setOnClickListener(v -> {
                if (onItemClickCallback != null) {
                    onItemClickCallback.apply(getAbsoluteAdapterPosition());
                }
            });
        }
    }
}