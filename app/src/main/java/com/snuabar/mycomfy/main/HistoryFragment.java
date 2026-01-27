package com.snuabar.mycomfy.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.snuabar.mycomfy.databinding.FragmentHistoryBinding;
import com.snuabar.mycomfy.preview.FullScreenImageActivity;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.Output;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A fragment representing a list of Items.
 */
public class HistoryFragment extends Fragment {

    private MainViewModel mViewModel;
    private FragmentHistoryBinding binding;
    private final List<ImageUtils.ImageContent> mImageContents = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryFragment() {
    }

    @SuppressWarnings("unused")
    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        binding.list.setAdapter(new HistoryAdapter(mImageContents, this::onItemClick));
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadImageContents();
    }

    private void onItemClick(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        ImageUtils.ImageContent content = mImageContents.get(position);
        Intent intent = new Intent(requireActivity(), FullScreenImageActivity.class);
        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, content.imageFile.getAbsolutePath());
        startActivity(intent);
    }

    private void loadImageContents() {
        executor.execute(() -> {
            if (getContext() == null) {
                return;
            }
            ArrayList<ImageUtils.ImageContent> imageContents = Output.getOutputFiles(getContext());
            mImageContents.clear();
            if (imageContents != null) {
                imageContents.sort((o1, o2) -> {
                    if (o1.getParams() == null || o2.getParams() == null) {
                        return 0;
                    }
                    return Math.toIntExact(o2.getParams().getTimestamp() - o1.getParams().getTimestamp());
                });
                mImageContents.addAll(imageContents);
            }

            requireActivity().runOnUiThread(() -> {
                if (binding.list.getAdapter() != null) {
                    binding.list.getAdapter().notifyDataSetChanged();
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}