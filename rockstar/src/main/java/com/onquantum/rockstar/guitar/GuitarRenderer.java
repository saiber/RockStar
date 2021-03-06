package com.onquantum.rockstar.guitar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;
import android.view.SurfaceHolder;

import com.onquantum.rockstar.R;
import com.onquantum.rockstar.Settings;
import com.onquantum.rockstar.common.Pentatonic;
import com.onquantum.rockstar.sequencer.QSoundPool;
import com.onquantum.rockstar.svprimitive.SBitmap;
import com.onquantum.rockstar.svprimitive.SCircle;
import com.onquantum.rockstar.svprimitive.SGuitarString;
import com.onquantum.rockstar.svprimitive.SLine;
import com.onquantum.rockstar.svprimitive.SShape;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by Admin on 8/16/14.
 */
public class GuitarRenderer implements SurfaceHolder.Callback {

    private int fretCount = 0;
    private float fretWidth = 0;

    private DrawFrame drawFrame;
    private float width, height;
    private Context context;

    private List<SShape> backGroundLayer = Collections.synchronizedList(new ArrayList<SShape>());
    private List<SShape> guitarString = Collections.synchronizedList(new ArrayList<SShape>());
    private List<SShape> pentatonicObjectsList = Collections.synchronizedList(new ArrayList<SShape>());
    private List<SShape> testLine = Collections.synchronizedList(new ArrayList<SShape>());
    private List<SShape> barNumberObjects = Collections.synchronizedList(new ArrayList<SShape>());
    private List<SShape> pentatonicMask = Collections.synchronizedList(new ArrayList<SShape>());

    private List<SShape> drawObjects = Collections.synchronizedList(new ArrayList<SShape>());


    private boolean loaded = false;
    private boolean enableRendering = false;

    private List<Pentatonic>pentatonicList = null;
    private Pentatonic currentPentatonic = null;
    private boolean isPentatonicLoaded = false;
    private int currentPentatonicStep = 0;
    private SShape current;

    private Paint circlePaint;
    private Paint touchPaint;

    private BitSet fretsMark = new BitSet(24);

    // Frets view
    private RectF fretsVisibleArea;
    private RectF openStringDrawArea;
    private boolean fretsNumberVisible = false;
    private boolean touchVisible = false;
    private boolean sliderFretsVisible = false;
    private boolean isNotesVisible = false;

    //Slide
    private int Slide = 0;

    public GuitarRenderer(Context context) {
        fretCount = new Settings(context).getFretNumbers();
        this.context = context;

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.RED);
        circlePaint.setAlpha(200);

        touchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        touchPaint.setColor(Color.BLUE);
        touchPaint.setAlpha(128);

        fretsMark.set(0);
        fretsMark.set(2);
        fretsMark.set(4);
        fretsMark.set(6);
        fretsMark.set(8);
        fretsMark.set(11);
        fretsMark.set(14);
        fretsMark.set(16);
        fretsMark.set(18);
        fretsMark.set(20);
        fretsMark.set(23);

