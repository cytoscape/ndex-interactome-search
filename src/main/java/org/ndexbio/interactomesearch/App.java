package org.ndexbio.interactomesearch;

import java.io.File;
import java.io.PrintStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.log.Log;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 *
 */
public class App 
{
	

	 static final String APPLICATION_PATH = "/interactome";
	 static final String CONTEXT_ROOT = "/";
	 private static String ndexServerName;   //Ndex server host which has these interactome networks

	 private static GeneQueryService interactionService;
	 private static GeneQueryService associationService;
	 
	 private static String workingPath;  // working directory of this service.
	 private static String serviceHost;  // host name of this service
	 private static int port;    //service port.
	 
	 //connection pool to the embedded db.
	 private static JdbcConnectionPool cp;


//	 private static int resultCacheSize = 600;
	 
//	 private static final Hashtable<String, NetworkShortSummary> dbTable = new Hashtable<>();
//	 private static final Hashtable<String, NetworkShortSummary> dbTableAssociation = new Hashtable<>();
	 
	// private static Collection<AspectElement> templateStyles;
	 	
	 // task ID to status table. In sync with the geneSetSearch cache by RemovalListener
//	 private static final Hashtable<UUID, SearchStatus> statusTable = new Hashtable<>();
	 
/*	 private static RemovalListener<Set<String>, UUID> removalListener = new RemovalListener<Set<String>, UUID>() {
		 @Override
		public void onRemoval(RemovalNotification<Set<String>, UUID> removal) {
			    UUID taskId = removal.getValue();
			    App.getStatusTable().remove(taskId);
			    // remove the directory from file system.
				File resultDir =  new File(App.getWorkingPath() + "/result/" + taskId.toString());
				try {
					FileUtils.deleteDirectory(resultDir);
				} catch (IOException e) {
				  	Log.getRootLogger().warn("Failed to remove result director for " + 
					   taskId.toString() );
					e.printStackTrace();

				}
			  }
			};	 
	 
	 // gene set to taskID cache
	 private static final LoadingCache<Set<String>,UUID> geneSetSearchCache =
			 CacheBuilder.newBuilder().initialCapacity(resultCacheSize)
			 .maximumSize(resultCacheSize)
			 .removalListener(removalListener)
			 .build(
				new CacheLoader<Set<String>,UUID>() {
					@Override
					public UUID load (Set<String> geneSet) throws IOException {
						UUID taskId = UUID.nameUUIDFromBytes(geneSet.stream().
								collect(Collectors.joining(",")).getBytes());
						java.nio.file.Path path =  Paths.get(App.getWorkingPath() + "/result/" + taskId.toString());
						Files.createDirectories(path);
							  // add entry to the status table
						SearchStatus st = new SearchStatus();
						st.setStatus(SearchStatus.submitted);
						App.getStatusTable().put(taskId, st);
			
						SearchWorkerThread t = new SearchWorkerThread(geneSet, taskId, st);
						t.start();
						
						return taskId;
					}
				}
			 )		;  */
	 
	 
	  public App() {}

	  
	  //public static String getHostName() { return ndexServerName;}
	  public static GeneQueryService getInteractionService() { return interactionService;}
	  public static GeneQueryService getAssociationService() { return associationService;} 
	  public static String getWorkingPath() {return workingPath;}
	  public static String getServiceHost() {return serviceHost;}
	  public static int getPort() { return port;}
	//  public static Hashtable<String, NetworkShortSummary> getDBTable() { return dbTable;}
	/*  public static UUID getTaskIdFromCache(Set<String> queryGeneSet) throws ExecutionException {
		  return geneSetSearchCache.get(queryGeneSet);
	  } */
	  
	/*  public static Collection<AspectElement> getVisualSytleTemplate() {
		  return templateStyles;
	  } */
	  
	  public static void main( String[] args ) throws Exception
	  {
		  

			
	    try
	    {
	      run();
	    }
	    catch (Throwable t)
	    {
	      t.printStackTrace();
	    }
	  }
	  
