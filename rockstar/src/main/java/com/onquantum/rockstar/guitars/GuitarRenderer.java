package com.onquantum.rockstar.guitars;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.onquantum.rockstar.R;
import com.onquantum.rockstar.common.Pentatonic;
import com.onquantum.rockstar.glprimitive.GLDGuitarString;
import com.onquantum.rockstar.glprimitive.GLDTexture;
import com.onquantum.rockstar.glprimitive.GLShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by saiber on 01.03.14.
 */
public class GuitarRenderer implements GLSurfaceView.Renderer{
    private Context context;
    private int fretCount = 0;

    /**********************************************************************************************/
    private GL10 gl10;

    int width, height;
    float ordinate = 1;
    float abscissa = 1;
    float ratio = 0;

    GLDGuitarString strig6;
    GLDGuitarString strig5;
    GLDGuitarString strig4;

    List<GLShape>guitarFretboard = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>Board = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>stringTexture = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>shadowTexture = Collections.synchronizedList(new ArrayList<GLShape>());

    List<GLShape>shadowTextureStringOne   = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>shadowTextureStringTwo   = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>shadowTextureStringThree = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>shadowTextureStringFor   = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>shadowTextureStringFive  = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>shadowTextureStringSix   = Collections.synchronizedList(new ArrayList<GLShape>());

    List<GLShape>drawObjectList;
    List<GLShape>touchesObjectsList    = Collections.synchronizedList(new ArrayList<GLShape>());
    List<GLShape>pentatonicObjectsList = Collections.synchronizedList(new ArrayList<GLShape>());


    private List<Pentatonic>pentatonicList = null;
    private Pentatonic currentPentatonic = null;
    private boolean isPentatonicLoaded = false;
    private int currentPentatonicStep = 0;
    GLDTexture current = null;

    private int touchX = 0;
    private int touchY = 0;

    private long lastTime = 0;
    private long totalElapsed = 0;

    /**********************************************************************************************/
    public GuitarRenderer(Context context, int fretCount) {
        Log.i("info","GuitarRender : GuitarRender");
        this.context = context;
        this.fretCount = fretCount;
    }
    public float getAbscissa() {
        return abscissa;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig eglConfig) {
        gl.glClearColor(0.6f, 0.6f, 0.6f, 1.0f);
        Log.i("info","GuitarRender : onSurfaceCreated");
        gl10 = gl;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i("info","GuitarRender : onSurfaceChanged");
        gl10 = gl;
        this.width = width;
        this.height = height;
        ratio = (float)width / (float)height;
        ordinate = 15;
        float h = this.height / ordinate;
        abscissa = fretCount;

        Log.i("info","abscissa = " + Float.toString(abscissa));
        Log.i("info","ordinate = " + Float.toString(ordinate));

        if(this.height == 0)
            this.height = 1;
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glEnable(GL10.GL_ALPHA_TEST);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glViewport(0, 0, this.width, this.height);
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, abscissa, 0.0f, ordinate, -1.0f, 1.0f);

        /******************************************************************************************/
        /* Create game object */
        /******************************************************************************************/
        // Clear drawable object collection
        if(drawObjectList != null)
            drawObjectList.clear();

