package com.pritesh.ritintercom.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.pritesh.ritintercom.R;
import com.pritesh.ritintercom.TinyDB;
import com.pritesh.ritintercom.data.ChatData;
import com.pritesh.ritintercom.datatransfer.DataSender;
import com.pritesh.ritintercom.utils.ConnectionUtils;
import com.pritesh.ritintercom.utils.NotificationToast;
import com.pritesh.ritintercom.utils.Utility;


import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String ACTION_CHAT_RECEIVED = "com.pritesh.ritintercom.chatreceived";
    public static final String KEY_CHAT_DATA = "chat_data_key";

    public static final String KEY_CHATTING_WITH = "chattingwith";
    public static final String KEY_CHAT_IP = "chatterip";
    public static final String KEY_CHAT_PORT = "chatterport";

    EditText etChat;
    RecyclerView chatListHolder;
    private ArrayList<ChatData> chatList;
    private ChatListAdapter chatListAdapter;

    private String chattingWith;
    private String destIP;
    private int destPort;

    boolean isNotif;
    TinyDB tinydb ;//= new TinyDB(context);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initialize();

        chatListHolder = (RecyclerView) findViewById(R.id.chat_list);
        etChat = (EditText) findViewById(R.id.et_chat_box);

        chatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(chatList);
        chatListHolder.setAdapter(chatListAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        linearLayoutManager.setStackFromEnd(true);
        chatListHolder.setLayoutManager(linearLayoutManager);
        tinydb = new TinyDB(getApplicationContext());


        if(isNotif){
            ArrayList<ChatData> chatDatas = HomeActivity.chatDatas;

            for (ChatData chatData:chatDatas)
            {

                updateChatView(chatData);

            }

        }

       if( tinydb.getListObject(chattingWith,ChatData.class).size() >0){

           for (ChatData chatData:tinydb.getListObject(chattingWith,ChatData.class))
           {
               updateChatView(chatData);
           }

       }
    }

    private void initialize() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CHAT_RECEIVED);


        LocalBroadcastManager.getInstance(ChatActivity.this).registerReceiver(chatReceiver, filter);

        if (getIntent().hasExtra("fromNotification")) {

            isNotif = true;

            Bundle extras = getIntent().getExtras();

            chattingWith = extras.getString(KEY_CHATTING_WITH);
            destIP = extras.getString(KEY_CHAT_IP);
            destPort = extras.getInt(KEY_CHAT_PORT);

            setToolBarTitle("Chat with " + chattingWith);



        }else{
            isNotif = false;

            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                NotificationToast.showToast(ChatActivity.this, "Invalid arguments to open chat");
                finish();
            }

            chattingWith = extras.getString(KEY_CHATTING_WITH);
            destIP = extras.getString(KEY_CHAT_IP);
            destPort = extras.getInt(KEY_CHAT_PORT);
        }




    }

    public void SendChatInfo(View v) {
        String message = etChat.getText().toString();

        ChatData myChat = new ChatData();
        myChat.setPort(ConnectionUtils.getPort(ChatActivity.this));
        myChat.setFromIP(Utility.getString(ChatActivity.this, "myip"));
        myChat.setLocalTimestamp(System.currentTimeMillis());
        myChat.setMessage(message);
        myChat.setSentBy(chattingWith);
        myChat.setMyChat(true);
        DataSender.sendChatInfo(ChatActivity.this, destIP, destPort, myChat);

        etChat.setText("");
//        chatListHolder.smoothScrollToPosition(chatList.size() - 1);

        updateChatView(myChat);
    }

    @Override
    public void onBackPressed() {


        tinydb.remove(chattingWith);
        tinydb.putListObject(chattingWith,chatList);

        finish();
        return;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CHAT_RECEIVED:
                    ChatData chat = (ChatData) intent.getSerializableExtra(KEY_CHAT_DATA);
                    chat.setMyChat(false);
                    updateChatView(chat);
                    break;
                default:
                    break;
            }
        }
    };







    private void updateChatView(ChatData chatObj) {





        chatList.add(chatObj);
        chatListAdapter.notifyDataSetChanged();
        chatListHolder.smoothScrollToPosition(chatList.size() - 1);
    }

    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatHolder> {

        private List<ChatData> chatList;

        ChatListAdapter(List<ChatData> chatList) {
            this.chatList = chatList;
        }

        @Override
        public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_mine,
                        parent, false);
                return new ChatHolder(itemView);
            } else {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_other,
                        parent, false);
                return new ChatHolder(itemView);
            }
        }



        @Override
        public void onBindViewHolder(ChatHolder holder, int position) {
            holder.bind(chatList.get(position));
        }

        @Override
        public int getItemCount() {
            return chatList == null ? 0 : chatList.size();
        }

        @Override
        public int getItemViewType(int position) {
            ChatData chatObj = chatList.get(position);
            return (chatObj.isMyChat() ? 0 : 1);
        }

        class ChatHolder extends RecyclerView.ViewHolder {
            TextView tvChatMessage;

            public ChatHolder(View itemView) {
                super(itemView);
                tvChatMessage = (TextView) itemView.findViewById(R.id.tv_chat_msg);
            }

            public void bind(ChatData singleChat) {
                tvChatMessage.setText(singleChat.getMessage());
            }
        }
    }

    private void setToolBarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
}
