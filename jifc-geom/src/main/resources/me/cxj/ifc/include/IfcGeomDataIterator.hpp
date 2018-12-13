#ifndef IFCGEOMDATAITERATOR_H
#define IFCGEOMDATAITERATOR_H

#include <cstdint>

#ifdef _WIN32
#define DllExport __declspec(dllexport)
#else // simply assume *nix + GCC-like compiler
#define DllExport __attribute__((visibility("default")))
#endif

namespace IfcGeom {

	const char* allocateByteArray(size_t size) {
		return new char[size];
	}

	class DataIterator;

	class DllExport GeomData
	{
	private:
		GeomData(
			int32_t id,
			const char* guid,
			const char* name,
			const char* type,
			int32_t parentId,
			const double(&matrix)[16],
			int32_t repId,
			const float* positions,
			size_t szPositions,
			const float* normals,
			size_t szNormals,
			const int32_t* indices,
			size_t szIndices,
			const float* colors,
			size_t szColors,
			const int32_t* materialIndices,
			size_t szMaterialIndices,
			double area,
			double volume
		);
		friend class DataIterator;

	public:
		~GeomData();

		int32_t id;
		char* guid;
		char* name;
		char* type;
		int32_t parentId;
		double matrix[16];
		int32_t repId;
		float* positions;
		size_t szPositions;
		float* normals;
		size_t szNormals;
		int32_t* indices;
		size_t szIndices;
		float* colors;
		size_t szColors;
		int32_t* materialIndices;
		size_t szMaterialIndices;
		double area;
		double volume;
	};

	template<typename P>
	class Iterator;

	class DllExport DataIterator {
	private:
		DataIterator(const DataIterator&) {}; // N/I
		DataIterator& operator=(const DataIterator&); // N/I

		void* bufferData;
		Iterator<float>* iterator;
		GeomData* data;
		bool more;
		float boundMin[3];
		float boundMax[3];
		bool bound;

		void getCurrent();

	public:
		/// Enumeration of setting identifiers. These settings define the
		/// behaviour of various aspects of IfcOpenShell.
		enum Setting
		{
			/// Specifies whether vertices are welded, meaning that the coordinates
			/// vector will only contain unique xyz-triplets. This results in a 
			/// manifold mesh which is useful for modelling applications, but might 
			/// result in unwanted shading artifacts in rendering applications.
			WELD_VERTICES = 1,
			/// Specifies whether to apply the local placements of building elements
			/// directly to the coordinates of the representation mesh rather than
			/// to represent the local placement in the 4x3 matrix, which will in that
			/// case be the identity matrix.
			USE_WORLD_COORDS = 1 << 1,
			/// Internally IfcOpenShell measures everything in meters. This settings
			/// specifies whether to convert IfcGeomObjects back to the units in which
			/// the geometry in the IFC file is specified.
			CONVERT_BACK_UNITS = 1 << 2,
			/// Specifies whether to use the Open Cascade BREP format for representation
			/// items rather than to create triangle meshes. This is useful is IfcOpenShell
			/// is used as a library in an application that is also built on Open Cascade.
			USE_BREP_DATA = 1 << 3,
			/// Specifies whether to sew IfcConnectedFaceSets (open and closed shells) to
			/// TopoDS_Shells or whether to keep them as a loose collection of faces.
			SEW_SHELLS = 1 << 4,
			/// Specifies whether to compose IfcOpeningElements into a single compound
			/// in order to speed up the processing of opening subtractions.
			FASTER_BOOLEANS = 1 << 5,
			/// Disables the subtraction of IfcOpeningElement representations from
			/// the related building element representations.
			DISABLE_OPENING_SUBTRACTIONS = 1 << 6,
			/// Disables the triangulation of the topological representations. Useful if
			/// the client application understands Open Cascade's native format.
			DISABLE_TRIANGULATION = 1 << 7,
			/// Applies default materials to entity instances without a surface style.
			APPLY_DEFAULT_MATERIALS = 1 << 8,
			/// Specifies whether to include subtypes of IfcCurve.
			INCLUDE_CURVES = 1 << 9,
			/// Specifies whether to exclude subtypes of IfcSolidModel and IfcSurface.
			EXCLUDE_SOLIDS_AND_SURFACES = 1 << 10,
			/// Disables computation of normals. Saves time and file size and is useful
			/// in instances where you're going to recompute normals for the exported
			/// model in other modelling application in any case.
			NO_NORMALS = 1 << 11,
			/// Generates UVs by using simple box projection. Requires normals.
			/// Applicable for OBJ and DAE output.
			GENERATE_UVS = 1 << 12,
			/// Specifies whether to slice representations according to associated IfcLayerSets.
			APPLY_LAYERSETS = 1 << 13,
			/// Search for a parent of type IfcBuildingStorey for each representation
			SEARCH_FLOOR = 1 << 14,
			///
			SITE_LOCAL_PLACEMENT = 1 << 15,
			///
			BUILDING_LOCAL_PLACEMENT = 1 << 16,
			/// Number of different setting flags.
			NUM_SETTINGS = 16
		};
		typedef unsigned SettingField;

		DataIterator(void* data, int length);
		DataIterator(unsigned settings, double deflection_tolerance, void* data, int length);
		~DataIterator();

		bool hasNext() const;

		GeomData* next();

		/// Computes model's bounding box (bounds_min and bounds_max).
		/// @note Can take several minutes for large files.
		void computeBounds();

		float* getBoundMin();
		float* getBoundMax();
		void* getData() const { return bufferData; };
	};

};

#endif