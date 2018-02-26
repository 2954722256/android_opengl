////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014, NVIDIA CORPORATION. All rights reserved.
// Copyright 2017 mzhg
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
package com.nvidia.developer.opengl.ui;

import android.opengl.GLES20;
import android.util.Log;

import com.nvidia.developer.opengl.utils.GLES;
import com.nvidia.developer.opengl.utils.GLUtil;
import com.nvidia.developer.opengl.utils.NvPackedColor;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL11;

/**
 * A graphical element, with no interactivity.<p>
 * This class renders a texture (or portion of one) to the screen, but has 
 * no interactivity with the user, just the visual.  Other UI classes derive
 * from this functionality, or otherwise utilize or proxy to it.<p>
 * <code>NvUIGraphic<code> can be initialized from a texture on disk, an 
 * existing {@link NvUITexture} object, or an existing GL texture ID.
 * @author Nvidia 2014-9-8 : 11: 53
 *
 */
public class NvUIGraphic extends NvUIElement{

	/** A static VBO set up for rendering a textured 2D rectangle. */
	private static int ms_vbo;
	/** A static VBO set up for rendering a textured 2D rectangle, with flipped Y coordinates to flip the texture vertically. */
	private static int ms_vboFlip;
	/** A static IBO set up for rendering a textured 2D rectangle. */
	private static int ms_ibo;
	
	private static float s_pixelScaleFactorX = 0.5f;
	private static float s_pixelScaleFactorY = 0.5f;
	private static int   s_pixelXLast = 1;
	private static int   s_pixelYLast = 1;
	private static float s_graphicWidth, s_graphicHeight;
	private static float[][] s_pixelToClipMatrix = new float[4][4];
	private static boolean   s_initCount;
	
	private static NvGraphicShader ms_shader;
	
	private static final String s_graphicVertShader = 
			"#version 100\n"+
			"// this is set from higher level.  think of it as the upper model matrix\n"+
			"uniform mat4 pixelToClipMat;\n"+
			"attribute vec2 position;\n"+
			"attribute vec2 tex;\n"+
			"varying vec2 tex_coord;\n"+
			"void main()\n"+
			"{\n"+
			"    gl_Position = pixelToClipMat * vec4(position, 0, 1);\n"+
			"    tex_coord = tex;\n"+
			"}\n";
	
	private static final String s_graphicFragShader = 
			"#version 100\n"+
			"precision mediump float;\n"+
			"varying vec2 tex_coord;\n"+
			"uniform sampler2D sampler;\n"+
			"uniform float alpha;\n"+
			"uniform vec4 color;\n"+
			"void main()\n"+
			"{\n"+
			"    gl_FragColor = texture2D(sampler, tex_coord) * vec4(color.r,color.g,color.b,alpha);\n"+
			"}\n";
	
	/** The texture we render from. */
	protected NvUITexture m_tex;
	/** Are we scaling the texture (pixel size other than original width/height texel size)? */
	protected boolean m_scale;
	/** Do we need to vertical flip the texture/graphic? */
	protected boolean m_vFlip;
	/** An RGB color to modulate with the texels of the graphic (alpha is ignored for the moment) */
	protected int     m_color;
	
	/**
	 * Basic constructor, taking texture filename as source.<p>
     * Note it uses the texture size for target w/h unless default params overridden,
     * in which case it will scale the texture appropriately.
	 * @param texname Texture filename (and path if any needed) in a String.
	 * @param dstw Target width dimension, defaults to texture width if not overridden.
	 * @param dsth Target height dimension, defaults to texture height if not overridden.
	 */
	public NvUIGraphic(String texname, int dstw, int dsth){
		staticInit();
		privateInit();
		loadTexture(texname,true);
		if(dstw != 0)
			setDimensions(dstw, dsth);
	}
	
