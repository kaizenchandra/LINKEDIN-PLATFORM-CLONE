package com.meridian.shared.adapter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
/** Bounds JSON before deserialization, including bodies without Content-Length. */
public final class ApplicationBodyLimit extends OncePerRequestFilter {
 public static final int LIMIT=192*1024;
 @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getMethod().equals("PUT")||!request.getRequestURI().matches("/api/v1/jobs/[^/]+/(application|questions)");}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
  byte[] body=request.getContentLengthLong()>LIMIT?null:request.getInputStream().readNBytes(LIMIT+1);
  if(body==null||body.length>LIMIT){response.setStatus(413);response.setContentType("application/problem+json");response.setHeader("Cache-Control","no-store");response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Content Too Large\",\"status\":413,\"detail\":\"Application request exceeds 192 KiB\"}");return;}
  chain.doFilter(new HttpServletRequestWrapper(request){
   @Override public ServletInputStream getInputStream(){var input=new ByteArrayInputStream(body);return new ServletInputStream(){
    public int read(){return input.read();}
    public int read(byte[] b,int off,int len){return input.read(b,off,len);}
    public boolean isFinished(){return input.available()==0;}
    public boolean isReady(){return true;}
    public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Synchronous application endpoint");}
   };}
   @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
  },response);
 }
}
