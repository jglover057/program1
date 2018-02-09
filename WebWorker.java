/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{
private int notFound = 0;
private Socket socket;
String contentType = "text/html";
String path;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   String temp;
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();//start the input stream
      OutputStream os = socket.getOutputStream();//start the output stream
      temp = readHTTPRequest(is);//get the path of the file from readHTTPRequest
      writeHTTPHeader(os,contentType);//write the header
      if(contentType.contains("html")){
         String content = writeStuff(temp);//get the content of the file
         writeContent(os,content);
         }
      else if(contentType.contains("image")){
         SendImage(os);
         }
      os.flush();//push to page
      socket.close();//close socket
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

private String writeStuff(String location){//function I wrote to write the contents of the file.
   String end = "";
   String format;
   File answer = new File(location.substring(1));//take the location and create a file with it
  try{ 
      BufferedReader yes = new BufferedReader(new FileReader(answer));//BufferedReader to traverse the file

   while(yes.ready()){//while there is more in the file
      String look = yes.readLine();//take the current line
      
      if(look.contains("<cs371date>")){//see if the line contains the date tag
         Date newDate = new Date();//if so then create new date objecty
         DateFormat form = DateFormat.getDateTimeInstance();//Get the current date and time
         form.setTimeZone(TimeZone.getTimeZone("GMT"));//set the time zone to GMT
         format = form.format(newDate);
         end = end + format + "\n";//add to the end string which is what is being pushed to the page
         }
      else if(look.contains("<cs371server>")){//see if the line contains the server tag
         format = "I can't believe my server works\n";//if so post string explaining my excitement
         end = end + format + "\n";//put it in the final string
         }
      else
         end = end + look + "\n";//if no tags, add the text to the final string.
        }
    } catch (Exception e){//if the file could not be found we end up here
      System.err.println("404 error: "+e);//print error
      notFound = 1;//sets the "notFound" flag
      return "404";//returns 404;
      }    
   return end;
}
/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line;
   String [] split;
   try{
   BufferedReader r = new BufferedReader(new InputStreamReader(is));//read the input of the stream
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();//read line
	      split = line.split(" ");//split at the space to seperate the path
	      path = split[1];//grab the path
         if(path.contains("gif")){
            contentType = "image/gif";
            }
         else if(path.contains("jpeg")){
            contentType = "image/jpeg";
            }
         else if(path.contains("png")){
            contentType = "image/png";
            }
         }
         catch (Exception e){//if it couldn't be found, 404
            System.err.println("Request error; "+e);
            return "404";
            }
   return path;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();//puts the date object in
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   if(notFound == 1){//checks the not found flag, if 1 then show 404 error
      os.write("HTTP/1.1 404 ERROR\n".getBytes());
   }
   else{//if not then continue
   os.write("HTTP/1.1 200 OK\n".getBytes());
   }
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jay's server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String content) throws Exception
{
   if(content.equals("404")){//if the earlier function failed and 404 was returned, then we print that and not found
      os.write("404: File not found \n".getBytes());
      }
   else{//else push out the final content.
      os.write(content.getBytes());
   }
 }
private void SendImage(OutputStream os) throws Exception{
   InputStream file = new FileInputStream(path.substring(1));
   double filesize = new File(path.substring(1)).length();
   byte [] tester = new byte [(int)filesize];
   int image = file.read(tester);
      while(image>0){
         os.write(tester,0,image);
         }
}
} // end class
