/*
 * Copyright 2017 mzhg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jet.learning.opengl.common;

import android.opengl.GLES20;

import com.nvidia.developer.opengl.utils.NvGLSLProgram;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class BaseShadingProgram extends SimpleLightProgram {
    private int m_alphaClip;
    private int m_reflectionEnabled;
    private int m_materialReflectIdx;
    private int m_normalMapEnabled;
    private int m_shadowMapEnabled;

    public BaseShadingProgram( NvGLSLProgram.LinkerTask task) {
        super(true, task);

        m_alphaClip = GLES20.glGetUniformLocation(getProgram(), "g_AlphaClip");
        m_reflectionEnabled = GLES20.glGetUniformLocation(getProgram(), "g_ReflectionEnabled");
        m_normalMapEnabled = GLES20.glGetUniformLocation(getProgram(), "g_NormalMapEnabled");
        m_shadowMapEnabled = GLES20.glGetUniformLocation(getProgram(), "g_UseShadowMap");

        int reflectTex = GLES20.glGetUniformLocation(getProgram(), "g_ReflectTex");
        m_materialReflectIdx = GLES20.glGetUniformLocation(getProgram(), "g_MaterialReflect");
        if (reflectTex >= 0)
            GLES20.glUniform1i(reflectTex, 1);

        int normalTex = GLES20.glGetUniformLocation(getProgram(), "g_NormalMap");
        assert (normalTex >=0);
        GLES20.glUniform1i(normalTex, 2);

        int shadowMapTex = GLES20.glGetUniformLocation(getProgram(), "g_ShadowMap");
        assert (shadowMapTex >= 0);
        GLES20.glUniform1i(shadowMapTex, 3);
    }

    public void setAlphaClip(boolean flag){
        if(m_alphaClip >=0){
            GLES20.glUniform1i(m_alphaClip, flag?1:0);
        }
    }

    public void setReflection(boolean flag){
        if(m_reflectionEnabled >=0){
            GLES20.glUniform1i(m_reflectionEnabled, flag?1:0);
        }
    }

    public void setNormalMap(boolean flag){
        if(m_normalMapEnabled >=0){
            GLES20.glUniform1i(m_normalMapEnabled, flag?1:0);
        }
    }

    public void setShadowMap(boolean flag){
        if(m_shadowMapEnabled >= 0){
            GLES20.glUniform1i(m_shadowMapEnabled, flag?1:0);
        }
    }

    public void setMaterialReflect(float x,float y, float z){
        if(m_materialReflectIdx >= 0){
            GLES20.glUniform3f(m_materialReflectIdx,x,y,z);
        }
    }


    public void setShadingParams(ShadingParams params) {
        super.setLightParams(params);

        setMaterialReflect(params.materialReflect.x, params.materialReflect.y, params.materialReflect.z);
        setAlphaClip(params.alphaClip);
        setReflection(params.enableReflection);
        setNormalMap(params.enableNormalMap);
        setShadowMap(params.enableShadowMap);
    }

    @Override
    protected String getFragmentShaderFile(boolean uniform) {
        return "d3dcoder/BaseShading.frag";
    }

    @Override
    protected String getVertexShaderFile(boolean uniform) {
        if (uniform)
            return "shaders/SimpleLightUniformColorInstanceVS.vert";
        else
            throw new UnsupportedOperationException();
    }

    public static class ShadingParams extends LightParams{
        public boolean alphaClip = false;
        public boolean enableReflection;
        public boolean enableNormalMap;
        public boolean enableShadowMap;
        public final Vector3f materialReflect = new Vector3f();
    }
}
