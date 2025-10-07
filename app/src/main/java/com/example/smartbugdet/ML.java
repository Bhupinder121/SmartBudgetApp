package com.example.smartbugdet;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ML extends AppCompatActivity{
    FloatingActionButton butt;
    String[] messages = {
            "Rs. 250 has been sent to Rahul G. from your Kotak Bank A/c. Ref ID: 987654321.",
            "This is a reminder that your bill is due on October 15, 2025. You can pay online at www.paybill.com.",
            "Your Kotak account has been debited for Rs. 650. This is a payment to S. Kulkarni.",
            "You have received a payment of $75.00 from Jane Doe. The amount has been credited to your account.",
            "Rs. 500 sent from your HDFC Bank A/c XXXXXXXXXXXX1234 to S. Kumar via UPI.",
            "Your password has been changed. If this was not you, please contact support immediately.",
            "HDFC Bank: You've sent Rs. 1850 to A. Gupta from A/c XXXXXXXXXXXX1234. Transaction complete.",
            "Your flight LH765 to London has been delayed by 2 hours. The new departure time is 11:30 AM.",
            "ALERT: You have a new message from Dr. Patel's office. Please call 555-123-4567 to confirm your appointment.",
            "A/c XXXXXXXXXXXX1234 is debited for Rs. 1200. Paid to M. Patel via UPI. - HDFC Bank",
            "Welcome to our new service! You are now subscribed to daily updates.",
            "Rs. 375 sent from your Kotak Bank A/c. Recipient: D. Anand. Ref No. 1122334455.",
            "Your verification code is 87291. Do not share this with anyone.",
            "Payment alert: Rs. 999 sent to J. Patel via UPI from HDFC Bank A/c ending in 1234.",
            "Your order #34567 is out for delivery with courier A. Tracking link: http://track.co/1a2b3c",
            "HDFC: A payment of Rs. 1500 has been sent to Ritu via UPI from your account.",
            "You've successfully sent Rs. 150 from Kotak A/c XXXXXXXXXXXX5678 to Pooja Sharma. Ref: 123456789.",
            "Congratulations! You've earned 50 bonus points on your loyalty card. Check your balance at www.points.co.",
            "Your Kotak A/c XXXXXXXXXXXX5678 has been debited by Rs. 800. Payment made to V. Singh.",
            "Get 20% off your next purchase with code SAVE20. Limited time only!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//
//        butt = findViewById(R.id.addTransactionFab);
//        butt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
////
////                if (cursor.moveToFirst()) { // must check the result to prevent exception
////                    do {
////                        String msgData = "";
////                        for(int idx=0;idx<cursor.getColumnCount();idx++)
////                        {
////                            if(cursor.getColumnName(idx).toString().equals("_id") || cursor.getColumnName(idx).toString().equals("body")){
////                                msgData += " " + cursor.getColumnName(idx) + " : " + cursor.getString(idx);
////                            }
////                        }
////                        System.out.println("SMS_RECEIVED "+ msgData);
////                        // use msgData
////                    } while (cursor.moveToNext());
////                } else {
////                    // empty box, no SMS
////                    System.out.println("not data");
////                }
////                cursor.close();
//                LlmInference.LlmInferenceOptions taskOptions = LlmInference.LlmInferenceOptions.builder()
//                        .setModelPath("/data/local/tmp/llm/q8.task")
//                        .setMaxTopK(64)
//                        .build();
//
//// Create an instance of the LLM Inference task
//                System.out.println("result STARTED");
//                LlmInference llmInference = LlmInference.createFromOptions(getApplicationContext(), taskOptions);
//                for (int i = 0; i < messages.length; i++) {
//                    String prompt = "I've forgotten the amount I have spend. tell me just the amount only." + messages[i];
//                    String result = llmInference.generateResponse("Analyze this SMS message. If it reports a debit or a payment sent from an account, respond with only the monetary amount. Otherwise, respond with only the word NULL. The message is: " + messages[i]);
//                    System.out.println("result: "+ result);
//                }
//
//            }
//        });



        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS},
                    100); // 100 is your request code
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, you can now proceed
            } else {
                // Permission was denied, handle the case gracefully
            }
        }
    }


}