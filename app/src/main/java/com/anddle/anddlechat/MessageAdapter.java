package com.anddle.anddlechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Gym on 16/6/13.
 */
public class MessageAdapter extends ArrayAdapter<ChatMessage> {

    private final LayoutInflater mInflater;
    private int mResourceMe;
    private int mResourceOthers;
    private byte[] bt;

    public MessageAdapter(Context context, int resourceMe, int resourceOthers) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);
        mResourceMe = resourceMe;
        mResourceOthers = resourceOthers;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ChatMessage message = getItem(position);
        convertView = mInflater.inflate(message.messageSender == ChatMessage.MSG_SENDER_ME ? mResourceMe:mResourceOthers, parent, false);

        TextView content = (TextView) convertView.findViewById(R.id.message_content);

            if(message.sendClass==1){
                content.setText(new String(message.messageContent));
            }
            else if(message.sendClass==2){
                Bitmap bitmap= BitmapFactory.decodeByteArray(message.messageContent,0,message.messageContent.length);
                BitmapDrawable drawable=new BitmapDrawable(bitmap);
                drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());//第一0是距左边距离，第二0是距上边距离
                content.setText("");
                content.setCompoundDrawables(drawable,null,null,null);
            }

        return convertView;
    }
}
