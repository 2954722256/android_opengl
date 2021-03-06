////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014, NVIDIA CORPORATION. All rights reserved.
// Copyright (c) 2018 mzhg
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
package jet.learning.opengl.optimization;

import android.opengl.GLES20;

import com.nvidia.developer.opengl.utils.BufferUtils;
import com.nvidia.developer.opengl.utils.GLES;
import com.nvidia.developer.opengl.utils.GLUtil;
import com.nvidia.developer.opengl.utils.NvDisposeable;
import com.nvidia.developer.opengl.utils.NvImage;
import com.nvidia.developer.opengl.utils.NvUtils;
import com.nvidia.developer.opengl.utils.VectorUtil;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL11;

/**
 * Created by mazhen'gui on 2018/2/6.
 */

final class Terrain implements NvDisposeable {
    int m_colorTex;
    int m_quadsBuffer;
    int m_indexBuffer;

    int width;
    int height;
    int m_vertNum;
    int m_indexCount;

    public Terrain(TerrainInput a_input) {
        NvImage heightImage = NvImage.createFromDDSFile(a_input.heightmap);

        if(heightImage == null)
            return;

        width  = heightImage.getWidth();
        height = heightImage.getHeight();

        // read heightmap from file
        int size = width*height;
        float[] heights = new float [size/*sizeof(float)*/]; // TODO

        byte[] src = heightImage.getLevel(0);

        for(int x=0;x<width;x++)
            for(int y=0;y<height;y++)
                heights[y*width+x] = (float)(src[width*y + x] & 0xFF)/255.0f;

//		    delete heightImage;

        HeightSampler heightSampler = new HeightSampler(width, height, heights);

        // create terrain geometry
        final Matrix4f normalmatrix = OptimizationApp.tmp_mat0;
        normalmatrix.load(a_input.transform);
//		    normalmatrix.set_row(3, nv::vec4f(0,0,0,1)); // seems that nv::matrix store transpased mmatrix
        normalmatrix.m03 = normalmatrix.m13 = normalmatrix.m23 = 0;
        normalmatrix.m33 = 1;
//		    normalmatrix = transpose(inverse(normalmatrix));
        normalmatrix.invert();
        normalmatrix.transpose();

        int subdivsX = a_input.subdivsX - 1;
        int subdivsY = a_input.subdivsY - 1;

        float minX = -0.5f;
        float minY = -0.5f;

        float maxX = +0.5f;
        float maxY = +0.5f;

        float tMinX = 0.0f;
        float tMaxX = 1.0f;

        float tMinY = 0.0f;
        float tMaxY = 1.0f;

        VPosNormTex[] vertices = new VPosNormTex[subdivsX*subdivsY*4];
        int[] indices          = new int[subdivsX*subdivsY*6];

        float halfX = 0.5f/(subdivsX);
        float halfY = 0.5f/(subdivsY);

        int[] indexCache = new int [(subdivsX+1)*(subdivsY+1)];
        for(int i=0;i<(subdivsX+1)*(subdivsY+1);i++)
            indexCache[i] = -1;

        boolean sizeForZCurveIsOk = checkInputSizeForZCurve(subdivsX+1, subdivsY+1);

        int indOffset  = 0;

        VPosNormTex[] quad = new VPosNormTex[4];
        for(int i = 0; i < 4; i++)
            quad[i] = new VPosNormTex();

        final Vector3f vx = new Vector3f();
        final Vector3f vz = new Vector3f();
        final Vector3f v = new Vector3f();

        for(int y=0;y<subdivsY;y++)
        {
            float ty0 = (float)(y)/(float)(subdivsY);
            float ty1 = (float)(y+1)/(float)(subdivsY);

            for(int x=0;x<subdivsX;x++)
            {
                float tx0 = (float)(x)/(float)(subdivsX);
                float tx1 = (float)(x+1)/(float)(subdivsX);

                quad[0].pos.x = minX  + tx0*(maxX-minX);
                quad[0].pos.y = heightSampler.texture2D(tx0, ty1);
                quad[0].pos.z = minY  + ty1*(maxY-minY);
                quad[0].t.x   = tMinX + tx0*(tMaxX-tMinX);
                quad[0].t.y   = tMinY + ty1*(tMaxY-tMinY);
                quad[0].norm.set(0, 0, 0);

                quad[1].pos.x = minX + tx0*(maxX-minX);
                quad[1].pos.y = heightSampler.texture2D(tx0, ty0);
                quad[1].pos.z = minY + ty0*(maxY-minY);
                quad[1].t.x   = tMinX + tx0*(tMaxX-tMinX);
                quad[1].t.y   = tMinY + ty0*(tMaxY-tMinY);
                quad[1].norm.set(0, 0, 0);

                quad[2].pos.x = minX + tx1*(maxX-minX);
                quad[2].pos.y = heightSampler.texture2D(tx1, ty1);
                quad[2].pos.z = minY + ty1*(maxY-minY);
                quad[2].t.x   = tMinX + tx1*(tMaxX-tMinX);
                quad[2].t.y   = tMinY + ty1*(tMaxY-tMinY);
                quad[2].norm.set(0, 0, 0);

                quad[3].pos.x = minX + tx1*(maxX-minX);
                quad[3].pos.y = heightSampler.texture2D(tx1, ty0);
                quad[3].pos.z = minY + ty0*(maxY-minY);
                quad[3].t.x   = tMinX + tx1*(tMaxX-tMinX);
                quad[3].t.y   = tMinY + ty0*(tMaxY-tMinY);
                quad[3].norm.set(0, 0, 0);

                for(int i=0;i<4;i++)
                {
                    float dyx = heightSampler.texture2D(quad[i].t.x + halfX, quad[i].t.y) - heightSampler.texture2D(quad[i].t.x-halfX, quad[i].t.y);
                    float dyz = heightSampler.texture2D(quad[i].t.x, quad[i].t.y+halfY)   - heightSampler.texture2D(quad[i].t.x, quad[i].t.y - halfY);

                    vx.set(1, dyx, 0.0f);
                    vz.set(0.0f, -dyz, -1);
//		                nv::vec3f v  = normalize(cross(vx, vz));
                    Vector3f.cross(vx, vz, v);
                    v.normalise();

                    quad[i].norm.set(v); // = normalize(cross(vx, vz));
                }

                for(int i=0;i<4;i++)
                {
//		                quad[i].pos  = a_input.transform*quad[i].pos;
                    VectorUtil.transformVector3(quad[i].pos, a_input.transform, quad[i].pos);
//		                quad[i].norm = normalize(normalmatrix*quad[i].norm);
                    VectorUtil.transformNormal(quad[i].norm, normalmatrix, quad[i].norm);
                    quad[i].norm.normalise();
                }

                int[] quadIndex/*[4]*/ = { (y+1)*(subdivsX+1) + x, y*(subdivsX+1) + x, (y+1)*(subdivsX+1) + x + 1, y*(subdivsX+1) + x + 1 }; // use ZCurve if possible
                if(sizeForZCurveIsOk)
                {
                    quadIndex[0] = ZIndex((short)(x),(short)(y+1));
                    quadIndex[1] = ZIndex((short)(x),(short)(y));
                    quadIndex[2] = ZIndex((short)(x+1), (short)(y+1));
                    quadIndex[3] = ZIndex((short)(x+1), (short)(y));
                }

                for(int i=0;i<4;i++)
                {
                    int offset = quadIndex[i];

                    if(indexCache[offset] == -1)
                    {
                        if(vertices  [offset] == null)
                            vertices  [offset] = new VPosNormTex();
                        vertices  [offset].set(quad[i]);
                        indexCache[offset] = offset;
                    }
                }


                indices[indOffset+0] = indexCache[quadIndex[2]];
                indices[indOffset+1] = indexCache[quadIndex[1]];
                indices[indOffset+2] = indexCache[quadIndex[0]];

                indices[indOffset+3] = indexCache[quadIndex[3]];
                indices[indOffset+4] = indexCache[quadIndex[1]];
                indices[indOffset+5] = indexCache[quadIndex[2]];

                indOffset += 6;
            }
        }

        m_vertNum    = (subdivsX+1)*(subdivsY+1);
        m_indexCount = indOffset;

        FloatBuffer buf = BufferUtils.createFloatBuffer(8 * m_vertNum);
        for(int i = 0; i < m_vertNum; i++)
            vertices[i].store(buf);
        buf.flip();

        m_quadsBuffer = GLES.glGenBuffers();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m_quadsBuffer);
        GLES.glBufferData(GLES20.GL_ARRAY_BUFFER, buf, GLES20.GL_STATIC_DRAW);

