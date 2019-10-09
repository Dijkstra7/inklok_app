package com.dijkstra.rick.inklok;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    HorizontalScrollView scrollView;
    LinearLayout graphLayout;
    Button startButton;
    Button endButton;
    double[] startGraph;
    double[] endGraph;
    Resources res;
    boolean startNotInUse;
    boolean endNotInUse;
    boolean firstFocusChange;
    private int pathStepWidth;
    float graphWidth;
    ImageView graphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firstFocusChange = true;
        mapGUI();
        retrieveData();
        setupButtons();
        graphLayout.post(new Runnable() {
            @Override
            public void run() {
                setupPath();
            }
        });
    }

    private void setupPath(){
        log("Setting up Paths", "start");
        log("New graph layout width is: "+ graphLayout.getWidth());
        log("New graph layout height is: "+ graphLayout.getHeight());

        graphView = new ImageView(this);
        graphLayout.addView(graphView);

        updateGraphView();

//        graphLayout.updateViewLayout(graphView, graphView.getLayoutParams());
        log(String.valueOf(graphWidth));

        // Add graphs view


        log("New graph view width is: "+ graphView.getWidth());
        log("New graph layout width is: "+ graphLayout.getWidth());
    }

    private void updateGraphView(){
        pathStepWidth = (int) Math.max(100, Math.floor(scrollView.getWidth() / (double)(startGraph.length-1)));
        graphWidth = Math.max(startGraph.length * pathStepWidth - pathStepWidth, 20);

        LayerDrawable graphsDrawable = createGraphDrawables(graphWidth);

        graphView.setLayoutParams(new LinearLayout.LayoutParams((int) graphWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        if (graphsDrawable != null) {
            graphView.setImageDrawable(graphsDrawable);
            graphView.setBackground(graphsDrawable);
        }
    }

    private LayerDrawable createGraphDrawables(float graphWidth){
        Drawable[] layers;
        if (startGraph.length>0) {

            //Create start paths
            Path startPath = createPath(startGraph, graphLayout.getHeight());

            // Create style of start path
            Paint startPaint = new Paint();
            startPaint.setStyle(Paint.Style.STROKE);
            startPaint.setColor(res.getColor(R.color.startButton));
            startPaint.setStrokeWidth(20);

            //Create layer
            ShapeDrawable startGraphDrawable = new ShapeDrawable(new PathShape(startPath, graphWidth, graphLayout.getHeight()));
            startGraphDrawable.getPaint().set(startPaint);
            startGraphDrawable.setBounds(0, 0, (int) graphWidth, graphLayout.getHeight());

            if (endGraph.length > 0) {
                // Create end path
                Path endPath = createPath(endGraph, graphLayout.getHeight());

                // Create style of graph
                Paint endPaint = new Paint();
                endPaint.setStyle(Paint.Style.STROKE);
                endPaint.setColor(res.getColor(R.color.endButton));
                endPaint.setStrokeWidth(20);

                // Create layer
                ShapeDrawable endGraphDrawable = new ShapeDrawable(new PathShape(endPath, graphWidth, graphLayout.getHeight()));
                endGraphDrawable.getPaint().set(endPaint);
                endGraphDrawable.setBounds(0, 0, (int) graphWidth, graphLayout.getHeight());

                // Combine layers
                layers = new Drawable[2];
                layers[0] = startGraphDrawable;
                layers[1] = endGraphDrawable;
            }
            else{
                layers = new Drawable[1];
                layers[0] = startGraphDrawable;
            }
            // Create graphs view
            return new LayerDrawable(layers);
        } else {
            return null;
        }
    }

    private Path createPath(double[] graph, int height) {
        Path path = new Path();
        path.moveTo(0, (float) (height - graph[0]/24*height));
        for (int i = 1; i < graph.length; i++) {
            path.lineTo(i*pathStepWidth, (float) (height - graph[i]/24*height));
        }
        if (graph.length == 1) {
            path.lineTo(20, (float) (height - graph[0]/24*height));
        }
        return path;
    }

    private void mapGUI(){
        res = getResources();
        graphLayout = findViewById(R.id.graphLayout);
        scrollView = findViewById(R.id.scrollLayout);
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        endButton = findViewById(R.id.endButton);
        endButton.setOnClickListener(this);
    }

    private void setupButtons() {
        if (startGraph.length > endGraph.length) {
            startButton.setBackground(res.getDrawable(R.drawable.round_not_in_use_button));
            endButton.setBackground(res.getDrawable(R.drawable.round_end_button));
            startNotInUse = true;
            endNotInUse = false;
        }
        else
        {
            endButton.setBackground(res.getDrawable(R.drawable.round_not_in_use_button));
            startButton.setBackground(res.getDrawable(R.drawable.round_start_button));
            startNotInUse = false;
            endNotInUse = true;
        }
    }

    private void retrieveData() {
        boolean retrievedData = getDataFromStorage();
        if (!retrievedData) {
            startGraph = new double[]{10.75, 10.66, 10.20};
            endGraph = new double[]{17.05, 14.33, 18.75};
        }
    }

    private boolean getDataFromStorage(){
        boolean retrieved = false;
        File directory = this.getFilesDir();
        File file = new File(directory, "save_data");
        if (file.canRead()){
            unpackFile(file);
            retrieved = true;
        }
        log("Opening file, "+file.canRead());
        return retrieved;
    }

    private void unpackFile(File file) {
        String fileString = "";
        try {
            InputStream inputStream = new FileInputStream(file);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            final StringBuilder stringBuilder = new StringBuilder();

            boolean done = false;

            while (!done) {
                final String line = reader.readLine();
                done = (line == null);

                if (line != null) {
                    stringBuilder.append(line);
                }
            }

            reader.close();
            inputStream.close();

            fileString = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stringToGraphs(fileString);
    }

    private void stringToGraphs(String fileString) {
        String[] graphStrings = fileString.replace("[", "").replace("]", "").split(":");
        startGraph = stringToGraph(graphStrings[0]);
        endGraph = stringToGraph(graphStrings[1]);
    }

    private double[] stringToGraph(String graphString) {
        String[] valueStrings = graphString.split(";");
        double[] returnGraph = new double[valueStrings.length];
        for (int i = 0; i < valueStrings.length; i++) {
            returnGraph[i] = Double.valueOf(valueStrings[i]);
        }
        return returnGraph;
    }


    private void addStartValue(double timeDouble){
        double[] oldStartGraph = startGraph.clone();
        startGraph = new double[startGraph.length+1];
        System.arraycopy(oldStartGraph, 0, startGraph, 0, oldStartGraph.length);
        startGraph[startGraph.length-1] = timeDouble;
        saveStates();
    }

    private void addEndValue(double timeDouble) {
        double[] oldEndGraph = endGraph.clone();
        endGraph = new double[endGraph.length+1];
        System.arraycopy(oldEndGraph, 0, endGraph, 0, oldEndGraph.length);
        endGraph[endGraph.length-1] = timeDouble;
        saveStates();
    }

    private void saveStates() {
        String filename = "save_data";
        String fileContents = graphListToString(startGraph)+":"+graphListToString(endGraph);
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String graphListToString(double[] graph){
        StringBuilder returnString = new StringBuilder();
        for (int i = 0; i < graph.length-1; i++) {
            returnString.append(graph[i]).append(";");
        }
        if (graph.length>0) {
            returnString.append(graph[graph.length - 1]);
        }
        return returnString.toString();
    }

    private double getCurrentTimeAsDouble(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        String currentTime = sdf.format(new Date());
        double hourDouble = (double) Integer.valueOf(currentTime.substring(0,2));
        double minuteDouble = (double) Integer.valueOf(currentTime.substring(3));
        return hourDouble + minuteDouble/60;
    }

    private void log(String message){
        log("Logging", message);
    }

    private void log(String tag, String message){
        Log.d(tag, message);
    }

    @Override
    public void onClick(View v) {
        if (v==startButton) {
            if (startNotInUse) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure you want to add a new start time and thus not enter an end-moment?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addStartValue(getCurrentTimeAsDouble());
                        addEndValue(.0);
                        setupButtons();
                        updateGraphView();
                        dialog.cancel();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            } else {
                addStartValue(getCurrentTimeAsDouble());
                setupButtons();
                updateGraphView();
            }
        }
        if (v==endButton) {
            if (endNotInUse) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure you want to add a new end time and thus not enter an start-moment?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addEndValue(getCurrentTimeAsDouble());
                        addStartValue(.0);
                        setupButtons();
                        updateGraphView();
                        dialog.cancel();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            } else {
                addEndValue(getCurrentTimeAsDouble());
                setupButtons();
                updateGraphView();
            }
        }
    }
}
