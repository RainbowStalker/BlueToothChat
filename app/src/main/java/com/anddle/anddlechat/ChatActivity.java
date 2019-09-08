package com.anddle.anddlechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private final int RESULT_CODE_BTDEVICE = 0;

    private ConnectionManager mConnectionManager;
    private EditText mMessageEditor;
    private TextView mSendBtn;
    private ListView mMessageListView;
    private MenuItem mConnectionMenuItem;
    private ImageView pictureImageView;
    private ImageView cameraImageView;
    private ImageView fileImageView;
    private int sendClass=0;//0无类型   1文字  2图片

    private final static int MSG_SENT_DATA = 0;
    private final static int MSG_RECEIVE_DATA = 1;
    private final static int MSG_UPDATE_UI = 2;

    private byte[] receiveAllByte=null;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SENT_DATA: {

                    byte [] data = (byte []) msg.obj;
                    boolean suc = msg.arg1 == 1;
                    if(data != null && suc) {
                        ChatMessage chatMsg = new ChatMessage();
                        chatMsg.messageSender = ChatMessage.MSG_SENDER_ME;
                        chatMsg.messageContent = subBytes(data,0,data.length-2);
                        chatMsg.sendClass=sendClass;
                        sendClass=0;
                        MessageAdapter adapter = (MessageAdapter) mMessageListView.getAdapter();
                        adapter.add(chatMsg);
                        adapter.notifyDataSetChanged();
                        mMessageEditor.setText("");
                        //Log.d("1234","handleMessageSend Content: "+new String(chatMsg.messageContent)+"   length: "+chatMsg.messageContent.length);
                        Log.d("123","handleMessageSend Content: "+"图片数据太长不显示了"+"   length: "+chatMsg.messageContent.length);
                    }
                }
                break;

                case MSG_RECEIVE_DATA: {

                    byte [] data = (byte []) msg.obj;
                    if(data != null) {
                        //将所有收到的包组合到receiveAllByte
                        if(receiveAllByte==null){
                            receiveAllByte=data;
                        }
                        else{
                            byte[] temp=new byte[receiveAllByte.length+data.length];
                            System.arraycopy(receiveAllByte, 0, temp, 0, receiveAllByte.length);
                            System.arraycopy(data,0,temp,receiveAllByte.length,data.length);
                            receiveAllByte=temp;
                        }
                       // Log.d("123","handleMessageReceive Start Content: "+new String(receiveAllByte)+"   length: "+receiveAllByte.length);
                        Log.d("123","handleMessageReceive Start Content: "+"图片数据太长不显示了"+"   length: "+receiveAllByte.length);
                        //判断receiveAllByte是否全部组合完毕
                        if((receiveAllByte[receiveAllByte.length-1]==1||receiveAllByte[receiveAllByte.length-1]==2)&&receiveAllByte[receiveAllByte.length-2]==16){
                            ChatMessage chatMsg = new ChatMessage();
                            chatMsg.messageSender = ChatMessage.MSG_SENDER_OTHERS;
                            chatMsg.messageContent = subBytes(receiveAllByte,0,receiveAllByte.length-2);
                            chatMsg.sendClass=receiveAllByte[receiveAllByte.length-1];
                            MessageAdapter adapter = (MessageAdapter) mMessageListView.getAdapter();
                            adapter.add(chatMsg);
                            adapter.notifyDataSetChanged();
                            receiveAllByte=null;
                            //Log.d("1234","handleMessageReceive End Content: "+new String(chatMsg.messageContent)+"   length: "+chatMsg.messageContent.length);
                            Log.d("1234","handleMessageReceive End Content: "+"图片数据太长不显示了"+"   length: "+chatMsg.messageContent.length);
                        }
                    }
                }
                break;

                case MSG_UPDATE_UI: {
                    updateUI();
                }
                break;
            }

        }
    };

    //截取byte数组
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!BTAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);
            finish();
            return;
        }

        int hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
            finish();
            return;
        }

        mMessageEditor = (EditText) findViewById(R.id.msg_editor);
        mMessageEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {


            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEND) {

                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        mSendBtn = (TextView) findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener(mSendClickListener);

        pictureImageView =(ImageView)findViewById(R.id.picture);
        pictureImageView.setOnClickListener(pictureClickListener);

        cameraImageView =(ImageView)findViewById(R.id.camera);
        cameraImageView.setOnClickListener(cameraClickListener);

        fileImageView =(ImageView)findViewById(R.id.file);
        fileImageView.setOnClickListener(fileClickListener);

        mMessageListView = (ListView) findViewById(R.id.message_list);
        MessageAdapter adapter = new MessageAdapter(this, R.layout.me_list_item, R.layout.others_list_item);
        mMessageListView.setAdapter(adapter);

        mConnectionManager = new ConnectionManager(mConnectionListener);
        mConnectionManager.startListen();

        if(BTAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(i);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_UPDATE_UI);
        mHandler.removeMessages(MSG_SENT_DATA);
        mHandler.removeMessages(MSG_RECEIVE_DATA);

        if(mConnectionManager != null) {
            mConnectionManager.disconnect();
            mConnectionManager.stopListen();
        }
    }

    private ConnectionManager.ConnectionListener mConnectionListener = new ConnectionManager.ConnectionListener() {
        @Override
        public void onConnectStateChange(int oldState, int State) {

            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        @Override
        public void onListenStateChange(int oldState, int State) {
            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        @Override
        public void onSendData(boolean suc, byte[] data) {

            mHandler.obtainMessage(MSG_SENT_DATA, suc?1:0, 0, data).sendToTarget();
        }

        @Override
        public void onReadData(byte[] data) {

            mHandler.obtainMessage(MSG_RECEIVE_DATA,  data).sendToTarget();

        }

    };

    private View.OnClickListener mSendClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
           sendMessage();
        }
    };

    private View.OnClickListener pictureClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent,1);
        }
    };

    private View.OnClickListener cameraClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //拍照功能暂未实现
        }
    };

    /*
     * 判断sdcard是否被挂载
     */
    private boolean hasSdcard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    private View.OnClickListener fileClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,3);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult, requestCode="+requestCode+" resultCode="+resultCode );

        if(requestCode == RESULT_CODE_BTDEVICE && resultCode == RESULT_OK) {
            String deviceAddr = data.getStringExtra("DEVICE_ADDR");
            mConnectionManager.connect(deviceAddr);
        }
        //选择图库图片
        else if(requestCode == 1 && resultCode == RESULT_OK){
            if(data!=null){
                Uri uri=data.getData();
                ContentResolver cr = this.getContentResolver();
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if(bitmap==null){
                    Log.d("123",uri.getPath());
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap=imageScale(bitmap,400,400);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] byteArray = baos.toByteArray();
                Log.d("123","arrayLength"+String.valueOf(byteArray.length));
                sendImage(byteArray);
            }
        }
    }
    /**
     * 调整图片大小
     *
     * @param bitmap
     *            源
     * @param dst_w
     *            输出宽度
     * @param dst_h
     *            输出高度
     * @return
     */
    public static Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,
                true);
        return dstbmp;
    }


    private void sendMessage() {
        String content = mMessageEditor.getText().toString();
        if(content != null) {
            content = content.trim();
            if(content.length() > 0) {
                byte one=1;
                byte[] b=addEndBytes(content.getBytes(),one);
               // Log.d("123","sendMessage Content: "+new String(b)+"   length: "+String.valueOf(b.length)+"   末尾为1");
                Log.d("123","sendMessage Content: "+"图片数据太长不显示了"+"   length: "+String.valueOf(b.length)+"   末尾为1");
                boolean ret = mConnectionManager.sendData(b);
                if(!ret) {
                    Toast.makeText(ChatActivity.this, R.string.send_fail, Toast.LENGTH_SHORT).show();
                }else{
                    sendClass=1;
                }
            }
        }
    }



    private void sendImage(byte[] imageByte){
        byte two=2;
        byte[] b=addEndBytes(imageByte,two);
        boolean ret = mConnectionManager.sendData(b);
        if(!ret) {
            Toast.makeText(ChatActivity.this, R.string.send_fail, Toast.LENGTH_SHORT).show();
        }else{
            sendClass=2;
        }
    }

    //合并两个byte数组
    public static byte[] addEndBytes(byte[] data,byte endNumber) {
        byte[] tempData = new byte[data.length + 2];
        System.arraycopy(data, 0, tempData, 0, data.length);
        tempData[tempData.length-1]=endNumber;
        tempData[tempData.length-2]=16;
        return tempData;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        mConnectionMenuItem = menu.findItem(R.id.connect_menu);
        updateUI();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.connect_menu: {
                if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
                    mConnectionManager.disconnect();

                }
                else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
                    mConnectionManager.disconnect();

                }
                else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
                    Intent i = new Intent(ChatActivity.this, DeviceListActivity.class);
                    startActivityForResult(i, RESULT_CODE_BTDEVICE);
                }

            }
            return true;

            case R.id.about_menu: {
                Intent i = new Intent(this, AboutActivity.class);
                startActivity(i);
            }
            return true;

            default:
                return false;
        }
    }



    private void updateUI()
    {
        if(mConnectionManager == null) {
            return;
        }

        if(mConnectionMenuItem == null) {
            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);

            return;
        }

        Log.d(TAG, "current BT ConnectState="+mConnectionManager.getState(mConnectionManager.getCurrentConnectState())
                +" ListenState="+mConnectionManager.getState(mConnectionManager.getCurrentListenState()));

        if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
            mConnectionMenuItem.setTitle(R.string.disconnect);

            mMessageEditor.setEnabled(true);
            mSendBtn.setEnabled(true);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
            mConnectionMenuItem.setTitle(R.string.cancel);

            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
            mConnectionMenuItem.setTitle(R.string.connect);

            mMessageEditor.setEnabled(false);
            mSendBtn.setEnabled(false);
        }
    }
}
