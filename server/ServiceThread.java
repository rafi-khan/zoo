import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class ServiceThread extends Thread {
	private ServerSocket welcomeSocket;
	private BufferedReader inFromClient;
	private DataOutputStream outToClient;
	
	/* Static variables */
	
	public ServiceThread(ServerSocket welcomeSocket, Map<String, String> updateTable) {
		this.welcomeSocket = welcomeSocket;
	}
	public void run() {
		System.out.println(this + " started.");
		while (true) {
			// Get a new request connection
			Socket s = null;
			synchronized (welcomeSocket) {
				try {
					s = welcomeSocket.accept();
					System.out.println("Thread "+this+" process request "+s);
				} catch (IOException e) {
				}
			}
			processRequest(s);
			
		}
	}
	private void processRequest(Socket connSock) {
		boolean html = false;
		try {
			// Create read stream to get input
			inFromClient = new BufferedReader(new InputStreamReader(connSock.getInputStream()));
			outToClient = new DataOutputStream(connSock.getOutputStream());
			// Map to store parameters
			Map<String, String> map = new HashMap<String, String>();
			
		    String query = inFromClient.readLine();
		    if(query == null) {
		    	outputError(404, "Path was null!");
		    	connSock.close();
		    }
		    System.out.println("Query: "+query);
		    String[] request = query.split("\\s");
		    if (request.length < 2 || !request[0].equals("GET")) {
			    outputError(500, "Bad request");
			    connSock.close();
			    return;
		    }
		    String[] path = request[1].split("\\?");
		    String action = path[0];
		    if (path.length > 1) {
			    String[] params = path[1].split("&");
			    for (String param : params) {
			    	String name = param.split("=")[0];
			    	String value = param.split("=")[1];
			    	map.put(name, value);
			    } 
		    }
		    // To get a URL variable: 
		    // String variable = map.get("varName");
		    String outputString = "Nothing happened!";
		    if(action.equals("/zoo")) {
		    	try{
            // Runtime rt = Runtime.getRuntime();
            // Process proc = rt.exec("ruby hzw.rb");
            // int exitVal = proc.waitFor();
            // System.out.println("Exit val: "+exitVal);
		    		outputString = Exec.execute("./hzw.rb");
		    		System.out.println("Got output string!");
		    	} catch (Exception e) {
		    		e.printStackTrace();
		    		outputError(500, "Internal Server Error...");
		    		connSock.close();
		    		return;
		    	}
		    } else {
		    	System.out.println("Path: "+ action);
		    	outputError(404, "No such path.");
		    	connSock.close();
		    	return;
		    } 
		    outputResponseHeader();
		    // outputHTMLHeader();
		    outputResponseBody(outputString);
		    System.out.println("Serving "+action+"...");
		    connSock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String readFile(String path) throws IOException {
		  FileInputStream stream = new FileInputStream(new File(path));
		  try {
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.defaultCharset().decode(bb).toString();
		  }
		  finally {
		    stream.close();
		  }
	}
	
	private static byte[] readByteFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[1024];
		int read = 0;
		while((read = stream.read(buff)) > 0) {
			baos.write(buff, 0, read);
		}
		baos.flush();
		stream.close();
		return baos.toByteArray();
	}

	private void outputResponseHeader() throws Exception {
		String string = "HTTP/1.0 200 Document Follows\r\n";
		string += "Access-Control-Allow-Origin: *\r\n";
		outToClient.writeBytes(string);
	}
	private void outputHTMLHeader() throws Exception {
		outToClient.writeBytes("Content-Type: text/html; charset=utf-8\r\n");
	}
	private void outputAudio(String file) throws Exception {
		outputResponseHeader();
		outToClient.writeBytes("Content-Type: audio/mpeg\r\n");
		byte[] toWrite = readByteFile(file);
		outToClient.writeBytes("Content-Length: " + toWrite.length + "\r\n");
		outToClient.write(toWrite);
	}
	private void outputResponseBody(String out) throws Exception {
		outToClient.writeBytes("Content-Length: " + out.length() + "\r\n");
		outToClient.writeBytes("\r\n");
		outToClient.writeBytes(out);
	}
	void outputError(int errCode, String errMsg) {
		try {
			outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg
					+ "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/* START JAVA EXECUTE CODE */
	/**
   * Asynchronously read the output of a given input stream. 
   * Any exception during execution of the command in managed in this thread.
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  public static class StreamGobbler extends Thread
  {
    private InputStream is;
    private String type;
    private StringBuffer output = new StringBuffer();

    StreamGobbler(final InputStream anIs, final String aType)
    {
      this.is = anIs;
      this.type = aType;
    }

    /**
     * Asynchronous read of the input stream. 
     * Will report output as its its displayed.
     * @see java.lang.Thread#run()
     */
    @Override
    public final void run()
    {
      try
      {
        final InputStreamReader isr = new InputStreamReader(this.is);
        final BufferedReader br = new BufferedReader(isr);
        String line=null;
        while ( (line = br.readLine()) != null)
        {
          this.output.append(line+"\n");
        }
      } catch (final IOException ioe)
      {
        ioe.printStackTrace();  
      }
    }
    /**
     * Get output filled asynchronously. 
     * Should be called after execution
     * @return final output
     */
    public final String getOutput()
    {
      return this.output.toString();
    }
  }
  /**
   * Execute a system command in the appropriate shell. 
   * Read asynchronously stdout and stderr to report any result.
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  public static final class Exec
  {
    /**
     * Execute a system command. 
     * Listen asynchronously to stdout and stderr
     * @param aCommand system command to be executed (must not be null or empty)
     * @param someParameters parameters of the command (must not be null or empty)
     * @return final output (stdout only)
     */
    public static String execute(final String aCommand, final String... someParameters) throws Exception
    {
      String output = "";
      try {     
        ExecEnvironmentFactory anExecEnvFactory = getExecEnvironmentFactory(aCommand, someParameters);
        final IShell aShell = anExecEnvFactory.createShell();
        final String aCommandLine = anExecEnvFactory.createCommandLine();

        final Runtime rt = Runtime.getRuntime();
        System.out.println("Exec-ing " + aShell.getShellCommand() + " " + aCommandLine);

        final Process proc = rt.exec(aShell.getShellCommand() + " " + aCommandLine);
        // any error message?
        final StreamGobbler errorGobbler = new 
          StreamGobbler(proc.getErrorStream(), "ERROR");      

        // any output?
        final StreamGobbler outputGobbler = new 
          StreamGobbler(proc.getInputStream(), "OUTPUT");

        // kick them off
        errorGobbler.start();
        outputGobbler.start();
        // any error???
        final int exitVal = proc.waitFor(); 
        output = outputGobbler.getOutput();
      } catch (final Throwable t) {
        t.printStackTrace();
      }
      return output;
    }
    
    private static ExecEnvironmentFactory getExecEnvironmentFactory(final String aCommand, final String... someParameters)
    {
      return new UnixExecEnvFactory(aCommand, someParameters);
    }
  }
  
  /*
   * ABSTRACT FACTORY PATTERN
   */
  /**
   * Environment needed to be build for the Exec class to be able to execute the system command. 
   * Must have the right shell and the right command line. 
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  public abstract static class ExecEnvironmentFactory
  {
    private String command = null;
    private ArrayList<String> parameters = new ArrayList<String>();
    final String getCommand() { return this.command; }
    final ArrayList<String> getParameters() { return this.parameters; }
    /**
     * Builds an execution environment for a system command to be played. 
     * Independent from the OS.
     * @param aCommand system command to be executed (must not be null or empty)
     * @param someParameters parameters of the command (must not be null or empty)
     */
    public ExecEnvironmentFactory(final String aCommand, final String... someParameters)
    {
      if(aCommand == null || aCommand.length() == 0) { throw new IllegalArgumentException("Command must not be empty"); }
      this.command = aCommand;
      for (int i = 0; i < someParameters.length; i++) {
        final String aParameter = someParameters[i];
        if(aParameter == null || aParameter.length() == 0) { throw new IllegalArgumentException("Parameter nÂ° '"+i+"' must not be empty"); }
        this.parameters.add(aParameter);
      }
    }
    /**
     * Builds the right Shell for the current OS. 
     * Allow for independent platform execution.
     * @return right shell, NEVER NULL
     */
    public abstract IShell createShell();
    /**
     * Builds the right command line for the current OS. 
     * Means that a command might be translated, if it does not fit the right OS ('dir' => 'ls' on unix)
     * @return  right complete command line, with parameters added (NEVER NULL)
     */
    public abstract String createCommandLine();
    
    protected final String buildCommandLine(final String aCommand, final ArrayList<String> someParameters)
    {
      final StringBuilder aCommandLine = new StringBuilder();
      aCommandLine.append(aCommand);
      for (String aParameter : someParameters) {
        aCommandLine.append(" ");
        aCommandLine.append(aParameter);
      }
      return aCommandLine.toString();
    }
  }
  
  /**
   * Builds a Execution Environment for Unix. 
   * Sh with Unix commands
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  public static final class UnixExecEnvFactory extends ExecEnvironmentFactory
  {

    /**
     * Builds an execution environment for a Unix system command to be played. 
     * Any command not from Unix will be translated in its Unix equivalent if possible.
     * @param aCommand system command to be executed (must not be null or empty)
     * @param someParameters parameters of the command (must not be null or empty)
     */
    public UnixExecEnvFactory(final String aCommand, final String... someParameters)
    {
      super(aCommand, someParameters);
    }
    /**
     * @see test.JavaSystemCaller.ExecEnvironmentFactory#createShell()
     */
    @Override
    public IShell createShell() {
      return new UnixShell();
    }

    /**
     * @see test.JavaSystemCaller.ExecEnvironmentFactory#createCommandLine()
     */
    @Override
    public String createCommandLine() {
      String aCommand = getCommand();
      return buildCommandLine(aCommand, getParameters());
    } 
  }
  
  /**
   * System Shell with its right OS command. 
   * 'cmd' for Windows or 'sh' for Unix, ...
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  public interface IShell
  {
    /**
     * Get the right shell command. 
     * Used to launch a new shell
     * @return command used to launch a Shell (NEVEL NULL)
     */
    String getShellCommand();
  }
  /**
   * Windows shell (cmd). 
   * More accurately 'cmd /C'
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  /**
   * Unix shell (sh). 
   * More accurately 'sh -C'
   * @author <a href="http://stackoverflow.com/users/6309/vonc">VonC</a>
   */
  public static class UnixShell implements IShell
  {
    /**
     * @see test.JavaSystemCaller.IShell#getShellCommand()
     */
    @Override
    public final String getShellCommand() {
      return "/bin/sh -c";
    }
  }
}

