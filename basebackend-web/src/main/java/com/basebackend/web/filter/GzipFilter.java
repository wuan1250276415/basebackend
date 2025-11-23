package com.basebackend.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip 压缩过滤器
 * 自动压缩响应内容，减少传输大小
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Component
@Order(3)
public class GzipFilter implements Filter {

    private static final String GZIP_ENCODING = "gzip";
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String VARY = "Vary";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 检查是否支持 Gzip 压缩
        String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
        boolean supportsGzip = acceptEncoding != null && acceptEncoding.contains(GZIP_ENCODING);

        // 检查内容类型（只压缩文本内容）
        String contentType = httpResponse.getContentType();
        boolean isCompressible = isCompressibleContent(contentType);

        // 检查响应大小（小于阈值的响应不需要压缩）
        // String contentLength = httpResponse.getHeader("Content-Length");
        // boolean isLargeEnough = contentLength != null && Long.parseLong(contentLength) > 1024;

        if (supportsGzip && isCompressible /*&& isLargeEnough*/) {
            // 包装响应以启用 Gzip 压缩
            GzipResponseWrapper gzipResponse = new GzipResponseWrapper(httpResponse);
            chain.doFilter(request, gzipResponse);
            gzipResponse.close();
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 判断内容类型是否可压缩
     */
    private boolean isCompressibleContent(String contentType) {
        if (contentType == null) {
            return false;
        }

        String lowerType = contentType.toLowerCase();
        return lowerType.startsWith("text/")
                || lowerType.contains("json")
                || lowerType.contains("xml")
                || lowerType.contains("javascript")
                || lowerType.contains("css")
                || lowerType.contains("html");
    }

    /**
     * Gzip 响应包装器
     */
    private static class GzipResponseWrapper extends HttpServletResponseWrapper {

        private GzipServletOutputStream gzipOutputStream;

        public GzipResponseWrapper(HttpServletResponse response) throws IOException {
            super(response);
        }

        @Override
        public void setContentLength(int len) {
            // 重置内容长度，因为压缩后长度会变化
            super.setContentLength(-1);
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, value);
            if (CONTENT_ENCODING.equals(name)) {
                // 设置编码
                super.addHeader(VARY, "Accept-Encoding");
            }
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, value);
            if (CONTENT_ENCODING.equals(name)) {
                super.setHeader(VARY, "Accept-Encoding");
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (gzipOutputStream == null) {
                gzipOutputStream = new GzipServletOutputStream(getResponse().getOutputStream());
            }
            return gzipOutputStream;
        }

        public void close() throws IOException {
            if (gzipOutputStream != null) {
                gzipOutputStream.close();
            }
        }
    }

    /**
     * Gzip Servlet 输出流
     */
    private static class GzipServletOutputStream extends ServletOutputStream {

        private final GZIPOutputStream gzipOutputStream;

        public GzipServletOutputStream(OutputStream originalOutputStream) throws IOException {
            this.gzipOutputStream = new GZIPOutputStream(originalOutputStream) {
                {
                    // 设置压缩级别为 6（平衡速度与压缩比）
                    // def.setLevel(6);
                }
            };
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // Not needed for this implementation
        }

        @Override
        public void write(int b) throws IOException {
            gzipOutputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            gzipOutputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            gzipOutputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            gzipOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                gzipOutputStream.finish();
                gzipOutputStream.flush();
            } finally {
                gzipOutputStream.close();
            }
        }
    }
}
