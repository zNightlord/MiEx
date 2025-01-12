/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import nl.bramstout.mcworldexporter.nbt.NBT_Tag;
import nl.bramstout.mcworldexporter.nbt.TAG_Byte;
import nl.bramstout.mcworldexporter.nbt.TAG_Compound;
import nl.bramstout.mcworldexporter.nbt.TAG_Double;
import nl.bramstout.mcworldexporter.nbt.TAG_Float;
import nl.bramstout.mcworldexporter.nbt.TAG_Int;
import nl.bramstout.mcworldexporter.nbt.TAG_Long;
import nl.bramstout.mcworldexporter.nbt.TAG_Short;
import nl.bramstout.mcworldexporter.nbt.TAG_String;

public class BlockStateMultiPart extends BlockStatePart{
	
	private JsonObject check;
	
	public BlockStateMultiPart(JsonElement data, boolean doubleSided) {
		super();
		
		check = null;
		if(data.getAsJsonObject().has("when"))
			check = data.getAsJsonObject().get("when").getAsJsonObject();
		
		JsonElement modelData = data.getAsJsonObject().get("apply");
		if(modelData.isJsonArray()) {
			for(JsonElement el : modelData.getAsJsonArray().asList()) {
				int modelId = ModelRegistry.getIdForName(el.getAsJsonObject().get("model").getAsString(), doubleSided);
				int rotX = 0;
				int rotY = 0;
				boolean uvLock = false;
				if(el.getAsJsonObject().has("x"))
					rotX = el.getAsJsonObject().get("x").getAsInt();
				if(el.getAsJsonObject().has("y"))
					rotY = el.getAsJsonObject().get("y").getAsInt();
				if(el.getAsJsonObject().has("uvlock"))
					uvLock = el.getAsJsonObject().get("uvlock").getAsBoolean();
				Model model = new Model(ModelRegistry.getModel(modelId));
				if(rotX != 0 || rotY != 0)
					model.rotate(rotX, rotY, uvLock);
				if(el.getAsJsonObject().has("weight"))
					model.setWeight(el.getAsJsonObject().get("weight").getAsInt());
				models.add(model);
			}
		} else if(modelData.isJsonObject()) {
			int modelId = ModelRegistry.getIdForName(modelData.getAsJsonObject().get("model").getAsString(), doubleSided);
			int rotX = 0;
			int rotY = 0;
			boolean uvLock = false;
			if(modelData.getAsJsonObject().has("x"))
				rotX = modelData.getAsJsonObject().get("x").getAsInt();
			if(modelData.getAsJsonObject().has("y"))
				rotY = modelData.getAsJsonObject().get("y").getAsInt();
			if(modelData.getAsJsonObject().has("uvlock"))
				uvLock = modelData.getAsJsonObject().get("uvlock").getAsBoolean();
			Model model = new Model(ModelRegistry.getModel(modelId));
			if(rotX != 0 || rotY != 0)
				model.rotate(rotX, rotY, uvLock);
			models.add(model);
		}
	}

	@Override
	public boolean usePart(TAG_Compound properties) {
		if(check == null)
			return true;
		if(check.has("OR")) {
			for(JsonElement checkObj : check.get("OR").getAsJsonArray().asList()) {
				if(testProperties(properties, checkObj.getAsJsonObject()))
					return true;
			}
			return false;
		} else if(check.has("AND")) {
			for(JsonElement checkObj : check.get("AND").getAsJsonArray().asList()) {
				if(!testProperties(properties, checkObj.getAsJsonObject()))
					return false;
			}
			return true;
		} else {
			return testProperties(properties, check);
		}
	}
	
	private boolean testProperties(TAG_Compound properties, JsonObject checkObject) {
		for(NBT_Tag tag : properties.elements) {
			if(!checkObject.has(tag.getName()))
				continue;
			String[] values = checkObject.get(tag.getName()).getAsString().split("\\|");
			String propValue = null;
			switch(tag.ID()) {
			case 1:
				// Byte
				propValue = Byte.toString(((TAG_Byte)tag).value);
				break;
			case 2:
				// Short
				propValue = Short.toString(((TAG_Short)tag).value);
				break;
			case 3:
				// Int
				propValue = Integer.toString(((TAG_Int)tag).value);
				break;
			case 4:
				// Long
				propValue = Long.toString(((TAG_Long)tag).value);
				break;
			case 5:
				// Float
				propValue = Float.toString(((TAG_Float)tag).value);
				break;
			case 6:
				// Double
				propValue = Double.toString(((TAG_Double)tag).value);
				break;
			case 8:
				// String
				propValue = ((TAG_String)tag).value;
				break;
			default:
				break;
			}
			
			if(propValue != null) {
				boolean found = false;
				for(String value : values) {
					if(propValue.equals(value)) {
						found = true;
						break;
					} else if((value.equals("false") && propValue.equals("0")) || (value.equals("true") && propValue.equals("1"))) {
						found = true;
						break;
					}
				}
				if(!found)
					return false;
			}
		}
		return true;
	}

}
