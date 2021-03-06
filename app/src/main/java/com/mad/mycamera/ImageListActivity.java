package com.mad.mycamera;



import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class ImageListActivity extends AppCompatActivity {

    private TextView mImageFolder;
    private RecyclerView mImageList;
    private ImageAdapter mAdapter;
    private String mChosenFolder;

    public static int position = 0;
    private static ArrayList<String> mFilePaths = new ArrayList<>(), mFileNames = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);

        Intent intent = getIntent();
        if (intent != null)
            mChosenFolder = intent.getExtras().getString(Constants.CHOSEN_FOLDER);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Choose a picture");
        setSupportActionBar(toolbar);

        getWindow().getSharedElementEnterTransition().setDuration(Constants.TRANSITION_DURATION);
        getWindow().getSharedElementReturnTransition().setDuration(Constants.TRANSITION_DURATION);

        Fade fade = new Fade();
        View view = getWindow().getDecorView();
        fade.excludeTarget(view.findViewById(R.id.action_bar_container), true);
        fade.excludeTarget(android.R.id.statusBarBackground, true);
        fade.excludeTarget(android.R.id.navigationBarBackground, true);
        getWindow().setEnterTransition(fade);
        getWindow().setExitTransition(fade);

        mImageFolder = findViewById(R.id.textView_image_count);
        mImageFolder.setText(mChosenFolder);

        mImageList = findViewById(R.id.recyclerView_image_list);
        mFileNames = new ArrayList<>();

        setAdapter();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityReenter(int resultCode, Intent data) {

        mImageList.scrollToPosition(position);

        final CustomSharedElementCallback callback = new CustomSharedElementCallback();
        setExitSharedElementCallback(callback);
        getWindow().getSharedElementExitTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                removeCallback();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                removeCallback();
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            private void removeCallback() {
                getWindow().getSharedElementExitTransition().removeListener(this);
                setExitSharedElementCallback((SharedElementCallback) null);
            }
        });

        supportPostponeEnterTransition();

        mImageList.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mImageList.getViewTreeObserver().removeOnPreDrawListener(this);

                RecyclerView.ViewHolder holder = mImageList.findViewHolderForAdapterPosition(position);
                if (holder != null) {
                    callback.setView(holder.itemView.findViewById(R.id.imageView_holderImage));
                }

                supportStartPostponedEnterTransition();

                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED)
            return;

        if (resultCode == RESULT_OK) {
            if (EditorPage.mSaved) {
                if (requestCode == Constants.REQUEST_CODE) {
                    fetchingData();
                    mImageFolder.setText(Constants.MY_DIRECTORY);
                    mAdapter.add(mFilePaths, mFileNames);
                    mAdapter.notifyDataSetChanged();
                    mImageList.scrollToPosition(0);
                }
                EditorPage.mSaved = false;
            }
        }
    }

    private void fetchingData() {
        mFilePaths = LoadedImages.folderMap.get(Constants.MY_DIRECTORY);
        mFileNames = new ArrayList<>();
        for (String path : mFilePaths) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            mFileNames.add(name);
        }
    }

    private void setAdapter() {
        mFilePaths = new ArrayList<>();
        mAdapter = new ImageAdapter(this);
        if (mChosenFolder.equals(Constants.ALL_IMAGES)) {
            mFilePaths = LoadedImages.allImages;
        } else {
            mFilePaths = FolderActivity.getChosenImages();
        }
        mFileNames = new ArrayList<>();
        for (String path : mFilePaths) {
            String name = path.substring(path.lastIndexOf('/') + 1);
            mFileNames.add(name);
        }
        mAdapter.add(mFilePaths, mFileNames);
        mAdapter.addOnClickListener((image, path, name, pos) -> {
            position = pos;
            Intent intent = new Intent(ImageListActivity.this, EditorPage.class);
            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(ImageListActivity.this,
                            image, Objects.requireNonNull(ViewCompat.getTransitionName(image)));
            intent.putExtra(Constants.IMAGE_TRANSITION_NAME, ViewCompat.getTransitionName(image));
            intent.putExtra(Constants.IMAGE_PATH, path);
            intent.putExtra(Constants.IMAGE_NAME, name);
            startActivityForResult(intent, Constants.REQUEST_CODE, optionsCompat.toBundle());
        });
        mImageList.setAdapter(mAdapter);
        mImageList.setLayoutManager(new GridLayoutManager(this, 4));
    }

    public static ArrayList<String> getFileNames() {
        return mFileNames;
    }

    public static ArrayList<String> getFilePaths() {
        return mFilePaths;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout_special);
    }
}