	  public static void run() throws Exception
	  {
		System.out.println("You can use -Dndex.queryport=8285 and -Dndex.fileRepoPrefix=/opt/ndex/data/ -Dndex.host=public.ndexbio.org -Dndex.interactomedb=/opt/ndex/services/interactome + "
				+ "\n        -Dndex.interactomehost=localhost to set runtime parameters.");
		
		// read in the template network for sytles.
		// comment this out. We no longer need to apply template to networks.
	/*	try (FileInputStream s = new FileInputStream("template.cx")) {
			NiceCXNetworkReader cxreader = new NiceCXNetworkReader();
			templateStyles = cxreader.readNiceCXNetwork(s)
					.getOpaqueAspectTable().get(CyVisualPropertiesElement.ASPECT_NAME);
		} */
		
		ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLog.setLevel(Level.INFO);
		

		
		//We are configuring a RolloverFileOutputStream with file name pattern  and appending property
		RolloverFileOutputStream os = new RolloverFileOutputStream("logs/queries_yyyy_mm_dd.log", true);
		
		//We are creating a print stream based on our RolloverFileOutputStream
		PrintStream logStream = new PrintStream(os);

		//We are redirecting system out and system error to our print stream.
		System.setOut(logStream);
		System.setErr(logStream);	  		
		
		
		String portStr = System.getProperty("ndex.queryport", "8285")  ;
		String serverFileRepoPrefix = System.getProperty("ndex.fileRepoPrefix", "/opt/ndex/data/");
	    port = Integer.valueOf(portStr).intValue();
		ndexServerName = System.getProperty("ndex.host", "public.ndexbio.org");
		workingPath = System.getProperty("ndex.interactomedb", "/opt/ndex/services/interactome");
		
		
		cp = JdbcConnectionPool.create("jdbc:h2:" + workingPath + "/genedb", "sa", "sa");

		// gene query services need to be initialized before initialize the dbTable
		interactionService = new GeneQueryService(cp, "i", ndexServerName);
		associationService = new GeneQueryService(cp, "a", ndexServerName);
		
		serviceHost = System.getProperty("ndex.interactomehost", "localhost");

		//remove the old results first
		FileUtils.deleteDirectory(new File(workingPath + "/result"));
		
	    NetworkQueryManager.setDataFilePathPrefix(serverFileRepoPrefix);
	    final Server server = new Server(port);
	    
	    rootLog.info("Server started on port " + portStr  + ", with network data repo at " + serverFileRepoPrefix);

	    // Setup the basic Application "context" at "/".
	    // This is also known as the handler tree (in Jetty speak).
	    final ServletContextHandler context = new ServletContextHandler(
	      server, CONTEXT_ROOT);

	    // Setup RESTEasy's HttpServletDispatcher at "/api/*".
	    final ServletHolder restEasyServlet = new ServletHolder(
	      new HttpServletDispatcher());
	    restEasyServlet.setInitParameter("resteasy.servlet.mapping.prefix",
	      APPLICATION_PATH);
	    restEasyServlet.setInitParameter("javax.ws.rs.Application",
	      "org.ndexbio.interactomesearch.InteractomeSearchApplication");
	    context.addServlet(restEasyServlet, APPLICATION_PATH + "/*");

	    // Setup the DefaultServlet at "/".
	    final ServletHolder defaultServlet = new ServletHolder(
	      new DefaultServlet());
	    context.addServlet(defaultServlet, CONTEXT_ROOT);

	    server.start();
	    //Now we are appending a line to our log 
	  	Log.getRootLogger().info("Embedded Jetty logging started.", new Object[]{});
	    
	    System.out.println("Server started on port " + port + ", with network data repo at " + serverFileRepoPrefix);
	    server.join();
	    
	  } 
	  
	/*	private synchronized static boolean createDirIfNotExists(java.nio.file.Path path) throws IOException {
			if (Files.exists(path)) 
				return false;

			Files.createDirectories(path);
			return true;
		} */
}
