//----------------------------------------------------------------------------------
// File:        NvGraphicShader.java
// SDK Version: v1.2 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------
package com.nvidia.developer.opengl.ui;

import com.nvidia.developer.opengl.utils.NvGLSLProgram;

/** A helper object for managing a shader program for NvUIGraphic and subclasses. */
public class NvGraphicShader {
	/** A pointer to an allocated NvGLSLProgram object.  We're not a subclass as we could easily have some other method for loading/storing what shader program we are using (previously, this was just a GLint for the compiled program). */
	public NvGLSLProgram m_program;
	/** Index for position attribute */
	public int m_positionIndex;
	/** Index for uv attribute */
	public int m_uvIndex;
	/** Index for matrix uniform */
	public int m_matrixIndex;
	/** Index for alpha uniform */
	public int m_alphaIndex;
	/** Index for color uniform */
	public int m_colorIndex;
	
	public void load(String vs, String fs){
		NvGLSLProgram prog = NvGLSLProgram.createFromStrings(vs, fs, false);
		
		m_program = prog;
		prog.enable();
		
		m_positionIndex = prog.getAttribLocation("position", false);
	    m_uvIndex = prog.getAttribLocation("tex", false);

	    prog.setUniform1i(prog.getUniformLocation("sampler", false), 0); // texunit index zero.

	    m_matrixIndex = prog.getUniformLocation("pixelToClipMat", false);
	    m_alphaIndex = prog.getUniformLocation("alpha", false);
	    m_colorIndex = prog.getUniformLocation("color", false);

	    prog.disable();
	}
}
