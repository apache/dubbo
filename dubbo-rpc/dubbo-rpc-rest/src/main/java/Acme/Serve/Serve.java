// Serve - minimal Java servlet container class
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
//

// All enhancements Copyright (C)1998-2010 by Dmitriy Rogatkin
// This version is compatible with JSDK 2.5
// http://tjws.sourceforge.net
// $Id: Serve.java,v 1.194 2009/12/31 05:02:13 dmitriy Exp $

package Acme.Serve;

import Acme.Utils;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// TODO Added this class for now since looks like there's a bug in resteasy...

/// Minimal Java servlet container class.
// <P>
// This class implements a very small embeddable servlet container.
// It runs Servlets compatible with the API used by Sun's 
// <A HREF="http://docs.sun.com/app/docs/doc/819-3653">Java System Application </A> server.
// Servlet API can be found <A HREF="http://java.sun.com/products/servlet/">here</A>.
// It comes with default Servlets which provide the usual
// httpd services, returning files and directory listings.
// <P>
// This is not in any sense a competitor for Java System Application server.
// Java System Application server is a full-fledged HTTP server and more.
// Acme.Serve is tiny, about 5000 lines, and provides only the
// functionality necessary to deliver an Applet's .class files
// and then start up a Servlet talking to the Applet.
// They are both written in Java, they are both web servers, and
// they both implement the Servlet API; other than that they couldn't
// be more different.
// <P>
// This is actually the second HTTP server I've written.
// The other one is called
// <A HREF="http://www.acme.com/software/thttpd/">thttpd</A>,
// it's written in C, and is also pretty small although much more
// featureful than this.
// <P>
// Other Java HTTP servers:
// <UL>
// <LI> The above-mentioned <A
// HREF="http://docs.sun.com/app/docs/doc/819-3653">JavaServer</A>.
// <LI> W3C's <A HREF="http://www.w3.org/pub/WWW/Jigsaw/">Jigsaw</A>.
// <LI> David Wilkinson's <A
// HREF="http://www.netlink.co.uk/users/cascade/http/">Cascade</A>.
// <LI> Yahoo's <A
// HREF="http://www.yahoo.com/Computers_and_Internet/Software/Internet/World_Wide_Web/Servers/Java/">list
// of Java web servers</A>.
// </UL>
// <P>
// A <A HREF="http://www.byte.com/art/9706/sec8/art1.htm">June 1997 BYTE
// magazine article</A> mentioning this server.<BR>
// A <A HREF="http://www.byte.com/art/9712/sec6/art7.htm">December 1997 BYTE
// magazine article</A> giving it an Editor's Choice Award of Distinction.<BR>
// <A HREF="/resources/classes/Acme/Serve/Serve.java">Fetch the
// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.http.HttpServlet
// @see FileServlet
// @see CgiServlet
// <h3>Post notes</h3>
// Currently the server 3 more times complex and can compete with
// most popular app and web servers used for deploying of web
// Java applications.

// Inheritance can extend usage of this server
public class Serve implements ServletContext, Serializable
{

   public static final String ARG_PORT = "port";

   public static final String ARG_THROTTLES = "throttles";

   public static final String ARG_SERVLETS = "servlets";

   public static final String ARG_REALMS = "realms";

   public static final String ARG_ALIASES = "aliases";

   public static final String ARG_BINDADDRESS = "bind-address";

   public static final String ARG_BACKLOG = "backlog";

   public static final String ARG_CGI_PATH = "cgi-path";

   public static final String ARG_ERR = "error-stream";

   public static final String ARG_OUT = "out-stream";

   public static final String ARG_SESSION_TIMEOUT = "session-timeout";

   public static final String ARG_LOG_DIR = "log-dir";

   public static final String ARG_LOG_OPTIONS = "log-options";

   public static final String ARG_NOHUP = "nohup";

   public static final String ARG_JSP = "JSP";

   public static final String ARG_WAR = "war-deployer";

   public static final String ARG_KEEPALIVE = "keep-alive";

   public static final String DEF_LOGENCODING = "tjws.serve.log.encoding";

   public static final String ARG_KEEPALIVE_TIMEOUT = "timeout-keep-alive";

   public static final String ARG_MAX_CONN_USE = "max-alive-conn-use";

   public static final String ARG_SESSION_PERSIST = "sssn-persistance";

   public static final String ARG_MAX_ACTIVE_SESSIONS = "max-active-sessions";

   public static final String ARG_ACCESS_LOG_FMT = "access-log-format";

   public static final String ARG_ACCEPTOR_CLASS = "acceptorImpl";

   public static final String ARG_WORK_DIRECTORY = "workdirectory";

   public static final String ARG_SESSION_SEED = "SessionSeed";

   public static final String ARG_THREAD_POOL_SIZE = Utils.ThreadPool.MAXNOTHREAD;


   protected static final int DEF_SESSION_TIMEOUT = 30; // in minutes

   protected static final int DEF_MIN_ACT_SESS = 10;

   protected static final int DESTROY_TIME_SEC = 15;

   protected static final int HTTP_MAX_HDR_LEN = 1024 * 1024 * 10;

   public static final int DEF_PORT = 8080;

   public static final String BGCOLOR = "BGCOLOR=\"#D1E9FE\"";

   /**
    * max number of alive connections default value
    */
   protected static final int DEF_MAX_CONN_USE = 100;

   public static final String UTF8 = "UTF-8"; // default encoding

   protected String hostName;

   private transient PrintStream logStream;

   private boolean useAccLog;

   private boolean keepAlive;

   private int timeoutKeepAlive;

   private int maxAliveConnUse;

   private boolean showUserAgent;

   private boolean showReferer;

   protected String keepAliveHdrParams;

   protected transient PathTreeDictionary registry;

   protected transient PathTreeDictionary realms;

   protected transient PathTreeDictionary mappingtable;

   private Hashtable attributes;

   protected transient KeepAliveCleaner keepAliveCleaner;

   protected transient ThreadGroup serverThreads;

   protected transient Utils.ThreadPool threadPool;

   protected transient Constructor gzipInStreamConstr;

   // for sessions
   private byte[] uniqer = new byte[20]; // TODO consider configurable strength

   private SecureRandom srandom;

   protected HttpSessionContextImpl sessions;

   protected int expiredIn;

   public Map arguments;

   public Properties mime;

   protected List<ServeConnection> connections = new ArrayList<ServeConnection>();

   // / Constructor.
   public Serve(Map arguments, PrintStream logStream)
   {
      this.arguments = arguments;
      this.logStream = logStream;
      registry = new PathTreeDictionary();
      realms = new PathTreeDictionary();
      attributes = new Hashtable();
      serverThreads = new ThreadGroup("TJWS threads");
      Properties props = new Properties();
      props.putAll(arguments);
      // TODO do not create thread pool unless requested
      threadPool = new Utils.ThreadPool(props, new Utils.ThreadFactory()
      {
         public Thread create(Runnable runnable)
         {
            Thread result = new Thread(serverThreads, runnable);
            result.setDaemon(true);
            return result;
         }
      });
      setAccessLogged();
       // modified by lishen
      keepAlive = arguments.get(ARG_KEEPALIVE) == null || ((Boolean) arguments.get(ARG_KEEPALIVE)).booleanValue();
//       keepAlive = false;
      System.out.println("KEEPALIVE!: " + keepAlive);
      int timeoutKeepAliveSec;
      try
      {
         timeoutKeepAliveSec = Integer.parseInt((String) arguments.get(ARG_KEEPALIVE_TIMEOUT));
      }
      catch (Exception ex)
      {
         timeoutKeepAliveSec = 30;
      }
      timeoutKeepAlive = timeoutKeepAliveSec * 1000;
      try
      {
         maxAliveConnUse = Integer.parseInt((String) arguments.get(ARG_MAX_CONN_USE));
      }
      catch (Exception ex)
      {
         maxAliveConnUse = DEF_MAX_CONN_USE;
      }
      keepAliveHdrParams = "timeout=" + timeoutKeepAliveSec + ", max=" + maxAliveConnUse;

      expiredIn = arguments.get(ARG_SESSION_TIMEOUT) != null ? ((Integer) arguments.get(ARG_SESSION_TIMEOUT))
              .intValue() : DEF_SESSION_TIMEOUT;
      srandom = new SecureRandom((arguments.get(ARG_SESSION_SEED) == null ? "TJWS" + new Date() : (String) arguments.get(ARG_SESSION_SEED)).getBytes());
      try
      {
         gzipInStreamConstr = Class.forName("java.util.zip.GZIPInputStream").getConstructor(new Class[]{InputStream.class});
      }
      catch (ClassNotFoundException cne)
      {

      }
      catch (NoSuchMethodException nsm)
      {

      }
      initMime();
   }

   /**
    * Default constructor to create TJWS as a bean
    */
   public Serve()
   {
      this(new HashMap(), System.err);
   }

   protected void setAccessLogged()
   {
      String logflags = (String) arguments.get(ARG_LOG_OPTIONS);
      if (logflags != null)
      {
         useAccLog = true;
         showUserAgent = logflags.indexOf('A') >= 0;
         showReferer = logflags.indexOf('R') >= 0;
      }
   }

   protected boolean isAccessLogged()
   {
      return useAccLog;
   }

   protected boolean isShowReferer()
   {
      return showReferer;
   }

   protected boolean isShowUserAgent()
   {
      return showUserAgent;
   }

   protected boolean isKeepAlive()
   {
      return keepAlive;
   }

   protected int getKeepAliveDuration()
   {
      return timeoutKeepAlive;
   }

   protected String getKeepAliveParamStr()
   {
      return keepAliveHdrParams;
   }

   protected int getMaxTimesConnectionUse()
   {
      return maxAliveConnUse;
   }

   protected void initMime()
   {
      mime = new Properties();
      try
      {
         mime.load(getClass().getClassLoader().getResourceAsStream("Acme/Resource/mime.properties"));
      }
      catch (Exception ex)
      {
         log("MIME map can't be loaded:" + ex);
      }
   }

   // / Register a Servlet by class name. Registration consists of a URL
   // pattern, which can contain wildcards, and the class name of the Servlet
   // to launch when a matching URL comes in. Patterns are checked for
   // matches in the order they were added, and only the first match is run.
   public void addServlet(String urlPat, String className)
   {
      addServlet(urlPat, className, (Hashtable) null);
   }

   /**
    * Adds a servlet to run
    *
    * @param urlPat     servlet invoker URL pattern
    * @param className  servlet class name
    * @param initParams servlet init parameters
    */
   public void addServlet(String urlPat, String className, Hashtable initParams)
   {
      // Check if we're allowed to make one of these.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         int i = className.lastIndexOf('.');
         if (i > 0)
         {
            security.checkPackageAccess(className.substring(0, i));
            security.checkPackageDefinition(className.substring(0, i));
         }
      }

