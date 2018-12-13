// Targeted by JavaCPP version 1.4.3: DO NOT EDIT THIS FILE

package me.cxj.ifc.ifc4;

import java.nio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

public class IfcGeomIterator extends me.cxj.ifc.ifc4.presets.IfcGeomIterator {
    static { Loader.load(); }

// Parsed from IfcGeomDataIterator.hpp

// #ifndef IFCGEOMDATAITERATOR_H
// #define IFCGEOMDATAITERATOR_H

// #include <cstdint>

// #ifdef _WIN32
// #define DllExport __declspec(dllexport)
// #else // simply assume *nix + GCC-like compiler
// #define DllExport __attribute__((visibility("default")))
// #endif

	@Namespace("IfcGeom") public static native @Cast("const char*") BytePointer allocateByteArray(@Cast("size_t") long size);

	@Namespace("IfcGeom") @NoOffset public static class GeomData extends Pointer {
	    static { Loader.load(); }
	    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
	    public GeomData(Pointer p) { super(p); }
	

		public native int id(); public native GeomData id(int id);
		public native @Cast("char*") BytePointer guid(); public native GeomData guid(BytePointer guid);
		public native @Cast("char*") BytePointer name(); public native GeomData name(BytePointer name);
		public native @Cast("char*") BytePointer type(); public native GeomData type(BytePointer type);
		public native int parentId(); public native GeomData parentId(int parentId);
		public native double matrix(int i); public native GeomData matrix(int i, double matrix);
		@MemberGetter public native DoublePointer matrix();
		public native int repId(); public native GeomData repId(int repId);
		public native FloatPointer positions(); public native GeomData positions(FloatPointer positions);
		public native @Cast("size_t") long szPositions(); public native GeomData szPositions(long szPositions);
		public native FloatPointer normals(); public native GeomData normals(FloatPointer normals);
		public native @Cast("size_t") long szNormals(); public native GeomData szNormals(long szNormals);
		public native IntPointer indices(); public native GeomData indices(IntPointer indices);
		public native @Cast("size_t") long szIndices(); public native GeomData szIndices(long szIndices);
		public native FloatPointer colors(); public native GeomData colors(FloatPointer colors);
		public native @Cast("size_t") long szColors(); public native GeomData szColors(long szColors);
		public native IntPointer materialIndices(); public native GeomData materialIndices(IntPointer materialIndices);
		public native @Cast("size_t") long szMaterialIndices(); public native GeomData szMaterialIndices(long szMaterialIndices);
		public native double area(); public native GeomData area(double area);
		public native double volume(); public native GeomData volume(double volume);
	}

	@Namespace("IfcGeom") @NoOffset public static class DataIterator extends Pointer {
	    static { Loader.load(); }
	    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
	    public DataIterator(Pointer p) { super(p); }
	
		/** Enumeration of setting identifiers. These settings define the
		 *  behaviour of various aspects of IfcOpenShell. */
		/** enum IfcGeom::DataIterator::Setting */
		public static final int
			/** Specifies whether vertices are welded, meaning that the coordinates
			 *  vector will only contain unique xyz-triplets. This results in a 
			 *  manifold mesh which is useful for modelling applications, but might 
			 *  result in unwanted shading artifacts in rendering applications. */
			WELD_VERTICES = 1,
			/** Specifies whether to apply the local placements of building elements
			 *  directly to the coordinates of the representation mesh rather than
			 *  to represent the local placement in the 4x3 matrix, which will in that
			 *  case be the identity matrix. */
			USE_WORLD_COORDS = 1 << 1,
			/** Internally IfcOpenShell measures everything in meters. This settings
			 *  specifies whether to convert IfcGeomObjects back to the units in which
			 *  the geometry in the IFC file is specified. */
			CONVERT_BACK_UNITS = 1 << 2,
			/** Specifies whether to use the Open Cascade BREP format for representation
			 *  items rather than to create triangle meshes. This is useful is IfcOpenShell
			 *  is used as a library in an application that is also built on Open Cascade. */
			USE_BREP_DATA = 1 << 3,
			/** Specifies whether to sew IfcConnectedFaceSets (open and closed shells) to
			 *  TopoDS_Shells or whether to keep them as a loose collection of faces. */
			SEW_SHELLS = 1 << 4,
			/** Specifies whether to compose IfcOpeningElements into a single compound
			 *  in order to speed up the processing of opening subtractions. */
			FASTER_BOOLEANS = 1 << 5,
			/** Disables the subtraction of IfcOpeningElement representations from
			 *  the related building element representations. */
			DISABLE_OPENING_SUBTRACTIONS = 1 << 6,
			/** Disables the triangulation of the topological representations. Useful if
			 *  the client application understands Open Cascade's native format. */
			DISABLE_TRIANGULATION = 1 << 7,
			/** Applies default materials to entity instances without a surface style. */
			APPLY_DEFAULT_MATERIALS = 1 << 8,
			/** Specifies whether to include subtypes of IfcCurve. */
			INCLUDE_CURVES = 1 << 9,
			/** Specifies whether to exclude subtypes of IfcSolidModel and IfcSurface. */
			EXCLUDE_SOLIDS_AND_SURFACES = 1 << 10,
			/** Disables computation of normals. Saves time and file size and is useful
			 *  in instances where you're going to recompute normals for the exported
			 *  model in other modelling application in any case. */
			NO_NORMALS = 1 << 11,
			/** Generates UVs by using simple box projection. Requires normals.
			 *  Applicable for OBJ and DAE output. */
			GENERATE_UVS = 1 << 12,
			/** Specifies whether to slice representations according to associated IfcLayerSets. */
			APPLY_LAYERSETS = 1 << 13,
			/** Search for a parent of type IfcBuildingStorey for each representation */
			
///
			SEARCH_FLOOR = 1 << 14,
			/** */
			
///
			SITE_LOCAL_PLACEMENT = 1 << 15,
			/** */
			BUILDING_LOCAL_PLACEMENT = 1 << 16,
			/** Number of different setting flags. */
			NUM_SETTINGS = 16;

		public DataIterator(Pointer data, int length) { super((Pointer)null); allocate(data, length); }
		private native void allocate(Pointer data, int length);
		public DataIterator(@Cast("unsigned") int settings, double deflection_tolerance, Pointer data, int length) { super((Pointer)null); allocate(settings, deflection_tolerance, data, length); }
		private native void allocate(@Cast("unsigned") int settings, double deflection_tolerance, Pointer data, int length);

		public native @Cast("bool") boolean hasNext();

		public native GeomData next();

		/** Computes model's bounding box (bounds_min and bounds_max).
		 *  \note Can take several minutes for large files. */
		public native void computeBounds();

		public native FloatPointer getBoundMin();
		public native FloatPointer getBoundMax();
		public native Pointer getData();
	}



// #endif

}
