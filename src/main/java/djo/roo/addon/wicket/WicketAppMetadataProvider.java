package djo.roo.addon.wicket;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link WicketAppMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public final class WicketAppMetadataProvider extends AbstractItdMetadataProvider {
	
	protected void activate(ComponentContext context) {
		// Ensure we're notified of all metadata related to physical Java types, in particular their initial creation
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		return new WicketAppMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "WicketApp";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = WicketAppMetadata.getJavaType(metadataIdentificationString);
		Path path = WicketAppMetadata.getPath(metadataIdentificationString);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		return physicalTypeIdentifier;
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return WicketAppMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return WicketAppMetadata.getMetadataIdentiferType();
	}
	
}
