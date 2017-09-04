package com.capestartproject.common.emppackage;

import static com.capestartproject.common.util.data.Collections.list;
import static com.capestartproject.common.util.data.functions.Options.sequenceOpt;
import static com.capestartproject.common.util.data.functions.Options.toOption;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.UnsupportedElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.util.PathSupport;
import com.capestartproject.common.util.data.Effect;
import com.capestartproject.common.util.data.Option;

/** Utility class used for media package handling. */
public final class EmployeePackageSupport {
  /** Disable construction of this utility class */
	private EmployeePackageSupport() {
  }

  private static final List NIL = java.util.Collections.EMPTY_LIST;

  /**
   * Mode used when merging media packages.
   * <p>
   * <ul>
   * <li><code>Merge</code> assigns a new identifier in case of conflicts</li>
   * <li><code>Replace</code> replaces elements in the target media package with matching identifier</li>
   * <li><code>Skip</code> skips elements from the source media package with matching identifer</li>
   * <li><code>Fail</code> fail in case of conflicting identifier</li>
   * </ul>
   */
  enum MergeMode {
    Merge, Replace, Skip, Fail
  }

  /** the logging facility provided by log4j */
	private static final Logger logger = LoggerFactory.getLogger(EmployeePackageSupport.class.getName());

  	/**
	 * Merges the contents of employee package located at <code>sourceDir</code>
	 * into the media package located at <code>targetDir</code>.
	 * <p>
	 * When choosing to move the media package element into the new place
	 * instead of copying them, the source media package folder will be removed
	 * afterwards.
	 * </p>
	 *
	 * @param dest
	 *            the target emp package directory
	 * @param src
	 *            the source emp package directory
	 * @param mode
	 *            conflict resolution strategy in case of identical element
	 *            identifier
	 * @throws EmployeePackageException
	 *             if an error occurs either accessing one of the two emp
	 *             packages or merging them
	 */
	public static EmployeePackage merge(EmployeePackage dest, EmployeePackage src, MergeMode mode)
			throws EmployeePackageException {
    try {
			for (EmployeePackageElement e : src.elements()) {
        if (dest.getElementById(e.getIdentifier()) == null)
          dest.add(e);
        else {
          if (MergeMode.Replace == mode) {
            logger.debug("Replacing element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            dest.remove(dest.getElementById(e.getIdentifier()));
            dest.add(e);
          } else if (MergeMode.Skip == mode) {
            logger.debug("Skipping element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            continue;
          } else if (MergeMode.Merge == mode) {
            logger.debug("Renaming element " + e.getIdentifier() + " while merging " + dest + " with " + src);
            e.setIdentifier(null);
            dest.add(e);
          } else if (MergeMode.Fail == mode) {
						throw new EmployeePackageException("Target employee package " + dest
								+ " already contains element with id "
                                                    + e.getIdentifier());
          }
        }
      }
    } catch (UnsupportedElementException e) {
			throw new EmployeePackageException(e);
    }
    return dest;
  }

  	/**
	 * Returns <code>true</code> if the media package contains an element with
	 * the specified identifier.
	 *
	 * @param identifier
	 *            the identifier
	 * @return <code>true</code> if the employee package contains an element
	 *         with this identifier
	 */
	public static boolean contains(String identifier, EmployeePackage ep) {
		for (EmployeePackageElement element : ep.getElements()) {
      if (element.getIdentifier().equals(identifier))
        return true;
    }
    return false;
  }

  /**
   * Creates a unique filename inside the root folder, based on the parameter <code>filename</code>.
   *
   * @param root
   *         the root folder
   * @param filename
   *         the original filename
   * @return the new and unique filename
   */
  public static File createElementFilename(File root, String filename) {
    String baseName = PathSupport.removeFileExtension(filename);
    String extension = PathSupport.getFileExtension(filename);
    int count = 1;
    StringBuffer name = null;
    File f = new File(root, filename);
    while (f.exists()) {
      name = new StringBuffer(baseName).append("-").append(count).append(".").append(extension);
      f = new File(root, name.toString());
      count++;
    }
    return f;
  }

	/** Immutable modification of a employee package. */
	public static EmployeePackage modify(EmployeePackage ep, Effect<EmployeePackage> e) {
		final EmployeePackage clone = (EmployeePackage) ep.clone();
    e.apply(clone);
    return clone;
  }

  /**
   * Immutable modification of a media package element. Attention: The returned element loses its media package
   * membership (see {@link org.opencastproject.mediapackage.AbstractMediaPackageElement#clone()})
   */
	public static <A extends EmployeePackageElement> A modify(A mpe, Effect<A> e) {
    final A clone = (A) mpe.clone();
    e.apply(clone);
    return clone;
  }

	/** Create a copy of the given employee package. */
	public static EmployeePackage copy(EmployeePackage ep) {
		return (EmployeePackage) ep.clone();
  }

	/**
	 * Update a employeepackage element of a employeepackage. Mutates
	 * <code>mp</code>.
	 */
	public static void updateElement(EmployeePackage ep, EmployeePackageElement e) {
		ep.removeElementById(e.getIdentifier());
		ep.add(e);
  }

	/**
	 * Basic sanity checking for employee packages.
	 *
	 * <pre>
	 * // media package is ok
	 * sanityCheck(mp).isNone()
	 * </pre>
	 *
	 * @return none if the media package is a healthy condition,
	 *         some([error_msgs]) otherwise
	 */
	public static Option<List<String>> sanityCheck(EmployeePackage ep) {
		final Option<List<String>> errors = sequenceOpt(list(toOption(ep.getIdentifier() != null, "no ID"),
				toOption(ep.getIdentifier() != null && isNotBlank(ep.getIdentifier().toString()), "blank ID")));
    return errors.getOrElse(NIL).size() == 0 ? Option.<List<String>>none() : errors;
  }
}
