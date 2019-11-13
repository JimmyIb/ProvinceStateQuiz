package jimmyibarra.provincestatequiz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.util.Scanner;

public class BonusActivity extends AppCompatActivity {
    private String stateOrProvince = "";
    private String capital = "";
    TextView capitalAnswer;
    Button submitButton;
    EditText capitalEditText;
    String points = "10";
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bonus_question);
        stateOrProvince = getIntent().getStringExtra("provinceState");
        TextView question = (TextView) findViewById(R.id.question);
        question.setText("What is the capital of " + stateOrProvince + "?");
        capital = getCapital(stateOrProvince);
        capitalAnswer = findViewById(R.id.capitalAnswer);
        submitButton = findViewById(R.id.submitButton);
        capitalEditText = findViewById(R.id.capitalEditText);
        submitButton.setOnClickListener(submitButtonListener);
        Log.i("answer", capital);

    }

    private final OnClickListener submitButtonListener = new OnClickListener(){

        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = capitalEditText.getText().toString();
            if(guess.equalsIgnoreCase(capital.trim())){
                capitalAnswer.setText(capital);
                capitalAnswer.setTextColor(Color.parseColor("#00CC00"));
            }else{
                capitalAnswer.setText(capital);
                capitalAnswer.setTextColor(Color.parseColor("#FF0000"));
                points = "0";
            }

            new CountDownTimer(2000, 1000){
                @Override
                public void onTick(long l) {

                }

                public void onFinish(){
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", points);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            }.start();
        }
    };

    private String getCapital(String s){
        String raw = "";
        s = s.replaceAll("\\s", "");
        Log.i("MYTAG", s);
        try {
            DataInputStream file = new DataInputStream(getAssets().open(String.format("capitals.txt")));
            Scanner scan = new Scanner(file);
            while(scan.hasNextLine()){
                if(scan.next().equalsIgnoreCase(s)){
                    raw = scan.nextLine();
                    Log.i("MYTAG", raw);
                    break;
                }
            }


            capital = raw.trim().substring(2);

            Log.i("MYTAG", capital);
        }catch(Exception e){
            Log.i("ASDASDASD", "cannot find file");
        }

        return capital;
    }
}
