package de.dplatz.quarkus.livereload.scriptinjection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class ServletResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capture;
    private ServletOutputStream output;
    private PrintWriter writer;

    public ServletResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        capture = new ByteArrayOutputStream(response.getBufferSize());
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }

        if (output == null) {
            output = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    capture.write(b);
                }

                @Override
                public void write(byte b[]) throws IOException {
                    capture.write(b);
                }
                
                @Override
                public void write(byte b[], int off, int len) throws IOException {
                    capture.write(b, off, len);
                }
                
                @Override
                public void flush() throws IOException {
                    capture.flush();
                }

                @Override
                public void close() throws IOException {
                    capture.close();
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setWriteListener(WriteListener arg0) {
                }
            };
        }

        return output;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (output != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }

        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(capture, getCharacterEncoding()));
        }

        return writer;
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
        if (output != null) {
            output.close();
        }
    }
    
    @Override
    public void flushBuffer() throws IOException {
        System.out.println("-- flush buffer");
        if (writer != null) {
            writer.flush();
        } else if (output != null) {
            output.flush();
        }
    }

    public byte[] getResponseData() throws IOException {
        if (writer != null) {
            writer.close();
        } else if (output != null) {
            output.close();
        }
        return capture.toByteArray();
    }
    
    
    
    /*
    @Override
    public void setStatus(int sc, String sm) {
        // TODO Auto-generated method stub
        System.out.println("--setStaus");
        //super.setStatus(sc, sm);
    }

    @Override
    public void setStatus(int sc) {
        System.out.println("--setStaus");
        //super.setStatus(sc);
    }
    */

    /*
    @Override
    public void setContentLength(int len) {
    }

    @Override
    public void setContentLengthLong(long len) {
    }
*/
    @Override
    public String toString() {
        try {
            return new String(getResponseData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}