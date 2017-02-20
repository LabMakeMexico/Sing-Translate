/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.io.UnsupportedEncodingException;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private TextView translateText;
    //private Button mSendButton;
    private TextView mdataview;

    //Strings
    String inputS = "";
    String axis1 = "", axis2 = "", axis3 = "", axis4 = "", axis5 = "", axisAx = "", axisAy = "";
    int s1 = 0, s2 = 0, s3 = 0, s4 = 0, s5 = 0, ax = 0, ay = 0;

    //Maximos y Minimos
    int min_1 = 50;
    int max_1 = 70;
    int min_2 = 50;
    int max_2 = 75;
    int min_3 = 51;
    int max_3 = 75;
    int min_4 = 50;
    int max_4 = 75;
    int min_5 = 52;
    int max_5 = 75;
    int max_x;
    int min_x;
    int max_y;
    int min_y;
    boolean data_correct;
    int error=6;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        translateText = (TextView) view.findViewById(R.id.dataTranslate);
        mdataview = (TextView) view.findViewById(R.id.dataview);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //  translateText.setOnEditorActionListener(mWriteListener);

       /* // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });*/

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //  translateText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    System.out.println("esta ecribiendo mensaje");
                    break;
                case Constants.MESSAGE_READ:
                    /*Aqui se reciben los mensajes del arduino y los pasa a una cadena de caracteres o String*/
                    String readBuf = (String) msg.obj;
                    // almacena los datos en otra variable donde se acumunlan
                    inputS += readBuf;
                    // aparece un asterisco en la cadena entra ala condicion
                    if (readBuf.equals("*")) {
                        /* ya que tenemos toda la trama de datos que necesitamos, tenemos que particionar la cadena en cadena mas pequeñas
                        * para eso utilizamos el metodo substring(), luego de eso se almacenas los datos en sus variables
                        * correspondientes. Utilizando el try-catch nos aseguramos de que los datos se almacenas de manera correcta.*/
                        //System.out.println("Cadena mandada: "+inputS);
                        axis1 = inputS.substring(0, 2);
                        axis2 = inputS.substring(2, 4);
                        axis3 = inputS.substring(4, 6);
                        axis4 = inputS.substring(6, 8);
                        axis5 = inputS.substring(8, 10);
                        axisAx = inputS.substring(10, 13);
                        axisAy = inputS.substring(13, 16);
                        try {
                            s1 = Integer.valueOf(axis1);
                            s2 = Integer.valueOf(axis2);
                            s3 = Integer.valueOf(axis3);
                            s4 = Integer.valueOf(axis4);
                            s5 = Integer.valueOf(axis5);
                            ax = Integer.valueOf(axisAx);
                            ay = Integer.valueOf(axisAy);
                            data_correct = true;
                        } catch (NumberFormatException e) {
                            //Will Throw exception!
                            //do something! anything to handle the exception.
                            data_correct = false;
                        }
                        // sout para depurar la app
                        System.out.println("s: " + axis1 + " " + axis2 + " " + axis3 + " " + axis4 + " " + axis5 + " " + axisAx + " " + axisAy);
                        // se llama al metodo que traducira las señas en letras
                        traslate();
                        // la cadena se limpia para que recibir la proxima trama de datos
                        inputS = "";

                    }
                    // se crea un formatos de presentacion de datos para la app
                    String head = " Val1  val2  val3  val4  val5  angx  angy";
                    String fin = "" + axis1 + "   " + axis2 + "   " + axis3 + "   " + axis4 + "   " + axis5 + "  " + axisAx + "  " + axisAy;
                    // se muestran los datos en la app con settex();
                    mdataview.setText(head);
                    mConversationArrayAdapter.add(fin);

                    //  System.out.println("se lee un mensaje");
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void traslate() {
        // here code of translater app
        System.out.println("entro al metodo");
        // si no hubo un error cuando se almecenaron las variables
        if (data_correct) {
            //aqui se llamaran a metodos donde se compararan los datos obtenidos con los calculados para generar la traduccion
            System.out.println("entro al if");
            if (a()){translateText.setText(" A ");
                System.out.println("a");
            }if(b()){translateText.setText(" B ");
                System.out.println("b");
            }if(c()){translateText.setText(" C ");
                System.out.println("c");
            }else {
               // translateText.setText("no translate");
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    // if condition, but transformt in funtions

    public boolean a() {
        if (s1 >= min_1 && s1 <= min_1 + error & s2 <= max_2 && s2 > max_2 - error & s3 <= max_3 && s3 > max_3 - error & s4 <= max_4 && s4 > max_4 - error & s5 <= max_5 && s5 > max_5 - error) {
            return true;
        }
        return false;}
    public boolean b() {
        if (s1 <= max_1 && s1 >= max_1 - error & s2 >= min_1 && s2 < min_2 + error & s3 >= min_3 && s3 < min_3 + error & s4 >= min_4 && s4 < min_4 + error & s5 >= min_5 && s5 < min_5 + error) {
            return true;
        }
        return false;}
    public boolean c() {
        if (s1 >= (max_1/4)-3 && s1 <= (max_1/4)+3 & s2>= (max_2/4)-3 && s2 <= (max_2/4)+3 & s3 >= (max_3/4)-3 && s3 <= (max_3/4)+3 & s4 >= (max_4/4)-3 && s4 <= (max_4/4)+3 & s5 >= (max_5/4)-3 && s5 <= (max_5/4)+3) {
            return true;
        }
        return false;}
    public boolean d() {
        if (s1 <= max_1 && s1 > max_1 - error & s2 >= min_2 && s2 <= min_2 + error & s3 <= max_3 && s3 > max_3 - error &s4 <= max_4 && s4 > max_4 - error & s5 <= max_5 && s5 > max_5 - error) {
            return true;
        }
        return false;}

}
