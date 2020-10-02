package com.example.canvasapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    RelativeLayout layout;
    int height;
    Path mPath;
    Canvas canvas;
    Paint paint;
    float pointX;
    float pointY;
    List<Pair<Path, Integer>> path_color_list;
    List<Pair<Path, Integer>> smudge_color_list;
    Button smudge;
    Bitmap bg;
    int color;
    int mask = 0xFF;
    int alpha;
    Coordinate coordinate;
    int selectedColorForSmudge;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout=(RelativeLayout)findViewById(R.id.main_layout);
        smudge=findViewById(R.id.smudgeId);
        DisplayMetrics dp=getResources().getDisplayMetrics();
        coordinate = new Coordinate();
        WindowManager windowManager=getWindowManager();
        height=windowManager.getDefaultDisplay().getHeight();
        int width =windowManager.getDefaultDisplay().getWidth();
        path_color_list = new ArrayList<Pair<Path,Integer>>();
        smudge_color_list = new ArrayList<>();
        bg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        layout.setBackground(new BitmapDrawable(bg));
        canvas=new Canvas(bg);
        paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        color = Color.RED;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(50);
        canvas.drawBitmap(bg,0,0,paint);
    }
    public void clearCanvas(View v) {
        ColorPickerDialogBuilder
                .with(MainActivity.this)
                .setTitle("Choose color")
                .initialColor(Color.RED)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        layout.invalidate();
                        Toast.makeText(MainActivity.this,path_color_list.size()+"x",Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this,smudge_color_list.size()+"y",Toast.LENGTH_SHORT).show();
                        for (Pair<Path,Integer> path_clr : path_color_list ){
                            paint.setColor(path_clr.second);
                            canvas.drawPath( path_clr.first, paint);
                        }
                        for (Pair<Path,Integer> path_clr : smudge_color_list ){
                            paint.setColor(path_clr.second);
                            canvas.drawPath( path_clr.first, paint);
                        }
                        color = selectedColor;
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }

    public static int mixTwoColors( int color1, int color2, float amount )
    {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL   = 16;
        final byte GREEN_CHANNEL =  8;
        final byte BLUE_CHANNEL  =  0;

        final float inverseAmount = 1.0f - amount;

        int a = ((int)(((float)(color1 >> ALPHA_CHANNEL & 0xff ))));
        int r = ((int)(((float)(color1 >> RED_CHANNEL & 0xff )*amount) +
                ((float)(color2 >> RED_CHANNEL & 0xff )*inverseAmount))) & 0xff;
        int g = ((int)(((float)(color1 >> GREEN_CHANNEL & 0xff )*amount) +
                ((float)(color2 >> GREEN_CHANNEL & 0xff )*inverseAmount))) & 0xff;
        int b = ((int)(((float)(color1 & 0xff )*amount) +
                ((float)(color2 & 0xff )*inverseAmount))) & 0xff;

        return a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        pointX = event.getX();
        pointY = event.getY()-250;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.v("TAG"," action down x:"+pointX+" y:"+pointY);
                if (smudge.getText().toString().equals("Smudge"))
                {mPath = new Path();mPath.moveTo(pointX,pointY);}
                else
                {
                    mPath=new Path();
                    mPath.moveTo(pointX,pointY);
                    alpha = paint.getAlpha();
                    coordinate.setStartX(pointX);
                    coordinate.setStartY(pointY);
                    color = bg.getPixel((int)event.getX(),(int)event.getY()-250);
                    if (color!=0) {
                        color = mixTwoColors(color, selectedColorForSmudge, 0.5f);
                        alpha = 0xFF & (color >> 24);
                        if (alpha < 0)
                            alpha = 0;
                    }
               }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.v("TAG"," actionmove x:"+pointX+" y:"+pointY);
                if (smudge.getText().toString().equals("Smudge")) {
                    mPath.lineTo(pointX, pointY);
                    paint.setColor(color);
                    canvas.drawPath(mPath, paint);
                }
                else
                {
                    if (color!=0) {
                        mPath=new Path();
                        mPath.moveTo(coordinate.getStartX(),coordinate.getStartY());
                        alpha -= 10;
                        if(alpha<0)
                            alpha=0;
                        coordinate.setStartY(pointY);
                        coordinate.setStartX(pointX);
                        mPath.lineTo(pointX, pointY);
                        int r = (color >> 16) & mask;
                        int g = (color >> 8) & mask;
                        int b = color & mask;
                        paint.setColor(color);
                        paint.setAlpha(alpha);
                        canvas.drawPath(mPath, paint);
                        smudge_color_list.add(new Pair<Path, Integer>(mPath,((alpha<<24)+(r<<16)+(g<<8)+b)));
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (smudge.getText().toString().equals("Done")&&mPath!=null) {
                    Toast.makeText(MainActivity.this, smudge_color_list.size() + "updated", Toast.LENGTH_SHORT).show();
                }
                if (smudge.getText().toString().equals("Smudge")&&mPath!=null)
                {
                    path_color_list.add(new Pair<Path, Integer>(mPath,color));
                    Toast.makeText(MainActivity.this,path_color_list.size()+"path",Toast.LENGTH_SHORT).show();
                }
                break;
        }
        layout.invalidate();
        return true;
    }

    public void changeText(View view) {
        String x = smudge.getText().toString();
        if (x.equals("Smudge"))
        {
            ColorPickerDialogBuilder
                    .with(MainActivity.this)
                    .setTitle("Choose color")
                    .initialColor(Color.RED)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setOnColorSelectedListener(new OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int selectedColor) {
                        }
                    })
                    .setPositiveButton("ok", new ColorPickerClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                            selectedColorForSmudge = selectedColor;
                            smudge.setText("Done");
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .build()
                    .show();
        }
        else {
            smudge.setText("Smudge");
        }
    }

    private class Coordinate {

        private float startX;
        private float endX;
        private float startY;
        private float endY;

        public float getStartX() {
            return startX;
        }
        public void setStartX(float startX) {
            this.startX = startX;
        }
        public float getEndX() {
            return endX;
        }
        public void setEndX(float endX) {
            this.endX = endX;
        }

        public void setStartY(float startY) { this.startY = startY; }

        public float getStartY() { return startY; }
    }
}