        m_indexBuffer = GLES.glGenBuffers();
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, m_indexBuffer);
        GLES.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, GLUtil.wrap(indices, 0, indOffset), GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        NvImage.upperLeftOrigin(false);
        m_colorTex = NvImage.uploadTextureFromDDSFile(a_input.colormap);
        NvImage.upperLeftOrigin(true);
    }

    @Override
    public void dispose() {
        GLES.glDeleteBuffers(m_quadsBuffer);
        GLES.glDeleteBuffers(m_colorTex);
    }

    void draw(int vPositionHandle, int vNormalHandle, int vTexCoordHandle){
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m_quadsBuffer);

        GLES20.glVertexAttribPointer(vPositionHandle, 3, GL11.GL_FLOAT, false, 32, 0);

        final int VEC3_SIZE = Vector3f.SIZE;
        if (vNormalHandle >= 0) {
            GLES20.glVertexAttribPointer(vNormalHandle,   3, GL11.GL_FLOAT, false, 32, VEC3_SIZE);
            GLES20.glEnableVertexAttribArray(vNormalHandle);
        }
        if (vTexCoordHandle >= 0) {
            GLES20.glVertexAttribPointer(vTexCoordHandle, 2, GL11.GL_FLOAT, false, 32, 2 * VEC3_SIZE);
            GLES20.glEnableVertexAttribArray(vTexCoordHandle);
        }

        GLES20.glEnableVertexAttribArray(vPositionHandle);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, m_indexBuffer);

        GLES20.glDrawElements(GL11.GL_TRIANGLES, m_indexCount, GLES20.GL_UNSIGNED_INT, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    int getColorTex() { return m_colorTex; }

    boolean checkInputSizeForZCurve(int a_x, int a_y)
    {
        int mult = 1;

        boolean iterate = true;
        while(iterate)
        {
            if(a_x%mult != 0 || a_y%mult != 0)
                return false;

            if(a_x/mult <= 1 && a_y%mult <= 0)
                return true;

            mult *= 2;
        }

        return false;
    }

    static final class VPosNormTex{
        final Vector3f pos = new Vector3f();
        final Vector3f norm = new Vector3f();
        final Vector2f t = new Vector2f();

        void set(VPosNormTex o){
            pos.set(o.pos);
            norm.set(o.norm);
            t.set(o.t);
        }

        void store(FloatBuffer buf){
            pos.store(buf);
            norm.store(buf);
            t.store(buf);
        }
    }

    static final class HeightSampler{
        float w;
        float h;
        float[] data;

        public HeightSampler(int sizeX, int sizeY, float[] a_data) {
            w = sizeX;
            h = sizeY;
            data = a_data;
        }

        float texture2D(float x, float y)
        {
            float u = x*(w-1.0f);
            float v = y*(h-1.0f);

            int ui = NvUtils.clamp((int)(u), 0, (int)(w-1));
            int vi = NvUtils.clamp((int)(v), 0, (int)(h-1));

            int ui1 = (ui+1 >= w) ? ui : ui+1;
            int vi1 = (vi+1 >= h) ? vi : vi+1;

            int iw = (int)(w);

            float du = u - (ui);
            float dv = v - (vi);

            return data[vi*iw + ui]*(1.0f-du)*(1.0f-dv) + data[vi*iw + ui1]*du*(1.0f-dv) + data[vi1*iw + ui1]*du*dv + data[vi1*iw + ui]*(1.0f-du)*dv;
        }
    }

    private static short MortonTable256[] =
            {
                    0x0000, 0x0001, 0x0004, 0x0005, 0x0010, 0x0011, 0x0014, 0x0015,
                    0x0040, 0x0041, 0x0044, 0x0045, 0x0050, 0x0051, 0x0054, 0x0055,
                    0x0100, 0x0101, 0x0104, 0x0105, 0x0110, 0x0111, 0x0114, 0x0115,
                    0x0140, 0x0141, 0x0144, 0x0145, 0x0150, 0x0151, 0x0154, 0x0155,
                    0x0400, 0x0401, 0x0404, 0x0405, 0x0410, 0x0411, 0x0414, 0x0415,
                    0x0440, 0x0441, 0x0444, 0x0445, 0x0450, 0x0451, 0x0454, 0x0455,
                    0x0500, 0x0501, 0x0504, 0x0505, 0x0510, 0x0511, 0x0514, 0x0515,
                    0x0540, 0x0541, 0x0544, 0x0545, 0x0550, 0x0551, 0x0554, 0x0555,
                    0x1000, 0x1001, 0x1004, 0x1005, 0x1010, 0x1011, 0x1014, 0x1015,
                    0x1040, 0x1041, 0x1044, 0x1045, 0x1050, 0x1051, 0x1054, 0x1055,
                    0x1100, 0x1101, 0x1104, 0x1105, 0x1110, 0x1111, 0x1114, 0x1115,
                    0x1140, 0x1141, 0x1144, 0x1145, 0x1150, 0x1151, 0x1154, 0x1155,
                    0x1400, 0x1401, 0x1404, 0x1405, 0x1410, 0x1411, 0x1414, 0x1415,
                    0x1440, 0x1441, 0x1444, 0x1445, 0x1450, 0x1451, 0x1454, 0x1455,
                    0x1500, 0x1501, 0x1504, 0x1505, 0x1510, 0x1511, 0x1514, 0x1515,
                    0x1540, 0x1541, 0x1544, 0x1545, 0x1550, 0x1551, 0x1554, 0x1555,
                    0x4000, 0x4001, 0x4004, 0x4005, 0x4010, 0x4011, 0x4014, 0x4015,
                    0x4040, 0x4041, 0x4044, 0x4045, 0x4050, 0x4051, 0x4054, 0x4055,
                    0x4100, 0x4101, 0x4104, 0x4105, 0x4110, 0x4111, 0x4114, 0x4115,
                    0x4140, 0x4141, 0x4144, 0x4145, 0x4150, 0x4151, 0x4154, 0x4155,
                    0x4400, 0x4401, 0x4404, 0x4405, 0x4410, 0x4411, 0x4414, 0x4415,
                    0x4440, 0x4441, 0x4444, 0x4445, 0x4450, 0x4451, 0x4454, 0x4455,
                    0x4500, 0x4501, 0x4504, 0x4505, 0x4510, 0x4511, 0x4514, 0x4515,
                    0x4540, 0x4541, 0x4544, 0x4545, 0x4550, 0x4551, 0x4554, 0x4555,
                    0x5000, 0x5001, 0x5004, 0x5005, 0x5010, 0x5011, 0x5014, 0x5015,
                    0x5040, 0x5041, 0x5044, 0x5045, 0x5050, 0x5051, 0x5054, 0x5055,
                    0x5100, 0x5101, 0x5104, 0x5105, 0x5110, 0x5111, 0x5114, 0x5115,
                    0x5140, 0x5141, 0x5144, 0x5145, 0x5150, 0x5151, 0x5154, 0x5155,
                    0x5400, 0x5401, 0x5404, 0x5405, 0x5410, 0x5411, 0x5414, 0x5415,
                    0x5440, 0x5441, 0x5444, 0x5445, 0x5450, 0x5451, 0x5454, 0x5455,
                    0x5500, 0x5501, 0x5504, 0x5505, 0x5510, 0x5511, 0x5514, 0x5515,
                    0x5540, 0x5541, 0x5544, 0x5545, 0x5550, 0x5551, 0x5554, 0x5555
            };

    private static int ZIndex(short x, short y)
    {
        return  MortonTable256[y >> 8]   << 17 |
                MortonTable256[x >> 8]   << 16 |
                MortonTable256[y & 0xFF] <<  1 |
                MortonTable256[x & 0xFF];
    }
}
