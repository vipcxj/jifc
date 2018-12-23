package me.cxj.ifc.utils;

import org.bimserver.models.ifc2x3tc1.*;
import org.eclipse.emf.common.util.EList;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by vipcxj on 2018/12/19.
 */
public class ModelUtils {

    private static Object nominalValueToObject(IfcValue nominalValue) {
        if (nominalValue instanceof IfcLabel) {
            return ((IfcLabel) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcIdentifier) {
            return ((IfcIdentifier) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcBoolean) {
            return ((IfcBoolean) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcText) {
            return ((IfcText) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcLengthMeasure) {
            return ((IfcLengthMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcPlaneAngleMeasure) {
            return ((IfcPlaneAngleMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcAreaMeasure) {
            return ((IfcAreaMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcVolumeMeasure) {
            return ((IfcVolumeMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcReal) {
            return ((IfcReal) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcPowerMeasure) {
            return ((IfcPowerMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcInteger) {
            return ((IfcInteger) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcElectricCurrentMeasure) {
            return ((IfcElectricCurrentMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcLogical) {
            return ((IfcLogical) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcCountMeasure) {
            return ((IfcCountMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcMassMeasure) {
            return ((IfcMassMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcMassPerLengthMeasure) {
            return ((IfcMassPerLengthMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcThermalAdmittanceMeasure) {
            return ((IfcThermalAdmittanceMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcThermalConductivityMeasure) {
            return ((IfcThermalConductivityMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcThermalExpansionCoefficientMeasure) {
            return ((IfcThermalExpansionCoefficientMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcThermalResistanceMeasure) {
            return ((IfcThermalResistanceMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcThermalTransmittanceMeasure) {
            return ((IfcThermalTransmittanceMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof IfcRatioMeasure) {
            return ((IfcRatioMeasure) nominalValue).getWrappedValue();
        } else {
            throw new RuntimeException("Not implemented: " + nominalValue.eClass().getName());
        }
    }

    private static Object nominalValueToObject(org.bimserver.models.ifc4.IfcValue nominalValue) {
        if (nominalValue instanceof org.bimserver.models.ifc4.IfcLabel) {
            return ((org.bimserver.models.ifc4.IfcLabel) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcIdentifier) {
            return ((org.bimserver.models.ifc4.IfcIdentifier) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcBoolean) {
            return ((org.bimserver.models.ifc4.IfcBoolean) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcText) {
            return ((org.bimserver.models.ifc4.IfcText) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcLengthMeasure) {
            return ((org.bimserver.models.ifc4.IfcLengthMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcPlaneAngleMeasure) {
            return ((org.bimserver.models.ifc4.IfcPlaneAngleMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcAreaMeasure) {
            return ((org.bimserver.models.ifc4.IfcAreaMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcVolumeMeasure) {
            return ((org.bimserver.models.ifc4.IfcVolumeMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcReal) {
            return ((org.bimserver.models.ifc4.IfcReal) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcPowerMeasure) {
            return ((org.bimserver.models.ifc4.IfcPowerMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcInteger) {
            return ((org.bimserver.models.ifc4.IfcInteger) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcElectricCurrentMeasure) {
            return ((org.bimserver.models.ifc4.IfcElectricCurrentMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcLogical) {
            return ((org.bimserver.models.ifc4.IfcLogical) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcCountMeasure) {
            return ((org.bimserver.models.ifc4.IfcCountMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcMassMeasure) {
            return ((org.bimserver.models.ifc4.IfcMassMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcMassPerLengthMeasure) {
            return ((org.bimserver.models.ifc4.IfcMassPerLengthMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcThermalAdmittanceMeasure) {
            return ((org.bimserver.models.ifc4.IfcThermalAdmittanceMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcThermalConductivityMeasure) {
            return ((org.bimserver.models.ifc4.IfcThermalConductivityMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcThermalExpansionCoefficientMeasure) {
            return ((org.bimserver.models.ifc4.IfcThermalExpansionCoefficientMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcThermalResistanceMeasure) {
            return ((org.bimserver.models.ifc4.IfcThermalResistanceMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcThermalTransmittanceMeasure) {
            return ((org.bimserver.models.ifc4.IfcThermalTransmittanceMeasure) nominalValue).getWrappedValue();
        } else if (nominalValue instanceof org.bimserver.models.ifc4.IfcRatioMeasure) {
            return ((org.bimserver.models.ifc4.IfcRatioMeasure) nominalValue).getWrappedValue();
        } else {
            throw new RuntimeException("Not implemented: " + nominalValue.eClass().getName());
        }
    }

    public static Object propertyToJavaObject(IfcProperty property) {
        if (property instanceof IfcPropertyBoundedValue) {
            return new Object[] {nominalValueToObject(((IfcPropertyBoundedValue) property).getLowerBoundValue()), nominalValueToObject(((IfcPropertyBoundedValue) property).getUpperBoundValue())};
        } else if (property instanceof IfcPropertyEnumeratedValue) {
            return ((IfcPropertyEnumeratedValue) property).getEnumerationValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList());
        } else if (property instanceof IfcPropertyListValue) {
            return ((IfcPropertyListValue) property).getListValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList());
        } else if (property instanceof IfcPropertyReferenceValue) {
            IfcObjectReferenceSelect reference = ((IfcPropertyReferenceValue) property).getPropertyReference();
            return "ref:" + ((IfcRoot) reference).getGlobalId();
        } else if (property instanceof IfcPropertySingleValue) {
            return nominalValueToObject(((IfcPropertySingleValue) property).getNominalValue());
        } else if (property instanceof IfcPropertyTableValue) {
            return new Object[] {
                    ((IfcPropertyTableValue) property).getDefiningValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList()),
                    ((IfcPropertyTableValue) property).getDefinedValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList())
            };
        } else if (property instanceof IfcComplexProperty) {
            Map<String, Object> properties = new HashMap<>();
            for (IfcProperty subProperty : ((IfcComplexProperty) property).getHasProperties()) {
                properties.put(subProperty.getName(), propertyToJavaObject(subProperty));
            }
            return properties;
        } else {
            throw new RuntimeException("Not implemented: " + property.eClass().getName());
        }
    }

    public static Object propertyToJavaObject(org.bimserver.models.ifc4.IfcProperty property) {
        if (property instanceof org.bimserver.models.ifc4.IfcPropertyBoundedValue) {
            return new Object[] {nominalValueToObject(((org.bimserver.models.ifc4.IfcPropertyBoundedValue) property).getLowerBoundValue()), nominalValueToObject(((org.bimserver.models.ifc4.IfcPropertyBoundedValue) property).getUpperBoundValue())};
        } else if (property instanceof org.bimserver.models.ifc4.IfcPropertyEnumeratedValue) {
            return ((org.bimserver.models.ifc4.IfcPropertyEnumeratedValue) property).getEnumerationValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList());
        } else if (property instanceof org.bimserver.models.ifc4.IfcPropertyListValue) {
            return ((org.bimserver.models.ifc4.IfcPropertyListValue) property).getListValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList());
        } else if (property instanceof org.bimserver.models.ifc4.IfcPropertyReferenceValue) {
            org.bimserver.models.ifc4.IfcObjectReferenceSelect reference = ((org.bimserver.models.ifc4.IfcPropertyReferenceValue) property).getPropertyReference();
            return "ref:" + ((org.bimserver.models.ifc4.IfcRoot) reference).getGlobalId();
        } else if (property instanceof org.bimserver.models.ifc4.IfcPropertySingleValue) {
            return nominalValueToObject(((org.bimserver.models.ifc4.IfcPropertySingleValue) property).getNominalValue());
        } else if (property instanceof org.bimserver.models.ifc4.IfcPropertyTableValue) {
            return new Object[] {
                    ((org.bimserver.models.ifc4.IfcPropertyTableValue) property).getDefiningValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList()),
                    ((org.bimserver.models.ifc4.IfcPropertyTableValue) property).getDefinedValues().stream().map(ModelUtils::nominalValueToObject).collect(Collectors.toList())
            };
        } else if (property instanceof org.bimserver.models.ifc4.IfcComplexProperty) {
            Map<String, Object> properties = new HashMap<>();
            for (org.bimserver.models.ifc4.IfcProperty subProperty : ((org.bimserver.models.ifc4.IfcComplexProperty) property).getHasProperties()) {
                properties.put(subProperty.getName(), propertyToJavaObject(subProperty));
            }
            return properties;
        } else {
            throw new RuntimeException("Not implemented: " + property.eClass().getName());
        }
    }

    public static IfcTypeObject getTypeObject(IfcObject object) {
        for (IfcRelDefines relDefines : object.getIsDefinedBy()) {
            if (relDefines instanceof IfcRelDefinesByType) {
                return ((IfcRelDefinesByType) relDefines).getRelatingType();
            }
        }
        return null;
    }

    public static org.bimserver.models.ifc4.IfcTypeObject getTypeObject(org.bimserver.models.ifc4.IfcObject object) {
        EList<org.bimserver.models.ifc4.IfcRelDefinesByType> typedBy = object.getIsTypedBy();
        return typedBy != null && !typedBy.isEmpty() ? typedBy.get(0).getRelatingType() : null;
    }

    public static IfcGeometricRepresentationContext getModelGeometricRepresentationContext(IfcProject project) {
        return getGeometricRepresentationContext(project, "Model");
    }

    public static IfcGeometricRepresentationContext getPlanGeometricRepresentationContext(IfcProject project) {
        return getGeometricRepresentationContext(project, "Plan");
    }

    public static IfcGeometricRepresentationContext getGeometricRepresentationContext(IfcProject project, String type) {
        EList<IfcRepresentationContext> representationContexts = project.getRepresentationContexts();
        if (representationContexts != null) {
            for (IfcRepresentationContext representationContext : representationContexts) {
                if (representationContext instanceof IfcGeometricRepresentationContext) {
                    IfcGeometricRepresentationContext grc = (IfcGeometricRepresentationContext) representationContext;
                    if (Objects.equals(grc.getContextType(), type)) {
                        return grc;
                    }
                }
            }
        }
        return null;
    }

    public static org.bimserver.models.ifc4.IfcGeometricRepresentationContext getModelGeometricRepresentationContext(org.bimserver.models.ifc4.IfcProject project) {
        return getGeometricRepresentationContext(project, "Model");
    }

    public static org.bimserver.models.ifc4.IfcGeometricRepresentationContext getPlanGeometricRepresentationContext(org.bimserver.models.ifc4.IfcProject project) {
        return getGeometricRepresentationContext(project, "Plan");
    }

    public static org.bimserver.models.ifc4.IfcGeometricRepresentationContext getGeometricRepresentationContext(org.bimserver.models.ifc4.IfcProject project, String type) {
        EList<org.bimserver.models.ifc4.IfcRepresentationContext> representationContexts = project.getRepresentationContexts();
        if (representationContexts != null) {
            for (org.bimserver.models.ifc4.IfcRepresentationContext representationContext : representationContexts) {
                if (representationContext instanceof org.bimserver.models.ifc4.IfcGeometricRepresentationContext) {
                    org.bimserver.models.ifc4.IfcGeometricRepresentationContext grc = (org.bimserver.models.ifc4.IfcGeometricRepresentationContext) representationContext;
                    if (Objects.equals(grc.getContextType(), type)) {
                        return grc;
                    }
                }
            }
        }
        return null;
    }

    private static final double[] DEFAULT_TRUE_NORTH = new double[] {0., 1.};

    public static double[] getTrueNorth(IfcProject project) {
        IfcGeometricRepresentationContext grc = getModelGeometricRepresentationContext(project);
        if (grc == null) {
            return DEFAULT_TRUE_NORTH;
        }
        IfcDirection trueNorth = grc.getTrueNorth();
        if (trueNorth != null) {
            return new double[] {trueNorth.getDirectionRatios().get(0), trueNorth.getDirectionRatios().get(1)};
        }
        return DEFAULT_TRUE_NORTH;
    }

    public static double[] getTrueNorth(org.bimserver.models.ifc4.IfcProject project) {
        org.bimserver.models.ifc4.IfcGeometricRepresentationContext grc = getModelGeometricRepresentationContext(project);
        if (grc == null) {
            return DEFAULT_TRUE_NORTH;
        }
        org.bimserver.models.ifc4.IfcDirection trueNorth = grc.getTrueNorth();
        if (trueNorth != null) {
            return new double[] {trueNorth.getDirectionRatios().get(0), trueNorth.getDirectionRatios().get(1)};
        }
        return DEFAULT_TRUE_NORTH;
    }
}