	/**
	 * Basic constructor, taking GL texture ID as source.<p>
	 * Note it uses the texture size for target w/h unless default params overridden,
     * in which case it will scale the texture appropriately.
	 * @param texId GL texture object ID.
	 * @param alpha Whether the source texture format contains an alpha channel.
	 * @param srcw Source texture width in texels.
	 * @param srch Source texture height in texels.
	 * @param dstw Target width dimension, defaults to texture width if not overridden.
	 * @param dsth Target height dimension, defaults to texture height if not overridden.
	 */
	public NvUIGraphic(int texId, boolean alpha, // dynamic/fbo/ext texture.
            int srcw, int srch, float dstw, float dsth){
		staticInit();
		privateInit();
		setTextureID(texId, alpha, srcw, srch);
		if(dstw != 0)
			setDimensions(dstw, dsth);
	}
	
	/**
	 * Basic constructor, taking GL texture ID as source.<p>
	 * Note it uses the texture size for target w/h unless default params overridden,
     * in which case it will scale the texture appropriately.
	 * @param tex Source NvUITexture object pointer.
	 * @param dstw Target width dimension, defaults to texture width if not overridden.
	 * @param dsth Target height dimension, defaults to texture height if not overridden.
	 */
	public NvUIGraphic(NvUITexture tex, float dstw, float dsth){
		staticInit();
	    privateInit();
	    m_tex = tex;
	    m_tex.addRef();
	    
	    if(dstw != 0)
	    	setDimensions(dstw, dsth);
	    else
	    	setDimensions(m_tex.getWidth(), m_tex.getHeight());
	}
	
	@Override
	public void dispose() {
		flushTexture();
		staticCleanup();
	}
	
	/**
	 * Loads a texture from cache or file.<p>
	 * This actually ends up loading data using the @ref NvUITexture::CacheTexture method,
     * to take advantage of the texture cache, and loaded from disk into the cache if not
     * already there.
	 * @param texname Filename of texture to load
	 * @param resetDimensions Whether to set our dimensions to match those of the texture.  Defaults true.
	 * @return
	 */
	public boolean loadTexture(String texname, boolean resetDimensions){
		flushTexture(); // in case we're being use to re-load new texture.

	    GLES.glActiveTexture(GL11.GL_TEXTURE0);
	    m_tex = NvUITexture.cacheTexture(texname, true);

	    if (m_tex != null && resetDimensions)
	    {
	        // set screenrect and dest dimensions to match the raw texel size of the texture.
	        setDimensions((float)m_tex.getWidth(), (float)m_tex.getHeight());
	    }

	    return(m_tex != null && m_tex.getGLTex()!=0);
	}
	
	/** Change this graphic to use a specific NvUITexture object. */
	public void setTexture(NvUITexture tex){
		flushTexture();
	    m_tex = tex;
	    m_tex.addRef();
	}
	
