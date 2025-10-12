package com.example.smartbugdet.ui.chat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbugdet.R;
import com.example.smartbugdet.adapter.ChatAdapter;
import com.example.smartbugdet.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatDialogFragment extends DialogFragment {

    private RecyclerView rvChatMessages;
    private EditText etChatInput;
    private ImageView btnSendChatMessage;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // The layout is now the CardView, so we don't need to return a different root.
        return inflater.inflate(R.layout.dialog_chat, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatMessages = view.findViewById(R.id.rv_chat_messages);
        etChatInput = view.findViewById(R.id.et_chat_input);
        btnSendChatMessage = view.findViewById(R.id.btn_send_chat_message);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);

        btnSendChatMessage.setOnClickListener(v -> sendMessage());

        addInitialMessage();
    }

    private void addInitialMessage() {
        chatMessages.add(new ChatMessage("Hello! How can I help you with your finances today?", ChatMessage.Sender.MODEL));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
    }

    private void sendMessage() {
        String messageText = etChatInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        // Add user message to the list
        chatMessages.add(new ChatMessage(messageText, ChatMessage.Sender.USER));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatMessages.scrollToPosition(chatMessages.size() - 1);

        etChatInput.setText("");

        // TODO: Add LLM interaction here
        // For now, let's just add a dummy response
        addDummyResponse();
    }

    private void addDummyResponse() {
        chatMessages.add(new ChatMessage("This is a placeholder response from the model.", ChatMessage.Sender.MODEL));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatMessages.scrollToPosition(chatMessages.size() - 1);
    }
}
