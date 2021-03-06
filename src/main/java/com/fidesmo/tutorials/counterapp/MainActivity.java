package com.fidesmo.tutorials.counterapp;

import android.content.Intent;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.UiThread;

import java.io.IOException;

import nordpol.IsoCard;
import nordpol.android.AndroidCard;
import nordpol.android.TagDispatcher;
import nordpol.android.TagArbiter;
import nordpol.android.OnDiscoveredTagListener;
import nordpol.Apdu;

/**
 * Unique Activity in the Counter App
 * Implements two operations, triggered by two buttons:
 * - read counter
 * - decrement counter
 * The result is shown in a large number (TextView counterValue) and a second TextView (mainText)
 * is used to log the NFC actions.
 */
@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements OnDiscoveredTagListener {

    // APPLICATION_ID is the value assigned to your application by Fidesmo
    final private static String APPLICATION_ID = "42286D75";
    final private static String APP_VERSION = "0101";

    // Card commands
    final private static int READ_COUNTER = 1;
    final private static int DECREMENT_COUNTER = 2;
    // Command to be sent to the card when it is detected. Initialized to the safe option
    private int pendingCommand = READ_COUNTER;

    // For the "read counter" APDU command we will use Nordpol's built-in select operation
    // For the "decrement counter", we need to define the raw command here
    final private static byte[] DECREMENT_APDU = {0x00, 0x00, 0x01, 0x01, 0x00};

    // status code returned when the counter is empty
    final private static byte[] EMPTY_STATUS_CODE = {(byte)0x69, (byte)0x85};

    private static final String TAG = "MainActivity";

    // The TagDispatcher is responsible for managing the NFC for the activity
    private TagDispatcher tagDispatcher;

    // The TagArbiter makes it easier to connect to the card without having to remove it from the
    // phone for a second operation
    private TagArbiter tagArbiter = TagArbiter.getTagArbiter();

    private IsoCard detectedIsoCard = null;

    // UI elements
    @ViewById
    TextView mainText;
    @ViewById
    TextView counterValue;
    @ViewById
    Button readCounterButton;
    @ViewById
    Button decrementCounterButton;

    //Two methods for setting the UI (on UI thread, because, threading...)
    @UiThread
    void setMainMessage(int resource) {
        setMainMessage(getString(resource));
    }

    @UiThread
    void setMainMessage(String text) {
        mainText.setText(text);
    }

    //Showing the counter's value or an error
    @UiThread
    void showCounterValue(int value) {
        if (value >= 0) {
            counterValue.setText(String.valueOf(value));
            counterValue.setTextColor(Color.GREEN);
        } else {
            counterValue.setText(getString(R.string.counter_empty));
            counterValue.setTextColor(Color.RED);
        }
    }

    @AfterViews
    void setUpDispatcher() {
        // The first argument is the activity for which the NFC is managed
        // The second argument is the OnDiscoveredTagListener which is also implemented by this activity
        // This means that tagDiscovered will be called whenever a new tag appears
        tagDispatcher = TagDispatcher.get(this, tagArbiter, true, true);
    }

    @Override
    protected void onResume() {
        tagDispatcher.enableExclusiveNfc();
        tagArbiter.setListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        // Stop listening on the NFC interface
        tagDispatcher.disableExclusiveNfc();
        super.onPause();
    }

    /**
     * This method is called when a contactless device is detected at the NFC interface
     * @param intent the PendingIntent declared in onResume
     */
    @Override
    protected void onNewIntent(Intent intent) {
        tagDispatcher.interceptIntent(intent);
    }

    @Override
    public void tagDiscovered(Tag tag) {
        setMainMessage(R.string.reading_card);
        try {
            detectedIsoCard = AndroidCard.get(tag);
            communicateWithCard(detectedIsoCard);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a SELECT APDU to the Counter Applet on the card and parses the response
     * - If the response's status bytes are '90 00' (=APDU successfully executed, Apdu.OK_APDU), it displays the response payload
     * - If not, it assumes that Counter Applet was not installed and shows a button
     *   so the user can launch the installation process
     * @param isoCard card detected at the NFC interface, supporting ISO 14443/4 standard
     */
    private void communicateWithCard(IsoCard isoCard) {
        try {
            isoCard.connect();
            byte[] response;
            switch (pendingCommand) {
                case READ_COUNTER:
                    response = isoCard.transceive(Apdu.select(APPLICATION_ID, APP_VERSION));
                    break;
                case DECREMENT_COUNTER:
                    response = isoCard.transceive(Apdu.select(APPLICATION_ID, APP_VERSION));
                    response = isoCard.transceive(DECREMENT_APDU);
                    break;
                default:
                    // TODO: raise exception. Meanwhile, just read the counter
                    response = isoCard.transceive(Apdu.select(APPLICATION_ID, APP_VERSION));
                    Log.i(TAG, "Unknown command");
            }

            // Analyze the response. Its last two bytes are the status bytes - '90 00'/Apdu.OK_APDU means 'success'
            if (Apdu.hasStatus(response, Apdu.OK_APDU)) {
                setMainMessage(getString(R.string.select_ok));
                // we know that we are receiving only one byte: the current value of the counter
                byte[] payload = Apdu.responseData(response);
                if (payload.length > 1) {
                    // TODO: raise exception
                    Log.i(TAG, "Unexpected response: longer than the counter");
                }

                int counterValue = (int) payload[0];
                setMainMessage(getString(R.string.current_counter) + " " + counterValue);
                showCounterValue(counterValue);

            } else if (Apdu.hasStatus(response, EMPTY_STATUS_CODE)) {
                setMainMessage(getString(R.string.decrement_not_ok));
                showCounterValue(-1);
            } else {
                setMainMessage(getString(R.string.select_not_ok));
                showCounterValue(-1);
            }
            isoCard.close();
            // make sure that if the user keeps the card to the phone, the counter will not be decreased by accident
            pendingCommand = READ_COUNTER;
        } catch (IOException e) {
            // Happens when we call this function when the card has been removed
            Log.e(TAG, "Error reading card", e);
            detectedIsoCard = null;
        }
    }

    /**
     * Reads the value held by the Counter Applet by sending a SELECT operation to the card
     */
    @Click
    void readCounterButtonClicked() {
        pendingCommand = READ_COUNTER;
        setMainMessage(getString(R.string.put_card_read));
        // if a card has been detected previously, we'll attempt to send the command even though
        // it will raise an exception if the user has removed it
        if (detectedIsoCard != null) {
            communicateWithCard(detectedIsoCard);
        }
    }

    /**
     * Decrements the counter's value held by the Counter Applet.
     * Displays if already 0.
     */
    @Click
    void decrementCounterButtonClicked() {
        pendingCommand = DECREMENT_COUNTER;
        setMainMessage(getString(R.string.put_card_decrease));
        if (detectedIsoCard != null) {
            communicateWithCard(detectedIsoCard);
        }
    }
}