        Settings settings = new Settings(context);
        touchVisible = settings.isTouchesVisible();
        fretsNumberVisible = settings.isFretsNumberVisible();
        sliderFretsVisible = settings.isFretsSliderVisible();
        isNotesVisible = settings.getShowNotes();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        drawFrame = new DrawFrame(surfaceHolder);
        drawFrame.setRunning(true);
        drawFrame.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int var, int width, int height) {
        //Log.i("info"," GuitarRenderer surfaceChanged with = " + width + " height = " + height + "  loaded = " + loaded);
        if (loaded)
            return;

        Slide = 0;
        backGroundLayer.clear();
        guitarString.clear();
        testLine.clear();
        drawObjects.clear();

        this.width = width;
        this.height = height;

        fretWidth = (int)(this.width / fretCount);
        float step = fretWidth;
        float heightDiv = this.height / 14;

        // Fret visible rect
        fretsVisibleArea = new RectF(0.0f, height, width, 0.0f);
        if((new Settings(context).getOpenStringStatus())) {
            fretsVisibleArea.left = fretWidth;
            openStringDrawArea = new RectF(0.0f, height, width, 0.0f);
        }

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.RED);
        for (int i = 0; i<6; i++) {
            SLine line = new SLine(0,height - (height / 6f * i),width,height - (height / 6f * i),linePaint);
            line.setLayer(Layer.PENTATONIC_LAYER);
            testLine.add(line);
        }

        /******************************************************************************************/
        // Draw frets
        /******************************************************************************************/
        float fret = 1;
        for(int i = 0; i<24; i++) {
            SBitmap bitmap;
            if (fretsMark.get(i)) {
                bitmap = new SBitmap(this.width - step, 0, fretWidth, this.height, context, R.drawable.b1);
            } else {
                bitmap = new SBitmap(this.width - step, 0, fretWidth, this.height, context, R.drawable.b0);
            }
            bitmap.setLayer(Layer.BACKGROUND_LAYER_0);
            bitmap.setVisibleArea(fretsVisibleArea);
            bitmap.setKinematic(true);
            backGroundLayer.add(bitmap);

            // Draw open string fret
            if(new Settings(context).getOpenStringStatus()) {
                if(i == (fretCount-1)) {
                    SBitmap soundCapture;
                    soundCapture = new SBitmap(this.width - step,0,fretWidth,this.height,context, R.drawable.sound_capture);
                    soundCapture.setLayer(Layer.BACKGROUND_LAYER_0);
                    soundCapture.setKinematic(false);
                    soundCapture.setVisibleArea(new RectF(width - step - fretWidth, height, fretWidth, 0.0f));
                    backGroundLayer.add(soundCapture);
                }
            }
            step += fretWidth;
            fret++;
        }

        // Draw frets on visible neck
        float stepLad = 0;
        float ladWidth = fretWidth * 0.16f;
        float ld = ladWidth / 2.0f;
        int visibleFrets = fretCount;
        if(new Settings(context).getOpenStringStatus() == false)
            visibleFrets+=1;

        for (int i = 0; i < visibleFrets; i++) {
            SBitmap bitmap = new SBitmap(
                    this.width - stepLad - ld,
                    0,
                    ladWidth,
                    this.height,context,
                    R.drawable.lad
            );
            bitmap.setLayer(Layer.BACKGROUND_LAYER_2);
            bitmap.setKinematic(false);
            bitmap.setVisibleArea(new RectF(-ladWidth, height, width, 0.0f));
            backGroundLayer.add(bitmap);
            stepLad += width / fretCount;
        }

        // Draw string
        float shadowStep = ladWidth/2;
        float shadowLadStep = -(ladWidth/2);
        float stringStep = height - heightDiv * 0.8f;
        int alpha = 60;
        float c1 = 0.12f;
        float c2 = 0.18f;

        for (int i = 0; i<6; i++) {
            for (int j = 0; j < fretCount+1; j++) {
                SBitmap shadow1 = new SBitmap(
                        shadowLadStep,
                        stringStep + heightDiv * c1,
                        ladWidth,
                        heightDiv * c2,
                        context,
                        R.drawable.shadow_p2
                );
                shadowLadStep += fretWidth;
                shadow1.setAlpha(alpha);
                shadow1.setLayer(Layer.STRING_SHADOW_LAYER);
                shadow1.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
                backGroundLayer.add(shadow1);

                SBitmap shadow = new SBitmap(
                        shadowStep,
                        stringStep + heightDiv * c2,
                        fretWidth - ladWidth,
                        heightDiv * c1,
                        context,
                        R.drawable.shadow_p1
                );
                shadow.setAlpha(alpha);
                shadow.setLayer(Layer.STRING_SHADOW_LAYER);
                shadow.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
                backGroundLayer.add(shadow);
                shadowStep += fretWidth;
            }
            stringStep -= heightDiv * 2.48f;
            shadowStep = ladWidth/2;
            shadowLadStep = -(ladWidth/2);
            c1+=0.04f;
            c2+=0.04f;
        }

        float stringHeight = heightDiv * 0.12f;
        float inc = this.height - heightDiv;
        SGuitarString string1 = new SGuitarString(0,inc + stringHeight / 2, width,stringHeight, context,R.drawable.string_3);
        string1.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
        guitarString.add(string1);
        string1.setLayer(Layer.STRING_LAYER);

        stringHeight = heightDiv * 0.14f;
        inc -= heightDiv * 2.5f;
        SGuitarString string2 = new SGuitarString(0,inc + stringHeight / 2, width,stringHeight,context,R.drawable.string_3);
        string2.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
        guitarString.add(string2);
        string2.setLayer(Layer.STRING_LAYER);

        stringHeight = heightDiv * 0.16f;
        inc -= heightDiv * 2.5f;
        SGuitarString string3 = new SGuitarString(0,inc + stringHeight / 2, width,stringHeight,context,R.drawable.string_3);
        string3.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
        guitarString.add(string3);
        string3.setLayer(Layer.STRING_LAYER);

        stringHeight = heightDiv * 0.25f;
        inc -= heightDiv * 2.5f;
        SGuitarString string4 = new SGuitarString(0,inc + stringHeight / 2, width,stringHeight,context,R.drawable.string_5);
        string4.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
        guitarString.add(string4);
        string4.setLayer(Layer.STRING_LAYER);

        stringHeight = heightDiv * 0.3f;
        inc -= heightDiv * 2.5f;
        SGuitarString string5 = new SGuitarString(0,inc + stringHeight / 2, width,stringHeight,context,R.drawable.string_5);
        string5.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
        guitarString.add(string5);
        string5.setLayer(Layer.STRING_LAYER);

        stringHeight = heightDiv * 0.35f;
        inc -= heightDiv * 2.5f;
        SGuitarString string6 = new SGuitarString(0,inc + stringHeight / 2, width,stringHeight,context,R.drawable.string_6);
        string6.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
        string6.setLayer(Layer.STRING_LAYER);
        guitarString.add(string6);

        /*******************************************************************************************
         *
         *
         *
         ******************************************************************************************/
        //drawObjects.addAll(testLine);
        drawObjects.addAll(backGroundLayer);
        drawObjects.addAll(guitarString);
        drawObjects.addAll(pentatonicObjectsList);

        showFretsNumber(fretsNumberVisible);
        showNotes(isNotesVisible);
        sortLayer();
        loaded = true;
    }

    /***********************************************************************************************
     * Call this method every time when new objects added to drawable list
     **********************************************************************************************/
    private void sortLayer() {
        for (int i = drawObjects.size() - 1; i >= 0; i--) {
            for (int j = 0; j<i; j++) {
                if(drawObjects.get(j).getLayer() > drawObjects.get(j+1).getLayer()){
                    SShape temp = drawObjects.get(j);
                    drawObjects.set(j,drawObjects.get(j+1));
                    drawObjects.set(j+1,temp);
                }
            }
        }
    }

    public boolean RemoveLayer(int layer) {
        ListIterator<SShape> iterator = this.drawObjects.listIterator();
        while (iterator.hasNext()) {
            SShape object = iterator.next();
            if(object.getLayer() == layer) {
                object.remove = true;
            }
        }
        return false;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        boolean retry = true;
        drawFrame.setRunning(false);
        while (retry) {
            try {
                drawFrame.join();
                retry = false;
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    /**********************************************************************************************/
    /* Draw Frame */
    /**********************************************************************************************/
    class DrawFrame extends Thread {
        private boolean isRunning = false;
        private SurfaceHolder surfaceHolder;
        public DrawFrame(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }
        public void setRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }
        @Override
        public void run() {
            Canvas canvas;

            while (isRunning) {
                if (enableRendering) {
                    // FPS
                    //long now = System.currentTimeMillis();
                    long time_start = System.currentTimeMillis(); //Get time at start of loop for FPS calc
                    canvas = null;
                    try {
                        canvas = surfaceHolder.lockCanvas(null);
                        if (canvas == null)
                            continue;
                        canvas.drawColor(Color.BLACK);

                        synchronized (drawObjects) {
                            ListIterator<SShape> iterator = drawObjects.listIterator();
                            while (iterator.hasNext()) {
                                SShape object = iterator.next();
                                object.draw(canvas);
                                if (object.remove)
                                    iterator.remove();
                            }
                        }
                        // Update Logic
                        /*if (isPentatonicLoaded && currentPentatonic == null) {
                            if(currentPentatonicStep == pentatonicList.size())
                                currentPentatonicStep = 0;
                            int x = pentatonicList.get(currentPentatonicStep).bar + 1;
                            int y = pentatonicList.get(currentPentatonicStep).line;
                            currentPentatonic = pentatonicList.get(currentPentatonicStep);
                            int _y = (int) ((SGuitarString)guitarString.get(y)).getPosition().y;
                            int sh = (int)guitarString.get(y).getHeight();
                            current = new SCircle(width - (fretWidth * x) + (fretWidth / 2),  _y + sh / 2, height / 18,circlePaint);
                            pentatonicObjectsList.add(current);
                            current.setKinematic(true);
                            current.setLayer(PENTATONIC_LAYER);
                            drawObjects.add(current);
                            currentPentatonicStep++;
                        }*/
                    }finally {
                        if (canvas != null) {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }

                    /*System.out.println("FPS:" + fps);
                    fps++;
                    if(now > (mLastTime + 1000)) {
                        mLastTime = now;
                        fps = ifps;
                        fps = 0;
                    }*/

                    //Frame limiting
                    long time_end = System.currentTimeMillis();
                    long frame_time = time_end - time_start;
                    int sleep = (int) ((1000/30) - frame_time);
                    if (sleep > 0){
                        try {
                            this.sleep(sleep);
                        } catch (InterruptedException e) {}
                    }
                    while (sleep < 0) {
                        sleep += 33.33333333333333;
                    }
                }
            }
        }
    }
    /**********************************************************************************************/
    /* Touches */
    /**********************************************************************************************/
    public void onTouchDown(final int x, final int y) {
        if (y >= 6)
            return;

        // Pentatonic touch handler
        if(isPentatonicLoaded /*&& !playWillStart*/) {
            try{
                //int slide = x == 0 ? 0 : Slide;
                if(x == currentPentatonic.bar  && y == currentPentatonic.line) {
                    currentPentatonic = null;
                }
            }catch (NullPointerException e){}
        }

        // Draw touch
        if (touchVisible){
            int _y = (int) ((SGuitarString)guitarString.get(y)).getPosition().y;
            int sh = (int)guitarString.get(y).getHeight();
            SCircle circle;
            if(x == 0) {
                circle =  new SCircle(fretsVisibleArea.left - (int)(fretWidth / 2),  _y + sh / 2, height / 18, touchPaint);
            } else {
                circle = new SCircle(width - (fretWidth * (x - Slide)) + (fretWidth / 2),  _y + sh / 2, height / 18, touchPaint);
            }
            circle.setVisibleArea(new RectF(0.0f, height, width, 0.0f));
            circle.Remove(250);
            drawObjects.add(circle);
        }
        ((SGuitarString)guitarString.get(y)).setAnimate();
    }

    public void onTouchUp(int x, int y) {}
    public void onTouchMove(int x, int y) {}

    /***********************************************************************************************
     * Load selected pentatonic
     **********************************************************************************************/
    public boolean LoadPentatonic(List<Pentatonic>pentatonics) {
        ClosePlayPentatonic();
        while (!pentatonicIsStoped);
        showNotes(new Settings(context).getShowNotes());
        pentatonicIsStoped = false;
        stopPlay = false;
        pentatonicList = pentatonics;
        isPentatonicLoaded = true;
        drawPentatonicMask();
        drawSimplePentatonic();
        startPlayPentatonic();
        return true;
    }
    public void ClosePlayPentatonic() {
        //Log.i("info", "GuitarRenderer : ClosePlayPentatonic");
        stopPlay = true;
        removePentatonicMask();
        //stopPlay = true;
        playWillStart = false;
        pentatonicList = null;
        isPentatonicLoaded = false;
        currentPentatonic = null;
        //pentatonicObjectsList.clear();
        currentPentatonicStep = 0;
        //if(current != null)
        //    current.Remove(0);
    }
    private void drawSimplePentatonic() {
        int x = pentatonicList.get(currentPentatonicStep).bar + 1;
        int y = pentatonicList.get(currentPentatonicStep).line;
        currentPentatonic = pentatonicList.get(currentPentatonicStep);
        current = pentatonicMask.get(currentPentatonicStep);
        current.setColor(Color.RED);
    }
    /***********************************************************************************************
     *  Play pentatonic engine
     **********************************************************************************************/
    private boolean playWillStart = false;
    boolean stopPlay = false;
    boolean pentatonicIsStoped = true;
    private void startPlayPentatonic() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stopPlay) {
                    if (isPentatonicLoaded && currentPentatonic == null) {
                        playWillStart = true;
                        //Log.i("info"," pentatonicList size = " + pentatonicList.size() + " currentPentatonicStep = " + currentPentatonicStep);

                        currentPentatonicStep++;

                        if(currentPentatonicStep == pentatonicList.size()) {
                            currentPentatonicStep = 0;
                        }
                        currentPentatonic = pentatonicList.get(currentPentatonicStep);
                        if(current != null) {
                            current.setColor(Color.YELLOW);
                        }
                        current = currentPentatonic.mask;
                        current.setColor(Color.RED);

                        //currentPentatonicStep++;
                        try {
                            TimeUnit.MILLISECONDS.sleep(currentPentatonic.delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //currentPentatonic = null;
                    }
                }
                pentatonicIsStoped = true;
            }
        }).start();
    }
    /**********************************************************************************************/
    /**/
    /**********************************************************************************************/
    private void drawPentatonicMask() {
        if (pentatonicList != null && !pentatonicList.isEmpty()) {
            List<Pentatonic>pList = new ArrayList<Pentatonic>();
            for (Pentatonic item : pentatonicList) {
                for (Pentatonic pItem : pList) {
                    if(item.position.equals(pItem.position.x, pItem.position.y)) {
                        item = null;
                        break;
                    }
                }
                if (item != null)
                    pList.add(item);
            }
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.BLUE);
            for (Pentatonic p : pList) {
                Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                maskPaint.setColor(Color.YELLOW);
                maskPaint.setAlpha(160);
                int x = p.bar;
                int y = p.line;
                int _y = (int) ((SGuitarString)guitarString.get(y)).getPosition().y;
                int sh = (int)guitarString.get(y).getHeight();

                SCircle circle;
                if(p.bar == 0) {
                    circle = new SCircle(fretsVisibleArea.left - (int)(fretWidth / 2),  _y + sh / 2, height / 18,maskPaint);
                    circle.setVisibleArea(new RectF(0.0f,height,fretsVisibleArea.left,0.0f));
                    circle.setKinematic(false);
                } else {
                    circle = new SCircle(width - (fretWidth * x) + (fretWidth / 2),  _y + sh / 2, height / 18,maskPaint);
                    circle.setVisibleArea(fretsVisibleArea);
                    circle.setKinematic(true);
                }
                String note = QSoundPool.getInstance().GetNoteForGuitarString(x,y);
                circle.drawText(note, textPaint);
                circle.setLayer(Layer.PENTATONIC_LAYER);


                pentatonicMask.add(circle);
                for (Pentatonic pentatonic : pentatonicList) {
                    if (pentatonic.position.equals(p.bar, p.line)) {
                        pentatonic.mask = circle;
                    }
                }
            }
            drawObjects.addAll(pentatonicMask);
            sortLayer();
        }
    }
    private void removePentatonicMask() {
        if (pentatonicList != null && !pentatonicList.isEmpty()) {
            for (SShape shape : pentatonicMask) {
                shape.remove = true;
            }
            pentatonicMask.clear();
        }
    }
    /***********************************************************************************************
     *  Renderer state change
     **********************************************************************************************/
    public void enableRendering(boolean rendering) {
        enableRendering = rendering;
    }
    public void resetLoaded(){
        loaded = false;
        Slide = 0;
    }
    public void resetFretSlider() {
        Slide = 0;
    }
    /***********************************************************************************************
     *  Neck view
     **********************************************************************************************/
    public void setFretNumberVisible(boolean visible){
        fretsNumberVisible = visible;
    }
    public void setShowTouchEnable(boolean visible){
        touchVisible = visible;
    }

    public void slide(int slide) {
        Slide += slide;
        synchronized (drawObjects) {
            ListIterator<SShape> iterator = drawObjects.listIterator();
            while (iterator.hasNext()) {
                SShape object = (SShape)iterator.next();
                if (object.isKinematic()) {
                    object.move((int)(fretWidth * slide), 0);
                }
            }
        }
    }

    private void showFretsNumber(boolean visible) {
        if(visible) {
            float step = fretWidth;
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            circlePaint.setColor(Color.WHITE);
            circlePaint.setAlpha(200);
            for(int i = 0; i<24; i++) {
                SCircle circle;
                if(!fretsMark.get(i)){
                    float radius = 0;
                    if(fretWidth > (height / 6)) {
                        //Log.i("info"," height / 18 " + (height / 18));
                        radius = height / 18;
                    } else {
                        //Log.i("info"," fretWidth / 3 " + (fretWidth / 3));
                        radius = fretWidth / 3;
                    }
                    circle = new SCircle(this.width - step + fretWidth / 2, this.height / 2, radius, circlePaint);
                    circle.setLayer(Layer.BAR_NUMBER_LAYER);
                    circle.setKinematic(true);
                    circle.drawNumber(i+1, textPaint);
                    circle.setVisibleArea(fretsVisibleArea);
                    barNumberObjects.add(circle);
                }
                step += fretWidth;
            }
            drawObjects.addAll(barNumberObjects);
        } else {
            barNumberObjects.clear();
        }
    }

    private void showNotes(boolean visible) {
        if (visible) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            for (int x = 0; x < 25; x++) {
                for (int y = 0; y < 6; y++) {
                    Paint notesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    notesPaint.setColor(getColorForOctave(QSoundPool.getInstance().GetOctaveForGuitarString(x,y)));
                    notesPaint.setAlpha(150);

                    int _y = (int) ((SGuitarString) guitarString.get(y)).getPosition().y;
                    int sh = (int)guitarString.get(y).getHeight();
                    SCircle circle;
                    if(x == 0) {
                        circle = new SCircle(fretsVisibleArea.left - (int)(fretWidth / 2),  _y + sh / 2, height / 22, notesPaint);
                        circle.setVisibleArea(new RectF(0.0f,height,fretsVisibleArea.left,0.0f));
                        circle.setKinematic(false);
                    } else {
                        circle = new SCircle(width - (fretWidth * x) + (fretWidth / 2),  _y + sh / 2, height / 22, notesPaint);
                        circle.setVisibleArea(fretsVisibleArea);
                        circle.setKinematic(true);
                    }
                    circle.setLayer(Layer.NOTES_LAYER);
                    String note = QSoundPool.getInstance().GetNoteForGuitarString(x,y);//y + "," + x;
                    circle.drawText(note, textPaint);
                    drawObjects.add(circle);
                }
            }
        } else {
            RemoveLayer(Layer.NOTES_LAYER);
        }
    }

    private int getColorForOctave(int octave) {
        switch (octave) {
            case 0: return Color.WHITE;
            case 1: return Color.CYAN;
            case 2: return Color.BLUE;
            case 3: return Color.RED;
            case 4: return Color.YELLOW;
        }
        return Color.WHITE;
    }

    public static class Layer {
        private static final int BACKGROUND_LAYER_0 = 1000;
        private static final int BACKGROUND_LAYER_1 = 1001;
        private static final int BACKGROUND_LAYER_2 = 1002;
        private static final int STRING_SHADOW_LAYER =2000;
        private static final int STRING_LAYER = 3000;
        private static final int NOTES_LAYER = 3001;
        private static final int PENTATONIC_LAYER = 4000;
        private static final int BAR_NUMBER_LAYER = 2001;
    }
}