	/** Helper method to set the GL texture filtering on our texture object. */
	public void setTextureFiltering(int minFilter, int magFilter){
		if (m_tex!=null && m_tex.getGLTex() != 0
		        && (minFilter != 0|| magFilter != 0))
		    {
		        GLES.glActiveTexture(GL11.GL_TEXTURE0);
		        GLES.glBindTexture(GL11.GL_TEXTURE_2D, m_tex.getGLTex());

		        if (minFilter != 0)
		        	GLES.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
		        if (magFilter != 0)
		        	GLES.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter);

		        GLES.glBindTexture(GL11.GL_TEXTURE_2D, 0); // unbind at end!
		    }
	}
	
	private void privateInit(){
		m_tex = null;
	    m_scale = false;
	    m_vFlip = false;
	    m_color = NvPackedColor.NV_PC_PREDEF_WHITE;
	}
	
	private static boolean staticInit(){
		if (!s_initCount)
	    {
			Log.i("OpenGL ES", "NvUIGraphics staticInit");
	        NvTexturedVertex[]    vert  = new NvTexturedVertex[4];
	        for(int i = 0; i < 4;i++)
	        	vert[i] = new NvTexturedVertex();
	        
	        short[]           indices   = new short[6];

	        if(ms_shader == null){
	        	ms_shader = new NvGraphicShader();
	           ms_shader.load(s_graphicVertShader, s_graphicFragShader);
	        }

	        short pos = 0;
	        int ipos = 0;

	        indices[ipos+0] = pos;
	        indices[ipos+1] = (short) (pos+1);
	        indices[ipos+2] = (short) (pos+3);
	        indices[ipos+3] = (short) (pos+3);
	        indices[ipos+4] = (short) (pos+1);
	        indices[ipos+5] = (short) (pos+2);

	        vert[pos].posX = 0; 
	        vert[pos].posY = 1;
	        vert[pos].uvX  = 0; 
	        vert[pos].uvY  = 1;
	        pos++;

	        vert[pos].posX = 0; 
	        vert[pos].posY = 0;
	        vert[pos].uvX = 0; 
	        vert[pos].uvY = 0;
	        pos++;

	        vert[pos].posX = 1; 
	        vert[pos].posY = 0;
	        vert[pos].uvX = 1; 
	        vert[pos].uvY = 0;
	        pos++;

	        vert[pos].posX = 1; 
	        vert[pos].posY = 1;
	        vert[pos].uvX = 1; 
	        vert[pos].uvY = 1;
	        pos++;

	        ms_ibo = GLES.glGenBuffers();
	        ms_vbo = GLES.glGenBuffers();
	        
	        FloatBuffer buffer = GLUtil.getCachedFloatBuffer(4 * 4);
	        for(int i = 0; i < 4;i++)
	        	vert[i].store(buffer);
	        buffer.flip();

	        GLES.glBindBuffer(GL11.GL_ARRAY_BUFFER, ms_vbo);
	        GLES.glBufferData(GL11.GL_ARRAY_BUFFER, buffer, GL11.GL_STATIC_DRAW);

	        GLES.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, ms_ibo);
	        GLES.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, GLUtil.wrap(indices), GL11.GL_STATIC_DRAW);

	        // make a texture-Y-flipped vbo...
	        ms_vboFlip = GLES.glGenBuffers();
	        pos = 0;
	        
	        vert[pos].posX = 0; 
	        vert[pos].posY = 1;
	        vert[pos].uvX = 0; 
	        vert[pos].uvY = 0;
	        pos++;

	        vert[pos].posX = 0; 
	        vert[pos].posY = 0;
	        vert[pos].uvX = 0; 
	        vert[pos].uvY = 1;
	        pos++;

	        vert[pos].posX = 1; 
	        vert[pos].posY = 0;
	        vert[pos].uvX = 1; 
	        vert[pos].uvY = 1;
	        pos++;

	        vert[pos].posX = 1; 
	        vert[pos].posY = 1;
	        vert[pos].uvX = 1; 
	        vert[pos].uvY = 0;
	        pos++;
	        
	        for(int i = 0; i < 4;i++)
	        	vert[i].store(buffer);
	        buffer.flip();

	        GLES.glBindBuffer(GL11.GL_ARRAY_BUFFER, ms_vboFlip);
	        GLES.glBufferData(GL11.GL_ARRAY_BUFFER, buffer, GL11.GL_STATIC_DRAW);

	        GLES.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	        GLES.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);

	        // The following entries are const
	        // so we set them up now and never change
	        s_pixelToClipMatrix[2][0] = 0.0f;
	        s_pixelToClipMatrix[2][1] = 0.0f;

	        s_pixelToClipMatrix[0][2] = 0.0f;
	        s_pixelToClipMatrix[1][2] = 0.0f;
	        s_pixelToClipMatrix[2][2] = 1.0f;
	        s_pixelToClipMatrix[3][2] = 0.0f;

	        s_pixelToClipMatrix[0][3] = 0.0f;
	        s_pixelToClipMatrix[1][3] = 0.0f;
	        s_pixelToClipMatrix[2][3] = 0.0f;
	        s_pixelToClipMatrix[3][3] = 1.0f;
	        
	        s_initCount = true;
	    }

	    return true;
	}
	
	static void staticCleanup(){
		if (s_initCount)
	    {
			GLES.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
			GLES.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);
	        GLES20.glUseProgram(0);

//	        ms_shader.m_program.dispose();  TODO can't do this on Android 
	        ms_shader.m_program = null;
	        ms_shader = null;

//	        GLES.glDeleteBuffers(ms_vbo);  TODO can't do this on Android 
//	        GLES.glDeleteBuffers(ms_vboFlip);
//	        GLES.glDeleteBuffers(ms_ibo);
	        ms_vbo = 0;
	        ms_vboFlip = 0;
	        ms_ibo = 0;
	        
	        s_initCount = false;
	    }
	}
	
	/** Change this graphic to use an existing GL texture ID and details. */
	public void setTextureID(int texID, boolean alpha, int srcw, int srch){
		if (m_tex != null)
	    {
	        // optimization. !!!!TBD TODO? WORKING PROPERLY??
	        if (texID == m_tex.getGLTex()) // we've got this already.  punt if match
	        {
	            if (srcw == m_tex.getWidth()
	            &&  srch == m_tex.getHeight())
	                return;
	        }

	        flushTexture(); // anything bound...
	    }
	    
	    m_tex = new NvUITexture(texID, alpha, srcw, srch, false, false);
	   
	    // and push dest dimensions into screenRect and texDestZZZ
	    setDimensions((float)srcw, (float)srch);
	}
	
	/** Does the heavy lifting to render our texture at target position/dimensions. */
	@Override
	public void draw(NvUIDrawState drawState) {
		if (!m_isVisible) return;
	    if (m_tex == null) return;

	    // calculate internal alpha value...
	    float myAlpha = m_alpha;
	    if (drawState.alpha != 1.0f)
	        myAlpha *= drawState.alpha;

	    // pick correct shader based on alpha...
	    ms_shader.m_program.enable();

	    // then if alpha shader, set alpha uniform...
	    if (ms_shader.m_alphaIndex >= 0)
	    	GLES20.glUniform1f(ms_shader.m_alphaIndex, myAlpha);
	    
	    // then if colorizing shader, set color uniform...
	    if (ms_shader.m_colorIndex >= 0)
	    {   // optimize it a bit...  // !!!!TBD alpha in color not just sep value?
	        if ((m_color & 0xFFFFFF) == 0xFFFFFF)
	        	GLES20.glUniform4f(ms_shader.m_colorIndex, 1,1,1,1);
	        else
	        	GLES20.glUniform4f(ms_shader.m_colorIndex,
	                            NvPackedColor.getRedFromRGBf(m_color),
	                            NvPackedColor.getGreenf(m_color),
	                            NvPackedColor.getBlueFromRGBf(m_color),
	                            1); // !!!!TBD
	    }
	    
	    // update the transform matrix.
	    int designWidth, designHeight;
	    if (drawState.designWidth != 0)
	    {
	        designWidth = drawState.designWidth;
	        designHeight = drawState.designHeight;
	    }
	    else
	    {
	        designWidth = drawState.width;
	        designHeight = drawState.height;
	    }
	    
	    // update the scale factors ONLY IF cached design size changed.
	    boolean scaleFactorChanged = false;
	    if (s_pixelXLast != designWidth)
	    {
	        s_pixelXLast = designWidth;
	        s_pixelScaleFactorX = 2.0f / s_pixelXLast;
	        scaleFactorChanged = true;
	    }
	    if (s_pixelYLast != designHeight)
	    {
	        s_pixelYLast = designHeight;
	        s_pixelScaleFactorY = 2.0f / s_pixelYLast;
	        scaleFactorChanged = true;
	    }

	    float rad = (float)(drawState.rotation / 180.0f * 3.14159f); // [-1,2]=>[-90,180] in radians...
	    float cosf = (float) Math.cos(rad);
	    float sinf = (float) Math.sin(rad);

	    final float wNorm = s_pixelScaleFactorX;
	    final float hNorm = s_pixelScaleFactorY;

	    if (s_graphicWidth != m_rect.width)
	    {
	        s_graphicWidth = m_rect.width;
	        scaleFactorChanged = true;
	    }
	    if (s_graphicHeight != m_rect.height)
	    {
	        s_graphicHeight = m_rect.height;
	        scaleFactorChanged = true;
	    }

	    //if (1) //(scaleFactorChanged) // scale factor OR w/h is different this call
	    {
	        s_pixelToClipMatrix[0][0] = wNorm * m_rect.width  * cosf;
	        s_pixelToClipMatrix[1][0] = hNorm * m_rect.height * -sinf;
	        s_pixelToClipMatrix[0][1] = wNorm * m_rect.width  * sinf;
	        s_pixelToClipMatrix[1][1] = hNorm * m_rect.height * cosf;
	    }
	    
	    s_pixelToClipMatrix[3][0] = ( wNorm * m_rect.left - 1) * cosf
	                              - ( 1 - hNorm * (m_rect.top + m_rect.height))  * sinf;
	    s_pixelToClipMatrix[3][1] = ( wNorm * m_rect.left - 1 ) * sinf
	                              + ( 1 - hNorm * (m_rect.top + m_rect.height))  * cosf;

	    GLES20.glUniformMatrix4fv(ms_shader.m_matrixIndex, 1, false, GLUtil.wrap(s_pixelToClipMatrix));

	    // set up texturing.
	    boolean ae = false;
	    if (m_tex.getHasAlpha() || (myAlpha<1.0f))
	    {
	        ae = true;
	        GLES.glEnable(GL11.GL_BLEND);
	        // Alpha sums in the destination channel to ensure that
	        // partially-opaque items do not decrease the destination
	        // alpha and thus "cut holes" in the backdrop
	        GLES.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
	        		GL11.GL_ONE, GL11.GL_ONE);
	    }
	    else
	    	GLES.glDisable(GL11.GL_BLEND);

	    GLES.glActiveTexture(GL11.GL_TEXTURE0);
	    GLES.glBindTexture(GL11.GL_TEXTURE_2D, m_tex.getGLTex());
	    GLES.glBindBuffer(GL11.GL_ARRAY_BUFFER, m_vFlip?ms_vboFlip:ms_vbo);
	    GLES.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, ms_ibo);

	    GLES20.glVertexAttribPointer(ms_shader.m_positionIndex, 2, GL11.GL_FLOAT, false, /*sizeof(NvTexturedVertex)*/ 16, 0);
	    GLES20.glEnableVertexAttribArray(ms_shader.m_positionIndex);
	    GLES20.glVertexAttribPointer(ms_shader.m_uvIndex, 2, GL11.GL_FLOAT, false, /*sizeof(NvTexturedVertex)*/16, /*(void*)sizeof(nv::vec2<float>)*/ 8);
	    GLES20.glEnableVertexAttribArray(ms_shader.m_uvIndex);

	    // draw it already!
	    GLES.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_SHORT, 0);

	    //nv_flush_tracked_attribs();
	    GLES20.glDisableVertexAttribArray(ms_shader.m_positionIndex);
	    GLES20.glDisableVertexAttribArray(ms_shader.m_uvIndex);

	    if (ae)
	    	GLES.glDisable(GL11.GL_BLEND);
	    ms_shader.m_program.disable();
	    GLES.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	    GLES.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	    GLES.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	/**
	 * Sets a color value to multiply with during fragment processing.<p>
	 * Setting to white (1,1,1,x) color effectively disables colorization.
	 * @param color
	 */
	public void setColor(int color){
		m_color = color;
	}
	
	/** If we have a texture object, deletes our reference, and nulls out our member. */
	public void flushTexture(){
		if(m_tex != null){
			m_tex.delRef();
			m_tex = null;
		}
		
		m_scale = false;
	}
	
	/** Accessor to get our texture's width. */
    public int getTexWidth() { return (m_tex != null ?m_tex.getWidth():0); }
    /** Accessor to get our texture's height. */
    public int getTexHeight() { return (m_tex!= null ?m_tex.getHeight():0); }
    /** Accessor to get our texture's GL texture object ID. */
    public int getTexID() { return (m_tex != null ? m_tex.getGLTex():-1); }

    /** Set whether to vertically-flip our texture during Draw method. */    
    public void FlipVertical(boolean flipped)
    {
        m_vFlip = flipped;
    }
    
    /** Set whether to vertically-flip our texture during Draw method. */    
    public void FlipVertical()
    {
        m_vFlip = true;
    }
}
