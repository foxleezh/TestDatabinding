package com.foxlee.testdatabinding;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.foxlee.testdatabinding.databinding.FragmentNewListBinding;

/**
 * Created by kelin on 16-4-25.
 */
public class NewsListFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentNewListBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_new_list, container, false);
        binding.setVariable(BR.viewModel,new NewsViewModel(getActivity().getApplicationContext()));
        return binding.getRoot();
    }
}