        // Create nek of guitar
        Board.clear();
        float fret = 1;
        for(int i = fretCount-1; i>=0; i--) {
            GLDTexture board = new GLDTexture(i,0,1,ordinate);
            if(((fret/2) % 1) > 0) { // Check if decimal value > 0
                board.loadGLTexture(gl, context, BitmapFactory.decodeResource(context.getResources(),R.drawable.b1));
            }else {
                board.loadGLTexture(gl, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.b0));
            }
            fret++;
            board.layer = 0;
            Board.add(board);
        }
        drawObjectList = Collections.synchronizedList(new ArrayList<GLShape>(Board));

        guitarFretboard.clear();
        float w = 0.17f;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        for(int i = 0; i < fretCount+1; i++) {
            GLDTexture fretTexture = new GLDTexture(i - w / 2, 0, w,ordinate);
            fretTexture.loadGLTexture(gl,context,BitmapFactory.decodeResource(context.getResources(), R.drawable.lad,options));
            fretTexture.layer = 1;
            guitarFretboard.add(fretTexture);
        }
        drawObjectList.addAll(guitarFretboard);

        // Init shadow strings texture
        float constant = 2.5f;
        shadowTexture.clear();
        shadowTextureStringOne.clear();
        shadowTextureStringTwo.clear();
        shadowTextureStringThree.clear();
        shadowTextureStringFor.clear();
        shadowTextureStringFive.clear();
        shadowTextureStringSix.clear();

        float shadowStep = 1;
        float shadowHeight = 0.15f;
        float shadowAlpha = 0.23f;
        for (int i = 0; i < 6; i++) {
            for(int j = 0; j<fretCount+1; j++) {
                GLDTexture shadow_1 = new GLDTexture(j+w/2,shadowStep-0.4f,1.0f - w,shadowHeight);
                shadow_1.loadGLTexture(gl,context,BitmapFactory.decodeResource(context.getResources(), R.drawable.shadow_p1));
                shadow_1.layer = 2;
                shadow_1.setColor(1.0f, 1.0f, 1.0f,shadowAlpha);

                GLDTexture shadow_2= new GLDTexture(j-w/2,shadowStep-0.4f,w,shadowHeight);
                shadow_2.loadGLTexture(gl,context,BitmapFactory.decodeResource(context.getResources(), R.drawable.shadow_p2));
                shadow_2.layer = 2;
                shadow_2.setColor(1.0f, 1.0f, 1.0f,shadowAlpha);
                switch (i) {
                    case 0:{
                        shadowTextureStringOne.add(shadow_1);
                        shadowTextureStringOne.add(shadow_2);
                        break;
                    }
                    case 1:{
                        shadowTextureStringTwo.add(shadow_1);
                        shadowTextureStringTwo.add(shadow_2);
                        break;
                    }
                    case 2:{
                        shadowTextureStringThree.add(shadow_1);
                        shadowTextureStringThree.add(shadow_2);
                        break;
                    }
                    case 3:{
                        shadowTextureStringFor.add(shadow_1);
                        shadowTextureStringFor.add(shadow_2);
                        break;
                    }
                    case 4:{
                        shadowTextureStringFive.add(shadow_1);
                        shadowTextureStringFive.add(shadow_2);
                        break;
                    }
                    case 5:{
                        shadowTextureStringSix.add(shadow_1);
                        shadowTextureStringSix.add(shadow_2);
                        break;
                    }
                    default: break;
                }
            }
            shadowHeight += 0.05f;
            shadowStep += constant;
        }
        drawObjectList.addAll(shadowTexture);
        drawObjectList.addAll(shadowTextureStringOne);
        drawObjectList.addAll(shadowTextureStringTwo);
        drawObjectList.addAll(shadowTextureStringThree);
        drawObjectList.addAll(shadowTextureStringFor);
        drawObjectList.addAll(shadowTextureStringFive);
        drawObjectList.addAll(shadowTextureStringSix);

        //Guitar string
        //String 1,3
        stringTexture.clear();
        float inc = 1;
        GLDGuitarString string_1 = new GLDGuitarString(0,inc, abscissa, 0.12f);
        string_1.loadGLTexture(gl, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.string_3));
        string_1.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        string_1.layer = 3;
        stringTexture.add(string_1);

        inc+=constant;
        GLDGuitarString string_2 = new GLDGuitarString(0,inc, abscissa, 0.12f);
        string_2.loadGLTexture(gl, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.string_3));
        string_2.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        string_2.layer = 3;
        stringTexture.add(string_2);
        inc+=constant;
        GLDGuitarString string_3 = new GLDGuitarString(0,inc, abscissa, 0.14f);
        string_3.loadGLTexture(gl, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.string_3));
        string_3.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        string_3.layer = 3;
        stringTexture.add(string_3);
        inc+=constant;

        //String 4,6
        strig4 = new GLDGuitarString(0,inc, abscissa, 0.25f);
        strig4.loadGLTexture(gl,context, BitmapFactory.decodeResource(context.getResources(), R.drawable.string_5));
        strig4.layer = 3;
        stringTexture.add(strig4);
        inc+=constant;
        strig5 = new GLDGuitarString(0,inc, abscissa, 0.3f);
        strig5.loadGLTexture(gl,context, BitmapFactory.decodeResource(context.getResources(), R.drawable.string_5));
        strig5.layer = 3;
        stringTexture.add(strig5);
        inc+=constant;
        strig6 = new GLDGuitarString(0,inc, abscissa, 0.35f);
        strig6.loadGLTexture(gl,context, BitmapFactory.decodeResource(context.getResources(), R.drawable.string_6));
        strig6.layer = 3;
        stringTexture.add(strig6);
        drawObjectList.addAll(stringTexture);
    }
    /**********************************************************************************************/
    /* Draw frame */
    /**********************************************************************************************/
    @Override
    public void onDrawFrame(GL10 gl) {
        // Draw
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        int count = 0;
        synchronized (drawObjectList) {
            for(GLShape object : drawObjectList) {
                object.draw(gl);
                if(object.remove == true)
                    drawObjectList.remove(count);
                else
                    count++;
            }
            Iterator<GLShape> iterator = touchesObjectsList.iterator();
            while (iterator.hasNext()) {
                GLShape object = iterator.next();
                object.draw(gl);
                if (object.remove)
                    iterator.remove();
            }
            Iterator<GLShape> iteratorPentatonic = pentatonicObjectsList.iterator();
            while (iteratorPentatonic.hasNext()) {
                GLShape object = iteratorPentatonic.next();
                object.draw(gl);
                if (object.remove)
                    iteratorPentatonic.remove();
            }

            // Update Logic
            if (isPentatonicLoaded && currentPentatonic == null) {
                if(currentPentatonicStep == pentatonicList.size())
                    currentPentatonicStep = 0;
                int x = pentatonicList.get(currentPentatonicStep).bar;
                int y = pentatonicList.get(currentPentatonicStep).line;
                currentPentatonic = pentatonicList.get(currentPentatonicStep);
                float _y = y == 0 ? 1 : 2.5f * y + 1;
                current = new GLDTexture(abscissa - 1 - x + 0.5f, _y, 0.5f, ordinate/abscissa,false);
                current.loadGLTexture(gl10, context, BitmapFactory.decodeResource(context.getResources(), R.drawable.circle));
                current.setAlpha(0.5f);
                pentatonicObjectsList.add(current);
                currentPentatonicStep++;
            }
        }
    }
    /**********************************************************************************************/
    /* Touch event handler */
    /**********************************************************************************************/
    // Touch Down
    public void onTouchDown(final int x, final int y) {
        touchY = y;
        touchX = x;
        //Log.i("info"," Touch Began: X = " + Integer.toString(x) + "  Y = " + Integer.toString(y));
        //Log.i("info"," Convert X = " + Integer.toString((int)abscissa - x));
        if (y >= 6)
            return;
        if(isPentatonicLoaded) {
            if(x == currentPentatonic.bar && y == currentPentatonic.line) {
                currentPentatonic = null;
                current.Remove(0);
            }
        }
        //Animate guitar string
        GLDGuitarString guitarString = (GLDGuitarString)stringTexture.get(y);
        guitarString.setAnimate();
    }

    // Touch Up
    public void onTouchUp(int x, int y) {
        if (y >= 6)
            return;
    }
    // Touch Move
    public void onTouchMove(int x, int y) {
    }

    /**********************************************************************************************/
    /* Load selected pentatonic */
    /**********************************************************************************************/
    public boolean LoadPentatonic(List<Pentatonic>pentatonics) {
        pentatonicList = pentatonics;
        isPentatonicLoaded = true;
        currentPentatonic = null;
        pentatonicObjectsList.clear();
        currentPentatonicStep = 0;
        return true;
    }
    public void ClosePlayPentatonic() {
        pentatonicList = null;
        isPentatonicLoaded = false;
        currentPentatonic = null;
        pentatonicObjectsList.clear();
        currentPentatonicStep = 0;
    }

}
