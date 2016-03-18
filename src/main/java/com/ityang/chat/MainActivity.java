package com.ityang.chat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends Activity {
    private SharedPreferences sp;
    private ListView lvList;
    private String[] items = {"小琪", "小峰", "楠楠", "小新", "小强（湖南）", "小梅（广东）", "小莉（台湾）", "小蓉（四川）", "小芸（东北）"};
    private String[] chatNames = {"xiaoqi", "vixf", "nannan", "xiaoxin", "xiaoqiang", "xiaomei", "xiaolin", "xiaorong", "xiaoqian"};
    private ArrayList<ChatBean> mChatList = new ArrayList<ChatBean>();
    private boolean isReapt = false;
    private ChatAdapter mAdapter;

    private String[] mMMAnswers = new String[]{"约吗?", "讨厌!", "不要再酱了!",
            "这是最后一张了!", "漂亮吧?"};
    private String[] defaultAnswers = new String[]{"你是火星来的吗", "你好厉害的样子呀", "然而我什么都没有听懂", "我累了，我要休息"};
    private int[] mMMImageIDs = new int[]{R.drawable.m1, R.drawable.m2,
            R.drawable.m3};
    private int currentChoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        // 初始化语音引擎
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=54b8bca3");
        sp = getSharedPreferences("config", MODE_PRIVATE);
        lvList = (ListView) findViewById(R.id.lv_list);
        mAdapter = new ChatAdapter();
        lvList.setAdapter(mAdapter);
        //欢迎语
        mChatList.add(new ChatBean("你好呀，我们聊天吧", false, -1));
        mAdapter.notifyDataSetChanged();
        read("你好呀，我们聊天吧");
    }

    /**
     * 跳出设置对话框
     *
     * @param view
     */
    public void setting(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择聊天对象");
        int defaultChat = sp.getInt("defaultChat", 0);
        builder.setSingleChoiceItems(items, defaultChat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentChoice = which;
            }
        });
        builder.setPositiveButton("好了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //保存数据
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("defaultChat", currentChoice);
                editor.commit();
                Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();

    }


    StringBuffer mTextBuffer = new StringBuffer();
    private RecognizerDialogListener recognizerDialogListener = new RecognizerDialogListener() {

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            // System.out.println(results.getResultString());
            // System.out.println("isLast=" + isLast);

            String text = parseData(results.getResultString());
            mTextBuffer.append(text);

            if (isLast) {// 会话结束
                String finalText = mTextBuffer.toString();
                mTextBuffer = new StringBuffer();// 清理buffer
                //System.out.println("最终结果:" + finalText);
                mChatList.add(new ChatBean(finalText, true, -1));
                String answer;
                int imageId = -1;
                if (isReapt) {
                    answer = finalText;
                } else {
                    //默认回答话语随机产生
                    answer = defaultAnswers[new Random().nextInt(mMMAnswers.length)];

                    if (finalText.contains("你好")) {
                        answer = "你好我就好！么么哒";
                    } else if (finalText.contains("重复我")) {
                        answer = "好的哟";
                        isReapt = true;
                    } else if (finalText.contains("不要重复我")) {
                        answer = "嗯，好的";
                        isReapt = false;
                    } else if (finalText.contains("你是谁")) {
                        answer = "你猜呀，好啦好啦，告诉你吧，我是" + items[sp.getInt("defaultChat", 0)];
                    } else if (finalText.contains("吃饭")) {
                        answer = "你请我吃饭好不好呀";
                        imageId = R.drawable.m;
                    }
                    if (finalText.contains("约")) {
                        answer = "姐姐，我们不约";
                    } else if (finalText.contains("美女")) {
                        Random random = new Random();
                        int i = random.nextInt(mMMAnswers.length);
                        int j = random.nextInt(mMMImageIDs.length);
                        answer = mMMAnswers[i];
                        imageId = mMMImageIDs[j];
                    }
                }
                mChatList.add(new ChatBean(answer, false, imageId));// 添加回答数据
                mAdapter.notifyDataSetChanged();// 刷新listview

                lvList.setSelection(mChatList.size() - 1);// 定位到最后一张

                read(answer);
            }

        }

        @Override
        public void onError(SpeechError arg0) {

        }
    };

    /**
     * 语音朗诵
     */
    public void read(String text) {
        SpeechSynthesizer mTts = SpeechSynthesizer
                .createSynthesizer(this, null);
        String chatName = chatNames[sp.getInt("defaultChat", 0)];
        mTts.setParameter(SpeechConstant.VOICE_NAME, chatName);
        mTts.setParameter(SpeechConstant.SPEED, "50");
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

        mTts.startSpeaking(text, null);
    }

    /**
     * 开始语音识别
     *
     * @param view
     */
    public void startListen(View view) {
        RecognizerDialog iatDialog = new RecognizerDialog(this, null);

        // 2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
        iatDialog.setParameter(SpeechConstant.DOMAIN, "iat");
        iatDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        iatDialog.setParameter(SpeechConstant.ACCENT, "mandarin");

        iatDialog.setListener(recognizerDialogListener);

        iatDialog.show();
    }

    class ChatAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mChatList.size();
        }

        @Override
        public ChatBean getItem(int position) {
            return mChatList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(MainActivity.this,
                        R.layout.list_item, null);

                holder.tvAsk = (TextView) convertView.findViewById(R.id.tv_ask);
                holder.tvAnswer = (TextView) convertView
                        .findViewById(R.id.tv_answer);
                holder.llAnswer = (LinearLayout) convertView
                        .findViewById(R.id.ll_answer);
                holder.ivPic = (ImageView) convertView
                        .findViewById(R.id.iv_pic);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ChatBean item = getItem(position);

            if (item.isAsker) {// 是提问者
                holder.tvAsk.setVisibility(View.VISIBLE);
                holder.llAnswer.setVisibility(View.GONE);

                holder.tvAsk.setText(item.text);
            } else {
                holder.tvAsk.setVisibility(View.GONE);
                holder.llAnswer.setVisibility(View.VISIBLE);
                holder.tvAnswer.setText(item.text);
                if (item.imageId != -1) {// 有图片
                    holder.ivPic.setVisibility(View.VISIBLE);
                    holder.ivPic.setImageResource(item.imageId);
                } else {
                    holder.ivPic.setVisibility(View.GONE);
                }
            }
            return convertView;
        }
    }
    static class ViewHolder {
        public TextView tvAsk;
        public TextView tvAnswer;
        public LinearLayout llAnswer;
        public ImageView ivPic;
    }
    /**
     * 解析语音数据
     *
     * @param resultString
     */
    protected String parseData(String resultString) {
        Gson gson = new Gson();
        VoiceBean bean = gson.fromJson(resultString, VoiceBean.class);
        ArrayList<VoiceBean.WSBean> ws = bean.ws;

        StringBuffer sb = new StringBuffer();
        for (VoiceBean.WSBean wsBean : ws) {
            String text = wsBean.cw.get(0).w;
            sb.append(text);
        }
        return sb.toString();
    }

}