      // Make a new one.
      try
      {
         addServlet(urlPat, (Servlet) Class.forName(className).newInstance(), initParams);
      }
      catch (ClassNotFoundException e)
      {
         log("Class not found: " + className);
         ClassLoader cl = getClass().getClassLoader();
         log("Class loader: " + cl);
         if (cl instanceof java.net.URLClassLoader)
            log("CP: " + java.util.Arrays.asList(((java.net.URLClassLoader) cl).getURLs()));
      }
      catch (ClassCastException e)
      {
         log("Servlet class doesn't implement javax.servlet.Servlet: " + e.getMessage());
      }
      catch (InstantiationException e)
      {
         log("Can't instantiate servlet: " + e.getMessage());
      }
      catch (IllegalAccessException e)
      {
         log("Illegal class access: " + e.getMessage());
      }
      catch (Exception e)
      {
         log("Unexpected problem of servlet creation: " + e, e);
      }
   }

   /**
    * Register a Servlet. Registration consists of a URL pattern,
    * which can contain wildcards, and the Servlet to
    * launch when a matching URL comes in. Patterns are checked for
    * matches in the order they were added, and only the first match is run.
    *
    * @param urlPat  servlet invoker URL pattern
    * @param servlet already instantiated servlet but init
    */
   public void addServlet(String urlPat, Servlet servlet)
   {
      addServlet(urlPat, servlet, (Hashtable) null);
   }

   /**
    * Register a Servlet
    *
    * @param urlPat
    * @param servlet
    * @param initParams
    */
   public synchronized void addServlet(String urlPat, Servlet servlet, Hashtable initParams)
   {
      try
      {
         if (getServlet(urlPat) != null)
            log("Servlet overriden by " + servlet + ", for path:" + urlPat);
         servlet.init(new ServeConfig((ServletContext) this, initParams, urlPat));
         registry.put(urlPat, servlet);
      }
      catch (ServletException e)
      { //
         // it handles UnavailableException as well without an attempt to re-adding
         log("Problem initializing servlet, it won't be used: " + e);
      }
   }

   public Servlet unloadServlet(Servlet servlet)
   {
      Servlet result = null;
      synchronized (registry)
      {
         result = (Servlet) registry.remove(servlet)[0];
      }
      return result;
   }

   public synchronized void unloadServlet(String urlPat)
   {
      Servlet servlet = (Servlet) registry.remove(urlPat)[0];
      if (servlet != null)
         servlet.destroy(); // sessions associated with it have to be invalidated to free up any the servlet specific object
      // TODO decide if UnavailableException should be thrown at access
   }

   // / Register a standard set of Servlets. These will return
   // files or directory listings, and run CGI programs, much like a
   // standard HTTP server.
   // <P>
   // Because of the pattern checking order, this should be called
   // <B>after</B> you've added any custom Servlets.
   // <P>
   // The current set of default servlet mappings:
   // <UL>
   // <LI> If enabled, *.cgi goes to CgiServlet, and gets run as a CGI program.
   // <LI> * goes to FileServlet, and gets served up as a file or directory.
   // </UL>
   // @param cgi whether to run CGI programs
   // TODO: provide user specified CGI directory
   public void addDefaultServlets(String cgi)
   {
      try
      {
         addDefaultServlets(cgi, null);
      }
      catch (IOException ioe)
      { /* ignore, makes sense only for throtles */
      }
   }

   /**
    * Register a standard set of Servlets, with optional throttles. These will return files or directory listings, and run CGI programs, much like a standard
    * HTTP server.
    * <p/>
    * Because of the pattern checking order, this should be called <B>after</B> you've added any custom Servlets.
    * <p/>
    * The current set of default servlet mappings:
    * <UL>
    * <LI> If enabled, *.cgi goes to CgiServlet, and gets run as a CGI program.
    * <LI> * goes to FileServlet, and gets served up as a file or directory.
    * </UL>
    *
    * @param cgi       whether to run CGI programs
    * @param throttles filename to read FileServlet throttle settings from, can be null
    * @throws java.io.IOException
    */
   public void addDefaultServlets(String cgi, String throttles) throws IOException
   {
      // TODO: provide user specified CGI directory
      if (cgi != null)
      {
         if (getServlet("/" + cgi) == null)
            addServlet("/" + cgi, new Acme.Serve.CgiServlet());
         else
            log("Servlet for path '/" + cgi + "' already defined and no default will be used.");
      }
      if (getServlet("/") == null)
         if (throttles != null)
            addServlet("/", new Acme.Serve.FileServlet(throttles, null));
         else
            addServlet("/", new Acme.Serve.FileServlet());
      else
         log("Servlet for path '/' already defined and no default will be used.");
   }

   protected void addWarDeployer(String deployerFactory, String throttles)
   {
      if (deployerFactory == null) // try to use def
         deployerFactory = "rogatkin.web.WarRoller";
      try
      {
         WarDeployer wd = (WarDeployer) Class.forName(deployerFactory).newInstance();
         wd.deploy(this);
      }
      catch (ClassNotFoundException cnf)
      {
         log("Problem initializing war deployer: " + cnf);
      }
      catch (Exception e)
      {
         log("Problem war(s) deployment", e);
      }
   }

   protected File getPersistentFile()
   {
      if (arguments.get(ARG_SESSION_PERSIST) == null || (Boolean) arguments.get(ARG_SESSION_PERSIST) == Boolean.FALSE)
         return null;
      String workPath = (String) arguments.get(ARG_WORK_DIRECTORY);
      if (workPath == null)
         workPath = ".";
      return new File(workPath, hostName + '-'
              + (arguments.get(ARG_PORT) == null ? String.valueOf(DEF_PORT) : arguments.get(ARG_PORT))
              + "-session.obj");
   }

   // Run the server. Returns only on errors.
   transient boolean running = true;

   protected transient Acceptor acceptor;

   protected transient Thread ssclThread;

   protected transient boolean initialized;

   protected CountDownLatch shutdownLatch;
   protected Thread backgroundThread;

   public void runInBackground()
   {
      shutdownLatch = new CountDownLatch(1);
      try
      {
         init();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      backgroundThread = new Thread()
      {
         public void run()
         {
            try
            {
               serve();
            }
            finally
            {
               shutdownLatch.countDown();
            }
         }
      };
      backgroundThread.start();

   }

   public void stopBackground()
   {
      try
      {
         notifyStop();
      }
      catch (Throwable ignored)
      {
      }
      try
      {
         if (shutdownLatch.await(100, TimeUnit.MILLISECONDS) == false)
         {
            try
            {
               backgroundThread.interrupt();
            }
            catch (Exception ignored)
            {
            }
         }
         shutdownLatch.await(1, TimeUnit.SECONDS);
      }
      catch (InterruptedException e)
      {
      }
      synchronized(connections)
      {
         for (ServeConnection conn : connections)
         {
            conn.closeSocket();
         }
         connections.clear();
      }
   }

   /**
    * Launches the server
    * It doesn't exist until server runs, so start it in a dedicated thread.
    *
    * @return 0 if the server successfully terminated, 1 if it can't be started and -1 if it
    *         was terminated during some errors
    */
   public int serve()
   {
      try
      {
         while (running)
         {
            try
            {
               Socket socket = acceptor.accept();
               // TODO consider to use ServeConnection object pool
               if (keepAliveCleaner != null) // we need to add regardless of keep alive
                  keepAliveCleaner.addConnection(new ServeConnection(socket, this));
               else
                  new ServeConnection(socket, this); // TODO consider req/resp objects pooling
            }
            catch (IOException e)
            {
               log("Accept: " + e);
            }
            catch (SecurityException se)
            {
               log("Illegal access: " + se);
            }
            catch (IllegalStateException is)
            {
               log("Illegal state: " + is);
            }
         }
      }
      catch (Throwable t)
      {
         log("Unhandled exception: " + t + ", server is terminating.", t);
         if (t instanceof ThreadDeath)
            throw (Error) t;
         return -1;
      }
      finally
      {
         try
         {
            if (acceptor != null)
               acceptor.destroy();
         }
         catch (IOException e)
         {
         }
      }
      return 0;
   }

   public void init() throws IOException
   {
      shutdownLatch = new CountDownLatch(1);
      acceptor = createAcceptor();

      if (expiredIn > 0)
      {
         ssclThread = new Thread(serverThreads, new Runnable()
         {
            public void run()
            {
               while (running)
               {
                  try
                  {
                     Thread.sleep(expiredIn * 60 * 1000);
                  }
                  catch (InterruptedException ie)
                  {
                     if (running == false)
                        break;
                  }
                  Enumeration e = sessions.keys();
                  while (e.hasMoreElements())
                  {
                     Object sid = e.nextElement();
                     if (sid != null)
                     {
                        AcmeSession as = (AcmeSession) sessions.get(sid);
                        if (as != null && (as.checkExpired() || !as.isValid()))
                        { // log("sesion
                           as = (AcmeSession) sessions.remove(sid);
                           if (as != null && as.isValid())
                              try
                              {
                                 as.invalidate();
                              }
                              catch (IllegalStateException ise)
                              {

                              }
                        }
                     }
                  }
               }
            }
         }, "Session cleaner");
         ssclThread.setPriority(Thread.MIN_PRIORITY);
         // ssclThread.setDaemon(true);
         ssclThread.start();
      } // else
      // expiredIn = -expiredIn;
      if (isKeepAlive())
      {
         keepAliveCleaner = new KeepAliveCleaner();
         keepAliveCleaner.start();
      }
      File fsessions = getPersistentFile();
      if (fsessions != null && fsessions.exists())
      {
         BufferedReader br = null;
         try
         {
            br = new BufferedReader(new FileReader(fsessions));
            sessions = HttpSessionContextImpl.restore(br, Math.abs(expiredIn) * 60, this);
         }
         catch (IOException ioe)
         {
            log("Problem in restoring sessions.", ioe);
         }
         catch (Exception e)
         {
            log("Unexpected problem in restoring sessions.", e);
         }
         finally
         {
            if (br != null)
               try
               {
                  br.close();
               }
               catch (IOException ioe)
               {
               }
         }
      }
      if (sessions == null)
         sessions = new HttpSessionContextImpl();
      // TODO: display address as name and as ip
      System.out.println("[" + new Date() + "] TJWS httpd " + hostName + " - " + acceptor + " is listening.");
   }

   /**
    * Tells the server to stop
    *
    * @throws java.io.IOException
    */
   public void notifyStop() throws IOException
   {
      running = false;
      acceptor.destroy();
      acceptor = null;
      if (ssclThread != null)
         ssclThread.interrupt();
   }

   public static interface Acceptor
   {
      public void init(Map inProperties, Map outProperties) throws IOException;

      public Socket accept() throws IOException;

      public void destroy() throws IOException;
   }

   protected Acceptor createAcceptor() throws IOException
   {
      String acceptorClass = (String) arguments.get(ARG_ACCEPTOR_CLASS);
      if (acceptorClass == null)
         acceptorClass = "Acme.Serve.SimpleAcceptor";
      // assured defaulting here
      try
      {
         acceptor = (Acceptor) Class.forName(acceptorClass).newInstance();
      }
      catch (InstantiationException e)
      {
         log("Couldn't instantiate Acceptor, the Server is inoperable", e);
      }
      catch (IllegalAccessException e)
      {
         Constructor c;
         try
         {
            c = Class.forName(acceptorClass).getDeclaredConstructor(Utils.EMPTY_CLASSES);
            c.setAccessible(true);
            acceptor = (Acceptor) c.newInstance(Utils.EMPTY_OBJECTS);
         }
         catch (Exception e1)
         {
            log("Acceptor is not accessable or can't be instantiated, the Server is inoperable", e);
         }
      }
      catch (ClassNotFoundException e)
      {
         log("Acceptor class not found, the Server is inoperable", e);
      }
      Map acceptorProperties = new Properties();
      acceptor.init(arguments, acceptorProperties);
      hostName = (String) acceptorProperties.get(ARG_BINDADDRESS);
      return acceptor;
   }

   // Methods from ServletContext.

   // / Gets a servlet by name.
   // @param name the servlet name
   // @return null if the servlet does not exist

   public Servlet getServlet(String name)
   {
      try
      {
         return (Servlet) registry.get(name)[0];
      }
      catch (NullPointerException npe)
      {
         return null;
      }
   }

   // / Enumerates the servlets in this context (server). Only servlets that
   // are accesible will be returned. This enumeration always includes the
   // servlet itself.
   public Enumeration getServlets()
   {
      return registry.elements();
   }

   // / Enumerates the names of the servlets in this context (server). Only
   // servlets that are accesible will be returned. This enumeration always
   // includes the servlet itself.
   public Enumeration getServletNames()
   {
      return registry.keys();
   }

   // / Destroys all currently-loaded servlets.
   public synchronized void destroyAllServlets()
   {
      //log("Entering destroyAllServlets()", new Exception("Entering destroyAllServlets()"));
      // serialize sessions

      // invalidate all sessions
      // TODO consider merging two pieces below, generally if session is stored,
      // it shouldn't be invalidated
      File sf = getPersistentFile();
      if (sf != null && sessions != null)
      {
         Writer w = null;
         try
         {
            w = new FileWriter(sf);
            sessions.save(w);
            log("Sessions stored.");
         }
         catch (IOException ioe)
         {
            log("IO problem in storing sessions " + ioe);
         }
         catch (Throwable t)
         {
            log("Problem in storing sessions " + t);
         }
         finally
         {
            try
            {
               w.close();
            }
            catch (Exception e)
            {
            }
         }

         Enumeration e = sessions.keys();
         while (e.hasMoreElements())
         {
            Object sid = e.nextElement();
            if (sid != null)
            {
               AcmeSession as = (AcmeSession) sessions.get(sid);
               if (as != null)
               {
                  as = (AcmeSession) sessions.remove(sid);
                  if (as != null && as.isValid())
                     try
                     {
                        as.invalidate();
                     }
                     catch (IllegalStateException ise)
                     {

                     }
               }
            }
         }
      }
      // destroy servlets
      final Enumeration en = registry.elements();
      Runnable servletDestroyer = new Runnable()
      {
         public void run()
         {
            ((Servlet) en.nextElement()).destroy();
         }
      };
      int dhc = 0;
      while (en.hasMoreElements())
      {
         Thread destroyThread = new Thread(servletDestroyer, "Destroy");
         destroyThread.setDaemon(true);
         destroyThread.start();
         try
         {
            destroyThread.join(DESTROY_TIME_SEC * 1000);
         }
         catch (InterruptedException e)
         {
         }
         if (destroyThread.isAlive())
         {
            log("Destroy thread didn't terminate in " + DESTROY_TIME_SEC);
            destroyThread.setName("Destroy too long " + (dhc++)); // let it running with different name
            //destroyThread.stop();
         }
      }
      // clean access tree
      registry = new PathTreeDictionary();
   }

   protected void setMappingTable(PathTreeDictionary mappingtable)
   {
      this.mappingtable = mappingtable;
   }

   protected void setRealms(PathTreeDictionary realms)
   {
      this.realms = realms;
   }

   AcmeSession getSession(String id)
   {
      return (AcmeSession) sessions.get(id);
   }

   HttpSession createSession()
   {
      Integer ms = (Integer) this.arguments.get(ARG_MAX_ACTIVE_SESSIONS);
      if (ms != null && ms.intValue() < sessions.size())
         return null;
      HttpSession result = new AcmeSession(generateSessionId(), Math.abs(expiredIn) * 60, this, sessions);
      synchronized (sessions)
      {
         sessions.put(result.getId(), result);
      }
      return result;
   }

   void removeSession(String id)
   {
      synchronized (sessions)
      {
         sessions.remove(id);
      }
   }

   // / Write information to the servlet log.
   // @param message the message to log
   public void log(String message)
   {
      Date date = new Date(System.currentTimeMillis());
      logStream.println("[" + date.toString() + "] " + message);
   }

   public void log(String message, Throwable throwable)
   {
      if (throwable != null)
      {
         StringWriter sw;
         PrintWriter pw = new PrintWriter(sw = new StringWriter());
         throwable.printStackTrace(pw);
         // printCauses(throwable, pw);
         message = message + '\n' + sw;
      }
      log(message);
   }

   // protected void printCauses(Throwable throwable, PrintWriter printWriter) {
   // try {
   // throwable = throwable instanceof ServletException ? ((ServletException) throwable).getRootCause()
   // : (Throwable) throwable.getClass().getMethod("getCause", new Class[] {}).invoke(throwable,
   // new Object[] {});
   // if (throwable != null) {
   // printWriter.write("Caused by:\n");
   // throwable.printStackTrace(printWriter);
   // printCauses(throwable, printWriter);
   // }
   // } catch (Exception e) {
   // }
   // }

   // / Write a stack trace to the servlet log.
   // @param exception where to get the stack trace
   // @param message the message to log

   public void log(Exception exception, String message)
   {
      log(message, exception);
   }

   // / Applies alias rules to the specified virtual path and returns the
   // corresponding real path. It returns null if the translation
   // cannot be performed.
   // @param path the path to be translated
   public String getRealPath(String path)
   {
      // try {
      // path = new String(path.getBytes("ISO-8859-1"), UTF8);
      // } catch (Exception ee) { // no encoding
      // }
      // System.err.print("[" + path + "]->[");
      if (mappingtable != null)
      {
         // try find first sub-path
         Object[] os = mappingtable.get(path);
         // System.err.println("Searching for path: "+path+" found: "+os[0]);
         if (os[0] == null)
            return null;
         int slpos = ((Integer) os[1]).intValue();
         int pl = path.length();
         if (slpos > 0)
         {
            if (path.length() > slpos)
               path = path.substring(slpos + 1);
            else
               path = "";
         }
         else if (pl > 0)
         {
            for (int i = 0; i < pl; i++)
            {
               char s = path.charAt(i);
               if (s == '/' || s == '\\')
                  continue;
               else
               {
                  if (i > 0)
                     path = path.substring(i);
                  break;
               }
            }
         }
         // System.err.println("Path after processing :"+path+" slash was at
         // "+slpos);
         return new File((File) os[0], path).getPath();
      }
      return path;
   }

   /**
    * @return
    */
   public String getContextPath()
   {
      return "";
   }

   // / Returns the MIME type of the specified file.
   // @param file file name whose MIME type is required
   public String getMimeType(String file)
   {
      int dp = file.lastIndexOf('.');
      if (dp > 0)
      {
         return mime.getProperty(file.substring(dp + 1).toUpperCase());
      }
      return null;
   }

   // / Returns the name and version of the web server under which the servlet
   // is running.
   // Same as the CGI variable SERVER_SOFTWARE.
   public String getServerInfo()
   {
      return Serve.Identification.serverName + " " + Serve.Identification.serverVersion + " ("
              + Serve.Identification.serverUrl + ")";
   }

   // / Returns the value of the named attribute of the network service, or
   // null if the attribute does not exist. This method allows access to
   // additional information about the service, not already provided by
   // the other methods in this interface.
   public Object getAttribute(String name)
   {
      return attributes.get(name);
   }

   // ///////////////// JSDK 2.1 extensions //////////////////////////
   public void removeAttribute(String name)
   {
      attributes.remove(name);
   }

   public void setAttribute(String name, Object object)
   {
      if (object != null)
         attributes.put(name, object);
      else
         attributes.remove(name);
   }

   public Enumeration getAttributeNames()
   {
      return attributes.keys();
   }

   public ServletContext getContext(String uripath)
   {
      // TODO check webapp servlets to find out conexts for uri
      return this; // only root context supported
   }

   public int getMajorVersion()
   {
      return 2; // support 2.x
   }

   public int getMinorVersion()
   {
      return 5; // support 2.5
   }

   // 2.3

   /**
    * Returns a directory-like listing of all the paths to resources within the web application whose longest sub-path matches the supplied path argument.
    * Paths indicating subdirectory paths end with a '/'. The returned paths are all relative to the root of the web application and have a leading '/'. For
    * example, for a web application containing
    * <p/>
    * /welcome.html <br>
    * /catalog/index.html <br>
    * /catalog/products.html <br>
    * /catalog/offers/books.html <br>
    * /catalog/offers/music.html <br>
    * /customer/login.jsp <br>
    * /WEB-INF/web.xml <br>
    * /WEB-INF/classes/com.acme.OrderServlet.class,
    * <p/>
    * getResourcePaths("/") returns {"/welcome.html", "/catalog/", "/customer/", "/WEB-INF/"} <br>
    * getResourcePaths("/catalog/") returns {"/catalog/index.html", "/catalog/products.html", "/catalog/offers/"}.
    * <p/>
    *
    * @param the -
    *            partial path used to match the resources, which must start with a /
    * @return a Set containing the directory listing, or null if there are no resources in the web application whose path begins with the supplied path.
    * @since Servlet 2.3
    */
   public java.util.Set getResourcePaths(String path)
   {
      String realPath = getRealPath(path);
      if (realPath != null)
      {

         String[] dir = new File(realPath).list();
         if (dir.length > 0)
         {
            HashSet set = new HashSet(dir.length);
            for (int i = 0; i < dir.length; i++)
               set.add(dir[i]);
            return set;
         }
      }
      return null;
   }

   /**
    * Returns the name of this web application correponding to this ServletContext as specified in the deployment descriptor for this web application by the
    * display-name element.
    *
    * @return The name of the web application or null if no name has been declared in the deployment descriptor.
    * @since Servlet 2.3
    */
   public String getServletContextName()
   {
      return null;
   }

   /**
    * Returns a URL to the resource that is mapped to a specified path. The path must begin with a "/" and is interpreted as relative to the current context
    * root.
    * <p/>
    * <p/>
    * This method allows the servlet container to make a resource available to servlets from any source. Resources can be located on a local or remote file
    * system, in a database, or in a <code>.war</code> file.
    * <p/>
    * <p/>
    * The servlet container must implement the URL handlers and <code>URLConnection</code> objects that are necessary to access the resource.
    * <p/>
    * <p/>
    * This method returns <code>null</code> if no resource is mapped to the pathname.
    * <p/>
    * <p/>
    * Some containers may allow writing to the URL returned by this method using the methods of the URL class.
    * <p/>
    * <p/>
    * The resource content is returned directly, so be aware that requesting a <code>.jsp</code> page returns the JSP source code. Use a
    * <code>RequestDispatcher</code> instead to include results of an execution.
    * <p/>
    * <p/>
    * This method has a different purpose than <code>java.lang.Class.getResource</code>, which looks up resources based on a class loader. This method does
    * not use class loaders.
    *
    * @param path a <code>String</code> specifying the path to the resource
    * @return the resource located at the named path, or <code>null</code> if there is no resource at that path
    * @throws java.net.MalformedURLException if the pathname is not given in the correct form
    */
   public URL getResource(String path) throws MalformedURLException
   {
      if (path == null || path.length() == 0 || path.charAt(0) != '/')
         throw new MalformedURLException("Path " + path + " is not in acceptable form.");
      File resFile = new File(getRealPath(path));
      if (resFile.exists()) // TODO get canonical path is more robust
         return new URL("file", "localhost", resFile.getPath());
      return null;
   }

   /**
    * Returns the resource located at the named path as an <code>InputStream</code> object.
    * <p/>
    * <p/>
    * The data in the <code>InputStream</code> can be of any type or length. The path must be specified according to the rules given in
    * <code>getResource</code>. This method returns <code>null</code> if no resource exists at the specified path.
    * <p/>
    * <p/>
    * Meta-information such as content length and content type that is available via <code>getResource</code> method is lost when using this method.
    * <p/>
    * <p/>
    * The servlet container must implement the URL handlers and <code>URLConnection</code> objects necessary to access the resource.
    * <p/>
    * <p/>
    * This method is different from <code>java.lang.Class.getResourceAsStream</code>, which uses a class loader. This method allows servlet containers to
    * make a resource available to a servlet from any location, without using a class loader.
    *
    * @param path a <code>String</code> specifying the path to the resource
    * @return the <code>InputStream</code> returned to the servlet, or <code>null</code> if no resource exists at the specified path
    */
   public InputStream getResourceAsStream(String path)
   {
      try
      {
         return getResource(path).openStream();
      }
      catch (Exception e)
      {
      }
      return null;
   }

   public RequestDispatcher getRequestDispatcher(String urlpath)
   {
      if (urlpath == null || urlpath.length() == 0 || urlpath.charAt(0) != '/')
         return null;
      try
      {
         return new SimpleRequestDispatcher(urlpath);
      }
      catch (NullPointerException npe)
      {
         return null;
      }
   }

   // no way to specify parameters for context
   public String getInitParameter(String param)
   {
      return null;
   }

   public Enumeration getInitParameterNames()
   {
      return Utils.EMPTY_ENUMERATION;
   }

   public RequestDispatcher getNamedDispatcher(String name)
   {
      // named resources are not supported
      return null;
   }

   synchronized String generateSessionId()
   {
      srandom.nextBytes(uniqer);
      // TODO swap randomly bytes
      return Utils.base64Encode(uniqer);
   }

   protected class SimpleRequestDispatcher implements RequestDispatcher
   {
      HttpServlet servlet;

      String dispatchPath;

      String dispatchQuery;

      int dispatchLen;

      SimpleRequestDispatcher(String path)
      {
         Object[] os = registry.get(path);
         servlet = (HttpServlet) os[0];
         //log("Dispatch to: " + path + ", servlet "+servlet);
         if (servlet == null)
            throw new NullPointerException();
         dispatchLen = ((Integer) os[1]).intValue();
         int qmp = path.indexOf('?');
         if (qmp < 0 || qmp >= path.length() - 1)
            dispatchPath = path;
         else
         {
            dispatchPath = path.substring(0, qmp);
            dispatchQuery = path.substring(qmp + 1);
         }
      }

      public void forward(ServletRequest _request, ServletResponse _response) throws ServletException,
              IOException
      {
         _request.removeAttribute("javax.servlet.forward.request_uri"); // reset in case of nested
         _response.reset();
         servlet.service(new HttpServletRequestWrapper((HttpServletRequest) _request)
         {
            public String getPathInfo()
            {
               return dispatchLen >= dispatchPath.length() ? null : dispatchPath.substring(dispatchLen);
            }

            public String getRequestURI()
            {
               return dispatchPath;
            }

            public String getQueryString()
            {
               return dispatchQuery;
            }

            public String getPathTranslated()
            {
               //System.out.println("Path t path i: "+getPathInfo()+", dp: "+dispatchPath);
               return getRequest().getRealPath(getPathInfo());
            }

            public String getServletPath()
            {
               return dispatchLen <= 0 ? "" : dispatchPath.substring(0, dispatchLen);
            }

            public synchronized Enumeration getAttributeNames()
            {
               if (super.getAttribute("javax.servlet.forward.request_uri") == null)
               {
                  setAttribute("javax.servlet.forward.request_uri", super.getRequestURI());
                  setAttribute("javax.servlet.forward.context_path", this.getContextPath());
                  setAttribute("javax.servlet.forward.servlet_path", super.getServletPath());
                  setAttribute("javax.servlet.forward.path_info", super.getPathInfo());
                  setAttribute("javax.servlet.forward.query_string", super.getQueryString());
               }
               return super.getAttributeNames();
            }

            public Object getAttribute(String name)
            {
               getAttributeNames(); // here is some overhead
               return super.getAttribute(name);
            }

         }, _response);
         // TODO think when response isn't actual response ServeConnection
         ((ServeConnection) _response).closeStreams(); // do not allow to continue
      }

      public void include(ServletRequest _request, ServletResponse _response) throws ServletException,
              IOException
      {
         _request.removeAttribute("javax.servlet.include.request_uri"); // reset in case of nested
         ((Serve.ServeConnection) _response).setInInclude(true);
         try
         {
            servlet.service(new HttpServletRequestWrapper((HttpServletRequest) _request)
            {
               public synchronized Enumeration getAttributeNames()
               {
                  if (super.getAttribute("javax.servlet.include.request_uri") == null)
                  {
                     setAttribute("javax.servlet.include.request_uri", dispatchPath);
                     setAttribute("javax.servlet.include.context_path", this.getContextPath());
                     setAttribute("javax.servlet.include.servlet_path", dispatchLen <= 0 ? "" : dispatchPath
                             .substring(0, dispatchLen));
                     setAttribute("javax.servlet.include.path_info", dispatchLen >= dispatchPath.length() ? null
                             : dispatchPath.substring(dispatchLen));
                     setAttribute("javax.servlet.include.query_string", dispatchQuery);
                  }
                  return super.getAttributeNames();
               }

               public Object getAttribute(String name)
               {
                  getAttributeNames(); // here is some overhead
                  return super.getAttribute(name);
               }

            }, _response);
         }
         finally
         {
            ((Serve.ServeConnection) _response).setInInclude(false);
         }
      }

   }

   // Keep Alive supporter, JDK 1.4 based for backwar compatibility
   class KeepAliveCleaner extends Thread
   {
      protected List connections;

      protected List ingoings;

      protected volatile boolean stopped;

      private boolean noCheckClose;

      KeepAliveCleaner()
      {
         super("KeepAlive cleaner");
         connections = new ArrayList();
         ingoings = new ArrayList();
         setDaemon(true);
      }

      public void end()
      {
         stopped = true;
         this.interrupt();
      }

      synchronized void addConnection(ServeConnection conn)
      {
         synchronized (ingoings)
         {
            if (stopped == false)
               ingoings.add(conn);
         }
      }

      public void run()
      {
         long d = getKeepAliveDuration();
         int maxUse = getMaxTimesConnectionUse();
         while (true)
         {
            synchronized (ingoings)
            {
               Iterator i = ingoings.iterator();
               while (i.hasNext())
               {
                  connections.add(i.next());
                  i.remove();
               }
            }
            Iterator i = connections.iterator();
            long ct = System.currentTimeMillis();
            d = getKeepAliveDuration();
            while (i.hasNext())
            {
               ServeConnection conn = (ServeConnection) i.next();
               boolean closed = conn.socket == null;
               if (noCheckClose == false)
                  synchronized (conn)
                  {
                     if (conn.socket != null)
                        try
                        {
                           closed = ((Boolean) conn.socket.getClass().getMethod("isClosed",
                                   Utils.EMPTY_CLASSES).invoke(conn.socket, Utils.EMPTY_OBJECTS))
                                   .booleanValue();
                        }
                        catch (IllegalArgumentException e)
                        {
                        }
                        catch (SecurityException e)
                        {
                        }
                        catch (IllegalAccessException e)
                        {
                        }
                        catch (InvocationTargetException e)
                        {
                        }
                        catch (NoSuchMethodException e)
                        {
                           noCheckClose = true;
                        }
                  }
               if (closed || (conn.keepAlive && (ct - conn.lastWait > d && conn.lastRun < conn.lastWait))
                       || stopped
                  /* || conn.timesRequested > maxUse */)
               {
                  i.remove();
                  synchronized (conn)
                  {
                     if (conn.socket != null)
                        try
                        {
                           //System.err.println("Closing socket:"+conn.socket.getClass().getName()); // !!!
                           //conn.socket.close();
                           conn.socket.getInputStream().close();
                        }
                        catch (IOException ioe)
                        {
                           // ignore
                        }
                     //System.err.println("done");
                  }
               }
            }
            if (stopped && connections.size() == 0)
               break;
            try
            {
               sleep(d);
            }
            catch (InterruptedException ie)
            {
               stopped = true; // not thread safe
            }
         }
      }
   }

   final static class Identification
   {
      public static final String serverName = "D. Rogatkin's TJWS based on Acme.Serve";

      public static final String serverVersion = "Version 1.70, $Revision: 1.194 $";

      public static final String serverUrl = "http://tjws.sourceforge.net";

      public static final String serverIdHtml = "<ADDRESS><A HREF=\"" + serverUrl + "\">" + serverName + " " + serverVersion + "</A></ADDRESS>";
   }

   // ////////////////////////////////////////////////////////////////

   protected static class ServeConfig implements ServletConfig
   {

      private ServletContext context;

      private Hashtable init_params;

      private String servletName;

      public ServeConfig(ServletContext context)
      {
         this(context, null, "undefined");
      }

      public ServeConfig(ServletContext context, Hashtable initParams, String servletName)
      {
         this.context = context;
         this.init_params = initParams;
         this.servletName = servletName;
      }

      // Methods from ServletConfig.

      // / Returns the context for the servlet.

      public ServletContext getServletContext()
      {
         return context;
      }

      // / Gets an initialization parameter of the servlet.
      // @param name the parameter name
      public String getInitParameter(String name)
      {
         // This server supports servlet init params. :)
         if (init_params != null)
            return (String) init_params.get(name);
         return null;
      }

      // / Gets the names of the initialization parameters of the servlet.
      // @param name the parameter name
      public Enumeration getInitParameterNames()
      {
         // This server does:) support servlet init params.
         if (init_params != null)
            return init_params.keys();
         return new Vector().elements();
      }

      // 2.2
      public String getServletName()
      {
         return servletName;
      }
   }

   // /////////////////////////////////////////////////////////////////////
   /**
    * provides request/response
    */
   public static class ServeConnection implements Runnable, HttpServletRequest, HttpServletResponse
   {
      private Socket socket;

      private Hashtable sslAttributes;

      private Serve serve;

      private ServletInputStream in;

      private ServletOutputStream out;

      private String scheme;

      public final static String WWWFORMURLENCODE = "application/x-www-form-urlencoded";

      public final static String TRANSFERENCODING = "transfer-encoding".toLowerCase();

      public final static String KEEPALIVE = "Keep-Alive".toLowerCase();

      public final static String CONTENT_ENCODING = "Content-Encoding".toLowerCase();

      public final static String CONNECTION = "Connection".toLowerCase();

      public final static String CHUNKED = "chunked";

      public final static String CONTENTLENGTH = "Content-Length".toLowerCase();

      public final static String CONTENTTYPE = "Content-Type".toLowerCase();

      public final static String SETCOOKIE = "Set-Cookie".toLowerCase();

      public final static String HOST = "Host".toLowerCase();

      public final static String COOKIE = "Cookie".toLowerCase();

      public final static String ACCEPT_LANGUAGE = "Accept-Language".toLowerCase();

      public final static String SESSION_COOKIE_NAME = "JSESSIONID";

      public final static String SESSION_URL_NAME = ";$sessionid$"; // ;jsessionid=

      private static final Map EMPTYHASHTABLE = new Hashtable();

      // URL rewriting
      // http://www.myserver.com/catalog/index.html;jsessionid=mysession1928
      // like:
      // http://www.sun.com/2001-0227/sunblade/;$sessionid$AD5RQ0IAADJAZAMTA1LU5YQ

      private String reqMethod; // == null by default

      private String reqUriPath, reqUriPathUn;

      private String reqProtocol;

      private String charEncoding; // req and resp

      private String remoteUser;

      private String authType;

      private boolean oneOne; // HTTP/1.1 or better

      private boolean reqMime;

      private Vector reqHeaderNames = new Vector();

      private Vector reqHeaderValues = new Vector();

      private Locale locale; // = java.util.Locale.getDefault();

      private int uriLen;

      protected boolean keepAlive = true;

      protected int timesRequested;

      protected long lastRun, lastWait;

      private Vector outCookies;

      private Vector inCookies;

      private String sessionCookieValue, sessionUrlValue, sessionValue;

      protected String reqQuery;

      private PrintWriter pw;

      private ServletOutputStream rout;

      private Map formParameters;

      private Hashtable attributes = new Hashtable();

      private int resCode = -1;

      private String resMessage;

      private Hashtable resHeaderNames = new Hashtable();

      private String[] postCache;

      private boolean headersWritten;

      private MessageFormat accessFmt;

      private Object[] logPlaceholders;

      // TODO consider creation an instance per thread in a pool, thread memory can be used

      private final SimpleDateFormat expdatefmt = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US); // used for cookie

      private final SimpleDateFormat rfc850DateFmt = new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss 'GMT'",
              Locale.US); // rfc850-date

      private final SimpleDateFormat headerdateformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'",
              Locale.US); // rfc1123-date

      private final SimpleDateFormat asciiDateFmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US); // ASCII date, used in headers

      private static final TimeZone tz = TimeZone.getTimeZone("GMT");

      static
      {
         tz.setID("GMT");
      }

      /*
         * protected void finalize() throws Throwable { serve.log("Connection collected"); super.finalize(); }
         */

      // / Constructor.

      public ServeConnection(Socket socket, Serve serve)
      {
         // Save arguments.
         this.socket = socket;
         this.serve = serve;
         expdatefmt.setTimeZone(tz);
         headerdateformat.setTimeZone(tz);
         rfc850DateFmt.setTimeZone(tz);
         asciiDateFmt.setTimeZone(tz);
         if (serve.isAccessLogged())
         {
            // not format string must be not tull
            accessFmt = new MessageFormat((String) serve.arguments.get(ARG_ACCESS_LOG_FMT));
            logPlaceholders = new Object[12];
         }
         serve.threadPool.executeThread(this);
         synchronized(serve.connections)
         {
            serve.connections.add(this);
         }
      }

      private void initSSLAttrs()
      {
         if (socket.getClass().getName().indexOf("SSLSocket") > 0)
         {
            try
            {
               sslAttributes = new Hashtable();
               Object sslSession = socket.getClass().getMethod("getSession", Utils.EMPTY_CLASSES).invoke(socket,
                       Utils.EMPTY_OBJECTS);
               if (sslSession != null)
               {
                  sslAttributes.put("javax.net.ssl.session", sslSession);
                  Method m = sslSession.getClass().getMethod("getCipherSuite", Utils.EMPTY_CLASSES);
                  m.setAccessible(true);
                  sslAttributes.put("javax.net.ssl.cipher_suite", m.invoke(sslSession, Utils.EMPTY_OBJECTS));
                  m = sslSession.getClass().getMethod("getPeerCertificates", Utils.EMPTY_CLASSES);
                  m.setAccessible(true);
                  sslAttributes.put("javax.net.ssl.peer_certificates", m.invoke(sslSession, Utils.EMPTY_OBJECTS));
               }
            }
            catch (IllegalAccessException iae)
            {
               sslAttributes = null;
               //iae.printStackTrace();
            }
            catch (NoSuchMethodException nsme)
            {
               sslAttributes = null;
               //nsme.printStackTrace();
            }
            catch (InvocationTargetException ite)
            {
               // note we do not clear attributes, because SSLPeerUnverifiedException
               // happens in the last call, when no client sertificate
               //sslAttributes = null;
               //ite.printStackTrace();
            }
            catch (IllegalArgumentException iae)
            {
               //sslAttributes = null;
               //iae.printStackTrace();
            }
            //System.err.println("Socket SSL attrs: "+sslAttributes);
         }
      }

      /**
       * it closes stream awaring of keep -alive
       *
       * @throws java.io.IOException
       */
      public void closeStreams() throws IOException
      {
         //System.err.println("===>CLOSE()");
         IOException ioe = null;
         try
         {
            if (pw != null)
               pw.flush();
            else
               out.flush();
         }
         catch (IOException io1)
         {
            ioe = io1;
         }
         try
         {
            out.close();
         }
         catch (IOException io1)
         {
            if (ioe != null)
               ioe = (IOException) ioe.initCause(io1);
            else
               ioe = io1;
         }
         try
         {
            in.close();
         }
         catch (IOException io1)
         {
            if (ioe != null)
               ioe = (IOException) ioe.initCause(io1);
            else
               ioe = io1;
         }
         if (ioe != null)
            throw ioe;
      }

      public void closeSocket()
      {
         if (socket != null)
         {
            try
            {
               socket.close();
            }
            catch (IOException e)
            {
            }
         }
      }

      // protected void finalize() throws Throwable {
      // System.err.println("Connection object gone"); // !!!
      // super.finalize();
      // }

      private void restart()
      {
         // new Exception("RESTART").printStackTrace();
         reqMethod = null;
         reqUriPath = reqUriPathUn = null;
         reqProtocol = null;
         charEncoding = null;
         remoteUser = null;
         authType = null;
         oneOne = false;
         reqMime = false;
         // considering that clear() works faster than new
         if (reqHeaderNames == null)
            reqHeaderNames = new Vector();
         else
            reqHeaderNames.clear();
         if (reqHeaderValues == null)
            reqHeaderValues = new Vector();
         else
            reqHeaderValues.clear();
         locale = null;
         uriLen = 0;
         outCookies = null;
         inCookies = null;
         sessionCookieValue = null;
         sessionUrlValue = null;
         sessionValue = null;
         reqQuery = null;
         pw = null;
         rout = null;
         formParameters = null;
         if (attributes == null)
            attributes = new Hashtable();
         else
            attributes.clear();
         if (sslAttributes != null)
            attributes.putAll(sslAttributes);
         resCode = -1;
         resMessage = null;
         resHeaderNames.clear();
         headersWritten = false;
         postCache = null;
         ((ServeInputStream) in).refresh();
         ((ServeOutputStream) out).refresh();
      }

      // Methods from Runnable.
      public void run()
      {
         try
         {
            initSSLAttrs();
            in = new ServeInputStream(socket.getInputStream(), this);
            out = new ServeOutputStream(socket.getOutputStream(), this);
            do
            {
               restart();
               // Get the streams.
               parseRequest();
               if (reqMethod != null && serve.isAccessLogged())
               {
                  // consider caching socket stuff for faster logging
                  // {0} {1} {2} [{3,date,dd/MMM/yyyy:HH:mm:ss Z}] \"{4} {5} {6}\" {7,number,#} {8,number} {9} {10}
                  // ARG_ACCESS_LOG_FMT
                  logPlaceholders[0] = socket.getInetAddress(); // IP
                  logPlaceholders[1] = "-"; // the RFC 1413 identity of the client
                  logPlaceholders[2] = remoteUser == null ? "-" : remoteUser; // remote user
                  logPlaceholders[3] = new Date(lastRun); // time stamp {3,date,dd/MMM/yyyy:HH:mm:ss Z} {3,time,}
                  logPlaceholders[4] = reqMethod; // method
                  logPlaceholders[5] = reqUriPathUn; // resource
                  logPlaceholders[6] = reqProtocol; // protocol
                  logPlaceholders[7] = new Integer(resCode); // res code
                  logPlaceholders[8] = new Long(((ServeOutputStream) out).lengthWritten());
                  logPlaceholders[9] = new Integer(socket.getLocalPort());
                  logPlaceholders[10] = serve.isShowReferer() ? getHeader("Referer") : "-";
                  logPlaceholders[11] = serve.isShowUserAgent() ? getHeader("User-Agent") : "-";
                  serve.logStream.println(accessFmt.format(logPlaceholders));
               }
               lastRun = 0;
               timesRequested++;
            } while (keepAlive && serve.isKeepAlive() && timesRequested < serve.getMaxTimesConnectionUse());
         }
         catch (IOException ioe)
         {
            //System.err.println("Drop "+ioe);
            String errMsg = ioe.getMessage();
            if ((errMsg == null || errMsg.indexOf("ocket closed") < 0) && ioe instanceof java.nio.channels.AsynchronousCloseException == false)
               serve.log("IO error: " + ioe + " in processing a request from " + socket.getInetAddress() + ":"
                       + socket.getLocalPort() + " / " + socket.getClass().getName()/*, ioe*/);
            else
               synchronized (this)
               {
                  //serve.log("Exception considered as socket closed:"+ioe, ioe);
                  socket = null;
               }
         }
         finally
         {
            synchronized (this)
            {
               synchronized(serve.connections)
               {
                  serve.connections.remove(this);
               }
               if (socket != null)
                  try
                  {
                     socket.close();
                  }
                  catch (IOException e)
                  { /* ignore */
                  }
               socket = null;
            }
         }
      }

      private void parseRequest() throws IOException
      {
         byte[] lineBytes = new byte[4096];
         int len;
         String line;
         // / TODO put time mark here for start waiting for receiving requests
         lastWait = System.currentTimeMillis();
         // Read the first line of the request.
         len = in.readLine(lineBytes, 0, lineBytes.length);
         if (len == -1 || len == 0)
         {
            if (keepAlive)
            {
               keepAlive = false;
               // connection seems be closed

            }
            else
            {
               problem("Status-Code 400: Bad Request(empty)", SC_BAD_REQUEST);
            }
            return;
         }
         if (len >= lineBytes.length)
         {
            problem("Status-Code 414: Request-URI Too Long", SC_REQUEST_URI_TOO_LONG);
            return;
         }
         // //lastRun = 0; // to avoid closing socket in long process
         line = new String(lineBytes, 0, len, UTF8);
         StringTokenizer ust = new StringTokenizer(line);
         if (ust.hasMoreTokens())
         {
            reqMethod = ust.nextToken();
            if (ust.hasMoreTokens())
            {
               reqUriPathUn = ust.nextToken();
               // TODO make it only when URL overwrite enambled
               int uop = reqUriPathUn.indexOf(SESSION_URL_NAME);
               if (uop > 0)
               {
                  sessionUrlValue = reqUriPathUn.substring(uop + SESSION_URL_NAME.length());
                  reqUriPathUn = reqUriPathUn.substring(0, uop);
                  try
                  {
                     serve.getSession(sessionUrlValue).userTouch();
                  }
                  catch (NullPointerException npe)
                  {
                     sessionUrlValue = null;
                  }
                  catch (IllegalStateException ise)
                  {
                     sessionUrlValue = null;
                  }
               }
               if (ust.hasMoreTokens())
               {
                  reqProtocol = ust.nextToken();
                  oneOne = !reqProtocol.toUpperCase().equals("HTTP/1.0");
                  reqMime = true;
                  // Read the rest of the lines.
                  String s;
                  while ((s = ((ServeInputStream) in).readLine(HTTP_MAX_HDR_LEN)) != null)
                  {
                     if (s.length() == 0)
                        break;
                     int c = s.indexOf(':', 0);
                     if (c > 0)
                     {
                        String key = s.substring(0, c).trim().toLowerCase();
                        String value = s.substring(c + 1).trim();
                        reqHeaderNames.addElement(key);
                        reqHeaderValues.addElement(value);
                        if (CONNECTION.equalsIgnoreCase(key))
                           if (oneOne)
                              keepAlive = "close".equalsIgnoreCase(value) == false;
                           else
                              keepAlive = KEEPALIVE.equalsIgnoreCase(value);
                        else if (KEEPALIVE.equalsIgnoreCase(key))
                        { /// FF specific ?
                           // parse value to extract the connection specific timeoutKeepAlive and maxAliveConnUse
                           // todo that introduce the value in req/resp and copy defaults from Serve

                        }
                     }
                     else
                        serve.log("header field '" + s + "' without ':'");
                  }
               }
               else
               {
                  reqProtocol = "HTTP/0.9";
                  oneOne = false;
                  reqMime = false;
               }
            }
         }

         if (reqProtocol == null)
         {
            problem("Status-Code 400: Malformed request line:" + line, SC_BAD_REQUEST);
            return;
         }
         // Check Host: header in HTTP/1.1 requests.
         if (oneOne)
         {
            String host = getHeader(HOST);
            if (host == null)
            {
               problem("'Host' header missing in HTTP/1.1 request", SC_BAD_REQUEST);
               return;
            }
         }

         // Split off query string, if any.
         int qmark = reqUriPathUn.indexOf('?');
         if (qmark > -1)
         {
            if (qmark < reqUriPathUn.length() - 1)
               reqQuery = reqUriPathUn.substring(qmark + 1);
            reqUriPathUn = reqUriPathUn.substring(0, qmark);
         }
         reqUriPath = Utils.decode(reqUriPathUn, UTF8);
         // TDOD check if reqUriPathUn starts with http://host:port
         if (CHUNKED.equals(getHeader(TRANSFERENCODING)))
         {
            setHeader(CONTENTLENGTH, null);
            ((ServeInputStream) in).chunking(true);
         }
         String contentEncoding = extractEncodingFromContentType(getHeader(CONTENTTYPE));
         // TODO: encoding in request can be invalid, then do default
         setCharacterEncoding(contentEncoding != null ? contentEncoding : UTF8);
         String contentLength = getHeader(CONTENTLENGTH);
         if (contentLength != null)
            try
            {
               ((ServeInputStream) in).setContentLength(Long.parseLong(contentLength));
            }
            catch (NumberFormatException nfe)
            {
               serve.log("Invalid value of input content-length: " + contentLength);
            }
         // the code was originally in processing headers loop, however hhas been moved here
         String encoding = getHeader(CONTENT_ENCODING);
         /*  Don't do content encoding.  Let RESTEASY handle it
         if (encoding != null)
         {
            if ((encoding.equalsIgnoreCase("gzip") || encoding.equalsIgnoreCase("compressed"))
                    && null != serve.gzipInStreamConstr && ((ServeInputStream) in).compressed(true))
            {
            }
            else
            {
               problem("Status-Code 415: Unsupported media type:" + encoding, SC_UNSUPPORTED_MEDIA_TYPE);
               return;
            }
         }
         */
         if (assureHeaders() && socket.getKeepAlive() == false)
            socket.setKeepAlive(true);
         // TODO new SimpleRequestDispatcher(reqUriPathUn).forward((ServletRequest) this, (ServletResponse) this);
         Object[] os = serve.registry.get(reqUriPath);
         if (os[0] != null)
         { // note, os always not null
            // / TODO put time mark here to monitor actual servicing
            lastRun = System.currentTimeMillis();
            // System.err.println("Servlet "+os[0]+" for path "+reqUriPath);
            uriLen = ((Integer) os[1]).intValue();
            runServlet((HttpServlet) os[0]);
         }
         else
         {
            problem("No any servlet found for serving " + reqUriPath, SC_BAD_REQUEST);
         }
      }

      private boolean assureHeaders()
      {
         if (reqMime)
            setHeader("MIME-Version", "1.0");
         setDateHeader("Date", System.currentTimeMillis());
         setHeader("Server", Serve.Identification.serverName + "/" + Serve.Identification.serverVersion);
         if (keepAlive && serve.isKeepAlive())
         {
            if (reqMime)
            {
               setHeader(CONNECTION, KEEPALIVE); // set for 1.1 too, because some client do not follow a standard
               if (oneOne)
                  setHeader(KEEPALIVE, serve.getKeepAliveParamStr());
            }
            return true;
         }
         else
            setHeader(CONNECTION, "close");
         return false;
      }

      private void runServlet(HttpServlet servlete) throws IOException
      {
         // Set default response fields.
         setStatus(SC_OK);
         try
         {
            parseCookies();
            if (sessionValue == null) // not from cookie
               sessionValue = sessionUrlValue;
            if (authenificate())
            {
               if (servlete instanceof SingleThreadModel)
                  synchronized (servlete)
                  {
                     servlete.service((ServletRequest) this, (ServletResponse) this);
                  }
               else
                  servlete.service((ServletRequest) this, (ServletResponse) this);
            }
            // old close
         }
         catch (UnavailableException e)
         {
            if (e.isPermanent())
            {
               serve.registry.remove(servlete);
               servlete.destroy();
            }
            else if (e.getUnavailableSeconds() > 0)
               serve.log("Temporary unavailability feature is not supported " + servlete);
            problem(e.getMessage(), SC_SERVICE_UNAVAILABLE);
         }
         catch (ServletException e)
         {
            serve.log("Servlet exception", e);
            Throwable rootCause = e.getRootCause();
            while (rootCause != null)
            {
               serve.log("Caused by", rootCause);
               if (rootCause instanceof ServletException)
                  rootCause = ((ServletException) rootCause).getRootCause();
               else
                  rootCause = rootCause.getCause(); /* 1.4 */
            }
            problem(e.toString(), SC_INTERNAL_SERVER_ERROR);
         }
         catch (IOException ioe)
         {
            throw ioe;
         }
         catch (Exception e)
         {
            serve.log("Unexpected problem running servlet", e);
            problem("Unexpected problem running servlet: " + e.toString(), SC_INTERNAL_SERVER_ERROR);
         }
         finally
         {
            closeStreams();
            // socket will be closed by a caller if no keep-alive
         }
      }

      private boolean authenificate() throws IOException
      {
         Object[] o = serve.realms.get(reqUriPath); // by Niel Markwick
         BasicAuthRealm realm = null;
         if (o != null)
            realm = (BasicAuthRealm) o[0];
         // System.err.println("looking for realm for path "+getPathInfo()+"
         // in
         // "+serve.realms+" found "+realm);
         if (realm == null)
            return true;

         String credentials = getHeader("Authorization");

         if (credentials != null)
         {
            credentials = Acme.Utils.base64Decode(credentials.substring(credentials.indexOf(' ') + 1),
                    getCharacterEncoding());
            int i = credentials.indexOf(':');
            String user = credentials.substring(0, i);
            String password = credentials.substring(i + 1);
            remoteUser = user;
            authType = "BASIC"; // support only basic authenification (FORM, CLIENT_CERT, DIGEST )
            String realPassword = (String) realm.get(user);
            // System.err.println("User "+user+" Password "+password+" real
            // "+realPassword);
            if (realPassword != null && realPassword.equals(password))
               return true;
         }

         setStatus(SC_UNAUTHORIZED);
         setHeader("WWW-Authenticate", "basic realm=\"" + realm.name() + '"');
         //writeHeaders(); // because sendError() is used
         realSendError();
         return false;
      }

      private void problem(String logMessage, int resCode)
      {
         serve.log(logMessage);
         try
         {
            sendError(resCode, logMessage);
         }
         catch (IllegalStateException e)
         { /* ignore */
         }
         catch (IOException e)
         { /* ignore */
         }
      }

      private static final int MAYBEVERSION = 1;

      private static final int INVERSION = 2;

      private static final int OLD_INNAME = 3;

      private static final int OLD_INVAL = 4;

      private static final int INVERSIONNUM = 5;

      private static final int RECOVER = 6;

      private static final int NEW_INNAME = 7;

      private static final int NEW_INVAL = 8;

      private static final int INPATH = 9;

      private static final int MAYBEINPATH = 10;

      private static final int INPATHVALUE = 11;

      private static final int MAYBEPORT = 12;

      private static final int INDOMAIN = 13;

      private static final int MAYBEDOMAIN = 14;

      private static final int INPORT = 15;

      private static final int INDOMAINVALUE = 16;

      private static final int INPORTVALUE = 17;

      private void parseCookies() throws IOException
      {
         if (inCookies == null)
            inCookies = new Vector();
         String cookies = getHeader(COOKIE);
         if (cookies == null)
            return;
         try
         {
            String cookie_name = null;
            String cookie_value = null;
            String cookie_path = null;
            String cookie_domain = null;
            if (cookies.length() > 300 * 4096)
               throw new IOException("Cookie string too long:" + cookies.length());
            //System.err.println("We received:" + cookies);
            char[] cookiesChars = cookies.toCharArray();
            int state = MAYBEVERSION;
            StringBuffer token = new StringBuffer(256);
            boolean quoted = false;
            for (int i = 0; i < cookiesChars.length; i++)
            {
               char c = cookiesChars[i];

               switch (state)
               {
                  case MAYBEVERSION:
                     if (c != ' ')
                     {
                        token.append(c);
                        if (c == '$')
                        {
                           state = INVERSION; // RFC 2965
                        }
                        else
                           // RFC 2109
                           state = OLD_INNAME;
                     }
                     break;
                  case OLD_INNAME:
                     if (c == '=')
                     {
                        state = OLD_INVAL;
                        cookie_name = token.toString();
                        token.setLength(0);
                     }
                     else if (c != ' ' || token.length() > 0)
                        token.append(c);
                     break;
                  // TODO introduce val_start. then quoted value and value
                  case OLD_INVAL:
                     if (quoted == false)
                     {
                        if (c == ';')
                        {
                           state = OLD_INNAME;
                           cookie_value = token.toString();
                           token.setLength(0);
                           addCookie(cookie_name, cookie_value, null, null);
                        }
                        else if (c == '"' && token.length() == 0)
                           quoted = true;
                        else
                           token.append(c);
                     }
                     else
                     {
                        if (c == '"')
                           quoted = false;
                        else
                           token.append(c);
                     }
                     break;
                  case INVERSION:
                     if (c == '=')
                     {
                        if ("$Version".equals(token.toString()))
                           state = INVERSIONNUM;
                        else
                        {
                           state = OLD_INVAL; // consider name starts with $
                           cookie_name = token.toString();
                        }
                        token.setLength(0);
                     }
                     else
                        token.append(c);
                     break;
                  case INVERSIONNUM:
                     if (c == ',' || c == ';')
                     {
                        token.setLength(0);
                        state = NEW_INNAME;
                     }
                     else if (Character.isDigit(c) == false)
                     {
                        state = RECOVER;
                     }
                     else
                        token.append(c);
                     break;
                  case NEW_INNAME:
                     if (c == '=')
                     {
                        state = NEW_INVAL;
                        cookie_name = token.toString();
                        token.setLength(0);
                     }
                     else if (c != ' ' || token.length() > 0)
                        token.append(c);
                     break;
                  case NEW_INVAL:
                     if (c == ';')
                     {
                        state = MAYBEINPATH;
                        cookie_value = token.toString();
                        token.setLength(0);
                        cookie_path = null;
                     }
                     else if (c == ',')
                     {
                        state = NEW_INNAME;
                        cookie_value = token.toString();
                        token.setLength(0);
                        addCookie(cookie_name, cookie_value, null, null);
                     }
                     else
                        token.append(c);
                     break;
                  case MAYBEINPATH:
                     if (c != ' ')
                     {
                        token.append(c);
                        if (c == '$')
                        {
                           state = INPATH;
                        }
                        else
                        {
                           addCookie(cookie_name, cookie_value, null, null);
                           state = NEW_INNAME;
                        }
                     }
                     break;
                  case INPATH:
                     if (c == '=')
                     {
                        if ("$Path".equals(token.toString()))
                           state = INPATHVALUE;
                        else
                        {
                           addCookie(cookie_name, cookie_value, null, null);
                           state = NEW_INVAL; // consider name starts with $
                           cookie_name = token.toString();
                        }
                        token.setLength(0);
                     }
                     else
                        token.append(c);
                     break;
                  case INPATHVALUE:
                     if (c == ',')
                     {
                        cookie_path = token.toString();
                        state = NEW_INNAME;
                        addCookie(cookie_name, cookie_value, cookie_path, null);
                        token.setLength(0);
                     }
                     else if (c == ';')
                     {
                        state = MAYBEDOMAIN;
                        cookie_path = token.toString();
                        token.setLength(0);
                     }
                     else
                        token.append(c);
                     break;
                  case MAYBEDOMAIN:
                     if (c != ' ')
                     {
                        token.append(c);
                        if (c == '$')
                        {
                           state = INDOMAIN;
                        }
                        else
                        {
                           addCookie(cookie_name, cookie_value, cookie_path, null);
                           state = NEW_INNAME;
                        }
                     }
                     break;
                  case INDOMAIN:
                     if (c == '=')
                     {
                        if ("$Domain".equals(token.toString()))
                           state = INDOMAINVALUE;
                        else
                        {
                           addCookie(cookie_name, cookie_value, cookie_path, null);
                           state = NEW_INVAL; // consider name starts with $
                           cookie_name = token.toString();
                        }
                        token.setLength(0);
                     }
                     break;
                  case INDOMAINVALUE:
                     if (c == ',')
                     {
                        state = NEW_INNAME;
                        addCookie(cookie_name, cookie_value, cookie_path, token.toString());
                        token.setLength(0);
                     }
                     else if (c == ';')
                     {
                        cookie_domain = token.toString();
                        state = MAYBEPORT;
                     }
                     else
                        token.append(c);
                     break;
                  case MAYBEPORT:
                     if (c != ' ')
                     {
                        token.append(c);
                        if (c == '$')
                        {
                           state = INPORT;
                        }
                        else
                        {
                           addCookie(cookie_name, cookie_value, cookie_path, cookie_domain);
                           state = NEW_INNAME;
                        }
                     }
                     break;
                  case INPORT:
                     if (c == '=')
                     {
                        if ("$Port".equals(token.toString()))
                           state = INPORTVALUE;
                        else
                        {
                           addCookie(cookie_name, cookie_value, cookie_path, cookie_domain);
                           state = NEW_INVAL; // consider name starts with $
                           cookie_name = token.toString();
                        }
                        token.setLength(0);
                     }
                     break;
                  case INPORTVALUE:
                     if (c == ',' || c == ';')
                     {
                        int port = Integer.parseInt(token.toString());
                        state = NEW_INNAME;
                        addCookie(cookie_name, cookie_value, cookie_path, cookie_domain);
                        token.setLength(0);
                     }
                     else if (Character.isDigit(c) == false)
                     {
                        state = RECOVER;
                     }
                     else
                        token.append(c);
                     break;
                  case RECOVER:
                     serve.log("Parsing recover of cookie string " + cookies, null);
                     if (c == ';' || c == ',')
                     {
                        token.setLength(0);
                        state = NEW_INNAME;
                     }
                     break;
               }
            }
            if (state == OLD_INVAL || state == NEW_INVAL)
            {
               cookie_value = token.toString();
               addCookie(cookie_name, cookie_value, null, null);
            }
            else if (state == INPATHVALUE)
            {
               addCookie(cookie_name, cookie_value, token.toString(), null);
            }
            else if (state == INDOMAINVALUE)
            {
               addCookie(cookie_name, cookie_value, cookie_path, token.toString());
            }
            else if (state == INPORTVALUE)
               addCookie(cookie_name, cookie_value, cookie_path, cookie_domain);
         }
         catch (Error e)
         {
            serve.log("Error in parsing cookies: " + cookies, e);
         }
         catch (Exception e)
         {
            serve.log("An exception in parsing cookies: " + cookies, e);
         }
      }

      private void addCookie(String name, String value, String path, String domain)
      {
         if (SESSION_COOKIE_NAME.equals(name) && sessionCookieValue == null)
         {
            sessionCookieValue = value;
            try
            {
               serve.getSession(sessionCookieValue).userTouch();
               sessionValue = sessionCookieValue;
               sessionUrlValue = null;
            }
            catch (IllegalStateException ise)
            {
               sessionCookieValue = null;
            }
            catch (NullPointerException npe)
            {
               sessionCookieValue = null;
            }
         }
         else
         {
            Cookie c;
            inCookies.addElement(c = new Cookie(name, value));
            if (path != null)
            {
               c.setPath(path);
               if (domain != null)
                  c.setDomain(domain);
            }
         }
      }

      // Methods from ServletRequest.

      // / Returns the size of the request entity data, or -1 if not known.
      // Same as the CGI variable CONTENT_LENGTH.

      public int getContentLength()
      {
         return getIntHeader(CONTENTLENGTH);
      }

      // / Returns the MIME type of the request entity data, or null if
      // not known.
      // Same as the CGI variable CONTENT_TYPE.
      public String getContentType()
      {
         return getHeader(CONTENTTYPE);
      }

      // / Returns the protocol and version of the request as a string of
      // the form <protocol>/<major version>.<minor version>.
      // Same as the CGI variable SERVER_PROTOCOL.
      public String getProtocol()
      {
         return reqProtocol;
      }

      // / Returns the scheme of the URL used in this request, for example
      // "http", "https", or "ftp". Different schemes have different rules
      // for constructing URLs, as noted in RFC 1738. The URL used to create
      // a request may be reconstructed using this scheme, the server name
      // and port, and additional information such as URIs.
      public String getScheme()
      {
         if (scheme == null)
            // lazy stuf dlc
            synchronized (this)
            {
               if (scheme == null)
                  scheme = socket.getClass().getName().indexOf("SSLSocket") > 0 ? "https" : "http";
            }
         return scheme;
      }

      // / Returns the host name of the server as used in the <host> part of
      // the request URI.
      // Same as the CGI variable SERVER_NAME.
      public String getServerName()
      {
         String serverName;
         serverName = getHeader(HOST);
         if (serverName != null && serverName.length() > 0)
         {
            int colon = serverName.indexOf(':');
            if (colon >= 0)
            {
               if (colon < serverName.length())
                  serverName = serverName.substring(0, colon);
            }
         }

         if (serverName == null)
         {
            try
            {
               serverName = InetAddress.getLocalHost().getHostName();
            }
            catch (java.net.UnknownHostException ignore)
            {
               serverName = "127.0.0.0";
            }
         }

         int slash = serverName.indexOf("/");
         if (slash >= 0)
            serverName = serverName.substring(slash + 1);
         return serverName;
      }

      // / Returns the port number on which this request was received as used
      // in
      // the <port> part of the request URI.
      // Same as the CGI variable SERVER_PORT.
      public int getServerPort()
      {
         return socket.getLocalPort();
      }

      // / Returns the IP address of the agent that sent the request.
      // Same as the CGI variable REMOTE_ADDR.
      public String getRemoteAddr()
      {
         return socket.getInetAddress().getHostAddress();
      }

      // / Returns the fully qualified host name of the agent that sent the
      // request.
      // Same as the CGI variable REMOTE_HOST.
      public String getRemoteHost()
      {
         String result = socket.getInetAddress().getHostName();
         return result != null ? result : getRemoteAddr();
      }

      // / Applies alias rules to the specified virtual path and returns the
      // corresponding real path, or null if the translation can not be
      // performed for any reason. For example, an HTTP servlet would
      // resolve the path using the virtual docroot, if virtual hosting is
      // enabled, and with the default docroot otherwise. Calling this
      // method with the string "/" as an argument returns the document root.
      public String getRealPath(String path)
      {
         return serve.getRealPath(path);
      }

      // / Returns an input stream for reading request data.
      // @exception IllegalStateException if getReader has already been called
      // @exception IOException on other I/O-related errors
      public ServletInputStream getInputStream() throws IOException
      {
         synchronized (in)
         {
            if (((ServeInputStream) in).isReturnedAsReader())
               throw new IllegalStateException("Already returned as a reader.");
            ((ServeInputStream) in).setReturnedAsStream(true);
         }
         return in;
      }

      // / Returns a buffered reader for reading request data.
      // @exception UnsupportedEncodingException if the character set encoding
      // isn't supported
      // @exception IllegalStateException if getInputStream has already been
      // called
      // @exception IOException on other I/O-related errors
      public BufferedReader getReader()
      {
         synchronized (in)
         {
            if (((ServeInputStream) in).isReturnedAsStream())
               throw new IllegalStateException("Already returned as a stream.");
            ((ServeInputStream) in).setReturnedAsReader(true);
         }
         if (charEncoding != null)
            try
            {
               return new BufferedReader(new InputStreamReader(in, charEncoding));
            }
            catch (UnsupportedEncodingException uee)
            {
            }
         return new BufferedReader(new InputStreamReader(in));
      }

      private synchronized Map getParametersFromRequest()
      {
         Map result = null;
         if ("GET".equals(reqMethod))
         {
            if (reqQuery != null)
               try
               {
                  result = Acme.Utils.parseQueryString(reqQuery, charEncoding);
               }
               catch (IllegalArgumentException ex)
               {
                  serve.log("Exception " + ex + " at parsing 'get' data " + reqQuery);
               }
         }
         else if ("POST".equals(reqMethod))
         {
            String contentType = getContentType();
            if (contentType != null && WWWFORMURLENCODE.regionMatches(true, 0, contentType, 0, WWWFORMURLENCODE.length()))
            {
               if (postCache == null)
               {
                  postCache = new String[1];
                  InputStream is = null;
                  try
                  {
                     result = Acme.Utils.parsePostData(getContentLength(), is = getInputStream(), charEncoding,
                             postCache);
                  }
                  catch (Exception ex)
                  {
                     serve.log("Exception " + ex + " at parsing 'POST' data of length " + getContentLength());
                     // TODO propagate the exception ?
                     return EMPTYHASHTABLE;
                  }
               }
               else
                  result = Acme.Utils.parseQueryString(postCache[0], charEncoding);
               if (reqQuery != null && reqQuery.length() > 0)
                  result.putAll(Acme.Utils.parseQueryString(reqQuery, charEncoding));
            }
            else if (reqQuery != null)
               result = Acme.Utils.parseQueryString(reqQuery, charEncoding);
         }
         return result != null ? result : EMPTYHASHTABLE;
      }

      // / Returns the parameter names for this request.
      public synchronized Enumeration getParameterNames()
      {
         if (formParameters == null)
            formParameters = getParametersFromRequest();
         return ((Hashtable) formParameters).keys();
      }

      // / Returns the value of the specified query string parameter, or null
      // if not found.
      // @param name the parameter name
      public String getParameter(String name)
      {
         String[] params = getParameterValues(name);
         if (params == null || params.length == 0)
            return null;

         return params[0];
      }

      // / Returns the values of the specified parameter for the request as an
      // array of strings, or null if the named parameter does not exist.
      public synchronized String[] getParameterValues(String name)
      {
         if (formParameters == null)
            getParameterNames();

         return (String[]) formParameters.get(name);
      }

      // / Returns the value of the named attribute of the request, or null if
      // the attribute does not exist. This method allows access to request
      // information not already provided by the other methods in this
      // interface.
      public Object getAttribute(String name)
      {
         // System.err.println("!!!Get att orig:"+name+"="+attributes.get(name));
         return attributes.get(name);
      }

      // Methods from HttpServletRequest.

      // / Gets the array of cookies found in this request.

      public Cookie[] getCookies()
      {
         Cookie[] cookieArray = new Cookie[inCookies.size()];
         inCookies.copyInto(cookieArray);
         return cookieArray;
      }

      // / Returns the method with which the request was made. This can be
      // "GET",
      // "HEAD", "POST", or an extension method.
      // Same as the CGI variable REQUEST_METHOD.
      public String getMethod()
      {
         return reqMethod;
      }

      /**
       * ****************************************************************************************************************************************************
       * Returns the part of this request's URL from the protocol name up to the query string in the first line of the HTTP request. To reconstruct an URL
       * with a scheme and host, use HttpUtils.getRequestURL(javax.servlet.http.HttpServletRequest).
       */
      // / Returns the full request URI.
      public String getRequestURI()
      {
         return reqUriPathUn;
      }

      /**
       * Reconstructs the URL the client used to make the request. The returned URL contains a protocol, server name, port number, and server path, but it
       * does not include query string parameters. <br>
       * Because this method returns a StringBuffer, not a string, you can modify the URL easily, for example, to append query parameters.
       * <p/>
       * This method is useful for creating redirect messages and for reporting errors.
       *
       * @return a StringBuffer object containing the reconstructed URL
       * @since 2.3
       */
      public StringBuffer getRequestURL()
      {
         int port = getServerPort();
         return new StringBuffer().append(getScheme()).append("://").append(getServerName()).append(
                 "https".equals(getScheme()) && port == 443 || port == 80 ? "" : ":" + String.valueOf(port)).append(
                 getRequestURI());
      }

      // / Returns the part of the request URI that referred to the servlet
      // being
      // invoked.
      // Analogous to the CGI variable SCRIPT_NAME.
      public String getServletPath()
      {
         // In this server, the entire path is regexp-matched against the
         // servlet pattern, so there's no good way to distinguish which
         // part refers to the servlet.
         return uriLen > 0 ? reqUriPath.substring(0, uriLen) : "";
      }

      // / Returns optional extra path information following the servlet path,
      // but
      // immediately preceding the query string. Returns null if not
      // specified.
      // Same as the CGI variable PATH_INFO.
      public String getPathInfo()
      {
         // In this server, the entire path is regexp-matched against the
         // servlet pattern, so there's no good way to distinguish which
         // part refers to the servlet.
         return uriLen >= reqUriPath.length() ? null : reqUriPath.substring(uriLen);
      }

      // / Returns extra path information translated to a real path. Returns
      // null if no extra path information was specified.
      // Same as the CGI variable PATH_TRANSLATED.
      public String getPathTranslated()
      {
         // In this server, the entire path is regexp-matched against the
         // servlet pattern, so there's no good way to distinguish which
         // part refers to the servlet.
         return getRealPath(getPathInfo());
      }

      // / Returns the query string part of the servlet URI, or null if not
      // known.
      // Same as the CGI variable QUERY_STRING.
      public String getQueryString()
      {
         return reqQuery;
      }

      // / Returns the name of the user making this request, or null if not
      // known.
      // Same as the CGI variable REMOTE_USER.
      public String getRemoteUser()
      {
         return remoteUser;
      }

      // / Returns the authentication scheme of the request, or null if none.
      // Same as the CGI variable AUTH_TYPE.
      public String getAuthType()
      {
         return authType;
      }

      // / Returns the value of a header field, or null if not known.
      // Same as the information passed in the CGI variabled HTTP_*.
      // @param name the header field name
      public String getHeader(String name)
      {
         int i = reqHeaderNames.indexOf(name.toLowerCase());
         if (i == -1)
            return null;
         return (String) reqHeaderValues.elementAt(i);
      }

      public int getIntHeader(String name)
      {
         String val = getHeader(name);
         if (val == null)
            return -1;
         return Integer.parseInt(val);
      }

      public long getDateHeader(String name)
      {
         String val = getHeader(name);
         if (val == null)
            return -1;
         try
         {
            return headerdateformat.parse(val).getTime();
         }
         catch (ParseException pe)
         {
            try
            {
               return rfc850DateFmt.parse(val).getTime();
            }
            catch (ParseException pe1)
            {
               try
               {
                  return asciiDateFmt.parse(val).getTime();
               }
               catch (ParseException pe3)
               {
                  throw new IllegalArgumentException("Value " + val
                          + " can't be converted to Date using any of formats: [" + headerdateformat.toPattern()
                          + "][ " + rfc850DateFmt.toPattern() + "][" + asciiDateFmt.toPattern());
               }
            }
         }
      }

      // / Returns an Enumeration of the header names.
      public Enumeration getHeaderNames()
      {
         return reqHeaderNames.elements();
      }

      // / Gets the current valid session associated with this request, if
      // create is false or, if necessary, creates a new session for the
      // request, if create is true.
      // <P>
      // Note: to ensure the session is properly maintained, the servlet
      // developer must call this method (at least once) before any output
      // is written to the response.
      // <P>
      // Additionally, application-writers need to be aware that newly
      // created sessions (that is, sessions for which HttpSession.isNew
      // returns true) do not have any application-specific state.
      public synchronized HttpSession getSession(boolean create)
      {
         HttpSession result = null;
         if (sessionValue != null)
         {
            result = serve.getSession(sessionValue);
            if (result != null && ((AcmeSession) result).isValid() == false)
            {
               serve.removeSession(sessionValue);
               result = null;
            }
            //System.err.println("^^^^^^^req sess: "+sessionValue+", found:"+result);
         }
         if (result == null && create)
         {
            result = serve.createSession();
            if (result != null)
            {
               sessionValue = result.getId();
            }
            else
               throw new RuntimeException("A session can't be created");
            //System.err.println("^~~~~~created: "+sessionValue);
         }
         return result;
      }

      // JSDK 2.1
      public HttpSession getSession()
      {
         return getSession(true);
      }

      public boolean isRequestedSessionIdFromURL()
      {
         return isRequestedSessionIdFromUrl();
      }

      // from ServletRequest
      public Enumeration getAttributeNames()
      {
         return attributes.keys();
      }

      /**
       * Stores an attribute in this request. Attributes are reset between requests.
       * This method is most often used in conjunction with RequestDispatcher.
       * <p>Attribute names should follow the same conventions as package names.
       * Names beginning with java.*, javax.*, and com.sun.*, are reserved for
       * use by Sun Microsystems. If the object passed in is null, the effect is
       * the same as calling removeAttribute(java.lang.String).
       * <p/>
       * It is warned that when the request is dispatched from the servlet resides
       * in a different web application by RequestDispatcher, the object set by
       * this method may not be correctly retrieved in the caller servlet.
       *
       * @param name - a String specifying the name of the attribute
       * @param o    - the Object to be stored
       */
      public void setAttribute(String key, Object o)
      {
         //System.err.println("!!!Set att orig:"+key+"="+o);
         //if ("javax.servlet.jsp.jspException".equals(key) && o instanceof Throwable)
         //((Throwable)o).printStackTrace();

         if (o != null)
            attributes.put(key, o);
         else
            attributes.remove(key);
      }

      // / Gets the session id specified with this request. This may differ
      // from the actual session id. For example, if the request specified
      // an id for an invalid session, then this will get a new session with
      // a new id.
      public String getRequestedSessionId()
      {
         return sessionValue;
      }

      // / Checks whether this request is associated with a session that is
      // valid in the current session context. If it is not valid, the
      // requested session will never be returned from the getSession
      // method.
      public boolean isRequestedSessionIdValid()
      {
         if (sessionValue != null)
         {
            AcmeSession session = serve.getSession(sessionValue);
            return (session != null && session.isValid());
         }
         return false;
      }

      /**
       * Checks whether the session id specified by this request came in as a cookie. (The requested session may not be one returned by the getSession
       * method.)
       */
      public boolean isRequestedSessionIdFromCookie()
      {
         return sessionCookieValue != null;
      }

      // / Checks whether the session id specified by this request came in as
      // part of the URL. (The requested session may not be the one returned
      // by the getSession method.)
      public boolean isRequestedSessionIdFromUrl()
      {
         return sessionUrlValue != null;
      }

      // Methods from ServletResponse.

      // / Sets the content length for this response.
      // @param length the content length

      public void setContentLength(int length)
      {
         if (length >= 0)
            setIntHeader(CONTENTLENGTH, length);
         else
            setHeader(CONTENTLENGTH, null);
      }

      // / Sets the content type for this response.
      // @param type the content type
      public void setContentType(String type)
      {
         setHeader(CONTENTTYPE, type != null ? type : "Unknown");
      }

      // / Returns an output stream for writing response data.
      public ServletOutputStream getOutputStream()
      {
         synchronized (out)
         {
            if (rout == null)
            {
               if (pw != null)
                  throw new IllegalStateException("Already returned as a writer");
               rout = out;
            }
         }
         return rout;
      }

      // / Returns a print writer for writing response data. The MIME type of
      // the response will be modified, if necessary, to reflect the character
      // encoding used, through the charset=... property. This means that the
      // content type must be set before calling this method.
      // @exception UnsupportedEncodingException if no such encoding can be
      // provided
      // @exception IllegalStateException if getOutputStream has been called
      // @exception IOException on other I/O errors
      public PrintWriter getWriter() throws IOException
      {
         synchronized (out)
         {
            if (pw == null)
            {
               if (rout != null)
                  throw new IllegalStateException("Already was returned as servlet output stream");
               String encoding = getCharacterEncoding();
               if (encoding != null)
                  pw = new PrintWriter(new OutputStreamWriter(out, encoding));
               else
                  pw = new PrintWriter(out);
            }
         }
         return pw;
      }

      // / Returns the character set encoding used for this MIME body. The
      // character encoding is either the one specified in the assigned
      // content type, or one which the client understands. If no content
      // type has yet been assigned, it is implicitly set to text/plain.
      public String getCharacterEncoding()
      {
         String ct = (String) resHeaderNames.get(CONTENTTYPE.toLowerCase());
         if (ct != null)
         {
            String enc = extractEncodingFromContentType(ct);
            if (enc != null)
               return enc;
         }
         return charEncoding;
      }

      private String extractEncodingFromContentType(String ct)
      {
         if (ct == null)
            return null;
         int scp = ct.indexOf(';');
         if (scp > 0)
         {
            scp = ct.toLowerCase().indexOf("charset=", scp);
            if (scp >= 0)
            {
               ct = ct.substring(scp + "charset=".length()).trim();
               scp = ct.indexOf(';');
               if (scp > 0)
                  ct = ct.substring(0, scp);
               int l = ct.length();
               if (l > 2 && ct.charAt(0) == '"')
                  return ct.substring(1, l - 1);
               return ct;
            }
         }
         return null;
      }

      // 2.2
      // do not use buffer
      public void flushBuffer() throws IOException
      {
         ((ServeOutputStream) out).flush();
      }

      /**
       * Clears the content of the underlying buffer in the response without clearing headers or status code. If the response has been committed, this method
       * throws an IllegalStateException.
       *
       * @since 2.3
       */
      public void resetBuffer()
      {
         ((ServeOutputStream) out).reset();
         synchronized (this)
         {
            headersWritten = false;
         }
      }

      public int getBufferSize()
      {
         return ((ServeOutputStream) out).getBufferSize();
      }

      public void setBufferSize(int size)
      {
         ((ServeOutputStream) out).setBufferSize(size);
      }

      /**
       * Returns a boolean indicating if the response has been committed. A commited response has already had its status code and headers written.
       *
       * @return a boolean indicating if the response has been committed
       * @see setBufferSize(int), getBufferSize(), flushBuffer(), reset()
       */
      // a caller should think about syncronization
      public boolean isCommitted()
      {
         return headersWritten && ((ServeOutputStream) out).lengthWritten() > 0;
      }

      /**
       * Clears any data that exists in the buffer as well as the status code and headers. If the response has been committed, this method throws an
       * IllegalStateException.
       *
       * @throws IllegalStateException
       *          -
       *          if the response has already been committed
       * @see setBufferSize(int), getBufferSize(), flushBuffer(), isCommitted()
       */
      public void reset() throws IllegalStateException
      {
         // new Exception("RESET").printStackTrace();
         if (!isCommitted())
         {
            if (outCookies != null)
               outCookies.clear();
            resHeaderNames.clear();
            pw = null;
            rout = null;
            ((ServeOutputStream) out).reset();
            assureHeaders();
         }
         else
            throw new IllegalStateException("Header have already been committed.");
      }

      /**
       * Sets the locale of the response, setting the headers (including the Content-Type's charset) as appropriate. This method should be called before a
       * call to getWriter(). By default, the response locale is the default locale for the server.
       *
       * @param loc -
       *            the locale of the response
       * @see getLocale()
       */
      public void setLocale(Locale locale)
      {
         this.locale = locale;
      }

      /**
       * For request: Returns the preferred Locale that the client will accept content in, based on the Accept-Language header. If the client request doesn't
       * provide an Accept-Language header, this method returns the default locale for the server.
       * <p/>
       * For response: Returns the locale specified for this response using the setLocale(java.util.Locale) method. Calls made to setLocale after the response
       * is committed have no effect. If no locale has been specified, the container's default locale is returned.
       */
      public Locale getLocale()
      {
         if (locale != null)
            return locale;
         Enumeration e = getLocales();
         if (e.hasMoreElements())
            return (Locale) e.nextElement();
         return Locale.getDefault();
      }

      /**
       * Returns an Enumeration of Locale objects indicating, in decreasing order starting with the preferred locale, the locales that are acceptable to the
       * client based on the Accept-Language header. If the client request doesn't provide an Accept-Language header, this method returns an Enumeration
       * containing one Locale, the default locale for the server.
       */
      public Enumeration getLocales()
      {
         // TODO: cache result
         String al = getHeader(ACCEPT_LANGUAGE);
         TreeSet ts = new TreeSet();
         if (al != null)
         {
            // System.err.println("Accept lang:"+al);
            StringTokenizer st = new StringTokenizer(al, ";", false);
            try
            {
               while (st.hasMoreTokens())
               {
                  String langs = st.nextToken(";");
                  // System.err.println("Langs:"+langs);
                  String q = st.nextToken(";=");
                  // System.err.println("q:"+q);
                  q = st.nextToken("=,");
                  // System.err.println("q:"+q);
                  float w = 0;
                  try
                  {
                     w = Float.valueOf(q).floatValue();
                  }
                  catch (NumberFormatException nfe)
                  {
                  }
                  if (w > 0)
                  {
                     StringTokenizer lst = new StringTokenizer(langs, ", ", false);
                     while (lst.hasMoreTokens())
                     {
                        String lan = lst.nextToken();
                        int di = lan.indexOf('-');
                        if (di < 0)
                           ts.add(new LocaleWithWeight(new Locale(lan.trim()) /* 1.4 */, w));
                        else
                           ts.add(new LocaleWithWeight(new Locale(lan.substring(0, di), lan.substring(di + 1)
                                   .trim().toUpperCase()), w));
                     }
                  }
               }
            }
            catch (NoSuchElementException ncee)
            {
               // can't parse
            }
         }
         if (ts.size() == 0)
            ts.add(new LocaleWithWeight(Locale.getDefault(), 1));
         return new AcceptLocaleEnumeration(ts);
      }

      /**
       * Overrides the name of the character encoding used in the body of this request. This method must be called prior to reading request parameters or
       * reading input using getReader().
       *
       * @param a -
       *          String containing the name of the chararacter encoding.
       * @throws java.io.UnsupportedEncodingException
       *          -
       *          if this is not a valid encoding
       * @since JSDK 2.3
       */
      public void setCharacterEncoding(String _enc)
      {
         // TODO: check if encoding is valid
         charEncoding = _enc;
         synchronized (this)
         {
            formParameters = null;
         }
      }

      public void addDateHeader(String header, long date)
      {
         addHeader(header, headerdateformat.format(new Date(date)));
      }

      public void addHeader(String header, String value)
      {
         header = header.trim().toLowerCase();
         Object o = resHeaderNames.get(header);
         if (o == null)
            setHeader(header, value);
         else
         {
            if (o instanceof String[])
            {
               String[] oldVal = (String[]) o;
               String[] newVal = new String[oldVal.length + 1];
               System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
               newVal[oldVal.length] = value;
               resHeaderNames.put(header, newVal);
            }
            else if (o instanceof String)
            {
               String[] newVal = new String[2];
               newVal[0] = (String) o;
               newVal[1] = value;
               resHeaderNames.put(header, newVal);
            }
            else
               throw new RuntimeException("Invalid content of header hash - " + o.getClass().getName());
         }
      }

      public void addIntHeader(String header, int value)
      {
         addHeader(header, Integer.toString(value));
      }

      public RequestDispatcher getRequestDispatcher(String urlpath)
      {
         if (urlpath.length() > 0 && urlpath.charAt(0) != '/')
         {
            String dispatchPath = getContextPath();
            String pathInfo = getPathInfo();
            String servletPath = getServletPath();
            ;
            if (pathInfo != null)
            {
               dispatchPath += servletPath;
               int slp = pathInfo.indexOf('/', 1);
               if (slp > 0) // can it ever happen?
                  dispatchPath += pathInfo.substring(0, slp - 1);
            }
            else
            {
               int spsp = servletPath.lastIndexOf('/');
               if (spsp >= 0)
                  dispatchPath += servletPath.substring(0, spsp);
            }
            // serve.log("Dispatch path:"+dispatchPath);
            urlpath = dispatchPath + '/' + urlpath;
         }
         return serve.getRequestDispatcher(urlpath);
      }

      public boolean isSecure()
      {
         return "https".equals(getScheme());
      }

      public void removeAttribute(String name)
      {
         attributes.remove(name);
      }

      // only root context supported
      public String getContextPath()
      {
         return "";
      }

      public Enumeration getHeaders(String header)
      {
         Vector result = new Vector();
         int i = -1;
         while ((i = reqHeaderNames.indexOf(header.toLowerCase(), i + 1)) >= 0)
            result.addElement(reqHeaderValues.elementAt(i));
         return result.elements();
      }

      public java.security.Principal getUserPrincipal()
      {
         return null;
      }

      public boolean isUserInRole(String user)
      {
         return false;
      }

      /**
       * Returns a java.util.Map of the parameters of this request. Request parameters are extra information sent with the request. For HTTP servlets,
       * parameters are contained in the query string or posted form data.
       *
       * @return an immutable java.util.Map containing parameter names as keys and parameter values as map values. The keys in the parameter map are of type
       *         String. The values in the parameter map are of type String array.
       * @since 2.3
       */
      public synchronized Map getParameterMap()
      {
         if (formParameters == null)
            getParameterNames();
         return formParameters;
      }

      // Methods from HttpServletResponse.

      // / Adds the specified cookie to the response. It can be called
      // multiple times to set more than one cookie.

      public void addCookie(Cookie cookie)
      {
         if (outCookies == null)
            outCookies = new Vector();

         outCookies.addElement(cookie);
      }

      // / Checks whether the response message header has a field with the
      // specified name.
      public boolean containsHeader(String name)
      {
         return resHeaderNames.contains(name);
      }

      // JSDK 2.1 extension
      public String encodeURL(String url)
      {
         int uop = url.indexOf(SESSION_URL_NAME);
         // TODO not robust enough
         if (uop > 0)
            url = url.substring(0, uop);
         if (sessionValue == null || isRequestedSessionIdFromCookie())
            return url;
         try
         {
            new URL(url); // for testing syntac
            int ehp = url.indexOf('/');
            if (ehp < 0)
               ehp = url.indexOf('?');
            if (ehp < 0)
               ehp = url.indexOf('#');
            if (ehp < 0)
               ehp = url.length();
            if (url.regionMatches(true, 0, getRequestURL().toString(), 0, ehp) == false)
               return url;
         }
         catch (MalformedURLException e)
         {
         }

         return url + SESSION_URL_NAME + sessionValue;
      }

      public String encodeRedirectURL(String url)
      {
         return encodeURL(url);
      }

      /**
       * Returns the Internet Protocol (IP) source port of the client or last proxy that sent the request.
       *
       * @return an integer specifying the port number
       * @since 2.4
       */
      public int getRemotePort()
      {
         return socket.getPort(); // TODO not quite robust
      }

      /**
       * Returns the host name of the Internet Protocol (IP) interface on which the request was received.
       *
       * @return a <code>String</code> containing the host name of the IP on which the request was received.
       * @since 2.4
       */
      public String getLocalName()
      {
         InetAddress ia = socket/* serve.serverSocket */.getLocalAddress();
         return ia == null ? null : ia.getHostAddress();
      }

      /**
       * Returns the Internet Protocol (IP) address of the interface on which the request was received.
       *
       * @return a <code>String</code> containing the IP address on which the request was received.
       * @since 2.4
       */
      public String getLocalAddr()
      {
         InetAddress ia = /* serve.serverSocket */socket.getLocalAddress();
         return ia == null ? null : ia.getCanonicalHostName(); /* 1.4 */
      }

      /**
       * Returns the Internet Protocol (IP) port number of the interface on which the request was received.
       *
       * @return an integer specifying the port number
       * @since 2.4
       */
      public int getLocalPort()
      {
         return getServerPort();
      }

      // / Sets the status code and message for this response.
      // @param resCode the status code
      // @param resMessage the status message
      public void setStatus(int resCode, String resMessage)
      {
         // if (((ServeOutputStream) out).isInInclude())
         // return;
         this.resCode = resCode;
         this.resMessage = resMessage;
      }

      // / Sets the status code and a default message for this response.
      // @param resCode the status code
      public void setStatus(int resCode)
      {
         switch (resCode)
         {
            case SC_CONTINUE:
               setStatus(resCode, "Continue");
               break;
            case SC_SWITCHING_PROTOCOLS:
               setStatus(resCode, "Switching protocols");
               break;
            case SC_OK:
               setStatus(resCode, "Ok");
               break;
            case SC_CREATED:
               setStatus(resCode, "Created");
               break;
            case SC_ACCEPTED:
               setStatus(resCode, "Accepted");
               break;
            case SC_NON_AUTHORITATIVE_INFORMATION:
               setStatus(resCode, "Non-authoritative");
               break;
            case SC_NO_CONTENT:
               setStatus(resCode, "No content");
               break;
            case SC_RESET_CONTENT:
               setStatus(resCode, "Reset content");
               break;
            case SC_PARTIAL_CONTENT:
               setStatus(resCode, "Partial content");
               break;
            case SC_MULTIPLE_CHOICES:
               setStatus(resCode, "Multiple choices");
               break;
            case SC_MOVED_PERMANENTLY:
               setStatus(resCode, "Moved permanentently");
               break;
            case SC_MOVED_TEMPORARILY:
               setStatus(resCode, "Moved temporarily");
               break;
            case SC_SEE_OTHER:
               setStatus(resCode, "See other");
               break;
            case SC_NOT_MODIFIED:
               setStatus(resCode, "Not modified");
               break;
            case SC_USE_PROXY:
               setStatus(resCode, "Use proxy");
               break;
            case SC_BAD_REQUEST:
               setStatus(resCode, "Bad request");
               break;
            case SC_UNAUTHORIZED:
               setStatus(resCode, "Unauthorized");
               break;
            case SC_PAYMENT_REQUIRED:
               setStatus(resCode, "Payment required");
               break;
            case SC_FORBIDDEN:
               setStatus(resCode, "Forbidden");
               break;
            case SC_NOT_FOUND:
               setStatus(resCode, "Not found");
               break;
            case SC_METHOD_NOT_ALLOWED:
               setStatus(resCode, "Method not allowed");
               break;
            case SC_NOT_ACCEPTABLE:
               setStatus(resCode, "Not acceptable");
               break;
            case SC_PROXY_AUTHENTICATION_REQUIRED:
               setStatus(resCode, "Proxy auth required");
               break;
            case SC_REQUEST_TIMEOUT:
               setStatus(resCode, "Request timeout");
               break;
            case SC_CONFLICT:
               setStatus(resCode, "Conflict");
               break;
            case SC_GONE:
               setStatus(resCode, "Gone");
               break;
            case SC_LENGTH_REQUIRED:
               setStatus(resCode, "Length required");
               break;
            case SC_PRECONDITION_FAILED:
               setStatus(resCode, "Precondition failed");
               break;
            case SC_REQUEST_ENTITY_TOO_LARGE:
               setStatus(resCode, "Request entity too large");
               break;
            case SC_REQUEST_URI_TOO_LONG:
               setStatus(resCode, "Request URI too long");
               break;
            case SC_UNSUPPORTED_MEDIA_TYPE:
               setStatus(resCode, "Unsupported media type");
               break;
            case SC_INTERNAL_SERVER_ERROR:
               setStatus(resCode, "Internal server error");
               break;
            case SC_NOT_IMPLEMENTED:
               setStatus(resCode, "Not implemented");
               break;
            case SC_BAD_GATEWAY:
               setStatus(resCode, "Bad gateway");
               break;
            case SC_SERVICE_UNAVAILABLE:
               setStatus(resCode, "Service unavailable");
               break;
            case SC_GATEWAY_TIMEOUT:
               setStatus(resCode, "Gateway timeout");
               break;
            case SC_HTTP_VERSION_NOT_SUPPORTED:
               setStatus(resCode, "HTTP version not supported");
               break;
            case 207:
               setStatus(resCode, "Multi Status");
               break;
            default:
               setStatus(resCode, "");
               break;
         }
      }

      // / Sets the value of a header field.
      // @param name the header field name
      // @param value the header field value
      public void setHeader(String header, String value)
      {
         header = header.trim().toLowerCase(); // normilize header
         if (value == null)
            resHeaderNames.remove(header);
         else
         {
            resHeaderNames.put(header, value);
            //if (header.equals(CONTENTTYPE)) {
            //	String enc = extractEncodingFromContentType(value);
            //	if (enc != null)
            //		setCharacterEncoding(enc);
            //}
         }
      }

      // / Sets the value of an integer header field.
      // @param name the header field name
      // @param value the header field integer value
      public void setIntHeader(String header, int value)
      {
         setHeader(header, Integer.toString(value));
      }

      // / Sets the value of a long header field.
      // @param name the header field name
      // @param value the header field long value
      public void setLongHeader(String header, long value)
      {
         setHeader(header, Long.toString(value));
      }

      // / Sets the value of a date header field.
      // @param name the header field name
      // @param value the header field date value
      public void setDateHeader(String header, long value)
      {
         setHeader(header, headerdateformat.format(new Date(value)));
      }

      // / Writes the status line and message headers for this response to the
      // output stream.
      // @exception IOException if an I/O error has occurred
      void writeHeaders() throws IOException
      {
         synchronized (this)
         {
            // TODO: possible to write trailer when chunked out,
            // so chunked out should be global flag
            if (headersWritten)
               return;

            headersWritten = true;
         }
         if (reqMime)
         {
            boolean chunked_out = false;
            boolean wasContentLen = false;
            if (resMessage.length() < 256)
               out.println(reqProtocol + " " + resCode + " " + resMessage.replace('\r', '/').replace('\n', '/'));
            else
               out.println(reqProtocol + " " + resCode + " "
                       + resMessage.substring(0, 255).replace('\r', '/').replace('\n', '/'));
            Enumeration he = resHeaderNames.keys();
            while (he.hasMoreElements())
            {
               String name = (String) he.nextElement();
               Object o = resHeaderNames.get(name);
               if (o instanceof String)
               {
                  String value = (String) o;
                  if (value != null)
                  {// just in case
                     out.println(name + ": " + value);
                     if (wasContentLen == false)
                        if (CONTENTLENGTH.equals(name))
                           try
                           {
                              wasContentLen = Long.parseLong(value) > 0;
                           }
                           catch (NumberFormatException nfe)
                           {
                           }
                     if (chunked_out == false)
                        if (TRANSFERENCODING.equals(name) && CHUNKED.equals(value))
                           chunked_out = true;
                  }
               }
               else if (o instanceof String[])
               {
                  String[] values = (String[]) o;
                  out.print(name + ": " + values[0]);
                  for (int i = 1; i < values.length; i++)
                     out.print("," + values[i]);
                  out.println();
               }
            }
            StringBuffer sb = null;
            StringBuffer sb2 = null;
            Cookie cc = null;
            // add session cookie
            if (sessionValue != null)
            {
               HttpSession session = serve.getSession(sessionValue);
               if (session != null)
               {
                  if (((AcmeSession) session).isValid())
                  {
                     if (session.isNew())
                     {
                        cc = new Cookie(SESSION_COOKIE_NAME, sessionValue);
                        if (serve.expiredIn < 0)
                           cc.setMaxAge(Math.abs(serve.expiredIn) * 60);
                        ServletContext sc = ((AcmeSession) session).getServletContext();
                        try
                        {
                           String cp = (String) sc.getClass().getMethod("getContextPath", Utils.EMPTY_CLASSES)
                                   .invoke(sc, Utils.EMPTY_OBJECTS);
                           if (cp.length() == 0)
                              cp = "/";
                           cc.setPath(cp);
                        }
                        catch (Exception e)
                        {

                        }

                        addCookie(cc);
                     }
                  }
                  else
                  {
                     cc = new Cookie(SESSION_COOKIE_NAME, "");
                     cc.setMaxAge(0);
                     addCookie(cc);
                  }
               }
            }

            // how to remove a cookie
            // cc = new Cookie(cookieName, "");
            // cc.setMaxAge(0);
            //
            for (int i = 0; outCookies != null && i < outCookies.size(); i++)
            {
               cc = (Cookie) outCookies.elementAt(i);
               if (cc.getSecure() && isSecure() == false)
                  continue;
               int version = cc.getVersion();
               String token;
               if (version > 1)
               {
                  if (sb2 == null)
                     sb2 = new StringBuffer(SETCOOKIE + "2: ");
                  else
                     sb2.append(',');
                  sb2.append(cc.getName());
                  sb2.append("=\"");
                  sb2.append(cc.getValue()).append('"');
                  token = cc.getComment();
                  if (token != null)
                     sb2.append("; Comment=\"").append(token).append('"');
                  token = cc.getDomain();
                  if (token != null)
                     sb2.append("; Domain=\"").append(token).append('"');
                  if (cc.getMaxAge() >= 0)
                     sb2.append("; Max-Age=\"").append(cc.getMaxAge()).append('"');
                  token = cc.getPath();
                  if (token != null)
                     sb2.append("; Path=\"").append(token).append('"');
                  if (cc.getSecure())
                  {
                     sb2.append("; Secure");
                  }
                  sb2.append("; Version=\"").append(version).append('"');
               }
               else
               {
                  if (sb == null)
                     sb = new StringBuffer(SETCOOKIE + ": ");
                  else
                     //sb.append(',');
                     sb.append("\r\n" + SETCOOKIE + ": "); // for IE not
                  sb.append(cc.getName());
                  sb.append('=');
                  sb.append(cc.getValue());//.append('"');
                  if (cc.getDomain() != null && cc.getDomain().length() > 0)
                  {
                     sb.append("; domain=" + cc.getDomain());
                  }
                  if (cc.getMaxAge() >= 0)
                  {
                     sb.append("; expires=");
                     sb.append(expdatefmt.format(new Date(new Date().getTime() + 1000l * cc.getMaxAge())));
                  }
                  if (cc.getPath() != null && cc.getPath().length() > 0)
                  {
                     sb.append("; path=" + cc.getPath());
                  }
                  if (cc.getSecure())
                  {
                     sb.append("; secure");
                  }
               }
            }
            if (sb != null)
            {
               out.println(sb.toString());
               //System.err.println("We sent cookies: " + sb);
            }
            if (sb2 != null)
            {
               out.println(sb2.toString());
               //System.err.println("We sent cookies 2: " + sb2);
            }
            // Test for existence of message body
            if (containsBody() && wasContentLen == false && chunked_out == false && serve.isKeepAlive())
            {
               out.println(TRANSFERENCODING + ": " + CHUNKED);
               chunked_out = true;
            }
            out.println();
            out.flush();
            ((ServeOutputStream) out).setChunked(chunked_out);
         }
      }

      private boolean containsBody()
      {
         return !("HEAD".equalsIgnoreCase(reqMethod) || (100 <= resCode && resCode < 200) || resCode == 204 || resCode == 304);
      }

      // / Writes an error response using the specified status code and
      // message.
      // @param resCode the status code
      // @param resMessage the status message
      // @exception IOException if an I/O error has occurred
      public void sendError(int resCode, String resMessage) throws IOException
      {
         setStatus(resCode, resMessage);
         realSendError();
      }

      // / Writes an error response using the specified status code and a
      // default
      // message.
      // @param resCode the status code
      // @exception IOException if an I/O error has occurred
      public void sendError(int resCode) throws IOException
      {
         setStatus(resCode);
         realSendError();
      }

      public void setInInclude(boolean set)
      {
         ((ServeOutputStream) out).setInInclude(set);
      }

      private void realSendError() throws IOException
      {
         if (isCommitted())
            throw new IllegalStateException("Can not send an error, headers have been already written");
         // if (((ServeOutputStream) out).isInInclude()) // ignore
         // return;
         setContentType("text/html");
         StringBuffer sb = new StringBuffer(100);
         int lsp = resMessage.indexOf('\n');
         sb.append("<HTML><HEAD>").append(
                 "<TITLE>" + resCode + " " + (lsp < 0 ? resMessage : resMessage.substring(0, lsp)) + "</TITLE>")
                 .append("</HEAD><BODY " + BGCOLOR).append(
                 "><H2>" + resCode + " " + (lsp < 0 ? resMessage : resMessage.substring(0, lsp)) + "</H2>");
         if (lsp > 0)
            sb.append("<PRE>").append(Utils.htmlEncode(resMessage.substring(lsp), false)).append("</PRE>");
         sb.append("<HR>");
         sb.append(Identification.serverIdHtml);
         sb.append("</BODY></HTML>");
         setContentLength(sb.length());
         out.print(sb.toString());
         closeStreams();
      }

      // / Sends a redirect message to the client using the specified redirect
      // location URL.
      // @param location the redirect location URL
      // @exception IOException if an I/O error has occurred
      public void sendRedirect(String location) throws IOException
      {
         if (isCommitted())
            throw new IllegalStateException("Can not redirect, headers have been already written");
         if (location.indexOf(":/") < 0)
         { // relative
            String portString = "";
            if ("https".equalsIgnoreCase(getScheme()))
            {
               if (getServerPort() != 443)
                  portString = ":" + getServerPort();
            }
            else if (getServerPort() != 80)
               portString = ":" + getServerPort();

            if (location.length() > 0 && location.charAt(0) == '/')
            {
               location = getScheme() + "://" + getServerName() + portString + location;
            }
            else
            {
               int sp = reqUriPathUn.lastIndexOf('/');
               String uri;
               if (sp < 0)
               {
                  uri = reqUriPathUn + '/';
                  sp = uri.length();
               }
               else
               {
                  uri = reqUriPathUn;
                  sp++;
               }
               location = getScheme() + "://" + getServerName() + portString + uri.substring(0, sp) + location;
            }
         }
         // serve.log("location:"+location);
         setHeader("Location", location);
         setStatus(SC_MOVED_TEMPORARILY);
         setContentType("text/html");
         StringBuffer sb = new StringBuffer(200);
         sb.append("<HTML><HEAD>" + "<TITLE>" + SC_MOVED_TEMPORARILY + " Moved</TITLE>" + "</HEAD><BODY " + BGCOLOR
                 + "><H2>" + SC_MOVED_TEMPORARILY + " Moved</H2>" + "This document has moved <a href=" + location
                 + ">here.<HR>");
         sb.append(Identification.serverIdHtml);
         sb.append("</BODY></HTML>");
         setContentLength(sb.length());
         // to avoid further out
         out.print(sb.toString());
         closeStreams();
      }

      // URL session-encoding stuff. Not implemented, but the API is here
      // for compatibility.

      // / Encodes the specified URL by including the session ID in it, or, if
      // encoding is not needed, returns the URL unchanged. The
      // implementation of this method should include the logic to determine
      // whether the session ID needs to be encoded in the URL. For example,
      // if the browser supports cookies, or session tracking is turned off,
      // URL encoding is unnecessary.
      // <P>
      // All URLs emitted by a Servlet should be run through this method.
      // Otherwise, URL rewriting cannot be used with browsers which do not
      // support cookies.
      // @deprecated

      public String encodeUrl(String url)
      {
         return encodeURL(url);
      }

      // / Encodes the specified URL for use in the sendRedirect method or, if
      // encoding is not needed, returns the URL unchanged. The
      // implementation of this method should include the logic to determine
      // whether the session ID needs to be encoded in the URL. Because the
      // rules for making this determination differ from those used to
      // decide whether to encode a normal link, this method is seperate
      // from the encodeUrl method.
      // <P>
      // All URLs sent to the HttpServletResponse.sendRedirect method should
      // be
      // run through this method. Otherwise, URL rewriting cannot be used with
      // browsers which do not support cookies.
      public String encodeRedirectUrl(String url)
      {
         return encodeRedirectURL(url);
      }

      public Socket getSocket()
      {
         // TODO apply security check
         return socket;
      }
   }

   protected static class BasicAuthRealm extends Hashtable
   {
      String name;

      BasicAuthRealm(String name)
      {
         this.name = name;
      }

      String name()
      {
         return name;
      }
   }

   public static class ServeInputStream extends ServletInputStream
   {
      private final static boolean STREAM_DEBUG = false;

      /**
       * The actual input stream (buffered).
       */
      private InputStream in, origIn;

      private ServeConnection conn;

      private int chunksize = 0;

      private boolean chunking = false, compressed;

      private boolean returnedAsReader, returnedAsStream;

      private long contentLength = -1;

      private long readCount;

      private byte[] oneReadBuf = new byte[1];

      private boolean closed;

      /* ------------------------------------------------------------ */
      /**
       * Constructor
       */
      public ServeInputStream(InputStream in, ServeConnection conn)
      {
         this.conn = conn;
         this.in = new BufferedInputStream(in);
      }

      void refresh()
      {
         returnedAsReader = false;
         returnedAsStream = false;
         contentLength = -1;
         readCount = 0;
         chunksize = 0;
         closed = false;
         compressed(false);
      }

      /* ------------------------------------------------------------ */
      /**
       * @param chunking
       */
      public void chunking(boolean chunking)
      {
         if (contentLength == -1)
            this.chunking = chunking;
      }

      boolean compressed(boolean on)
      {
         if (on)
         {
            if (compressed == false)
            {
               origIn = in;
               try
               {
                  ServeInputStream sis = new ServeInputStream(in, conn);
                  if (chunking)
                  {
                     sis.chunking(true);
                     chunking(false);
                  }
                  in = (InputStream) conn.serve.gzipInStreamConstr.newInstance(new Object[]{sis});
                  compressed = true;
                  //conn.serve.log("Compressed stream was created with success", null);
               }
               catch (Exception ex)
               {
                  if (ex instanceof InvocationTargetException)
                     conn.serve.log("Problem in compressed stream creation", ((InvocationTargetException) ex).getTargetException());
                  else
                     conn.serve.log("Problem in compressed stream obtaining", ex);
               }
            }
         }
         else if (compressed)
         {
            compressed = false;
            in = origIn;
         }
         return compressed;
      }

      /**
       * sets max read byte in input
       */
      void setContentLength(long contentLength)
      {
         if (this.contentLength == -1 && contentLength >= 0 && chunking == false)
         {
            this.contentLength = contentLength;
            readCount = 0;
         }
      }

      /* ------------------------------------------------------------ */
      /**
       * Read a line ended by CRLF, used internally only for reading headers.
       * No char encoding, ASCII only
       */
      protected String readLine(int maxLen) throws IOException
      {
         if (maxLen <= 0)
            throw new IllegalArgumentException("Max len:" + maxLen);
         StringBuffer buf = new StringBuffer(Math.min(1024, maxLen));

         int c;
         boolean cr = false;
         int i = 0;
         while ((c = in.read()) != -1)
         {
            if (c == 10)
            { // LF
               if (cr)
                  break;
               break;
               //throw new IOException ("LF without CR");
            }
            else if (c == 13) // CR
               cr = true;
            else
            {
               //if (cr)
               //throw new IOException ("CR without LF");
               // see http://www.w3.org/Protocols/HTTP/1.1/rfc2616bis/draft-lafon-rfc2616bis-03.html#tolerant.applications
               cr = false;
               if (i >= maxLen)
                  throw new IOException("Line lenght exceeds " + maxLen);
               buf.append((char) c);
               i++;
            }
         }
         if (STREAM_DEBUG)
            System.err.println(buf);
         if (c == -1 && buf.length() == 0)
            return null;

         return buf.toString();
      }

      /* ------------------------------------------------------------ */
      public int read() throws IOException
      {
         int result = read(oneReadBuf, 0, 1);
         if (result == 1)
            return 255 & oneReadBuf[0];
         return -1;
      }

      /* ------------------------------------------------------------ */
      public int read(byte b[]) throws IOException
      {
         return read(b, 0, b.length);
      }

      /* ------------------------------------------------------------ */
      public synchronized int read(byte b[], int off, int len) throws IOException
      {
         if (closed)
            throw new IOException("The stream is already closed");
         if (chunking)
         {
            if (chunksize <= 0 && getChunkSize() <= 0)
               return -1;
            if (len > chunksize)
               len = chunksize;
            len = in.read(b, off, len);
            chunksize = (len < 0) ? -1 : (chunksize - len);
         }
         else
         {
            if (contentLength >= 0)
            {
               if (contentLength - readCount < Integer.MAX_VALUE)

                  len = Math.min(len, (int) (contentLength - readCount));
               if (len <= 0)
               {
                  if (STREAM_DEBUG)
                     System.err.print("EOF");
                  return -1;
               }
               len = in.read(b, off, len);
               if (len > 0)
                  readCount += len;
            }
            else
               // to avoid extra if
               len = in.read(b, off, len);
         }
         if (STREAM_DEBUG && len > 0)
            System.err.print(new String(b, off, len));

         return len;
      }

      /* ------------------------------------------------------------ */
      public long skip(long len) throws IOException
      {
         if (STREAM_DEBUG)
            System.err.println("instream.skip() :" + len);
         if (closed)
            throw new IOException("The stream is already closed");
         if (chunking)
         {
            if (chunksize <= 0 && getChunkSize() <= 0)
               return -1;
            if (len > chunksize)
               len = chunksize;
            len = in.skip(len);
            chunksize = (len < 0) ? -1 : (chunksize - (int) len);
         }
         else
         {
            if (contentLength >= 0)
            {
               len = Math.min(len, contentLength - readCount);
               if (len <= 0)
                  return -1;
               len = in.skip(len);
               readCount += len;
            }
            else
               len = in.skip(len);
         }
         return len;
      }

      /* ------------------------------------------------------------ */
      /**
       * Available bytes to read without blocking. If you are unlucky may return 0 when there are more
       */
      public int available() throws IOException
      {
         if (STREAM_DEBUG)
            System.err.println("instream.available()");
         if (closed)
            throw new IOException("The stream is already closed");
         if (chunking)
         {
            int len = in.available();
            if (len <= chunksize)
               return len;
            return chunksize;
         }

         if (contentLength >= 0)
         {
            int len = in.available();
            if (contentLength - readCount < Integer.MAX_VALUE)
               return Math.min(len, (int) (contentLength - readCount));
            return len;
         }
         else
            return in.available();
      }

      /* ------------------------------------------------------------ */
      public void close() throws IOException
      {
         // keep alive, will be closed by socket
         // in.close();
         if (STREAM_DEBUG)
            System.err.println("instream.close() " + closed);
         if (closed)
            return; //throw new IOException("The stream is already closed");
         // read until end of chunks or content length
         if (chunking)
            while (read() >= 0) ;
         else if (contentLength < 0) ;
         else
         {
            long skipCount = contentLength - readCount;
            while (skipCount > 0)
            {
               long skipped = skip(skipCount);
               if (skipped <= 0)
                  break;
               skipCount -= skipped;
            }
         }
         if (conn.keepAlive == false)
            in.close();
         closed = true;
      }

      /* ------------------------------------------------------------ */
      /**
       * Mark is not supported
       *
       * @return false
       */
      public boolean markSupported()
      {
         return false;
      }

      /* ------------------------------------------------------------ */
      /**
       *
       */
      public void reset() throws IOException
      {
         // no buffering, so not possible
         if (closed)
            throw new IOException("The stream is already closed");
         if (STREAM_DEBUG)
            System.err.println("instream.reset()");
         in.reset();
      }

      /* ------------------------------------------------------------ */
      /**
       * Not Implemented
       *
       * @param readlimit
       */
      public void mark(int readlimit)
      {
         // not supported
         if (STREAM_DEBUG)
            System.err.println("instream.mark(" + readlimit + ")");
      }

      /* ------------------------------------------------------------ */
      private int getChunkSize() throws IOException
      {
         if (chunksize < 0)
            return -1;

         chunksize = -1;

         // Get next non blank line
         chunking = false;
         String line = readLine(60);
         while (line != null && line.length() == 0)
            line = readLine(60);
         chunking = true;

         // Handle early EOF or error in format
         if (line == null)
            return -1;

         // Get chunksize
         int i = line.indexOf(';');
         if (i > 0)
            line = line.substring(0, i).trim();
         try
         {
            chunksize = Integer.parseInt(line, 16);
         }
         catch (NumberFormatException nfe)
         {
            throw new IOException("Chunked stream is broken, " + line);
         }

         // check for EOF
         if (chunksize == 0)
         {
            chunksize = -1;
            // Look for footers
            readLine(60);
            chunking = false;
         }
         return chunksize;
      }

      boolean isReturnedAsStream()
      {
         return returnedAsStream;
      }

      void setReturnedAsStream(boolean _on)
      {
         returnedAsStream = _on;
      }

      boolean isReturnedAsReader()
      {
         return returnedAsReader;
      }

      void setReturnedAsReader(boolean _on)
      {
         returnedAsReader = _on;
      }
   }

   public static class ServeOutputStream extends ServletOutputStream
   {

      private static final boolean STREAM_DEBUG = false;

      private boolean chunked;

      private boolean closed;

      // TODO: predefine as static byte[] used by chunked
      // underneath stream
      private OutputStream out;

      // private BufferedWriter writer; // for top speed
      private ServeConnection conn;

      private int inInclude;

      private String encoding;

      private/*volatile*/ long lbytes;

      private Utils.SimpleBuffer buffer;

      public ServeOutputStream(OutputStream out, ServeConnection conn)
      {
         this.out = out;
         this.conn = conn;
         buffer = new Utils.SimpleBuffer();
         encoding = conn.getCharacterEncoding();
         if (encoding == null)
            encoding = Utils.ISO_8859_1;
      }

      void refresh()
      {
         chunked = false;
         closed = false;
         inInclude = 0;
         lbytes = 0;
         buffer.reset();
         encoding = conn.getCharacterEncoding();
         if (encoding == null)
            encoding = Utils.ISO_8859_1;
      }

      protected void reset()
      {
         if (lbytes == 0)
            buffer.reset();
         else
            throw new IllegalStateException("Result was already committed");
      }

      protected int getBufferSize()
      {
         return buffer.getSize();
      }

      protected void setBufferSize(int size)
      {
         if (lbytes > 0)
            throw new IllegalStateException("Bytes already written in response");
         buffer.setSize(size);
      }

      protected void setChunked(boolean set)
      {
         chunked = set;
      }

      public void print(String s) throws IOException
      {
         write(s.getBytes(encoding));
      }

      public void write(int b) throws IOException
      {
         write(new byte[]{(byte) b}, 0, 1);
      }

      public void write(byte[] b) throws IOException
      {
         write(b, 0, b.length);
      }

      public void write(byte[] b, int off, int len) throws IOException
      {
         if (closed)
         {
            if (STREAM_DEBUG)
               System.err.println((b == null ? "null" : new String(b, off, len)) + "\n won't be written, stream closed.");
            throw new IOException("An attempt of writing " + len + " bytes to a closed out.");
         }

         if (len == 0)
            return;
         //
         conn.writeHeaders();
         b = buffer.put(b, off, len);
         len = b.length;
         if (len == 0)
            return;
         off = 0;
         if (chunked)
         {
            String hexl = Integer.toHexString(len);
            out.write((hexl + "\r\n").getBytes()); // no encoding Ok
            lbytes += 2 + hexl.length();
            out.write(b, off, len);
            lbytes += len;
            out.write("\r\n".getBytes());
            lbytes += 2;
         }
         else
         {
            out.write(b, off, len);
            lbytes += len;
         }

         if (STREAM_DEBUG)
         {
            if (chunked)
               System.err.println(Integer.toHexString(len));
            System.err.print(new String(b, off, len));
            if (chunked)
               System.err.println();
         }
      }

      public void flush() throws IOException
      {
         if (closed)
            return;
         // throw new IOException("An attempt of flushig closed out.");
         conn.writeHeaders();
         byte[] b = buffer.get();
         if (b.length > 0)
         {
            if (chunked)
            {
               String hexl = Integer.toHexString(b.length);
               out.write((hexl + "\r\n").getBytes()); // no encoding Ok
               lbytes += 2 + hexl.length();
               out.write(b);
               lbytes += b.length;
               out.write("\r\n".getBytes());
               lbytes += 2;
               if (STREAM_DEBUG)
               {
                  System.err.println(hexl);
                  System.err.print(new String(b));
                  System.err.println();
               }
            }
            else
            {
               out.write(b);
               lbytes += b.length;
               if (STREAM_DEBUG)
               {
                  System.err.print(new String(b));
               }
            }
         }
         out.flush();
      }

      public void close() throws IOException
      {
         if (closed)
            return;
         // throw new IOException("Stream is already closed.");
         // new IOException("Stream closing").printStackTrace();
         try
         {
            flush();
            if (inInclude == 0)
            {
               if (chunked)
               {
                  out.write("0\r\n\r\n".getBytes());
                  lbytes += 5;
                  if (STREAM_DEBUG)
                     System.err.print("0\r\n\r\n");
                  // TODO: here is possible to write trailer headers
                  out.flush();
               }
               if (conn.keepAlive == false)
                  out.close();
            }
         }
         finally
         {
            closed = true;
         }
      }

      private long lengthWritten()
      {
         return lbytes;
      }

      boolean isInInclude()
      {
         return inInclude == 0;
      }

      void setInInclude(boolean _set)
      {
         inInclude = _set ? 1 : 0;
         /*if (_set)
               inInclude++;
            else
               inInclude--;
            if (inInclude < 0)
               throw new IllegalStateException("Not matching include set");*/
      }
   }

   /**
    * Class PathTreeDictionary - this class allows to put path elements in format n1/n2/n2[/*.ext] and get match to a pattern and a unmatched tail
    */
   public static class PathTreeDictionary
   {
      Node root_node;

      public PathTreeDictionary()
      {
         root_node = new Node();
      }

      public synchronized void put(String path, Object value)
      {
         StringTokenizer st = new StringTokenizer(path, "\\/");
         Node cur_node = root_node;
         while (st.hasMoreTokens())
         {
            String nodename = st.nextToken();
            Node node = (Node) cur_node.get(nodename);
            if (node == null)
            {
               node = new Node();
               cur_node.put(nodename, node);
            }
            cur_node = node;
         }
         cur_node.object = value;
      }

      public synchronized Object[] remove(Object value)
      {
         return remove(root_node, value);
      }

      public synchronized Object[] remove(String path)
      {
         Object[] result = get(path);
         if (result[1] != null)
            return remove(result[1]);
         return result;
      }

      public Object[] remove(Node node, Object value)
      {
         // TODO make full path, not only last element
         Enumeration e = node.keys();
         while (e.hasMoreElements())
         {
            String path = (String) e.nextElement();
            Node childNode = (Node) node.get(path);
            if (childNode.object == value)
            {// it's safe because the same instance can't be shared for several paths in this design
               childNode.object = null;
               return new Object[]{value, new Integer(0)};
            }
            Object[] result = remove(childNode, value);
            if (result[0] != null)
               return result;
         }

         return new Object[]{null, null};
      }

      /**
       * This function looks up in the directory to find the perfect match and remove matching part from path, so if you need to keep original path, save it
       * somewhere
       */
      public Object[] get(String path)
      {
         Object[] result = new Object[2];
         if (path == null)
            return result;
         char[] ps = path.toCharArray();
         Node cur_node = root_node;
         int p0 = 0, lm = 0; // last match
         result[0] = cur_node.object;
         boolean div_state = true;
         for (int i = 0; i < ps.length; i++)
         {
            if (ps[i] == '/' || ps[i] == '\\')
            {
               if (div_state)
                  continue;
               Node node = (Node) cur_node.get(new String(ps, p0, i - p0));
               if (node == null)
               {
                  result[1] = new Integer(lm);
                  return result;
               }
               if (node.object != null)
               {
                  result[0] = node.object;
                  lm = i;
               }
               cur_node = node;
               div_state = true;
            }
            else
            {
               if (div_state)
               {
                  p0 = i;
                  div_state = false;
               }
            }
         }
         cur_node = (Node) cur_node.get(new String(ps, p0, ps.length - p0));
         if (cur_node != null && cur_node.object != null)
         {
            result[0] = cur_node.object;
            lm = ps.length;
         }
         result[1] = new Integer(lm);
         return result;
      }

      public Enumeration keys()
      {
         Vector result = new Vector();
         addSiblingNames(root_node, result, "");
         return result.elements();
      }

      public void addSiblingNames(Node node, Vector result, String path)
      {
         Enumeration e = node.keys();
         while (e.hasMoreElements())
         {
            String pc = (String) e.nextElement();
            Node childNode = (Node) node.get(pc);
            pc = path + '/' + pc;
            if (childNode.object != null)
               result.addElement(pc);
            addSiblingNames(childNode, result, pc);
         }
      }

      public Enumeration elements()
      {
         Vector result = new Vector();
         addSiblingObjects(root_node, result);
         return result.elements();
      }

      public void addSiblingObjects(Node node, Vector result)
      {
         Enumeration e = node.keys();
         while (e.hasMoreElements())
         {
            Node childNode = (Node) node.get(e.nextElement());
            if (childNode.object != null)
               result.addElement(childNode.object);
            addSiblingObjects(childNode, result);
         }
      }

      class Node extends Hashtable
      {
         Object object;
      }
   }

   /**
    * Http session support
    * <p/>
    * TODO: provide lazy session restoring, it should allow to load classes from wars 1st step it read serialization data and store under session attribute 2nd
    * when the session requested, it tries to deserialize all session attributes considered that all classes available
    */
   public static class AcmeSession extends Hashtable implements HttpSession
   {
      private long createTime;

      private long lastAccessTime;

      private String id;

      private int inactiveInterval; // in seconds

      private boolean expired;

      private transient ServletContext servletContext;

      private transient HttpSessionContext sessionContext;

      private transient List listeners;

      // TODO: check in documentation what is default inactive interval and
      // what
      // means 0
      // and what is mesurement unit
      AcmeSession(String id, ServletContext servletContext, HttpSessionContext sessionContext)
      {
         this(id, 0, servletContext, sessionContext);
      }

      AcmeSession(String id, int inactiveInterval, ServletContext servletContext, HttpSessionContext sessionContext)
      {
         // new Exception("Session created with: "+servletContext).printStackTrace(); //!!!
         createTime = System.currentTimeMillis();
         this.id = id;
         this.inactiveInterval = inactiveInterval;
         this.servletContext = servletContext;
         this.sessionContext = sessionContext;
      }

      public long getCreationTime()
      {
         return createTime;
      }

      public String getId()
      {
         return id;
      }

      public long getLastAccessedTime()
      {
         return lastAccessTime;
      }

      public void setMaxInactiveInterval(int interval)
      {
         inactiveInterval = interval;
      }

      public int getMaxInactiveInterval()
      {
         return inactiveInterval;
      }

      /**
       * @deprecated
       */
      public HttpSessionContext getSessionContext()
      {
         return sessionContext;
      }

      /**
       * Returns the ServletContext to which this session belongs.
       *
       * @return The ServletContext object for the web application
       * @ince 2.3
       */
      public ServletContext getServletContext()
      {
         // System.err.println("ctx from:"+servletContext); //!!!
         return servletContext;
      }

      public Object getAttribute(String name) throws IllegalStateException
      {
         if (expired)
            throw new IllegalStateException();
         return get((Object) name);
      }

      public Object getValue(String name) throws IllegalStateException
      {
         return getAttribute(name);
      }

      public Enumeration getAttributeNames() throws IllegalStateException
      {
         if (expired)
            throw new IllegalStateException();
         return keys();
      }

      public String[] getValueNames() throws IllegalStateException
      {
         Enumeration e = getAttributeNames();
         Vector names = new Vector();
         while (e.hasMoreElements())
            names.addElement(e.nextElement());
         String[] result = new String[names.size()];
         names.copyInto(result);
         return result;
      }

      public void setAttribute(String name, Object value) throws IllegalStateException
      {
         if (expired)
            throw new IllegalStateException();
         Object oldValue = value != null ? put((Object) name, value) : remove(name);
         if (oldValue != null)
            if (oldValue instanceof HttpSessionBindingListener)
               ((HttpSessionBindingListener) oldValue).valueUnbound(new HttpSessionBindingEvent(this, name));
            else if (oldValue instanceof HttpSessionAttributeListener)
               ((HttpSessionAttributeListener) oldValue).attributeReplaced(new HttpSessionBindingEvent(this, name,
                       value));
         if (value instanceof HttpSessionBindingListener)
            ((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name));
         else if (value instanceof HttpSessionAttributeListener)
            ((HttpSessionAttributeListener) value).attributeAdded(new HttpSessionBindingEvent(this, name));
      }

      public void putValue(String name, Object value) throws IllegalStateException
      {
         setAttribute(name, value);
      }

      public void removeAttribute(String name) throws IllegalStateException
      {
         if (expired)
            throw new IllegalStateException();
         Object value = remove((Object) name);
         if (value != null)
            if (value instanceof HttpSessionBindingListener)
               ((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name));
            else if (value instanceof HttpSessionAttributeListener)
               ((HttpSessionAttributeListener) value).attributeRemoved(new HttpSessionBindingEvent(this, name));
      }

      public void removeValue(String name) throws IllegalStateException
      {
         removeAttribute(name);
      }

      public synchronized void invalidate() throws IllegalStateException
      {
         if (expired)
            throw new IllegalStateException();
         notifyListeners();
         Enumeration e = getAttributeNames();
         while (e.hasMoreElements())
         {
            removeAttribute((String) e.nextElement());
         }
         setExpired(true);
         // would be nice remove it from hash table also
      }

      public boolean isNew() throws IllegalStateException
      {
         if (expired)
            throw new IllegalStateException();
         return lastAccessTime == 0;
      }

      public synchronized void setListeners(List l)
      {
         if (listeners == null)
         {
            listeners = l;
            if (listeners != null)
            {
               HttpSessionEvent event = new HttpSessionEvent(this);
               for (int i = 0; i < listeners.size(); i++)
                  try
                  {
                     ((HttpSessionListener) listeners.get(0)).sessionCreated(event);
                  }
                  catch (ClassCastException cce)
                  {
                     // log("Wrong session listener type."+cce);
                  }
                  catch (NullPointerException npe)
                  {
                     // log("Null session listener.");
                  }
            }
         }
      }

      /**
       * something hack, to update servlet context since session created out of scope
       *
       * @param sc
       */
      public synchronized void setServletContext(ServletContext sc)
      {
         // System.err.println("ctx to:"+servletContext); //!!!
         servletContext = sc;
      }

      private void notifyListeners()
      {
         if (listeners != null)
         {
            HttpSessionEvent event = new HttpSessionEvent(this);
            for (int i = 0; i < listeners.size(); i++)
               try
               {
                  ((HttpSessionListener) listeners.get(i)).sessionDestroyed(event);
               }
               catch (ClassCastException cce)
               {
                  // log("Wrong session listener type."+cce);
               }
               catch (NullPointerException npe)
               {
                  // log("Null session listener.");
               }
         }
      }

      private void setExpired(boolean expired)
      {
         this.expired = expired;
      }

      boolean isValid()
      {
         return !expired;
      }

      boolean checkExpired()
      {
         return inactiveInterval > 0 && (inactiveInterval * 1000 < System.currentTimeMillis() - lastAccessTime);
      }

      void userTouch()
      {
         if (isValid())
            lastAccessTime = System.currentTimeMillis();
         else
            throw new IllegalStateException();
      }

      // storing session in format
      // id:latency:contextname:tttt
      // entry:base64 ser data
      // entry:base64 ser data
      // $$
      void save(Writer w) throws IOException
      {
         if (expired)
            return;
         // can't use append because old JDK
         w.write(id);
         w.write(':');
         w.write(Integer.toString(inactiveInterval));
         w.write(':');
         w.write(servletContext == null || servletContext.getServletContextName() == null ? "" : servletContext
                 .getServletContextName());
         w.write(':');
         w.write(Long.toString(lastAccessTime));
         w.write("\r\n");
         Enumeration e = getAttributeNames();
         ByteArrayOutputStream os = new ByteArrayOutputStream(1024 * 16);
         while (e.hasMoreElements())
         {
            String aname = (String) e.nextElement();
            Object so = get(aname);
            if (so instanceof Serializable)
            {
               os.reset();
               ObjectOutputStream oos = new ObjectOutputStream(os);
               try
               {
                  oos.writeObject(so);
                  w.write(aname);
                  w.write(":");
                  w.write(Utils.base64Encode(os.toByteArray()));
                  w.write("\r\n");
               }
               catch (IOException ioe)
               {
                  servletContext.log("Problem storing a session value of " + aname, ioe);
               }
            }
            else
               servletContext.log("Non serializable object " + so.getClass().getName() + " skiped in storing of " + aname, null);
            if (so instanceof HttpSessionActivationListener)
               ((HttpSessionActivationListener) so).sessionWillPassivate(new HttpSessionEvent(this));
         }
         w.write("$$\r\n");
      }

      static AcmeSession restore(BufferedReader r, int inactiveInterval, ServletContext servletContext,
                                 HttpSessionContext sessionContext) throws IOException
      {
         String s = r.readLine();
         if (s == null) // eos
            return null;
         int cp = s.indexOf(':');
         if (cp < 0)
            throw new IOException("Invalid format for a session header, no session id: " + s);
         String id = s.substring(0, cp);
         int cp2 = s.indexOf(':', cp + 1);
         if (cp2 < 0)
            throw new IOException("Invalid format for a session header, no latency: " + s);
         try
         {
            inactiveInterval = Integer.parseInt(s.substring(cp + 1, cp2));
         }
         catch (NumberFormatException nfe)
         {
            servletContext.log("Session latency is invalid:" + s.substring(cp + 1, cp2) + " " + nfe);
         }
         cp = s.indexOf(':', cp2 + 1);
         if (cp < 0)
            throw new IOException("Invalid format for a session header, context name: " + s);
         String contextName = s.substring(cp2 + 1, cp);
         // consider servletContext.getContext("/"+contextName)
         AcmeSession result = new AcmeSession(id, inactiveInterval, contextName.length() == 0 ? servletContext
                 : null, sessionContext);
         try
         {
            result.lastAccessTime = Long.parseLong(s.substring(cp + 1));
         }
         catch (NumberFormatException nfe)
         {
            servletContext.log("Last access time is invalid:" + s.substring(cp + 1) + " " + nfe);
         }
         do
         {
            s = r.readLine();
            if (s == null)
               throw new IOException("Unexpected end of a stream.");
            if ("$$".equals(s))
               return result;
            cp = s.indexOf(':');
            if (cp < 0)
               throw new IOException("Invalid format for a session entry: " + s);
            String aname = s.substring(0, cp);
            // if (lazyRestore)
            // result.put(aname, s.substring(cp+1));
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Utils.decode64(s
                    .substring(cp + 1))));
            Throwable restoreError;
            try
            {
               Object so;
               result.put(aname, so = ois.readObject());
               restoreError = null;
               if (so instanceof HttpSessionActivationListener)
                  ((HttpSessionActivationListener) so).sessionDidActivate(new HttpSessionEvent(result));

            }
            catch (ClassNotFoundException cnfe)
            {
               restoreError = cnfe;

            }
            catch (NoClassDefFoundError ncdfe)
            {
               restoreError = ncdfe;
            }
            catch (IOException ioe)
            {
               restoreError = ioe;
            }
            if (restoreError != null)
               servletContext.log("Can't restore :" + aname + ", " + restoreError);
         } while (true);
      }
   }

   protected static class LocaleWithWeight implements Comparable
   {
      protected float weight; // should be int

      protected Locale locale;

      LocaleWithWeight(Locale l, float w)
      {
         locale = l;
         weight = w;
         // System.err.println("Created "+l+", with:"+w);
      }

      public int compareTo(Object o)
      {
         if (o instanceof LocaleWithWeight)
            return (int) (((LocaleWithWeight) o).weight - weight) * 100;
         throw new IllegalArgumentException();
      }

      public Locale getLocale()
      {
         return locale;
      }
   }

   protected static class AcceptLocaleEnumeration implements Enumeration
   {
      Iterator i;

      public AcceptLocaleEnumeration(TreeSet/* <LocaleWithWeight> */ts)
      {
         i = ts.iterator();
      }

      public boolean hasMoreElements()
      {
         return i.hasNext();
      }

      public Object nextElement()
      {
         return ((LocaleWithWeight) i.next()).getLocale();
         /*
             * Locale l =((LocaleWithWeight)i.next()).getLocale(); System.err.println("Returned l:"+l); return l;
             */
      }
   }

   // TODO: reconsider implementation by providing
   // inner class implementing HttpSessionContext
   // and returning it on request
   // to avoid casting this class to Hashtable

   protected static class HttpSessionContextImpl extends Hashtable implements HttpSessionContext
   {

      public Enumeration getIds()
      {
         return keys();
      }

      public HttpSession getSession(String sessionId)
      {
         return (HttpSession) get(sessionId);
      }

      void save(Writer w) throws IOException
      {
         Enumeration e = elements();
         while (e.hasMoreElements())
            ((AcmeSession) e.nextElement()).save(w);
      }

      static HttpSessionContextImpl restore(BufferedReader br, int inactiveInterval, ServletContext servletContext)
              throws IOException
      {
         HttpSessionContextImpl result = new HttpSessionContextImpl();
         AcmeSession session;
         while ((session = AcmeSession.restore(br, inactiveInterval, servletContext, result)) != null)
            if (session.checkExpired() == false)
               result.put(session.getId(), session);
         return result;
      }
   }
}