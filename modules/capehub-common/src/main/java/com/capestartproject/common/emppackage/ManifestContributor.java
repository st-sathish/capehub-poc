package com.capestartproject.common.emppackage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * To be implemented by everyone who wishes to contribute to the media package manifest.
 */
public interface ManifestContributor {

  	/**
	 * This method returns an XML serialization of the object to be stored in
	 * the employee package manifest. It should be possible to reconstruct the
	 * object from this data.
	 * <p/>
	 * For creating <em>{@link EmployeePackageElement}s</em> from a manifest,
	 * please use
	 * {@link EmployeePackageElementBuilder#elementFromManifest(org.w3c.dom.Node, EmployeePackageSerializer)}.
	 * All other objects shall provide their own implementation specific
	 * reconstruction mechanism.
	 *
	 * @param document
	 *            the parent
	 * @param serializer
	 *            the employee package serializer
	 * @return the object's xml representation
	 */
	Node toManifest(Document document, EmployeePackageSerializer serializer);

}
