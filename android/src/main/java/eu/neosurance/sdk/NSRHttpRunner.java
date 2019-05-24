package eu.neosurance.sdk;

import android.os.Build;
import android.support.annotation.RequiresApi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class NSRHttpRunner {

	private static final String USER_AGENT = "NSRHttpRunner/1.0";

	private static final String CRLF = "\r\n";
	private static final int CHUNK_SIZE = 1000 * 1024;

	private int status = -1;
	private String message = null;

	private boolean isPost;
	private boolean isMultipart;

	private String contentType;
	private String charset;
	private HttpURLConnection connection;

	private String payload = null;
	private List<String[]> params;
	private Map<File, String[]> files;

	public NSRHttpRunner(String url) throws Exception {
		init(new URL(url), null, -1);
	}

	public NSRHttpRunner(String url, int timeout) throws Exception {
		init(new URL(url), null, timeout);
	}

	public NSRHttpRunner(String url, String charset) throws Exception {
		init(new URL(url), charset, -1);
	}

	public NSRHttpRunner(URL url) throws Exception {
		init(url, null, -1);
	}

	public NSRHttpRunner(URL url, String charset) throws Exception {
		init(url, charset, -1);
	}

	public NSRHttpRunner() throws Exception {
	}

	public void init(String url) throws Exception {
		init(new URL(url), null, -1);
	}

	private void init(URL url, String charset, int timeout) throws Exception {
		init(url, charset, timeout, null);
	}

	private void init(URL url, String charset, int timeout, Proxy proxy) throws Exception {
		this.connection = proxy != null ? (HttpURLConnection) url.openConnection(proxy) : (HttpURLConnection) url.openConnection();
		this.connection.setRequestProperty("Accept-Encoding", "gzip");
		this.charset = (charset != null ? charset : "UTF-8");
		if (timeout > 0)
			this.connection.setConnectTimeout(timeout);
		this.isPost = false;
		this.isMultipart = false;
	}

	public NSRHttpRunner param(String name, String value) {
		if (params == null)
			params = new ArrayList<String[]>();
		isPost = true;
		params.add(new String[]{name, value});
		return this;
	}

	public NSRHttpRunner payload(String payload, String contentType) {
		isPost = true;
		this.payload = payload;
		this.contentType = contentType;
		return this;
	}

	public NSRHttpRunner file(String name, File file) {
		return file(file, name, null);
	}

	public NSRHttpRunner file(File file, String name, String mime) {
		if (files == null)
			files = new HashMap<File, String[]>();
		isPost = isMultipart = true;
		files.put(file, new String[]{name, mime});
		return this;
	}

	public NSRHttpRunner header(String name, String value) {
		connection.setRequestProperty(name, value);
		return this;
	}

	public NSRHttpRunner contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public String read() throws Exception {
		try {
			send();
			return readResponse();
		} finally {
			if (connection != null)
				connection.disconnect();
		}
	}

	public String save(OutputStream out) throws Exception {
		try {
			send();
			return saveResponse(out);
		} finally {
			if (connection != null)
				connection.disconnect();
		}
	}

	public String save(File file) throws Exception {
		try (FileOutputStream out = new FileOutputStream(file)) {
			return save(out) + "," + file.getAbsolutePath();
		}
	}

	public String save(String file) throws Exception {
		return save(new File(file));
	}

	public HttpURLConnection send() throws Exception {
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("User-Agent", USER_AGENT);
		if (isMultipart)
			sendPostMultipart();
		else if (isPost)
			sendPost();
		else {
			connection.setRequestMethod("GET");
			connection.connect();
		}
		return connection;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	private void sendPost() throws Exception {
		String postData = payload;
		if (postData == null) {
			StringBuilder sb = new StringBuilder();
			for (String[] param : params)
				sb.append(param[0]).append("=").append(URLEncoder.encode(param[1], charset)).append("&");
			postData = sb.toString();
		}
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Type", contentType() + "; charset=" + charset);
		byte[] postBytes = postData.getBytes(charset);
		connection.setRequestProperty("Content-Length", "" + postBytes.length);
		try (OutputStream output = connection.getOutputStream()) {
			output.write(postBytes);
			output.flush();
		}
	}

	private void sendPostMultipart() throws Exception {
		String boundary = "===" + System.currentTimeMillis() + "===";

		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setChunkedStreamingMode(CHUNK_SIZE);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		try (OutputStream output = connection.getOutputStream()) {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
			if (params != null) {
				for (String[] param : params) {
					writer.append("--" + boundary).append(CRLF);
					writer.append("Content-Disposition: form-data; name=\"" + param[0] + "\"").append(CRLF);
					writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
					writer.append(CRLF).append(param[1]).append(CRLF);
				}
				writer.flush();
			}
			for (File f : files.keySet()) {
				String[] props = files.get(f);
				writer.append("--" + boundary).append(CRLF);
				writer.append("Content-Disposition: form-data; name=\"" + props[0] + "\"; filename=\"" + f.getName() + "\"").append(CRLF);
				writer.append("Content-Type: " + props[1]).append(CRLF);
				writer.append("Content-Transfer-Encoding: binary").append(CRLF);
				writer.append(CRLF);
				writer.flush();
				try (FileInputStream in = new FileInputStream(f)) {
					copy(in, output);
				}
				writer.append(CRLF);
				output.flush();
			}
			writer.append(CRLF);
			writer.append("--" + boundary + "--").append(CRLF);
			writer.flush();
		}
	}

	private String saveResponse(OutputStream out) throws Exception {
		status = connection.getResponseCode();
		if (isResponseOk()) {
			try (InputStream in = getInputStream()) {
				copy(in, out);
			}
			return connection.getHeaderField("Content-Type") + "," + connection.getHeaderField("Content-Length");
		} else {
			message = readResponseText();
			throw new IOException("Download failed. Server returned non-OK status: " + status);
		}
	}

	private String readResponse() throws Exception {
		status = connection.getResponseCode();
		String responseText = readResponseText();
		if (isResponseOk()) {
			return responseText;
		} else {
			message = responseText;
			throw new IOException("Server returned non-OK status: " + status);
		}
	}

	public Map<String, List<String>> readResponseHeaders() throws Exception {
		try {
			send();
			status = connection.getResponseCode();
			if (isResponseOk()) {
				return connection.getHeaderFields();
			} else {
				message = readResponseText();
				throw new IOException("Server returned non-OK status: " + status);
			}
		} finally {
			if (connection != null)
				connection.disconnect();
		}
	}

	public String readResponseText() throws Exception {
		StringBuilder output = new StringBuilder();
		try (BufferedReader input = new BufferedReader(new InputStreamReader(getInputStream()))) {
			String line = null;
			while ((line = input.readLine()) != null) {
				output.append(line).append('\n');
			}
			return output.toString();
		}
	}

	public String responseContentType() {
		return connection.getContentType();
	}

	public String contentType() {
		if (isPost)
			return contentType != null ? contentType : (connection.getRequestProperty("Content-Type") != null ? connection.getRequestProperty("Content-Type") : "application/x-www-form-urlencoded");
		else if (isMultipart)
			return "multipart/form-data";
		return null;
	}

	public boolean isResponseOk() throws Exception {
		return (status >= 200 && status < 300);
	}

	public InputStream getInputStream() throws Exception {
		InputStream candidateIS = isResponseOk() ? connection.getInputStream() : connection.getErrorStream();
		return "gzip".equals(connection.getContentEncoding()) ? new GZIPInputStream(candidateIS) : candidateIS;
	}

	public boolean isPost() {
		return isPost;
	}

	public boolean isMultipart() {
		return isMultipart;
	}

	private void copy(InputStream from, OutputStream to) throws Exception {
		byte[] bytes = new byte[4096];
		int len;
		while ((len = from.read(bytes)) >= 0) {
			to.write(bytes, 0, len);
		}
	}
}
