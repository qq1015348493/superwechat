package cn.ucai.superwechat.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.controller.EaseUI;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.ucloud.common.logger.L;
import com.ucloud.player.widget.v2.UVideoView;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.superwechat.R;
import cn.ucai.superwechat.data.LiveRoom;

public class LiveDetailsActivity extends LiveBaseActivity implements UVideoView.Callback {

    String rtmpPlayStreamUrl = "rtmp://vlive3.rtmp.cdn.ucloud.com.cn/ucloud/";
    @BindView(R.id.live_details_iv)
    ImageView liveDetailsIv;
    @BindView(R.id.screenshot_image)
    ImageView screenshotImage;
    @BindView(R.id.comment_image)
    ImageView commentImage;
    @BindView(R.id.present_image)
    ImageView presentImage;
    @BindView(R.id.chat_image)
    ImageView chatImage;
    @BindView(R.id.message_container)
    RelativeLayout messageContainer;
    @BindView(R.id.root_layout)
    RelativeLayout rootLayout;
    private UVideoView mVideoView;

    @BindView(R.id.loading_layout)
    RelativeLayout loadingLayout;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.loading_text)
    TextView loadingText;
    @BindView(R.id.cover_image)
    ImageView coverView;
    @BindView(R.id.tv_username)
    TextView usernameView;

    @Override
    protected void onActivityCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_live_details);
        ButterKnife.bind(this);


        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        LiveRoom liveRoom = getIntent().getParcelableExtra("liveroom");
        if (liveRoom == null) {
            finish();
            return;
        }
        liveId = liveRoom.getId();
        chatroomId = liveRoom.getChatroomId();
//        int coverRes = liveRoom.getCover();
//        coverView.setImageResource(coverRes);
        EaseUserUtils.setCover(this, liveRoom.getCover() + "", coverView);

        anchorId = liveRoom.getAnchorId();
        EaseUserUtils.setAppUserNick(anchorId,usernameView);
        EaseUserUtils.setAppUserAvatar(this,anchorId,liveDetailsIv);
        mVideoView = (UVideoView) findViewById(R.id.videoview);

        mVideoView.setPlayType(UVideoView.PlayType.LIVE);
        mVideoView.setPlayMode(UVideoView.PlayMode.NORMAL);
        mVideoView.setRatio(UVideoView.VIDEO_RATIO_FILL_PARENT);
        mVideoView.setDecoder(UVideoView.DECODER_VOD_SW);

        mVideoView.registerCallback(this);
        mVideoView.setVideoPath(rtmpPlayStreamUrl + liveId);
//      mVideoView.setVideoPath(rtmpPlayStreamUrl);

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isMessageListInited)
            messageView.refresh();
        EaseUI.getInstance().pushActivity(this);
        // register the event listener when enter the foreground
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister this event listener when this activity enters the
        // background
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);

        // 把此activity 从foreground activity 列表里移除
        EaseUI.getInstance().popActivity(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatroomManager().leaveChatRoom(chatroomId);

        if (chatRoomChangeListener != null) {
            EMClient.getInstance().chatroomManager().removeChatRoomChangeListener(chatRoomChangeListener);
        }
        if (mVideoView != null) {
            mVideoView.setVolume(0, 0);
            mVideoView.stopPlayback();
            mVideoView.release(true);
        }
    }

    @Override
    public void onEvent(int what, String message) {
        L.d(TAG, "what:" + what + ", message:" + message);
        Log.i("main", "what:" + what + ", message:" + message);
        switch (what) {
            case UVideoView.Callback.EVENT_PLAY_START:
                loadingLayout.setVisibility(View.INVISIBLE);
                EMClient.getInstance().chatroomManager().joinChatRoom(chatroomId, new EMValueCallBack<EMChatRoom>() {
                    @Override
                    public void onSuccess(EMChatRoom emChatRoom) {
                        chatroom = emChatRoom;
                        addChatRoomChangeListenr();
                        onMessageListInit();
                    }

                    @Override
                    public void onError(int i, String s) {

                    }
                });

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!isFinishing()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    periscopeLayout.addHeart();
                                }
                            });
                            try {
                                Thread.sleep(new Random().nextInt(400) + 200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
                break;
            case UVideoView.Callback.EVENT_PLAY_PAUSE:
                break;
            case UVideoView.Callback.EVENT_PLAY_STOP:
                break;
            case UVideoView.Callback.EVENT_PLAY_COMPLETION:
                Toast.makeText(this, "直播已结束", Toast.LENGTH_LONG).show();
                finish();
                break;
            case UVideoView.Callback.EVENT_PLAY_DESTORY:
                Toast.makeText(this, "DESTORY", Toast.LENGTH_SHORT).show();
                break;
            case UVideoView.Callback.EVENT_PLAY_ERROR:
                loadingText.setText("主播尚未开播");
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "主播尚未开播", Toast.LENGTH_LONG).show();
                break;
            case UVideoView.Callback.EVENT_PLAY_RESUME:
                break;
            case UVideoView.Callback.EVENT_PLAY_INFO_BUFFERING_START:
//                Toast.makeText(VideoActivity.this, "unstable network", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @OnClick(R.id.img_bt_close)
    void close() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }
}
