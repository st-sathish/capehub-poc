package com.capestartproject.workspace.impl;

import static com.capestartproject.common.util.IoSupport.locked;
import static com.capestartproject.common.util.data.functions.Misc.chuck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import javax.management.ObjectInstance;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.security.api.TrustedHttpClient;
import com.capestartproject.common.util.FileSupport;
import com.capestartproject.common.util.IoSupport;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.PathSupport;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Monadics;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.workingfilerepository.api.PathMappable;
import com.capestartproject.workingfilerepository.api.WorkingFileRepository;
import com.capestartproject.workspace.api.Workspace;
import com.capestartproject.workspace.impl.jmx.WorkspaceBean;

/**
 * Implements a simple cache for remote URIs. Delegates methods to {@link WorkingFileRepository} wherever possible.
 * <p>
 * Note that if you are running the workspace on the same machine as the singleton working file repository, you can save
 * a lot of space if you configure both root directories onto the same volume (that is, if your file system supports
 * hard links).
 *
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
public class WorkspaceImpl implements Workspace {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);

  /** Configuration key for the workspace root directory */
	public static final String WORKSPACE_ROOTDIR_KEY = "com.capestartproject.workspace.rootdir";

  /** Workspace JMX type */
  private static final String JMX_WORKSPACE_TYPE = "Workspace";

  /** The JMX workspace bean */
  private WorkspaceBean workspaceBean = new WorkspaceBean(this);

  /** The JMX bean object instance */
  private ObjectInstance registeredMXBean;

  protected String wsRoot = null;
  protected int maxAgeInSeconds = -1;
  protected int garbageCollectionPeriodInSeconds = -1;
  protected Timer garbageFileCollector;
  protected boolean linkingEnabled = false;

  protected TrustedHttpClient trustedHttpClient;

  protected WorkingFileRepository wfr = null;
  protected String wfrRoot = null;
  protected String wfrUrl = null;

  private WorkspaceCleaner workspaceCleaner;

  public WorkspaceImpl() {
  }

  /**
   * Creates a workspace implementation which is located at the given root directory.
   * <p>
   * Note that if you are running the workspace on the same machine as the singleton working file repository, you can
   * save a lot of space if you configure both root directories onto the same volume (that is, if your file system
   * supports hard links).
   *
   * @param rootDirectory
   *          the repository root directory
   */
  public WorkspaceImpl(String rootDirectory) {
    this.wsRoot = rootDirectory;
  }

  /**
   * OSGi service activation callback.
   *
   * @param cc
   *          the OSGi component context
   */
  public void activate(ComponentContext cc) {
    if (this.wsRoot == null) {
      if (cc != null && cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR_KEY) != null) {
        // use rootDir from CONFIG
        this.wsRoot = cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR_KEY);
        logger.info("CONFIG " + WORKSPACE_ROOTDIR_KEY + ": " + this.wsRoot);
			} else if (cc != null && cc.getBundleContext().getProperty("com.capestartproject.storage.dir") != null) {
        // create rootDir by adding "workspace" to the default data directory
				this.wsRoot = PathSupport.concat(cc.getBundleContext().getProperty("com.capestartproject.storage.dir"),
                "workspace");
        logger.warn("CONFIG " + WORKSPACE_ROOTDIR_KEY + " is missing: falling back to " + this.wsRoot);
      } else {
        throw new IllegalStateException("Configuration '" + WORKSPACE_ROOTDIR_KEY + "' is missing");
      }
    }

    // Create the root directory
    File f = new File(this.wsRoot);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    // Set up the garbage file collection timer
		if (cc != null && cc.getBundleContext().getProperty("com.capestartproject.workspace.cleanup.period") != null) {
			String period = cc.getBundleContext().getProperty("com.capestartproject.workspace.cleanup.period");
      if (period != null) {
        try {
          garbageCollectionPeriodInSeconds = Integer.parseInt(period);
        } catch (NumberFormatException e) {
          logger.warn("Workspace garbage collection period can not be set to {}. Please choose a valid number "
                  + "for the 'org.opencastproject.workspace.cleanup.period' setting", period);
        }
      }
    }

    // Test whether hard linking between working file repository and workspace is possible
    if (wfr instanceof PathMappable) {
      File srcFile = new File(wfrRoot, ".linktest");
      File targetFile = new File(wsRoot, ".linktest");
      try {
        FileUtils.touch(srcFile);
      } catch (IOException e) {
        throw new IllegalStateException("The working file repository seems read-only", e);
      }
      linkingEnabled = FileSupport.supportsLinking(srcFile, targetFile);
      if (linkingEnabled)
        logger.info("Hard links between the working file repository and the workspace enabled");
      else {
        logger.warn("Hard links between the working file repository and the workspace are not possible");
        logger.warn("This will increase the overall amount of disk space used");
      }
    }

    // Activate garbage collection
		if (cc != null && cc.getBundleContext().getProperty("com.capestartproject.workspace.cleanup.max.age") != null) {
			String age = cc.getBundleContext().getProperty("com.capestartproject.workspace.cleanup.max.age");
      if (age != null) {
        try {
          maxAgeInSeconds = Integer.parseInt(age);
        } catch (NumberFormatException e) {
          logger.warn("Workspace garbage collection max age can not be set to {}. Please choose a valid number "
									+ "for the 'com.capestartproject.workspace.cleanup.max.age' setting",
							age);
        }
      }
    }

    registeredMXBean = JmxUtil.registerMXBean(workspaceBean, JMX_WORKSPACE_TYPE);

    workspaceCleaner = new WorkspaceCleaner(this, garbageCollectionPeriodInSeconds, maxAgeInSeconds);
    workspaceCleaner.schedule();
  }

  /**
   * Callback from OSGi on service deactivation.
   */
  public void deactivate() {
    JmxUtil.unregisterMXBean(registeredMXBean);
    workspaceCleaner.shutdown();
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#get(java.net.URI)
	 */
  public File get(final URI uri) throws NotFoundException, IOException {
    final String urlString = uri.toString();
    final File f = getWorkspaceFile(uri, true);

    // Does the file exist and is it up to date?
    Long workspaceFileLastModified = new Long(0); // make sure this is not null, otherwise the requested file can not be
                                                  // copied
    if (f.isFile()) {
      workspaceFileLastModified = new Long(f.lastModified());
    }

    if (wfrRoot != null && wfrUrl != null) {
      if (uri.toString().startsWith(wfrUrl)) {
        String localPath = uri.toString().substring(wfrUrl.length());
        File wfrCopy = new File(PathSupport.concat(wfrRoot, localPath));
        if (wfrCopy.isFile()) {
          // if the file exists in the workspace, but is older than the wfr copy, replace it
          if (workspaceFileLastModified < wfrCopy.lastModified()) {
            logger.debug("Replacing {} with an updated version from the file repository", f.getAbsolutePath());
            if (linkingEnabled) {
              FileUtils.deleteQuietly(f);
              FileSupport.link(wfrCopy, f);
            } else {
              FileSupport.copy(wfrCopy, f);
            }
          } else {
            logger.debug("{} is up to date", f);
          }
          logger.debug("Getting {} directly from working file repository root at {}", uri, f);
          return new File(f.getAbsolutePath());
        }
      }
    }

    String ifNoneMatch = null;
    if (f.isFile()) {
      ifNoneMatch = md5(f);
    }

    final HttpGet get = new HttpGet(urlString);
    if (ifNoneMatch != null)
      get.setHeader("If-None-Match", ifNoneMatch);

    return locked(f, new Function<File, File>() {
      @Override
      public File apply(File file) {
        InputStream in = null;
        OutputStream out = null;
        HttpResponse response = null;
        try {
          response = trustedHttpClient.execute(get);
          if (HttpServletResponse.SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
            throw new NotFoundException(uri + " does not exist");
          } else if (HttpServletResponse.SC_NOT_MODIFIED == response.getStatusLine().getStatusCode()) {
            logger.debug("{} has not been modified.", urlString);
            return file;
          } else if (HttpServletResponse.SC_ACCEPTED == response.getStatusLine().getStatusCode()) {
            logger.debug("{} is not ready, try again in one minute.", urlString);
            String token = response.getHeaders("token")[0].getValue();
            get.setParams(new BasicHttpParams().setParameter("token", token));
            Thread.sleep(60000);
            while (true) {
              response = trustedHttpClient.execute(get);
              if (HttpServletResponse.SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
                throw new NotFoundException(uri + " does not exist");
              } else if (HttpServletResponse.SC_NOT_MODIFIED == response.getStatusLine().getStatusCode()) {
                logger.debug("{} has not been modified.", urlString);
                return file;
              } else if (HttpServletResponse.SC_ACCEPTED == response.getStatusLine().getStatusCode()) {
                logger.debug("{} is not ready, try again in one minute.", urlString);
                Thread.sleep(60000);
              } else if (HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode()) {
                logger.info("Downloading {} to {}", urlString, file.getAbsolutePath());
                file.createNewFile();
                in = response.getEntity().getContent();
                out = new FileOutputStream(file);
                IOUtils.copyLarge(in, out);
                return file;
              } else {
                logger.warn("Received unexpected response status {} while trying to download from {}", response
                        .getStatusLine().getStatusCode(), urlString);
                FileUtils.deleteQuietly(file);
                return chuck(new NotFoundException("Unexpected response status "
                        + response.getStatusLine().getStatusCode()));
              }
            }
          } else if (HttpServletResponse.SC_OK == response.getStatusLine().getStatusCode()) {
            logger.info("Downloading {} to {}", urlString, file.getAbsolutePath());
            file.createNewFile();
            in = response.getEntity().getContent();
            out = new FileOutputStream(file);
            IOUtils.copyLarge(in, out);
            return file;
          } else {
            logger.warn("Received unexpected response status {} while trying to download from {}", response
                    .getStatusLine().getStatusCode(), urlString);
            FileUtils.deleteQuietly(file);
            return chuck(new NotFoundException("Unexpected response status " + response.getStatusLine().getStatusCode()));
          }
        } catch (Exception e) {
          logger.warn("Could not copy {} to {}: {}", new String[] { urlString, file.getAbsolutePath(), e.getMessage() });
          FileUtils.deleteQuietly(file);
          return chuck(new NotFoundException(e));
        } finally {
          IOUtils.closeQuietly(in);
          IOUtils.closeQuietly(out);
          trustedHttpClient.close(response);
        }
      }
    });
  }

  /**
   * Returns the md5 of a file
   *
   * @param file
   *          the source file
   * @return the md5 hash
   * @throws IOException
   *           if the file cannot be accessed
   * @throws IllegalArgumentException
   *           if <code>file</code> is <code>null</code>
   * @throws IllegalStateException
   *           if <code>file</code> does not exist or is not a regular file
   */
  protected String md5(File file) throws IOException, IllegalArgumentException, IllegalStateException {
    if (file == null)
      throw new IllegalArgumentException("File must not be null");
    if (!file.isFile())
      throw new IllegalArgumentException("File " + file.getAbsolutePath() + " can not be read");

    InputStream in = null;
    try {
      in = new FileInputStream(file);
      return DigestUtils.md5Hex(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#delete(java.net.URI)
	 */
  @Override
  public void delete(URI uri) throws NotFoundException, IOException {
    String uriPath = uri.toString();
    if (uriPath.startsWith(wfr.getBaseUri().toString())) {
      if (uriPath.indexOf(WorkingFileRepository.COLLECTION_PATH_PREFIX) > 0) {
        String[] uriElements = uriPath.split("/");
        if (uriElements.length > 2) {
          String collectionId = uriElements[uriElements.length - 2];
          String filename = uriElements[uriElements.length - 1];
          wfr.deleteFromCollection(collectionId, filename);
        }
			} else if (uriPath.indexOf(WorkingFileRepository.EMPLOYEEPACKAGE_PATH_PREFIX) > 0) {
        String[] uriElements = uriPath.split("/");
        if (uriElements.length >= 3) {
          String mediaPackageId = uriElements[uriElements.length - 3];
          String elementId = uriElements[uriElements.length - 2];
          wfr.delete(mediaPackageId, elementId);
        }
      }
    }

    // Remove the file and optionally its parent directory if empty
    File f = getWorkspaceFile(uri, false);
    if (f.isFile()) {
      synchronized (wsRoot) {
        File mpElementDir = f.getParentFile();
        FileUtils.forceDelete(f);
        if (mpElementDir.isDirectory() && mpElementDir.list().length == 0)
          FileUtils.forceDelete(mpElementDir);

        // Also delete mediapackage itself when empty
        if (mpElementDir.getParentFile().list().length == 0)
          FileUtils.forceDelete(mpElementDir.getParentFile());
      }
    }

  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#delete(java.lang.String,
	 *      java.lang.String)
	 */
  public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
    wfr.delete(mediaPackageID, mediaPackageElementID);
		File f = new File(PathSupport.concat(new String[] { wsRoot, WorkingFileRepository.EMPLOYEEPACKAGE_PATH_PREFIX,
            mediaPackageID, mediaPackageElementID }));
    File mpDirectory = f.getParentFile();
    FileUtils.deleteQuietly(f);
    if (mpDirectory.isDirectory() && mpDirectory.list().length == 0)
      FileUtils.deleteDirectory(mpDirectory);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#put(java.lang.String,
	 *      java.lang.String, java.lang.String, java.io.InputStream)
	 */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in)
          throws IOException {
    String safeFileName = PathSupport.toSafeName(fileName);
    URI uri = wfr.getURI(mediaPackageID, mediaPackageElementID, fileName);

    // Determine the target location in the workspace
    File workspaceFile = null;
    FileOutputStream out = null;
    synchronized (wsRoot) {
      workspaceFile = getWorkspaceFile(uri, true);
      FileUtils.touch(workspaceFile);
    }

    // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
    if (linkingEnabled) {
      // The WFR stores an md5 hash along with the file, so we need to use the API and not try to write (link) the file
      // there ourselves
      wfr.put(mediaPackageID, mediaPackageElementID, fileName, in);
      File workingFileRepoDirectory = new File(PathSupport.concat(new String[] { wfrRoot,
					WorkingFileRepository.EMPLOYEEPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID }));
      File workingFileRepoCopy = new File(workingFileRepoDirectory, safeFileName);
      FileSupport.link(workingFileRepoCopy, workspaceFile, true);
    } else {
      InputStream tee = null;
      try {
        out = new FileOutputStream(workspaceFile);
        tee = new TeeInputStream(in, out, true);
        wfr.put(mediaPackageID, mediaPackageElementID, fileName, tee);
      } finally {
        IOUtils.closeQuietly(tee);
        IOUtils.closeQuietly(out);
      }
    }

    return uri;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#putInCollection(java.lang.String,
	 *      java.lang.String, java.io.InputStream)
	 */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
    String safeFileName = PathSupport.toSafeName(fileName);
    URI uri = wfr.getCollectionURI(collectionId, fileName);

    // Determine the target location in the workspace
    InputStream tee = null;
    File tempFile = null;
    FileOutputStream out = null;
    try {
      synchronized (wsRoot) {
        tempFile = getWorkspaceFile(uri, true);
        FileUtils.touch(tempFile);
        out = new FileOutputStream(tempFile);
      }

      // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
      if (linkingEnabled) {
        tee = in;
        wfr.putInCollection(collectionId, fileName, tee);
        FileUtils.forceMkdir(tempFile.getParentFile());
        File workingFileRepoDirectory = new File(PathSupport.concat(new String[] { wfrRoot,
                WorkingFileRepository.COLLECTION_PATH_PREFIX, collectionId }));
        File workingFileRepoCopy = new File(workingFileRepoDirectory, safeFileName);
        FileSupport.link(workingFileRepoCopy, tempFile, true);
      } else {
        tee = new TeeInputStream(in, out, true);
        wfr.putInCollection(collectionId, fileName, tee);
      }
    } catch (IOException e) {
      FileUtils.deleteQuietly(tempFile);
      throw e;
    } finally {
      IoSupport.closeQuietly(tee);
      IoSupport.closeQuietly(out);
    }
    return uri;
  }

  public void setRepository(WorkingFileRepository repo) {
    this.wfr = repo;
    if (repo instanceof PathMappable) {
      this.wfrRoot = ((PathMappable) repo).getPathPrefix();
      this.wfrUrl = ((PathMappable) repo).getUrlPrefix();
      logger.info("Mapping workspace to working file repository using {}", wfrRoot);
    }
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getURI(java.lang.String,
	 *      java.lang.String)
	 */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return wfr.getURI(mediaPackageID, mediaPackageElementID);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getURI(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
  public URI getURI(String mediaPackageID, String mediaPackageElementID, String filename) {
    return wfr.getURI(mediaPackageID, mediaPackageElementID, filename);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getCollectionURI(java.lang.String,
	 *      java.lang.String)
	 */
  @Override
  public URI getCollectionURI(String collectionID, String fileName) {
    return wfr.getCollectionURI(collectionID, fileName);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#copyTo(java.net.URI,
	 *      java.lang.String, java.lang.String, java.lang.String)
	 */
  public URI copyTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException {
    String path = collectionURI.toString();
    String filename = FilenameUtils.getName(path);
    String collection = getCollection(collectionURI);

    // Copy the local file
    File original = getWorkspaceFile(collectionURI, false);
    if (original.isFile()) {
      URI copyURI = wfr.getURI(toMediaPackage, toMediaPackageElement, filename);
      File copy = getWorkspaceFile(copyURI, true);
      FileUtils.forceMkdir(copy.getParentFile());
      FileSupport.link(original, copy);
    }

    // Tell working file repository
    return wfr.copyTo(collection, filename, toMediaPackage, toMediaPackageElement, toFileName);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#moveTo(java.net.URI,
	 *      java.lang.String, java.lang.String, java.lang.String)
	 */
  @Override
  public URI moveTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException {
    String path = collectionURI.toString();
    String filename = FilenameUtils.getName(path);
    String collection = getCollection(collectionURI);

    logger.debug("Moving {} from {} to {}/{}", new String[] { filename, collection, toMediaPackage,
            toMediaPackageElement });

    // Move the local file
    File original = getWorkspaceFile(collectionURI, false);
    if (original.isFile()) {
      URI copyURI = wfr.getURI(toMediaPackage, toMediaPackageElement, toFileName);
      File copy = getWorkspaceFile(copyURI, true);
      FileUtils.forceMkdir(copy.getParentFile());
      FileUtils.deleteQuietly(copy);
      FileUtils.moveFile(original, copy);
    }

    // Tell working file repository
    return wfr.moveTo(collection, filename, toMediaPackage, toMediaPackageElement, toFileName);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getCollectionContents(java.lang.String)
	 */
  @Override
  public URI[] getCollectionContents(String collectionId) throws NotFoundException {
    return wfr.getCollectionContents(collectionId);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#deleteFromCollection(java.lang.String,
	 *      java.lang.String)
	 */
  @Override
  public void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
    try {
      wfr.deleteFromCollection(collectionId, fileName);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e);
    }
    File f = new File(PathSupport.concat(new String[] { wsRoot, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            collectionId, PathSupport.toSafeName(fileName) }));
    File collectionDir = f.getParentFile();
    FileUtils.deleteQuietly(f);
    if (collectionDir.isDirectory() && collectionDir.list().length == 0)
      FileUtils.deleteDirectory(collectionDir);
  }

  /**
   * Transforms a URI into a workspace File. If the file comes from the working file repository, the path in the
   * workspace mirrors that of the repository. If the file comes from another source, directories are created for each
   * segment of the URL. Sub-directories may be created as needed.
   *
   * @param uri
   *          the uri
   * @param createDirectories
   *          <code>true</code> to have subdirectories created
   * @return the local file representation
   */
  protected File getWorkspaceFile(URI uri, boolean createDirectories) {
    String uriString = uri.toString();
    String wfrPrefix = wfr.getBaseUri().toString();
    String serverPath = FilenameUtils.getPath(uriString);
    if (uriString.startsWith(wfrPrefix)) {
      serverPath = serverPath.substring(wfrPrefix.length());
    } else {
      serverPath = serverPath.replaceAll(":/*", "_");
    }
    String wsDirectoryPath = PathSupport.concat(wsRoot, serverPath);
    File wsDirectory = new File(wsDirectoryPath);
    if (createDirectories)
      wsDirectory.mkdirs();

    String safeFileName = PathSupport.toSafeName(FilenameUtils.getName(uriString));
    return new File(wsDirectory, safeFileName);
  }

  /**
   * Returns the working file repository collection.
   * <p>
   *
   * <pre>
   * http://localhost:8080/files/collection/&lt;collection&gt;/ -> &lt;collection&gt;
   * </pre>
   *
   * @param uri
   *          the working file repository collection uri
   * @return the collection name
   */
  private String getCollection(URI uri) {
    String path = uri.toString();
    if (path.indexOf(WorkingFileRepository.COLLECTION_PATH_PREFIX) < 0)
      throw new IllegalArgumentException(uri + " must point to a working file repository collection");

    String collection = FilenameUtils.getPath(path);
    if (collection.endsWith("/"))
      collection = collection.substring(0, collection.length() - 1);
    collection = collection.substring(collection.lastIndexOf("/"));
    collection = collection.substring(collection.lastIndexOf("/") + 1, collection.length());
    return collection;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getTotalSpace()
	 */
  @Override
  public Option<Long> getTotalSpace() {
    return Option.some(new File(wsRoot).getTotalSpace());
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getUsableSpace()
	 */
  @Override
  public Option<Long> getUsableSpace() {
    return Option.some(new File(wsRoot).getUsableSpace());
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getUsedSpace()
	 */
  @Override
  public Option<Long> getUsedSpace() {
    return Option.some(FileUtils.sizeOfDirectory(new File(wsRoot)));
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workspace.api.Workspace#getBaseUri()
	 */
  @Override
  public URI getBaseUri() {
    return wfr.getBaseUri();
  }

  @Override
  public void cleanup(final Option<Integer> maxAge) {
    final File rootDirecotry = new File(wsRoot);

    logger.info("Starting cleanup of workspace at {}", rootDirecotry);
    Collection<File> files = FileUtils.listFiles(rootDirecotry, null, true);
    List<File> filesToDelete = Monadics.mlist(files).filter(new Function<File, Boolean>() {
      @Override
      public Boolean apply(File file) {
        if (file.isDirectory())
          return false;

				File mediapackageDirectory = new File(rootDirecotry, WorkingFileRepository.EMPLOYEEPACKAGE_PATH_PREFIX);
        File collectionDirectory = new File(rootDirecotry, WorkingFileRepository.COLLECTION_PATH_PREFIX);

        if (file.getAbsolutePath().startsWith(mediapackageDirectory.getAbsolutePath())
                || file.getAbsolutePath().startsWith(collectionDirectory.getAbsolutePath()))
          return false;

        boolean maxAgeReached = false;
        if (maxAge.isSome()) {
          long fileAgeInSeconds = (new Date().getTime() - file.lastModified()) / 1000;
          maxAgeReached = fileAgeInSeconds > maxAgeInSeconds;
        }

        return maxAgeReached;
      }
    }).value();

    for (File file : filesToDelete) {
      logger.info("Workspace cleanup: Deleting {}", file);
      FileSupport.delete(file);
      FileSupport.deleteHierarchyIfEmpty(rootDirecotry, file.getParentFile());
    }
    logger.info("Finished cleanup of workspace!");
  }
}
