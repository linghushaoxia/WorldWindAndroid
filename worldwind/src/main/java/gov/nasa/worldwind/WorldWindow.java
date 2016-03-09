/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import gov.nasa.worldwind.globe.GlobeWgs84;
import gov.nasa.worldwind.globe.Globe;
import gov.nasa.worldwind.layer.LayerList;
import gov.nasa.worldwind.render.BasicFrameController;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.FrameController;
import gov.nasa.worldwind.render.FrameStatistics;
import gov.nasa.worldwind.util.Logger;

public class WorldWindow extends GLSurfaceView implements GLSurfaceView.Renderer {

    protected Globe globe = new GlobeWgs84();

    protected LayerList layers = new LayerList();

    protected double verticalExaggeration = 1;

    protected Navigator navigator = new BasicNavigator();

    protected NavigatorController navigatorController = new BasicNavigatorController();

    protected FrameController frameController = new BasicFrameController();

    protected Rect viewport = new Rect();

    protected DrawContext dc;

    /**
     * Constructs a WorldWindow associated with the specified application context. This is the constructor to use when
     * creating a WorldWindow from code.
     */
    public WorldWindow(Context context) {
        super(context);
        this.init();
    }

    /**
     * Constructs a WorldWindow associated with the specified application context and attributes from an XML tag. This
     * constructor is included to provide support for creating WorldWindow from an Android XML layout file, and is not
     * intended to be used directly.
     * <p>
     * This is called when a view is being constructed from an XML file, supplying attributes that were specified in the
     * XML file. This version uses a default style of 0, so the only attribute values applied are those in the Context's
     * Theme and the given AttributeSet.
     */
    public WorldWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    /**
     * Prepares this WorldWindow for drawing and event handling.
     */
    protected void init() {

        // Initialize the drawing context with the Android context.
        this.navigatorController.setWorldWindow(this);
        this.dc = new DrawContext(this.getContext());

        // Set up to render on demand to an OpenGL ES 2.x context
        this.setEGLContextClientVersion(2); // must be called before setRenderer
        this.setRenderer(this);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // must be called after setRenderer
    }

    public Globe getGlobe() {
        return globe;
    }

    public void setGlobe(Globe globe) {
        if (globe == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "WorldWindow", "setGlobe", "missingGlobe"));
        }

        this.globe = globe;
    }

    public LayerList getLayers() {
        return layers;
    }

    public void setLayers(LayerList layers) {
        if (layers == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "WorldWindow", "setLayers", "missingList"));
        }

        this.layers = layers;
    }

    public double getVerticalExaggeration() {
        return verticalExaggeration;
    }

    public void setVerticalExaggeration(double verticalExaggeration) {
        if (verticalExaggeration <= 0) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "WorldWindow", "setVerticalExaggeration", "invalidVerticalExaggeration"));
        }

        this.verticalExaggeration = verticalExaggeration;
    }

    public Navigator getNavigator() {
        return navigator;
    }

    public void setNavigator(Navigator navigator) {
        if (navigator == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "WorldWindow", "setNavigator", "missingNavigator"));
        }

        this.navigator = navigator;
    }

    public NavigatorController getNavigatorController() {
        return navigatorController;
    }

    public void setNavigatorController(NavigatorController navigatorController) {
        if (navigatorController == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "WorldWindow", "setNavigatorController", "missingController"));
        }

        this.navigatorController.setWorldWindow(null); // detach the old controller
        this.navigatorController = navigatorController; // switch to the new controller
        this.navigatorController.setWorldWindow(this); // attach the new controller
    }

    public FrameController getFrameController() {
        return frameController;
    }

    public void setFrameController(FrameController frameController) {
        if (frameController == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "WorldWindow", "setFrameController", "missingController"));
        }

        this.frameController = frameController;
    }

    public FrameStatistics getFrameStatistics() {
        return this.frameController.getFrameStatistics();
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.viewport.set(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Setup the draw context and render the WorldWindow's current state.
        this.prepareToDrawFrame();
        this.navigator.applyState(dc);
        this.frameController.drawFrame(this.dc);

        // Propagate render requests submitted during rendering to the WorldWindow. The draw context provides a layer of
        // indirection that insulates rendering code from establishing a dependency on a specific WorldWindow.
        if (this.dc.isRenderRequested()) {
            this.requestRender();
        }
    }

    protected void prepareToDrawFrame() {
        this.dc.reset();
        this.dc.setGlobe(this.globe);
        this.dc.setLayers(this.layers);
        this.dc.setVerticalExaggeration(this.verticalExaggeration);
        this.dc.setViewport(this.viewport);
    }
}