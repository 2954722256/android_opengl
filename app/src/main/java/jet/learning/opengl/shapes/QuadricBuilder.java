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
package jet.learning.opengl.shapes;

public class QuadricBuilder {

	int x_steps = 2;
	int y_steps = 2;
	boolean hasNormal = true;
	boolean autoGenNormal = false;
	boolean hasTexCoord = false;
	boolean autoGenTexCoord = true;
	boolean genNormalClamped = true;  // true for clamp, false for repeat.
	boolean hasColor = false;
	
	boolean centerToOrigin;

	// True for OpenGL ES 2.0 above, false for OpenGL ES 1.1
	boolean flag = true;
	int positionLocation;
	int normalLocation;
	int texCoordLocation;
	int colorLocation;
	DrawMode drawMode = DrawMode.FILL;
	
	public QuadricBuilder() {}
	
	public QuadricBuilder(QuadricBuilder o) {
		set(o);
	}
	
	public void set(QuadricBuilder o){
		x_steps = o.x_steps;
		y_steps = o.y_steps;
		hasNormal = o.hasNormal;
		autoGenNormal = o.autoGenNormal;
		hasTexCoord = o.hasTexCoord;
		autoGenTexCoord = o.autoGenTexCoord;
		genNormalClamped = o.genNormalClamped;
		hasColor = o.hasColor;
		flag = o.flag;
		positionLocation = o.positionLocation;
		normalLocation = o.normalLocation;
		texCoordLocation = o.texCoordLocation;
		colorLocation = o.colorLocation;
		drawMode = o.drawMode;
		centerToOrigin = o.centerToOrigin;
	}
	
	public QuadricBuilder setXSteps(int steps){
		if(x_steps < 1)
			throw new IllegalArgumentException("x_steps is less than 1. x_steps = " + x_steps);
		x_steps = steps;
		return this;
	}
	
	public QuadricBuilder setYSteps(int steps){
		if(y_steps < 1)
			throw new IllegalArgumentException("y_steps is less than 1. y_steps = " + y_steps);
		y_steps = steps;
		return this;
	}
	
	public QuadricBuilder setGenNormal(boolean flag){
		hasNormal = flag;
		return this;
	}
	
	public QuadricBuilder setAutoGenNormal(boolean flag){
		autoGenNormal = flag;
		return this;
	}
	
	public QuadricBuilder setGenTexCoord(boolean flag){
		hasTexCoord = flag;
		return this;
	}
	
	public QuadricBuilder setAutoGenTexCoord(boolean flag){
		autoGenTexCoord = flag;
		return this;
	}
	
	public QuadricBuilder setGenNormalMode(boolean clamp){
		genNormalClamped = clamp;
		return this;
	}
	
	public QuadricBuilder setGenColor(boolean flag){
		hasColor = flag;
		return this;
	}
	
	public QuadricBuilder setPostionLocation(int location){
		positionLocation = location;
		return this;
	}
	
	public int getPositionLocation() {return positionLocation;}
	
	public QuadricBuilder setNormalLocation(int location){
		normalLocation = location;
		return this;
	}
	
	public int getNormalLocation() {return normalLocation;}
	
	public QuadricBuilder setTexCoordLocation(int location){
		texCoordLocation = location;
		return this;
	}
	
	public int getTexCoordLocation() {return texCoordLocation;}
	
	public QuadricBuilder setColorLocation(int location){
		colorLocation = location;
		return this;
	}

	public QuadricBuilder setFlag(boolean flag){
		this.flag = flag;
		return this;
	}
	
	public int getColorLocation() { return colorLocation;}
	
	public QuadricBuilder setDrawMode(DrawMode mode){
		this.drawMode = mode;
		return this;
	}
	
	public QuadricBuilder setCenterToOrigin(boolean flag){
		centerToOrigin = flag;
		return this;
	}
	
	public boolean isCenterToOrigin() { return centerToOrigin;}
}
