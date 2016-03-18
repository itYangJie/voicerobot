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
    private String[] items = {"С��", "С��", "��", "С��", "Сǿ�����ϣ�", "С÷���㶫��", "С��̨�壩", "С�أ��Ĵ���", "Сܿ��������"};
    private String[] chatNames = {"xiaoqi", "vixf", "nannan", "xiaoxin", "xiaoqiang", "xiaomei", "xiaolin", "xiaorong", "xiaoqian"};
    private ArrayList<ChatBean> mChatList = new ArrayList<ChatBean>();
    private boolean isReapt = false;
    private ChatAdapter mAdapter;

    private String[] mMMAnswers = new String[]{"Լ��?", "����!", "��Ҫ�ٽ���!",
            "�������һ����!", "Ư����?"};
    private String[] defaultAnswers = new String[]{"���ǻ���������", "�������������ѽ", "Ȼ����ʲô��û������", "�����ˣ���Ҫ��Ϣ"};
    private int[] mMMImageIDs = new int[]{R.drawable.m1, R.drawable.m2,
            R.drawable.m3};
    private int currentChoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        // ��ʼ����������
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=54b8bca3");
        sp = getSharedPreferences("config", MODE_PRIVATE);
        lvList = (ListView) findViewById(R.id.lv_list);
        mAdapter = new ChatAdapter();
        lvList.setAdapter(mAdapter);
        //��ӭ��
        mChatList.add(new ChatBean("���ѽ�����������", false, -1));
        mAdapter.notifyDataSetChanged();
        read("���ѽ�����������");
    }

    /**
     * �������öԻ���
     *
     * @param view
     */
    public void setting(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ѡ���������");
        int defaultChat = sp.getInt("defaultChat", 0);
        builder.setSingleChoiceItems(items, defaultChat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentChoice = which;
            }
        });
        builder.setPositiveButton("����", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //��������
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("defaultChat", currentChoice);
                editor.commit();
                Toast.makeText(MainActivity.this, "���óɹ�", Toast.LENGTH_SHORT).show();
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

            if (isLast) {// �Ự����
                String finalText = mTextBuffer.toString();
                mTextBuffer = new StringBuffer();// ����buffer
                //System.out.println("���ս��:" + finalText);
                mChatList.add(new ChatBean(finalText, true, -1));
                String answer;
                int imageId = -1;
                if (isReapt) {
                    answer = finalText;
                } else {
                    //Ĭ�ϻش����������
                    answer = defaultAnswers[new Random().nextInt(mMMAnswers.length)];

                    if (finalText.contains("���")) {
                        answer = "����Ҿͺã�ôô��";
                    } else if (finalText.contains("�ظ���")) {
                        answer = "�õ�Ӵ";
                        isReapt = true;
                    } else if (finalText.contains("��Ҫ�ظ���")) {
                        answer = "�ţ��õ�";
                        isReapt = false;
                    } else if (finalText.contains("����˭")) {
                        answer = "���ѽ������������������ɣ�����" + items[sp.getInt("defaultChat", 0)];
                    } else if (finalText.contains("�Է�")) {
                        answer = "�����ҳԷ��ò���ѽ";
                        imageId = R.drawable.m;
                    }
                    if (finalText.contains("Լ")) {
                        answer = "��㣬���ǲ�Լ";
                    } else if (finalText.contains("��Ů")) {
                        Random random = new Random();
                        int i = random.nextInt(mMMAnswers.length);
                        int j = random.nextInt(mMMImageIDs.length);
                        answer = mMMAnswers[i];
                        imageId = mMMImageIDs[j];
                    }
                }
                mChatList.add(new ChatBean(answer, false, imageId));// ��ӻش�����
                mAdapter.notifyDataSetChanged();// ˢ��listview

                lvList.setSelection(mChatList.size() - 1);// ��λ�����һ��

                read(answer);
            }

        }

        @Override
        public void onError(SpeechError arg0) {

        }
    };

    /**
     * ��������
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
     * ��ʼ����ʶ��
     *
     * @param view
     */
    public void startListen(View view) {
        RecognizerDialog iatDialog = new RecognizerDialog(this, null);

        // 2.������д������������ƴ�Ѷ��MSC API�ֲ�(Android)��SpeechConstant��
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

            if (item.isAsker) {// ��������
                holder.tvAsk.setVisibility(View.VISIBLE);
                holder.llAnswer.setVisibility(View.GONE);

                holder.tvAsk.setText(item.text);
            } else {
                holder.tvAsk.setVisibility(View.GONE);
                holder.llAnswer.setVisibility(View.VISIBLE);
                holder.tvAnswer.setText(item.text);
                if (item.imageId != -1) {// ��ͼƬ
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
     * ������������
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
