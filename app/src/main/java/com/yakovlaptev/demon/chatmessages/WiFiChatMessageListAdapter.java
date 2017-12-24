package com.yakovlaptev.demon.chatmessages;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yakovlaptev.R;
import com.yakovlaptev.demon.data.ImageConverter;
import com.yakovlaptev.demon.model.LocalP2PDevice;

import java.util.zip.Inflater;

class WiFiChatMessageListAdapter extends ArrayAdapter<String> {

    private final WiFiChatFragment chatFragment;

    public WiFiChatMessageListAdapter(Context context, int textViewResourceId,
                                      WiFiChatFragment chatFragment) {
        super(context, textViewResourceId, chatFragment.getItems());
        this.chatFragment = chatFragment;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        Inflater inflater = new Inflater();

        if (v == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatmessage_row, parent, false);
        }

        String message = chatFragment.getItems().get(position);
        if (message != null && !message.isEmpty()) {
            TextView nameText = (TextView) v.findViewById(R.id.message);
            ImageView avatarMessage = (ImageView) v.findViewById(R.id.imageView);
            if (nameText != null) {
                if(!message.contains(":")) {
                    avatarMessage.setImageBitmap(ImageConverter.convertToBitmap(LocalP2PDevice.getInstance().getProfile().getAvatar()));
                } else {
                    if(chatFragment.getDeviceAvatar() != null && chatFragment.getDeviceAvatar().length != 0) {
                        avatarMessage.setImageBitmap(ImageConverter.convertToBitmap(chatFragment.getDeviceAvatar()));
                    } else {
                        avatarMessage.setImageDrawable(this.getContext().getResources().getDrawable(R.drawable.user_logo));
                    }
                }
                nameText.setText(message);
                nameText.setTextAppearance(chatFragment.getActivity(), R.style.normalText);
                if (chatFragment.isGrayScale()) {
                    nameText.setTextColor(chatFragment.getResources().getColor(R.color.gray));
                }
            }
        }
        return v;
    }

}