package jimmyibarra.provincestatequiz;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class MainActivityFragment extends Fragment {
    public static final int REQUEST_CODE_BONUS= 17;
    //String used when logging error messages
    private static final String TAG = "FlagQuiz Activity";
    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; //flag file names
    private List<String> quizCountriesList; //countries in current quiz
    private Set<String> regionsSet;
    private String correctAnswer;
    private boolean isQuizFinish = false;
    private boolean isFirstTry = true;
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows;
    private SecureRandom random;
    private Handler handler;
    private Animation shakeAnimation;
    private int totalPoints = 0;
    private int points = 10;
    private LinearLayout quizLinearLayout;
    private TextView questionNumberTextView;
    private ImageView imageView;
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main,container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //load the shake animation that's used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        //get references to GUI components
        quizLinearLayout = view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView =  view.findViewById(R.id.questionNumberTextView);
        imageView = view.findViewById(R.id.imageView);
        guessLinearLayouts = new LinearLayout[3];
        guessLinearLayouts[0] = view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = view.findViewById(R.id.row3LinearLayout);
        answerTextView = view.findViewById(R.id.answerTextView);

        //configure listeners for the guess buttons
        for(LinearLayout row : guessLinearLayouts){
            for(int column = 0 ; column < row.getChildCount(); column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        questionNumberTextView.setText(getString(R.string.question,1,FLAGS_IN_QUIZ));
        return view; //return the fragment's view for display
    }

    //update guessRpws based on value in SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences){
        //get the number of guess buttons that should be displayed
        String choices = sharedPreferences.getString(MainActivity.CHOICES,null);
        guessRows = (Integer.parseInt(choices)) / 2;

        //hide all guess button LinearLayouts
        for(LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        //display appropriate guess button LinearLayouts
        for(int row=0;row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz(){
        //use AssetManager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); //empty list of image file names

        try{
            for(String region : regionsSet){
                //get a list of all flag image files in this region
                String[] paths = assets.list(region);

                for(String path : paths)
                    fileNameList.add(path.replace(".JPG", ""));
            }
        }catch(IOException exception){
            Log.e(TAG, "Error loading image file names", exception);
        }
        totalPoints = 0;
        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();
        //add FLAGS_IN_QUIZ
        while(flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            //get the random file name
            String filename = fileNameList.get(randomIndex);

            //if the region is enabled and it hasn't already been chosen
            if(!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }

        loadNextFlag();
    }

    private void loadNextFlag(){
        //get filename of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");
        isFirstTry = true;
        points = 10;
        //display current question number
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //extract the region from the next image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //use AssetManager to load ext image fro assets folder
        AssetManager assets = getActivity().getAssets();

        //get an InputStream, to the asset representing the next flag
        //and try to use the InputStream

        try(InputStream stream = assets.open(region + "/" + nextImage + ".JPG")){
            //load the asset as a Drawable and display on the imageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            imageView.setImageDrawable(flag);
            animate(false);
        }catch(IOException exception){
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList);

        //put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //add 2,4,6 or 8 guess Buttons based on the value of guessRows
        for(int row = 0; row < guessRows; row++){
            for(int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){
                //get reference to Button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row*2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }
        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private String getCountryName(String name){
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }
    private void animate(boolean animateOut){
        //prevent animation into the UI for the first flag
        if(correctAnswers == 0 ){
            return;
        }

        //calculate center x and center y

        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        //calculate radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        //if the quizLinearLayout should animate out rather than in
        if(animateOut){
            //create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY,radius,0);
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextFlag();
                        }
                    }
            );
        }else{ //if the quizLinearLayout should animate in
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0 , radius);
        }

        animator.setDuration(500);
        animator.start();
    }

    private final OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;

            if (guess.equals(answer)) {

                if(isFirstTry){
                    Intent i= new Intent(v.getContext(), BonusActivity.class);
                    i.putExtra("provinceState", answer);
                    startActivityForResult(i, 1);
                }
                totalPoints += points;
                Log.i("POINTS", totalPoints + "");
                points = 10;
                ++correctAnswers;

                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));
                disableButtons();

                if (correctAnswers == FLAGS_IN_QUIZ) {
                    if(!isFirstTry)
                         showDialog();
                } else {
                    handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                animate(true);
                                            }
                                        }
                            , 2000);
                }
            } else {
                points--;
                imageView.startAnimation(shakeAnimation);
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);
                isFirstTry = false;
            }
        }
    };

    private void disableButtons(){
        for(int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for(int i = 0; i < guessRow.getChildCount(); i++) {
                guessRow.getChildAt(i).setEnabled(true);
            }
        }
    }
    public static class PopupDialog extends DialogFragment {

        private buttonFunction buttonFunction;

        public static PopupDialog newInstance(int totalGuesses, int points){
            PopupDialog dialog = new PopupDialog();
            //Hold new Arugments
            Bundle args = new Bundle();
            //Set argument for Total Guesses
            args.putInt("TotalGuesses", totalGuesses);
            args.putInt("Points", points);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int totalGuesses = getArguments().getInt("TotalGuesses");
            int totalPoints = getArguments().getInt("Points");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.results, totalGuesses, (1000/ (double) totalGuesses)) + "\nYou scored: " + totalPoints + "points");

            builder.setPositiveButton(R.string.reset_quiz,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(buttonFunction != null)
                        buttonFunction.onButtonPress();
                }
            });
            return builder.create();
        }

        public interface buttonFunction{
            void onButtonPress();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 1){
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                totalPoints += Integer.parseInt(result);
                if(correctAnswers == FLAGS_IN_QUIZ){
                    showDialog();
                }
                Log.i("ADDEDPOINTS",result.toString());
            }
        }
    }

    public void showDialog(){
        PopupDialog quizResults = PopupDialog.newInstance(totalGuesses, totalPoints);
        quizResults.buttonFunction = new PopupDialog.buttonFunction() {
            @Override
            public void onButtonPress() {
                resetQuiz();
            }
        };
        quizResults.setCancelable(false);
        quizResults.show(getFragmentManager(),"quizResults");
    }
}