package de.dplatz.quarkus.livereload.scriptinjection;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.runtime.configuration.ProfileManager;
import io.undertow.servlet.spec.HttpServletRequestImpl;

@WebFilter
public class LiveReloadScriptInjectionFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        if (!ProfileManager.getActiveProfile().equals("dev")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (request instanceof HttpServletRequestImpl) {
            String url = ((HttpServletRequestImpl) request).getRequestURL().toString();
            String contextPath = ((HttpServletRequestImpl) request).getContextPath();

            // Just serve by placing in META-INF/resources
            /*
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if (url.endsWith("/live-reload.js")) {
                httpResponse.setContentType("application/javascript");
                String respBody = "console.warn('unable to load livereload.js; live-reload will not work.')";
                try {
                    respBody = new Scanner(LiveReloadScriptInjectionFilter.class.getResourceAsStream("/live-reload.js"), "UTF-8").useDelimiter("\\A").next();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                httpResponse.setContentLength(respBody.length());
                httpResponse.getWriter().write(respBody);
                return;
            }*/
            
            // Limit this filter to interfer only with requests to ".html" or the root "localhost:8080" resource.
            if (contextPath.endsWith(".html") || contextPath.equals("/") || contextPath.equals("")) {

                ServletResponseWrapper wrappedResp = new ServletResponseWrapper((HttpServletResponse) response);

                filterChain.doFilter(request, wrappedResp);

                String respBody = wrappedResp.toString();
                
                if (wrappedResp.getStatus() == 200 &&
                        wrappedResp.getContentType() != null &&
                        wrappedResp.getContentType().contains("text/html")) {
                    
                    String modifiedResponse = respBody.replace("</head>", "</head><script src=\"/live-reload.js\"></script>");                   
                    response.setContentLength(modifiedResponse .length());                    
                    response.getWriter().write(modifiedResponse);
                }
                else {
                    if (respBody != null) {
                        response.getWriter().append(respBody);
                    }
                }
            }
        }
        else {
            filterChain.doFilter(request, response);
        }
    }
    
    @Override
    public void destroy() {
    }
}