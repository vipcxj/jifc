package me.cxj.ifc.deserializer;

/******************************************************************************
 * Copyright (C) 2009-2018  BIMserver.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import org.bimserver.plugins.deserializers.DeserializeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParserPlan {
	private final List<Pass> passes = new ArrayList<>();
	
	public ParserPlan(Pass... passes) {
		Collections.addAll(this.passes, passes);
	}
	
	public void add(Pass pass) {
		passes.add(pass);
	}
	
	public String process(int lineNumber, String input) throws DeserializeException {
		for (Pass pass : passes) {
			input = pass.process(lineNumber, input);
		}
		return input;
	}
}
