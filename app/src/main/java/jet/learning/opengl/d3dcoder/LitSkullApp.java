////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 mzhg
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
// License for the specific language governing permissions and limitations
// under the License.
////////////////////////////////////////////////////////////////////////////////
package jet.learning.opengl.d3dcoder;


import android.opengl.GLES20;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.AttribBinder;
import com.nvidia.developer.opengl.utils.AttribBindingTask;
import com.nvidia.developer.opengl.utils.GLES;
import com.nvidia.developer.opengl.utils.NvPackedColor;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import javax.microedition.khronos.opengles.GL11;

import jet.learning.opengl.common.RenderMesh;
import jet.learning.opengl.common.ShapeMesh;
import jet.learning.opengl.common.SimpleLightProgram;
import jet.learning.opengl.common.SkullMesh;
import jet.learning.opengl.common.SkyRenderer;

/**
 * 2017-12-12 Update: Add the instance rendering and reflection.
 *
 * Created by mazhen'gui on 2017/11/22.
 */

public final class LitSkullApp extends NvSampleApp {
    SimpleLightProgram mLightProgram;

    final SimpleLightProgram.LightParams mLightParams = new SimpleLightProgram.LightParams();
    final Matrix4f mSkullWorld = new Matrix4f();
    final Matrix4f mProj = new Matrix4f();
    final Matrix4f mView = new Matrix4f();
    final Matrix4f mProjView = new Matrix4f();

    ShapeMesh mShapeVBO;
    SkullMesh mSkullVBO;
    SkyRenderer mSky;

    @Override
    protected void initRendering() {
        mLightParams.lightAmbient.set(0.2f, 0.2f, 0.2f);
        mLightParams.lightDiffuse.set(0.8f, 0.8f, 0.8f);
        mLightParams.lightSpecular .set(1.f, 1.f, 1.0f);
        mLightParams.lightPos .set(0.57735f, -0.57735f, 0.57735f, 0);
        mLightParams.lightPos.scale(-1);
        mLightParams.color.set(0.6f,0.6f,0.6f,1.0f);

        buildFX();
        buildGeometryBuffers();

        m_transformer.setTranslation(0,-5,-15);
        m_transformer.setRotationVec(new Vector3f(0, PI, 0));

        mSky = new SkyRenderer("textures/snowcube1024.dds", 100.0f);

        GLES.checkGLError();
    }

    @Override
    protected void draw() {
        ReadableVector3f color = NvPackedColor.LIGHT_STEEL_BLUE;
        GLES20.glClearColor(color.getX(), color.getY(), color.getZ(), 1);
        GLES20.glClear(GL11.GL_COLOR_BUFFER_BIT| GL11.GL_DEPTH_BUFFER_BIT);


        // Draw the sky box first
        {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthMask(false);

            Matrix4f rotate = m_transformer.getRotationMat();
            Matrix4f.mul(mProj, rotate, mProjView);
            mSky.draw(mProjView);

            GLES20.glEnable(GL11.GL_DEPTH_TEST);
            GLES20.glDepthMask(true);
        }

        m_transformer.getModelViewMat(mView);

//        Matrix4f.lookAt(2.5f,0,0, 0,0,0, 0,1,0, mView);
        Matrix4f.mul(mProj, mView, mProjView);
        mView.invert();
        Matrix4f.transformVector(mView, Vector3f.ZERO, mLightParams.eyePos);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        drawScene();
    }

