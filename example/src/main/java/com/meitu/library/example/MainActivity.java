package com.meitu.library.example;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.wyh.slideAdapter.FooterBind;
import com.wyh.slideAdapter.ItemBind;
import com.wyh.slideAdapter.ItemView;
import com.wyh.slideAdapter.SlideAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String FILE_KEY = "fileKey";
    public final static String VIDEO_TYPE = "video/*";
    public final static int REQUEST_VIDEO = 1;
    private static final String TAG = "MainActivity";

    private List<VideoItem> mVideoItems;
    private RecyclerView mRecyclerView;
    private SlideAdapter mSlideAdapter;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycleView);
        mVideoItems = new ArrayList<>();
        initRecyclerView();
    }

    private void selectVideo() {
        if (verifyStoragePermissions()) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VIDEO_TYPE);
            Intent chooserIntent = Intent.createChooser(intent, null);
            startActivityForResult(chooserIntent, REQUEST_VIDEO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO) {
                Uri uri = data.getData();
                if (uri != null) {
                    VideoItem videoItem = new VideoItem();
                    videoItem.setPath(UriUtil.getPath(this, uri));
                    videoItem.setFileName(new File(videoItem.getPath()).getName());
                    mVideoItems.add(videoItem);
                    mSlideAdapter.notifyDataSetChanged();
                }
            }
        }
    }
    private void editor() {

        if (verifyStoragePermissions()) {
            FileUtil.init(this);
            Intent intent = new Intent(this, VideoPlayActivity.class);
            ArrayList<String> videoPaths = new ArrayList<>();
            for (VideoItem videoItem : mVideoItems) {
                videoPaths.add(videoItem.getPath());
            }
            if (!videoPaths.isEmpty()) {
                intent.putStringArrayListExtra(FILE_KEY, videoPaths);
                startActivity(intent);
            } else {
                Toast.makeText(this, "请添加视频！", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public boolean verifyStoragePermissions() {
        int permission = ActivityCompat.checkSelfPermission(this,
                "android.permission.WRITE_EXTERNAL_STORAGE");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            showAlert();
            return false;
        } else {
            return true;
        }
    }

    private void showAlert() {
        Dialog alertDialog = new AlertDialog.Builder(this).
                setTitle("权限说明").
                setMessage("请允许SD卡读写权限以保证此Demo的正常运行").
                setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                    }
                }).
                setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                    }
                }).
                create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            editor();
        }
    }

    private void initRecyclerView() {
        mSlideAdapter = SlideAdapter.load(mVideoItems)
                .item(R.layout.item_video, 0, 0,
                        R.layout.item_right_menu, 0.3f)
                .padding(1)
                .bind(new ItemBind<VideoItem>() {
                    @Override
                    public void onBind(ItemView itemView, VideoItem videoItem, final int i) {
                        itemView.setOnClickListener(R.id.delete, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mVideoItems.remove(i);
                                mSlideAdapter.notifyDataSetChanged();
                            }
                        }).setText(R.id.fileName, videoItem.getFileName());
                        Glide.with(MainActivity.this).load(videoItem.getPath())
                                .asBitmap().into((ImageView) itemView.getView(R.id.fileImg));
                    }
                })
                .footer(R.layout.item_foot)
                .footer(R.layout.item_foot)
                .bind(new FooterBind() {
                    @Override
                    public void onBind(ItemView itemView, int i) {
                        if (i == 1) {
                            itemView.setText(R.id.footTv, "添加视频")
                                    .setOnClickListener(R.id.footTv, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            selectVideo();
                                        }
                                    });
                        }
                        if (i == 2) {
                            itemView.setText(R.id.footTv, "视频编辑")
                                    .setOnClickListener(R.id.footTv, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            editor();
                                        }
                                    });
                        }
                    }
                })
                .into(mRecyclerView);
    }
}
