package jet.learning.opengl.common;

import android.opengl.GLES20;
import android.opengl.GLES30;

import com.nvidia.developer.opengl.utils.GLES;
import com.nvidia.developer.opengl.utils.GLUtil;
import com.nvidia.developer.opengl.utils.NvGLSLProgram;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL11;

public class SkyRenderer {

	private static final int POSITION = 0;
	
	private int mVB;
	private int mIB;
	private int mVBO;
	
	private int mCubeMapSRV;
	private int mProgram;
	
	private int mCubeMapLoc;
	private int mWvpLoc;
	private int mIndexCount;

	public SkyRenderer(String cubemapFilename, float skySphereRadius) {
		mCubeMapSRV = NvImage.uploadTextureFromDDSFile(cubemapFilename);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, mCubeMapSRV);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GL11.GL_REPEAT);
		GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, 0);

		NvGLSLProgram program = NvGLSLProgram.createFromFiles("d3dcoder/sky.glvs", "d3dcoder/sky.glfs");

		mProgram = program.getProgram();
		mCubeMapLoc = GLES30.glGetUniformLocation(mProgram, "gCubeMap");
		mWvpLoc 	= GLES30.glGetUniformLocation(mProgram, "gWorldViewProj");
		
		MeshData sphere = new MeshData();
		GeometryGenerator geoGen = new GeometryGenerator();
		
		geoGen.createSphere(skySphereRadius, 30, 30, sphere);
		mIndexCount = sphere.getIndiceCount();
		FloatBuffer vertices = GLUtil.getCachedFloatBuffer(sphere.vertices.size() * 3);
		for(int i = 0; i < sphere.vertices.size(); ++i)
		{
			Vertex v = sphere.vertices.get(i);
			vertices.put(v.positionX).put(v.positionY).put(v.positionZ);
		}
		vertices.flip();
		
		mVB = GLES.glGenBuffers();
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVB);
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.remaining() * 4, vertices, GLES30.GL_STATIC_DRAW);
		
		mIB = GLES.glGenBuffers();
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIB);
		GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, 2 * sphere.getIndiceCount(), GLUtil.wrap(sphere.indices.getData(), 0, sphere.getIndiceCount()), GLES30.GL_STATIC_DRAW);
		
		mVBO = GLES.glGenVertexArrays();
		GLES30.glBindVertexArray(mVBO);
		{
			GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVB);
			
			GLES30.glVertexAttribPointer(POSITION, 3, GL11.GL_FLOAT, false, 0, 0);
			GLES30.glEnableVertexAttribArray(POSITION);

			GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, mIB);
		}

		GLES30.glBindVertexArray(0);
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
		GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	/**
	 * Cube box must be draw first.
	 * @param mvpWithoutTranslate the modelViewProjection Matrix that removed the camera translate
     */
	public void draw(Matrix4f mvpWithoutTranslate){
		GLES20.glUseProgram(mProgram);
		GLES20.glUniformMatrix4fv(mWvpLoc, 1, false, GLUtil.wrap(mvpWithoutTranslate));
		GLES20.glUniform1i(mCubeMapLoc, 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, mCubeMapSRV);

		GLES30.glBindVertexArray(mVBO);
		GLES20.glDrawElements(GL11.GL_TRIANGLES, mIndexCount, GL11.GL_UNSIGNED_SHORT, 0);
		GLES30.glBindVertexArray(0);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, 0);
	}
	
	public int cubeMapSRV() { return mCubeMapSRV;}
	
	public void dispose(){
		GLES.glDeleteBuffers(mVB);
		GLES.glDeleteBuffers(mIB);

		GLES.glDeleteVertexArrays(mVBO);
		GLES.glDeleteTextures(mCubeMapSRV);
	}
}