    private void drawScene(){
        mLightProgram.enable();
        mShapeVBO.bind();
        {
            // Draw the grid
            buildFrame(mShapeVBO.getGridWorld());
            setupGridMat();
            mLightProgram.setLightParams(mLightParams);
            mShapeVBO.drawGrid();
            GLES.checkGLError();
        }

        {//		 Draw the box
            buildFrame(mShapeVBO.getBoxWorld());
            setupBoxMat();
            mLightProgram.setLightParams(mLightParams);
            mShapeVBO.drawBox();
            GLES.checkGLError();
        }

        { //  Draw the cylinders.

            setupCylinderMat();
            for(int i = 0; i < 10; i++){
                buildFrame(mShapeVBO.getCylinderWorld(i));
                mLightProgram.setLightParams(mLightParams);
                mShapeVBO.drawCylinders();
            }
            GLES.checkGLError();
        }

        { // Draw the spheres.
            setupSphereMat();
            for(int i = 0; i < 10; i++){
                buildFrame(mShapeVBO.getSphereWorld(i));
                mLightProgram.setLightParams(mLightParams);
                mShapeVBO.drawSphere();
            }
            GLES.checkGLError();
        }

        mShapeVBO.unbind();

        { // Draw the skull
            buildFrame(mSkullWorld);
            setupSkullMat();
            mLightProgram.setLightParams(mLightParams);
            mSkullVBO.draw();
            GLES.checkGLError();
        }
    }


    @Override
    protected void reshape(int width, int height) {
        Matrix4f.perspective((float)Math.toDegrees(0.25 * PI), (float)width/height, 1.0f, 1000.0f, mProj);
        GLES20.glViewport(0,0, width, height);
    }

    private void buildFrame(Matrix4f world){
        mLightParams.model.load(world);
        Matrix4f.mul(mProjView, world, mLightParams.modelViewProj);
    }

    private void setupGridMat(){
        mLightParams.materialAmbient  .set(0.48f, 0.77f, 0.46f);
        mLightParams.materialDiffuse  .set(0.48f, 0.77f, 0.46f);
        mLightParams.materialSpecular .set(0.2f, 0.2f, 0.2f, 16.0f);
    }

    private void setupCylinderMat(){
        mLightParams.materialAmbient  .set(0.7f, 0.85f, 0.7f);
        mLightParams.materialDiffuse  .set(0.7f, 0.85f, 0.7f);
        mLightParams.materialSpecular .set(0.8f, 0.8f, 0.8f, 16.0f);
    }

    private void setupSphereMat(){
        mLightParams.materialAmbient  .set(0.1f, 0.2f, 0.3f);
        mLightParams.materialDiffuse  .set(0.2f, 0.4f, 0.6f);
        mLightParams.materialSpecular .set(0.9f, 0.9f, 0.9f, 16.0f);
    }

    private void setupBoxMat(){
        mLightParams.materialAmbient  .set(0.651f, 0.5f, 0.392f);
        mLightParams.materialDiffuse  .set(0.651f, 0.5f, 0.392f);
        mLightParams.materialSpecular .set(0.2f, 0.2f, 0.2f, 16.0f);
    }

    private void setupSkullMat(){
        mLightParams.materialAmbient  .set(0.8f, 0.8f, 0.8f);
        mLightParams.materialDiffuse  .set(0.8f, 0.8f, 0.8f);
        mLightParams.materialSpecular .set(0.8f, 0.8f, 0.8f, 16.0f);
    }

    void buildFX(){
        mLightProgram = new SimpleLightProgram(true, new AttribBindingTask(
                new AttribBinder(SimpleLightProgram.POSITION_ATTRIB_NAME, 0),
                new AttribBinder(SimpleLightProgram.TEXTURE_ATTRIB_NAME, 1),
                new AttribBinder(SimpleLightProgram.NORMAL_ATTRIB_NAME, 2)));
    }

    void buildGeometryBuffers(){
        RenderMesh.MeshParams params = new RenderMesh.MeshParams();
        params.posAttribLoc = mLightProgram.getAttribPosition();
        params.norAttribLoc = mLightProgram.getNormalAttribLoc();
        params.texAttribLoc = mLightProgram.getAttribTexCoord();

        mShapeVBO = new ShapeMesh();
        mShapeVBO.initlize(params);

        mSkullVBO = new SkullMesh();
        mSkullVBO.initlize(params);

        mSkullWorld.setIdentity();
        mSkullWorld.translate(0.0f, 1.0f, 0.0f);
        mSkullWorld.scale(0.5f, 0.5f, 0.5f);
    }
}
